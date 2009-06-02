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

import org.apache.avro.reflect.ReflectData;
import org.apache.avro.test.Simple;
import org.apache.avro.test.Simple.TestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class TestReflect {
  private static final Logger LOG
    = LoggerFactory.getLogger(TestProtocolSpecific.class);

  private static final File FILE = new File("src/test/schemata/simple.avpr");
  private static final Protocol PROTOCOL;
  static {
    try {
      PROTOCOL = Protocol.parse(FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testRecord() throws IOException {
    assertEquals(PROTOCOL.getTypes().get("TestRecord"),
                 ReflectData.getSchema(TestRecord.class));
  }

  @Test
  public void testProtocol() throws IOException {
    assertEquals(PROTOCOL, ReflectData.getProtocol(Simple.class));
  }
}
