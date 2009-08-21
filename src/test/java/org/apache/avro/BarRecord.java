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

import org.apache.avro.util.Utf8;

public class BarRecord {
  private Utf8 beerMsg;

  public BarRecord() {
  }

  public BarRecord(String beerMsg) {
    this.beerMsg = new Utf8(beerMsg);
  }

  @Override
  public boolean equals(Object that) {
    if (that instanceof BarRecord) {
      return this.beerMsg.equals(((BarRecord) that).beerMsg);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return beerMsg.hashCode();
  }

  @Override
  public String toString() {
    return BarRecord.class.getSimpleName() + "{msg=" + beerMsg + "}";
  }
}