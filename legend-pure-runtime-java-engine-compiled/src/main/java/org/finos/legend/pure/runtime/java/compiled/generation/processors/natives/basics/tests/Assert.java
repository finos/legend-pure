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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.tests;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;

public class Assert extends AbstractNativeFunctionGeneric
{
    public Assert() {
        super(getMethod(CompiledSupport.class, "pureAssert"), true, true, false, "assert_Boolean_1__Function_1__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        FastList<String> params = FastList.newListWith(
                transformedParams.get(0),
                "CoreGen.getSharedPureFunction(" + transformedParams.get(1) + ", es)");

        return super.build(topLevelElement, functionExpression, params, processorContext);
    }

    @Override
    public String buildBody() {
        return "new DefendedPureFunction2<Boolean, " + FullJavaPaths.Function + ", Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Boolean condition, " + FullJavaPaths.Function + " func, ExecutionSupport es)\n" +
                "            {\n" +
                "                return CompiledSupport.pureAssert(condition, CoreGen.getSharedPureFunction(func, es), null, es);\n" +
                "            }\n" +
                "        }";
    }
}
