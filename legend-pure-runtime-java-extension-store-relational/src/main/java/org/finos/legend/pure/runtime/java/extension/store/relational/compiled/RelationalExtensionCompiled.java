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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.RelationalStoreCoreInstanceFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.CreateTempTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.CreateTempTableWithFinally;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.DropTempTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ExecuteInDb;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbColumnsMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbImportedKeysMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbPrimaryKeysMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbSchemasMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbTablesMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LoadCsvToDbTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LoadValuesToDbTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LoadValuesToDbTableNew;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LogActivities;

import java.util.List;

public class RelationalExtensionCompiled implements CompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "RelationalGen",
                "package org.finos.legend.pure.generated;\n" +
                        "\n" +
                        "import org.eclipse.collections.api.RichIterable;\n" +
                        "import org.eclipse.collections.api.block.function.Function;\n" +
                        "import org.eclipse.collections.api.factory.Lists;\n" +
                        "import org.eclipse.collections.api.list.ListIterable;\n" +
                        "import org.eclipse.collections.api.list.MutableList;\n" +
                        "import org.eclipse.collections.api.map.ImmutableMap;\n" +
                        "import org.eclipse.collections.api.tuple.Pair;\n" +
                        "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                        "import org.eclipse.collections.impl.utility.LazyIterate;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SQLNull;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;\n" +
                        "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                        "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                        "import org.finos.legend.pure.m3.navigation.ProcessorSupport;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.CsvReader;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.LoadToDbTableHelper;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.RelationalNativeImplementation;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.RelationalExecutionProperties;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetRowIterableProvider;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetValueHandlers;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.SqlFunction;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.shared.IConnectionManagerHandler;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.shared.PureConnectionUtils;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.store.relational.shared.SQLExceptionHandler;\n" +
                        "import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListenerState;\n" +
                        "import org.finos.legend.pure.runtime.java.shared.listeners.IdentifableExecutionEndListner;\n" +
                        "\n" +
                        "import java.sql.*;\n" +
                        "import java.util.Calendar;\n" +
                        "import java.util.GregorianCalendar;\n" +
                        "import java.util.TimeZone;\n" +
                        "\n" +
                        "import static org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure.collectIterable;\n" +
                        "\n" +
                        "public class RelationalGen\n" +
                        "{\n" +
                        "    private static final IConnectionManagerHandler connectionManagerHandler = IConnectionManagerHandler.CONNECTION_MANAGER_HANDLER;\n" +
                        "\n" +
                        "    public static Object logActivities(RichIterable<? extends Root_meta_pure_mapping_Activity> activities, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        for (Root_meta_pure_mapping_Activity activity : activities)\n" +
                        "        {\n" +
                        "            if (activity instanceof Root_meta_relational_mapping_RelationalActivity)\n" +
                        "            {\n" +
                        "                Root_meta_relational_mapping_RelationalActivity relationalActivity = (Root_meta_relational_mapping_RelationalActivity)activity;\n" +
                        "                String sql = relationalActivity._sql();\n" +
                        "                String executionPlanInformation = relationalActivity._executionPlanInformation();\n" +
                        "                Long executionTimeInNanoSeconds = relationalActivity._executionTimeInNanoSecond();\n" +
                        "                Long sqlGenerationTimeInNanoSeconds = relationalActivity._sqlGenerationTimeInNanoSecond();\n" +
                        "                Long connectionAcquisitionTimeInNanoSeconds = relationalActivity._connectionAcquisitionTimeInNanoSecond();\n" +
                        "                String dbType = null;\n" +
                        "                String dbHost = null;\n" +
                        "                Integer dbPort = null;\n" +
                        "                String dbName = null;\n" +
                        "                if (relationalActivity._dataSource() != null)\n" +
                        "                {\n" +
                        "                    dbType = relationalActivity._dataSource()._type()._name();\n" +
                        "                    dbHost = relationalActivity._dataSource()._host();\n" +
                        "                    dbPort = (int)relationalActivity._dataSource()._port();\n" +
                        "                    dbName = relationalActivity._dataSource()._name();\n" +
                        "                }\n" +
                        "                ((CompiledExecutionSupport)es).getExecutionActivityListener().relationalActivityCompleted(dbHost, dbPort, dbName, dbType, sql, executionPlanInformation, executionTimeInNanoSeconds, sqlGenerationTimeInNanoSeconds, connectionAcquisitionTimeInNanoSeconds);\n" +
                        "            }\n" +
                        "            if (activity instanceof Root_meta_pure_mapping_RoutingActivity)\n" +
                        "            {\n" +
                        "                Root_meta_pure_mapping_RoutingActivity routingActivity = (Root_meta_pure_mapping_RoutingActivity)activity;\n" +
                        "                Long routingTimeInNanoSecond = routingActivity._routingTimeInNanoSecond();\n" +
                        "                ((CompiledExecutionSupport)es).getExecutionActivityListener().routingActivityCompleted(routingTimeInNanoSecond);\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return null;\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Root_meta_relational_metamodel_execute_ResultSet fetchDbMetaData(Root_meta_relational_runtime_DatabaseConnection pureConnection, SqlFunction<DatabaseMetaData, java.sql.ResultSet> sqlFunction, ImmutableMap<String, ? extends Function<ListIterable<Object>, String>> extraValues, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        Root_meta_relational_metamodel_execute_ResultSet pureResult = new Root_meta_relational_metamodel_execute_ResultSet_Impl(\"\");\n" +
                        "\n" +
                        "        Connection connection = null;\n" +
                        "        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;\n" +
                        "        try\n" +
                        "        {\n" +
                        "\n" +
                        "            long startRequestConnection = System.nanoTime();\n" +
                        "            connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(pureConnection, ((CompiledExecutionSupport)es).getProcessorSupport());\n" +
                        "            pureResult._connectionAcquisitionTimeInNanoSecond(System.nanoTime() - startRequestConnection);\n" +
                        "            connection = connectionWithDataSourceInfo.getConnection();\n" +
                        "            connection.setAutoCommit(true);\n" +
                        "\n" +
                        "            int rowLimit = RelationalExecutionProperties.getMaxRows();\n" +
                        "            SQLNull sqlNull = new Root_meta_relational_metamodel_SQLNull_Impl(\"SQLNull\");\n" +
                        "            String tz = pureConnection._timeZone() == null ? \"GMT\" : pureConnection._timeZone();\n" +
                        "\n" +
                        "            String URL = connectionManagerHandler.getPotentialDebug(pureConnection, connection);\n" +
                        "            if (URL != null)\n" +
                        "            {\n" +
                        "                pureResult = pureResult._executionPlanInformation(URL);\n" +
                        "            }\n" +
                        "\n" +
                        "            DatabaseMetaData databaseMetadata = connection.getMetaData();\n" +
                        "\n" +
                        "            MutableList<String> columns = FastList.newList();\n" +
                        "            java.sql.ResultSet rs = sqlFunction.valueOf(databaseMetadata);\n" +
                        "            ResultSetMetaData resultSetMetaData = rs.getMetaData();\n" +
                        "            pureResult._executionTimeInNanoSecond(System.nanoTime() - startRequestConnection);\n" +
                        "            int count = resultSetMetaData.getColumnCount();\n" +
                        "            for (int i = 1; i <= count; i++)\n" +
                        "            {\n" +
                        "                String column = resultSetMetaData.getColumnLabel(i);\n" +
                        "                columns.add(column);\n" +
                        "            }\n" +
                        "\n" +
                        "            MutableList<Function<ListIterable<Object>, String>> extraValueFunctions = FastList.newList();\n" +
                        "            for (Pair<String, ? extends Function<ListIterable<Object>, String>> pair : extraValues.keyValuesView())\n" +
                        "            {\n" +
                        "                columns.add(pair.getOne());\n" +
                        "                extraValueFunctions.add(pair.getTwo());\n" +
                        "            }\n" +
                        "\n" +
                        "            int rowCount = 0;\n" +
                        "            MutableList<Root_meta_relational_metamodel_execute_Row> rows = Lists.mutable.of();\n" +
                        "            while (rs.next())\n" +
                        "            {\n" +
                        "                rowCount++;\n" +
                        "                ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(resultSetMetaData);\n" +
                        "                MutableList<Object> rowValues = RelationalNativeImplementation.processRow(rs, handlers, sqlNull, new GregorianCalendar(TimeZone.getTimeZone(tz)));\n" +
                        "                for (Function<ListIterable<Object>, String> function : extraValueFunctions)\n" +
                        "                {\n" +
                        "                    rowValues.add(function.valueOf(rowValues));\n" +
                        "                }\n" +
                        "                rows.add((new Root_meta_relational_metamodel_execute_Row_Impl(\"Anonymous_NoCounter\"))._valuesAddAll(rowValues)._parent(pureResult));\n" +
                        "                if (RelationalExecutionProperties.shouldThrowIfMaxRowsExceeded() && rowLimit > 0)\n" +
                        "                {\n" +
                        "                    if (rowCount > rowLimit)\n" +
                        "                    {\n" +
                        "                        throw new PureExecutionException(\"Too many rows returned. PURE currently supports results with up to \" + rowLimit + \" rows. Please add a filter or use the take or limit function to limit the rows returned\");\n" +
                        "                    }\n" +
                        "                    ;\n" +
                        "                }\n" +
                        "            }\n" +
                        "\n" +
                        "            pureResult._rows(rows);\n" +
                        "            pureResult._columnNames(columns);\n" +
                        "\n" +
                        "            String dbHost = connectionWithDataSourceInfo.getDataSource().getHost();\n" +
                        "            Integer dbPort = connectionWithDataSourceInfo.getDataSource().getPort();\n" +
                        "            String dbName = connectionWithDataSourceInfo.getDataSource().getDataSourceName();\n" +
                        "            String serverPrincipal = connectionWithDataSourceInfo.getDataSource().getServerPrincipal();\n" +
                        "            if (pureConnection._type() != null && dbHost != null && dbPort != null && dbName != null)\n" +
                        "            {\n" +
                        "                Root_meta_relational_runtime_DataSource ds = new Root_meta_relational_runtime_DataSource_Impl(\"ID\");\n" +
                        "                ds._type(pureConnection._type());\n" +
                        "                ds._port(dbPort.longValue());\n" +
                        "                ds._host(dbHost);\n" +
                        "                ds._name(dbName);\n" +
                        "                if (serverPrincipal != null)\n" +
                        "                {\n" +
                        "                    ds._serverPrincipal(serverPrincipal);\n" +
                        "                }\n" +
                        "                pureResult._dataSource(ds);\n" +
                        "            }\n" +
                        "            connection.close();\n" +
                        "            rs.close();\n" +
                        "            return pureResult;\n" +
                        "        }\n" +
                        "        catch (SQLException e)\n" +
                        "        {\n" +
                        "            if (connection != null)\n" +
                        "            {\n" +
                        "                try\n" +
                        "                {\n" +
                        "                    connection.close();\n" +
                        "                }\n" +
                        "                catch (SQLException e1)\n" +
                        "                {\n" +
                        "                }\n" +
                        "            }\n" +
                        "            throw new PureExecutionException(SQLExceptionHandler.buildExceptionString(e, connection), e);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Root_meta_relational_metamodel_execute_ResultSet executeInDb(String sql, Root_meta_relational_runtime_DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        Root_meta_relational_metamodel_execute_ResultSet pureResult = new Root_meta_relational_metamodel_execute_ResultSet_Impl(\"OK\");\n" +
                        "\n" +
                        "        Connection connection = null;\n" +
                        "        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;\n" +
                        "        try\n" +
                        "        {\n" +
                        "\n" +
                        "            long startRequestConnection = System.nanoTime();\n" +
                        "            connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(pureConnection, ((CompiledExecutionSupport)es).getProcessorSupport());\n" +
                        "            connection = connectionWithDataSourceInfo.getConnection();\n" +
                        "            if (!PureConnectionUtils.isPureConnectionType(pureConnection, \"Hive\"))\n" +
                        "            {\n" +
                        "                connection.setAutoCommit(true);\n" +
                        "            }\n" +
                        "            pureResult._connectionAcquisitionTimeInNanoSecond(System.nanoTime() - startRequestConnection);\n" +
                        "\n" +
                        "            SQLNull sqlNull = new Root_meta_relational_metamodel_SQLNull_Impl(\"SQLNull\");\n" +
                        "            String tz = pureConnection._timeZone() == null ? \"GMT\" : pureConnection._timeZone();\n" +
                        "\n" +
                        "            String URL = connectionManagerHandler.getPotentialDebug(pureConnection, connection);\n" +
                        "            if (URL != null)\n" +
                        "            {\n" +
                        "                pureResult = pureResult._executionPlanInformation(URL);\n" +
                        "            }\n" +
                        "\n" +
                        "            ResultSetRowIterableProvider.ResultSetIterableContainer resultContainer = ResultSetRowIterableProvider.createResultSetIterator(pureConnection, connection, sql, RelationalExecutionProperties.getMaxRows(), RelationalExecutionProperties.shouldThrowIfMaxRowsExceeded(), (int)queryTimeoutInSeconds, (int)fetchSize, new CreateRowFunction(pureResult), sqlNull, tz, si, (CompiledExecutionSupport)es, connectionWithDataSourceInfo);\n" +
                        "            pureResult._columnNamesAddAll(resultContainer.columnNames);\n" +
                        "            pureResult._executionTimeInNanoSecond(resultContainer.queryTimeInNanos);\n" +
                        "            pureResult._rows(resultContainer.rowIterable);\n" +
                        "\n" +
                        "            String dbHost = connectionWithDataSourceInfo.getDataSource().getHost();\n" +
                        "            Integer dbPort = connectionWithDataSourceInfo.getDataSource().getPort();\n" +
                        "            String dbName = connectionWithDataSourceInfo.getDataSource().getDataSourceName();\n" +
                        "            String serverPrincipal = connectionWithDataSourceInfo.getDataSource().getServerPrincipal();\n" +
                        "            if (pureConnection._type() != null && dbHost != null && dbPort != null && dbName != null)\n" +
                        "            {\n" +
                        "                Root_meta_relational_runtime_DataSource ds = new Root_meta_relational_runtime_DataSource_Impl(\"ID\");\n" +
                        "                ds._type(pureConnection._type());\n" +
                        "                ds._port(dbPort.longValue());\n" +
                        "                ds._host(dbHost);\n" +
                        "                ds._name(dbName);\n" +
                        "                if (serverPrincipal != null) ds._serverPrincipal(serverPrincipal);\n" +
                        "                pureResult._dataSource(ds);\n" +
                        "            }\n" +
                        "            return pureResult;\n" +
                        "        }\n" +
                        "        catch (SQLException e)\n" +
                        "        {\n" +
                        "            throw new PureExecutionException(si, SQLExceptionHandler.buildExceptionString(e, connection), e);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Root_meta_relational_metamodel_execute_ResultSet dropTempTable(String tableName, String sql, Root_meta_relational_runtime_DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        ((CompiledExecutionSupport)es).unRegisterIdentifableExecutionEndListener(tableName);\n" +
                        "        return executeInDb(sql, pureConnection, 0, 0, si, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Root_meta_relational_metamodel_execute_ResultSet createTempTable(final String tableName, String sql, final Root_meta_relational_runtime_DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, final SourceInformation si, final boolean relyOnFinallyForCleanup, final ExecutionSupport es)\n" +
                        "    {\n" +
                        "        ((CompiledExecutionSupport)es).registerIdentifableExecutionEndListener(new IdentifableExecutionEndListner()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public ExecutionEndListenerState executionEnd(Exception endException)\n" +
                        "            {\n" +
                        "                executeInDb(\"drop table if exists \" + tableName, pureConnection, 0, 0, si, es);\n" +
                        "                if (endException == null)\n" +
                        "                {\n" +
                        "                    return relyOnFinallyForCleanup ? new ExecutionEndListenerState(false) : new ExecutionEndListenerState(true, \"Temporary table: \" + tableName + \" should be dropped explicitly\");\n" +
                        "                }\n" +
                        "                else\n" +
                        "                {\n" +
                        "                    return new ExecutionEndListenerState(false);\n" +
                        "                }\n" +
                        "            }\n" +
                        "\n" +
                        "            @Override\n" +
                        "            public String getId()\n" +
                        "            {\n" +
                        "                return tableName;\n" +
                        "            }\n" +
                        "        });\n" +
                        "        return executeInDb(sql, pureConnection, 0, 0, si, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class CreateRowFunction extends DefendedFunction<RichIterable<Object>, Root_meta_relational_metamodel_execute_Row>\n" +
                        "    {\n" +
                        "        private final Root_meta_relational_metamodel_execute_ResultSet pureResult;\n" +
                        "\n" +
                        "        public CreateRowFunction(Root_meta_relational_metamodel_execute_ResultSet pureResult)\n" +
                        "        {\n" +
                        "            this.pureResult = pureResult;\n" +
                        "        }\n" +
                        "\n" +
                        "        @Override\n" +
                        "        public Root_meta_relational_metamodel_execute_Row valueOf(RichIterable<Object> values)\n" +
                        "        {\n" +
                        "\n" +
                        "            Root_meta_relational_metamodel_execute_Row row = new Root_meta_relational_metamodel_execute_Row_Impl(\"ID\");\n" +
                        "            row = row._parent(this.pureResult);\n" +
                        "            row._valuesAddAll(values);\n" +
                        "            return row;\n" +
                        "\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static RichIterable<Object> loadCsvToDbTable(String filePath, Table table, Root_meta_relational_runtime_DatabaseConnection pureConnection, Long numberOfRows, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        Integer rowLimit = numberOfRows == null ? null : numberOfRows.intValue();\n" +
                        "        ListIterable<String> columnTypes = getColumnTypes(table, ((CompiledExecutionSupport)es).getProcessorSupport());\n" +
                        "        Iterable<ListIterable<?>> values = LoadToDbTableHelper.collectIterable(LazyIterate.drop(CsvReader.readCsv(((CompiledExecutionSupport)es).getCodeStorage(), null, filePath, 500, rowLimit), 1), columnTypes, filePath, table._name());\n" +
                        "        bulkInsertInDb(pureConnection, table, values, rowLimit, es);\n" +
                        "        return Lists.mutable.empty();\n" +
                        "    }\n" +
                        "\n" +
                        "    public static RichIterable<Object> loadValuesToDbTable(RichIterable<? extends Object> values, Table table, Root_meta_relational_runtime_DatabaseConnection pureConnection, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        ListIterable<String> columnTypes = getColumnTypes(table, ((CompiledExecutionSupport)es).getProcessorSupport());\n" +
                        "        Iterable<ListIterable<?>> columnValues = collectIterable(LazyIterate.drop(values, 3), columnTypes);\n" +
                        "        bulkInsertInDb(pureConnection, table, columnValues, null, es);\n" +
                        "        return Lists.mutable.empty();\n" +
                        "    }\n" +
                        "\n" +
                        "    public static RichIterable<Object> loadValuesToDbTable(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<? extends Object>> values, Table table, Root_meta_relational_runtime_DatabaseConnection pureConnection, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        ListIterable<String> columnTypes = getColumnTypes(table, ((CompiledExecutionSupport)es).getProcessorSupport());\n" +
                        "        Iterable<ListIterable<?>> columnValues = collectIterable(LazyIterate.drop(values._values(), 3), columnTypes);\n" +
                        "        bulkInsertInDb(pureConnection, table, columnValues, null, es);\n" +
                        "        return Lists.mutable.empty();\n" +
                        "    }\n" +
                        "\n" +
                        "    private static ListIterable<String> getColumnTypes(Table table, final ProcessorSupport support)\n" +
                        "    {\n" +
                        "        return table._columns().collect(new DefendedFunction<RelationalOperationElement, String>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public String valueOf(RelationalOperationElement column)\n" +
                        "            {\n" +
                        "                return support.getClassifier(((Column)column)._type()).toString();\n" +
                        "            }\n" +
                        "        }).toList();\n" +
                        "    }\n" +
                        "\n" +
                        "    private static void bulkInsertInDb(Root_meta_relational_runtime_DatabaseConnection pureConnection, Table table, Iterable<ListIterable<?>> values, Integer rowLimit, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        if (!(pureConnection instanceof Root_meta_relational_runtime_TestDatabaseConnection))\n" +
                        "        {\n" +
                        "            throw new PureExecutionException(\"Bulk insert is only supported for the TestDatabaseConnection\");\n" +
                        "        }\n" +
                        "\n" +
                        "        String schemaName = table._schema()._name();\n" +
                        "        String tableName = table._name();\n" +
                        "        ListIterable<String> columnNames = table._columns().collect(new DefendedFunction<RelationalOperationElement, String>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public String valueOf(RelationalOperationElement column)\n" +
                        "            {\n" +
                        "                return ((Column)column)._name();\n" +
                        "            }\n" +
                        "        }).toList();\n" +
                        "\n" +
                        "        StringBuilder sql = LoadToDbTableHelper.buildInsertStatementHeader(schemaName, tableName, columnNames);\n" +
                        "\n" +
                        "        Connection connection = null;\n" +
                        "        ConnectionWithDataSourceInfo connectionWithDataSourceInfo = null;\n" +
                        "        PreparedStatement statement = null;\n" +
                        "\n" +
                        "        try\n" +
                        "        {\n" +
                        "            try\n" +
                        "            {\n" +
                        "                connectionWithDataSourceInfo = connectionManagerHandler.getConnectionWithDataSourceInfo(pureConnection, ((CompiledExecutionSupport)es).getProcessorSupport());\n" +
                        "                connection = connectionWithDataSourceInfo.getConnection();\n" +
                        "                connection.setAutoCommit(true);\n" +
                        "                String sqlString = sql.toString();\n" +
                        "                statement = connection.prepareStatement(sqlString);\n" +
                        "                if (rowLimit != null)\n" +
                        "                {\n" +
                        "                    statement.setMaxRows(rowLimit);\n" +
                        "                }\n" +
                        "                connectionManagerHandler.addPotentialDebug(pureConnection, statement);\n" +
                        "                connectionManagerHandler.registerStatement(statement, sqlString, -1, -1);\n" +
                        "\n" +
                        "                int[] res = LoadToDbTableHelper.insertBatch(values, statement);\n" +
                        "                connection.commit();\n" +
                        "            }\n" +
                        "            finally\n" +
                        "            {\n" +
                        "                if (connection != null)\n" +
                        "                {\n" +
                        "                    connection.close();\n" +
                        "                }\n" +
                        "                if (statement != null)\n" +
                        "                {\n" +
                        "                    connectionManagerHandler.unregisterStatement(statement);\n" +
                        "                    statement.close();\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        catch (SQLException e)\n" +
                        "        {\n" +
                        "            throw new PureExecutionException(SQLExceptionHandler.buildExceptionString(e, connection), e);\n" +
                        "        }\n" +
                        "    }\n" +
                        "    \n" +
                        "}\n", false));
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(new CreateTempTable(), new CreateTempTableWithFinally(), new DropTempTable(), new ExecuteInDb(), new FetchDbColumnsMetaData(),
                new FetchDbImportedKeysMetaData(), new FetchDbPrimaryKeysMetaData(), new FetchDbSchemasMetaData(), new FetchDbTablesMetaData(), new LoadCsvToDbTable(),
                new LoadValuesToDbTable(), new LoadValuesToDbTableNew(), new LogActivities());
    }

    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return RelationalStoreCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_store_relational";
    }

    public static CompiledExtension extension()
    {
        return new RelationalExtensionCompiled();
    }
}
