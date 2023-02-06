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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.IConnectionManagerHandler;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.SQLExceptionHandler;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;

public abstract class AbstractFetchDbMetadata extends NativeFunction
{
    protected ModelRepository repository;
    protected Message message;
    protected int maxRows;
    private static final IConnectionManagerHandler connectionManagerHandler = IConnectionManagerHandler.CONNECTION_MANAGER_HANDLER;

    @Override
    public abstract CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException;

    protected CoreInstance loadDatabaseMetaData(CoreInstance connectionInformation, CoreInstance functionExpressionToUseInStack, ProcessorSupport processorSupport, SqlFunction<DatabaseMetaData, ResultSet> databaseMetadataFunction)
    {
        CoreInstance resultSetClassifier = processorSupport.package_getByUserPath("meta::relational::metamodel::execute::ResultSet");
        if (resultSetClassifier == null)
        {
            throw new RuntimeException("'meta::relational::metamodel::execute::ResultSet' is unknown");
        }
        CoreInstance rowClassifier = processorSupport.package_getByUserPath("meta::relational::metamodel::execute::Row");
        if (rowClassifier == null)
        {
            throw new RuntimeException("'meta::relational::metamodel::execute::Row' is unknown");
        }

        CoreInstance pureResult = this.repository.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), resultSetClassifier);
        Connection connection = null;
        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;
        try
        {
            try
            {
                this.message.setMessage("Acquiring connection...");

                CoreInstance dbTimeZone = connectionInformation.getValueForMetaPropertyToOne("timeZone");
                String tz = dbTimeZone == null ? "GMT" : dbTimeZone.getName();

                long startRequestConnection = System.nanoTime();
                connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(connectionInformation, processorSupport);
                Instance.addValueToProperty(pureResult, "connectionAcquisitionTimeInNanoSecond", this.repository.newIntegerCoreInstance(System.nanoTime() - startRequestConnection), processorSupport);

                connection = connectionWithDataSourceInfo.getConnection();

                this.message.setMessage("Getting Database Metadata...");
                long start = System.nanoTime();
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                ResultSet rs = databaseMetadataFunction.valueOf(databaseMetaData);

                ExecuteInDb.createPureResultSetFromDatabaseResultSet(pureResult, rs, functionExpressionToUseInStack, rowClassifier, tz, this.repository, start, this.maxRows, processorSupport);

                CoreInstance dbType = Instance.getValueForMetaPropertyToOneResolved(connectionInformation, "type", processorSupport);
                String dbHost = connectionWithDataSourceInfo.getDataSource().getHost();
                Integer dbPort = connectionWithDataSourceInfo.getDataSource().getPort();
                String dbName = connectionWithDataSourceInfo.getDataSource().getDataSourceName();
                String serverPrincipal = connectionWithDataSourceInfo.getDataSource().getServerPrincipal();

                if (dbType != null && dbHost != null && dbName != null && dbPort != null)
                {
                    CoreInstance dataSourceCoreInstance = repository.newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath("meta::relational::runtime::DataSource"));

                    Instance.addValueToProperty(dataSourceCoreInstance, "host", repository.newStringCoreInstance(dbHost), processorSupport);
                    Instance.addValueToProperty(dataSourceCoreInstance, "port", repository.newIntegerCoreInstance(dbPort), processorSupport);
                    Instance.addValueToProperty(dataSourceCoreInstance, "name", repository.newStringCoreInstance(dbName), processorSupport);
                    Instance.addValueToProperty(dataSourceCoreInstance, "type", dbType, processorSupport);
                    if (serverPrincipal != null)
                        Instance.addValueToProperty(dataSourceCoreInstance, "serverPrincipal", this.repository.newStringCoreInstance(serverPrincipal), processorSupport);

                    Instance.addValueToProperty(pureResult, "dataSource", dataSourceCoreInstance, processorSupport);
                }
            }
            finally
            {
                if (connection != null)
                {
                    connection.close();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), SQLExceptionHandler.buildExceptionString(e, connection), e);
        }

        this.message.setMessage("Getting Database Metadata...[DONE]");
        return pureResult;
    }
}
