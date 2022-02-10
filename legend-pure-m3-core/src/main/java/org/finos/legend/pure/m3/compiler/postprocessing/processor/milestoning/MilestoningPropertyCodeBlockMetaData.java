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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

class MilestoningPropertyCodeBlockMetaData
{
    final String propertyName;
    final String edgepointPropertyName;
    final String rangePropertyName;
    final String varName;
    final Object multiplicityFunctionCall;
    final SourceInformation propertySourceInformation;
    final SourceInformation propertyGenericTypeSourceInformation;
    final AbstractProperty<?> property;
    final String returnType;
    final String possiblyOverridenMultiplicity;

    MilestoningPropertyCodeBlockMetaData(AbstractProperty<?> property, String returnType, String multiplicity)
    {
        this.property = property;
        this.propertyName = property.getName();
        this.edgepointPropertyName = MilestoningFunctions.getEdgePointPropertyName(this.propertyName);
        this.rangePropertyName = MilestoningFunctions.getRangePropertyName(this.propertyName);
        this.varName = MilestoningFunctions.MILESTONE_LAMBDA_VARIABLE_NAME;
        this.multiplicityFunctionCall = getMultiplicityFunctionCall(multiplicity);
        this.possiblyOverridenMultiplicity = possiblyApplyDefaultReturnMultiplicity(multiplicity);
        this.propertySourceInformation = property.getSourceInformation();
        this.propertyGenericTypeSourceInformation = property._genericType().getSourceInformation();
        this.returnType = returnType;
    }

    MilestoningPropertyCodeBlockMetaData(CoreInstance property, String returnType, String multiplicity)
    {
        this((AbstractProperty<?>) property, returnType, multiplicity);
    }

    private String possiblyApplyDefaultReturnMultiplicity(String multiplicity)
    {
        String appliedMultiplicityFunc = getMultiplicityFunctionCall(multiplicity);
        return appliedMultiplicityFunc.isEmpty() ? "*" : multiplicity;
    }

    private String getMultiplicityFunctionCall(String multiplicity)
    {
        switch (multiplicity)
        {
            case "1":
            {
                return "->toOne()";
            }
            case "0..1":
            {
                return "->first()";
            }
            case "1..*":
            {
                return "->toOneMany()";
            }
            default:
            {
                return "";
            }
        }
    }
}
