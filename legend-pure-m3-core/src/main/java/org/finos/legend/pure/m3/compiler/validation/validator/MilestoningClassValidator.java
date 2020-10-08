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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotypeEnum;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MilestoningClassValidator
{

    public static void runValidations(Class cls, MutableList<AbstractProperty<?>> properties, ProcessorSupport processorSupport)
    {
        validateNotMoreThanOneTemporalStereotypeSpecified(cls, processorSupport);
        validatePropertyNamesAgainstReservedTemporalPropertyNames(cls, properties, processorSupport);
    }

    private static void validatePropertyNamesAgainstReservedTemporalPropertyNames(Class cls, MutableList<AbstractProperty<?>> properties, final ProcessorSupport processorSupport)
    {
        ListIterable<String> temporalStereotypeNames = MilestoningFunctions.getAllTemporalStereoTypePropertyNamesFromTopMostNonTopTypeGeneralizations(cls, processorSupport);
        ListIterable<String> propertyNames = properties.select(Predicates.not(new Predicate<AbstractProperty<?>>()
        {
            @Override
            public boolean accept(AbstractProperty<?> property)
            {
                return MilestoningFunctions.isGeneratedMilestoningDateProperty(property, processorSupport);
            }
        })).collect(CoreInstance.GET_NAME);
        ListIterable<String> disallowedPropertyNames = propertyNames.select(Predicates.in(temporalStereotypeNames));
        if (disallowedPropertyNames.notEmpty())
        {
            throw new PureCompilationException(cls.getSourceInformation(), "Type: " + cls.getName() + " has temporal specification: " + temporalStereotypeNames.makeString("[", ",", "]") + " properties:" + disallowedPropertyNames.makeString("[", ",", "]") + " are reserved and should not be explicit in the Model");
        }
    }

    private static void validateNotMoreThanOneTemporalStereotypeSpecified(Class cls, ProcessorSupport processorSupport)
    {
        ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(cls, processorSupport);
        if (temporalStereotypes.size() > 1)
        {
            throw new PureCompilationException(cls.getSourceInformation(), "A Type may only have one Temporal Stereotype, '" + cls.getName() + "' has " + temporalStereotypes.collect(MilestoningFunctions.GET_PLATFORM_NAME).toSortedList().makeString("[", ",", "]"));
        }
    }


    static void validateTemporalStereotypesAppliedForAllSubTypesInTemporalHierarchy(Class cls, ProcessorSupport processorSupport)
    {
        if (!Type.directSubTypeOf(cls, processorSupport.type_TopType(), processorSupport))
        {
            ListIterable<MilestoningStereotypeEnum> topMostTypeMilestoningStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(cls, processorSupport);
            MilestoningStereotype subTypeMilestoningStereotype = MilestoningFunctions.getTemporalStereoTypesExcludingParents(cls, processorSupport).getFirst();
            boolean subTypeIsTemporal = subTypeMilestoningStereotype != null;

            if (!topMostTypeMilestoningStereotypes.isEmpty())
            {
                MilestoningStereotypeEnum topMostTypeMilestoningStereotype = topMostTypeMilestoningStereotypes.getFirst();

                boolean topMostTypeIsTemporal = topMostTypeMilestoningStereotype != null;
                MutableSet<CoreInstance> leafUserDefTypes = Type.getTopMostNonTopTypeGeneralizations(cls, processorSupport);
                String topMostClasses = leafUserDefTypes.collect(CoreInstance.GET_NAME).makeString(",");

                if (topMostTypeIsTemporal && !subTypeIsTemporal)
                {
                    throw new PureCompilationException(cls.getSourceInformation(), "Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype(s): '" + topMostClasses + "' has milestoning stereotype: '" + topMostTypeMilestoningStereotype + "'");
                }

                if (topMostTypeIsTemporal && subTypeIsTemporal && (topMostTypeMilestoningStereotype != subTypeMilestoningStereotype))
                {
                    throw new PureCompilationException(cls.getSourceInformation(), "All temporal stereotypes in a hierarchy must be the same, class: '" + cls.getName() + "' is " + subTypeMilestoningStereotype + ", top most supertype(s): '" + topMostClasses + "' has milestoning stereotype: '" + topMostTypeMilestoningStereotype + "'");
                }
            }
            else if (subTypeIsTemporal)
            {
                MutableSet<CoreInstance> leafUserDefTypes = Type.getTopMostNonTopTypeGeneralizations(cls, processorSupport);
                String topMostClasses = leafUserDefTypes.collect(CoreInstance.GET_NAME).makeString(",");
                throw new PureCompilationException(cls.getSourceInformation(), "All temporal stereotypes in a hierarchy must be the same, class: '" + cls.getName() + "' is " + subTypeMilestoningStereotype + ", top most supertype(s): '" + topMostClasses + "' is not temporal'");
            }
        }
    }
}
