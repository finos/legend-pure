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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.conjunctions;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class Not extends AbstractNative
{
    public Not()
    {
        super("not_Boolean_1__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "!(" + transformedParams.get(0) + ")";
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction1<Boolean, Boolean>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Boolean value(Boolean p1, ExecutionSupport es)\n" +
                "            {\n" +
                "                return !p1;\n" +
                "            }\n" +
                "        }";
    }
}
