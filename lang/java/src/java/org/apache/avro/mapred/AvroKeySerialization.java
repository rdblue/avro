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

package org.apache.avro.mapred;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serializer;
import org.apache.hadoop.conf.Configured;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

/** The {@link Serialization} used by jobs configured with {@link AvroJob}. */
public class AvroKeySerialization<T> extends Configured 
  implements Serialization<AvroWrapper<T>> {

  public boolean accept(Class<?> c) {
    return AvroWrapper.class.isAssignableFrom(c);
  }
  
  /** Returns the specified map output deserializer.  Defaults to the final
   * output deserializer if no map output schema was specified. */
  public Deserializer<AvroWrapper<T>> getDeserializer(Class<AvroWrapper<T>> c) {
    //  We need not rely on mapred.task.is.map here to determine whether map
    //  output or final output is desired, since the mapreduce framework never
    //  creates a deserializer for final output, only for map output.
    String json = getConf().get(AvroJob.MAP_OUTPUT_SCHEMA,
                                getConf().get(AvroJob.OUTPUT_SCHEMA));
    Schema schema = Schema.parse(json);

    String api = getConf().get(AvroJob.MAP_OUTPUT_API,
                               getConf().get(AvroJob.OUTPUT_API));
    DatumReader<T> reader = AvroJob.API_SPECIFIC.equals(api)
      ? new SpecificDatumReader<T>(schema)
      : new GenericDatumReader<T>(schema);

    return new AvroWrapperDeserializer(reader);
  }
  
  private static final DecoderFactory FACTORY = new DecoderFactory();
  static { FACTORY.configureDirectDecoder(true); }

  private class AvroWrapperDeserializer
    implements Deserializer<AvroWrapper<T>> {

    private DatumReader<T> reader;
    private BinaryDecoder decoder;
    
    public AvroWrapperDeserializer(DatumReader<T> reader) {
      this.reader = reader;
    }
    
    public void open(InputStream in) {
      this.decoder = FACTORY.createBinaryDecoder(in, decoder);
    }
    
    public AvroWrapper<T> deserialize(AvroWrapper<T> wrapper)
      throws IOException {
      T datum = reader.read(wrapper == null ? null : wrapper.datum(), decoder);
      if (wrapper == null) {
        wrapper = new AvroWrapper<T>(datum);
      } else {
        wrapper.datum(datum);
      }
      return wrapper;
    }

    public void close() throws IOException {
      decoder.inputStream().close();
    }
    
  }
  
  /** Returns the specified output serializer. */
  public Serializer<AvroWrapper<T>> getSerializer(Class<AvroWrapper<T>> c) {
    // Here we must rely on mapred.task.is.map to tell whether the map output
    // or final output is needed.
    boolean isMap = getConf().getBoolean("mapred.task.is.map", false);

    String json = getConf().get(AvroJob.OUTPUT_SCHEMA);
    if (isMap) 
      json = getConf().get(AvroJob.MAP_OUTPUT_SCHEMA, json);
    Schema schema = Schema.parse(json);

    String api = getConf().get(AvroJob.OUTPUT_API);
    if (isMap) 
      api = getConf().get(AvroJob.MAP_OUTPUT_API, json);

    DatumWriter<T> writer = AvroJob.API_SPECIFIC.equals(api)
      ? new SpecificDatumWriter<T>(schema)
      : new GenericDatumWriter<T>(schema);
    return new AvroWrapperSerializer(writer);
  }

  private class AvroWrapperSerializer implements Serializer<AvroWrapper<T>> {

    private DatumWriter<T> writer;
    private OutputStream out;
    private BinaryEncoder encoder;
    
    public AvroWrapperSerializer(DatumWriter<T> writer) {
      this.writer = writer;
    }

    public void open(OutputStream out) {
      this.out = out;
      this.encoder = new BinaryEncoder(out);
    }

    public void serialize(AvroWrapper<T> wrapper) throws IOException {
      writer.write(wrapper.datum(), encoder);
    }

    public void close() throws IOException {
      out.close();
    }

  }

}
