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

import java.util.*;
import java.io.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.*;

/** An abstract data type.
 * <p>A schema may be one of:
 * <ul>
 * <li>An <i>record</i>, mapping field names to field value data;
 * <li>An <i>array</i> of values, all of the same schema;
 * <li>A <i>map</i> containing key/value pairs, each of a declared schema;
 * <li>A <i>union</i> of other schemas;
 * <li>A unicode <i>string</i>;
 * <li>A sequence of <i>bytes</i>;
 * <li>A 32-bit signed <i>int</i>;
 * <li>A 64-bit signed <i>long</i>;
 * <li>A 32-bit IEEE single-<i>float</i>; or
 * <li>A 64-bit IEEE <i>double</i>-float; or
 * <li>A <i>boolean</i>; or
 * <li><i>null</i>.
 * </ul>
 */
public abstract class Schema {
  static final JsonTypeMapper MAPPER = new JsonTypeMapper();
  static final JsonFactory FACTORY = new JsonFactory();

  static {
    FACTORY.enableParserFeature(JsonParser.Feature.ALLOW_COMMENTS);
  }

  /** The type of a schema. */
  public enum Type
  { RECORD, ARRAY, MAP, UNION, STRING, BYTES,
      INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL };

  private final Type type;

  Schema(Type type) { this.type = type; }

  /** Create a schema for a primitive type. */
  public static Schema create(Type type) {
    switch (type) {
    case STRING:  return STRING_SCHEMA;
    case BYTES:   return BYTES_SCHEMA;
    case INT:     return INT_SCHEMA ;
    case LONG:    return LONG_SCHEMA ;
    case FLOAT:   return FLOAT_SCHEMA;
    case DOUBLE:  return DOUBLE_SCHEMA;
    case BOOLEAN: return BOOLEAN_SCHEMA;
    case NULL:    return NULL_SCHEMA;
    default: throw new AvroRuntimeException("Can't create a: "+type);
    }
  }

  /** Create an anonymous record schema. */
  public static Schema create(Map<String,Schema> fields) {
    return create(null, null, fields, false);
  }

  /** Create a named record schema. */
  public static Schema create(String name, String namespace,
                              Map<String,Schema> fields, boolean isError) {
    return new RecordSchema(name, namespace, fields, isError);
  }

  /** Create an array schema. */
  public static Schema create(Schema elementType) {
    return new ArraySchema(elementType);
  }

  /** Create a map schema. */
  public static Schema create(Schema keyType, Schema valueType) {
    return new MapSchema(keyType, valueType);
  }

  /** Create a union schema. */
  public static Schema create(List<Schema> types) {
    return new UnionSchema(types);
  }

  /** Return the type of this schema. */
  public Type getType() { return type; }

  /** If this is a record, returns its fields. */
  public Map<String,Schema> getFields() {
    throw new AvroRuntimeException("Not a record: "+this);
  }

  /** If this is a record, returns its name, if any. */
  public String getName() {
    throw new AvroRuntimeException("Not a record: "+this);
  }

  /** If this is a record, returns its namespace, if any. */
  public String getNamespace() {
    throw new AvroRuntimeException("Not a record: "+this);
  }

  /** Returns true if this record is an error type. */
  public boolean isError() {
    throw new AvroRuntimeException("Not a record: "+this);
  }

  /** If this is an array, returns its element type. */
  public Schema getElementType() {
    throw new AvroRuntimeException("Not an array: "+this);
  }

  /** If this is a map, returns its key type. */
  public Schema getKeyType() {
    throw new AvroRuntimeException("Not a map: "+this);
  }

  /** If this is a map, returns its value type. */
  public Schema getValueType() {
    throw new AvroRuntimeException("Not a map: "+this);
  }

  /** If this is a union, returns its types. */
  public List<Schema> getTypes() {
    throw new AvroRuntimeException("Not a union: "+this);
  }

  /** Render this as <a href="http://json.org/">JSON</a>.*/
  public String toString() { return toString(new Names()); }

  /** Render this, resolving names.*/
  public String toString(Map<String,Schema> names) { return toString(); }

  public boolean equals(Object o) {
    if (o == this) return true;
    return o instanceof Schema && type.equals(((Schema)o).type);
  }
  public int hashCode() { return getType().hashCode(); }

