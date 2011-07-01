/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.avro.ipc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.NettyTransportCodec.NettyDataPack;
import org.apache.avro.ipc.NettyTransportCodec.NettyFrameDecoder;
import org.apache.avro.ipc.NettyTransportCodec.NettyFrameEncoder;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Netty-based {@link Transceiver} implementation.
 */
public class NettyTransceiver extends Transceiver {
  private static final Logger LOG = LoggerFactory.getLogger(NettyTransceiver.class
      .getName());

  private final AtomicInteger serialGenerator = new AtomicInteger(0);
  private final Map<Integer, Callback<List<ByteBuffer>>> requests = 
    new ConcurrentHashMap<Integer, Callback<List<ByteBuffer>>>();
  
  private final ChannelFactory channelFactory;
  private final ClientBootstrap bootstrap;
  private final InetSocketAddress remoteAddr;
  
  /**
   * Read lock must be acquired whenever using non-final state.
   * Write lock must be acquired whenever modifying state.
   */
  private final ReadWriteLock stateLock = new ReentrantReadWriteLock();
  private Channel channel;       // Synchronized on stateLock
  private Protocol remote;       // Synchronized on stateLock

  NettyTransceiver() {
    channelFactory = null;
    bootstrap = null;
    remoteAddr = null;
  }

