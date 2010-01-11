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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.avro.TestReflect.SampleRecord.AnotherSampleRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.reflect.Union;
import org.apache.avro.reflect.Stringable;

import org.junit.Test;

public class TestReflect {

  // test primitive type inference
  @Test public void testVoid() {
    check(Void.TYPE, "\"null\"");
    check(Void.class, "\"null\"");
  }

  @Test public void testBoolean() {
    check(Boolean.TYPE, "\"boolean\"");
    check(Boolean.class, "\"boolean\"");
  }

  @Test public void testInt() {
    check(Integer.TYPE, "\"int\"");
    check(Integer.class, "\"int\"");
  }

  @Test public void testLong() {
    check(Long.TYPE, "\"long\"");
    check(Long.class, "\"long\"");
  }

  @Test public void testFloat() {
    check(Float.TYPE, "\"float\"");
    check(Float.class, "\"float\"");
  }

  @Test public void testDouble() {
    check(Double.TYPE, "\"double\"");
    check(Double.class, "\"double\"");
  }

  @Test public void testString() {
    check("Foo", "\"string\"");
  }

  @Test public void testBytes() {
    check(new byte[0], "\"bytes\"");
  }

  // test map, array and list type inference
  public static class R1 {
    private Map<String,String> mapField = new HashMap<String,String>();
    private String[] arrayField = new String[] { "foo" };
    private List<String> listField = new ArrayList<String>();

    {
      mapField.put("foo", "bar");
      listField.add("foo");
    }
    
    public boolean equals(Object o) {
      if (!(o instanceof R1)) return false;
      R1 that = (R1)o;
      return mapField.equals(that.mapField)
        && Arrays.equals(this.arrayField, that.arrayField) 
        &&  listField.equals(that.listField);
    }
  }

  @Test public void testMap() throws Exception {
    check(R1.class.getDeclaredField("mapField").getGenericType(),
          "{\"type\":\"map\",\"values\":\"string\"}");
  }

  @Test public void testArray() throws Exception {
    check(R1.class.getDeclaredField("arrayField").getGenericType(),
          "{\"type\":\"array\",\"items\":\"string\"}");
  }
  @Test public void testList() throws Exception {
    check(R1.class.getDeclaredField("listField").getGenericType(),
          "{\"type\":\"array\",\"items\":\"string\""
          +",\"java-class\":\"java.util.List\"}");
  }

  @Test public void testR1() throws Exception {
    checkReadWrite(new R1());
  }

  // test record, array and list i/o
  public static class R2 {
    private String[] arrayField;
    private Collection<String> collectionField;
    
    public boolean equals(Object o) {
      if (!(o instanceof R2)) return false;
      R2 that = (R2)o;
      return Arrays.equals(this.arrayField, that.arrayField) 
        &&  collectionField.equals(that.collectionField);
    }
  }

  @Test public void testR2() throws Exception {
    R2 r2 = new R2();
    r2.arrayField = new String[] {"foo"};
    r2.collectionField = new ArrayList<String>();
    r2.collectionField.add("foo");
    checkReadWrite(r2);
  }

  // test array i/o of unboxed type
  public static class R3 {
    private int[] intArray;
    
    public boolean equals(Object o) {
      if (!(o instanceof R3)) return false;
      R3 that = (R3)o;
      return Arrays.equals(this.intArray, that.intArray);
    }
  }

  @Test public void testR3() throws Exception {
    R3 r3 = new R3();
    r3.intArray = new int[] {1};
    checkReadWrite(r3);
  }

  // test inherited fields & short datatype
  public static class R4 {
    public short value;
    public short[] shorts;
    
    public boolean equals(Object o) {
      if (!(o instanceof R4)) return false;
      R4 that = (R4)o;
      return this.value == that.value
        && Arrays.equals(this.shorts, that.shorts);
    }
  }

  public static class R5 extends R4 {}

  @Test public void testR5() throws Exception {
    R5 r5 = new R5();
    r5.value = 1;
    r5.shorts = new short[] {3,255,256,Short.MAX_VALUE,Short.MIN_VALUE};
    checkReadWrite(r5);
  }

