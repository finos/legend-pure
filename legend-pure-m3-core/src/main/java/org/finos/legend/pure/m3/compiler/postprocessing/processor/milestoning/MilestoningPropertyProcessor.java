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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MilestoningPropertyProcessor
{
    private MilestoningPropertyProcessor()
    {
    }

    public static void process(Class<?> propertyOwner, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository) throws PureCompilationException
    {
        ListIterable<GeneratedMilestonedProperties> generatedMilestonedProperties = getSynthesizedMilestonedProperties(propertyOwner, propertyOwner, propertyOwner._properties(), modelRepository, context, processorSupport);

        ListIterable<AbstractProperty<?>> generatedQualifiedProperties = generatedMilestonedProperties.flatCollect(GeneratedMilestonedProperties::getQualifiedProperties);
        ListIterable<AbstractProperty<?>> generatedEdgePointProperties = generatedMilestonedProperties.collect(GeneratedMilestonedProperties::getEdgePointProperty);

        addSynthesizedQualifiedProperties(propertyOwner, generatedQualifiedProperties, context);
        addSynthesizedSimpleProperties(propertyOwner, generatedEdgePointProperties, context);
        moveProcessedoriginalMilestonedProperties(propertyOwner, generatedMilestonedProperties.collect(GeneratedMilestonedProperties::getSourceMilestonedProperty), context, processorSupport);
    }

    public static GeneratedMilestonedProperties processAssociationProperty(Association propertyOwner, Class<?> sourceType, Property<?, ?> property, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository) throws PureCompilationException
    {
        ListIterable<GeneratedMilestonedProperties> generatedMilestonedProperties = getSynthesizedMilestonedProperties(propertyOwner, sourceType, Lists.immutable.with(property), modelRepository, context, processorSupport);

        addSynthesizedQualifiedProperties(propertyOwner, generatedMilestonedProperties.flatCollect(GeneratedMilestonedProperties::getQualifiedProperties), context);
        addSynthesizedSimpleProperties(propertyOwner, generatedMilestonedProperties.collect(GeneratedMilestonedProperties::getEdgePointProperty), context);

        return generatedMilestonedProperties.notEmpty() ? generatedMilestonedProperties.get(0) : new GeneratedMilestonedProperties(property);
    }

    private static void addSynthesizedSimpleProperties(PropertyOwner propertyOwner, ListIterable<AbstractProperty<?>> synthesizedMilestonedProperties, Context context)
    {
        if (synthesizedMilestonedProperties.notEmpty())
        {
            // TODO should we validate anything here, like name conflicts?
            RichIterable<? extends Property<?, ?>> existingProperties = PropertyOwnerStrategy.getPropertyOwnerStrategy(propertyOwner).properties(propertyOwner);
            ListIterable<? extends CoreInstance> updated = Lists.mutable.<CoreInstance>withAll(existingProperties).withAll(synthesizedMilestonedProperties);
            MilestoningFunctions.setProperties(updated, propertyOwner, context);
        }
    }

    private static void addSynthesizedQualifiedProperties(PropertyOwner propertyOwner, ListIterable<? extends AbstractProperty<?>> synthesizedMilestonedProperties, Context context)
    {
        if (synthesizedMilestonedProperties.notEmpty())
        {
            RichIterable<? extends AbstractProperty<?>> existingProperties = PropertyOwnerStrategy.getPropertyOwnerStrategy(propertyOwner).qualifiedProperties(propertyOwner);
            MutableList<? extends CoreInstance> updatedProperties = Lists.mutable.<CoreInstance>withAll(existingProperties).withAll(synthesizedMilestonedProperties);
            MilestoningFunctions.setQualifiedProperties(updatedProperties, propertyOwner, context);
        }
    }


    public static void moveProcessedoriginalMilestonedProperties(PropertyOwner propertyOwner, ListIterable<? extends Property<?, ?>> propertiesToMove, Context context, ProcessorSupport processorSupport)
    {
        PropertyOwnerStrategy propertyOwnerStrategy = PropertyOwnerStrategy.getPropertyOwnerStrategy(propertyOwner);
        propertiesToMove.forEach(propertyToMove ->
        {
            propertyOwnerStrategy.propertiesRemove(propertyOwner, propertyToMove);
            propertyOwner.addKeyValue(M3PropertyPaths.originalMilestonedProperties, propertyToMove);
        });
        MilestoningFunctions.updateAndInvalidate(propertyOwner, context);
    }


    public static ListIterable<GeneratedMilestonedProperties> getSynthesizedMilestonedProperties(PropertyOwner propertyOwner, CoreInstance propertySourceType, Iterable<? extends Property<?, ?>> properties, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        MutableList<GeneratedMilestonedProperties> synthesizedMilestonedProperties = Lists.mutable.empty();
        ListIterable<MilestoningStereotypeEnum> ownerMilestoneStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(propertySourceType, processorSupport);
        properties.forEach(property ->
        {
            CoreInstance returnType = ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
            if (returnType != null)
            {
                ListIterable<MilestoningStereotypeEnum> returnTypeMilestoneStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(returnType, processorSupport);
                if (returnTypeMilestoneStereotypes.notEmpty())
                {
                    GeneratedMilestonedProperties generatedMilestonedProperties = new GeneratedMilestonedProperties(property);
                    String returnTypeIdOrPath = PackageableElement.getUserPathForPackageableElement(returnType);
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicity = property._multiplicity();
                    returnTypeMilestoneStereotypes.forEach(milestoneStereotype ->
                    {
                        ListIterable<AbstractProperty<?>> qualifiedProperties = getQualifiedProperties(property, propertyOwner, multiplicity, returnTypeIdOrPath, milestoneStereotype, ownerMilestoneStereotypes, context, processorSupport, modelRepository);
                        generatedMilestonedProperties.addQualifiedProperties(qualifiedProperties);
                    });
                    generatedMilestonedProperties.setEdgePointProperty(getEdgePointProperty(property, propertyOwner, multiplicity, returnTypeIdOrPath, modelRepository, context, processorSupport));
                    synthesizedMilestonedProperties.add(generatedMilestonedProperties);
                }
            }
        });
        return synthesizedMilestonedProperties;
    }

    private static ListIterable<AbstractProperty<?>> getQualifiedProperties(AbstractProperty<?> property, CoreInstance propertyOwner, CoreInstance multiplicity, String returnTypeIdOrPath, MilestoningStereotypeEnum milestoneStereotype, ListIterable<MilestoningStereotypeEnum> ownerMilestoneStereotypes, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        String multiplicityAsString = Multiplicity.print(multiplicity, false);
        ListIterable<MilestonePropertyCodeBlock> milestonePropertyCodeBlock = milestoneStereotype.getQualifiedPropertyCodeBlocks(ownerMilestoneStereotypes, property, multiplicityAsString, returnTypeIdOrPath);
        return PropertyInstanceBuilder.createMilestonedProperties(property, propertyOwner, milestonePropertyCodeBlock, context, processorSupport, modelRepository);
    }

    private static AbstractProperty<?> getEdgePointProperty(Property<?, ?> property, CoreInstance propertyOwner, CoreInstance multiplicity, String returnTypeIdOrPath, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        String multiplicityAsString = getEdgePointMultiplicityWithManyUpperBound(multiplicity);
        MilestonePropertyCodeBlock edgePointCodeBlock = getEdgePointPropertyCodeBlock(property, returnTypeIdOrPath, multiplicityAsString);
        return PropertyInstanceBuilder.createMilestonedProperties(propertyOwner, Lists.immutable.with(edgePointCodeBlock), context, processorSupport, modelRepository).getFirst();
    }

    private static String getEdgePointMultiplicityWithManyUpperBound(CoreInstance multiplicity)
    {
        int lowerBound = Multiplicity.multiplicityLowerBoundToInt(multiplicity);
        return (lowerBound == 0) ? "*" : (lowerBound + "..*");
    }

    private static MilestonePropertyCodeBlock getEdgePointPropertyCodeBlock(AbstractProperty<?> property, String returnTypeIdOrPath, String multiplicity)
    {
        String propertyDefinition = MilestoningFunctions.GENERATED_MILESTONING_STEREOTYPE + " " + MilestoningFunctions.getEdgePointPropertyName(property.getName()) + " : " + returnTypeIdOrPath + "[" + multiplicity + "];";
        return new MilestonePropertyCodeBlock(MilestonePropertyCodeBlock.MilestonePropertyHolderType.REGULAR, propertyDefinition, property, property.getSourceInformation(), property.getSourceInformation());
    }
}