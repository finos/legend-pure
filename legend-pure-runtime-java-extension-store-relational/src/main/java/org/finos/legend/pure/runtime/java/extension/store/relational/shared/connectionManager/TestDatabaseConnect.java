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

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;
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

    private static final Function0<String> CONNECTION_URL = new Function0<String>()
    {
        @Override
        public String value()
        {
            return System.getProperty("legend.test.h2.port") != null ? "jdbc:h2:tcp://127.0.0.1:" + System.getProperty("legend.test.h2.port") + "/mem:testDB" : "jdbc:h2:mem:;ALIAS_COLUMN_NAME=TRUE";
        }
    };

    private static final Function0<Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource>> NEW_TEST_DS_PAIR = new CheckedFunction0<Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource>>()
    {
        @Override
        public Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource> safeValue() throws SQLException
        {
            BasicDataSource ds = new BasicDataSource();
            ds.setUrl(CONNECTION_URL.value());
            ds.setUsername("sa");
            ds.setPassword("");
            ds.setMaxTotal(1);
            ds.setMaxIdle(1);
            ThreadLocal<PerThreadPoolableConnectionWrapper> connTL = new ThreadLocal<PerThreadPoolableConnectionWrapper>();
            return Tuples.pair(connTL, ds);
        }
    };


    private static final Procedure2<String, MutableMultimap<String, DataSourceConnectionDisplayInfo>> ADD_USER_CONNECTION = new Procedure2<String, MutableMultimap<String, DataSourceConnectionDisplayInfo>>()
    {
        @Override
        public void value(String user, MutableMultimap<String, DataSourceConnectionDisplayInfo> connectionsByUser)
        {
            connectionsByUser.put(user, new DataSourceConnectionDisplayInfo(TEST_DATA_SOURCE, CONNECTION_URL.value()));//TODO - pooling info
        }
    };

    private final KeyLockManager<String> userLocks = KeyLockManager.newManager();

    public TestDatabaseConnect()
    {
        try
        {
            Class.forName("org.h2.Driver");
        }
        catch (ClassNotFoundException e)
        {
        }
    }


    public ConnectionWithDataSourceInfo getConnectionWithDataSourceInfo(String user)
    {
        PerThreadPoolableConnectionWrapper pcw;
        Pair<ThreadLocal<PerThreadPoolableConnectionWrapper>, BasicDataSource> cs;
        synchronized (this.userLocks.getLock(user))
        {
            cs = this.connectionPoolByUser.getIfAbsentPut(user, NEW_TEST_DS_PAIR);
        }
        ThreadLocal<PerThreadPoolableConnectionWrapper> tl = cs.getOne();
        pcw = tl.get();
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
            throw new PureExecutionException("Unable to create TestDatabaseConnection for user: " + user, ex);
        }

        pcw.incrementBorrowedCounter();
        return new ConnectionWithDataSourceInfo(pcw, TEST_DATA_SOURCE, "TestDatabaseConnect");
    }


    public void collectConnectionsByUser(MutableMultimap<String, DataSourceConnectionDisplayInfo> connectionsByUser)
    {
        this.connectionPoolByUser.keysView().forEachWith(ADD_USER_CONNECTION, connectionsByUser);
    }

    public RichIterable<String> getUsersWithConnections()
    {
        return this.connectionPoolByUser.keysView();
    }

    public void closeConnection(String user)
    {
    }


}
