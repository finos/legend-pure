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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.Executor;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.collection.map.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.constraints.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class New extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public New(ModelRepository repository, FunctionExecutionInterpreted functionExecution)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        // The parameter is a Class ... but we encode the typeArguments in the ValueExpression genericType's typeArguments ...
        CoreInstance __genericType = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, M3Properties.typeArguments, processorSupport);
        // Classifier
        final CoreInstance classifier = Instance.getValueForMetaPropertyToOneResolved(__genericType, M3Properties.rawType, processorSupport);

        if (classifier.equals(processorSupport.package_getByUserPath(M3Paths.Map)))
        {
            CoreInstance mapRawType = processorSupport.package_getByUserPath(M3Paths.Map);
            MapCoreInstance map = new MapCoreInstance(Lists.immutable.<CoreInstance>empty(), "", functionExpressionToUseInStack.getSourceInformation(), mapRawType, -1, this.repository, false, processorSupport);
            CoreInstance genericType = processorSupport.newGenericType(null, map, false);
            Instance.addValueToProperty(genericType, M3Properties.rawType, mapRawType, processorSupport);
            Instance.addValueToProperty(genericType, M3Properties.typeArguments, params.get(0).getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToMany(M3Properties.typeArguments), processorSupport);
            genericType = GenericType.makeTypeArgumentAsConcreteAsPossible(genericType, resolvedTypeParameters.elementAt(resolvedTypeParameters.size() - (resolvedTypeParameters.size() >= 2 ? 2 : 1)).asUnmodifiable(), Maps.immutable.<String, CoreInstance>of(), processorSupport);
            Instance.addValueToProperty(map, M3Properties.classifierGenericType, genericType, processorSupport);
            return ValueSpecificationBootstrap.wrapValueSpecification(map, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
        }

        instantiationContext.push(classifier);

        // Id
        String id = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName();
        // key / value list
        ListIterable<? extends CoreInstance> keyValues = params.size() > 2 ? Instance.getValueForMetaPropertyToManyResolved(params.get(2), M3Properties.values, processorSupport) : Lists.immutable.<CoreInstance>with();

        // Manage Generics
        if (Type.isBottomType(classifier, processorSupport))
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Cannot instantiate " + PackageableElement.getUserPathForPackageableElement(classifier, "::"));
        }

        CoreInstance genericType = classifier.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne( M3Properties.typeArguments);
        ListIterable<? extends CoreInstance> typeArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, processorSupport);
        ListIterable<? extends CoreInstance> multiplicityArguments = Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.multiplicityArguments, processorSupport);

        // TODO should we start a repository transaction here?
        final CoreInstance instance = id.isEmpty() ? this.repository.newEphemeralAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), classifier) : this.repository.newEphemeralCoreInstance(id, classifier, null);

        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);
        CoreInstance classifierGenericType = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), genericTypeType);
        Instance.addValueToProperty(classifierGenericType, M3Properties.rawType, classifier, processorSupport);
        for (CoreInstance typeArgument : typeArguments)
        {
            Instance.addValueToProperty(classifierGenericType, M3Properties.typeArguments, typeArgument, processorSupport);
        }
        for (CoreInstance multiplicityArgument : multiplicityArguments)
        {
            CoreInstance concreteMultiplicityArgument = Multiplicity.makeMultiplicityAsConcreteAsPossible(multiplicityArgument, resolvedMultiplicityParameters.peek().asUnmodifiable());
            Instance.addValueToProperty(classifierGenericType, M3Properties.multiplicityArguments, concreteMultiplicityArgument, processorSupport);
        }
        classifierGenericType = GenericType.makeTypeArgumentAsConcreteAsPossible(classifierGenericType, resolvedTypeParameters.peek().asUnmodifiable(), Maps.immutable.<String, CoreInstance>of(), processorSupport);
        Instance.addValueToProperty(instance, M3Properties.classifierGenericType, classifierGenericType, processorSupport);

        // Set property values
        VariableContext evaluationVariableContext = this.getParentOrEmptyVariableContext(variableContext);
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(classifier);
        for (CoreInstance keyValue : keyValues)
        {
            // Find and validate Property
            CoreInstance keyInstance = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport);
            String key = Instance.getValueForMetaPropertyToOneResolved(keyInstance, M3Properties.values, processorSupport).getName();
            SourceInformation sourceInfoForErrors = keyInstance.getSourceInformation();
            CoreInstance property = propertiesByName.get(key);
            if (property == null)
            {
                throw new PureExecutionException(sourceInfoForErrors, "The property '" + key + "' can't be found in the type '" + classifier.getName() + "' or in its hierarchy.");
            }
            CoreInstance expression = Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.expression, processorSupport);

            setValuesToProperty(expression, keyInstance, property, instance, sourceInfoForErrors, classifierGenericType, evaluationVariableContext, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, context, processorSupport);
        }

        // Set default values if the values for any required fields were not provided
        for (CoreInstance property: propertiesByName)
        {
            if(property.getValueForMetaPropertyToOne(M3Properties.defaultValue) != null && instance.getValueForMetaPropertyToMany(property.getName()).size() == 0)
            {
                CoreInstance expression = Property.getDefaultValueExpression(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));
                SourceInformation sourceInfoForErrors = expression.getSourceInformation();

                if (Instance.instanceOf(expression, M3Paths.EnumStub, processorSupport)) {
                    ListIterable<? extends CoreInstance> values = org.finos.legend.pure.m3.navigation.property.Property.getDefaultValue(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));
                    for (CoreInstance value : values)
                    {
                        Instance.addValueToProperty(instance, property.getName(), value.getValueForMetaPropertyToOne(M3Properties.resolvedEnum), processorSupport);
                    }
                } else {
                    setValuesToProperty(expression, expression, property, instance, sourceInfoForErrors, classifierGenericType, evaluationVariableContext, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, context, processorSupport);
                }
            }
        }

        instantiationContext.registerValidation(new Runnable()
        {
            @Override
            public void run()
            {
                // Verify that all property values meet multiplicity constraints
                validatePropertyValueMultiplicities(instance, classifier, functionExpressionToUseInStack.getSourceInformation(), processorSupport);
            }
        });

        updateReverseProperties(instance, functionExpressionToUseInStack.getSourceInformation(), processorSupport);

        CoreInstance value = ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);


        instantiationContext.popAndExecuteProcedures(value);

        if (instantiationContext.isEmpty())
        {
            instantiationContext.runValidations();
            instantiationContext.reset();
        }

        return DefaultConstraintHandler.handleConstraints(classifier,  value, functionExpressionToUseInStack.getSourceInformation(), this.functionExecution, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
    }

    private void setValuesToProperty(CoreInstance expression, CoreInstance keyInstance, CoreInstance property, final CoreInstance instance, SourceInformation sourceInfoForErrors, CoreInstance classifierGenericType, VariableContext evaluationVariableContext, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, final ProcessorSupport processorSupport)
    {
        CoreInstance propertyGenericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(instance, processorSupport), property, processorSupport);

        Executor executor = FunctionExecutionInterpreted.findValueSpecificationExecutor(expression, functionExpressionToUseInStack, processorSupport, this.functionExecution);
        CoreInstance evaluatedExpression = executor.execute(expression, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, evaluationVariableContext, profiler, instantiationContext, executionSupport, this.functionExecution, processorSupport);

        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(evaluatedExpression, M3Properties.values, processorSupport);

        if (org.finos.legend.pure.m3.navigation.measure.Measure.isUnitOrMeasureInstance(evaluatedExpression, processorSupport)) {
            values = Lists.mutable.with(evaluatedExpression);
        }

        if (values.notEmpty() && !ValueSpecification.isExecutable(evaluatedExpression, processorSupport) && Instance.instancesOf(values, M3Paths.ValueSpecification, processorSupport)) {
            values = ValueSpecification.instanceOf(evaluatedExpression, M3Paths.Nil, processorSupport) ? Lists.immutable.<CoreInstance>empty() : Lists.immutable.with(evaluatedExpression);
        }

        CoreInstance propertyMultiplicity = Property.resolveInstancePropertyReturnMultiplicity(instance, property, processorSupport);

        CoreInstance concretePropertyMultiplicity = resolveToConcreteMultiplicity(propertyMultiplicity, resolvedMultiplicityParameters);
        if (concretePropertyMultiplicity == null) {
            throw new PureExecutionException(keyInstance.getSourceInformation(), "Error instantiating the type '" + GenericType.print(classifierGenericType, processorSupport) + "'. Could not resolve multiplicity for the property '" + Property.getPropertyName(property) + "': " + Multiplicity.print(propertyMultiplicity));
        }
        validateRangeUsingMultiplicity(instance, property, concretePropertyMultiplicity, values.size(), sourceInfoForErrors, processorSupport);

        CoreInstance concretePropertyGenericType = resolveToConcreteGenericType(propertyGenericType, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);
        if (concretePropertyGenericType == null) {
            throw new PureExecutionException(keyInstance.getSourceInformation(), "Error instantiating the type '" + GenericType.print(classifierGenericType, processorSupport) + "'. Could not resolve type for the property '" + Property.getPropertyName(property) + "': " + GenericType.print(propertyGenericType, processorSupport));
        }
        for (CoreInstance value : values) {
            validateType(concretePropertyGenericType, value, expression, processorSupport);
            Instance.addValueToProperty(instance, property.getName(), value, processorSupport);
        }
    }

    private CoreInstance resolveToConcreteGenericType(CoreInstance genericType, Stack<MutableMap<String, CoreInstance>> resolvedTypeParametersStack, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParametersStack, ProcessorSupport processorSupport)
    {
        if (GenericType.isGenericTypeFullyConcrete(genericType, processorSupport))
        {
            return genericType;
        }
        CoreInstance resolved = genericType;
        for (int i = resolvedTypeParametersStack.size() - 2; i >= 0; i--)
        {
            MapIterable<String, CoreInstance> resolvedTypeParameters = resolvedTypeParametersStack.elementAt(i);
            MapIterable<String, CoreInstance> resolvedMultiplicityParameters = resolvedMultiplicityParametersStack.elementAt(i);
            if (resolvedTypeParameters.notEmpty() || resolvedMultiplicityParameters.notEmpty())
            {
                resolved = GenericType.makeTypeArgumentAsConcreteAsPossible(resolved, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);
                if (GenericType.isGenericTypeFullyConcrete(resolved, processorSupport))
                {
                    return resolved;
                }
            }
        }
        return null;
    }

    private CoreInstance resolveToConcreteMultiplicity(CoreInstance multiplicity, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParametersStack)
    {
        if (Multiplicity.isMultiplicityConcrete(multiplicity))
        {
            return multiplicity;
        }
        String parameter = Multiplicity.getMultiplicityParameter(multiplicity);
        for (int i = resolvedMultiplicityParametersStack.size() - 2; i >= 0; i--)
        {
            MapIterable<String, CoreInstance> resolvedMultiplicityParameters = resolvedMultiplicityParametersStack.elementAt(i);
            if (resolvedMultiplicityParameters.notEmpty())
            {
                CoreInstance resolved = resolvedMultiplicityParameters.get(parameter);
                if (resolved != null)
                {
                    if (Multiplicity.isMultiplicityConcrete(resolved))
                    {
                        return resolved;
                    }
                    parameter = Multiplicity.getMultiplicityParameter(resolved);
                }
            }
        }
        return null;
    }


    /**
     * Update the values of the reverse properties of all the properties
     * from associations for instance.  If a property has a reverse, then
     * for each value set for the property on instance, instance will be
     * added to the reverse property for value.
     *
     * @param instance            newly created instance
     * @param sourceInfoForErrors source information for error reports
     * @throws PureExecutionException
     */
    public static void updateReverseProperties(CoreInstance instance, SourceInformation sourceInfoForErrors, ProcessorSupport processorSupport) throws PureExecutionException
    {
        updateReverseProperties(instance, sourceInfoForErrors, processorSupport, false);
    }

    public static void updateReverseProperties(CoreInstance instance, SourceInformation sourceInfoForErrors, ProcessorSupport processorSupport, boolean skipMultiplicityChecks) throws PureExecutionException
    {
        CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
        for (String propertyName : instance.getKeys())
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, propertyName, processorSupport);
            if (values.notEmpty())
            {
                CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(processorSupport.getClassifier(instance), propertyName);
                CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
                if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
                {
                    ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(propertyOwner, M3Properties.properties, processorSupport);
                    CoreInstance reverseProperty = associationProperties.get(property == associationProperties.get(0) ? 1 : 0);
                    String reversePropertyName = Property.getPropertyName(reverseProperty);
                    for (CoreInstance value : values)
                    {
                        ListIterable<? extends CoreInstance> currentValues = Instance.getValueForMetaPropertyToManyResolved(value, reversePropertyName, processorSupport);
                        if (!currentValues.contains(instance))
                        {
                            int newSize = currentValues.size() + 1;
                            CoreInstance reversePropertyGenericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(value, processorSupport), reverseProperty, processorSupport);
                            validateType(reversePropertyGenericType, instance, value, processorSupport);
                            if(!skipMultiplicityChecks) validateRangeUsingMultiplicity(value, reverseProperty, Property.resolveInstancePropertyReturnMultiplicity(instance, reverseProperty, processorSupport), newSize, sourceInfoForErrors, processorSupport);
                            Instance.addValueToProperty(value, reversePropertyName, instance, processorSupport);
                        }
                    }
                }
            }
        }
    }

    private static void validateType(CoreInstance propertyGenericType, CoreInstance value, CoreInstance expression, ProcessorSupport processorSupport) throws PureExecutionException
    {
        boolean isUnitOrMeasure = Measure.isUnitOrMeasureInstance(value, processorSupport);
        CoreInstance valGenericType = Instance.instanceOf(value, M3Paths.NonExecutableValueSpecification, processorSupport) ? Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.genericType, processorSupport) : (isUnitOrMeasure? Instance.getValueForMetaPropertyToOneResolved(value, M3Properties.genericType, processorSupport) : Instance.extractGenericTypeFromInstance(value, processorSupport));
        validateTypeFromGenericType(propertyGenericType, valGenericType, expression, processorSupport);
    }

    static void validateTypeFromGenericType(CoreInstance propertyGenericType, CoreInstance valGenericType, CoreInstance expression, ProcessorSupport processorSupport) throws PureExecutionException
    {
        boolean compatible;
        try
        {
            compatible = GenericType.isGenericCompatibleWith(valGenericType, propertyGenericType, processorSupport);
        }
        catch (Exception e)
        {
            String valTypeString = GenericType.print(valGenericType, false, processorSupport);
            String propertyTypeString = GenericType.print(propertyGenericType, false, processorSupport);
            if (valTypeString.equals(propertyTypeString))
            {
                valTypeString = GenericType.print(valGenericType, true, processorSupport);
                propertyTypeString = GenericType.print(propertyGenericType, true, processorSupport);
            }
            throw new PureExecutionException(expression.getSourceInformation(), "Error checking if value type '" + valTypeString + "' is compatible with property type '" + propertyTypeString + "'", e);
        }
        if (!compatible)
        {
            String valTypeString = GenericType.print(valGenericType, false, processorSupport);
            String propertyTypeString = GenericType.print(propertyGenericType, false, processorSupport);
            if (valTypeString.equals(propertyTypeString))
            {
                valTypeString = GenericType.print(valGenericType, true, processorSupport);
                propertyTypeString = GenericType.print(propertyGenericType, true, processorSupport);
            }
            throw new PureExecutionException(expression.getSourceInformation(), "Type Error: '" + valTypeString + "' not a subtype of '" + propertyTypeString + "'" + (expression.getSourceInformation() == null ? expression.print("") : ""));
        }
    }

    static void validateRangeUsingMultiplicity(CoreInstance instance, CoreInstance keyValue, CoreInstance property, ListIterable<? extends CoreInstance> values, ProcessorSupport processorSupport) throws PureExecutionException
    {
        validateRangeUsingMultiplicity(instance, property, Property.resolveInstancePropertyReturnMultiplicity(instance, property, processorSupport), values.size(), Instance.getValueForMetaPropertyToOneResolved(keyValue, M3Properties.key, processorSupport).getSourceInformation(), processorSupport);
    }

    private static void validateRangeUsingMultiplicity(CoreInstance instance, CoreInstance property, CoreInstance propertyMultiplicity, int valueCount, SourceInformation sourceInfoForExceptions, ProcessorSupport processorSupport) throws PureExecutionException
    {
        if (!Multiplicity.isValid(propertyMultiplicity, valueCount))
        {
            throw new PureExecutionException(sourceInfoForExceptions, "Error instantiating the type '" + GenericType.print(Instance.extractGenericTypeFromInstance(instance, processorSupport), processorSupport) + "'. The property '" + Property.getPropertyName(property) + "' has a multiplicity range of " + Multiplicity.print(propertyMultiplicity) + " when the given list has a cardinality equal to " + valueCount);
        }
    }

    /**
     * Validate that all of instance's properties have a valid number of values
     * according to the property multiplicities.  A PureExecutionException will
     * be thrown if any property has an invalid number of values.
     *
     * @param instance   newly created instance
     * @param classifier instance classifier
     * @param sourceInfo source information for exceptions
     * @throws PureExecutionException if any property has an invalid number of values
     */
    public static void validatePropertyValueMultiplicities(CoreInstance instance, CoreInstance classifier, SourceInformation sourceInfo, ProcessorSupport processorSupport) throws PureExecutionException
    {
        validatePropertyValueMultiplicities(instance, classifier, processorSupport.class_getSimpleProperties(classifier), sourceInfo, processorSupport);
    }

    /**
     * Validate that all of instance's properties have a valid number of values
     * according to the property multiplicities.  A PureExecutionException will
     * be thrown if any property has an invalid number of values.
     *
     * @param instance   newly created instance
     * @param classifier instance classifier
     * @param sourceInfo source information for exceptions
     * @throws PureExecutionException if any property has an invalid number of values
     */
    public static void validatePropertyValueMultiplicities(CoreInstance instance, CoreInstance classifier, RichIterable<CoreInstance> propertiesToValidate, SourceInformation sourceInfo, ProcessorSupport processorSupport) throws PureExecutionException
    {
        MutableMap<String, Pair<String, Integer>> mismatches = Maps.mutable.with();
        for (CoreInstance property : propertiesToValidate)
        {
            // TODO consider non-concrete multiplicities
            CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport);
            if (Multiplicity.isMultiplicityConcrete(multiplicity))
            {
                ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, property, processorSupport);
                if (!Multiplicity.isValid(multiplicity, values.size()))
                {
                    mismatches.put(Property.getPropertyName(property), Tuples.pair(Multiplicity.print(multiplicity, false), values.size()));
                }
            }
        }

        if (mismatches.notEmpty())
        {
            StringBuilder message = new StringBuilder("Error instantiating class '" + classifier.getName() + "'.  The following properties have multiplicity violations:");
            if (mismatches.size() == 1)
            {
                String property = mismatches.keysView().getFirst();
                Pair<String, Integer> mismatch = mismatches.get(property);
                String multiplicity = mismatch.getOne();
                Integer count = mismatch.getTwo();

                message.append(" '");
                message.append(property);
                message.append("' requires ");
                message.append(multiplicity);
                message.append(" value");
                if (!"1".equals(multiplicity))
                {
                    message.append('s');
                }
                message.append(", got ");
                message.append(count);
            }
            else
            {
                for (String property : mismatches.keysView().toSortedList())
                {
                    Pair<String, Integer> mismatch = mismatches.get(property);
                    String multiplicity = mismatch.getOne();
                    Integer count = mismatch.getTwo();

                    message.append("\n\t'");
                    message.append(property);
                    message.append("' requires ");
                    message.append(multiplicity);
                    message.append(" value");
                    if (!"1".equals(multiplicity))
                    {
                        message.append('s');
                    }
                    message.append(", got ");
                    message.append(count);
                }
            }
            throw new PureExecutionException(sourceInfo, message.toString());
        }
    }
}
