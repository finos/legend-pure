// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;

import java.math.BigDecimal;

abstract class AbstractBinaryObjSerializer implements BinaryObjSerializer
{
    @Override
    public void serializeObj(Writer writer, Obj obj)
    {
        writer.writeBoolean(obj.isEnum());
        writeSourceInformation(writer, obj.getSourceInformation());
        writeIdentifier(writer, obj.getIdentifier());
        writeClassifier(writer, obj.getClassifier());
        writeName(writer, obj.getName());
        writePropertyValues(writer, obj);
    }

    protected void writeSourceInformation(Writer writer, SourceInformation sourceInformation)
    {
        if (sourceInformation == null)
        {
            writer.writeBoolean(false);
        }
        else
        {
            writer.writeBoolean(true);
            writeString(writer, sourceInformation.getSourceId());
            writer.writeInt(sourceInformation.getStartLine());
            writer.writeInt(sourceInformation.getStartColumn());
            writer.writeInt(sourceInformation.getLine());
            writer.writeInt(sourceInformation.getColumn());
            writer.writeInt(sourceInformation.getEndLine());
            writer.writeInt(sourceInformation.getEndColumn());
        }
    }

    protected void writeIdentifier(Writer writer, String identifier)
    {
        writeString(writer, identifier);
    }

    protected void writeClassifier(Writer writer, String classifier)
    {
        writeString(writer, classifier);
    }

    protected void writeName(Writer writer, String name)
    {
        writeString(writer, name);
    }

    protected void writePropertyValues(Writer writer, Obj obj)
    {
        ListIterable<PropertyValue> propertyValues = obj.getPropertyValues();
        writer.writeInt(propertyValues.size());
        for (PropertyValue propertyValue : propertyValues)
        {
            writePropertyValue(writer, propertyValue);
        }
    }

    protected void writePropertyValue(Writer writer, PropertyValue propertyValue)
    {
        if (propertyValue instanceof PropertyValueMany)
        {
            PropertyValueMany many = (PropertyValueMany)propertyValue;
            writer.writeBoolean(true);
            writeString(writer, many.getProperty());
            ListIterable<RValue> values = many.getValues();
            writer.writeInt(values.size());
            for (RValue rValue : values)
            {
                writeRValue(writer, rValue);
            }
        }
        else
        {
            PropertyValueOne propertyValueOne = (PropertyValueOne)propertyValue;
            writer.writeBoolean(false);
            writeString(writer, propertyValueOne.getProperty());
            writeRValue(writer, propertyValueOne.getValue());
        }
    }

    protected void writeRValue(Writer writer, RValue rValue)
    {
        if (rValue instanceof EnumRef)
        {
            EnumRef enumeration = (EnumRef)rValue;
            writer.writeByte(BinaryGraphSerializationTypes.ENUM_REF);
            writeString(writer, enumeration.getEnumerationId());
            writeString(writer, enumeration.getEnumName());
        }
        else if (rValue instanceof ObjRef)
        {
            ObjRef objRef = (ObjRef)rValue;
            writer.writeByte(BinaryGraphSerializationTypes.OBJ_REF);
            writeString(writer, objRef.getClassifierId());
            writeString(writer, objRef.getId());
        }
        else if (rValue instanceof Primitive)
        {
            Primitive primitive = (Primitive)rValue;
            Object value = primitive.getValue();
            if (value instanceof Boolean)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_BOOLEAN);
                writer.writeBoolean((Boolean)value);
            }
            else if (value instanceof Double)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_DOUBLE);
                writer.writeDouble((Double)value);
            }
            else if (value instanceof Long)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_LONG);
                writer.writeLong((Long)value);
            }
            else if (value instanceof String)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_STRING);
                writeString(writer, (String)value);
            }
            else if (value instanceof PureDate)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_DATE);
                writeString(writer, value.toString());
            }
            else if (value instanceof BigDecimal)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_DECIMAL);
                writer.writeString(((BigDecimal)value).toPlainString());
            }
            else
            {
                throw new UnsupportedOperationException("Unsupported primitive type: " + value.getClass().getSimpleName());
            }
        }
        else
        {
            throw new UnsupportedOperationException("serialization for RValue type not supported: " + rValue.getClass().getName());
        }
    }

    protected abstract void writeString(Writer writer, String string);
}
