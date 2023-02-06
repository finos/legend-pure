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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.lang;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.compiler.validation.validator.PropertyValidator;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.New;
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
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, final Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        // The parameter is a Class ... but we encode the typeArguments in the ValueExpression genericType's typeArguments ...
        CoreInstance genericType = isGenericType ? Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport) :
                Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, M3Properties.typeArguments, processorSupport);
        // key / value list
        ListIterable<? extends CoreInstance> keyValues = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);

        CoreInstance classifier = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        CoreInstance instance = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), classifier);
        if (isGenericType)
        {
            Instance.addValueToProperty(instance, M3Properties.classifierGenericType, genericType, processorSupport);
        }

        //TODO - extend to cover generics
        boolean shouldValidate = Iterate.isEmpty(Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport));
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(classifier);

        // Set property values
        for (CoreInstance keyValue : keyValues)
        {
            // Find and validate Property
            String key = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport).getName();
            CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(classifier, key);
            if (property == null)
            {
                throw new PureExecutionException(Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport).getSourceInformation(), "The property '" + key + "' can't be found in the type '" + classifier.getName() + "' or in its hierarchy.");
            }

            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(keyValue, M3Properties.value, processorSupport);
            Instance.setValuesForProperty(instance, key, values, processorSupport);

            if (shouldValidate)
            {
                try
                {
                    PropertyValidator.validateMultiplicityRange(instance, property, values, processorSupport);

                    for (CoreInstance val : values)
                    {
                        PropertyValidator.validateTypeRange(instance, property, val, processorSupport);
                    }
                }
                catch (PureCompilationException ex)
                {
                    throw new PureExecutionException("Unable to create a new instance of class '" + classifier.getName() + "'. Invalid value '" + values.collect(new Function<CoreInstance, String>()
                    {
                        @Override
                        public String valueOf(CoreInstance object)
                        {
                            return object.printWithoutDebug("", 1);
                        }
                    }).makeString(",") + "' provided for class property '" + key + "': " + ex.getInfo());
                }
            }
        }

        // Set default values if the values for any required fields were not provided
        for (CoreInstance property: propertiesByName)
        {
            if(property.getValueForMetaPropertyToOne(M3Properties.defaultValue) != null && instance.getValueForMetaPropertyToMany(property.getName()).size() == 0)
            {
                CoreInstance expression = Property.getDefaultValueExpression(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));

                ListIterable<? extends CoreInstance> values = org.finos.legend.pure.m3.navigation.property.Property.getDefaultValue(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));
                for (CoreInstance value : values)
                {
                    if (Instance.instanceOf(expression, M3Paths.EnumStub, processorSupport)) {
                        Instance.addValueToProperty(instance, property.getName(), value.getValueForMetaPropertyToOne(M3Properties.resolvedEnum), processorSupport);
                    } else {
                        Instance.addValueToProperty(instance, property.getName(), value, processorSupport);
                    }
                }
            }
        }

        New.updateReverseProperties(instance, functionExpressionToUseInStack.getSourceInformation(), processorSupport);

        CoreInstance override = null ;
        if (params.size() > 5)
        {
           override =  processorSupport.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), M3Paths.ConstraintsGetterOverride);
           Instance.addValueToProperty(override, M3Properties.constraintsManager, Instance.getValueForMetaPropertyToOneResolved(params.get(5), M3Properties.values, processorSupport), processorSupport);
        }
        else
        {
            if(params.size() > 2 )
            {
                override =  processorSupport.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), M3Paths.GetterOverride);
            }
        }
        if( override != null)
        {
            Instance.addValueToProperty(override, M3Properties.getterOverrideToOne, Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport), processorSupport);
            Instance.addValueToProperty(override, M3Properties.getterOverrideToMany, Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport), processorSupport);
            Instance.addValueToProperty(override, M3Properties.hiddenPayload, Instance.getValueForMetaPropertyToOneResolved(params.get(4), M3Properties.values, processorSupport), processorSupport);
            Instance.addValueToProperty(instance, M3Properties.elementOverride, override, processorSupport);
        }
        CoreInstance value = ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);
        return DefaultConstraintHandler.handleConstraints(classifier,  value, functionExpressionToUseInStack.getSourceInformation(), this.functionExecution,resolvedTypeParameters,resolvedMultiplicityParameters,variableContext,functionExpressionToUseInStack,profiler,instantiationContext, executionSupport);
    }
}
