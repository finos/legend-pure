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

package org.finos.legend.pure.m3.compiler.validation.functionExpression;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.lang.reflect.Field;

public class NewValidator
{
    private static ImmutableSet<String> EXCEPTION_FILES;

    static
    {
        try
        {
            java.lang.Class externalClass = NewValidator.class.getClassLoader().loadClass("org.finos.legend.pure.m3.compiler.validation.functionExpression.NewValidatorExclusions");
            Field field = externalClass.getField("EXCEPTION_FILES");
            EXCEPTION_FILES = (ImmutableSet<String>) field.get(null);
        }
        catch (Exception e)
        {
            EXCEPTION_FILES = Sets.immutable.empty();
        }
    }

    public static void validateNew(Matcher matcher, ValidatorState state, FunctionExpression expression, ProcessorSupport processorSupport) throws PureCompilationException
    {
        GenericType genericType = expression._parametersValues().getFirst()._genericType()._typeArguments().getFirst();
        validateClass(expression, genericType, processorSupport);
        validateProperties(matcher, state, expression, genericType, processorSupport);
    }

    private static void validateClass(FunctionExpression expression, GenericType genericType, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Type classifier = (Type) ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
        if (!(classifier instanceof Class))
        {
            StringBuilder message = new StringBuilder("Cannot instantiate a non-Class: ");
            if (classifier instanceof PackageableElement)
            {
                org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(message, classifier);
            }
            else
            {
                message.append(classifier.getName());
            }
            throw new PureCompilationException(expression.getSourceInformation(), message.toString());
        }
        GenericTypeValidator.validateGenericType(genericType, true, processorSupport);
    }

    private static void validateProperties(Matcher matcher, ValidatorState state, FunctionExpression expression, GenericType genericType, ProcessorSupport processorSupport)
    {
        Type classifier = (Type) ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(classifier);
        MutableSet<String> remainingProperties = propertiesByName.keysView().toSet();

        ListIterable<? extends ValueSpecification> parametersValues = expression._parametersValues().toList();
        if (parametersValues.size() > 2)
        {
            if (parametersValues.get(2) instanceof InstanceValue)
            {
                for (CoreInstance keyValue : ((InstanceValue) parametersValues.get(2))._valuesCoreInstance())
                {
                    if (keyValue instanceof KeyExpression)
                    {
                        // Validate key is an actual property
                        InstanceValue keyInstance = ((KeyExpression) keyValue)._key();
                        RichIterable<? extends CoreInstance> keys = keyInstance._valuesCoreInstance();
                        if (keys.size() != 1)
                        {
                            throw new PureCompilationException(keyInstance.getSourceInformation(), "Chained properties are not allowed in new expressions");
                        }
                        CoreInstance key = keys.getFirst();
                        String propertyName = key.getName();
                        CoreInstance property = propertiesByName.get(propertyName);
                        if (property == null)
                        {
                            StringBuilder message = new StringBuilder("The property '");
                            message.append(propertyName);
                            message.append("' can't be found in the type '");
                            message.append(classifier.getName());  // TODO we should write the full path
                            message.append("' or in its hierarchy.");
                            throw new PureCompilationException(keyInstance.getSourceInformation(), message.toString());
                        }
                        remainingProperties.remove(propertyName);

                        validatePropertyValue(matcher, state, expression, (KeyExpression) keyValue, genericType, property, processorSupport);
                    }
                }
            }
        }
        // TODO remove exception check when there are no more files with violations
        SourceInformation sourceInfo = expression.getSourceInformation();
        if ((sourceInfo == null) || !EXCEPTION_FILES.contains(sourceInfo.getSourceId()))
        {
            for (String propertyName : remainingProperties)
            {
                CoreInstance property = propertiesByName.get(propertyName);
                CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
                if (!(propertyOwner instanceof Association))
                {
                    CoreInstance propertyMultiplicity = Property.resolvePropertyReturnMultiplicity(genericType, property, processorSupport);
                    ListIterable<? extends CoreInstance> defaultValue = org.finos.legend.pure.m3.navigation.property.Property.getDefaultValue(property.getValueForMetaPropertyToOne(M3Properties.defaultValue));
                    if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(propertyMultiplicity) && !(org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isValid(propertyMultiplicity, 0) || defaultValue.size() > 0))
                    {
                        StringBuilder message = new StringBuilder("Missing value(s) for required property '");
                        message.append(propertyName);
                        message.append("' which has a multiplicity of ");
                        org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(message, propertyMultiplicity, true);
                        message.append(" for type ");
                        org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(message, classifier);
                        throw new PureCompilationException(expression.getSourceInformation(), message.toString());
                    }
                }
            }
        }
    }

    static void validatePropertyValue(Matcher matcher, ValidatorState state, FunctionExpression expression, KeyExpression keyValue, GenericType genericType, CoreInstance property, ProcessorSupport processorSupport)
    {
        // Validate value
        ValueSpecification value = keyValue._expression();
        Validator.validate(value, state, matcher, processorSupport);

        // Validate the type of value is compatible with the property type (when possible)
        GenericType propertyGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.resolvePropertyReturnType(genericType, property, processorSupport);
        if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeFullyConcrete(propertyGenericType, processorSupport))
        {
            // TODO remove this condition once we fix issues with property compatibility
            if (!org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(ImportStub.withImportStubByPass(propertyGenericType._rawTypeCoreInstance(), processorSupport), processorSupport.package_getByUserPath(M3Paths.Property), processorSupport))
            {
                GenericType valueGenericType = value._genericType();
                if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeFullyConcrete(valueGenericType, processorSupport))
                {
                    if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(valueGenericType, propertyGenericType, processorSupport))
                    {
                        String expressionTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(valueGenericType, false, processorSupport);
                        String propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, false, processorSupport);
                        if (expressionTypeString.equals(propertyTypeString))
                        {
                            expressionTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(valueGenericType, true, processorSupport);
                            propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, true, processorSupport);
                        }
                        throw new PureCompilationException(keyValue.getSourceInformation(), "Type Error: " + expressionTypeString + " not a subtype of " + propertyTypeString + (expression.getSourceInformation() == null ? expression.print("") : ""));
                    }
                }
            }
        }

        // Validate the multiplicity of the value is compatible with the property multiplicity (when possible)
        CoreInstance propertyMultiplicity = Property.resolvePropertyReturnMultiplicity(genericType, property, processorSupport);
        if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(propertyMultiplicity))
        {
            Multiplicity valueMultiplicity = value._multiplicity();
            if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isMultiplicityConcrete(valueMultiplicity) && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(propertyMultiplicity, valueMultiplicity))
            {
                StringBuilder message = new StringBuilder("Multiplicity Error: ");
                org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(message, valueMultiplicity, true);
                message.append(" is not compatible with ");
                org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(message, propertyMultiplicity, true);
                throw new PureCompilationException(keyValue.getSourceInformation(), message.toString());
            }
        }
    }
}
