// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;


public class ColumnDataTypeFactory
{
    private static final String DATA_TYPE_ROOT_PACKAGE = "meta::relational::metamodel::datatype::";
    private static final ImmutableMap<String, String> COLUMN_TYPE_MAP = buildTypeMap();
    private static final ImmutableMap<String, ImmutableList<String>> TYPE_PARAM_MAP = pureDataTypeConstructorParameterMap();

    private ColumnDataTypeFactory()
    {
        // Utility class
    }

    public static String pureDataTypeConstructorString(final String typeName, final String... paramValues) throws ColumnDataTypeException
    {
        String pureTypeName = getPureTypeName(typeName);
        if (pureTypeName == null)
        {
            throw new ColumnDataTypeException(String.format("Unknown column data type '%s'", typeName));
        }
        else
        {
            ListIterable<String> constructorParamNames = TYPE_PARAM_MAP.get(pureTypeName);
            constructorParamNames = constructorParamNames == null ? Lists.immutable.empty() : constructorParamNames;
            final int conParamLength = constructorParamNames.size();
            if (conParamLength == paramValues.length)
            {
                MutableList<String> conParamsAndValues = FastList.newList();

                if (paramValues.length == 1)
                {
                    conParamsAndValues.add(String.format("%s=%s", constructorParamNames.get(0), paramValues[0]));
                }
                else if (paramValues.length == 2)
                {
                    conParamsAndValues.add(String.format("%s=%s", constructorParamNames.get(0), paramValues[0]));
                    conParamsAndValues.add(String.format("%s=%s", constructorParamNames.get(1), paramValues[1]));
                }

                return String.format("^%s%s(%s)", DATA_TYPE_ROOT_PACKAGE, pureTypeName, conParamsAndValues.makeString(","));
            }
            else
            {
                //System.out.println("ColumnDataTypeException --> "+sourceName+" "+typeName.beginLine+" "+typeName.toString());
                //return String.format("^%s%s()", DATA_TYPE_ROOT_PACKAGE, pureTypeName);
                throw new ColumnDataTypeException(String.format("The data type %s requires %d values but %d were provided", typeName, conParamLength, paramValues.length));
            }
        }

    }

    public static String getPureTypeName(String typeName)
    {
        return COLUMN_TYPE_MAP.get(typeName.toUpperCase());
    }

    private static ImmutableMap<String, String> buildTypeMap()
    {
        MutableMap<String, String> map = Maps.mutable.empty();

        map.put("FLOAT", "Float");
        map.put("DOUBLE", "Double");

        map.put("INT", "Integer");
        map.put("INTEGER", "Integer");
        map.put("BIGINT", "BigInt");
        map.put("SMALLINT", "SmallInt");
        map.put("TINYINT", "TinyInt");

        map.put("CHAR", "Char");
        map.put("VARCHAR", "Varchar");

        map.put("BINARY", "Binary");
        map.put("VARBINARY", "Varbinary");

        map.put("TIMESTAMP", "Timestamp");
        map.put("DATE", "Date");

        map.put("DECIMAL", "Decimal");
        map.put("NUMERIC", "Numeric");

        map.put("DISTINCT", "Distinct");
        map.put("OTHER", "Other");
        map.put("BIT", "Bit");
        map.put("REAL", "Real");
        map.put("ARRAY", "Array");

        return map.toImmutable();
    }


    private static ImmutableMap<String, ImmutableList<String>> pureDataTypeConstructorParameterMap()
    {
        MutableMap<String, ImmutableList<String>> map = Maps.mutable.empty();

        map.put("Varchar", Lists.immutable.with("size"));
        map.put("Char", Lists.immutable.with("size"));

        map.put("Decimal", Lists.immutable.with("precision", "scale"));
        map.put("Numeric", Lists.immutable.with("precision", "scale"));

        map.put("Binary", Lists.immutable.with("size"));
        map.put("Varbinary", Lists.immutable.with("size"));

        return map.toImmutable();
    }


    public static class ColumnDataTypeException extends Exception
    {

        public ColumnDataTypeException(String message)
        {
            super(message);
        }
    }


}
