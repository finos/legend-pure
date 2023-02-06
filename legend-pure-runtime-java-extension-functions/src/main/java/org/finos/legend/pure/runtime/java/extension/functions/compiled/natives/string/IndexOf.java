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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class IndexOf extends AbstractNative
{
    public IndexOf() {
        super("indexOf_String_1__String_1__Integer_1_", "indexOf_String_1__String_1__Integer_1__Integer_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        String fromIndex = transformedParams.size() == 3 ? ",((Long)" + transformedParams.get(2) + ").intValue()" : "";

        return "Long.valueOf(((String)" + transformedParams.get(0) + ")" +
                ".indexOf((String)" + transformedParams.get(1) + fromIndex + "))";
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction3<String, String, Number, Long>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Long value(String input, String str, Number from, ExecutionSupport es)\n" +
                "            {\n" +
                "               return from == null ? Long.valueOf(input.indexOf(str)) : Long.valueOf(input.indexOf(str, from.intValue()));" +
                "            }\n" +
                "        }";
    }
}
