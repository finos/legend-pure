// Copyright 2025 Goldman Sachs
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


package org.finos.legend.pure.runtime.java.interpreted.natives.variant;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.shared.variant.VariantInstanceImpl;

public abstract class AbstractTo extends NativeFunction
{
    private final ModelRepository repository;

    public AbstractTo(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance variantCoreInstance = params.get(0);
        CoreInstance typeParameter = params.get(1);
        CoreInstance targetGenericType = Instance.getValueForMetaPropertyToOneResolved(typeParameter, M3Properties.genericType, processorSupport);

        RichIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(variantCoreInstance, M3Properties.values, processorSupport)
                .flatCollect(x -> this.toCoreInstances((VariantInstanceImpl) x, targetGenericType, functionExpressionCallStack, processorSupport))
                .select(Objects::nonNull);

        return ValueSpecificationBootstrap.wrapValueSpecification(values, true, targetGenericType, processorSupport);
    }

    abstract Iterable<? extends CoreInstance> toCoreInstances(VariantInstanceImpl variantCoreInstance, CoreInstance targetGenericType, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport);

    CoreInstance toCoreInstance(JsonNode jsonNode, CoreInstance targetGenericType, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        CoreInstance targetRawType = Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.rawType, processorSupport);

        if (targetRawType == processorSupport.package_getByUserPath(M3Paths.Variant))
        {
            return VariantInstanceImpl.newVariant(jsonNode, this.repository, processorSupport);
        }

        if (jsonNode.isNull())
        {
            return null;
        }

        if (targetRawType == processorSupport.package_getByUserPath(M3Paths.List))
        {
            if (jsonNode.isArray())
            {
                CoreInstance listValueGenericType = Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.typeArguments, processorSupport);

                Iterable<CoreInstance> values = Iterate.collect(jsonNode, x -> this.toCoreInstance(x, listValueGenericType, functionExpressionCallStack, processorSupport));

                CoreInstance listCoreInstance = processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.List);
                Instance.addValueToProperty(listCoreInstance, M3Properties.values, values, processorSupport);
                Instance.setValueForProperty(listCoreInstance, M3Properties.classifierGenericType, targetGenericType, processorSupport);

                return listCoreInstance;
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.Map))
        {
            if (jsonNode.isObject())
            {
                CoreInstance keyValueGenericType = Instance.getValueForMetaPropertyToManyResolved(targetGenericType, M3Properties.typeArguments, processorSupport).get(0);

                if (Instance.getValueForMetaPropertyToOneResolved(keyValueGenericType, M3Properties.rawType, processorSupport) == processorSupport.package_getByUserPath(M3Paths.String))
                {
                    CoreInstance mapValueGenericType = Instance.getValueForMetaPropertyToManyResolved(targetGenericType, M3Properties.typeArguments, processorSupport).get(1);

                    MapCoreInstance mapCoreInstance = new MapCoreInstance(Lists.immutable.empty(), "", null, processorSupport.package_getByUserPath(M3Paths.Map), -1, this.repository, false, processorSupport);
                    Instance.setValueForProperty(mapCoreInstance, M3Properties.classifierGenericType, targetGenericType, processorSupport);

                    MutableMap<CoreInstance, CoreInstance> internalMap = mapCoreInstance.getMap();
                    for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); )
                    {
                        Map.Entry<String, JsonNode> entry = it.next();
                        internalMap.put(
                                this.repository.newStringCoreInstance(entry.getKey()),
                                this.toCoreInstance(entry.getValue(), mapValueGenericType, functionExpressionCallStack, processorSupport)
                        );
                    }

                    return mapCoreInstance;
                }
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.Integer))
        {
            if (jsonNode.isIntegralNumber())
            {
                return this.repository.newIntegerCoreInstance(jsonNode.bigIntegerValue());
            }
            else if (jsonNode.isTextual())
            {
                return this.repository.newIntegerCoreInstance(jsonNode.textValue());
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.Float))
        {
            if (jsonNode.isNumber())
            {
                return this.repository.newFloatCoreInstance(jsonNode.decimalValue());
            }
            else if (jsonNode.isTextual())
            {
                return this.repository.newFloatCoreInstance(jsonNode.textValue());
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.StrictDate))
        {
            if (jsonNode.isTextual())
            {
                return this.repository.newStrictDateCoreInstance(jsonNode.textValue());
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.DateTime))
        {
            if (jsonNode.isTextual())
            {
                return this.repository.newDateTimeCoreInstance(jsonNode.textValue());
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.String))
        {
            if (jsonNode.isValueNode())
            {
                return this.repository.newStringCoreInstance(jsonNode.asText());
            }
        }
        else if (targetRawType == processorSupport.package_getByUserPath(M3Paths.Boolean))
        {
            if (jsonNode.isBoolean())
            {
                return this.repository.newBooleanCoreInstance(jsonNode.booleanValue());
            }
            else if (jsonNode.isTextual())
            {
                return this.repository.newBooleanCoreInstance(jsonNode.textValue());
            }
        }

        throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Variant of type '" + jsonNode.getNodeType() + "' cannot be converted to " + GenericType.print(targetGenericType, processorSupport), functionExpressionCallStack);
    }
}
