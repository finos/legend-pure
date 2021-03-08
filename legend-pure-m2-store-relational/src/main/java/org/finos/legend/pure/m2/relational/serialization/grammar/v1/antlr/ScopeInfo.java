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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr;

import org.antlr.v4.runtime.Token;

class ScopeInfo
{
    private String database;
    private Token schema;
    private Token tableAlias;
    private Token column;
    private boolean parseView;

    public ScopeInfo(String database, Token schema, Token tableAlias, Token column, boolean parseView)
    {
        this(database, schema, tableAlias, column);
        this.parseView=parseView;
    }

    public ScopeInfo(String database, Token schema, Token tableAlias, Token column)
    {
        this.database = database;
        this.schema = schema;
        this.tableAlias = tableAlias;
        this.column = column;
    }

    @Override
    public String toString()
    {
        return "ScopeInfo{" +
                "database='" + database + "'" +
                ", schema=" + schema +
                ", tableAlias=" + tableAlias +
                ", column=" + column +
                '}';
    }

    public String getDatabase()
    {
        return this.database;
    }

    public Token getSchema()
    {
        return this.schema;
    }

    public Token getTableAlias()
    {
        return this.tableAlias;
    }

    public Token getColumn()
    {
        return this.column;
    }

    public boolean isParseView()
    {
        return this.parseView;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public void setSchema(Token schema)
    {
        this.schema = schema;
    }

    public void setTableAlias(Token tableAlias)
    {
        this.tableAlias = tableAlias;
    }

    public void setColumn(Token column)
    {
        this.column = column;
    }

    public void setParseView(boolean parseView)
    {
        this.parseView = parseView;
    }
}
