// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar.multiplicity;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Stack;

public class ToMultiplicity extends CommonToMultiplicity
{
    public ToMultiplicity(ModelRepository repository)
    {
        super(repository, false);
    }

    @Override
    protected CoreInstance getReturnMultiplicity(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ProcessorSupport processorSupport)
    {
        CoreInstance targetMultiplicity = params.get(1).getValueForMetaPropertyToOne(M3Properties.multiplicity);
        return makeMultiplicityAsConcreteAsPossible(targetMultiplicity, resolvedMultiplicityParameters);
    }

    private CoreInstance makeMultiplicityAsConcreteAsPossible(CoreInstance multiplicity, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParametersStack)
    {
        if (Multiplicity.isMultiplicityConcrete(multiplicity))
        {
            return multiplicity;
        }
        CoreInstance result = multiplicity;
        for (int i = resolvedMultiplicityParametersStack.size() - 2; i >= 0; i--)
        {
            MutableMap<String, CoreInstance> resolvedMultiplicityParameters = resolvedMultiplicityParametersStack.elementAt(i);
            if (resolvedMultiplicityParameters.notEmpty())
            {
                result = Multiplicity.makeMultiplicityAsConcreteAsPossible(result, resolvedMultiplicityParameters);
                if (Multiplicity.isMultiplicityConcrete(result))
                {
                    return result;
                }
            }
        }
        return result;
    }
}
