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
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap.*;

public class TestTDS
{
    private MutableMap<String, Object> dataByColumnName = Maps.mutable.empty();
    private MutableMap<String, DataType> columnType = Maps.mutable.empty();
    private long rowCount;
    private ModelRepository modelRepository;
    private ProcessorSupport processorSupport;

    public TestTDS(CsvReader.Result result, ModelRepository modelRepository, ProcessorSupport processorSupport)
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

    private TestTDS()
    {
    }

    public TestTDS copy()
    {
        TestTDS result = new TestTDS();
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
                    throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in copy!");
            }
            result.dataByColumnName.put(columnName, copy);
        });
        return result;
    }

    public TestTDS drop(IntSet rows)
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
                case CHAR:
                {
                    char[] src = (char[]) dataAsObject;
                    char[] target = new char[(int) rowCount - size];
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
                case STRING:
                {
                    String[] src = (String[]) dataAsObject;
                    String[] target = new String[(int) rowCount - size];
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
                case DOUBLE:
                {
                    double[] src = (double[]) dataAsObject;
                    double[] target = new double[(int) rowCount - size];
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
                    throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in drop!");
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
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in getValue");
        }
        return result;
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public TestTDS concatenate(TestTDS tds2)
    {
        TestTDS result = new TestTDS();
        result.modelRepository = modelRepository;
        result.processorSupport = processorSupport;
        result.rowCount = this.rowCount + tds2.rowCount;
        result.columnType = Maps.mutable.withMap(columnType);
        result.dataByColumnName = Maps.mutable.empty();
        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject1 = dataByColumnName.get(columnName);
            Object dataAsObject2 = tds2.dataByColumnName.get(columnName);
            Object copy;
            switch (columnType.get(columnName))
            {
                case INT:
                {
                    int[] data1 = (int[]) dataAsObject1;
                    int[] data2 = (int[]) dataAsObject2;
                    int[] _copy = Arrays.copyOf(data1, (int) result.rowCount);
                    System.arraycopy(data2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    break;
                }
                case CHAR:
                {
                    char[] data1 = (char[]) dataAsObject1;
                    char[] data2 = (char[]) dataAsObject2;
                    char[] _copy = Arrays.copyOf(data1, (int) result.rowCount);
                    System.arraycopy(data2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    break;
                }
                case STRING:
                {
                    String[] data1 = (String[]) dataAsObject1;
                    String[] data2 = (String[]) dataAsObject2;
                    String[] _copy = Arrays.copyOf(data1, (int) result.rowCount);
                    System.arraycopy(data2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    break;
                }
                case DOUBLE:
                {
                    double[] data1 = (double[]) dataAsObject1;
                    double[] data2 = (double[]) dataAsObject2;
                    double[] _copy = Arrays.copyOf(data1, (int) result.rowCount);
                    System.arraycopy(data2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in copy!");
            }
            result.dataByColumnName.put(columnName, copy);
        });
        return result;
    }

    public TestTDS addColumn(String name, DataType dataType, Object res)
    {
        this.dataByColumnName.put(name, res);
        this.columnType.put(name, dataType);
        return this;
    }

    public TestTDS rename(String oldName, String newName)
    {
        DataType type = this.columnType.get(oldName);
        Object data = this.dataByColumnName.get(oldName);
        this.columnType.put(newName, type);
        this.dataByColumnName.put(newName, data);
        this.columnType.remove(oldName);
        this.dataByColumnName.remove(oldName);
        return this;
    }

    public TestTDS slice(int from, int to)
    {
        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = dataByColumnName.get(columnName);
            switch (columnType.get(columnName))
            {
                case INT:
                {
                    int[] src = (int[]) dataAsObject;
                    int[] target = Arrays.copyOfRange(src, from, to);
                    dataByColumnName.put(columnName, target);
                    break;
                }
                case CHAR:
                {
                    char[] src = (char[]) dataAsObject;
                    char[] target = Arrays.copyOfRange(src, from, to);
                    dataByColumnName.put(columnName, target);
                    break;
                }
                case STRING:
                {
                    String[] src = (String[]) dataAsObject;
                    String[] target = Arrays.copyOfRange(src, from, to);
                    dataByColumnName.put(columnName, target);
                    break;
                }
                case DOUBLE:
                {
                    double[] src = (double[]) dataAsObject;
                    double[] target = Arrays.copyOfRange(src, from, to);
                    dataByColumnName.put(columnName, target);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in drop!");
            }
        });
        this.rowCount = to - from;
        return this;
    }
}
