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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OtherwiseEmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PropertyMappingProcessor
{
    private PropertyMappingProcessor()
    {
    }

    public static void processPropertyMapping(PropertyMapping propertyMapping, ModelRepository repository, ProcessorSupport processorSupport, PropertyOwner propertyOwner, PropertyMappingsImplementation propertyMappingOwner) throws PureCompilationException
    {
        propertyMapping._owner(propertyMappingOwner);

        Property property;
        if (propertyMapping._localMappingProperty() != null &&  propertyMapping._localMappingProperty())
        {
            ClassInstance propertyClass = (ClassInstance) _Package.getByUserPath(M3Paths.Property, processorSupport);
            ClassInstance genericTypeClass = (ClassInstance) _Package.getByUserPath(M3Paths.GenericType, processorSupport);

            Property propertyInstance = (Property) repository.newAnonymousCoreInstance(propertyMapping.getSourceInformation(), propertyClass);

            GenericType sourceGenericType = (GenericType) repository.newAnonymousCoreInstance(null, genericTypeClass);
            sourceGenericType._rawTypeCoreInstance(null);

            GenericType targetGenericType = (GenericType) repository.newAnonymousCoreInstance(null, genericTypeClass);
            targetGenericType._rawTypeCoreInstance(propertyMapping._localMappingPropertyTypeCoreInstance());

            GenericType propertyGenericType = (GenericType) repository.newAnonymousCoreInstance(null, genericTypeClass);
            propertyGenericType._rawTypeCoreInstance(propertyClass);
            propertyGenericType._typeArguments(Lists.immutable.<GenericType>withAll(propertyGenericType._typeArguments()).newWith(sourceGenericType));
            propertyGenericType._typeArguments(Lists.immutable.<GenericType>withAll(propertyGenericType._typeArguments()).newWith(targetGenericType));
            // WRONG.. should come from grammar
            propertyGenericType._multiplicityArguments(Lists.immutable.<Multiplicity>withAll(propertyGenericType._multiplicityArguments()).newWith(propertyMapping._localMappingPropertyMultiplicity()));

            propertyInstance._classifierGenericType(propertyGenericType);
            propertyInstance._genericType(targetGenericType);
            propertyInstance._name(ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport).getName());

            propertyInstance._aggregation((Enum)((Enumeration)_Package.getByUserPath(M3Paths.AggregationKind, processorSupport))._values().toList().get(0));

            propertyInstance._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));
            property = propertyInstance;
        }
        else
        {
            // Property
            String propertyName = ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport).getName();
            property = getEdgePointProperty(propertyOwner, propertyName, processorSupport);
            if (property == null)
            {
                property = (Property) processorSupport.class_findPropertyUsingGeneralization(propertyOwner, propertyName);
            }
            if (property == null)
            {
                StringBuilder message = new StringBuilder("The property '");
                message.append(propertyName);
                message.append("' is unknown in the Element '");
                PackageableElement.writeUserPathForPackageableElement(message, propertyOwner);
                message.append('\'');
                throw new PureCompilationException(propertyMapping.getSourceInformation(), message.toString());
            }
        }
        propertyMapping._propertyCoreInstance(property);

        // TargetID
        String targetId = propertyMapping._targetSetImplementationId();
        Type targetClass = (Type)ImportStub.withImportStubByPass(property._classifierGenericType()._typeArguments().toList().get(1)._rawTypeCoreInstance(), processorSupport);
        if (targetId == null)
        {
            String targetIdString = targetClass instanceof DataType ? "" : PackageableElement.getUserPathForPackageableElement(targetClass, "_");
            propertyMapping._targetSetImplementationId(targetIdString);
        }

        // SourceId
        String sourceId = propertyMapping._sourceSetImplementationId();
        Class sourceClass = propertyOwner instanceof Class ? (Class) propertyOwner : getSourceClassForAssociationProperty(processorSupport, propertyOwner, property.getName());
        if (sourceId == null)
        {
            String sourceIdString = PackageableElement.getUserPathForPackageableElement(sourceClass, "_");
            propertyMapping._sourceSetImplementationId(sourceIdString);
        }

        if (propertyMapping instanceof PropertyMappingsImplementation)
        {
            PropertyMappingsImplementation propertyMappingsImplementation = (PropertyMappingsImplementation) propertyMapping;
            Class propertyCls = property._genericType() == null ? null : (Class) ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);//Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.genericType, M3Properties.rawType, processorSupport);
            for (PropertyMapping childPropertyMapping : propertyMappingsImplementation._propertyMappings())
            {
                processPropertyMapping(childPropertyMapping, repository, processorSupport, propertyCls, propertyMappingsImplementation);
            }
            Mapping mapping = (Mapping) ImportStub.withImportStubByPass(propertyMappingsImplementation._parentCoreInstance(), processorSupport);

            if (!mapping._classMappings().contains(propertyMapping))
            {
                mapping._classMappings(Lists.immutable.<SetImplementation>withAll(mapping._classMappings()).newWithAll(Lists.immutable.with((SetImplementation)propertyMappingsImplementation)));
            }
        }

        if (propertyMapping instanceof OtherwiseEmbeddedSetImplementation)
        {
            PropertyMapping otherwiseMapping = ((OtherwiseEmbeddedSetImplementation)propertyMapping)._otherwisePropertyMapping();
            otherwiseMapping._propertyCoreInstance(property);
            PropertyMappingsImplementation otherOwner = otherwiseMapping._owner();
            if (otherOwner == null)
            {
                otherwiseMapping._owner((OtherwiseEmbeddedSetImplementation)propertyMapping);
            }
        }
    }

    static void populateReferenceUsagesForPropertyMapping(PropertyMapping propertyMapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
        Property property = (Property) ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);
        ReferenceUsage.addReferenceUsage(property, propertyMapping, M3Properties.property, 0, repository, processorSupport);

        if (propertyMapping instanceof PropertyMappingsImplementation)
        {
            for (PropertyMapping childPropertyMapping : ((PropertyMappingsImplementation)propertyMapping)._propertyMappings())
            {
                populateReferenceUsagesForPropertyMapping(childPropertyMapping, repository, processorSupport);
            }
        }
    }

    private static Property getEdgePointProperty(PropertyOwner propertyOwner, String propertyName, ProcessorSupport processorSupport)
    {
        String propertyEdgePointName = MilestoningFunctions.getEdgePointPropertyName(propertyName);
        return (Property) processorSupport.class_findPropertyUsingGeneralization(propertyOwner, propertyEdgePointName);
    }

    private static Class getSourceClassForAssociationProperty(ProcessorSupport processorSupport, PropertyOwner propertyOwner, String propertyName)
    {
        RichIterable<? extends Property<?,?>> associationProperties = propertyOwner instanceof Class ? ((Class)propertyOwner)._properties() : ((Association)propertyOwner)._properties();
        Property otherProperty = associationProperties.detect(Predicates.attributeNotEqual(CoreInstance.GET_NAME, propertyName));
        return (Class) ImportStub.withImportStubByPass(otherProperty._classifierGenericType()._typeArguments().toList().get(1)._rawTypeCoreInstance(), processorSupport);
    }
}
