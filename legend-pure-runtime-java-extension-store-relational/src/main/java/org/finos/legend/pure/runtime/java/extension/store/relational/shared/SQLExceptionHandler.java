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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLExceptionHandler
{
    private static final IConnectionManagerHandler connectionManagerHandler = IConnectionManagerHandler.CONNECTION_MANAGER_HANDLER;

    private SQLExceptionHandler()
    {
    }

    public static void closeAndCleanUp(ResultSet resultSet, Statement statement, Connection connection)
    {
        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (SQLException ex)
            {
                //Nothing we can do
                ex.printStackTrace();
            }


        }

        if (statement != null)
        {
            connectionManagerHandler.unregisterStatement(statement);
            try
            {
                statement.close();
            }
            catch (SQLException ex)
            {
                //Nothing we can do
                ex.printStackTrace();
            }
            statement = null;
        }

        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException ex)
            {
                //Nothing we can do
                ex.printStackTrace();
            }
            //Force these to null in case this is being held onto in a ResultSetIterator in the graph
            connection = null;
        }
    }

    public static String buildExceptionString(SQLException e, Connection connection)
    {
        StringBuilder builder = new StringBuilder(512);
        builder.append("Error executing sql query");
        builder.append("; SQL reason: ");
        builder.append(e.getMessage());
        builder.append("; SQL error code: ");
        builder.append(e.getErrorCode());
        builder.append("; SQL state: ");
        builder.append(e.getSQLState());
        try
        {
            if (connection != null && connection.getMetaData() != null)
            {
                builder.append("; DB URL: ");
                String url = connection.getMetaData().getURL();
                builder.append(extractHostName(url));
                builder.append("; User name: ");
                builder.append(connection.getMetaData().getUserName());
            }
        }
        catch (Exception ignore)
        {
            //ignore
        }
        return builder.toString();
    }

    private static String extractHostName(String url)
    {
        String host = "";
        if (url.contains("jdbc:sqlanywhere"))
        {
            String[] urlTokens = url.split(";");
            for (String token : urlTokens)
            {
                if (token.startsWith("LINKS=TCPIP{host"))
                {
                    host += token.substring("LINKS=TCPIP{".length(), token.length() - 1) + ";";
                }
                else if (token.startsWith("ServerName"))
                {
                    host += token + ";";
                }
            }
        }
        else
        {
            host = url;
        }
        return host;
    }
}
