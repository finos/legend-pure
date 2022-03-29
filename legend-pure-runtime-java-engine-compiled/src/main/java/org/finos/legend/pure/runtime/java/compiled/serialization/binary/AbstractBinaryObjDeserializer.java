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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;

import java.math.BigDecimal;

abstract class AbstractBinaryObjDeserializer implements BinaryObjDeserializer
{
    @Override
    public Obj deserialize(Reader reader)
    {
        byte code = reader.readByte();
        boolean isEnum = BinaryGraphSerializationTypes.isEnum(code);
        String classifier = readClassifier(reader);
        String identifier = readIdentifier(reader);
        String name = BinaryGraphSerializationTypes.hasName(code) ? readName(reader) : null;
        SourceInformation sourceInformation = BinaryGraphSerializationTypes.hasSourceInfo(code) ? readSourceInformation(reader) : null;
        ListIterable<PropertyValue> propertiesList = readPropertyValues(reader);
        return Obj.newObj(classifier, identifier, name, propertiesList, sourceInformation, isEnum);
    }

    protected SourceInformation readSourceInformation(Reader reader)
    {
        String sourceId = readString(reader);
        int startLine = reader.readInt();
        int startColumn = reader.readInt();
        int line = reader.readInt();
        int column = reader.readInt();
        int endLine = reader.readInt();
        int endColumn = reader.readInt();
        return new SourceInformation(sourceId, startLine, startColumn, line, column, endLine, endColumn);
    }

    protected String readIdentifier(Reader reader)
    {
        return readString(reader);
    }

    protected String readClassifier(Reader reader)
    {
        return readString(reader);
    }

    protected String readName(Reader reader)
    {
        return readString(reader);
    }

    protected ListIterable<PropertyValue> readPropertyValues(Reader reader)
    {
        int propertiesSize = reader.readInt();
        switch (propertiesSize)
        {
            case 0:
            {
                return Lists.immutable.empty();
            }
            case 1:
            {
                PropertyValue value = readPropertyValue(reader);
                return Lists.immutable.with(value);
            }
            case 2:
            {
                PropertyValue value1 = readPropertyValue(reader);
                PropertyValue value2 = readPropertyValue(reader);
                return Lists.immutable.with(value1, value2);
            }
            default:
            {
                MutableList<PropertyValue> propertiesList = Lists.mutable.withInitialCapacity(propertiesSize);
                for (int i = 0; i < propertiesSize; i++)
                {
                    PropertyValue value = readPropertyValue(reader);
                    propertiesList.add(value);
                }
                return propertiesList;
            }
        }
    }

    protected PropertyValue readPropertyValue(Reader reader)
    {
        boolean isMany = reader.readBoolean();
        String propertyName = readString(reader);
        return isMany ? new PropertyValueMany(propertyName, readManyRValues(reader)) : new PropertyValueOne(propertyName, readRValue(reader));
    }

    private ListIterable<RValue> readManyRValues(Reader reader)
    {
        int valueCount = reader.readInt();
        switch (valueCount)
        {
            case 0:
            {
                return Lists.immutable.empty();
            }
            case 1:
            {
                RValue value = readRValue(reader);
                return Lists.immutable.with(value);
            }
            case 2:
            {
                RValue value1 = readRValue(reader);
                RValue value2 = readRValue(reader);
                return Lists.immutable.with(value1, value2);
            }
            default:
            {
                MutableList<RValue> valuesList = Lists.mutable.withInitialCapacity(valueCount);
                for (int i = 0; i < valueCount; i++)
                {
                    RValue value = readRValue(reader);
                    valuesList.add(value);
                }
                return valuesList;
            }
        }
    }

    protected RValue readRValue(Reader reader)
    {
        byte valueType = reader.readByte();
        switch (valueType)
        {
            case BinaryGraphSerializationTypes.OBJ_REF:
            {
                return new ObjRef(readString(reader), readString(reader));
            }
            case BinaryGraphSerializationTypes.ENUM_REF:
            {
                return new EnumRef(readString(reader), readString(reader));
            }
            case BinaryGraphSerializationTypes.PRIMITIVE_BOOLEAN:
            {
                return new Primitive(reader.readBoolean());
            }
            case BinaryGraphSerializationTypes.PRIMITIVE_DOUBLE:
            {
                return new Primitive(reader.readDouble());
            }
            case BinaryGraphSerializationTypes.PRIMITIVE_LONG:
            {
                return new Primitive(reader.readLong());
            }
            case BinaryGraphSerializationTypes.PRIMITIVE_STRING:
            {
                return new Primitive(readString(reader));
            }
            case BinaryGraphSerializationTypes.PRIMITIVE_DATE:
            {
                String dateString = readString(reader);
                return new Primitive(LatestDate.isLatestDateString(dateString) ? LatestDate.instance : DateFunctions.parsePureDate(dateString));
            }
            case BinaryGraphSerializationTypes.PRIMITIVE_DECIMAL:
            {
                String decimalString = reader.readString();
                return new Primitive(new BigDecimal(decimalString));
            }
            default:
            {
                throw new UnsupportedOperationException("serialization for RValue type not supported: " + valueType);
            }
        }
    }

    protected abstract String readString(Reader reader);
}
