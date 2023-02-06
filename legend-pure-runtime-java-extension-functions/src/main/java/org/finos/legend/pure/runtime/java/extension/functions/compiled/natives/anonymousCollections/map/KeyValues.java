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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;

public class KeyValues extends AbstractNative
{
    private static final String EXPRESSION = ".getMap().keyValuesView().collect(new DefendedFunction<Object," + FullJavaPaths.Pair + ">(){public " + FullJavaPaths.Pair + " valueOf(Object o){return new " + FullJavaPaths.Pair_Impl + "(\"\")._first(((org.eclipse.collections.api.tuple.Pair)o).getOne())._second(((org.eclipse.collections.api.tuple.Pair)o).getTwo());}})";

    public KeyValues()
    {
        super("keyValues_Map_1__Pair_MANY_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "((PureMap)"+ transformedParams.get(0) +")" + EXPRESSION;
    }

    @Override
    public String buildBody() {

        return "new DefendedPureFunction1<PureMap, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(PureMap map, ExecutionSupport es)\n" +
                "            {\n" +
                "                return map" + EXPRESSION + ";\n" +
                "            }\n" +
                "        }";
    }
}
