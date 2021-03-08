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
import org.finos.legend.pure.m2.ds.mapping.test.GraphWalker;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class RelationalGraphWalker extends GraphWalker
{

    public RelationalGraphWalker(PureRuntime runtime, ProcessorSupport processorSupport)
    {
        super(runtime,processorSupport);
    }

    public CoreInstance getDbInstance(String dbName)
    {
        return runtime.getCoreInstance(dbName);
    }

    public CoreInstance getDefaultSchema(CoreInstance db)
    {
        return getSchema(db, "default");
    }

    public ListIterable<? extends CoreInstance> getSchemas(CoreInstance db)
    {
        return this.getMany(db, "schemas");
    }

    public CoreInstance getSchema(CoreInstance db, String schemaName)
    {
        return this.getSchemas(db).detectWith(CoreInstanceNamePropertyValuePredicate, schemaName);
    }

    public ListIterable<? extends CoreInstance> getTables(CoreInstance schema)
    {
        return this.getMany(schema, "tables");
    }

    public CoreInstance getTable(CoreInstance schema, final String tableName)
    {
        return this.getTables(schema).detectWith(CoreInstanceNamePropertyValuePredicate, tableName);
    }

    public ListIterable<? extends CoreInstance> getViews(CoreInstance schema)
    {
        return this.getMany(schema, "views");
    }

    public CoreInstance getView(CoreInstance schema, final String viewName)
    {
        return this.getViews(schema).detectWith(CoreInstanceNamePropertyValuePredicate, viewName);
    }

    public ListIterable<? extends CoreInstance> getColumns(CoreInstance table)
    {
        return this.getMany(table, "columns");
    }

    public CoreInstance getColumn(CoreInstance table, final String columnName)
    {
        return this.getColumns(table).detectWith(CoreInstanceNamePropertyValuePredicate, columnName);
    }

    public CoreInstance getColumnType(CoreInstance column)
    {
        return this.getOne(column, "type");
    }

    public int getColumnSize(CoreInstance column)
    {
        return Integer.valueOf(this.getOne(this.getColumnType(column), "size").getName());
    }

    public ListIterable<? extends CoreInstance> getJoins(CoreInstance db)
    {
        return this.getMany(db, "joins");
    }

    public CoreInstance getJoin(CoreInstance db, String joinName)
    {
        return this.getJoins(db).detectWith(CoreInstanceNamePropertyValuePredicate, joinName);
    }

    public ListIterable<? extends CoreInstance> getJoinAliases(CoreInstance join)
    {
        return this.getMany(join, "aliases");
    }

    public CoreInstance getJoinAliasFirst(CoreInstance alias)
    {
        return this.getOne(alias, "first");
    }

    public CoreInstance getJoinAliasSecond(CoreInstance alias)
    {
        return this.getOne(alias, "second");
    }

    public CoreInstance getJoinOperation(CoreInstance join)
    {
        return this.getOne(join, "operation");
    }

    public CoreInstance getJoinOperationLeft(CoreInstance joinOperation)
    {
        return this.getOne(joinOperation, "left");
    }

    public CoreInstance getJoinOperationRight(CoreInstance joinOperation)
    {
        return this.getOne(joinOperation, "right");
    }

    public ListIterable<? extends CoreInstance> getJoinOperationParameters(CoreInstance joinOperation)
    {
        return this.getMany(joinOperation, "parameters");
    }

    public CoreInstance getJoinOperationAlias(CoreInstance joinOperation)
    {
        return this.getOne(joinOperation, "alias");
    }

    public CoreInstance getJoinOperationRelationalElement(CoreInstance joinOperation)
    {
        return this.getOne(joinOperation, "column");
    }

    public CoreInstance getClassMappingImplementationMainTable(CoreInstance classMappingImpl)
    {
        return this.getOne(classMappingImpl, "mainTableAlias").getValueForMetaPropertyToOne(M2RelationalProperties.relationalElement);
    }

    public CoreInstance getClassMappingImplementationPropertyMappingRelationalOperationElement(CoreInstance classMappingImplPropMapping)
    {
        return this.getOne(classMappingImplPropMapping, "relationalOperationElement");
    }

    public CoreInstance getRelationalOperationElementJoinTreeNode(CoreInstance relationalOperationElementWithJoin)
    {
        return this.getOne(relationalOperationElementWithJoin, "joinTreeNode");
    }

    public String getRelationalOperationElementJoinTreeNodeJoinName(CoreInstance joinTreeNode)
    {
        return this.getOne(joinTreeNode, "joinName").getName();
    }

    public String getTableAliasColumnAliasName(CoreInstance tableAliasColumn)
    {
        return this.getName(this.getOne(tableAliasColumn, "alias"));
    }

    public String getTableAliasColumnColumnName(CoreInstance tableAliasColumn)
    {
        return this.getName(this.getOne(tableAliasColumn, "column"));
    }

}
