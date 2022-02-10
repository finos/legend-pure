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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class Support
{
    static Pair<ImmutableMap<String, CoreInstance>, ImmutableMap<String, CoreInstance>> inferTypeAndMultiplicityParameterUsingFunctionExpression(CoreInstance functionType, CoreInstance functionExpression, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> parameters = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);
        ListIterable<? extends CoreInstance> parameterValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        int size = parameters.size();
        if (size != parameterValues.size())
        {
            throw new RuntimeException("Mismatch between the number of parameters (" + size + ") and the number of values (" + parameterValues.size() + ")");
        }

        if (size == 0)
        {
            return Tuples.pair(Maps.immutable.empty(), Maps.immutable.empty());
        }

        MutableMap<String, CoreInstance> inferredTypeParams = Maps.mutable.empty();
        MutableMap<String, CoreInstance> inferredMultiplicityParams = Maps.mutable.empty();
        for (int i = 0; i < size; i++)
        {
            CoreInstance parameter = parameters.get(i);
            CoreInstance parameterValue = parameterValues.get(i);
            CoreInstance parameterGenericType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
            CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
            CoreInstance parameterValueGenericType = Instance.instanceOf(parameterValue, M3Paths.ValueSpecification, processorSupport) ? Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.genericType, processorSupport) : Instance.extractGenericTypeFromInstance(parameterValue, processorSupport);
            CoreInstance parameterValueMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.multiplicity, processorSupport);
            processOneLevelGenericType(parameterGenericType, parameterMultiplicity, parameterValueGenericType, parameterValueMultiplicity, inferredTypeParams, inferredMultiplicityParams, processorSupport);
        }
        return Tuples.pair(inferredTypeParams.toImmutable(), inferredMultiplicityParams.toImmutable());
    }

    private static void processOneLevelGenericType(CoreInstance parameterGenericType, CoreInstance parameterMultiplicity, CoreInstance parameterValueGenericType, CoreInstance parameterValueMultiplicity, MutableMap<String, CoreInstance> inferredTypeParams, MutableMap<String, CoreInstance> inferredMultiplicityParams, ProcessorSupport processorSupport)
    {
        if (!Multiplicity.isMultiplicityConcrete(parameterMultiplicity))
        {
            String multiplicityParam = Multiplicity.getMultiplicityParameter(parameterMultiplicity);
            CoreInstance currentMultiplicity = inferredMultiplicityParams.get(multiplicityParam);
            if (currentMultiplicity == null)
            {
                inferredMultiplicityParams.put(multiplicityParam, parameterValueMultiplicity);
            }
            else if (!Multiplicity.isMultiplicityConcrete(currentMultiplicity) && Multiplicity.isMultiplicityConcrete(parameterValueMultiplicity))
            {
                inferredMultiplicityParams.put(multiplicityParam, parameterValueMultiplicity);
            }
            else if (!Multiplicity.multiplicitiesEqual(currentMultiplicity, parameterValueMultiplicity))
            {
                inferredMultiplicityParams.put(multiplicityParam, Multiplicity.minSubsumingMultiplicity(currentMultiplicity, parameterValueMultiplicity, processorSupport));
            }
        }

        if (!GenericType.isGenericTypeConcrete(parameterGenericType))
        {
            String typeParameter = GenericType.getTypeParameterName(parameterGenericType);
            CoreInstance currentGenericType = inferredTypeParams.get(typeParameter);
            if (currentGenericType == null)
            {
                inferredTypeParams.put(typeParameter, parameterValueGenericType);
            }
            else if (!GenericType.isGenericTypeConcrete(currentGenericType) && GenericType.isGenericTypeConcrete(parameterValueGenericType))
            {
                inferredTypeParams.put(typeParameter, parameterValueGenericType);
            }
            else if (!GenericType.genericTypesEqual(currentGenericType, parameterValueGenericType, processorSupport))
            {
                inferredTypeParams.put(typeParameter, GenericType.findBestCommonGenericType(Lists.immutable.with(currentGenericType, parameterValueGenericType), true, false, processorSupport));
            }
        }
        else if (FunctionType.isFunctionType(parameterGenericType, processorSupport))
        {
            inferTypeParameterUsingResolvedFunctionType(parameterGenericType, parameterValueGenericType, inferredTypeParams, inferredMultiplicityParams, processorSupport);
        }
        else if (!Type.isBottomType(parameterValueGenericType.getValueForMetaPropertyToOne(M3Properties.rawType), processorSupport))
        {
            ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(parameterGenericType, M3Properties.typeArguments, processorSupport);
            int typeArgumentCount = typeArguments.size();
            if (typeArgumentCount > 0)
            {
                CoreInstance parameterRawType = Instance.getValueForMetaPropertyToOneResolved(parameterGenericType, M3Properties.rawType, processorSupport);
                ListIterable<? extends CoreInstance> typeParameters = Instance.getValueForMetaPropertyToManyResolved(parameterRawType, M3Properties.typeParameters, processorSupport);
                if (typeArgumentCount != typeParameters.size())
                {
                    throw new RuntimeException("Mismatch between the number of type parameters (" + typeParameters.size() + ") and the number of type arguments provided (" + typeArgumentCount + ")");
                }
                GenericTypeWithXArguments homogenizedTypeArgs = GenericType.resolveClassTypeParameterUsingInheritance(parameterValueGenericType, parameterGenericType, processorSupport);
                for (int i = 0; i < typeArgumentCount; i++)
                {
                    CoreInstance typeArgument = typeArguments.get(i);
                    CoreInstance typeParameter = typeParameters.get(i);
                    String typeParameterNameInSource = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(typeParameter, M3Properties.name, processorSupport));
                    processOneLevelGenericType(typeArgument, parameterMultiplicity, homogenizedTypeArgs.getArgumentByParameterName(typeParameterNameInSource), parameterValueMultiplicity, inferredTypeParams, inferredMultiplicityParams, processorSupport);
                }
            }
        }
    }

    private static void inferTypeParameterUsingResolvedFunctionType(CoreInstance functionType, CoreInstance resolvedFunctionType, MutableMap<String, CoreInstance> inferredTypeParams, MutableMap<String, CoreInstance> inferredMultiplicityParams, ProcessorSupport processorSupport)
    {
        // TODO consider doing something when resolvedFunctionType is not concrete
        if (GenericType.isGenericTypeConcrete(resolvedFunctionType))
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.rawType, processorSupport);
            CoreInstance resolvedRawType = Instance.getValueForMetaPropertyToOneResolved(resolvedFunctionType, M3Properties.rawType, processorSupport);

            ListIterable<? extends CoreInstance> parameters = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);
            int size = parameters.size();
            if (size > 0)
            {
                ListIterable<? extends CoreInstance> resolvedParameters = Instance.getValueForMetaPropertyToManyResolved(resolvedRawType, M3Properties.parameters, processorSupport);
                if (size != resolvedParameters.size())
                {
                    throw new RuntimeException("Mismatch between the expected number of parameters (" + size + ") and the actual number of parameters (" + resolvedParameters.size() + ")");
                }
                for (int i = 0; i < size; i++)
                {
                    CoreInstance parameter = parameters.get(i);
                    CoreInstance parameterGenericType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
                    CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);

                    CoreInstance resolvedParameter = resolvedParameters.get(i);
                    CoreInstance resolvedParameterGenericType = Instance.instanceOf(resolvedParameter, M3Paths.ValueSpecification, processorSupport) ? Instance.getValueForMetaPropertyToOneResolved(resolvedParameter, M3Properties.genericType, processorSupport) : Instance.extractGenericTypeFromInstance(resolvedParameter, processorSupport);
                    CoreInstance resolvedParameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(resolvedParameter, M3Properties.multiplicity, processorSupport);

                    processOneLevelGenericType(parameterGenericType, parameterMultiplicity, resolvedParameterGenericType, resolvedParameterMultiplicity, inferredTypeParams, inferredMultiplicityParams, processorSupport);
                }
            }
            processOneLevelGenericType(Instance.getValueForMetaPropertyToOneResolved(rawType, M3Properties.returnType, processorSupport),
                    Instance.getValueForMetaPropertyToOneResolved(rawType, M3Properties.returnMultiplicity, processorSupport),
                    Instance.getValueForMetaPropertyToOneResolved(resolvedRawType, M3Properties.returnType, processorSupport),
                    Instance.getValueForMetaPropertyToOneResolved(resolvedRawType, M3Properties.returnMultiplicity, processorSupport),
                    inferredTypeParams,
                    inferredMultiplicityParams,
                    processorSupport);
        }
    }
}
