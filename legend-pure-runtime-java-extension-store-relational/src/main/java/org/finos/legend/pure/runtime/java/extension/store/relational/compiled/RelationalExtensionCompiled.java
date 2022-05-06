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
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
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

public class RelationalExtensionCompiled extends BaseCompiledExtension
{
    public RelationalExtensionCompiled()
    {
        super(
                Lists.fixedSize.with(new CreateTempTable(), new CreateTempTableWithFinally(), new DropTempTable(), new ExecuteInDb(), new FetchDbColumnsMetaData(),
                        new FetchDbImportedKeysMetaData(), new FetchDbPrimaryKeysMetaData(), new FetchDbSchemasMetaData(), new FetchDbTablesMetaData(), new LoadCsvToDbTable(),
                        new LoadValuesToDbTable(), new LoadValuesToDbTableNew(), new LogActivities()),
                Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.runtime.java.extension.store.relational.compiled", "RelationalGen",
                        "package org.finos.legend.pure.runtime.java.extension.store.relational.compiled;\n" +
                                "\n" +
                                "import org.eclipse.collections.api.block.function.Function;\n" +
                                "import org.eclipse.collections.api.list.ListIterable;\n" +
                                "import org.eclipse.collections.api.map.ImmutableMap;\n" +
                                "import org.finos.legend.pure.generated.Root_meta_relational_metamodel_SQLNull_Impl;\n" +
                                "import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_ResultSet_Impl;\n" +
                                "import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_Row_Impl;\n" +
                                "import org.finos.legend.pure.generated.Root_meta_relational_runtime_DataSource_Impl;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.execute.ResultSet;\n" +
                                "import org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.DatabaseConnection;\n" +
                                "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                                "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                                "import org.finos.legend.pure.runtime.java.extension.store.relational.RelationalNativeImplementation;\n" +
                                "import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.SqlFunction;\n" +
                                "\n" +
                                "import java.sql.DatabaseMetaData;\n" +
                                "\n" +
                                "public class RelationalGen\n" +
                                "{\n" +
                                "    public static ResultSet executeInDb(String sql, DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return RelationalNativeImplementation.executeInDb(sql, pureConnection, queryTimeoutInSeconds, fetchSize, si, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl(\"OK\"), () -> new Root_meta_relational_metamodel_SQLNull_Impl(\"SQLNull\"), () -> new Root_meta_relational_metamodel_execute_Row_Impl(\"ID\"), () -> new Root_meta_relational_runtime_DataSource_Impl(\"ID\"), es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static ResultSet dropTempTable(String tableName, String sql, DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return RelationalNativeImplementation.dropTempTable(tableName, sql, pureConnection, queryTimeoutInSeconds, fetchSize, si, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl(\"OK\"), () -> new Root_meta_relational_metamodel_SQLNull_Impl(\"SQLNull\"), () -> new Root_meta_relational_metamodel_execute_Row_Impl(\"ID\"), () -> new Root_meta_relational_runtime_DataSource_Impl(\"ID\"), es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static ResultSet createTempTable(final String tableName, String sql, final DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, final SourceInformation si, final boolean relyOnFinallyForCleanup, final ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return RelationalNativeImplementation.createTempTable(tableName, sql, pureConnection, queryTimeoutInSeconds, fetchSize, si, relyOnFinallyForCleanup, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl(\"OK\"), () -> new Root_meta_relational_metamodel_SQLNull_Impl(\"SQLNull\"), () -> new Root_meta_relational_metamodel_execute_Row_Impl(\"ID\"), () -> new Root_meta_relational_runtime_DataSource_Impl(\"ID\"), es);\n" +
                                "    }\n" +
                                "\n" +
                                "    public static ResultSet fetchDbMetaData(DatabaseConnection pureConnection, SqlFunction<DatabaseMetaData, java.sql.ResultSet> sqlFunction, ImmutableMap<String, ? extends Function<ListIterable<Object>, String>> extraValues, ExecutionSupport es)\n" +
                                "    {\n" +
                                "        return RelationalNativeImplementation.fetchDbMetaData(pureConnection, sqlFunction, extraValues, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl(\"OK\"), () -> new Root_meta_relational_metamodel_SQLNull_Impl(\"SQLNull\"), () -> new Root_meta_relational_metamodel_execute_Row_Impl(\"ID\"), () -> new Root_meta_relational_runtime_DataSource_Impl(\"ID\"), es);\n" +
                                "    }\n" +
                                "}\n")),
                Lists.fixedSize.empty(),
                Lists.fixedSize.empty());
    }

    public static CompiledExtension extension()
    {
        return new RelationalExtensionCompiled();
    }
}
