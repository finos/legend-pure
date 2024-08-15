// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.generated;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SQLNull;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.CsvReader;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.LoadToDbTableHelper;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.RelationalNativeImplementation;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.RelationalExecutionProperties;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetRowIterableProvider;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetValueHandlers;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.SqlFunction;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.IConnectionManagerHandler;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.PureConnectionUtils;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.SQLExceptionHandler;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListenerState;
import org.finos.legend.pure.runtime.java.shared.listeners.IdentifableExecutionEndListner;

import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure.collectIterable;

public class RelationalGen
{
    private static final IConnectionManagerHandler connectionManagerHandler = IConnectionManagerHandler.CONNECTION_MANAGER_HANDLER;

    public static Object logActivities(RichIterable<? extends Root_meta_pure_mapping_Activity> activities, ExecutionSupport es)
    {
        for (Root_meta_pure_mapping_Activity activity : activities)
        {
            if (activity instanceof Root_meta_relational_mapping_RelationalActivity)
            {
                Root_meta_relational_mapping_RelationalActivity relationalActivity = (Root_meta_relational_mapping_RelationalActivity)activity;
                String sql = relationalActivity._sql();
                String executionPlanInformation = relationalActivity._executionPlanInformation();
                Long executionTimeInNanoSeconds = relationalActivity._executionTimeInNanoSecond();
                Long sqlGenerationTimeInNanoSeconds = relationalActivity._sqlGenerationTimeInNanoSecond();
                Long connectionAcquisitionTimeInNanoSeconds = relationalActivity._connectionAcquisitionTimeInNanoSecond();
                String dbType = null;
                String dbHost = null;
                Integer dbPort = null;
                String dbName = null;
                if (relationalActivity._dataSource() != null)
                {
                    dbType = relationalActivity._dataSource()._type()._name();
                    dbHost = relationalActivity._dataSource()._host();
                    dbPort = (int)relationalActivity._dataSource()._port();
                    dbName = relationalActivity._dataSource()._name();
                }
                ((CompiledExecutionSupport)es).getExecutionActivityListener().relationalActivityCompleted(dbHost, dbPort, dbName, dbType, sql, executionPlanInformation, executionTimeInNanoSeconds, sqlGenerationTimeInNanoSeconds, connectionAcquisitionTimeInNanoSeconds);
            }
            if (activity instanceof Root_meta_pure_mapping_RoutingActivity)
            {
                Root_meta_pure_mapping_RoutingActivity routingActivity = (Root_meta_pure_mapping_RoutingActivity)activity;
                Long routingTimeInNanoSecond = routingActivity._routingTimeInNanoSecond();
                ((CompiledExecutionSupport)es).getExecutionActivityListener().routingActivityCompleted(routingTimeInNanoSecond);
            }
        }
        return null;
    }

