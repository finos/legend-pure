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

package org.finos.legend.pure.runtime.java.extension.store.relational;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.exception.PureExecutionException;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoadToDbTableHelper
{
    public static Iterable<ListIterable<?>> collectIterable(LazyIterable iterable, final ListIterable<String> columnTypes, final String filePath, final String tableName)
    {
        return iterable.collect(new Function<Iterable<String>, ListIterable<?>>()
        {
            @Override
            public ListIterable<?> valueOf(Iterable<String> csvRecord)
            {
                MutableList<Object> result = FastList.newList();
                int i = 0;
                for (String str : csvRecord)
                    try
                    {
                        String type = columnTypes.get(i);
                        if (StringIterate.isEmpty(str))
                        {
                            result.add(null);
                        }
                        else if ("Integer".equals(type))
                        {
                            result.add(Long.valueOf(str));
                        }
                        else
                        {
                            result.add(str);
                        }
                        i++;
                    }
                    catch (NumberFormatException ex)
                    {
                        throw new PureExecutionException("Failed to load CSV file " + filePath + " into DB table " + tableName +
                                ".\n Table requires a " + columnTypes.get(i) + " for column number " + i + ". CSV row:" +
                                (csvRecord instanceof CSVRecord ? ((CSVRecord)csvRecord).getRecordNumber() : "N/A") + " column:" + (i + 1) +
                                " failed to convert to " + columnTypes.get(i) + " with error '" + ex.getMessage() + "'", ex);
                    }
                return result;
            }
        });
    }

    public static StringBuilder buildInsertStatementHeader(String schemaName, String tableName, ListIterable<String> columnNames)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(("default".equals(schemaName)) ? tableName : (schemaName + "." + tableName));
        sql.append(columnNames.makeString("(", ",", ")"));
        sql.append(" values (");
        for (int i = 0; i < columnNames.size() - 1; i++)
        {
            sql.append("?,");
        }
        sql.append("?)");
        return sql;
    }

    public static int[] insertBatch(Iterable<? extends Iterable<?>> values, PreparedStatement statement) throws SQLException
    {
        ParameterMetaData metaData = statement.getParameterMetaData();

        for (Iterable<?> row : values)
        {
            int i = 1;
            for (Object object : row)
            {
                if (object == null)
                {
                    statement.setNull(i, metaData.getParameterType(i));
                }
                else
                {
                    statement.setObject(i, object, metaData.getParameterType(i));
                }
                i++;
            }
            statement.addBatch();
            statement.clearParameters();
        }

        return statement.executeBatch();
    }
}
