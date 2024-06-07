// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.collection.anonymous.map;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class NewMap extends AbstractNativeFunctionGeneric
{
    public NewMap()
    {
        super("CoreGen.newMap", new Class[]{RichIterable.class, ExecutionSupport.class}, false, true, false, "newMap_Pair_MANY__Map_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        String param = transformedParams.get(0);
        ListIterable<String> params = "null".equals(param) ? Lists.immutable.with("Lists.immutable.<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?,?>>empty()") : transformedParams;
        return super.build(topLevelElement, functionExpression, params, processorContext);
    }
}
