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

#ifndef avro_NodeImpl_hh__
#define avro_NodeImpl_hh__

#include <limits>

#include "Node.hh"
#include "NodeConcepts.hh"

namespace avro {

/// Implementation details for Node.  NodeImpl represents all the avro types,
/// whose properties are enabled are disabled by selecting concept classes.

template 
< 
    class NameConcept,
    class LeavesConcept,
    class LeafNamesConcept,
    class SizeConcept
>
class NodeImpl : public Node
{

  protected:

    NodeImpl(Type type) :
        Node(type),
        nameAttribute_(),
        leafAttributes_(),
        leafNameAttributes_(),
        sizeAttribute_()
    { }

    NodeImpl(Type type, 
             const NameConcept &name, 
             const LeavesConcept &leaves, 
             const LeafNamesConcept &leafNames,
             const SizeConcept &size) :
        Node(type),
        nameAttribute_(name),
        leafAttributes_(leaves),
        leafNameAttributes_(leafNames),
        sizeAttribute_(size)
    { }

    bool hasName() const {
        return NameConcept::hasAttribute;
    }

    void doSetName(const std::string &name) {
        nameAttribute_.add(name);
    }
    
    const std::string &name() const {
        return nameAttribute_.get();
    }

    void doAddLeaf(const NodePtr &newLeaf) { 
        leafAttributes_.add(newLeaf);
    }

    size_t leaves() const {
        return leafAttributes_.size();
    }

    const NodePtr &leafAt(int index) const { 
        return leafAttributes_.get(index);
    }

    void doAddName(const std::string &name) { 
        leafNameAttributes_.add(name);
    }

    size_t names() const {
        return leafNameAttributes_.size();
    }

    const std::string &nameAt(int index) const { 
        return leafNameAttributes_.get(index);
    }

    void doSetFixedSize(int size) {
        sizeAttribute_.add(size);
    }

    int fixedSize() const {
        return sizeAttribute_.get();
    }

    virtual bool isValid() const = 0;

    void printBasicInfo(std::ostream &os) const;

    void setLeafToSymbolic(int index);
   
    NameConcept nameAttribute_;
    LeavesConcept leafAttributes_;
    LeafNamesConcept leafNameAttributes_;
    SizeConcept sizeAttribute_;
};

typedef concepts::NoAttribute<std::string>     NoName;
typedef concepts::SingleAttribute<std::string> HasName;

typedef concepts::NoAttribute<NodePtr>      NoLeaves;
typedef concepts::SingleAttribute<NodePtr>  SingleLeaf;
typedef concepts::MultiAttribute<NodePtr>   MultiLeaves;

typedef concepts::NoAttribute<std::string>     NoLeafNames;
typedef concepts::MultiAttribute<std::string>  LeafNames;

typedef concepts::NoAttribute<int>     NoSize;
typedef concepts::SingleAttribute<int> HasSize;

typedef NodeImpl< NoName,  NoLeaves,    NoLeafNames,  NoSize  > NodeImplPrimitive;
typedef NodeImpl< HasName, NoLeaves,    NoLeafNames,  NoSize  > NodeImplSymbolic;

typedef NodeImpl< HasName, MultiLeaves, LeafNames,    NoSize  > NodeImplRecord;
typedef NodeImpl< HasName, NoLeaves,    LeafNames,    NoSize  > NodeImplEnum;
typedef NodeImpl< NoName,  SingleLeaf,  NoLeafNames,  NoSize  > NodeImplArray;
typedef NodeImpl< NoName,  MultiLeaves, NoLeafNames,  NoSize  > NodeImplMap;
typedef NodeImpl< NoName,  MultiLeaves, NoLeafNames,  NoSize  > NodeImplUnion;
typedef NodeImpl< HasName, NoLeaves,    NoLeafNames,  HasSize > NodeImplFixed;

class NodePrimitive : public NodeImplPrimitive
{
  public:

    explicit NodePrimitive(Type type) :
        NodeImplPrimitive(type)
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return true;
    }
};

class NodeSymbolic : public NodeImplSymbolic
{
  public:

    NodeSymbolic() :
        NodeImplSymbolic(AVRO_SYMBOLIC)
    { }

