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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.creation;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.validator.PropertyValidator;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.creation.New;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class DynamicNew extends NativeFunction
{
    private final ModelRepository repository;
    private final boolean isGenericType;
    private final FunctionExecutionInterpreted functionExecution;

    public DynamicNew(ModelRepository repository, boolean isGenericType, FunctionExecutionInterpreted functionExecutionInterpreted)
    {
        this.repository = repository;
        this.isGenericType = isGenericType;
        this.functionExecution = functionExecutionInterpreted;
    }

    public DynamicNew(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this(repository, false, functionExecution);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        // The parameter is a Class ... but we encode the typeArguments in the ValueExpression genericType's typeArguments ...
        CoreInstance genericType = this.isGenericType ? Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport) :
                processorSupport.type_wrapGenericType(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
        // key / value list
        ListIterable<? extends CoreInstance> keyValues = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);

        CoreInstance classifier = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        CoreInstance instance = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), classifier);
        if (this.isGenericType)
        {
            Instance.addValueToProperty(instance, M3Properties.classifierGenericType, genericType, processorSupport);
        }

        // Only validate if the type is fully specified (type parameters match type arguments)
        // Skip validation for generic types where type arguments are missing, as we can't properly resolve property types
        ListIterable<? extends CoreInstance> typeParameters = Instance.getValueForMetaPropertyToManyResolved(classifier, M3Properties.typeParameters, processorSupport);
        ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
        boolean shouldValidate = typeParameters.size() == typeArguments.size();
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(classifier);

        // Set property values
        MutableSet<String> setKeys = Sets.mutable.empty();
        for (CoreInstance keyValue : keyValues)
        {
            // Find and validate Property
            String key = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport).getName();
            CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(classifier, key);
            if (property == null)
            {
                throw new PureExecutionException(Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport).getSourceInformation(), "The property '" + key + "' can't be found in the type '" + classifier.getName() + "' or in its hierarchy.", functionExpressionCallStack);
            }

            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(keyValue, M3Properties.value, processorSupport);
            Instance.setValuesForProperty(instance, key, values, processorSupport);
            setKeys.add(key);
            if (shouldValidate)
            {
                try
                {
                    PropertyValidator.validateMultiplicityRange(instance, property, values, processorSupport);
                    values.forEach(val -> PropertyValidator.validateTypeRange(instance, property, val, processorSupport));
                }
                catch (PureCompilationException ex)
                {
                    throw new PureExecutionException("Unable to create a new instance of class '" + classifier.getName() + "'. Invalid value '" + values.collect(v -> v.printWithoutDebug("", 1)).makeString(",") + "' provided for class property '" + key + "': " + ex.getInfo(), ex, functionExpressionCallStack);
                }
            }
        }

        // Set default values if the values for any required fields were not provided
        VariableContext evaluationVariableContext = this.getParentOrEmptyVariableContext(variableContext);
        for (CoreInstance property : propertiesByName)
        {
            if (!setKeys.contains(property.getName()) && property.getValueForMetaPropertyToOne(M3Properties.defaultValue) != null)
            {
                CoreInstance expression = Property.getDefaultValueExpression(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));
                if (Instance.instanceOf(expression, M3Paths.EnumStub, processorSupport))
                {
                    ListIterable<? extends CoreInstance> values = Property.getDefaultValue(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));
                    for (CoreInstance value : values)
                    {
                        Instance.addValueToProperty(instance, property.getName(), value.getValueForMetaPropertyToOne(M3Properties.resolvedEnum), processorSupport);
                    }
                }
                else
                {
                    New.setValuesToProperty(expression, expression, property, instance, expression.getSourceInformation(), genericType, evaluationVariableContext, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, this.functionExecution, processorSupport);
                }
            }
        }

        New.updateReverseProperties(instance, functionExpressionCallStack.peek().getSourceInformation(), functionExpressionCallStack, processorSupport);

        CoreInstance override = null;
        if (params.size() > 5)
        {
            override = processorSupport.newAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), M3Paths.ConstraintsGetterOverride);
            Instance.addValueToProperty(override, M3Properties.constraintsManager, Instance.getValueForMetaPropertyToOneResolved(params.get(5), M3Properties.values, processorSupport), processorSupport);
        }
        else
        {
            if (params.size() > 2)
            {
                override = processorSupport.newAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), M3Paths.GetterOverride);
            }
        }
        if (override != null)
        {
            Instance.addValueToProperty(override, M3Properties.getterOverrideToOne, Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport), processorSupport);
            Instance.addValueToProperty(override, M3Properties.getterOverrideToMany, Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport), processorSupport);
            Instance.addValueToProperty(override, M3Properties.hiddenPayload, Instance.getValueForMetaPropertyToOneResolved(params.get(4), M3Properties.values, processorSupport), processorSupport);
            Instance.addValueToProperty(instance, M3Properties.elementOverride, override, processorSupport);
        }
        CoreInstance value = ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);
        return DefaultConstraintHandler.handleConstraints(classifier, value, functionExpressionCallStack.peek().getSourceInformation(), this.functionExecution, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }
}
