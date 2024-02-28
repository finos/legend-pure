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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListenerState;
import org.finos.legend.pure.runtime.java.shared.listeners.IdentifableExecutionEndListner;

import java.util.Stack;

public class CreateTempTable extends NativeFunction
{
    private ModelRepository repository;
    private FunctionExecutionInterpreted functionExecution;
    private final Message message;

    public CreateTempTable(ModelRepository repository, FunctionExecutionInterpreted functionExecution, Message message)
    {
        this.repository = repository;
        this.functionExecution = functionExecution;
        this.message = message;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance tableName = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        ListIterable<? extends CoreInstance> columns = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance toSql = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        CoreInstance connection;
        CoreInstance relyOnFinallyForCleanup = null;
        if (params.size() == 5)
        {
            relyOnFinallyForCleanup = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
            connection = Instance.getValueForMetaPropertyToOneResolved(params.get(4), M3Properties.values, processorSupport);
        }
        else
        {
            connection = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        }
        CoreInstance dbType = Instance.getValueForMetaPropertyToOneResolved(connection, "type", processorSupport);

        CoreInstance sql = this.functionExecution.executeLambdaFromNative(toSql, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(tableName, false, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(columns, false, processorSupport), ValueSpecificationBootstrap.wrapValueSpecification(dbType, false, processorSupport)), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
        String sqlStr = Instance.getValueForMetaPropertyToOneResolved(sql, M3Properties.values, processorSupport).getName();

        final ExecuteInDb executeInDb = new ExecuteInDb(this.repository, this.message, 0);
        executeInDb.executeInDb(connection, sqlStr, 0, 0, functionExpressionToUseInStack, processorSupport);

        executionSupport.registerIdentifableExecutionEndListener(new TempTableCleanup(tableName.getName(), relyOnFinallyForCleanup, executeInDb, connection, functionExpressionToUseInStack, processorSupport));

        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);
    }

    private static class TempTableCleanup implements IdentifableExecutionEndListner
    {
        private String tableName;
        private Function0<Void> cleanUp;
        private static String dropTableStmt = "drop table ";
        private boolean relyOnFinallyForCleanup;

        public TempTableCleanup(String tableName, CoreInstance relyOnFinallyForCleanup, final ExecuteInDb executeInDb, final CoreInstance connection, final CoreInstance functionExpressionToUseInStack, final ProcessorSupport processorSupport)
        {
            this.tableName = tableName;
            this.cleanUp = getCleanUp(executeInDb, connection, dropTableStmt + tableName, functionExpressionToUseInStack, processorSupport);
            this.relyOnFinallyForCleanup = relyOnFinallyForCleanup == null ? false : PrimitiveUtilities.getBooleanValue(relyOnFinallyForCleanup);
            ;
        }

        private Function0 getCleanUp(final ExecuteInDb executeInDb, final CoreInstance connection, final String sqlStr, final CoreInstance functionExpressionToUseInStack, final ProcessorSupport processorSupport)
        {
            return new Function0<Void>()
            {
                @Override
                public Void value()
                {
                    executeInDb.executeInDb(connection, sqlStr, 0, 0, functionExpressionToUseInStack, processorSupport);
                    return null;
                }
            };
        }

        @Override
        public ExecutionEndListenerState executionEnd(Exception endException)
        {
            cleanUp.value();
            if (endException == null)
            {
                return relyOnFinallyForCleanup ? new ExecutionEndListenerState(false) : new ExecutionEndListenerState(true, "Temporary table: " + this.tableName + " should be dropped explicitly");
            }
            else
            {
                return new ExecutionEndListenerState(false);
            }
        }

        private boolean isRelyOnFinallyForCleanup()
        {
            return relyOnFinallyForCleanup;
        }

        @Override
        public String getId()
        {
            return this.tableName;
        }
    }

}
