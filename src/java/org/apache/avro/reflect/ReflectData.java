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
package org.apache.avro.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.Protocol.Message;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.FixedSize;
import org.apache.avro.ipc.AvroRemoteException;
import org.apache.avro.util.WeakIdentityHashMap;

import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;

/** Utilities to use existing Java classes and interfaces via reflection.
 *
 * <p><b>Records</b>Fields are not permitted to be null.  Fields which are not
 * static or transient are used.
 *
 * <p><b>Arrays</b>Both Java arrays and implementations of {@link Collection}
 * are mapped to Avro arrays.
 *
 * <p><b>{@link String}</b> is mapped to Avro string.
 * <p><b>byte[]</b> is mapped to Avro bytes.
 */
public class ReflectData extends SpecificData {
  
  /** {@link ReflectData} implementation that permits null field values.  The
   * schema generated for each field is a union of its declared type and
   * null. */
  public static class AllowNull extends ReflectData {

    private static final AllowNull INSTANCE = new AllowNull();

    /** Return the singleton instance. */
    public static AllowNull get() { return INSTANCE; }

    protected Schema createFieldSchema(Field field, Map<String, Schema> names) {
      Schema schema = super.createFieldSchema(field, names);
      return Schema.createUnion(Arrays.asList(new Schema[] {
            schema,
            Schema.create(Schema.Type.NULL) }));
    }
  }
  
  private static final ReflectData INSTANCE = new ReflectData();

  protected ReflectData() {}
  
  /** Return the singleton instance. */
  public static ReflectData get() { return INSTANCE; }

  @Override
  protected boolean isRecord(Object datum) {
    if (datum == null) return false;
    return getSchema(datum.getClass()).getType() == Schema.Type.RECORD;
  }

  @Override
  protected boolean isArray(Object datum) {
    return (datum instanceof Collection) || datum.getClass().isArray();
  }

  @Override
  protected boolean isString(Object datum) {
    return datum instanceof String;
  }

  @Override
  protected boolean isBytes(Object datum) {
    if (datum == null) return false;
    Class c = datum.getClass();
    return c.isArray() && c.getComponentType() == Byte.TYPE;
  }

  @Override
  protected Schema getRecordSchema(Object record) {
    return getSchema(record.getClass());
  }

  @Override
  public boolean validate(Schema schema, Object datum) {
    switch (schema.getType()) {
    case RECORD:
      Class c = datum.getClass(); 
      if (!(datum instanceof Object)) return false;
      for (Map.Entry<String, Schema> entry : schema.getFieldSchemas()) {
        try {
          if (!validate(entry.getValue(),
                        getField(c, entry.getKey()).get(datum)))
          return false;
        } catch (IllegalAccessException e) {
          throw new AvroRuntimeException(e);
        }
      }
      return true;
    case ARRAY:
      if (datum instanceof Collection) {          // collection
        for (Object element : (Collection)datum)
          if (!validate(schema.getElementType(), element))
            return false;
        return true;
      } else if (datum.getClass().isArray()) {    // array
        int length = java.lang.reflect.Array.getLength(datum);
        for (int i = 0; i < length; i++)
          if (!validate(schema.getElementType(),
                        java.lang.reflect.Array.get(datum, i)))
            return false;
        return true;
      }
      return false;
    default:
      return super.validate(schema, datum);
    }
  }

  private static final Map<Class,Map<String,Field>> FIELD_CACHE =
    new ConcurrentHashMap<Class,Map<String,Field>>();

  /** Return the named field of the provided class.  Implementation caches
   * values, since this is used at runtime to get and set fields. */
  protected static Field getField(Class c, String name) {
    Map<String,Field> fields = FIELD_CACHE.get(c);
    if (fields == null) {
      fields = new ConcurrentHashMap<String,Field>();
      FIELD_CACHE.put(c, fields);
    }
    Field f = fields.get(name);
    if (f == null) {
      f = findField(c, name);
      fields.put(name, f);
    }
    return f;
  }