    public static Root_meta_relational_metamodel_execute_ResultSet fetchDbMetaData(Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, SqlFunction<DatabaseMetaData, java.sql.ResultSet> sqlFunction, ImmutableMap<String, ? extends Function<ListIterable<Object>, String>> extraValues, ExecutionSupport es)
    {
        Root_meta_relational_metamodel_execute_ResultSet pureResult = new Root_meta_relational_metamodel_execute_ResultSet_Impl("");

        Connection connection = null;
        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;
        try
        {

            long startRequestConnection = System.nanoTime();
            connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(pureConnection, ((CompiledExecutionSupport)es).getProcessorSupport());
            pureResult._connectionAcquisitionTimeInNanoSecond(System.nanoTime() - startRequestConnection);
            connection = connectionWithDataSourceInfo.getConnection();
            connection.setAutoCommit(true);

            int rowLimit = RelationalExecutionProperties.getMaxRows();
            SQLNull sqlNull = new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull");
            String tz = pureConnection._timeZone() == null ? "GMT" : pureConnection._timeZone();

            String URL = connectionManagerHandler.getPotentialDebug(pureConnection, connection);
            if (URL != null)
            {
                pureResult = pureResult._executionPlanInformation(URL);
            }

            DatabaseMetaData databaseMetadata = connection.getMetaData();

            MutableList<String> columns = FastList.newList();
            java.sql.ResultSet rs = sqlFunction.valueOf(databaseMetadata);
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            pureResult._executionTimeInNanoSecond(System.nanoTime() - startRequestConnection);
            int count = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= count; i++)
            {
                String column = resultSetMetaData.getColumnLabel(i);
                columns.add(column);
            }

            MutableList<Function<ListIterable<Object>, String>> extraValueFunctions = FastList.newList();
            for (Pair<String, ? extends Function<ListIterable<Object>, String>> pair : extraValues.keyValuesView())
            {
                columns.add(pair.getOne());
                extraValueFunctions.add(pair.getTwo());
            }

            int rowCount = 0;
            MutableList<Root_meta_relational_metamodel_execute_Row> rows = Lists.mutable.of();
            while (rs.next())
            {
                rowCount++;
                ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(resultSetMetaData);
                MutableList<Object> rowValues = RelationalNativeImplementation.processRow(rs, handlers, sqlNull, new GregorianCalendar(TimeZone.getTimeZone(tz)));
                for (Function<ListIterable<Object>, String> function : extraValueFunctions)
                {
                    rowValues.add(function.valueOf(rowValues));
                }
                rows.add((new Root_meta_relational_metamodel_execute_Row_Impl("Anonymous_NoCounter"))._valuesAddAll(rowValues)._parent(pureResult));
                if (RelationalExecutionProperties.shouldThrowIfMaxRowsExceeded() && rowLimit > 0)
                {
                    if (rowCount > rowLimit)
                    {
                        throw new PureExecutionException("Too many rows returned. PURE currently supports results with up to " + rowLimit + " rows. Please add a filter or use the take or limit function to limit the rows returned");
                    }
                    ;
                }
            }

            pureResult._rows(rows);
            pureResult._columnNames(columns);

            String dbHost = connectionWithDataSourceInfo.getDataSource().getHost();
            Integer dbPort = connectionWithDataSourceInfo.getDataSource().getPort();
            String dbName = connectionWithDataSourceInfo.getDataSource().getDataSourceName();
            String serverPrincipal = connectionWithDataSourceInfo.getDataSource().getServerPrincipal();
            if (pureConnection._type() != null && dbHost != null && dbPort != null && dbName != null)
            {
                Root_meta_relational_runtime_DataSource ds = new Root_meta_relational_runtime_DataSource_Impl("ID");
                ds._type(pureConnection._type());
                ds._port(dbPort.longValue());
                ds._host(dbHost);
                ds._name(dbName);
                if (serverPrincipal != null)
                {
                    ds._serverPrincipal(serverPrincipal);
                }
                pureResult._dataSource(ds);
            }
            connection.close();
            rs.close();
            return pureResult;
        }
        catch (SQLException e)
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException e1)
                {
                }
            }
            throw new PureExecutionException(SQLExceptionHandler.buildExceptionString(e, connection), e);
        }
    }

    public static Root_meta_relational_metamodel_execute_ResultSet executeInDb(String sql, Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)
    {
        Root_meta_relational_metamodel_execute_ResultSet pureResult = new Root_meta_relational_metamodel_execute_ResultSet_Impl("OK");

        Connection connection = null;
        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;
        try
        {

            long startRequestConnection = System.nanoTime();
            connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(pureConnection, ((CompiledExecutionSupport)es).getProcessorSupport());
            connection = connectionWithDataSourceInfo.getConnection();
            if (!PureConnectionUtils.isPureConnectionType(pureConnection, "Hive"))
            {
                connection.setAutoCommit(true);
            }
            pureResult._connectionAcquisitionTimeInNanoSecond(System.nanoTime() - startRequestConnection);

            SQLNull sqlNull = new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull");
            String tz = pureConnection._timeZone() == null ? "GMT" : pureConnection._timeZone();

            String URL = connectionManagerHandler.getPotentialDebug(pureConnection, connection);
            if (URL != null)
            {
                pureResult = pureResult._executionPlanInformation(URL);
            }

            ResultSetRowIterableProvider.ResultSetIterableContainer resultContainer = ResultSetRowIterableProvider.createResultSetIterator(pureConnection, connection, sql, RelationalExecutionProperties.getMaxRows(), RelationalExecutionProperties.shouldThrowIfMaxRowsExceeded(), (int)queryTimeoutInSeconds, (int)fetchSize, new CreateRowFunction(pureResult), sqlNull, tz, si, (CompiledExecutionSupport)es, connectionWithDataSourceInfo);
            pureResult._columnNamesAddAll(resultContainer.columnNames);
            pureResult._executionTimeInNanoSecond(resultContainer.queryTimeInNanos);
            pureResult._rows(resultContainer.rowIterable);

            String dbHost = connectionWithDataSourceInfo.getDataSource().getHost();
            Integer dbPort = connectionWithDataSourceInfo.getDataSource().getPort();
            String dbName = connectionWithDataSourceInfo.getDataSource().getDataSourceName();
            String serverPrincipal = connectionWithDataSourceInfo.getDataSource().getServerPrincipal();
            if (pureConnection._type() != null && dbHost != null && dbPort != null && dbName != null)
            {
                Root_meta_relational_runtime_DataSource ds = new Root_meta_relational_runtime_DataSource_Impl("ID");
                ds._type(pureConnection._type());
                ds._port(dbPort.longValue());
                ds._host(dbHost);
                ds._name(dbName);
                if (serverPrincipal != null) ds._serverPrincipal(serverPrincipal);
                pureResult._dataSource(ds);
            }
            return pureResult;
        }
        catch (SQLException e)
        {
            throw new PureExecutionException(si, SQLExceptionHandler.buildExceptionString(e, connection), e);
        }
    }

    public static Root_meta_relational_metamodel_execute_ResultSet dropTempTable(String tableName, String sql, Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)
    {
        ((CompiledExecutionSupport)es).unRegisterIdentifableExecutionEndListener(tableName);
        return executeInDb(sql, pureConnection, 0, 0, si, es);
    }

    public static Root_meta_relational_metamodel_execute_ResultSet createTempTable(final String tableName, String sql, final Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, final SourceInformation si, final boolean relyOnFinallyForCleanup, final ExecutionSupport es)
    {
        ((CompiledExecutionSupport)es).registerIdentifableExecutionEndListener(new IdentifableExecutionEndListner()
        {
            @Override
            public ExecutionEndListenerState executionEnd(Exception endException)
            {
                executeInDb("drop table if exists " + tableName, pureConnection, 0, 0, si, es);
                if (endException == null)
                {
                    return relyOnFinallyForCleanup ? new ExecutionEndListenerState(false) : new ExecutionEndListenerState(true, "Temporary table: " + tableName + " should be dropped explicitly");
                }
                else
                {
                    return new ExecutionEndListenerState(false);
                }
            }

            @Override
            public String getId()
            {
                return tableName;
            }
        });
        return executeInDb(sql, pureConnection, 0, 0, si, es);
    }

    public static class CreateRowFunction extends DefendedFunction<RichIterable<Object>, Root_meta_relational_metamodel_execute_Row>
    {
        private final Root_meta_relational_metamodel_execute_ResultSet pureResult;

        public CreateRowFunction(Root_meta_relational_metamodel_execute_ResultSet pureResult)
        {
            this.pureResult = pureResult;
        }

        @Override
        public Root_meta_relational_metamodel_execute_Row valueOf(RichIterable<Object> values)
        {

            Root_meta_relational_metamodel_execute_Row row = new Root_meta_relational_metamodel_execute_Row_Impl("ID");
            row = row._parent(this.pureResult);
            row._valuesAddAll(values);
            return row;

        }
    }

    public static RichIterable<Object> loadCsvToDbTable(String filePath, Table table, Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, Long numberOfRows, ExecutionSupport es)
    {
        Integer rowLimit = numberOfRows == null ? null : numberOfRows.intValue();
        ListIterable<String> columnTypes = getColumnTypes(table, ((CompiledExecutionSupport)es).getProcessorSupport());
        Iterable<ListIterable<?>> values = LoadToDbTableHelper.collectIterable(LazyIterate.drop(CsvReader.readCsv(((CompiledExecutionSupport)es).getCodeStorage(), null, filePath, 500, rowLimit), 1), columnTypes, filePath, table._name());
        bulkInsertInDb(pureConnection, table, values, rowLimit, es);
        return Lists.mutable.empty();
    }

    public static RichIterable<Object> loadValuesToDbTable(RichIterable<? extends Object> values, Table table, Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, ExecutionSupport es)
    {
        ListIterable<String> columnTypes = getColumnTypes(table, ((CompiledExecutionSupport)es).getProcessorSupport());
        Iterable<ListIterable<?>> columnValues = collectIterable(LazyIterate.drop(values, 3), columnTypes);
        bulkInsertInDb(pureConnection, table, columnValues, null, es);
        return Lists.mutable.empty();
    }

    public static RichIterable<Object> loadValuesToDbTable(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends Object>> values, Table table, Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, ExecutionSupport es)
    {
        ListIterable<String> columnTypes = getColumnTypes(table, ((CompiledExecutionSupport)es).getProcessorSupport());
        Iterable<ListIterable<?>> columnValues = collectIterable(LazyIterate.drop(values._values(), 3), columnTypes);
        bulkInsertInDb(pureConnection, table, columnValues, null, es);
        return Lists.mutable.empty();
    }

    private static ListIterable<String> getColumnTypes(Table table, final ProcessorSupport support)
    {
        return table._columns().collect(new DefendedFunction<RelationalOperationElement, String>()
        {
            @Override
            public String valueOf(RelationalOperationElement column)
            {
                return support.getClassifier(((Column)column)._type()).toString();
            }
        }).toList();
    }

    private static void bulkInsertInDb(Root_meta_external_store_relational_runtime_DatabaseConnection pureConnection, Table table, Iterable<ListIterable<?>> values, Integer rowLimit, ExecutionSupport es)
    {
        if (!(pureConnection instanceof Root_meta_external_store_relational_runtime_TestDatabaseConnection))
        {
            throw new PureExecutionException("Bulk insert is only supported for the TestDatabaseConnection");
        }

        String schemaName = table._schema()._name();
        String tableName = table._name();
        ListIterable<String> columnNames = table._columns().collect(new DefendedFunction<RelationalOperationElement, String>()
        {
            @Override
            public String valueOf(RelationalOperationElement column)
            {
                return ((Column)column)._name();
            }
        }).toList();

        StringBuilder sql = LoadToDbTableHelper.buildInsertStatementHeader(schemaName, tableName, columnNames);

        Connection connection = null;
        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;
        PreparedStatement statement = null;

        try
        {
            try
            {
                connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(pureConnection, ((CompiledExecutionSupport)es).getProcessorSupport());
                connection = connectionWithDataSourceInfo.getConnection();
                connection.setAutoCommit(true);
                String sqlString = sql.toString();
                statement = connection.prepareStatement(sqlString);
                if (rowLimit != null)
                {
                    statement.setMaxRows(rowLimit);
                }
                connectionManagerHandler.addPotentialDebug(pureConnection, statement);
                connectionManagerHandler.registerStatement(statement, sqlString, -1, -1);

                int[] res = LoadToDbTableHelper.insertBatch(values, statement);
                connection.commit();
            }
            finally
            {
                if (connection != null)
                {
                    connection.close();
                }
                if (statement != null)
                {
                    connectionManagerHandler.unregisterStatement(statement);
                    statement.close();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PureExecutionException(SQLExceptionHandler.buildExceptionString(e, connection), e);
        }
    }
}
