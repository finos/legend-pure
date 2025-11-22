// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.element.v1;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.element.PropertyValues;
import org.finos.legend.pure.m3.serialization.compiler.element.Reference;
import org.finos.legend.pure.m3.serialization.compiler.element.Value;
import org.finos.legend.pure.m3.serialization.compiler.element.ValueOrReference;
import org.finos.legend.pure.m3.serialization.compiler.strings.StringIndexer;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.serialization.Reader;

import java.math.BigInteger;

public class DeserializerV1 extends BaseV1
{
    DeserializerV1()
    {
    }

    DeserializedConcreteElement deserialize(Reader reader, StringIndexer stringIndexer, int referenceIdVersion)
    {
        Reader stringIndexedReader = stringIndexer.readStringIndex(reader);
        String path = stringIndexedReader.readString();
        InstanceData[] nodes = deserializeNodes(stringIndexedReader);
        return DeserializedConcreteElement.newDeserializedConcreteElement(path, referenceIdVersion, Lists.immutable.with(nodes));
    }

    private InstanceData[] deserializeNodes(Reader reader)
    {
        String sourceId = reader.readString();
        int compileStateBitSetWidth = reader.readByte();
        int nodeCount = reader.readInt();
        int internalIdWidth = getIntWidth(nodeCount);
        InstanceData[] nodes = new InstanceData[nodeCount];
        for (int i = 0; i < nodeCount; i++)
        {
            nodes[i] = deserializeNode(reader, sourceId, internalIdWidth, compileStateBitSetWidth);
        }
        return nodes;
    }

    private InstanceData deserializeNode(Reader reader, String sourceId, int internalIdWidth, int compileStateBitSetWidth)
    {
        String name = readName(reader);
        String classifierId = readClassifier(reader);
        SourceInformation sourceInfo = readSourceInfo(reader, sourceId);
        String referenceId = readReferenceId(reader);
        int compileStateBitSet = readCompileStateBitSet(reader, compileStateBitSetWidth);
        int propertyCount = reader.readInt();
        MutableList<PropertyValues> propertiesWithValues = Lists.mutable.ofInitialCapacity(propertyCount);
        for (int i = 0; i < propertyCount; i++)
        {
            propertiesWithValues.add(readPropertyWithValues(reader, internalIdWidth));
        }
        return InstanceData.newInstanceData(name, classifierId, sourceInfo, referenceId, compileStateBitSet, propertiesWithValues.asUnmodifiable());
    }

    private String readName(Reader reader)
    {
        boolean hasName = reader.readBoolean();
        return hasName ? reader.readString() : null;
    }

    private String readClassifier(Reader reader)
    {
        return reader.readString();
    }

    private SourceInformation readSourceInfo(Reader reader, String sourceId)
    {
        int code = reader.readByte();
        switch (code & VALUE_PRESENT_MASK)
        {
            case VALUE_NOT_PRESENT:
            {
                return null;
            }
            case VALUE_PRESENT:
            {
                int startLine = readIntOfWidth(reader, code);
                int startCol = readIntOfWidth(reader, code);
                int line = readIntOfWidth(reader, code);
                int col = readIntOfWidth(reader, code);
                int endLine = readIntOfWidth(reader, code);
                int endCol = readIntOfWidth(reader, code);
                return new SourceInformation(sourceId, startLine, startCol, line, col, endLine, endCol);
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown value present code: %02x", code & NODE_TYPE_MASK));
            }
        }
    }

    private String readReferenceId(Reader reader)
    {
        int code = reader.readByte();
        switch (code & VALUE_PRESENT_MASK)
        {
            case VALUE_NOT_PRESENT:
            {
                return null;
            }
            case VALUE_PRESENT:
            {
                return reader.readString();
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown value present code: %02x", code & NODE_TYPE_MASK));
            }
        }
    }

    private int readCompileStateBitSet(Reader reader, int compileStateBitSetWidth)
    {
        return readIntOfWidth(reader, compileStateBitSetWidth);
    }

    private PropertyValues readPropertyWithValues(Reader reader, int internalIdWidth)
    {
        String name = reader.readString();
        String sourceType = reader.readString();
        int valueCount = reader.readInt();
        if (valueCount == 1)
        {
            ValueOrReference value = readPropertyValue(reader, internalIdWidth);
            return PropertyValues.newPropertyValues(name, sourceType, value);
        }
        MutableList<ValueOrReference> values = Lists.mutable.ofInitialCapacity(valueCount);
        for (int i = 0; i < valueCount; i++)
        {
            values.add(readPropertyValue(reader, internalIdWidth));
        }
        return PropertyValues.newPropertyValues(name, sourceType, values.asUnmodifiable());
    }

    private ValueOrReference readPropertyValue(Reader reader, int internalIdWidth)
    {
        int code = reader.readByte();
        switch (code & NODE_TYPE_MASK)
        {
            case INTERNAL_REFERENCE:
            {
                int id = readIntOfWidth(reader, internalIdWidth);
                return Reference.newInternalReference(id);
            }
            case EXTERNAL_REFERENCE:
            {
                String id = reader.readString();
                return Reference.newExternalReference(id);
            }
            case BOOLEAN:
            {
                boolean value = deserializeBoolean(code);
                return Value.newBooleanValue(value);
            }
            case BYTE:
            {
                byte value = deserializeByte(reader);
                return Value.newByteValue(value);
            }
            case DATE:
            {
                switch (code & DATE_TYPE_MASK)
                {
                    case DATE_TYPE:
                    {
                        PureDate value = deserializeDate(reader, code);
                        return Value.newDateValue(value);
                    }
                    case DATE_TIME_TYPE:
                    {
                        PureDate value = deserializeDate(reader, code);
                        return Value.newDateTimeValue(value);
                    }
                    case STRICT_DATE_TYPE:
                    {
                        PureDate value = deserializeDate(reader, code);
                        return Value.newStrictDateValue(value);
                    }
                    case LATEST_DATE_TYPE:
                    {
                        return Value.newLatestDateValue();
                    }
                    default:
                    {
                        throw new RuntimeException(String.format("Unknown date type code: %02x", code & DATE_TYPE_MASK));
                    }
                }
            }
            case NUMBER:
            {
                switch (code & NUMBER_TYPE_MASK)
                {
                    case DECIMAL_TYPE:
                    {
                        String value = deserializeDecimal(reader, code);
                        return Value.newDecimalValue(value);
                    }
                    case FLOAT_TYPE:
                    {
                        String value = deserializeFloat(reader, code);
                        return Value.newFloatValue(value);
                    }
                    case INTEGER_TYPE:
                    {
                        if (((code & INTEGRAL_WIDTH_MASK) == LONG_WIDTH))
                        {
                            long value = deserializeOrdinaryIntegerAsLong(reader, code);
                            return Value.newIntegerValue(value);
                        }
                        else
                        {
                            int value = deserializeOrdinaryIntegerAsInt(reader, code);
                            return Value.newIntegerValue(value);
                        }
                    }
                    case BIG_INTEGER_TYPE:
                    {
                        BigInteger value = deserializeBigInteger(reader, code);
                        return Value.newIntegerValue(value);
                    }
                    default:
                    {
                        throw new RuntimeException(String.format("Unknown number type code: %02x", code & NUMBER_TYPE_MASK));
                    }
                }
            }
            case STRICT_TIME:
            {
                PureStrictTime value = deserializeStrictTime(reader, code);
                return Value.newStrictTimeValue(value);
            }
            case STRING:
            {
                String value = deserializeString(reader);
                return Value.newStringValue(value);
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown node type code: %02x", code & NODE_TYPE_MASK));
            }
        }
    }
}
