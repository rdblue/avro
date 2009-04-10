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
package org.apache.avro.io;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.avro.util.Utf8;

/** Read leaf values.
 * <p>Has no state except that of the OutputStream it wraps.
 * <p>Used by {@link DatumReader} implementations to read datum leaf values.
 * @see ValueWriter
 */
public class ValueReader extends FilterInputStream {
  public ValueReader(InputStream in) {
    super(in);
  }
  
  /** Same contract as {@link InputStream#read()}, except that EOFException is
   * throw when EOF reached rather than returning -1.
   * @throws EOFException if at EOF. */
  public int read() throws IOException {
      int value = in.read();
      if (value < 0) throw new EOFException();
      return value;
  }

  /** Read a string written by {@link ValueWriter#writeUtf8(Utf8)}.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public Utf8 readUtf8(Object old) throws IOException {
    Utf8 utf8 = old instanceof Utf8 ? (Utf8)old : new Utf8();
    utf8.setLength((int)readLong());
    readBytes(utf8.getBytes(), 0, utf8.getLength());
    return utf8;
  }
  /** Read buffer written by {@link ValueWriter#writeBuffer(ByteBuffer)}.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public ByteBuffer readBuffer(Object old) throws IOException {
    int length = (int)readLong();
    ByteBuffer bytes;
    if ((old instanceof ByteBuffer) && ((ByteBuffer)old).capacity() >= length) {
      bytes = (ByteBuffer)old;
      bytes.clear();
    } else {
      bytes = ByteBuffer.allocate(length);
    }
    readBytes(bytes.array(), 0, length);
    bytes.limit(length);
    return bytes;
  }

  /** Read an int written by {@link ValueWriter#writeInt(int)}.
   * @throws EOFException if EOF is reached before reading all the bytes.*/
  public int readInt() throws IOException {
    return (int)readLong();
  }

  /** Read a long written by {@link ValueWriter#writeLong(long)}.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public long readLong() throws IOException {
    long b = read();
    long n = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = read();
      n |= (b & 0x7F) << shift;
    }
    return (n >>> 1) ^ -(n & 1);                  // back to two's-complement
  }

  /** Read a float written by {@link ValueWriter#writeFloat(float)}.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public float readFloat() throws IOException {
      return Float.intBitsToFloat(((read() & 0xff)      ) |
                                  ((read() & 0xff) <<  8) |
                                  ((read() & 0xff) << 16) |
                                  ((read() & 0xff) << 24));
  }

  /** Read a double written by {@link ValueWriter#writeDouble(double)}.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public double readDouble() throws IOException {
      return Double.longBitsToDouble(((read() & 0xffL)      ) |
                                     ((read() & 0xffL) <<  8) |
                                     ((read() & 0xffL) << 16) |
                                     ((read() & 0xffL) << 24) |
                                     ((read() & 0xffL) << 32) |
                                     ((read() & 0xffL) << 40) |
                                     ((read() & 0xffL) << 48) |
                                     ((read() & 0xffL) << 56));
  }

  /** Read a boolean written by {@link ValueWriter#writeBoolean(boolean)}.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public boolean readBoolean() throws IOException {
    return read() == 1;
  }

  /** Read bytes into an array.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public void readBytes(byte[] buffer) throws IOException {
    readBytes(buffer, 0, buffer.length);
  }
  /** Read bytes into an array.
   * @throws EOFException if EOF is reached before reading all the bytes. */
  public void readBytes(byte[] buffer, int offset, int length)
    throws IOException {
    int total = 0;
    while (total < length) {
      int n = read(buffer, offset+total, length-total);
      if (n < 0) throw new EOFException();
      total += n;
    }
  }

}
