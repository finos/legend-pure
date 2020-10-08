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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.Executor;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.constraints.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Copy extends NativeFunction
{
    private final ModelRepository repository;
    private final FunctionExecutionInterpreted functionExecution;



    public Copy(ModelRepository repository, FunctionExecutionInterpreted functionExecution)
    {
        this.repository = repository;
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {

        final CoreInstance instance = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);

        CoreInstance classifierGenericType = Instance.extractGenericTypeFromInstance(instance, processorSupport);
        final CoreInstance sourceClassifier = Instance.getValueForMetaPropertyToOneResolved(classifierGenericType, M3Properties.rawType, processorSupport);

        instantiationContext.push(sourceClassifier);

        // TODO should we start a repository transaction here?
        final CoreInstance newInstance = this.repository.newEphemeralAnonymousCoreInstance(null, sourceClassifier);

        ListIterable<? extends CoreInstance> keyValues = (params.size() > 2) ? Instance.getValueForMetaPropertyToManyResolved(params.get(2), M3Properties.values, processorSupport) : Lists.immutable.<CoreInstance>with();

        MutableSet<CoreInstance> addedValues = UnifiedSet.newSet();

        MutableMap<CoreInstance, MutableMap> propertyTree = Maps.mutable.empty();

        // Add the modified properties
        VariableContext evaluationVariableContext = getParentOrEmptyVariableContext(variableContext);
        for (CoreInstance keyValue : keyValues)
        {
            CoreInstance classifier = sourceClassifier;
            // Find Property and newInstanceCurrent
            ListIterable<? extends CoreInstance> keys = Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport), M3Properties.values, processorSupport);
            String finalPropertyName = keys.getLast().getName();
            CoreInstance property = null;
            CoreInstance newInstanceCurrent = newInstance;
            CoreInstance instanceCurrent = instance;
            int size = keys.size() - 1;

            MutableMap<CoreInstance, MutableMap> treeNode = propertyTree;
            for (CoreInstance key : keys)
            {
                property = processorSupport.class_findPropertyUsingGeneralization(classifier, key.getName());
                if (property == null)
                {
                    throw new PureExecutionException(Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport).getSourceInformation(), "The property '" + key.getName() + "' can't be found in the type '" + classifier.getName() + "' or in its hierarchy.");
                }
                treeNode = treeNode.getIfAbsentPut(property, Maps.mutable.empty());

                classifier = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.genericType, M3Properties.rawType, processorSupport);
                if (size > 0)
                {
                    if (Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false))
                    {
                        instanceCurrent = Instance.getValueForMetaPropertyToOneResolved(instanceCurrent, property, processorSupport);
                        CoreInstance res = Instance.getValueForMetaPropertyToOneResolved(newInstanceCurrent, property, processorSupport);
                        if (res == null)
                        {
                            res = this.repository.newEphemeralAnonymousCoreInstance(null, classifier);
                            Instance.addValueToProperty(newInstanceCurrent, key.getName(), res, processorSupport);
                        }
                        newInstanceCurrent = res;
                    }
                    else
                    {
                        throw new RuntimeException("Not supported yet!");
                    }
                }
                size--;
            }

            // Maybe add the existing ones
            CoreInstance addValue = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.add, processorSupport);
            boolean add = (addValue != null) && PrimitiveUtilities.getBooleanValue(addValue);
            if (add)
            {
                ListIterable<? extends CoreInstance> existingOnes = Instance.getValueForMetaPropertyToManyResolved(instanceCurrent, property, processorSupport);
                for (CoreInstance existing : existingOnes)
                {
                    Instance.addValueToProperty(newInstanceCurrent, finalPropertyName, existing, processorSupport);
                    addedValues.add(existing);
                }
            }

            // Add the requested ones
            CoreInstance propertyGenericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(instanceCurrent, processorSupport), property, processorSupport);
            CoreInstance expression = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.expression, processorSupport);
            Executor executor = FunctionExecutionInterpreted.findValueSpecificationExecutor(expression, functionExpressionToUseInStack, processorSupport, this.functionExecution);
            CoreInstance instanceValResult = executor.execute(expression, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, evaluationVariableContext, profiler, instantiationContext, executionSupport, this.functionExecution, processorSupport);
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instanceValResult, M3Properties.values, processorSupport);
            New.validateRangeUsingMultiplicity(instance, keyValue, property, values, processorSupport);
            if (values.isEmpty())
            {

                if (newInstanceCurrent.isValueDefinedForKey(finalPropertyName))
                {
                    if (!add)
                    {
                        Instance.removeProperty(newInstanceCurrent, finalPropertyName, processorSupport);
                        Instance.addPropertyWithEmptyList(newInstanceCurrent, finalPropertyName, processorSupport);
                    }
                }
            }
            else
            {
                New.validateTypeFromGenericType(propertyGenericType, Instance.getValueForMetaPropertyToOneResolved(instanceValResult, M3Properties.genericType, processorSupport), expression, processorSupport);
                for (CoreInstance value : values)
                {
                    Instance.addValueToProperty(newInstanceCurrent, finalPropertyName, value, processorSupport);
                    addedValues.add(value);
                }
            }
        }

        this.copy(instance, newInstance, sourceClassifier, addedValues, functionExpressionToUseInStack.getSourceInformation(), processorSupport, instantiationContext, propertyTree);

        if(addedValues.isEmpty())
        {
            newInstance.setSourceInformation(instance.getSourceInformation());
        }
        else
        {
            newInstance.setSourceInformation(functionExpressionToUseInStack.getSourceInformation());
        }

        CoreInstance value = ValueSpecificationBootstrap.wrapValueSpecification(newInstance, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);

        instantiationContext.popAndExecuteProcedures(value);

        if (instantiationContext.isEmpty())
        {
            instantiationContext.runValidations();
            instantiationContext.reset();
        }

        return DefaultConstraintHandler.handleConstraints(sourceClassifier,  value, functionExpressionToUseInStack.getSourceInformation(), this.functionExecution,resolvedTypeParameters,resolvedMultiplicityParameters,variableContext,functionExpressionToUseInStack,profiler,instantiationContext, executionSupport);

    }

    private void copy(CoreInstance instance, final CoreInstance newInstance, final CoreInstance sourceClassifier, MutableSet<CoreInstance> addedValues, final SourceInformation sourceInfoForErrors, final ProcessorSupport processorSupport, InstantiationContext instantiationContext, MapIterable<CoreInstance, ? extends MapIterable> propertyTree) throws PureExecutionException
    {
        final MutableList<CoreInstance> propertiesToValidate = Lists.mutable.of();
        for (Pair<String, CoreInstance> pair : processorSupport.class_getSimplePropertiesByName(instance.getClassifier()).keyValuesView())
        {
            String key = pair.getOne();
            CoreInstance property = pair.getTwo();
            CoreInstance propertyGenericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(instance, processorSupport), property, processorSupport);

            if (!propertyTree.containsKey(property))
            {
                for (CoreInstance val : Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport))
                {
                    // The new instance doesn't have the value ... Add (not deep!)
                    Instance.addValueToProperty(newInstance, key, val, processorSupport);
                }
            }
            else
            {
                // Things were modified .. need to copy
                if (!Type.isPrimitiveType(Instance.getValueForMetaPropertyToOneResolved(propertyGenericType, M3Properties.rawType, processorSupport), processorSupport))
                {
                    if (Instance.getValueForMetaPropertyToManyResolved(newInstance, key, processorSupport).size() == 1 && Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport).size() == 1)
                    {
                        // Modify is only done on toOne sets.
                        CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(instance, key, processorSupport);
                        CoreInstance newValue = Instance.getValueForMetaPropertyToOneResolved(newInstance, key, processorSupport);
                        // Need to make sure we don't go deep for something we added!!!!
                        if (!addedValues.contains(newValue))
                        {
                            this.copy(value, newValue, sourceClassifier, addedValues, sourceInfoForErrors, processorSupport, instantiationContext, propertyTree.get(property));
                        }
                    }
                }
                propertiesToValidate.add(property);
            }
        }

        instantiationContext.registerValidation(new Runnable()
        {
            @Override
            public void run()
            {
                // Verify that all updated property values meet multiplicity constraints
                New.validatePropertyValueMultiplicities(newInstance, sourceClassifier, propertiesToValidate, sourceInfoForErrors, processorSupport);
            }
        });

        New.updateReverseProperties(newInstance, sourceInfoForErrors, processorSupport);

    }
}
