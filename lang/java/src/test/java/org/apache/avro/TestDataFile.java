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
package org.apache.avro;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Random;
import java.io.File;
import java.io.IOException;

public class TestDataFile {
  private static final int COUNT =
    Integer.parseInt(System.getProperty("test.count", "10"));
  private static final boolean VALIDATE = 
    !"false".equals(System.getProperty("test.validate", "true"));
  private static final File DIR
    = new File(System.getProperty("test.dir", "/tmp"));
  private static final File DATAFILE_DIR
    = new File(System.getProperty("test.dir", "/tmp"));
  private static final File FILE = new File(DIR, "test.avro");
  private static final long SEED = System.currentTimeMillis();

  private static final String SCHEMA_JSON =
    "{\"type\": \"record\", \"name\": \"Test\", \"fields\": ["
    +"{\"name\":\"stringField\", \"type\":\"string\"},"
    +"{\"name\":\"longField\", \"type\":\"long\"}]}";
  private static final Schema SCHEMA = Schema.parse(SCHEMA_JSON);

  @Test
  public void testGenericWrite() throws IOException {
    DataFileWriter<Object> writer =
      new DataFileWriter<Object>(new GenericDatumWriter<Object>())
      .setSyncInterval(100)
      .create(SCHEMA, FILE);
    try {
      int count = 0;
      for (Object datum : new RandomData(SCHEMA, COUNT, SEED)) {
        writer.append(datum);
        if (++count%(COUNT/3) == 0)
          writer.sync();                          // force some syncs mid-file
      }
    } finally {
      writer.close();
    }
  }

  @Test
  public void testGenericRead() throws IOException {
    DataFileReader<Object> reader =
      new DataFileReader<Object>(FILE, new GenericDatumReader<Object>());
    try {
      Object datum = null;
      if (VALIDATE) {
        for (Object expected : new RandomData(SCHEMA, COUNT, SEED)) {
          datum = reader.next(datum);
          assertEquals(expected, datum);
        }
      } else {
        for (int i = 0; i < COUNT; i++) {
          datum = reader.next(datum);
        }
      }
    } finally {
      reader.close();
    }
  }

  @Test
  public void testSplits() throws IOException {
    DataFileReader<Object> reader =
      new DataFileReader<Object>(FILE, new GenericDatumReader<Object>());
    Random rand = new Random(SEED);
    try {
      int splits = 10;                            // number of splits
      int length = (int)FILE.length();            // length of file
      int end = length;                           // end of split
      int remaining = end;                        // bytes remaining
      int count = 0;                              // count of entries
      while (remaining > 0) {
        int start = Math.max(0, end - rand.nextInt(2*length/splits));
        reader.sync(start);                       // count entries in split
        while (!reader.pastSync(end)) {
          reader.next();
          count++;
        }
        remaining -= end-start;
        end = start;
      }
      assertEquals(COUNT, count);
    } finally {
      reader.close();
    }
  }

  @Test
  public void testGenericAppend() throws IOException {
    long start = FILE.length();
    DataFileWriter<Object> writer =
      new DataFileWriter<Object>(new GenericDatumWriter<Object>())
      .appendTo(FILE);
    try {
      for (Object datum : new RandomData(SCHEMA, COUNT, SEED+1)) {
        writer.append(datum);
      }
    } finally {
      writer.close();
    }
    DataFileReader<Object> reader =
      new DataFileReader<Object>(FILE, new GenericDatumReader<Object>());
    try {
      reader.seek(start);
      Object datum = null;
      if (VALIDATE) {
        for (Object expected : new RandomData(SCHEMA, COUNT, SEED+1)) {
          datum = reader.next(datum);
          assertEquals(expected, datum);
        }
      } else {
        for (int i = 0; i < COUNT; i++) {
          datum = reader.next(datum);
        }
      }
    } finally {
      reader.close();
    }
  }

  protected void readFile(File f, DatumReader<Object> datumReader)
    throws IOException {
    System.out.println("Reading "+ f.getName());
    DataFileReader<Object> reader = new DataFileReader<Object>(f, datumReader);
    for (Object datum : reader) {
      assertNotNull(datum);
    }
  }

  public static void main(String[] args) throws Exception {
    File input = new File(args[0]);
    Schema projection = null;
    if (args.length > 1)
      projection = Schema.parse(new File(args[1]));
    TestDataFile tester = new TestDataFile();
    tester.readFile(input, new GenericDatumReader<Object>(null, projection));
    long start = System.currentTimeMillis();
    for (int i = 0; i < 4; i++)
      tester.readFile(input, new GenericDatumReader<Object>(null, projection));
    System.out.println("Time: "+(System.currentTimeMillis()-start));
  }

  public static class InteropTest {

  @Test
    public void testGeneratedGeneric() throws IOException {
      System.out.println("Reading with generic:");
      readFiles(new GenericDatumReader<Object>());
    }

  @Test
    public void testGeneratedSpecific() throws IOException {
      System.out.println("Reading with specific:");
      readFiles(new SpecificDatumReader<Object>());
    }

  // Can't use same Interop.java as specific for reflect, since its stringField
  // has type Utf8, which reflect would try to assign a String to.  We could
  // fix this by defining a reflect-specific version of Interop.java, but we'd
  // need to put it on a different classpath than the specific one.

  // @Test
  //   public void testGeneratedReflect() throws IOException {
  //     readFiles(new ReflectDatumReader(Interop.class));
  //   }

    private void readFiles(DatumReader<Object> datumReader) throws IOException {
      TestDataFile test = new TestDataFile();
      for (File f : DATAFILE_DIR.listFiles())
        test.readFile(f, datumReader);
    }
  }
}
