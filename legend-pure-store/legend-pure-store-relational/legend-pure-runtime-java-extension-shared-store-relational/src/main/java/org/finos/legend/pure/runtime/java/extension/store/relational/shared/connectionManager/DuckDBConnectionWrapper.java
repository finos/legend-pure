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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DuckDBConnectionWrapper extends ConnectionWrapper
{
    Logger logger = LoggerFactory.getLogger(DuckDBConnectionWrapper.class);

    private int borrowedCounter;
    String user;

    public DuckDBConnectionWrapper(Connection connection, String user)
    {
        super(connection);
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
        //never actually close duck db connection and re-use same connection over
//        if (borrowedCounter <= 0)
//        {
//            this.closeConnection();
//        }
    }
}
