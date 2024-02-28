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

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.IConnectionManagerHandler;

import java.sql.Connection;
import java.sql.Statement;

public class ConnectionManagerHandler implements IConnectionManagerHandler
{
    @Override
    public ConnectionWithDataSourceInfo getConnectionWithDataSourceInfo(CoreInstance connectionInformation, ProcessorSupport processorSupport)
    {
        return ConnectionManager.getConnectionWithDataSourceInfo(connectionInformation, processorSupport);
    }

    @Override
    public void registerStatement(Statement statement, String sql, int fetchSize, int queryTimeoutSeconds)
    {
        ConnectionManager.registerStatement(statement, sql, fetchSize, queryTimeoutSeconds);
    }

    @Override
    public void unregisterStatement(final Statement statement)
    {
        ConnectionManager.unregisterStatement(statement);
    }

    @Override
    public void addPotentialDebug(CoreInstance connectionInformation, Statement statement)
    {
    }

    @Override
    public String getPotentialDebug(CoreInstance connectionInformation, Connection connection)
    {
        return null;
    }
}
