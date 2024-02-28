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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;

public class FetchDbSchemasMetadata extends AbstractFetchDbMetadata
{
    public FetchDbSchemasMetadata(ModelRepository repository, Message message, int maxRows)
    {
        this.repository = repository;
        this.message = message;
        this.maxRows = maxRows;
    }

    @Override
    public CoreInstance execute(final ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance connectionInformation = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance pureResult = this.loadDatabaseMetaData(connectionInformation, functionExpressionToUseInStack, processorSupport, new SqlFunction<DatabaseMetaData, ResultSet>()
        {
            @Override
            public ResultSet valueOf(DatabaseMetaData databaseMetaData) throws SQLException
            {
                String schemaPattern = Multiplicity.isToOne(params.get(1).getValueForMetaPropertyToOne(M3Properties.multiplicity)) ? Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName() : null;
                return databaseMetaData.getSchemas(null, schemaPattern);
            }
        });
        return ValueSpecificationBootstrap.wrapValueSpecification(pureResult, true, processorSupport);
    }
}