  private static Field findField(Class c, String name) {
    do {
      try {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
      } catch (NoSuchFieldException e) {}
      c = c.getSuperclass();
    } while (c != null);
    throw new AvroRuntimeException("No field named "+name+" in: "+c);
  }

  // Indicates the Java representation for an array schema.  If an entry is
  // present, it contains the Java Collection class of this array.  If no entry
  // is present, then a Java array should be used to implement this array.
  private static final Map<Schema,Class> COLLECTION_CLASSES =
    new WeakIdentityHashMap<Schema,Class>();
  private static synchronized void setCollectionClass(Schema schema, Class c) {
    COLLECTION_CLASSES.put(schema, c);
  }

  /** Return the {@link Collection} subclass that implements this schema.*/
  public static synchronized Class getCollectionClass(Schema schema) {
    return COLLECTION_CLASSES.get(schema);
  }

  private static final Class BYTES_CLASS = new byte[0].getClass();

  @Override
  public Class getClass(Schema schema) {
    switch (schema.getType()) {
    case ARRAY:
      Class collectionClass = getCollectionClass(schema);
      if (collectionClass != null)
        return collectionClass;
      return java.lang.reflect.Array.newInstance(getClass(schema.getElementType()),0).getClass();
    case STRING:  return String.class;
    case BYTES:   return BYTES_CLASS;
    default:
      return super.getClass(schema);
    }
  }

