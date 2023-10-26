// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared;

import io.deephaven.csv.parsers.DataType;
import io.deephaven.csv.reading.CsvReader;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

import java.math.BigDecimal;

public class TestTDSCompiled extends TestTDS
{
    public TestTDSCompiled()
    {
        super();
    }

    public TestTDSCompiled(CsvReader.Result result)
    {
        super(result);
    }

    public TestTDSCompiled(MutableMap<String, DataType> columnType, int rows)
    {
        super(columnType, rows);
    }

    @Override
    public TestTDS newTDS()
    {
        return new TestTDSCompiled();
    }

    @Override
    public TestTDS newTDS(MutableMap<String, DataType> columnType, int rows)
    {
        return new TestTDSCompiled(columnType, rows);
    }

    public Object getValueAsCoreInstance(String columnName, int rowNum)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        boolean[] isNull = (boolean[]) isNullByColumn.get(columnName);
        Object result;
        switch (columnType.get(columnName))
        {
            case INT:
            {
                int[] data = (int[]) dataAsObject;
                int value = data[rowNum];
                result = !isNull[rowNum] ? (long)value : null;
                break;
            }
            case CHAR:
            {
                char[] data = (char[]) dataAsObject;
                result = !isNull[rowNum] ? "" + data[rowNum] : null;
                break;
            }
            case STRING:
            {
                String[] data = (String[]) dataAsObject;
                result = data[rowNum];
                break;
            }
            case DOUBLE:
            {
                double[] data = (double[]) dataAsObject;
                result = !isNull[rowNum] ? Double.valueOf(data[rowNum]) : null;
                break;
            }
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in getValue");
        }
        return result;
    }
}
