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
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.RelationalStoreCoreInstanceFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.AbstractCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.*;

import java.util.List;

public class RelationalExtensionCompiled extends AbstractCompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(
                loadExtraJavaSource("org.finos.legend.pure.generated", "RelationalGen", "org/finos/legend/pure/runtime/java/extension/store/relational/compiled/RelationalGen.java")
        );
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(new CreateTempTable(), new CreateTempTableWithFinally(), new DropTempTable(), new ExecuteInDb(), new FetchDbColumnsMetaData(),
                new FetchDbImportedKeysMetaData(), new FetchDbPrimaryKeysMetaData(), new FetchDbSchemasMetaData(), new FetchDbTablesMetaData(), new LoadCsvToDbTable(),
                new LoadValuesToDbTable(), new LoadValuesToDbTableNew(), new LogActivities());
    }

    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return RelationalStoreCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_store_relational";
    }

    public static CompiledExtension extension()
    {
        return new RelationalExtensionCompiled();
    }
}
