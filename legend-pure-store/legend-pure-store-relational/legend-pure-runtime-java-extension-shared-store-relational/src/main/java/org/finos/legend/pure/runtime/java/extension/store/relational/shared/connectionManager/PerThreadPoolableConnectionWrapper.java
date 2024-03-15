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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class PerThreadPoolableConnectionWrapper extends ConnectionWrapper
{
    Logger logger = LoggerFactory.getLogger(PerThreadPoolableConnectionWrapper.class);

    private int borrowedCounter;
    private PerThreadPoolableConnectionProvider provider;
    String user;

    public PerThreadPoolableConnectionWrapper(Connection connection, String user, PerThreadPoolableConnectionProvider provider)
    {
        super(connection);
        this.provider = provider;
        this.user = user;
    }

    public void incrementBorrowedCounter()
    {
        borrowedCounter++;
    }

    private void decrementBorrowedCounter()
    {
        borrowedCounter--;
    }

    @Override
    public void close() throws SQLException
    {
        this.decrementBorrowedCounter();
        if (borrowedCounter <= 0)
        {
            this.closeConnection();
            this.provider.removePerThreadConnections(user);
        }
    }
}
