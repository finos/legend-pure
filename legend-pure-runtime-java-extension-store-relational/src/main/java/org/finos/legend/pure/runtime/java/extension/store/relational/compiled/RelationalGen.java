package org.finos.legend.pure.runtime.java.extension.store.relational.compiled;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_SQLNull_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_ResultSet_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_Row_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_runtime_DataSource_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.execute.ResultSet;
import org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.DatabaseConnection;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.store.relational.RelationalNativeImplementation;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.SqlFunction;

import java.sql.DatabaseMetaData;

public class RelationalGen
{
    public static ResultSet executeInDb(String sql, DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)
    {
        return RelationalNativeImplementation.executeInDb(sql, pureConnection, queryTimeoutInSeconds, fetchSize, si, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl("OK"), () -> new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull"), () -> new Root_meta_relational_metamodel_execute_Row_Impl("ID"), () -> new Root_meta_relational_runtime_DataSource_Impl("ID"), es);
    }

    public static ResultSet dropTempTable(String tableName, String sql, DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, SourceInformation si, ExecutionSupport es)
    {
        return RelationalNativeImplementation.dropTempTable(tableName, sql, pureConnection, queryTimeoutInSeconds, fetchSize, si, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl("OK"), () -> new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull"), () -> new Root_meta_relational_metamodel_execute_Row_Impl("ID"), () -> new Root_meta_relational_runtime_DataSource_Impl("ID"), es);
    }

    public static ResultSet createTempTable(final String tableName, String sql, final DatabaseConnection pureConnection, long queryTimeoutInSeconds, long fetchSize, final SourceInformation si, final boolean relyOnFinallyForCleanup, final ExecutionSupport es)
    {
        return RelationalNativeImplementation.createTempTable(tableName, sql, pureConnection, queryTimeoutInSeconds, fetchSize, si, relyOnFinallyForCleanup, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl("OK"), () -> new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull"), () -> new Root_meta_relational_metamodel_execute_Row_Impl("ID"), () -> new Root_meta_relational_runtime_DataSource_Impl("ID"), es);
    }

    public static ResultSet fetchDbMetaData(DatabaseConnection pureConnection, SqlFunction<DatabaseMetaData, java.sql.ResultSet> sqlFunction, ImmutableMap<String, ? extends Function<ListIterable<Object>, String>> extraValues, ExecutionSupport es)
    {
        return RelationalNativeImplementation.fetchDbMetaData(pureConnection, sqlFunction, extraValues, () -> new Root_meta_relational_metamodel_execute_ResultSet_Impl("OK"), () -> new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull"), () -> new Root_meta_relational_metamodel_execute_Row_Impl("ID"), () -> new Root_meta_relational_runtime_DataSource_Impl("ID"), es);
    }
}