  static class RecordSchema extends Schema {
    private final String name; 
    private final String namespace; 
    private final Map<String, Schema> fields;
    private final boolean isError;
    public RecordSchema(String name, String namespace,
                        Map<String, Schema> fields, boolean isError) {
      super(Type.RECORD);
      this.name = name;
      this.namespace = namespace;
      this.fields = fields;
      this.isError = isError;
    }
    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public boolean isError() { return isError; }
    public Map<String, Schema> getFields() { return fields; }
    public boolean equals(Object o) {
      if (o == this) return true;
      return o instanceof RecordSchema
        && fields.equals(((RecordSchema)o).fields);
    }
    public int hashCode() { return getType().hashCode() + fields.hashCode(); }
    public String toString(Map<String,Schema> names) {
      if (this.equals(names.get(name))) return "\""+name+"\"";
      else if (name != null) names.put(name, this);
      StringBuilder buffer = new StringBuilder();
      buffer.append("{\"type\": \""+(isError?"error":"record")+"\", "
                    +(name==null?"":"\"name\": \""+name+"\", ")
                    +"\"fields\": {");
      int count = 0;
      for (Map.Entry<String,Schema> entry : fields.entrySet()) {
        buffer.append("\"");
        buffer.append(entry.getKey());
        buffer.append("\": ");
        buffer.append(entry.getValue().toString(names));
        if (++count < fields.size())
          buffer.append(", ");
      }
      buffer.append("}}");
      return buffer.toString();
    }
  }

