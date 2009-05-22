{"type": "record", "name":"Interop", "namespace": "org.apache.avro",
  "fields": [
      {"name": "intField", "type": "int"},
      {"name": "longField", "type": "long"},
      {"name": "stringField", "type": "string"},
      {"name": "boolField", "type": "boolean"},
      {"name": "floatField", "type": "float"},
      {"name": "doubleField", "type": "double"},
      {"name": "bytesField", "type": "bytes"},
      {"name": "nullField", "type": "null"},
      {"name": "arrayField", "type": {"type": "array", "items": "double"}},
      {"name": "mapField", "type":
       {"type": "map", "keys": "long", "values":
        {"type": "record", "name": "Foo",
         "fields": [{"name": "label", "type": "string"}]}}},
      {"name": "unionField", "type":
       ["boolean", "double", {"type": "array", "items": "bytes"}]},
      {"name": "enumField", "type":
       {"type": "enum", "name": "Kind", "symbols": ["A","B","C"]}},
      {"name": "fixedField", "type":
       {"type": "fixed", "name": "MD5", "size": 16}},
      {"name": "recordField", "type":
       {"type": "record", "name": "Node",
        "fields": [
            {"name": "label", "type": "string"},
            {"name": "children", "type": {"type": "array", "items": "Node"}}]}}
  ]
}
