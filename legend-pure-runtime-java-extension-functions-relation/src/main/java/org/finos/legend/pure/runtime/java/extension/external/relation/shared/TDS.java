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

package org.finos.legend.pure.runtime.java.extension.external.relation.shared;

import io.deephaven.csv.parsers.DataType;
import io.deephaven.csv.reading.CsvReader;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap.*;

public class TDS
{
    private MutableMap<String, Object> dataByColumnName = Maps.mutable.empty();
    private MutableMap<String, DataType> columnType = Maps.mutable.empty();
    private long rowCount;
    private ModelRepository modelRepository;
    private ProcessorSupport processorSupport;

    public TDS(CsvReader.Result result, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        this.rowCount = result.numRows();
        this.modelRepository = modelRepository;
        this.processorSupport = processorSupport;
        ArrayIterate.forEach(result.columns(), c ->
        {
            columnType.put(c.name(), c.dataType());
            dataByColumnName.put(c.name(), c.data());
        });
    }

    private TDS()
    {
    }

    public TDS copy()
    {
        TDS result = new TDS();
        result.rowCount = rowCount;
        result.modelRepository = modelRepository;
        result.processorSupport = processorSupport;
        result.columnType = Maps.mutable.withMap(columnType);
        result.dataByColumnName = Maps.mutable.empty();
        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = dataByColumnName.get(columnName);
            Object copy;
            switch (columnType.get(columnName))
            {
                case INT:
                {
                    int[] data = (int[]) dataAsObject;
                    copy = Arrays.copyOf(data, data.length);
                    break;
                }
                case CHAR:
                {
                    char[] data = (char[]) dataAsObject;
                    copy = Arrays.copyOf(data, data.length);
                    break;
                }
                case STRING:
                {
                    String[] data = (String[]) dataAsObject;
                    copy = Arrays.copyOf(data, data.length);
                    break;
                }
                case DOUBLE:
                {
                    double[] data = (double[]) dataAsObject;
                    copy = Arrays.copyOf(data, data.length);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(columnName));
            }
            result.dataByColumnName.put(columnName, copy);
        });
        return result;
    }

    public TDS drop(IntSet rows)
    {
        int size = rows.size();
        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = dataByColumnName.get(columnName);
            switch (columnType.get(columnName))
            {
                case INT:
                {
                    int[] src = (int[]) dataAsObject;
                    int[] target = new int[(int) rowCount - size];
                    int j = 0;
                    for (int i = 0; i < rowCount; i++)
                    {
                        if (!rows.contains(i))
                        {
                            target[j++] = src[i];
                        }
                    }
                    dataByColumnName.put(columnName, target);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(columnName));
            }
        });
        this.rowCount = rowCount - size;
        return this;
    }

    public CoreInstance getValueAsCoreInstance(String columnName, int rowNum)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        CoreInstance result;
        switch (columnType.get(columnName))
        {
            case INT:
            {
                int[] data = (int[]) dataAsObject;
                result = newIntegerLiteral(modelRepository, data[rowNum], processorSupport);
                break;
            }
            case CHAR:
            {
                char[] data = (char[]) dataAsObject;
                result = newStringLiteral(modelRepository, "" + data[rowNum], processorSupport);
                break;
            }
            case STRING:
            {
                String[] data = (String[]) dataAsObject;
                result = newStringLiteral(modelRepository, data[rowNum], processorSupport);
                break;
            }
            case DOUBLE:
            {
                double[] data = (double[]) dataAsObject;
                result = newFloatLiteral(modelRepository, BigDecimal.valueOf(data[rowNum]), processorSupport);
                break;
            }
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName));
        }
        return result;
    }

    public long getRowCount()
    {
        return rowCount;
    }
}
