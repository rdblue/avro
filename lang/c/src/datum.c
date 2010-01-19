/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <limits.h>
#include <assert.h>
#include "avro.h"
#include "schema.h"
#include "datum.h"
#include "encoding.h"

static void
avro_datum_init (avro_datum_t datum, avro_type_t type)
{
  datum->type = type;
  datum->class_type = AVRO_DATUM;
  datum->refcount = 1;
}

int
avro_datum_equal (avro_datum_t a, avro_datum_t b)
{
  if (!(is_avro_datum (a) && is_avro_datum (b)))
    {
      return 0;
    }
  if (avro_typeof (a) != avro_typeof (b))
    {
      return 0;
    }
  switch (avro_typeof (a))
    {
    case AVRO_STRING:
      return strcmp (avro_datum_to_string (a)->s,
		     avro_datum_to_string (b)->s) == 0;
    case AVRO_BYTES:
      return (avro_datum_to_bytes (a)->size == avro_datum_to_bytes (b)->size)
	&& memcmp (avro_datum_to_bytes (a)->bytes,
		   avro_datum_to_bytes (b)->bytes,
		   avro_datum_to_bytes (a)->size) == 0;
    case AVRO_INT:
      return avro_datum_to_int (a)->i == avro_datum_to_int (b)->i;
    case AVRO_LONG:
      return avro_datum_to_long (a)->l == avro_datum_to_long (b)->l;
    case AVRO_FLOAT:
      return avro_datum_to_float (a)->f == avro_datum_to_float (b)->f;
    case AVRO_DOUBLE:
      return avro_datum_to_double (a)->d == avro_datum_to_double (b)->d;
    case AVRO_BOOLEAN:
      return avro_datum_to_boolean (a)->i == avro_datum_to_boolean (b)->i;
    case AVRO_NULL:
      return 1;
    case AVRO_RECORD:
    case AVRO_ENUM:
    case AVRO_FIXED:
    case AVRO_MAP:
    case AVRO_ARRAY:
    case AVRO_UNION:
    case AVRO_LINK:
      /* TODO */
      return 0;
    }
  return 0;
}

