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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class LoadValuesToDbTableNew extends NativeFunction
{
    private final ModelRepository repository;
    private final Message message;


    public LoadValuesToDbTableNew(ModelRepository repository, Message message)
    {
        this.repository = repository;
        this.message = message;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance tableData = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance table = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance connectionInformation = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);

        ListIterable<? extends CoreInstance> columns = LoadCsvToDbTable.getColumns(table, processorSupport);
        ListIterable<String> columnTypes = LoadCsvToDbTable.getColumnTypes(table, processorSupport);

        ListIterable<ListIterable<String>> rows = this.getRows(tableData, processorSupport);
        Iterable<ListIterable<?>> tableIterable = this.getTableIterable(rows, table.getValueForMetaPropertyToOne(M3Properties.name).getName(), columns, columnTypes);

        new ExecuteInDb(this.repository, this.message, 0).bulkInsertInDb(connectionInformation, table, tableIterable, functionExpressionToUseInStack, processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);

    }

    private ListIterable<ListIterable<String>> getRows(CoreInstance tableDataInstance, ProcessorSupport processorSupport)
    {

        MutableList<ListIterable<String>> rows = FastList.newList();
        RichIterable<? extends CoreInstance> tableData = Instance.getValueForMetaPropertyToManyResolved(tableDataInstance, M3Properties.values, processorSupport);
        for (CoreInstance row : tableData)
        {
            Iterable<? extends CoreInstance> cells = Instance.getValueForMetaPropertyToManyResolved(row, M3Properties.values, processorSupport);
            MutableList<String> rowIterable = FastList.newList();
            for (CoreInstance cell : cells)
            {
                rowIterable.add(cell.getName());
            }
            rows.add(rowIterable);
        }

        return rows;
    }

    private Iterable<ListIterable<?>> getTableIterable(ListIterable<ListIterable<String>> table, final String tableName, final ListIterable<? extends CoreInstance> columns, final ListIterable<String> columnTypes)
    {
        return LoadCsvToDbTable.collectIterable(LazyIterate.drop(table, 3), "", tableName, columns, columnTypes);
    }
}