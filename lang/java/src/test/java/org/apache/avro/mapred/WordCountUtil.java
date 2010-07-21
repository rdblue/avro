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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.fs.FileUtil;

import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.DataFileStream;

class WordCountUtil {

  private static final File DIR
    = new File(System.getProperty("test.dir", ".") + "/mapred");
  private static final File LINES_FILE
    = new File(new File(DIR, "in"), "lines.avro");
  private static final File LINES_TEXT_FILE
    = new File(new File(DIR, "in"), "lines.txt");
  private static final File COUNTS_FILE
    = new File(new File(DIR, "out"), "part-00000.avro");

  private static final String[] LINES = new String[] {
    "the quick brown fox jumps over the lazy dog",
    "the cow jumps over the moon",
    "the rain in spain falls mainly on the plains"
  };

  private static final Map<String,Long> COUNTS =
    new TreeMap<String,Long>();
  static {
    for (String line : LINES) {
      StringTokenizer tokens = new StringTokenizer(line);
      while (tokens.hasMoreTokens()) {
        String word = tokens.nextToken();
        long count = COUNTS.containsKey(word) ? COUNTS.get(word) : 0L;
        count++;
        COUNTS.put(word, count);
      }
    }
  }

  public static void writeLinesFile() throws IOException {
    FileUtil.fullyDelete(DIR);
    DatumWriter<Utf8> writer = new GenericDatumWriter<Utf8>();
    DataFileWriter<Utf8> out = new DataFileWriter<Utf8>(writer);
    LINES_FILE.getParentFile().mkdirs();
    out.create(Schema.create(Schema.Type.STRING), LINES_FILE);
    for (String line : LINES)
      out.append(new Utf8(line));
    out.close();
  }
  
  public static void writeLinesTextFile() throws IOException {
    FileUtil.fullyDelete(DIR);
    LINES_FILE.getParentFile().mkdirs();
    PrintStream out = new PrintStream(LINES_TEXT_FILE);
    for (String line : LINES)
      out.println(line);
    out.close();
  }

  public static void validateCountsFile() throws IOException {
    DatumReader<Pair<Utf8,Long>> reader
      = new SpecificDatumReader<Pair<Utf8,Long>>();
    InputStream in = new BufferedInputStream(new FileInputStream(COUNTS_FILE));
    DataFileStream<Pair<Utf8,Long>> counts
      = new DataFileStream<Pair<Utf8,Long>>(in,reader);
    int numWords = 0;
    for (Pair<Utf8,Long> wc : counts) {
      assertEquals(wc.key().toString(),
                   COUNTS.get(wc.key().toString()), wc.value());
      numWords++;
    }
    in.close();
    assertEquals(COUNTS.size(), numWords);
  }

}