avro_datum_t
avro_string (const char *str)
{
  struct avro_string_datum_t *datum =
    malloc (sizeof (struct avro_string_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->s = strdup (str);

  avro_datum_init (&datum->obj, AVRO_STRING);
  return &datum->obj;
}

avro_datum_t
avro_bytes (const char *bytes, int64_t size)
{
  struct avro_bytes_datum_t *datum =
    malloc (sizeof (struct avro_bytes_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->bytes = malloc (size);
  if (!datum->bytes)
    {
      free (datum);
      return NULL;
    }
  memcpy (datum->bytes, bytes, size);
  datum->size = size;

  avro_datum_init (&datum->obj, AVRO_BYTES);
  return &datum->obj;
}

avro_datum_t
avro_int (int32_t i)
{
  struct avro_int_datum_t *datum = malloc (sizeof (struct avro_int_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->i = i;

  avro_datum_init (&datum->obj, AVRO_INT);
  return &datum->obj;
}

avro_datum_t
avro_long (int64_t l)
{
  struct avro_long_datum_t *datum =
    malloc (sizeof (struct avro_long_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->l = l;

  avro_datum_init (&datum->obj, AVRO_LONG);
  return &datum->obj;
}

avro_datum_t
avro_float (float f)
{
  struct avro_float_datum_t *datum =
    malloc (sizeof (struct avro_float_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->f = f;

  avro_datum_init (&datum->obj, AVRO_FLOAT);
  return &datum->obj;
}

avro_datum_t
avro_double (double d)
{
  struct avro_double_datum_t *datum =
    malloc (sizeof (struct avro_double_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->d = d;

  avro_datum_init (&datum->obj, AVRO_DOUBLE);
  return &datum->obj;
}

avro_datum_t
avro_boolean (int8_t i)
{
  struct avro_boolean_datum_t *datum =
    malloc (sizeof (struct avro_boolean_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->i = i;
  avro_datum_init (&datum->obj, AVRO_BOOLEAN);
  return &datum->obj;
}

avro_datum_t
avro_null (void)
{
  static struct avro_obj_t obj = {
    .type = AVRO_NULL,
    .class_type = AVRO_DATUM,
    .refcount = 1
  };
  return &obj;
}

avro_datum_t
avro_record (const char *name)
{
  struct avro_record_datum_t *datum =
    malloc (sizeof (struct avro_record_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->name = strdup (name);
  datum->fields = st_init_strtable ();

  avro_datum_init (&datum->obj, AVRO_RECORD);
  return &datum->obj;
}

avro_datum_t
avro_record_field_get (const avro_datum_t datum, const char *field_name)
{
  struct avro_record_datum_t *field = NULL;
  if (is_avro_datum (datum) && is_avro_record (datum))
    {
      struct avro_record_datum_t *record = avro_datum_to_record (datum);
      st_lookup (record->fields, (st_data_t) field_name,
		 (st_data_t *) & field);
    }
  return &field->obj;
}

int
avro_record_field_set (const avro_datum_t datum,
		       const char *field_name, const avro_datum_t field_value)
{
  if (is_avro_datum (datum) && is_avro_record (datum))
    {
      struct avro_record_datum_t *record = avro_datum_to_record (datum);
      st_insert (record->fields, (st_data_t) field_name,
		 (st_data_t) field_value);
      return 0;
    }
  return EINVAL;
}

avro_datum_t
avro_enum (const char *name, const char *symbol)
{
  struct avro_enum_datum_t *datum =
    malloc (sizeof (struct avro_enum_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->name = strdup (name);
  datum->symbol = strdup (symbol);

  avro_datum_init (&datum->obj, AVRO_ENUM);
  return &datum->obj;
}

avro_datum_t
avro_fixed (const char *name, const int64_t size, const char *bytes)
{
  struct avro_fixed_datum_t *datum =
    malloc (sizeof (struct avro_fixed_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->name = strdup (name);
  datum->size = size;
  datum->bytes = malloc (size);
  if (datum->bytes)
    {
      free (datum);
      return NULL;
    }
  memcpy (datum->bytes, bytes, size);
  avro_datum_init (&datum->obj, AVRO_FIXED);
  return &datum->obj;
}

avro_datum_t
avro_map (const avro_datum_t values)
{
  struct avro_map_datum_t *datum = malloc (sizeof (struct avro_map_datum_t));
  if (!datum)
    {
      return NULL;
    }
  datum->map = st_init_strtable ();
  avro_datum_init (&datum->obj, AVRO_MAP);
  return &datum->obj;
}

avro_datum_t
avro_array (const avro_datum_t items)
{
  struct avro_array_datum_t *datum =
    malloc (sizeof (struct avro_array_datum_t));
  if (!datum)
    {
      return NULL;
    }
  STAILQ_INIT (&datum->els);
  avro_datum_init (&datum->obj, AVRO_ARRAY);
  return &datum->obj;
}

avro_datum_t
avro_datum_incref (avro_datum_t value)
{
  /* TODO */
  return value;
}

void
avro_datum_decref (avro_datum_t value)
{

}

void
avro_datum_print (avro_datum_t value, FILE * fp)
{

}

int
avro_schema_match (avro_schema_t writers_schema, avro_schema_t readers_schema)
{
  if (is_avro_union (writers_schema) || is_avro_union (readers_schema))
    {
      return 1;
    }
  /* union */
  else if (is_avro_primitive (writers_schema)
	   && is_avro_primitive (readers_schema)
	   && avro_typeof (writers_schema) == avro_typeof (readers_schema))
    {
      return 1;
    }
  /* record */
  else if (is_avro_record (writers_schema) && is_avro_record (readers_schema)
	   && strcmp (avro_schema_name (writers_schema),
		      avro_schema_name (readers_schema)) == 0)
    {
      return 1;
    }
  /* fixed */
  else if (is_avro_fixed (writers_schema) && is_avro_fixed (readers_schema)
	   && strcmp (avro_schema_name (writers_schema),
		      avro_schema_name (readers_schema)) == 0
	   && (avro_schema_to_fixed (writers_schema))->size ==
	   (avro_schema_to_fixed (readers_schema))->size)
    {
      return 1;
    }
  /* enum */
  else if (is_avro_enum (writers_schema) && is_avro_enum (readers_schema)
	   && strcmp (avro_schema_name (writers_schema),
		      avro_schema_name (readers_schema)) == 0)
    {
      return 1;
    }
  /* map */
  else if (is_avro_map (writers_schema) && is_avro_map (readers_schema)
	   && avro_typeof ((avro_schema_to_map (writers_schema))->values)
	   == avro_typeof ((avro_schema_to_map (readers_schema))->values))
    {
      return 1;
    }
  /* array */
  else if (is_avro_array (writers_schema) && is_avro_array (readers_schema)
	   && avro_typeof ((avro_schema_to_array (writers_schema))->items)
	   == avro_typeof ((avro_schema_to_array (readers_schema))->items))
    {
      return 1;
    }

  /* handle schema promotion */
  else if (is_avro_int (writers_schema)
	   && (is_avro_long (readers_schema) || is_avro_float (readers_schema)
	       || is_avro_double (readers_schema)))
    {
      return 1;
    }
  else if (is_avro_long (writers_schema)
	   && (is_avro_float (readers_schema)
	       && is_avro_double (readers_schema)))
    {
      return 1;
    }
  else if (is_avro_float (writers_schema) && is_avro_double (readers_schema))
    {
      return 1;
    }
  return 0;
}

static int
read_fixed (avro_reader_t reader, const avro_encoding_t * enc,
	    avro_schema_t writers_schema, avro_schema_t readers_schema,
	    avro_datum_t * datum)
{
  return 1;
}

static int
read_enum (avro_reader_t reader, const avro_encoding_t * enc,
	   avro_schema_t writers_schema, avro_schema_t readers_schema,
	   avro_datum_t * datum)
{
  return 1;
}

static int
read_array (avro_reader_t reader, const avro_encoding_t * enc,
	    avro_schema_t writers_schema, avro_schema_t readers_schema,
	    avro_datum_t * datum)
{
  return 1;
}

static int
read_map (avro_reader_t reader, const avro_encoding_t * enc,
	  avro_schema_t writers_schema, avro_schema_t readers_schema,
	  avro_datum_t * datum)
{
  return 1;
}

static int
read_union (avro_reader_t reader, const avro_encoding_t * enc,
	    avro_schema_t writers_schema, avro_schema_t readers_schema,
	    avro_datum_t * datum)
{
  return 1;
}

static int
read_record (avro_reader_t reader, const avro_encoding_t * enc,
	     avro_schema_t writers_schema, avro_schema_t readers_schema,
	     avro_datum_t * datum)
{
  return 1;
}

int
avro_read_data (avro_reader_t reader, avro_schema_t writers_schema,
		avro_schema_t readers_schema, avro_datum_t * datum)
{
  int rval = EINVAL;
  const avro_encoding_t *enc = &avro_binary_encoding;

  if (!reader || !is_avro_schema (writers_schema) || !datum)
    {
      return EINVAL;
    }

  if (readers_schema == NULL)
    {
      readers_schema = writers_schema;
    }
  else if (!avro_schema_match (writers_schema, readers_schema))
    {
      return EINVAL;
    }

  /* schema resolution */
  if (!is_avro_union (writers_schema) && is_avro_union (readers_schema))
    {
      struct avro_union_branch_t *branch;
      struct avro_union_schema_t *union_schema =
	avro_schema_to_union (readers_schema);

      for (branch = STAILQ_FIRST (&union_schema->branches);
	   branch != NULL; branch = STAILQ_NEXT (branch, branches))
	{
	  if (avro_schema_match (writers_schema, branch->schema))
	    {
	      return avro_read_data (reader, writers_schema, branch->schema,
				     datum);
	    }
	}
      return EINVAL;
    }

  switch (avro_typeof (writers_schema))
    {
    case AVRO_NULL:
      rval = enc->read_null (reader);
      break;

    case AVRO_BOOLEAN:
      {
	int8_t b;
	rval = enc->read_boolean (reader, &b);
	*datum = avro_boolean (b);
      }
      break;

    case AVRO_STRING:
      {
	char *s;
	rval = enc->read_string (reader, &s);
	*datum = avro_string (s);
      }
      break;

    case AVRO_INT:
      {
	int32_t i;
	rval = enc->read_int (reader, &i);
	*datum = avro_int (i);
      }
      break;

    case AVRO_LONG:
      {
	int64_t l;
	rval = enc->read_long (reader, &l);
	*datum = avro_long (l);
      }
      break;

    case AVRO_FLOAT:
      {
	float f;
	rval = enc->read_float (reader, &f);
	*datum = avro_float (f);
      }
      break;

    case AVRO_DOUBLE:
      {
	double d;
	rval = enc->read_double (reader, &d);
	*datum = avro_double (d);
      }
      break;

    case AVRO_BYTES:
      {
	char *bytes;
	int64_t len;
	rval = enc->read_bytes (reader, &bytes, &len);
	*datum = avro_bytes (bytes, len);
      }
      break;

    case AVRO_FIXED:
      rval = read_fixed (reader, enc, writers_schema, readers_schema, datum);
      break;

    case AVRO_ENUM:
      rval = read_enum (reader, enc, writers_schema, readers_schema, datum);
      break;

    case AVRO_ARRAY:
      rval = read_array (reader, enc, writers_schema, readers_schema, datum);
      break;

    case AVRO_MAP:
      rval = read_map (reader, enc, writers_schema, readers_schema, datum);
      break;

    case AVRO_UNION:
      rval = read_union (reader, enc, writers_schema, readers_schema, datum);
      break;

    case AVRO_RECORD:
      rval = read_record (reader, enc, writers_schema, readers_schema, datum);
      break;

    case AVRO_LINK:
      rval =
	avro_read_data (reader, (avro_schema_to_link (writers_schema))->to,
			readers_schema, datum);
      break;
    }

  return rval;
}

struct validate_st
{
  avro_schema_t expected_schema;
  int rval;
};

static int
schema_map_validate_foreach (char *key, avro_datum_t datum,
			     struct validate_st *vst)
{
  if (!avro_schema_datum_validate (vst->expected_schema, datum))
    {
      vst->rval = 0;
      return ST_STOP;
    }
  return ST_CONTINUE;
}

int
avro_schema_datum_validate (avro_schema_t expected_schema, avro_datum_t datum)
{
  if (!is_avro_schema (expected_schema) || !is_avro_datum (datum))
    {
      return EINVAL;
    }

  switch (avro_typeof (expected_schema))
    {
    case AVRO_NULL:
      return is_avro_null (datum);

    case AVRO_BOOLEAN:
      return is_avro_boolean (datum);

    case AVRO_STRING:
      return is_avro_string (datum);

    case AVRO_BYTES:
      return is_avro_bytes (datum);

    case AVRO_INT:
      return is_avro_int (datum)
	|| (is_avro_long (datum)
	    && (INT_MIN <= avro_datum_to_long (datum)->l
		&& avro_datum_to_long (datum)->l <= INT_MAX));

    case AVRO_LONG:
      return is_avro_int (datum) || is_avro_long (datum);

    case AVRO_FLOAT:
      return is_avro_int (datum) || is_avro_long (datum)
	|| is_avro_float (datum);

    case AVRO_DOUBLE:
      return is_avro_int (datum) || is_avro_long (datum)
	|| is_avro_float (datum) || is_avro_double (datum);

    case AVRO_FIXED:
      return (is_avro_fixed (datum)
	      && (avro_schema_to_fixed (expected_schema)->size ==
		  avro_datum_to_fixed (datum)->size));

    case AVRO_ENUM:
      {
	struct avro_enum_schema_t *enump =
	  avro_schema_to_enum (expected_schema);
	struct avro_enum_symbol_t *symbol = STAILQ_FIRST (&enump->symbols);
	while (symbol)
	  {
	    if (!strcmp (symbol->symbol, avro_datum_to_enum (datum)->symbol))
	      {
		return 1;
	      }
	    symbol = STAILQ_NEXT (symbol, symbols);
	  }
	return 0;
      }
      break;

    case AVRO_ARRAY:
      {
	if (is_avro_array (datum))
	  {
	    struct avro_array_datum_t *array = avro_datum_to_array (datum);
	    struct avro_array_element_t *el = STAILQ_FIRST (&array->els);
	    while (el)
	      {
		if (!avro_schema_datum_validate
		    ((avro_schema_to_array (expected_schema))->items,
		     el->datum))
		  {
		    return 0;
		  }
		el = STAILQ_NEXT (el, els);
	      }
	    return 1;
	  }
	return 0;
      }
      break;

    case AVRO_MAP:
      if (is_avro_map (datum))
	{
	  struct validate_st vst = { expected_schema, 1 };
	  st_foreach (avro_datum_to_map (datum)->map,
		      schema_map_validate_foreach, (st_data_t) & vst);
	  return vst.rval;
	}
      break;

    case AVRO_UNION:
      {
	struct avro_union_schema_t *union_schema =
	  avro_schema_to_union (expected_schema);
	struct avro_union_branch_t *branch;

	for (branch = STAILQ_FIRST (&union_schema->branches);
	     branch != NULL; branch = STAILQ_NEXT (branch, branches))
	  {
	    if (avro_schema_datum_validate (branch->schema, datum))
	      {
		return 1;
	      }
	  }
	return 0;
      }
      break;

    case AVRO_RECORD:
      if (is_avro_record (datum))
	{
	  struct avro_record_schema_t *record_schema =
	    avro_schema_to_record (expected_schema);
	  struct avro_record_field_t *field;
	  for (field = STAILQ_FIRST (&record_schema->fields);
	       field != NULL; field = STAILQ_NEXT (field, fields))
	    {
	      avro_datum_t field_datum =
		avro_record_field_get (datum, field->name);
	      if (!field_datum)
		{
		  /* TODO: check for default values */
		  return 0;
		}
	      if (!avro_schema_datum_validate (field->type, field_datum))
		{
		  return 0;
		}
	    }
	  return 1;
	}
      break;

    case AVRO_LINK:
      {
	return
	  avro_schema_datum_validate ((avro_schema_to_link (expected_schema))->to,
				 datum);
      }
      break;
    }
  return 0;
}

static int
write_record (avro_writer_t writer, const avro_encoding_t * enc,
	      avro_schema_t writer_schema, avro_datum_t datum)
{
  /* TODO */
  return EINVAL;
}

static int
write_enum (avro_writer_t writer, const avro_encoding_t * enc,
	    avro_schema_t writer_schema, avro_datum_t datum)
{
  /* TODO */
  return EINVAL;
}

static int
write_fixed (avro_writer_t writer, const avro_encoding_t * enc,
	     avro_schema_t writer_schema, avro_datum_t datum)
{
  /* TODO */
  return EINVAL;
}

static int
write_map (avro_writer_t writer, const avro_encoding_t * enc,
	   avro_schema_t writer_schema, avro_datum_t datum)
{
  /* TODO */
  return EINVAL;
}

static int
write_array (avro_writer_t writer, const avro_encoding_t * enc,
	     avro_schema_t writer_schema, avro_datum_t datum)
{
  /* TODO */
  return EINVAL;
}

int
avro_write_data (avro_writer_t writer, avro_schema_t writer_schema,
		 avro_datum_t datum)
{
  const avro_encoding_t *enc = &avro_binary_encoding;
  int rval = -1;

  if (!(is_avro_schema (writer_schema) && is_avro_datum (datum)))
    {
      return EINVAL;
    }
  if (!avro_schema_datum_validate (writer_schema, datum))
    {
      return EINVAL;
    }
  switch (avro_typeof (writer_schema))
    {
    case AVRO_NULL:
      rval = enc->write_null (writer);
      break;
    case AVRO_BOOLEAN:
      rval = enc->write_boolean (writer, avro_datum_to_boolean (datum)->i);
      break;
    case AVRO_STRING:
      rval = enc->write_string (writer, avro_datum_to_string (datum)->s);
      break;
    case AVRO_BYTES:
      rval = enc->write_bytes (writer, avro_datum_to_bytes (datum)->bytes,
			       avro_datum_to_bytes (datum)->size);
      break;
    case AVRO_INT:
      {
	int32_t i;
	if (is_avro_int (datum))
	  {
	    i = avro_datum_to_int (datum)->i;
	  }
	else if (is_avro_long (datum))
	  {
	    i = (int32_t) avro_datum_to_long (datum)->l;
	  }
	else
	  {
	    assert (0 && "Serious bug in schema validation code");
	  }
	rval = enc->write_int (writer, i);
      }
      break;
    case AVRO_LONG:
      rval = enc->write_long (writer, avro_datum_to_long (datum)->l);
      break;
    case AVRO_FLOAT:
      {
	float f;
	if (is_avro_int (datum))
	  {
	    f = (float) (avro_datum_to_int (datum)->i);
	  }
	else if (is_avro_long (datum))
	  {
	    f = (float) (avro_datum_to_long (datum)->l);
	  }
	else if (is_avro_float (datum))
	  {
	    f = avro_datum_to_float (datum)->f;
	  }
	else if (is_avro_double (datum))
	  {
	    f = (float) (avro_datum_to_double (datum)->d);
	  }
	else
	  {
	    assert (0 && "Serious bug in schema validation code");
	  }
	rval = enc->write_float (writer, f);
      }
      break;
    case AVRO_DOUBLE:
      {
	double d;
	if (is_avro_int (datum))
	  {
	    d = (double) (avro_datum_to_int (datum)->i);
	  }
	else if (is_avro_long (datum))
	  {
	    d = (double) (avro_datum_to_long (datum)->l);
	  }
	else if (is_avro_float (datum))
	  {
	    d = (double) (avro_datum_to_float (datum)->f);
	  }
	else if (is_avro_double (datum))
	  {
	    d = avro_datum_to_double (datum)->d;
	  }
	else
	  {
	    assert (0 && "Bug in schema validation code");
	  }
	rval = enc->write_double (writer, d);
      }
      break;

    case AVRO_RECORD:
      rval = write_record (writer, enc, writer_schema, datum);
      break;
    case AVRO_ENUM:
      rval = write_enum (writer, enc, writer_schema, datum);
      break;
    case AVRO_FIXED:
      rval = write_fixed (writer, enc, writer_schema, datum);
      break;
    case AVRO_MAP:
      rval = write_map (writer, enc, writer_schema, datum);
      break;
    case AVRO_ARRAY:
      rval = write_array (writer, enc, writer_schema, datum);
      break;

    case AVRO_UNION:
      {
	assert (0 && "Bug in schema validation code");
      }
      break;

    case AVRO_LINK:
      rval =
	avro_write_data (writer, (avro_schema_to_link (writer_schema))->to,
			 datum);
      break;
    }
  return rval;
}