    explicit NodeSymbolic(const HasName &name) :
        NodeImplSymbolic(AVRO_SYMBOLIC, name, NoLeaves(), NoLeafNames(), NoSize())
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (nameAttribute_.size() == 1);
    }

};

class NodeRecord : public NodeImplRecord
{
  public:

    NodeRecord() :
        NodeImplRecord(AVRO_RECORD) 
    { }

    NodeRecord(const HasName &name, const MultiLeaves &fields, const LeafNames &fieldsNames) :
        NodeImplRecord(AVRO_RECORD, name, fields, fieldsNames, NoSize())
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (
                (nameAttribute_.size() == 1) && 
                (leafAttributes_.size() > 0) &&
                (leafAttributes_.size() == leafNameAttributes_.size())
               );
    }
};

class NodeEnum : public NodeImplEnum
{
  public:

    NodeEnum() :
        NodeImplEnum(AVRO_ENUM) 
    { }

    NodeEnum(const HasName &name, const LeafNames &symbols) :
        NodeImplEnum(AVRO_ENUM, name, NoLeaves(), symbols, NoSize())
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (
                (nameAttribute_.size() == 1) && 
                (leafNameAttributes_.size() > 0) 
               );
    }
};

class NodeArray : public NodeImplArray
{
  public:

    NodeArray() :
        NodeImplArray(AVRO_ARRAY)
    { }

    explicit NodeArray(const SingleLeaf &items) :
        NodeImplArray(AVRO_ARRAY, NoName(), items, NoLeafNames(), NoSize())
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (leafAttributes_.size() == 1);
    }
};

class NodeMap : public NodeImplMap
{
  public:

    NodeMap() :
        NodeImplMap(AVRO_MAP)
    { 
         NodePtr key(new NodePrimitive(AVRO_STRING));
         doAddLeaf(key);
    }

    explicit NodeMap(const SingleLeaf &values) :
        NodeImplMap(AVRO_MAP, NoName(), values, NoLeafNames(), NoSize())
    { 
        // need to add the key for the map too
        NodePtr key(new NodePrimitive(AVRO_STRING));
        doAddLeaf(key);

        // key goes before value
        std::swap(leafAttributes_.get(0), leafAttributes_.get(1));
    }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (leafAttributes_.size() == 2);
    }
};

class NodeUnion : public NodeImplUnion
{
  public:

    NodeUnion() :
        NodeImplUnion(AVRO_UNION)
    { }

    explicit NodeUnion(const MultiLeaves &types) :
        NodeImplUnion(AVRO_UNION, NoName(), types, NoLeafNames(), NoSize())
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (leafAttributes_.size() > 1);
    }
};

class NodeFixed : public NodeImplFixed
{
  public:

    NodeFixed() :
        NodeImplFixed(AVRO_FIXED)
    { }

    NodeFixed(const HasName &name, const HasSize &size) :
        NodeImplFixed(AVRO_FIXED, name, NoLeaves(), NoLeafNames(), size)
    { }

    void printJson(std::ostream &os, int depth) const;

    bool isValid() const {
        return (
                (nameAttribute_.size() == 1) && 
                (sizeAttribute_.size() == 1) 
               );
    }
};

template < class A, class B, class C, class D >
inline void 
NodeImpl<A,B,C,D>::setLeafToSymbolic(int index)
{
    if(!B::hasAttribute) {
        throw Exception("Cannot change leaf node for nonexistent leaf");
    } 
    NodePtr symbol(new NodeSymbolic);

    NodePtr &node = const_cast<NodePtr &>(leafAttributes_.get(index));
    symbol->setName(node->name());
    node = symbol;
}

template < class A, class B, class C, class D >
inline void 
NodeImpl<A,B,C,D>::printBasicInfo(std::ostream &os) const
{
    os << type();
    if(hasName()) {
        os << " " << nameAttribute_.get();
    }
    if(D::hasAttribute) {
        os << " " << sizeAttribute_.get();
    }
    os << '\n';
    int count = leaves();
    count = count ? count : names();
    for(int i= 0; i < count; ++i) {
        if( C::hasAttribute ) {
            os << "name " << nameAt(i) << '\n';
        }
        if( leafAttributes_.hasAttribute) {
            leafAt(i)->printBasicInfo(os);
        }
    }
    if(isCompound(type())) {
        os << "end " << type() << '\n';
    }
}

} // namespace avro

#endif
