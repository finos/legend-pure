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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class PropertyInstanceBuilder
{
    private PropertyInstanceBuilder()
    {
    }

    static ListIterable<AbstractProperty<?>> createMilestonedProperties(CoreInstance sourceProperty, CoreInstance propertyOwner, ListIterable<MilestonePropertyCodeBlock> propertyCodeBlocks, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        CoreInstance rawType = sourceProperty instanceof AbstractProperty ? ((AbstractProperty<?>) sourceProperty)._genericType()._rawTypeCoreInstance() : null;
        ImportGroup importId = rawType instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub ? ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub) rawType)._importGroup() : null;
        return createM3MilestonedProperties(propertyOwner, importId, propertyCodeBlocks, context, processorSupport, modelRepository);
    }

    public static AbstractProperty<?> createPropertyForQualifiedProperty(CoreInstance propertyOwner, QualifiedProperty<?> qualifiedProperty, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        String propertyName = qualifiedProperty._functionName();
        CoreInstance returnType = ImportStub.withImportStubByPass(qualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);
        String returnTypeIdOrPath = PackageableElement.getUserPathForPackageableElement(returnType);
        String multiplicity = getMultiplicity(qualifiedProperty);
        String propertyDefinition = propertyName + ":" + returnTypeIdOrPath + "[" + multiplicity + "];";
        MilestonePropertyCodeBlock propertyCodeBlock = new MilestonePropertyCodeBlock(MilestonePropertyCodeBlock.MilestonePropertyHolderType.REGULAR, propertyDefinition, qualifiedProperty, qualifiedProperty.getSourceInformation(), qualifiedProperty._genericType().getSourceInformation());
        return createMilestonedProperties(propertyOwner, Lists.mutable.with(propertyCodeBlock), context, processorSupport, modelRepository).getFirst();
    }

    private static String getMultiplicity(QualifiedProperty<?> property)
    {
        return Multiplicity.print(property._multiplicity(), false);
    }

    static ListIterable<AbstractProperty<?>> createMilestonedProperties(CoreInstance propertyOwner, ListIterable<MilestonePropertyCodeBlock> propertyCodeBlocks, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        ImportGroup importId = (ImportGroup) PackageableElement.findPackageableElement("system::imports::" + getImportIdForOwner(propertyOwner), modelRepository);
        return createM3MilestonedProperties(propertyOwner, importId, propertyCodeBlocks, context, processorSupport, modelRepository);
    }

    private static String getImportIdForOwner(CoreInstance owner)
    {
        return Source.importForSourceName(owner.getSourceInformation().getSourceId()) + "_1";
    }


    private static ListIterable<AbstractProperty<?>> createM3MilestonedProperties(CoreInstance propertyOwner, ImportGroup importId, ListIterable<MilestonePropertyCodeBlock> propertyCodeBlocks, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        int offset = propertyOwner.getValueForMetaPropertyToMany(M3Properties.qualifiedProperties).size();
        MutableList<AbstractProperty<?>> newProperties = Lists.mutable.ofInitialCapacity(propertyCodeBlocks.size());
        propertyCodeBlocks.forEachWithIndex((propertyCodeBlock, i) -> newProperties.add(createM3Property(importId, (PropertyOwner) propertyOwner, propertyCodeBlock, modelRepository, context, processorSupport, i + offset)));
        return newProperties;
    }

    private static AbstractProperty<?> createM3Property(ImportGroup importId, PropertyOwner propertyOwner, MilestonePropertyCodeBlock propertyCodeBlock, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport, int startingQualifiedPropertyIndex)
    {
        ImportStubInstance typeOwner = ImportStubInstance.createPersistent(modelRepository, null, propertyOwner.getSourceInformation(), PackageableElement.getUserPathForPackageableElement(propertyOwner), importId);
        String fileName = propertyOwner.getSourceInformation().getSourceId();
        MutableList<QualifiedProperty<? extends CoreInstance>> qps = Lists.mutable.empty();
        MutableList<Property<? extends CoreInstance, ?>> ps = Lists.mutable.empty();
        new M3AntlrParser().parseProperties(propertyCodeBlock.getCodeBlock(), fileName, ps, qps, typeOwner, importId, true, modelRepository, context, startingQualifiedPropertyIndex);
        AbstractProperty<?> property = ps.isEmpty() ? qps.getLast() : ps.getLast();
        if (property != null)
        {
            updatePropertySourceInformation(propertyCodeBlock, property, qps.notEmpty());
            property._owner(propertyOwner);
            property._stereotypesAddAll(propertyCodeBlock.getNonMilestonedStereotypes(processorSupport));
            property._taggedValuesAddAll(propertyCodeBlock.getTaggedValues());
            AbstractProperty<?> sourceProperty = propertyCodeBlock.getSourceProperty();
            if (sourceProperty instanceof Property && property instanceof Property)
            {
                Enum aggregation = ((Property<?, ?>) sourceProperty)._aggregation();
                ((Property<?, ?>) property)._aggregation(aggregation);
            }
        }
        return property;
    }

    private static void updatePropertySourceInformation(MilestonePropertyCodeBlock propertyCodeBlock, AbstractProperty<?> property, boolean isQualifiedProperty)
    {
        property.setSourceInformation(propertyCodeBlock.getPropertySourceInformation());
        if (propertyCodeBlock.isPropertyGenericTypeSourceInformationIsAvailable())
        {
            GenericType genericType = property._genericType();
            CoreInstance rawType = genericType._rawTypeCoreInstance();
            GenericType classifierGenericType = property._classifierGenericType();
            if (!isQualifiedProperty && classifierGenericType != null)
            {
                ListIterable<? extends GenericType> typeArguments = classifierGenericType._typeArguments().toList();
                if (typeArguments.size() == 2)
                {
                    typeArguments.get(1)._rawTypeCoreInstance().setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
                }
            }
            genericType.setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
            rawType.setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
        }
    }
}
