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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * The purpose of this wrapper is to hijack the close method so
 * that it does not actually close the connection.  This provides
 * some safety in connection pooling so that consumers do not
 * accidentally close connections that are being pooled.
 */
public class ConnectionWrapper implements Connection
{
    private final Connection connection;

    public ConnectionWrapper(Connection connection)
    {
        this.connection = connection;
    }

    public void closeConnection() throws SQLException
    {
        this.connection.close();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
        this.connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public Statement createStatement() throws SQLException
    {
        return this.connection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return this.connection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        return this.connection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException
    {
        return this.connection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        this.connection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return this.connection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException
    {
        this.connection.commit();
    }

    @Override
    public void rollback() throws SQLException
    {
        this.connection.rollback();
    }

    @Override
    public void close() throws SQLException
    {
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return this.connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return this.connection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        this.connection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        return this.connection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException
    {
        this.connection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException
    {
        return this.connection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException
    {
        this.connection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return this.connection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return this.connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        this.connection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return this.connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return this.connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        this.connection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException
    {
        this.connection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return this.connection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        return this.connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException
    {
        return this.connection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException
    {
        this.connection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        this.connection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return this.connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return this.connection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return this.connection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return this.connection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException
    {
        return this.connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        return this.connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        return this.connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return this.connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException
    {
        return this.connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        this.connection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        this.connection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException
    {
        return this.connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        return this.connection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return this.connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return this.connection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException
    {
        this.connection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException
    {
        return this.connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException
    {
        this.connection.abort(executor);
    }

    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return this.connection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return this.connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return this.connection.isWrapperFor(iface);
    }

    public Connection getConnection()
    {
        return this.connection;
    }
}
