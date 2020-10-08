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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public abstract class AbstractNative implements Native
{
    private final ImmutableList<String> signatures;

    protected AbstractNative(String signature)
    {
        this.signatures = Lists.immutable.with(signature);
    }

    protected AbstractNative(String signature1, String signature2)
    {
        this.signatures = Lists.immutable.with(signature1, signature2);
    }

    @Override
    public ListIterable<String> transformParameterValues(ListIterable<? extends CoreInstance> parametersValues, CoreInstance topLevelElement, ProcessorSupport processorSupport, ProcessorContext processorContext) {
        MutableList<String> transformedParams = FastList.newList(parametersValues.size());

        for (CoreInstance parameterValue :  parametersValues)
        {
            transformedParams.add(ValueSpecificationProcessor.processValueSpecification(topLevelElement, parameterValue, processorContext));
        }

        return transformedParams;
    }

    protected AbstractNative(String... signatures)
    {
        this.signatures = Lists.immutable.with(signatures);
    }

    @Override
    public final Iterable<String> signatures()
    {
        return this.signatures;
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "                throw new RuntimeException(\"Not Implemented for function:" + signatures.makeString() + "\");\n" +
                "            }\n" +
                "        }";
    }
}