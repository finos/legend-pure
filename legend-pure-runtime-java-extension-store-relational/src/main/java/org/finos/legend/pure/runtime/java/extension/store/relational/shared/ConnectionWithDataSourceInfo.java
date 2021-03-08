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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared;

import java.sql.Connection;

public class ConnectionWithDataSourceInfo
{
    private final Connection connection;
    private final DataSource dataSource;
    private final String additionalInfo;

    private final long creationTime = System.currentTimeMillis();

    public ConnectionWithDataSourceInfo(Connection connection, DataSource dataSource, String additionalInfo)
    {
        this.connection = connection;
        this.dataSource = dataSource;
        this.additionalInfo = additionalInfo;
    }

    public Connection getConnection()
    {
        return this.connection;
    }

    public DataSource getDataSource()
    {
        return this.dataSource;
    }

    public String getAdditionalInfo()
    {
        return this.additionalInfo;
    }

    public long getCreationTime()
    {
        return this.creationTime;
    }
}
