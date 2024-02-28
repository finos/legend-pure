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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

public class Eq extends AbstractNativeFunctionGeneric
{
    public Eq()
    {
        super(getMethod(CompiledSupport.class, "eq", Object.class, Object.class),"eq_Any_1__Any_1__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        final ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());
        final ProcessorSupport processorSupport = processorContext.getSupport();

        CoreInstance left = parametersValues.get(0);


        CoreInstance leftMult = left.getValueForMetaPropertyToOne(M3Properties.multiplicity);

        if (Multiplicity.isToOne(leftMult))
        {
            CoreInstance right = parametersValues.get(1);
            CoreInstance rightMult = right.getValueForMetaPropertyToOne(M3Properties.multiplicity);
            if (Multiplicity.isToOne(rightMult))
            {
                if (ValueSpecification.instanceOf(left, M3Paths.Integer, processorSupport)
                        && ValueSpecification.instanceOf(right, M3Paths.Integer, processorSupport))
                {
                    return "CompiledSupport.eq_Integer_1(" + transformedParams.get(0) + ", " + transformedParams.get(1) + ")";
                }

                if (ValueSpecification.instanceOf(left, M3Paths.Float, processorSupport)
                        && ValueSpecification.instanceOf(right, M3Paths.Float, processorSupport))
                {
                    return "CompiledSupport.eq_Float_1(" + transformedParams.get(0) + ", " + transformedParams.get(1) + ")";
                }
            }

        }

        return "CompiledSupport.eq(" + transformedParams.get(0) + ", " + transformedParams.get(1) + ")";
    }
}