  // test union annotation on a class
  @Union({R7.class, R8.class})
  public static class R6 {}

  public static class R7 extends R6 {
    public int value;
    public boolean equals(Object o) {
      if (!(o instanceof R7)) return false;
      return this.value == ((R7)o).value;
    }
  }
  public static class R8 extends R6 {
    public float value;
    public boolean equals(Object o) {
      if (!(o instanceof R8)) return false;
      return this.value == ((R8)o).value;
    }
  }

  // test arrays of union annotated class
  public static class R9  {
    public R6[] r6s;
    public boolean equals(Object o) {
      if (!(o instanceof R9)) return false;
      return Arrays.equals(this.r6s, ((R9)o).r6s);
    }
  }

  @Test public void testR6() throws Exception {
    R7 r7 = new R7();
    r7.value = 1;
    checkReadWrite(r7, ReflectData.get().getSchema(R6.class));
    R8 r8 = new R8();
    r8.value = 1;
    checkReadWrite(r8, ReflectData.get().getSchema(R6.class));
    R9 r9 = new R9();
    r9.r6s = new R6[] {r7, r8};
    checkReadWrite(r9, ReflectData.get().getSchema(R9.class));
  }

  // test union annotation on methods and parameters
  public static interface P0 {
    @Union({Void.class,String.class})
      String foo(@Union({Void.class,String.class}) String s);
  }

  @Test public void testP0() throws Exception {
    Protocol p0 = ReflectData.get().getProtocol(P0.class);
    Protocol.Message message = p0.getMessages().get("foo");
    // check response schema is union
    Schema response = message.getResponse();
    assertEquals(Schema.Type.UNION, response.getType());
    assertEquals(Schema.Type.NULL, response.getTypes().get(0).getType());
    assertEquals(Schema.Type.STRING, response.getTypes().get(1).getType());
    // check request schema is union
    Schema request = message.getRequest();
    Schema param = request.getFields().get("s").schema();
    assertEquals(Schema.Type.UNION, param.getType());
    assertEquals(Schema.Type.NULL, param.getTypes().get(0).getType());
    assertEquals(Schema.Type.STRING, param.getTypes().get(1).getType());
    // check union erasure
    assertEquals(String.class, ReflectData.get().getClass(response));
    assertEquals(String.class, ReflectData.get().getClass(param));
  }

  // test Stringable annotation
  @Stringable public static class R10 {
    private String text;
    public R10(String text) { this.text = text; }
    public String toString() { return text; }
    public boolean equals(Object o) {
      if (!(o instanceof R10)) return false;
      return this.text.equals(((R10)o).text);
    }
  }
  
  @Test public void testR10() throws Exception {
    Schema r10Schema = ReflectData.get().getSchema(R10.class);
    assertEquals(Schema.Type.STRING, r10Schema.getType());
    assertEquals(R10.class.getName(), r10Schema.getProp("java-class"));
    checkReadWrite(new R10("foo"), r10Schema);
  }

  void checkReadWrite(Object object) throws Exception {
    checkReadWrite(object, ReflectData.get().getSchema(object.getClass()));
  }
  void checkReadWrite(Object object, Schema s) throws Exception {
    ReflectDatumWriter<Object> writer = new ReflectDatumWriter<Object>(s);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writer.write(object, new BinaryEncoder(out));
    ReflectDatumReader<Object> reader = new ReflectDatumReader<Object>(s);
    Object after =
      reader.read(null, new BinaryDecoder
                  (new ByteArrayInputStream(out.toByteArray())));
    assertEquals(object, after);
  }

  public static enum E { A, B };
  @Test public void testEnum() throws Exception {
    check(E.class, "{\"type\":\"enum\",\"name\":\"E\",\"namespace\":"
          +"\"org.apache.avro.TestReflect$\",\"symbols\":[\"A\",\"B\"]}");
  }

