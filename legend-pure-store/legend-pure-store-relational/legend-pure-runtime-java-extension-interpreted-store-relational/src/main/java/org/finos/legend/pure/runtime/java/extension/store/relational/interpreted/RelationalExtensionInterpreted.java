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

package org.finos.legend.pure.runtime.java.extension.store.relational.interpreted;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.CreateTempTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.DropTempTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.ExecuteInDb;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.FetchDbColumnsMetadata;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.FetchDbImportedKeysMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.FetchDbPrimaryKeysMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.FetchDbSchemasMetadata;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.FetchDbTablesMetadata;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.LoadCsvToDbTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.LoadValuesToDbTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.LoadValuesToDbTableNew;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.LogActivities;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class RelationalExtensionInterpreted extends BaseInterpretedExtension
{
    public RelationalExtensionInterpreted()
    {
        super(Tuples.pair("loadCsvToDbTable_String_1__Table_1__DatabaseConnection_1__Integer_$0_1$__Nil_0_", (e, r) -> new LoadCsvToDbTable(e.getStorage(), r, e.getMessage())),
                Tuples.pair("loadValuesToDbTable_List_MANY__Table_1__DatabaseConnection_1__Nil_0_", (e, r) -> new LoadValuesToDbTable(r, e.getMessage())),
                Tuples.pair("loadValuesToDbTable_List_1__Table_1__DatabaseConnection_1__Nil_0_", (e, r) -> new LoadValuesToDbTableNew(r, e.getMessage())),
                Tuples.pair("createTempTable_String_1__Column_MANY__Function_1__DatabaseConnection_1__Nil_0_", (e, r) -> new CreateTempTable(r, e, e.getMessage())),
                Tuples.pair("createTempTable_String_1__Column_MANY__Function_1__Boolean_1__DatabaseConnection_1__Nil_0_", (e, r) -> new CreateTempTable(r, e, e.getMessage())),
                Tuples.pair("dropTempTable_String_1__DatabaseConnection_1__Nil_0_", (e, r) -> new DropTempTable(r, e.getMessage())),
                Tuples.pair("executeInDb_String_1__DatabaseConnection_1__Integer_1__Integer_1__ResultSet_1_", (e, r) -> new ExecuteInDb(r, e.getMessage(), e.getMaxSQLRows())),
                Tuples.pair("fetchDbTablesMetaData_DatabaseConnection_1__String_$0_1$__String_$0_1$__ResultSet_1_", (e, r) -> new FetchDbTablesMetadata(r, e.getMessage(), e.getMaxSQLRows())),
                Tuples.pair("fetchDbColumnsMetaData_DatabaseConnection_1__String_$0_1$__String_$0_1$__String_$0_1$__ResultSet_1_", (e, r) -> new FetchDbColumnsMetadata(r, e.getMessage(), e.getMaxSQLRows())),
                Tuples.pair("fetchDbSchemasMetaData_DatabaseConnection_1__String_$0_1$__ResultSet_1_", (e, r) -> new FetchDbSchemasMetadata(r, e.getMessage(), e.getMaxSQLRows())),
                Tuples.pair("fetchDbPrimaryKeysMetaData_DatabaseConnection_1__String_$0_1$__String_1__ResultSet_1_", (e, r) -> new FetchDbPrimaryKeysMetaData(r, e.getMessage(), e.getMaxSQLRows())),
                Tuples.pair("fetchDbImportedKeysMetaData_DatabaseConnection_1__String_$0_1$__String_1__ResultSet_1_", (e, r) -> new FetchDbImportedKeysMetaData(r, e.getMessage(), e.getMaxSQLRows())),
                Tuples.pair("logActivities_Activity_MANY__Nil_0_", (e, r) -> new LogActivities(e.getExecutionActivityListener()))
        );
    }

    public static InterpretedExtension extension()
    {
        return new RelationalExtensionInterpreted();
    }
}
