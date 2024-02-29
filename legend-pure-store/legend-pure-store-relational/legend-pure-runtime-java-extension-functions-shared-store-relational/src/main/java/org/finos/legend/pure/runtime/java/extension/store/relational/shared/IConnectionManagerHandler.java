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

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.connectionManager.ConnectionManagerHandler;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public interface IConnectionManagerHandler
{
    ImmutableList<IConnectionManagerHandler> CONNECTION_MANAGER_HANDLERS = Lists.immutable.withAll(ServiceLoader.load(IConnectionManagerHandler.class));
    IConnectionManagerHandler CONNECTION_MANAGER_HANDLER = getHandler();

    static IConnectionManagerHandler getHandler()
    {
        if (CONNECTION_MANAGER_HANDLERS.size() == 1)
        {
            return CONNECTION_MANAGER_HANDLERS.get(0);
        }
        if (CONNECTION_MANAGER_HANDLERS.isEmpty())
        {
            return new ConnectionManagerHandler();
        }
        throw new RuntimeException("Multiple ConnectionManagerHandler present in scope - " + CONNECTION_MANAGER_HANDLERS.stream().map(handler -> handler.getClass().getName()).collect(Collectors.joining(",", "[", "]")));
    }

    ConnectionWithDataSourceInfo getConnectionWithDataSourceInfo(CoreInstance connectionInformation, ProcessorSupport processorSupport);

    void registerStatement(Statement statement, String sql, int fetchSize, int queryTimeoutSeconds);

    void unregisterStatement(final Statement statement);

    void addPotentialDebug(CoreInstance connectionInformation, Statement statement);

    String getPotentialDebug(CoreInstance connectionInformation, Connection connection);
}
