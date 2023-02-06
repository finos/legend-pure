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

package org.finos.legend.pure.m2.relational;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.processor.DatabaseProcessor;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.io.IOException;

public class Database
{
    public static SetIterable<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database> getAllIncludedDBs(org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database, ProcessorSupport processorSupport)
    {
        ListIterable<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database> includes = (ListIterable<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database>)ImportStub.withImportStubByPasses(database._includesCoreInstance().toList(), processorSupport);
        if (includes.isEmpty())
        {
            return Sets.immutable.with(database);
        }

        MutableSet<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database> results = UnifiedSet.<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database>newSet(includes.size() + 1).with(database);
        collectIncludedDBs(results, includes, processorSupport);
        return results;
    }

    private static void collectIncludedDBs(MutableSet<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database> results, ListIterable<? extends CoreInstance> databases, ProcessorSupport processorSupport)
    {
        for (CoreInstance db : databases)
        {
            if (results.add((org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database)db))
            {
                ListIterable<? extends CoreInstance> includes = ImportStub.withImportStubByPasses(((org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database)db)._includesCoreInstance().toList(), processorSupport);
                collectIncludedDBs(results, includes, processorSupport);
            }
        }
    }

    public static CoreInstance findTable(org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database, final String schemaName, final String tableName, final ProcessorSupport processorSupport)
    {
        MutableList<CoreInstance> tables = Lists.mutable.empty();
        for (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database db : getAllIncludedDBs(database, processorSupport))
        {
            Schema schema = db._schemas().selectWith(DatabaseProcessor.SCHEMA_NAME_PREDICATE, schemaName).toList().getFirst();
            if (schema != null)
            {
                CoreInstance table = schema._tables().selectWith(DatabaseProcessor.NAMED_RELATION_NAME_PREDICATE, tableName).toList().getFirst();
                if (table == null)
                {
                    table = schema._views().selectWith(DatabaseProcessor.NAMED_RELATION_NAME_PREDICATE, tableName).toList().getFirst();
                }
                if (table != null)
                {
                    tables.add(table);
                }
            }
        }
        switch (tables.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return tables.get(0);
            }
            default:
            {
                StringBuilder message = new StringBuilder("The table '");
                message.append(tableName);
                message.append("' has been found ");
                message.append(tables.size());
                message.append(" times in the schema '");
                message.append(schemaName);
                message.append("' of the database '");
                PackageableElement.writeUserPathForPackageableElement(message, database);
                message.append('\'');
                throw new PureCompilationException(database.getSourceInformation(), message.toString());
            }
        }
    }

    public static Join findJoin(org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database, String joinName, ProcessorSupport processorSupport)
    {
        MutableList<Join> joins = Lists.mutable.empty();
        for (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database db : getAllIncludedDBs(database, processorSupport))
        {
            Join join = (Join) db.getValueInValueForMetaPropertyToManyWithKey(M2RelationalProperties.joins, M3Properties.name, joinName);
            if (join != null)
            {
                joins.add(join);
            }
        }
        switch (joins.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return joins.get(0);
            }
            default:
            {
                StringBuilder message = new StringBuilder("The join '");
                message.append(joinName);
                message.append("' has been found ");
                message.append(joins.size());
                message.append(" times in the database '");
                PackageableElement.writeUserPathForPackageableElement(message, database);
                message.append('\'');
                throw new PureCompilationException(database.getSourceInformation(), message.toString());
            }
        }
    }

    public static void writeTableId(Appendable appendable, RelationalOperationElement table, ProcessorSupport processorSupport)
    {
        try
        {
            String tableName = null;
            Schema tableSchema = null;
            if (table instanceof Table)
            {
                tableName = ((Table)table)._name();
                tableSchema = ((Table)table)._schema();
            }
            else if (table instanceof View)
            {
                tableName = ((View)table)._name();
                tableSchema = ((View)table)._schema();
            }
            if (tableSchema != null)
            {
                String tableSchemaName = tableSchema._name();
                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database tableDB = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database)ImportStub.withImportStubByPass(tableSchema._databaseCoreInstance(), processorSupport);
                if (tableDB != null)
                {
                    writeDatabaseName(appendable, tableDB, true);
                }
                if (!DatabaseProcessor.DEFAULT_SCHEMA_NAME.equals(tableSchemaName))
                {
                    appendable.append(tableSchemaName);
                    appendable.append('.');
                }
            }
            appendable.append(tableName);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void writeJoinId(Appendable appendable, Join join, boolean writeAt, ProcessorSupport processorSupport)
    {
        try
        {
            String joinName = join._name();
            org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database joinDB = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database)ImportStub.withImportStubByPass(join._databaseCoreInstance() , processorSupport);
            if (joinDB != null)
            {
                writeDatabaseName(appendable, joinDB, true);
            }
            if (writeAt)
            {
                appendable.append('@');
            }
            appendable.append(joinName);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void writeDatabaseName(Appendable appendable, org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database, boolean writeBrackets)
    {
        try
        {
            String databaseName = database._name();
            if (writeBrackets)
            {
                appendable.append('[');
            }
            appendable.append(databaseName);
            if (writeBrackets)
            {
                appendable.append(']');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
