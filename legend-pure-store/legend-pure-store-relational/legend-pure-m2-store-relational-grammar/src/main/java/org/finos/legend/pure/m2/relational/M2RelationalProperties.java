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

import org.finos.legend.pure.m3.navigation.M3Properties;

/**
 * Standard property name for the M2 Relational model.
 */
public class M2RelationalProperties
{
    private M2RelationalProperties()
    {
    }

    public static final String alias = "alias";
    public static final String aliases = "aliases";
    public static final String businessFrom = "from";
    public static final String businessThru = "thru";
    public static final String processingIn = "in";
    public static final String processingOut = "out";
    public static final String column = "column";
    public static final String columns = "columns";
    public static final String columnMappings = "columnMappings";
    public static final String columnName = "columnName";
    public static final String database = "database";
    public static final String distinct = "distinct";
    public static final String filter = "filter";
    public static final String filterName = "filterName";
    public static final String filters = "filters";
    public static final String groupBy = "groupBy";
    public static final String includes = M3Properties.includes;
    public static final String join = "join";
    public static final String joinName = "joinName";
    public static final String joins = "joins";
    public static final String joinTreeNode = "joinTreeNode";
    public static final String joinType = "joinType";
    public static final String left = "left";
    public static final String mainTableAlias = "mainTableAlias";
    public static final String milestoning = "milestoning";
    public static final String namespaces = "namespaces";
    public static final String nested = "nested";
    public static final String nullable = "nullable";
    public static final String operation = "operation";
    public static final String primaryKey = "primaryKey";
    public static final String propertyMappings = "propertyMappings";
    public static final String relationalElement = "relationalElement";
    public static final String relationalOperationElement = "relationalOperationElement";
    public static final String relations = "relations";
    public static final String resolved = "resolved";
    public static final String right = "right";
    public static final String schema = "schema";
    public static final String schemas = "schemas";
    public static final String setColumns = "setColumns";
    public static final String setMappingOwner = "setMappingOwner";
    public static final String tables = "tables";
    public static final String transformer = "transformer";
    public static final String userDefinedPrimaryKey = "userDefinedPrimaryKey";
    public static final String views = "views";
}
