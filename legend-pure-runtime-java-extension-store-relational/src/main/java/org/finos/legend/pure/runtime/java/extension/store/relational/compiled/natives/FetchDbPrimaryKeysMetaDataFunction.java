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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives;

import java.sql.DatabaseMetaData;

public class FetchDbPrimaryKeysMetaDataFunction implements SqlFunction<DatabaseMetaData, java.sql.ResultSet>
{
    private String schemaPattern;
    private String tablePattern;

    public FetchDbPrimaryKeysMetaDataFunction(String schemaPattern, String tablePattern)
    {
        this.schemaPattern = schemaPattern;
        this.tablePattern = tablePattern;
    }

    @Override
    public java.sql.ResultSet valueOf(DatabaseMetaData databaseMetaData) throws java.sql.SQLException
    {
        return databaseMetaData.getPrimaryKeys(null, this.schemaPattern, this.tablePattern);
    }
}