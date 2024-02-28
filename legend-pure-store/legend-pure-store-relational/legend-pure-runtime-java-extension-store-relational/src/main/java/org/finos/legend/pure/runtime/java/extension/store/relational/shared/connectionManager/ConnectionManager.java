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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.SynchronizedMutableList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.shared.identity.IdentityManager;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;

import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionManager
{
    private static final Function0<SynchronizedMutableList<ConnectionManager.StatementProperties>> NEW_STATEMENT_LIST = new Function0<SynchronizedMutableList<ConnectionManager.StatementProperties>>()
    {
        @Override
        public SynchronizedMutableList<ConnectionManager.StatementProperties> value()
        {
            return SynchronizedMutableList.of(Lists.mutable.<ConnectionManager.StatementProperties>with());
        }
    };

    private static final Procedure<ConnectionManager.StatementProperties> CANCEL_STATEMENT = new Procedure<ConnectionManager.StatementProperties>()
    {
        @Override
        public void value(ConnectionManager.StatementProperties statement)
        {
            try
            {
                statement.getStatement().cancel();
            }
            catch (SQLException e)
            {
                // Ignore exception
            }
        }
    };

    private static final Predicate2<ConnectionManager.StatementProperties, Statement> STATEMENT_EQUALITY_PREDICATE = new Predicate2<ConnectionManager.StatementProperties, Statement>()
    {
        @Override
        public boolean accept(ConnectionManager.StatementProperties each, Statement statement)
        {
            return each.getStatement() == statement;
        }
    };

    private static final ConcurrentMutableMap<String, SynchronizedMutableList<ConnectionManager.StatementProperties>> statements = ConcurrentHashMap.newMap();

    private static final String TestDatabaseConnection = "meta::external::store::relational::runtime::TestDatabaseConnection";
    private static final TestDatabaseConnect testDatabaseConnect = new TestDatabaseConnect();

    private ConnectionManager()
    {
    }

    public static ConnectionWithDataSourceInfo getConnectionWithDataSourceInfo(CoreInstance connectionInformation, ProcessorSupport processorSupport)
    {
        if (processorSupport.instance_instanceOf(connectionInformation, TestDatabaseConnection))
        {
            return testDatabaseConnect.getConnectionWithDataSourceInfo(IdentityManager.getAuthenticatedUserId());
        }

        throw new RuntimeException(connectionInformation + " is not supported for execution!!");
    }

    public static void closeConnections(String userId)
    {
        cancelStatements(userId);
    }

    //PLEASE NOTE THE FETCH SIZE AND QUERY TIMEOUT ARE DELIBERATELY PASSED IN RATHER THAN GETTING THEM FROM THE STATEMENT
    //GETTOR CALLS ON THE STATEMENT BLOCK ON THE DB
    public static void registerStatement(Statement statement, String sql, int fetchSize, int queryTimeoutSeconds)
    {
        statements.getIfAbsentPut(IdentityManager.getAuthenticatedUserId(), NEW_STATEMENT_LIST).add(new ConnectionManager.StatementProperties(statement, sql, fetchSize, queryTimeoutSeconds));
    }


    public static void unregisterStatement(final Statement statement)
    {
        statements.getIfAbsentPut(IdentityManager.getAuthenticatedUserId(), NEW_STATEMENT_LIST).removeIfWith(STATEMENT_EQUALITY_PREDICATE, statement);
    }

    public static void cancelStatements(String userId)
    {
        SynchronizedMutableList<ConnectionManager.StatementProperties> userStatements = statements.remove(userId);
        if (userStatements != null)
        {
            userStatements.forEach(CANCEL_STATEMENT);
        }
    }

    public static class StatementProperties
    {
        private final String sql;

        //PLEASE NOTE THESE ARE DELIBERATELY PASSED IN RATHER THAN GETTING THEM FROM THE STATEMENT
        //GETTOR CALLS ON THE STATEMENT BLOCK ON THE DB
        private final int fetchSize;
        private final int queryTimeoutSeconds;

        private final Statement statement;

        private final long startTime = System.currentTimeMillis();

        private StatementProperties(Statement statement, String sql, int fetchSize, int queryTimeoutSeconds)
        {
            this.sql = sql;
            this.statement = statement;
            this.fetchSize = fetchSize;
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public String getSql()
        {
            return this.sql;
        }

        public Statement getStatement()
        {
            return this.statement;
        }

        public int getFetchSize()
        {
            return this.fetchSize;
        }

        public int getQueryTimeoutSeconds()
        {
            return this.queryTimeoutSeconds;
        }

        public long getStartTime()
        {
            return this.startTime;
        }

        @Override
        public String toString()
        {
            return "StatementProperties{" +
                    "sql='" + this.sql.substring(0, 100) + '\'' +
                    ", fetchSize=" + this.fetchSize +
                    ", queryTimeoutSeconds=" + this.queryTimeoutSeconds +
                    ", startTime=" + String.format("%1tY-%<tm-%<td %<tH:%<tM:%<tS.%<tL %<tZ", this.startTime) +
                    '}';
        }
    }
}
