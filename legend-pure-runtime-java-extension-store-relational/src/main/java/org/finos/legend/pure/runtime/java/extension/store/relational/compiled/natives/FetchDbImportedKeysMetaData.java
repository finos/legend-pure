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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class FetchDbImportedKeysMetaData extends AbstractNative
{
    public FetchDbImportedKeysMetaData()
    {
        super("fetchDbImportedKeysMetaData_DatabaseConnection_1__String_$0_1$__String_1__ResultSet_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "org.finos.legend.pure.runtime.java.extension.store.relational.compiled.RelationalGen.fetchDbMetaData(" + transformedParams.get(0) + ", new org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.FetchDbImportedKeysMetaDataFunction" + transformedParams.get(1) + "," + transformedParams.get(2) + "), Maps.immutable.<String, Function<ListIterable<Object>, String>>empty(),es)";
    }
}
