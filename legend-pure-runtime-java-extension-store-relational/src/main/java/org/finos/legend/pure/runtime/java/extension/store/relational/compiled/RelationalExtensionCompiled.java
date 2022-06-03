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

package org.finos.legend.pure.runtime.java.extension.store.relational.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.CreateTempTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.CreateTempTableWithFinally;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.DropTempTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ExecuteInDb;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbColumnsMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbImportedKeysMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbPrimaryKeysMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbSchemasMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbTablesMetaData;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LoadCsvToDbTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LoadValuesToDbTable;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LoadValuesToDbTableNew;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.LogActivities;

public class RelationalExtensionCompiled extends BaseCompiledExtension
{
    public RelationalExtensionCompiled()
    {
        super(
                Lists.fixedSize.with(new CreateTempTable(), new CreateTempTableWithFinally(), new DropTempTable(), new ExecuteInDb(), new FetchDbColumnsMetaData(),
                        new FetchDbImportedKeysMetaData(), new FetchDbPrimaryKeysMetaData(), new FetchDbSchemasMetaData(), new FetchDbTablesMetaData(), new LoadCsvToDbTable(),
                        new LoadValuesToDbTable(), new LoadValuesToDbTableNew(), new LogActivities()),
                Lists.fixedSize.with(),
                Lists.fixedSize.empty(),
                Lists.fixedSize.empty());
    }

    public static CompiledExtension extension()
    {
        return new RelationalExtensionCompiled();
    }
}