  public static class R { int a; long b; }
  @Test public void testRecord() throws Exception {
    check(R.class, "{\"type\":\"record\",\"name\":\"R\",\"namespace\":"
          +"\"org.apache.avro.TestReflect$\",\"fields\":["
          +"{\"name\":\"a\",\"type\":\"int\"},"
          +"{\"name\":\"b\",\"type\":\"long\"}]}");
  }

  private void check(Object o, String schemaJson) {
    check(o.getClass(), schemaJson);
  }

  private void check(Type type, String schemaJson) {
    assertEquals(schemaJson, ReflectData.get().getSchema(type).toString());
  }

  @Test
  public void testRecordIO() throws IOException {
    Schema schm = ReflectData.get().getSchema(SampleRecord.class);
    ReflectDatumWriter<SampleRecord> writer = 
      new ReflectDatumWriter<SampleRecord>(schm);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SampleRecord record = new SampleRecord();
    record.x = 5;
    record.y = 10;
    writer.write(record, new BinaryEncoder(out));
    ReflectDatumReader<SampleRecord> reader = 
      new ReflectDatumReader<SampleRecord>(schm);
    SampleRecord decoded =
      reader.read(null, new BinaryDecoder
                  (new ByteArrayInputStream(out.toByteArray())));
    assertEquals(record, decoded);
  }

  @Test
  public void testRecordWithNullIO() throws IOException {
    ReflectData reflectData = ReflectData.AllowNull.get();
    Schema schm = reflectData.getSchema(AnotherSampleRecord.class);
    ReflectDatumWriter<AnotherSampleRecord> writer = 
      new ReflectDatumWriter<AnotherSampleRecord>(schm);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // keep record.a null and see if that works
    AnotherSampleRecord a = new AnotherSampleRecord();
    writer.write(a, new BinaryEncoder(out));
    AnotherSampleRecord b = new AnotherSampleRecord(10);
    writer.write(b, new BinaryEncoder(out));
    ReflectDatumReader<AnotherSampleRecord> reader = 
      new ReflectDatumReader<AnotherSampleRecord>(schm);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    AnotherSampleRecord decoded = reader.read(null, new BinaryDecoder(in));
    assertEquals(a, decoded);
    decoded = reader.read(null, new BinaryDecoder(in));
    assertEquals(b, decoded);
  }

  public static class SampleRecord {
    public int x = 1;
    private int y = 2;

    public int hashCode() {
      return x + y;
    }

    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final SampleRecord other = (SampleRecord)obj;
      if (x != other.x)
        return false;
      if (y != other.y)
        return false;
      return true;
    }
    
    public static class AnotherSampleRecord {
      private Integer a = null;
      private SampleRecord s = null;

      public AnotherSampleRecord() {
      }

      AnotherSampleRecord(Integer a) {
        this.a = a;
        this.s = new SampleRecord();
      }

      public int hashCode() {
        int hash = (a != null ? a.hashCode() : 0);
        hash += (s != null ? s.hashCode() : 0);
        return hash;
      }

      public boolean equals(Object other) {
        if (other instanceof AnotherSampleRecord) {
          AnotherSampleRecord o = (AnotherSampleRecord) other;
          if ( (this.a == null && o.a != null) ||
               (this.a != null && !this.a.equals(o.a)) ||
               (this.s == null && o.s != null) ||
               (this.s != null && !this.s.equals(o.s)) ) {
            return false;
          }
          return true;
        } else {
          return false;
        }
      }
    }
  }

  public static class X { int i; }
  public static class B1 { X x; }
  public static class B2 { X x; }
  public static class A { B1 b1; B2 b2; }
  public static interface C { void foo(A a); }

  @Test
  public void testForwardReference() {
    ReflectData data = ReflectData.get();
    Protocol reflected = data.getProtocol(C.class);
    Protocol reparsed = Protocol.parse(reflected.toString());
    assertEquals(reflected, reparsed);
    assert(reparsed.getTypes().contains(data.getSchema(A.class)));
    assert(reparsed.getTypes().contains(data.getSchema(B1.class)));
    assert(reparsed.getTypes().contains(data.getSchema(B2.class)));
    assert(reparsed.getTypes().contains(data.getSchema(X.class)));
  }

}
