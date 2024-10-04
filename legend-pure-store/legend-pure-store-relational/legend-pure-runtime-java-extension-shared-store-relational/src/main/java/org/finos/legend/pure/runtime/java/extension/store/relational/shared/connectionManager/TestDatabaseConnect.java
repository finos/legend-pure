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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared.connectionManager;

import java.sql.DriverManager;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tools.locks.KeyLockManager;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.DataSource;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.DataSourceConnectionDisplayInfo;

import java.sql.Connection;
import java.sql.SQLException;

public class TestDatabaseConnect extends PerThreadPoolableConnectionProvider
{
    public static final String TEST_DB_HOST_NAME = "local";
    private static final String TEST_DB_NAME = "pure-h2-test-Db";
    private static final DataSource TEST_DATA_SOURCE = new DataSource(TEST_DB_HOST_NAME, -1, TEST_DB_NAME, null);
    private final KeyLockManager<String> userLocks = KeyLockManager.newManager();

    public TestDatabaseConnect()
    {
        try
        {
            Class.forName("org.h2.Driver");
        }
        catch (ClassNotFoundException ignore)
        {
            // ignore exception about not finding the H2 driver
        }
    }


    public ConnectionWithDataSourceInfo getConnectionWithDataSourceInfo(String user)
    {
        Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource> cs;
        synchronized (this.userLocks.getLock(user))
        {
            cs = this.connectionPoolByUser.getIfAbsentPut(user, TestDatabaseConnect::newTestDataSourcePair);
        }
        ThreadLocal<PerThreadPoolableConnectionWrapper> tl = cs.getOne();
        PerThreadPoolableConnectionWrapper pcw = tl.get();
        try
        {
            if (pcw == null || pcw.isClosed())
            {
                Connection connection = cs.getTwo().getConnection();
                pcw = new PerThreadPoolableConnectionWrapper(connection, user, this);
                tl.set(pcw);
            }
        }
        catch (SQLException ex)
        {
            throw new PureExecutionException("Unable to create TestDatabaseConnection of type: H2 for user: " + user + ", message: " + ex.getMessage(), ex);
        }

        pcw.incrementBorrowedCounter();
        return new ConnectionWithDataSourceInfo(pcw, TEST_DATA_SOURCE, "TestDatabaseConnect");
    }


    public void collectConnectionsByUser(MutableMultimap<String, DataSourceConnectionDisplayInfo> connectionsByUser)
    {
        this.connectionPoolByUser.forEachKey(user -> addUserConnection(user, connectionsByUser));
    }

    public RichIterable<String> getUsersWithConnections()
    {
        return this.connectionPoolByUser.keysView();
    }

    public void closeConnection(String user)
    {
    }

    private static int getMajorVersion()
    {
        try
        {
            return DriverManager.getDriver("jdbc:h2:").getMajorVersion();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("cannot identify H2 driver major version", e);
        }
    }

    private static String getConnectionURL()
    {
        String defaultH2Properties;

        if (getMajorVersion() == 2)
        {
            defaultH2Properties = System.getProperty("legend.test.h2.properties",
                    ";NON_KEYWORDS=ANY,ASYMMETRIC,AUTHORIZATION,CAST,CURRENT_PATH,CURRENT_ROLE,DAY,DEFAULT,ELSE,END,HOUR,KEY,MINUTE,MONTH,SECOND,SESSION_USER,SET,SOME,SYMMETRIC,SYSTEM_USER,TO,UESCAPE,USER,VALUE,WHEN,YEAR;MODE=LEGACY");
        }
        else
        {
            defaultH2Properties = ";ALIAS_COLUMN_NAME=TRUE";
        }

        String port = System.getProperty("legend.test.h2.port");
        return ((port != null) ?
                ("jdbc:h2:tcp://127.0.0.1:" + port + "/mem:testDB") :
                "jdbc:h2:mem:")
                + defaultH2Properties;
    }

    private static Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource> newTestDataSourcePair()
    {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(getConnectionURL());
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setMaxTotal(1);
        ds.setMaxIdle(1);
        ThreadLocal<PerThreadPoolableConnectionWrapper> connTL = new ThreadLocal<>();
        return Tuples.pair(connTL, ds);
    }

    private static void addUserConnection(String user, MutableMultimap<String, DataSourceConnectionDisplayInfo> connectionsByUser)
    {
        connectionsByUser.put(user, new DataSourceConnectionDisplayInfo(TEST_DATA_SOURCE, getConnectionURL()));//TODO - pooling info
    }
}
