
#include "Schema.hh"

namespace avro {

Schema::Schema() 
{ }

Schema::~Schema() 
{ }

Schema::Schema(const NodePtr &node) :
    node_(node)
{ }

Schema::Schema(Node *node) :
    node_(node)
{ }

RecordSchema::RecordSchema(const std::string &name) :
    Schema(new NodeRecord)
{
    node_->setName(name);
}

void
RecordSchema::addField(const std::string &name, const Schema &fieldSchema) 
{
    node_->addLeaf(fieldSchema.root());
    node_->addName(name);
}

EnumSchema::EnumSchema(const std::string &name) :
    Schema(new NodeEnum)
{
    node_->setName(name);
}

void
EnumSchema::addSymbol(const std::string &symbol)
{
    node_->addName(symbol);
}

ArraySchema::ArraySchema(const Schema &itemsSchema) :
    Schema(new NodeArray)
{
    node_->addLeaf(itemsSchema.root());
}

MapSchema::MapSchema(const Schema &valuesSchema) :
    Schema(new NodeMap)
{
    node_->addLeaf(valuesSchema.root());
}

UnionSchema::UnionSchema() :
    Schema(new NodeUnion)
{ }

void
UnionSchema::addType(const Schema &typeSchema) 
{
    if(typeSchema.type() == AVRO_UNION) {
        throw Exception("Cannot add unions to unions");
    }

    if(typeSchema.type() == AVRO_RECORD) {
        // check for duplicate records
        size_t types = node_->leaves();
        for(size_t i = 0; i < types; ++i) {
            const NodePtr &leaf = node_->leafAt(i);
            // TODO, more checks?
            if(leaf->type() == AVRO_RECORD && leaf->name() == typeSchema.root()->name()) {
                throw Exception("Records in unions cannot have duplicate names");
            }
        }
    }

    node_->addLeaf(typeSchema.root());
}

FixedSchema::FixedSchema(int size, const std::string &name) :
    Schema(new NodeFixed)
{
    node_->setFixedSize(size);
    node_->setName(name);
}

} // namespace avro
