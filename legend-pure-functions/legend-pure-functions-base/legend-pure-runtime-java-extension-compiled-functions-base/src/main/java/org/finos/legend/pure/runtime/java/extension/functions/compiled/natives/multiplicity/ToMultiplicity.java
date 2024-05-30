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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.multiplicity;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class ToMultiplicity extends AbstractNative
{
    public ToMultiplicity()
    {
        super("toMultiplicity_T_MANY__Any_z__T_z_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        CoreInstance targetMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.multiplicity, processorSupport);
        String sourceObject = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);
        if (!Multiplicity.isMultiplicityConcrete(targetMultiplicity))
        {
            return sourceObject;
        }
        else
        {
            String lowerValue = targetMultiplicity.getValueForMetaPropertyToOne("lowerBound").getValueForMetaPropertyToOne("value").getName();
            CoreInstance upperValue = targetMultiplicity.getValueForMetaPropertyToOne("upperBound").getValueForMetaPropertyToOne("value");
            return "CompiledSupport.toMultiplicity" + (Multiplicity.isToZeroOrOne(targetMultiplicity) ? "One(" : "Many(") + sourceObject + "," + lowerValue + "," + (upperValue == null ? -1 : upperValue.getName()) + "," + NativeFunctionProcessor.buildM4LineColumnSourceInformation(functionExpression.getSourceInformation()) + ")";
        }
    }
}