  @Override
  @SuppressWarnings(value="unchecked")
  protected Schema createSchema(Type type, Map<String,Schema> names) {
    if (type instanceof GenericArrayType) {                  // generic array
      Type component = ((GenericArrayType)type).getGenericComponentType();
      if (component == Byte.TYPE)                            // byte array
        return Schema.create(Schema.Type.BYTES);           
      return Schema.createArray(createSchema(component, names));
    } else if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType)type;
      Class raw = (Class)ptype.getRawType();
      Type[] params = ptype.getActualTypeArguments();
      if (Map.class.isAssignableFrom(raw)) {                 // Map
        Type key = params[0];
        Type value = params[1];
        if (!(key == String.class))
          throw new AvroTypeException("Map key class not String: "+key);
        return Schema.createMap(createSchema(value, names));
      } else if (Collection.class.isAssignableFrom(raw)) {   // Collection
        if (params.length != 1)
          throw new AvroTypeException("No array type specified.");
        Schema schema = Schema.createArray(createSchema(params[0], names));
        setCollectionClass(schema, raw);
        return schema;
      }
    } else if (type instanceof Class) {                      // Class
      Class c = (Class)type;
      if (c.isPrimitive() || Number.class.isAssignableFrom(c)
          || c == Void.class || c == Boolean.class)          // primitive
        return super.createSchema(type, names);
      if (c.isArray()) {                                     // array
        Class component = c.getComponentType();
        if (component == Byte.TYPE)                          // byte array
          return Schema.create(Schema.Type.BYTES);
        return Schema.createArray(createSchema(component, names));
      }
      if (c == String.class)                                 // String
        return Schema.create(Schema.Type.STRING);
      String fullName = c.getName();
      Schema schema = names.get(fullName);
      if (schema == null) {
        String name = c.getSimpleName();
        String space = c.getPackage().getName();
        if (c.getEnclosingClass() != null)                   // nested class
          space = c.getEnclosingClass().getName() + "$";
        if (c.isEnum()) {                                    // Enum
          List<String> symbols = new ArrayList<String>();
          Enum[] constants = (Enum[])c.getEnumConstants();
          for (int i = 0; i < constants.length; i++)
            symbols.add(constants[i].name());
          schema = Schema.createEnum(name, space, symbols);
        } else if (GenericFixed.class.isAssignableFrom(c)) { // fixed
          int size = ((FixedSize)c.getAnnotation(FixedSize.class)).value();
          schema = Schema.createFixed(name, space, size);
        } else {                                             // record
          LinkedHashMap<String,Schema.Field> fields =
            new LinkedHashMap<String,Schema.Field>();
          schema = Schema.createRecord(name, space,
                                       Throwable.class.isAssignableFrom(c));
          names.put(c.getName(), schema);
          for (Field field : getFields(c))
            if ((field.getModifiers()&(Modifier.TRANSIENT|Modifier.STATIC))==0){
              Schema fieldSchema = createFieldSchema(field, names);
              fields.put(field.getName(), new Schema.Field(fieldSchema, null));
            }
          schema.setFields(fields);
        }
        names.put(fullName, schema);
      }
      return schema;
    }
    return super.createSchema(type, names);
  }

  // Return of this class and its superclasses to serialize.
  // Not cached, since this is only used to create schemas, which are cached.
  private Collection<Field> getFields(Class recordClass) {
    Map<String,Field> fields = new LinkedHashMap<String,Field>();
    Class c = recordClass;
    do {
      if (c.getPackage().getName().startsWith("java."))
        break;                                    // skip java built-in classes
      for (Field field : c.getDeclaredFields())
        if ((field.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0)
          if (fields.put(field.getName(), field) != null)
            throw new AvroTypeException(c+" contains two fields named: "+field);
      c = c.getSuperclass();
    } while (c != null);
    return fields.values();
  }

  /** Create a schema for a field. */
  protected Schema createFieldSchema(Field field, Map<String, Schema> names) {
    return createSchema(field.getGenericType(), names);
  }

  /** Return the protocol for a Java interface.
   * <p>Note that this requires that <a
   * href="http://paranamer.codehaus.org/">Paranamer</a> is run over compiled
   * interface declarations, since Java 6 reflection does not provide access to
   * method parameter names.  See Avro's build.xml for an example. */
  @Override
  public Protocol getProtocol(Class iface) {
    Protocol protocol =
      new Protocol(iface.getSimpleName(), iface.getPackage().getName()); 
    Map<String,Schema> names = new LinkedHashMap<String,Schema>();
    for (Method method : iface.getMethods())
      if ((method.getModifiers() & Modifier.STATIC) == 0)
        protocol.getMessages().put(method.getName(),
                                   getMessage(method, protocol, names));

    // reverse types, since they were defined in reference order
    List<Schema> types = new ArrayList<Schema>();
    types.addAll(names.values());
    Collections.reverse(types);
    protocol.setTypes(types);

    return protocol;
  }

  private final Paranamer paranamer = new CachingParanamer();

  private Message getMessage(Method method, Protocol protocol,
                             Map<String,Schema> names) {
    LinkedHashMap<String,Schema.Field> fields =
      new LinkedHashMap<String,Schema.Field>();
    String[] paramNames = paranamer.lookupParameterNames(method);
    Type[] paramTypes = method.getGenericParameterTypes();
    for (int i = 0; i < paramTypes.length; i++) {
      Schema paramSchema = getSchema(paramTypes[i], names);
      String paramName =  paramNames.length == paramTypes.length
        ? paramNames[i]
        : paramSchema.getName()+i;
      fields.put(paramName, new Schema.Field(paramSchema, null));
    }
    Schema request = Schema.createRecord(fields);

    Schema response = getSchema(method.getGenericReturnType(), names);

    List<Schema> errs = new ArrayList<Schema>();
    errs.add(Protocol.SYSTEM_ERROR);              // every method can throw
    for (Type err : method.getGenericExceptionTypes())
      if (err != AvroRemoteException.class) 
        errs.add(getSchema(err, names));
    Schema errors = Schema.createUnion(errs);

    return protocol.createMessage(method.getName(), request, response, errors);
  }

  private Schema getSchema(Type type, Map<String,Schema> names) {
    try {
      return createSchema(type, names);
    } catch (AvroTypeException e) {               // friendly exception
      throw new AvroTypeException("Error getting schema for "+type+": "
                                  +e.getMessage(), e);
    }
  }

  @Override
  public int compare(Object o1, Object o2, Schema s) {
    throw new UnsupportedOperationException();
  }


}
