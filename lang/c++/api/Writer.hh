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

#ifndef avro_Writer_hh__
#define avro_Writer_hh__

#include <boost/noncopyable.hpp>

#include "OutputStreamer.hh"
#include "Zigzag.hh"
#include "Types.hh"

namespace avro {

/// Class for writing avro data to a stream.

class Writer : private boost::noncopyable
{

  public:

    explicit Writer(OutputStreamer &out) :
        out_(out)
    {}

    void writeValue(const Null &) {}

    void writeValue(bool val) {
        int8_t byte = (val != 0);
        out_.writeByte(byte);
    }

    void writeValue(int32_t val) {
        boost::array<uint8_t, 5> bytes;
        size_t size = encodeInt32(val, bytes);
        out_.writeBytes(bytes.data(), size);
    }

    void writeValue(int64_t val) {
        boost::array<uint8_t, 10> bytes;
        size_t size = encodeInt64(val, bytes);
        out_.writeBytes(bytes.data(), size);
    }

    void writeValue(float val) {
        union {
            float f;
            int32_t i;
        } v;
    
        v.f = val;
        out_.writeWord(v.i);
    }

    void writeValue(double val) {
        union {
            double d;
            int64_t i;
        } v;
        
        v.d = val;
        out_.writeLongWord(v.i);
    }

    void writeValue(const std::string &val) {
        writeBytes(reinterpret_cast<const uint8_t *>(val.c_str()), val.size());
    }

    void writeBytes(const uint8_t *val, size_t size) {
        this->writeValue(static_cast<int64_t>(size));
        out_.writeBytes(val, size);
    }

    template <size_t N>
    void writeFixed(const uint8_t (&val)[N]) {
        out_.writeBytes(val, N);
    }

    template <size_t N>
    void writeFixed(const boost::array<uint8_t, N> &val) {
        out_.writeBytes(val.data(), val.size());
    }

    void writeRecord() {}

    void writeArrayBlock(int64_t size) {
        this->writeValue(static_cast<int64_t>(size));
    }

    void writeArrayEnd() {
        out_.writeByte(0);
    }

    void writeMapBlock(int64_t size) {
        this->writeValue(static_cast<int64_t>(size));
    }

    void writeMapEnd() {
        out_.writeByte(0);
    }

    void writeUnion(int64_t choice) {
        this->writeValue(static_cast<int64_t>(choice));
    }

    void writeEnum(int64_t choice) {
        this->writeValue(static_cast<int64_t>(choice));
    }

  private:

    OutputStreamer &out_;

};

} // namespace avro

#endif
