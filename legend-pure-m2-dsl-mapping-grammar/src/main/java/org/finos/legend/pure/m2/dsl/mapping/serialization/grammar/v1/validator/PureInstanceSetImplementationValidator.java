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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PureInstanceSetImplementationValidator implements MatchRunner<PureInstanceSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.PureInstanceSetImplementation;
    }

    @Override
    public void run(PureInstanceSetImplementation classMapping, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        Class mappedClass = (Class)ImportStub.withImportStubByPass(classMapping._classCoreInstance(), processorSupport);

        LambdaFunction filter = classMapping._filter();
        if (filter != null)
        {
            Validator.validate(filter, (ValidatorState)state, matcher, processorSupport);
            Type booleanType = (Type)processorSupport.package_getByUserPath(M3Paths.Boolean);
            GenericType filterReturnType = ((FunctionType)processorSupport.function_getFunctionType(filter))._returnType();
            if (filterReturnType._rawTypeCoreInstance() != booleanType)
            {
                throw new PureCompilationException(((RichIterable<ValueSpecification>)filter._expressionSequence()).toList().get(0).getSourceInformation(), "A filter should be a Boolean expression");
            }
        }

        MutableSet<String> requiredProperties = getRequiredProperties(mappedClass, processorSupport);
        for (PropertyMapping propertyMapping : classMapping._propertyMappings())
        {
            Property property = (Property)ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);
            requiredProperties.remove(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property));

            LambdaFunction transform = ((PurePropertyMapping)propertyMapping)._transform();
            FunctionType fType = (FunctionType)processorSupport.function_getFunctionType(transform);

            Validator.validate(transform, (ValidatorState)state, matcher, processorSupport);

            GenericType expressionGenericType = fType._returnType();
            GenericType propertyGenericType = ((Property)ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport))._genericType();

            Multiplicity expressionMultiplicity = fType._returnMultiplicity();
            Multiplicity propertyMultiplicity = ((Property)ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport))._multiplicity();

            if (((PurePropertyMapping)propertyMapping)._transformerCoreInstance() != null)
            {
                CoreInstance propertyRawType = ImportStub.withImportStubByPass(propertyGenericType._rawTypeCoreInstance(), processorSupport);
                EnumerationMapping transformer = (EnumerationMapping)ImportStub.withImportStubByPass(((PurePropertyMapping)propertyMapping)._transformerCoreInstance(), processorSupport);

                if (!propertyRawType.equals(transformer._enumeration()))
                {
                    throw new PureCompilationException(propertyMapping.getSourceInformation(), "Property : [" + property._name() + "] is of type : [" + PackageableElement.getUserPathForPackageableElement(propertyRawType) + "] but enumeration mapping : [" + transformer._name() + "] is defined on enumeration : [" + PackageableElement.getUserPathForPackageableElement(transformer._enumeration()) + "].");
                }
            }
            else if (ImportStub.withImportStubByPass(propertyGenericType._rawTypeCoreInstance(), processorSupport) instanceof DataType)
            {
                if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(expressionGenericType, propertyGenericType, processorSupport))
                {
                    String valTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(expressionGenericType, false, processorSupport);
                    String propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, false, processorSupport);
                    if (valTypeString.equals(propertyTypeString))
                    {
                        valTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(expressionGenericType, true, processorSupport);
                        propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, true, processorSupport);
                    }
                    throw new PureCompilationException(((RichIterable<ValueSpecification>)((PurePropertyMapping)propertyMapping)._transform()._expressionSequence()).toList().get(0).getSourceInformation(),
                            "Type Error: '" + valTypeString + "' not a subtype of '" + propertyTypeString + "'");
                }
            }
            else
            {
                Mapping mapping = (Mapping)ImportStub.withImportStubByPass(classMapping._parentCoreInstance(), processorSupport);
                SetImplementation setImplementation = org.finos.legend.pure.m2.dsl.mapping.Mapping.getClassMappingById(mapping, propertyMapping._targetSetImplementationId(), processorSupport);
                if (setImplementation == null)
                {
                    throw new PureCompilationException(propertyMapping.getSourceInformation(), "The set implementation '"+propertyMapping._targetSetImplementationId()+"' is unknown in the mapping '" + mapping.getName()+"'");
                }
                Type srcClass = setImplementation instanceof PureInstanceSetImplementation ?
                        (Type)ImportStub.withImportStubByPass(((PureInstanceSetImplementation)setImplementation)._srcClassCoreInstance(), processorSupport) : null;
                Type expRawType = (Type)ImportStub.withImportStubByPass(expressionGenericType._rawTypeCoreInstance(), processorSupport);
                if (srcClass != null && srcClass != expRawType)
                {
                    throw new PureCompilationException(((RichIterable<ValueSpecification>)((PurePropertyMapping)propertyMapping)._transform()._expressionSequence()).toList().get(0).getSourceInformation(),
                            "Type Error: '" + PackageableElement.getUserPathForPackageableElement(srcClass) + "' is not '" + PackageableElement.getUserPathForPackageableElement(expRawType) + "'");
                }
            }

            if (!((PurePropertyMapping)propertyMapping)._explodeProperty() && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(propertyMultiplicity, expressionMultiplicity))
            {
                throw new PureCompilationException(((RichIterable<ValueSpecification>)transform._expressionSequence()).toList().get(0).getSourceInformation(),
                        "Multiplicity Error ' The property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(propertyMapping._propertyCoreInstance())
                                + "' has a multiplicity range of " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(propertyMultiplicity)
                                + " when the given expression has a multiplicity range of " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(expressionMultiplicity));
            }
        }
        // TODO add this validation once violations have been removed
//        if (requiredProperties.notEmpty())
//        {
//            StringBuilder message = new StringBuilder("The following required properties for ");
//            _Class.print(message, mappedClass, true);
//            message.append(" are not mapped: ");
//            requiredProperties.toSortedList().appendString(message, ", ");
//            throw new PureCompilationException(classMapping.getSourceInformation(), message.toString());
//        }
    }

    private MutableSet<String> getRequiredProperties(Class mappedClass, final ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> classProperties = processorSupport.class_getSimplePropertiesByName(mappedClass);
        final MutableSet<String> requiredProperties = UnifiedSet.newSet(classProperties.size());
        classProperties.forEachKeyValue(new Procedure2<String, CoreInstance>()
        {
            @Override
            public void value(String propertyName, CoreInstance property)
            {
                Multiplicity multiplicity = ((Property)property)._multiplicity();
                if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.multiplicityLowerBoundToInt(multiplicity) > 0)
                {
                    // TODO figure out what to do about properties from associations (as they may not need to be mapped by both classes)
                    PropertyOwner owner = ((Property)property)._owner();
                    if (owner instanceof Class)
                    {
                        requiredProperties.add(propertyName);
                    }
                }
            }
        });
        return requiredProperties;
    }
}
