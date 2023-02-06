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

package org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.extension.store.relational.CsvReader;

import java.util.Stack;

public class LoadCsvToDbTable extends NativeFunction
{
    private final CodeStorage codeStorage;
    private final ModelRepository repository;
    private final Message message;

    private static final int _500_MB_SIZE_LIMIT = 500;

    public LoadCsvToDbTable(CodeStorage codeStorage, ModelRepository repository, Message message)
    {
        this.codeStorage = codeStorage;
        this.repository = repository;
        this.message = message;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        final String filePath = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
        final CoreInstance table = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance connectionInformation = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        CoreInstance numberOfRows = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);

        final ListIterable<? extends CoreInstance> columns = getColumns(table, processorSupport);
        final ListIterable<String> columnTypes = getColumnTypes(table, processorSupport);

        Iterable<ListIterable<?>> values = getCsvIterable(this.codeStorage, functionExpressionToUseInStack.getSourceInformation(), filePath, table.getValueForMetaPropertyToOne(M3Properties.name).getName(), numberOfRows, columns, columnTypes, _500_MB_SIZE_LIMIT);

        new ExecuteInDb(this.repository, this.message, 0).bulkInsertInDb(connectionInformation, table, values, functionExpressionToUseInStack, processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);
    }

    public static Iterable<ListIterable<?>> getCsvIterable(CodeStorage codeStorage, SourceInformation sourceInformation, final String filePath, final String tableName, CoreInstance numberOfRows,
                                                           final ListIterable<? extends CoreInstance> columns, final ListIterable<String> columnTypes, int sizeLimitMb)
    {
        Integer rowLimit = numberOfRows == null ? null : Integer.valueOf(numberOfRows.getName());
        return collectIterable(LazyIterate.drop(CsvReader.readCsv(codeStorage, sourceInformation, filePath, sizeLimitMb, rowLimit), 1), filePath, tableName, columns, columnTypes);
    }

    public static Iterable<ListIterable<?>> collectIterable(LazyIterable iterable, final String filePath, final String tableName, final ListIterable<? extends CoreInstance> columns, final ListIterable<String> columnTypes)
    {
        return iterable.collect(new Function<Iterable<String>, ListIterable<?>>()
        {
            @Override
            public ListIterable<?> valueOf(Iterable<String> csvRecord)
            {
                MutableList<Object> result = FastList.newList();
                int i = 0;
                try
                {
                    for (String value : csvRecord)
                    {

                        String type = columnTypes.get(i);
                        if (StringIterate.isEmpty(value))
                        {
                            result.add(null);
                        }
                        else if ("Integer".equals(type) || "SmallInt".equals(type) || "TinyInt".equals(type))
                        {
                            result.add(Integer.valueOf(value));
                        }
                        else if ("BigInt".equals(type))
                        {
                            result.add(Long.valueOf(value));
                        }
                        else if ("Double".equals(type) || "Float".equals(type) || "Numeric".equals(type))
                        {
                            result.add(Double.valueOf(value));
                        }
                        else
                        {
                            //Dates and Strings deliberately kept as strings - dates should be converted in the mapping
                            result.add(value);
                        }
                        i++;
                    }
                }
                catch (NumberFormatException ex)
                {
                    throw new PureExecutionException("Failed to load CSV file " + filePath + " into DB table " + tableName +
                            ".\n Table requires a " + columnTypes.get(i) + " for column " + columns.get(i).getValueForMetaPropertyToOne(M3Properties.name).getName() +
                            ". CSV row:" + (csvRecord instanceof CSVRecord ? ((CSVRecord)csvRecord).getRecordNumber() : "N/A") + " column:" + (i + 1) + " failed to convert to " + columnTypes.get(i) + " with error '" + ex.getMessage() + "'", ex);
                }
                return result;
            }
        });
    }

    public static ListIterable<? extends CoreInstance> getColumns(CoreInstance table, final ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToManyResolved(table, "columns", processorSupport);
    }

    public static ListIterable<String> getColumnTypes(CoreInstance table, final ProcessorSupport processorSupport)
    {
        return getColumns(table, processorSupport).collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance column)
            {
                return Instance.getValueForMetaPropertyToOneResolved(column, "type", processorSupport).getClassifier().getName();
            }
        });
    }
}


