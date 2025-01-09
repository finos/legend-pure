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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;

import java.util.UUID;

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
        ImportGroup importId = getImportIdForOwner(propertyOwner, processorSupport);
        return createM3MilestonedProperties(propertyOwner, importId, propertyCodeBlocks, context, processorSupport, modelRepository);
    }

    private static ImportGroup getImportIdForOwner(CoreInstance owner, ProcessorSupport processorSupport)
    {
        SourceInformation sourceInfo = owner.getSourceInformation();
        return (ImportGroup) Imports.getImportGroupsForSource(sourceInfo.getSourceId(), processorSupport)
                .asLazy()
                .select(ig -> ig.getSourceInformation().getStartLine() <= sourceInfo.getStartLine())
                .maxBy(ig -> ig.getSourceInformation().getStartLine());
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
        // we create a unique filename for this, which we will use to replace source info after parsing
        String fileName = UUID.randomUUID() + propertyOwner.getSourceInformation().getSourceId();
        MutableList<QualifiedProperty<? extends CoreInstance>> qps = Lists.mutable.empty();
        MutableList<Property<? extends CoreInstance, ?>> ps = Lists.mutable.empty();
        new M3AntlrParser().parseProperties(propertyCodeBlock.getCodeBlock(), fileName, ps, qps, typeOwner, importId, true, modelRepository, context, startingQualifiedPropertyIndex);
        AbstractProperty<?> property = ps.isEmpty() ? qps.getLast() : ps.getLast();
        if (property != null)
        {
            updatePropertySourceInformation(propertyCodeBlock, fileName, property, qps.notEmpty());
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

    private static void updatePropertySourceInformation(MilestonePropertyCodeBlock propertyCodeBlock, String fileName, AbstractProperty<?> property, boolean isQualifiedProperty)
    {
        GraphNodeIterable.builder()
                .withStartingNode(property)
                .withNodeFilter(node ->
                {
                    SourceInformation sourceInfo = node.getSourceInformation();
                    if (sourceInfo == null)
                    {
                        return GraphWalkFilterResult.REJECT_AND_CONTINUE;
                    }
                    if (fileName.equals(sourceInfo.getSourceId()))
                    {
                        return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
                    }
                    return GraphWalkFilterResult.REJECT_AND_STOP;
                })
                .build()
                .forEach(node -> node.setSourceInformation(propertyCodeBlock.getPropertySourceInformation()));

        if (propertyCodeBlock.isPropertyGenericTypeSourceInformationIsAvailable())
        {
            GenericType genericType = property._genericType();
            genericType.setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
            genericType._rawTypeCoreInstance().setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
            GenericType classifierGenericType = property._classifierGenericType();
            if (classifierGenericType != null)
            {
                if (isQualifiedProperty)
                {
                    GenericType typeArgument = classifierGenericType._typeArguments().getAny();
                    FunctionType functionType = (FunctionType) typeArgument._rawType();
                    functionType._returnType().setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
                }
                else
                {
                    ListIterable<? extends GenericType> typeArguments = ListHelper.wrapListIterable(classifierGenericType._typeArguments());
                    if (typeArguments.size() == 2)
                    {
                        typeArguments.get(1)._rawTypeCoreInstance().setSourceInformation(propertyCodeBlock.getPropertyGenericTypeSourceInformation());
                    }
                }
            }
        }
    }
}
