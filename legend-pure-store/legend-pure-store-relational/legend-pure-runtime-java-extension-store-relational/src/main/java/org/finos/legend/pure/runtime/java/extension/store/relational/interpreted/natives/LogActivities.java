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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
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
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class LogActivities extends NativeFunction
{
    private final ExecutionActivityListener listener;

    private static class RelationalActivityProperties
    {

        static final String sql = "sql";
        static final String executionTimeInNanoSecond = "executionTimeInNanoSecond";
        static final String sqlGenerationTimeInNanoSecond = "sqlGenerationTimeInNanoSecond";
        static final String connectionAcquisitionTimeInNanoSecond = "connectionAcquisitionTimeInNanoSecond";
        static final String executionPlanInformation = "executionPlanInformation";
        static final String dataSource = "dataSource";
        static final String dataSource_type = "type";
        static final String dataSource_host = "host";
        static final String dataSource_name = "name";
        static final String dataSource_port = "port";

    }

    private static class RoutingActivityProperties
    {
        static final String routingTimeInNanoSecond = "routingTimeInNanoSecond";
    }

    public LogActivities(ExecutionActivityListener listener)
    {
        this.listener = listener;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        RichIterable<? extends CoreInstance> activities = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);

        this.handleRelationalActivities(activities, processorSupport);
        this.handleRoutingActivities(activities, processorSupport);

        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);
    }

    private void handleRelationalActivities(RichIterable<? extends CoreInstance> activities, final ProcessorSupport processorSupport) throws PureExecutionException
    {

        activities.select(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return Instance.instanceOf(coreInstance, "meta::relational::mapping::RelationalActivity", processorSupport);
            }
        }).forEach(new Procedure<CoreInstance>()
        {
            @Override
            public void value(CoreInstance relationalActivity)
            {
                final String sql = Instance.getValueForMetaPropertyToOneResolved(relationalActivity, RelationalActivityProperties.sql, processorSupport).getName();

                final CoreInstance executionTimeInNanoSecondsAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(relationalActivity, RelationalActivityProperties.executionTimeInNanoSecond, processorSupport);
                final Long executionTimeInNanoSeconds = executionTimeInNanoSecondsAsCoreInstance == null ? null : PrimitiveUtilities.getIntegerValue(executionTimeInNanoSecondsAsCoreInstance).longValue();

                final CoreInstance executionPlanInformationAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(relationalActivity, RelationalActivityProperties.executionPlanInformation, processorSupport);
                final String executionPlanInformation = executionPlanInformationAsCoreInstance == null ? null : executionPlanInformationAsCoreInstance.getName();

                final CoreInstance dataSourceCoreInstance = Instance.getValueForMetaPropertyToOneResolved(relationalActivity, RelationalActivityProperties.dataSource, processorSupport);

                String dbType = null;
                String dbHost = null;
                Integer dbPort = null;
                String dbName = null;
                if (dataSourceCoreInstance != null)
                {
                    final CoreInstance dbTypeAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(dataSourceCoreInstance, RelationalActivityProperties.dataSource_type, processorSupport);
                    dbType = dbTypeAsCoreInstance.getName();

                    final CoreInstance dbHostAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(dataSourceCoreInstance, RelationalActivityProperties.dataSource_host, processorSupport);
                    dbHost = dbHostAsCoreInstance.getName();

                    final CoreInstance dbPortAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(dataSourceCoreInstance, RelationalActivityProperties.dataSource_port, processorSupport);
                    dbPort = PrimitiveUtilities.getIntegerValue(dbPortAsCoreInstance).intValue();

                    final CoreInstance dbNameAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(dataSourceCoreInstance, RelationalActivityProperties.dataSource_name, processorSupport);
                    dbName = dbNameAsCoreInstance.getName();

                }

                final CoreInstance sqlGenerationTimeAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(relationalActivity, RelationalActivityProperties.sqlGenerationTimeInNanoSecond, processorSupport);
                final Long sqlGenerationTimeInNanoSeconds = sqlGenerationTimeAsCoreInstance == null ? null : PrimitiveUtilities.getIntegerValue(sqlGenerationTimeAsCoreInstance).longValue();

                final CoreInstance connectionAcquisitionTimeAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(relationalActivity, RelationalActivityProperties.connectionAcquisitionTimeInNanoSecond, processorSupport);
                final Long connectionAcquisitionTimeInNanoSeconds = connectionAcquisitionTimeAsCoreInstance == null ? null : PrimitiveUtilities.getIntegerValue(connectionAcquisitionTimeAsCoreInstance).longValue();

                listener.relationalActivityCompleted(dbHost, dbPort, dbName, dbType, sql, executionPlanInformation, executionTimeInNanoSeconds, sqlGenerationTimeInNanoSeconds, connectionAcquisitionTimeInNanoSeconds);


            }
        });
    }

    private void handleRoutingActivities(RichIterable<? extends CoreInstance> activities, final ProcessorSupport processorSupport) throws PureExecutionException
    {

        activities.select(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return Instance.instanceOf(coreInstance, "meta::pure::mapping::RoutingActivity", processorSupport);
            }
        }).forEach(new Procedure<CoreInstance>()
        {
            @Override
            public void value(CoreInstance routingActivity)
            {

                final CoreInstance routingTimeInNanoSecondAsCoreInstance = Instance.getValueForMetaPropertyToOneResolved(routingActivity, RoutingActivityProperties.routingTimeInNanoSecond, processorSupport);
                final Long routingTimeInNanoSecond = routingTimeInNanoSecondAsCoreInstance == null ? null : PrimitiveUtilities.getIntegerValue(routingTimeInNanoSecondAsCoreInstance).longValue();

                listener.routingActivityCompleted(routingTimeInNanoSecond);

            }
        });
    }
}
