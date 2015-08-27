/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 *
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
package avro.examples.baseball;  
@SuppressWarnings("all")
/** 選手 is Japanese for player. */
@org.apache.avro.specific.AvroGenerated
public class Player extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 3865593031278745715L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Player\",\"namespace\":\"avro.examples.baseball\",\"doc\":\"選手 is Japanese for player.\",\"fields\":[{\"name\":\"number\",\"type\":\"int\",\"doc\":\"The number of the player\"},{\"name\":\"first_name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"last_name\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"position\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"enum\",\"name\":\"Position\",\"symbols\":[\"P\",\"C\",\"B1\",\"B2\",\"B3\",\"SS\",\"LF\",\"CF\",\"RF\",\"DH\"]}}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** The number of the player */
  @Deprecated public int number;
  @Deprecated public java.lang.String first_name;
  @Deprecated public java.lang.String last_name;
  @Deprecated public java.util.List<avro.examples.baseball.Position> position;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public Player() {}

  /**
   * All-args constructor.
   * @param number The number of the player
   */
  public Player(java.lang.Integer number, java.lang.String first_name, java.lang.String last_name, java.util.List<avro.examples.baseball.Position> position) {
    this.number = number;
    this.first_name = first_name;
    this.last_name = last_name;
    this.position = position;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return number;
    case 1: return first_name;
    case 2: return last_name;
    case 3: return position;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: number = (java.lang.Integer)value$; break;
    case 1: first_name = (java.lang.String)value$; break;
    case 2: last_name = (java.lang.String)value$; break;
    case 3: position = (java.util.List<avro.examples.baseball.Position>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'number' field.
   * @return The number of the player
   */
  public java.lang.Integer getNumber() {
    return number;
  }

  /**
   * Sets the value of the 'number' field.
   * The number of the player
   * @param value the value to set.
   */
  public void setNumber(java.lang.Integer value) {
    this.number = value;
  }

  /**
   * Gets the value of the 'first_name' field.
   */
  public java.lang.String getFirstName() {
    return first_name;
  }

  /**
   * Sets the value of the 'first_name' field.
   * @param value the value to set.
   */
  public void setFirstName(java.lang.String value) {
    this.first_name = value;
  }

  /**
   * Gets the value of the 'last_name' field.
   */
  public java.lang.String getLastName() {
    return last_name;
  }

  /**
   * Sets the value of the 'last_name' field.
   * @param value the value to set.
   */
  public void setLastName(java.lang.String value) {
    this.last_name = value;
  }

  /**
   * Gets the value of the 'position' field.
   */
  public java.util.List<avro.examples.baseball.Position> getPosition() {
    return position;
  }

  /**
   * Sets the value of the 'position' field.
   * @param value the value to set.
   */
  public void setPosition(java.util.List<avro.examples.baseball.Position> value) {
    this.position = value;
  }

  /**
   * Creates a new Player RecordBuilder.
   * @return A new Player RecordBuilder
   */
  public static avro.examples.baseball.Player.Builder newBuilder() {
    return new avro.examples.baseball.Player.Builder();
  }
  
  /**
   * Creates a new Player RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Player RecordBuilder
   */
  public static avro.examples.baseball.Player.Builder newBuilder(avro.examples.baseball.Player.Builder other) {
    return new avro.examples.baseball.Player.Builder(other);
  }
  
  /**
   * Creates a new Player RecordBuilder by copying an existing Player instance.
   * @param other The existing instance to copy.
   * @return A new Player RecordBuilder
   */
  public static avro.examples.baseball.Player.Builder newBuilder(avro.examples.baseball.Player other) {
    return new avro.examples.baseball.Player.Builder(other);
  }
  
  /**
   * RecordBuilder for Player instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Player>
    implements org.apache.avro.data.RecordBuilder<Player> {

    /** The number of the player */
    private int number;
    private java.lang.String first_name;
    private java.lang.String last_name;
    private java.util.List<avro.examples.baseball.Position> position;

    /** Creates a new Builder */
    private Builder() {
      super(avro.examples.baseball.Player.SCHEMA$);
    }
    
    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(avro.examples.baseball.Player.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.number)) {
        this.number = data().deepCopy(fields()[0].schema(), other.number);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.first_name)) {
        this.first_name = data().deepCopy(fields()[1].schema(), other.first_name);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.last_name)) {
        this.last_name = data().deepCopy(fields()[2].schema(), other.last_name);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.position)) {
        this.position = data().deepCopy(fields()[3].schema(), other.position);
        fieldSetFlags()[3] = true;
      }
    }
    
    /**
     * Creates a Builder by copying an existing Player instance
     * @param other The existing instance to copy.
     */
    private Builder(avro.examples.baseball.Player other) {
            super(avro.examples.baseball.Player.SCHEMA$);
      if (isValidValue(fields()[0], other.number)) {
        this.number = data().deepCopy(fields()[0].schema(), other.number);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.first_name)) {
        this.first_name = data().deepCopy(fields()[1].schema(), other.first_name);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.last_name)) {
        this.last_name = data().deepCopy(fields()[2].schema(), other.last_name);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.position)) {
        this.position = data().deepCopy(fields()[3].schema(), other.position);
        fieldSetFlags()[3] = true;
      }
    }

    /**
      * Gets the value of the 'number' field.
      * The number of the player
      * @return The value.
      */
    public java.lang.Integer getNumber() {
      return number;
    }

    /**
      * Sets the value of the 'number' field.
      * The number of the player
      * @param value The value of 'number'.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder setNumber(int value) {
      validate(fields()[0], value);
      this.number = value;
      fieldSetFlags()[0] = true;
      return this; 
    }

    /**
      * Checks whether the 'number' field has been set.
      * The number of the player
      * @return True if the 'number' field has been set, false otherwise.
      */
    public boolean hasNumber() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'number' field.
      * The number of the player
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder clearNumber() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'first_name' field.
      * @return The value.
      */
    public java.lang.String getFirstName() {
      return first_name;
    }

    /**
      * Sets the value of the 'first_name' field.
      * @param value The value of 'first_name'.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder setFirstName(java.lang.String value) {
      validate(fields()[1], value);
      this.first_name = value;
      fieldSetFlags()[1] = true;
      return this; 
    }

    /**
      * Checks whether the 'first_name' field has been set.
      * @return True if the 'first_name' field has been set, false otherwise.
      */
    public boolean hasFirstName() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'first_name' field.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder clearFirstName() {
      first_name = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'last_name' field.
      * @return The value.
      */
    public java.lang.String getLastName() {
      return last_name;
    }

    /**
      * Sets the value of the 'last_name' field.
      * @param value The value of 'last_name'.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder setLastName(java.lang.String value) {
      validate(fields()[2], value);
      this.last_name = value;
      fieldSetFlags()[2] = true;
      return this; 
    }

    /**
      * Checks whether the 'last_name' field has been set.
      * @return True if the 'last_name' field has been set, false otherwise.
      */
    public boolean hasLastName() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'last_name' field.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder clearLastName() {
      last_name = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'position' field.
      * @return The value.
      */
    public java.util.List<avro.examples.baseball.Position> getPosition() {
      return position;
    }

    /**
      * Sets the value of the 'position' field.
      * @param value The value of 'position'.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder setPosition(java.util.List<avro.examples.baseball.Position> value) {
      validate(fields()[3], value);
      this.position = value;
      fieldSetFlags()[3] = true;
      return this; 
    }

    /**
      * Checks whether the 'position' field has been set.
      * @return True if the 'position' field has been set, false otherwise.
      */
    public boolean hasPosition() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'position' field.
      * @return This builder.
      */
    public avro.examples.baseball.Player.Builder clearPosition() {
      position = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    @Override
    public Player build() {
      try {
        Player record = new Player();
        record.number = fieldSetFlags()[0] ? this.number : (java.lang.Integer) defaultValue(fields()[0]);
        record.first_name = fieldSetFlags()[1] ? this.first_name : (java.lang.String) defaultValue(fields()[1]);
        record.last_name = fieldSetFlags()[2] ? this.last_name : (java.lang.String) defaultValue(fields()[2]);
        record.position = fieldSetFlags()[3] ? this.position : (java.util.List<avro.examples.baseball.Position>) defaultValue(fields()[3]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);  

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, org.apache.avro.specific.SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);  

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, org.apache.avro.specific.SpecificData.getDecoder(in));
  }

}
