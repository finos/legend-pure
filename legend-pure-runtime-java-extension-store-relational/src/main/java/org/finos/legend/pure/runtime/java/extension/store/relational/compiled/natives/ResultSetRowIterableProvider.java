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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m3.tools.MetricsRecorder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.ResultLazyIterable;
import org.finos.legend.pure.runtime.java.shared.canstreamstate.CanStreamState;
import org.finos.legend.pure.runtime.java.extension.store.relational.RelationalNativeImplementation;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.IConnectionManagerHandler;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.PureConnectionUtils;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.SQLExceptionHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Runs the database query and provides the appropriate iterator, streaming or a normal ListIterable
 */
public class ResultSetRowIterableProvider
{
    private static final IConnectionManagerHandler connectionManagerHandler = IConnectionManagerHandler.CONNECTION_MANAGER_HANDLER;

    private ResultSetRowIterableProvider()
    {
    }

    public static ResultSetIterableContainer createResultSetIterator(CoreInstance pureConnection, Connection connection, String sql, int maxRows, boolean shouldThrowIfMaxRowsExceeded,
                                                                     int queryTimeoutInSeconds, int fetchSize,
                                                                     Function<RichIterable<Object>, ? extends CoreInstance> processRowFunction, CoreInstance sqlNull,
                                                                     String tz, SourceInformation sourceInformation, CompiledExecutionSupport executionSupport, ConnectionWithDataSourceInfo dataSourceInfo)
    {
        ExecutionActivityListener listener = executionSupport.getExecutionActivityListener();
        String hostname = "";
        Integer port = -1;
        String databaseName = "";
        Statement statement = null;
        try
        {
            long startTimeInNanos = System.nanoTime();
            statement = connection.createStatement();
            if (!CanStreamState.canStream() && maxRows > 0)
            {
                statement.setMaxRows(maxRows + 1);
            }

            if (!PureConnectionUtils.isPureConnectionType(pureConnection, "Hive"))
            {
                statement.setQueryTimeout(queryTimeoutInSeconds);
            }

            int actualFetchSize = maxRows > 0 ? Math.min(fetchSize, maxRows) : fetchSize;
            statement.setFetchSize(actualFetchSize);
            connectionManagerHandler.addPotentialDebug(pureConnection, statement);
            connectionManagerHandler.registerStatement(statement, sql, actualFetchSize, queryTimeoutInSeconds);
            MutableList<String> columns = FastList.newList();
            ;

            try
            {
                MetricsRecorder.incrementRelationalExecutionCounters();
                if (statement.execute(sql))
                {
                    ResultSet rs = statement.getResultSet();
                    ResultSetMetaData metaData = rs.getMetaData();

                    int count = metaData.getColumnCount();
                    for (int i = 1; i <= count; i++)
                    {
                        String column = metaData.getColumnLabel(i);
                        columns.add(column);
                    }

                    ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(metaData);
                    RichIterable<CoreInstance> theResults;

                    if (CanStreamState.canStream())
                    {
                        CacheNextReadOnceForwardOnlyResultSet resultSet = CacheNextReadOnceForwardOnlyResultSet.create(connection, rs, statement, processRowFunction, sqlNull, tz, handlers, executionSupport, dataSourceInfo, sql);
                        theResults = new ResultLazyIterable(resultSet);
                    }
                    else
                    {
                        MutableList<CoreInstance> results = Lists.mutable.of();
                        int rowCount = 0;

                        try
                        {
                            GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(tz));
                            boolean showCheckMaxRows = shouldThrowIfMaxRowsExceeded && maxRows > 0;
                            while (rs.next())
                            {
                                rowCount++;
                                CoreInstance row = processRowFunction.valueOf(RelationalNativeImplementation.processRow(rs, handlers, sqlNull, calendar));
                                results.add(row);

                                if (showCheckMaxRows && rowCount > maxRows)
                                {
                                    throw new PureExecutionException("Too many rows returned. PURE currently supports results with up to " + maxRows + " rows. Please add a filter or use the take or limit function to limit the rows returned");
                                }
                            }
                        }
                        finally
                        {
                            SQLExceptionHandler.closeAndCleanUp(rs, statement, connection);
                        }
                        theResults = results;
                    }

                    return new ResultSetIterableContainer(theResults, columns, startTimeInNanos);
                }
                else
                {

                    //There are no results, but we should close the statement and release the connection
                    SQLExceptionHandler.closeAndCleanUp(null, statement, connection);

                    return new ResultSetIterableContainer(startTimeInNanos);
                }
            }
            finally
            {
                MetricsRecorder.decrementCurrentRelationalExecutionCounter();
            }

        }
        catch (SQLException e)
        {
            if (statement != null)
            {
                try
                {
                    connectionManagerHandler.unregisterStatement(statement);
                    statement.close();
                }
                catch (SQLException e1)
                {
                    //Nothing we can do
                }
            }

            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException e1)
                {
                    //Nothing we can do
                }
            }

            try
            {
                if (dataSourceInfo.getDataSource() != null)
                {
                    hostname = dataSourceInfo.getDataSource().getHost();
                    port = dataSourceInfo.getDataSource().getPort();
                    databaseName = dataSourceInfo.getDataSource().getDataSourceName();
                }
                listener.relationalActivityCompleted(hostname, port, databaseName, "", sql, "", 0L, 0L, 0L);
            }
            catch (Exception logException)
            {
            }

            throw new PureExecutionException(sourceInformation, SQLExceptionHandler.buildExceptionString(e, connection), e);
        }
    }


    public static class ResultSetIterableContainer
    {
        public final RichIterable rowIterable;
        public final RichIterable<String> columnNames;
        public final long queryTimeInNanos;

        ResultSetIterableContainer(long startTimeInNanos)
        {
            this(Lists.fixedSize.empty(), Lists.fixedSize.<String>empty(), startTimeInNanos);
        }

        ResultSetIterableContainer(RichIterable rowIterable, RichIterable<String> columnNames, long startTimeInNanos)
        {
            this.rowIterable = rowIterable;
            this.columnNames = columnNames;
            this.queryTimeInNanos = System.nanoTime() - startTimeInNanos;
        }
    }
}