  /**
   * Creates a NettyTransceiver, and attempts to connect to the given address.
   * @param addr the address to connect to.
   * @throws IOException if an error occurs connecting to the given address.
   */
  public NettyTransceiver(InetSocketAddress addr) throws IOException {
    this(addr, new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), 
        Executors.newCachedThreadPool()));
  }

  /**
   * Creates a NettyTransceiver, and attempts to connect to the given address.
   * @param addr the address to connect to.
   * @param channelFactory the factory to use to create a new Netty Channel.
   * @throws IOException if an error occurs connecting to the given address.
   */
  public NettyTransceiver(InetSocketAddress addr, ChannelFactory channelFactory) throws IOException {
    if (channelFactory == null) {
      throw new NullPointerException("channelFactory is null");
    }
    
    // Set up.
    this.channelFactory = channelFactory;
    bootstrap = new ClientBootstrap(channelFactory);
    remoteAddr = addr;

    // Configure the event pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = Channels.pipeline();
        p.addLast("frameDecoder", new NettyFrameDecoder());
        p.addLast("frameEncoder", new NettyFrameEncoder());
        p.addLast("handler", new NettyClientAvroHandler());
        return p;
      }
    });

    bootstrap.setOption("tcpNoDelay", true);

    // Make a new connection.
    stateLock.readLock().lock();
    try {
      getChannel();
    } finally {
      stateLock.readLock().unlock();
    }
  }
  
  /**
   * Tests whether the given channel is ready for writing.
   * @return true if the channel is open and ready; false otherwise.
   */
  private static boolean isChannelReady(Channel channel) {
    return (channel != null) && 
      channel.isOpen() && channel.isBound() && channel.isConnected();
  }
  
  /**
   * Gets the Netty channel.  If the channel is not connected, first attempts 
   * to connect.
   * NOTE: The stateLock read lock *must* be acquired before calling this 
   * method.
   * @return the Netty channel
   * @throws IOException if an error occurs connecting the channel.
   */
  private Channel getChannel() throws IOException {
    if (!isChannelReady(channel)) {
      // Need to reconnect
      // Upgrade to write lock
      stateLock.readLock().unlock();
      stateLock.writeLock().lock();
      try {
        LOG.info("Connecting to " + remoteAddr);
        ChannelFuture channelFuture = bootstrap.connect(remoteAddr);
        channelFuture.awaitUninterruptibly();
        if (!channelFuture.isSuccess()) {
          channelFuture.getCause().printStackTrace();
          throw new IOException("Error connecting to " + remoteAddr, 
              channelFuture.getCause());
        }
        channel = channelFuture.getChannel();
      } finally {
        // Downgrade to read lock:
        stateLock.readLock().lock();
        stateLock.writeLock().unlock();
      }
    }
    return channel;
  }
  
  /**
   * Closes the connection to the remote peer if connected.
   */
  private void disconnect() {
    disconnect(false, false);
  }
  
  /**
   * Closes the connection to the remote peer if connected.
   * @param awaitCompletion if true, will block until the close has completed.
   * @param cancelPendingRequests if true, will drain the requests map and 
   * send an IOException to all Callbacks.
   */
  private void disconnect(boolean awaitCompletion, boolean cancelPendingRequests) {
    Map<Integer, Callback<List<ByteBuffer>>> requestsToCancel = null;
    stateLock.writeLock().lock();
    try {
      if (channel != null) {
        LOG.info("Disconnecting from " + remoteAddr);
        ChannelFuture closeFuture = channel.close();
        if (awaitCompletion) {
          closeFuture.awaitUninterruptibly();
        }
        channel = null;
        remote = null;
        if (cancelPendingRequests) {
          // Remove all pending requests (will be canceled after relinquishing 
          // write lock).
          requestsToCancel = 
            new ConcurrentHashMap<Integer, Callback<List<ByteBuffer>>>(requests);
          requests.clear();
        }
      }
    } finally {
      stateLock.writeLock().unlock();
    }
    
    if ((requestsToCancel != null) && !requestsToCancel.isEmpty()) {
      LOG.warn("Removing " + requestsToCancel.size() + " pending request(s).");
      for (Callback<List<ByteBuffer>> request : requestsToCancel.values()) {
        request.handleError(
            new IOException(getClass().getSimpleName() + " closed"));
      }
    }
  }
  
  /**
   * Netty channels are thread-safe, so there is no need to acquire locks.
   * This method is a no-op.
   */
  @Override
  public void lockChannel() {
    
  }
  
  /**
   * Netty channels are thread-safe, so there is no need to acquire locks.
   * This method is a no-op.
   */
  @Override
  public void unlockChannel() {
    
  }

  public void close() {
    try {
      // Close the connection:
      disconnect(true, true);
    } finally {
      // Shut down all thread pools to exit.
      channelFactory.releaseExternalResources();
    }
  }

  @Override
  public String getRemoteName() throws IOException {
    stateLock.readLock().lock();
    try {
      return getChannel().getRemoteAddress().toString();
    } finally {
      stateLock.readLock().unlock();
    }
  }

  /**
   * Override as non-synchronized method because the method is thread safe.
   */
  @Override
  public List<ByteBuffer> transceive(List<ByteBuffer> request) 
    throws IOException {
    try {
      CallFuture<List<ByteBuffer>> transceiverFuture = new CallFuture<List<ByteBuffer>>();
      transceive(request, transceiverFuture);
      return transceiverFuture.get();
    } catch (InterruptedException e) {
      LOG.warn("failed to get the response", e);
      return null;
    } catch (ExecutionException e) {
      LOG.warn("failed to get the response", e);
      return null;
    }
  }
  
  @Override
  public void transceive(List<ByteBuffer> request, 
      Callback<List<ByteBuffer>> callback) throws IOException {
    stateLock.readLock().lock();
    try {
      int serial = serialGenerator.incrementAndGet();
      NettyDataPack dataPack = new NettyDataPack(serial, request);
      requests.put(serial, callback);
      writeDataPack(dataPack);
    } finally {
      stateLock.readLock().unlock();
    }
  }
  
  @Override
  public void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    writeDataPack(new NettyDataPack(serialGenerator.incrementAndGet(), buffers));
  }
  
  /**
   * Writes a NettyDataPack, reconnecting to the remote peer if necessary.
   * @param dataPack the data pack to write.
   * @throws IOException if an error occurs connecting to the remote peer.
   */
  private void writeDataPack(NettyDataPack dataPack) throws IOException {
    stateLock.readLock().lock();
    try {
      getChannel().write(dataPack);
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public List<ByteBuffer> readBuffers() throws IOException {
    throw new UnsupportedOperationException();  
  }
  
  @Override
  public Protocol getRemote() {
    stateLock.readLock().lock();
    try {
      return remote;
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public boolean isConnected() {
    stateLock.readLock().lock();
    try {
      return remote!=null;
    } finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public void setRemote(Protocol protocol) {
    stateLock.writeLock().lock();
    try {
      this.remote = protocol;
    } finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Avro client handler for the Netty transport 
   */
  class NettyClientAvroHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
        throws Exception {
      if (e instanceof ChannelStateEvent) {
        LOG.info(e.toString());
        ChannelStateEvent cse = (ChannelStateEvent)e;
        if ((cse.getState() == ChannelState.OPEN) && (Boolean.FALSE.equals(cse.getValue()))) {
          // Server closed connection; disconnect client side
          LOG.info("Remote peer " + remoteAddr + " closed connection.");
          disconnect();
        }
      }
      super.handleUpstream(ctx, e);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
        throws Exception {
      // channel = e.getChannel();
      super.channelOpen(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
      NettyDataPack dataPack = (NettyDataPack)e.getMessage();
      Callback<List<ByteBuffer>> callback = requests.get(dataPack.getSerial());
      if (callback==null) {
        throw new RuntimeException("Missing previous call info");
      }
      try {
        callback.handleResult(dataPack.getDatas());
      } finally {
        requests.remove(dataPack.getSerial());
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
      LOG.warn("Unexpected exception from downstream.", e.getCause());
      e.getChannel().close();
      // let the blocking waiting exit
      Iterator<Callback<List<ByteBuffer>>> it = requests.values().iterator();
      while(it.hasNext()) {
        it.next().handleError(e.getCause());
        it.remove();
      }
      
    }

  }

}