  static class ArraySchema extends Schema {
    private final Schema elementType;
    public ArraySchema(Schema elementType) {
      super(Type.ARRAY);
      this.elementType = elementType;
    }
    public Schema getElementType() { return elementType; }
    public boolean equals(Object o) {
      if (o == this) return true;
      return o instanceof ArraySchema
        && elementType.equals(((ArraySchema)o).elementType);
    }
    public int hashCode() {return getType().hashCode()+elementType.hashCode();}
    public String toString(Map<String,Schema> names) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("{\"type\": \"array\", \"items\": ");
      buffer.append(elementType.toString(names));
      buffer.append("}");
      return buffer.toString();
    }
  }

  static class MapSchema extends Schema {
    private final Schema keyType;
    private final Schema valueType;
    public MapSchema(Schema keyType, Schema valueType) {
      super(Type.MAP);
      this.keyType = keyType;
      this.valueType = valueType;
    }
    public Schema getKeyType() { return keyType; }
    public Schema getValueType() { return valueType; }
    public boolean equals(Object o) {
      if (o == this) return true;
      return o instanceof MapSchema
        && keyType.equals(((MapSchema)o).keyType)
        && valueType.equals(((MapSchema)o).valueType);
    }
    public int hashCode() {
      return getType().hashCode()+keyType.hashCode()+valueType.hashCode();
    }
    public String toString(Map<String,Schema> names) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("{\"type\": \"map\", \"keys\":  ");
      buffer.append(keyType.toString(names));
      buffer.append(", \"values\": ");
      buffer.append(valueType.toString(names));
      buffer.append("}");
      return buffer.toString();
    }
  }

  static class UnionSchema extends Schema {
    private final List<Schema> types;
    public UnionSchema(List<Schema> types) {
      super(Type.UNION);
      this.types = types;
      int seen = 0;
      for (Schema type : types) {                 // check legality of union
        if (type.getType() == Type.UNION)
          throw new AvroRuntimeException("Nested union: "+this);
        int mask = 1 << type.getType().ordinal();
        if (type.getType() == Type.RECORD && type.getName() != null)
          continue;
        if ((seen & mask) != 0)
          throw new AvroRuntimeException("Ambiguous union: "+this);
        seen |= mask;
      }
    }
    public List<Schema> getTypes() { return types; }
    public boolean equals(Object o) {
      if (o == this) return true;
      return o instanceof UnionSchema && types.equals(((UnionSchema)o).types);
    }
    public int hashCode() {return getType().hashCode()+types.hashCode();}
    public String toString(Map<String,Schema> names) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("[");
      int count = 0;
      for (Schema type : types) {
        buffer.append(type.toString(names));
        if (++count < types.size())
          buffer.append(", ");
      }
      buffer.append("]");
      return buffer.toString();
    }
  }

  static class StringSchema extends Schema {
    public StringSchema() { super(Type.STRING); }
    public String toString() { return "\"string\""; }
  }

  static class BytesSchema extends Schema {
    public BytesSchema() { super(Type.BYTES); }
    public String toString() { return "\"bytes\""; }
  }

  static class IntSchema extends Schema {
    public IntSchema() { super(Type.INT); }
    public String toString() { return "\"int\""; }
  }

  static class LongSchema extends Schema {
    public LongSchema() { super(Type.LONG); }
    public String toString() { return "\"long\""; }
  }

  static class FloatSchema extends Schema {
    public FloatSchema() { super(Type.FLOAT); }
    public String toString() { return "\"float\""; }
  }

  static class DoubleSchema extends Schema {
    public DoubleSchema() { super(Type.DOUBLE); }
    public String toString() { return "\"double\""; }
  }

  static class BooleanSchema extends Schema {
    public BooleanSchema() { super(Type.BOOLEAN); }
    public String toString() { return "\"boolean\""; }
  }
  
  static class NullSchema extends Schema {
    public NullSchema() { super(Type.NULL); }
    public String toString() { return "\"null\""; }
  }
  
  private static final StringSchema  STRING_SCHEMA =  new StringSchema();
  private static final BytesSchema   BYTES_SCHEMA =   new BytesSchema();
  private static final IntSchema     INT_SCHEMA =     new IntSchema();
  private static final LongSchema    LONG_SCHEMA =    new LongSchema();
  private static final FloatSchema   FLOAT_SCHEMA =   new FloatSchema();
  private static final DoubleSchema  DOUBLE_SCHEMA =  new DoubleSchema();
  private static final BooleanSchema BOOLEAN_SCHEMA = new BooleanSchema();
  private static final NullSchema    NULL_SCHEMA =    new NullSchema();

  public static Schema parse(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    try {
      JsonParser parser = FACTORY.createJsonParser(in);
      try {
        return Schema.parse(MAPPER.read(parser), new Names());
      } catch (JsonParseException e) {
        throw new SchemaParseException(e);
      }
    } finally {
      in.close();
    }
  }

  /** Construct a schema from <a href="http://json.org/">JSON</a> text. */
  public static Schema parse(String jsonSchema) {
    return parse(parseJson(jsonSchema), new Names());
  }

  static final Names PRIMITIVES = new Names(null);
  static {
    PRIMITIVES.put("string",  STRING_SCHEMA);
    PRIMITIVES.put("bytes",   BYTES_SCHEMA);
    PRIMITIVES.put("int",     INT_SCHEMA);
    PRIMITIVES.put("long",    LONG_SCHEMA);
    PRIMITIVES.put("float",   FLOAT_SCHEMA);
    PRIMITIVES.put("double",  DOUBLE_SCHEMA);
    PRIMITIVES.put("boolean", BOOLEAN_SCHEMA);
    PRIMITIVES.put("null",    NULL_SCHEMA);
  }

  static class Names extends LinkedHashMap<String, Schema> {
    private Names defaults = PRIMITIVES;

    public Names(Names defaults) { this.defaults = defaults; }
    public Names() { this(PRIMITIVES); }

    @Override
    public Schema get(Object name) {
      if (containsKey(name))
        return super.get(name);
      if (defaults != null)
        return defaults.get(name);
      return null;
    }
    @Override
    public Schema put(String name, Schema schema) {
      if (get(name) != null)
        throw new SchemaParseException("Can't redefine: "+name);
      return super.put(name, schema);
    }
    public Names except(String name) {
      Names result = new Names(this);
      result.clear(name);
      return result;
    }
    private void clear(String name) { super.put(name, null); }
  }

  /** @see #parse(String) */
  static Schema parse(JsonNode schema, Names names) {
    if (schema.isTextual()) {                     // name
      Schema result = names.get(schema.getTextValue());
      if (result == null)
        throw new SchemaParseException("Undefined name: "+schema);
      return result;
    } else if (schema.isObject()) {
      JsonNode typeNode = schema.getFieldValue("type");
      if (typeNode == null)
        throw new SchemaParseException("No type: "+schema);
      String type = typeNode.getTextValue();
      if (type.equals("record") || type.equals("error")) { // record
        Map<String,Schema> fields = new LinkedHashMap<String,Schema>();
        JsonNode nameNode = schema.getFieldValue("name");
        String name = nameNode != null ? nameNode.getTextValue() : null;
        JsonNode spaceNode = schema.getFieldValue("namespace");
        String space = spaceNode != null ? spaceNode.getTextValue() : null;
        RecordSchema result =
          new RecordSchema(name, space, fields, type.equals("error"));
        if (name != null) names.put(name, result);
        JsonNode props = schema.getFieldValue("fields");
        for (Iterator<String> i = props.getFieldNames(); i.hasNext();) {
          String prop = i.next();
          fields.put(prop, parse(props.getFieldValue(prop), names));
        }
        return result;
      } else if (type.equals("array")) {          // array
        return new ArraySchema(parse(schema.getFieldValue("items"), names));
      } else if (type.equals("map")) {            // map
        return new MapSchema(parse(schema.getFieldValue("keys"), names),
                             parse(schema.getFieldValue("values"), names));
      } else
        throw new SchemaParseException("Type not yet supported: "+type);
    } else if (schema.isArray()) {                // union
      List<Schema> types = new ArrayList<Schema>(schema.size());
      for (JsonNode typeNode : schema)
        types.add(parse(typeNode, names));
      return new UnionSchema(types);
    } else {
      throw new SchemaParseException("Schema not yet supported: "+schema);
    }
  }

  static JsonNode parseJson(String s) {
    try {
      return MAPPER.read(FACTORY.createJsonParser(new StringReader(s)));
    } catch (JsonParseException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
