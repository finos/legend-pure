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

package org.finos.legend.pure.m3.navigation.functionexpression;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class FunctionExpression
{
    /**
     * Resolve the generic return type and multiplicity of the function in a
     * function expression.  This is returned as resolved type-multiplicity
     * pair.
     *
     * @param functionExpression function expression
     * @return resolved type-multiplicity pair
     */
    public static Pair<CoreInstance, CoreInstance> resolveFunctionGenericReturnTypeAndMultiplicity(CoreInstance functionExpression, ProcessorSupport processorSupport)
    {
        CoreInstance function = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, processorSupport);
        CoreInstance functionType = processorSupport.function_getFunctionType(function);

        CoreInstance returnGenericType;
        CoreInstance returnMultiplicity;

        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        if (Instance.instanceOf(function, M3Paths.Property, processorSupport))
        {
            // We need to handle properties different as they may have typeParameters & multiplicityParameters from the owning Class
            ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
            CoreInstance owner = parametersValues.get(0);
            CoreInstance classGenericType = Instance.getValueForMetaPropertyToOneResolved(owner, M3Properties.genericType, processorSupport);
            returnGenericType = GenericType.resolvePropertyReturnType(classGenericType, function, processorSupport);
            returnMultiplicity = Property.resolveInstancePropertyReturnMultiplicity(owner, function, processorSupport);
        }
        else
        {
            returnGenericType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
            Pair<ImmutableMap<String, CoreInstance>, ImmutableMap<String, CoreInstance>> resolvedTypeAndMultiplicityParams = Support.inferTypeAndMultiplicityParameterUsingFunctionExpression(functionType, functionExpression, processorSupport);

            returnGenericType = GenericType.makeTypeArgumentAsConcreteAsPossible(returnGenericType, resolvedTypeAndMultiplicityParams.getOne(), resolvedTypeAndMultiplicityParams.getTwo(), processorSupport);
            returnMultiplicity = Multiplicity.makeMultiplicityAsConcreteAsPossible(multiplicity, resolvedTypeAndMultiplicityParams.getTwo());
        }
        return Tuples.pair(returnGenericType, returnMultiplicity);
    }

    public static <T extends Appendable> T printFunctionSignatureFromExpression(T appendable, CoreInstance functionExpression, ProcessorSupport processorSupport)
    {
        String functionName = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.functionName, processorSupport).getName();
        ListIterable<? extends CoreInstance> parameterValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        return printFunctionSignatureFromExpression(appendable, functionName, parameterValues, processorSupport);
    }

    public static <T extends Appendable> T printFunctionSignatureFromExpression(T appendable, String functionName, ListIterable<? extends CoreInstance> parameterValues, ProcessorSupport processorSupport)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        safeAppendable.append(functionName).append('(');
        parameterValues.forEachWithIndex((parameterValue, i) ->
        {
            ((i == 0) ? safeAppendable : safeAppendable.append(',')).append("_:");
            GenericType.print(safeAppendable, Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.genericType, processorSupport), processorSupport);
            Multiplicity.print(safeAppendable, Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.multiplicity, processorSupport), true);
        });
        safeAppendable.append(')');
        return appendable;
    }
}
