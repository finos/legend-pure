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
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractCacheNextReadOnceForwardOnly;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListener;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListenerState;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionListeners;
import org.finos.legend.pure.runtime.java.extension.store.relational.RelationalNativeImplementation;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetValueHandlers.ResultSetValueHandler;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.ConnectionWithDataSourceInfo;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.SQLExceptionHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.GregorianCalendar;
import java.util.TimeZone;

class CacheNextReadOnceForwardOnlyResultSet extends AbstractCacheNextReadOnceForwardOnly implements ExecutionEndListener
{
    private Connection connection;
    private ResultSet resultSet;
    private Statement statement;
    private final Function<RichIterable<Object>, ? extends CoreInstance> processRowFunction;
    private final CoreInstance sqlNull;
    private final GregorianCalendar calendar;
    private final ListIterable<ResultSetValueHandler> handlers;

    private static final int CACHE_MAX_SIZE = 1000;

    private final ExecutionListeners executionListeners;
    private ConnectionWithDataSourceInfo dataSourceInfo;
    private final ExecutionActivityListener executionActivityListener;
    private String executedSQL;

    private CacheNextReadOnceForwardOnlyResultSet(Connection connection, ResultSet resultSet, Statement statement,
                                                  Function<RichIterable<Object>, ? extends CoreInstance> processRowFunction,
                                                  CoreInstance sqlNull, String tz,
                                                  ListIterable<ResultSetValueHandler> handlers, CompiledExecutionSupport executionSupport,
                                                  ConnectionWithDataSourceInfo dataSourceInfo, String executedSQL)
    {
        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.processRowFunction = processRowFunction;
        this.sqlNull = sqlNull;
        this.handlers = handlers;
        this.calendar = new GregorianCalendar(TimeZone.getTimeZone(tz));
        this.executionListeners = executionSupport.getExecutionListeners();
        this.dataSourceInfo = dataSourceInfo;
        this.executionActivityListener = executionSupport.getExecutionActivityListener();
        this.executedSQL = executedSQL;
        executionSupport.registerExecutionEndListener(this);


    }

    @Override
    protected void readNext()
    {
        try
        {

            //int row = this.resultSet.getRow();
            //System.out.println("Processing Row:" + row);
            /*if (row % 100 == 0)
            {
                System.out.println("Processing Row:" + row);
            }*/

            if (this.resultSet.next())
            {
                RichIterable<Object> values = RelationalNativeImplementation.processRow(this.resultSet, this.handlers, this.sqlNull, this.calendar);
                this.next = this.processRowFunction.valueOf(values);

                if (this.currentIndex < CACHE_MAX_SIZE)
                {
                    this.cachedResults.add(this.next);
                }
            }
            else
            {
                this.closeAndCleanUp();
                //Deliberately set this to null
                this.next = null;
            }
        }
        catch (SQLException e)
        {
            //build this first, while we still have the connection information
            String hostname = "";
            Integer port = -1;
            String databaseName = "";
            try
            {
                if (dataSourceInfo.getDataSource() != null)
                {
                    hostname = dataSourceInfo.getDataSource().getHost();
                    port = dataSourceInfo.getDataSource().getPort();
                    databaseName = dataSourceInfo.getDataSource().getDataSourceName();
                }
                executionActivityListener.relationalActivityCompleted(hostname, port, databaseName, "", executedSQL, "", 0L, 0L, 0L);
            }
            catch (Exception logException)
            {
            }

            String error = SQLExceptionHandler.buildExceptionString(e, this.connection);
            //close and clean up the connections
            this.closeAndCleanUp();
            throw new PureExecutionException(error, e);
        }
    }

    @Override
    protected String streamingExceptionMessage()
    {
        return "Trying to process rows from a streaming database resultset more than once for a resultset that contains more than " + CACHE_MAX_SIZE + " rows. The best practice is to maximise processing inside one execute function to push processing to the data server where possible.";
    }

    private void closeAndCleanUp()
    {

        SQLExceptionHandler.closeAndCleanUp(this.resultSet, this.statement, this.connection);

        //Clean-up in case the iterable is still being held onto
        this.resultSet = null;
        this.statement = null;
        this.connection = null;

        this.executionListeners.unregisterExecutionEndListener(this);
    }


    @Override
    public ExecutionEndListenerState executionEnd(Exception exception)
    {
        //Always close and clean-up, it is possible that the user never reads the results
        this.closeAndCleanUp();
        return new ExecutionEndListenerState(false);
    }

    static CacheNextReadOnceForwardOnlyResultSet create(Connection connection, ResultSet resultSet, Statement statement,
                                                        Function<RichIterable<Object>, ? extends CoreInstance> processRowFunction,
                                                        CoreInstance sqlNull, String tz,
                                                        ListIterable<ResultSetValueHandler> handlers, CompiledExecutionSupport executionSupport,
                                                        ConnectionWithDataSourceInfo dataSourceInfo,
                                                        String executedSQL)
    {
        CacheNextReadOnceForwardOnlyResultSet result = new CacheNextReadOnceForwardOnlyResultSet(connection, resultSet, statement, processRowFunction, sqlNull, tz, handlers, executionSupport, dataSourceInfo, executedSQL);
        //Force it to initialize the first element and fully run the query. Some drivers do not run the DB query until you call next.

        result.readNext();


        //Peek at next element, this will force the rs & statement clean-up if there is only one element
        boolean look = result.hasNext(0) && result.hasNext(1);

        return result;
    }
}
