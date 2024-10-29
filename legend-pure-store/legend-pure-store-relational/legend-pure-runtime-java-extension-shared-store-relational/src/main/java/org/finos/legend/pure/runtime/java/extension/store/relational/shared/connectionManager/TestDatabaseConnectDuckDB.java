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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared.connectionManager;

import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.DataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDatabaseConnectDuckDB
{
    private DuckDBConnectionWrapper singletonConnection;         // disabled thread-level and user-level segregation,
    public static final String TEST_DB_HOST_NAME = "local";
    private static final String TEST_DB_NAME = "pure-duckDB-test-Db";
    private static final DataSource TEST_DATA_SOURCE = new DataSource(TEST_DB_HOST_NAME, -1, TEST_DB_NAME, null);

    public TestDatabaseConnectDuckDB()
    {
        try
        {
            Class.forName("org.duckdb.DuckDBDriver");
        }
        catch (ClassNotFoundException ignore)
        {
            // ignore exception about not finding the duckDB driver
        }
    }

    public ConnectionWithDataSourceInfo getConnectionWithDataSourceInfo(String user)
    {
        try
        {
            if (this.singletonConnection == null || this.singletonConnection.isClosed())
            {
                Connection connection = DriverManager.getConnection(getConnectionURL());
                //there is no way to configure timezone as jdbc url param or system properties
                connection.createStatement().execute("SET TimeZone = 'UTC'");

                this.singletonConnection = new DuckDBConnectionWrapper(connection, user);
            }
        }
        catch (SQLException ex)
        {
            throw new PureExecutionException("Unable to create TestDatabaseConnection of type: DuckDB for user: " + user + ", message: " + ex.getMessage(), ex, Stacks.mutable.empty());
        }
        return new ConnectionWithDataSourceInfo(this.singletonConnection, TEST_DATA_SOURCE, this.getClass().getSimpleName());
    }


    private static String getConnectionURL()
    {
        return "jdbc:duckdb:" + TEST_DB_NAME;
    }

}
