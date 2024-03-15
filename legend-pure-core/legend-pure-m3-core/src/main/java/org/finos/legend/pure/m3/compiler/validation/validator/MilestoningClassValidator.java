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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotypeEnum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MilestoningClassValidator
{
    public static void runValidations(Class<?> cls, Iterable<? extends AbstractProperty<?>> properties, ProcessorSupport processorSupport)
    {
        validateNotMoreThanOneTemporalStereotypeSpecified(cls, processorSupport);
        validatePropertyNamesAgainstReservedTemporalPropertyNames(cls, properties, processorSupport);
    }

    private static void validatePropertyNamesAgainstReservedTemporalPropertyNames(Class<?> cls, Iterable<? extends AbstractProperty<?>> properties, ProcessorSupport processorSupport)
    {
        MutableSet<String> reservedNames = MilestoningFunctions.getAllTemporalStereotypePropertyNameSetFromTopMostNonTopTypeGeneralizations(cls, processorSupport);
        MutableList<String> badPropertyNames = Iterate.collectIf(properties, p -> reservedNames.contains(p._name()) && !MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), CoreInstance::getName, Lists.mutable.empty());
        if (badPropertyNames.notEmpty())
        {
            StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Type: "), cls);
            reservedNames.toSortedList().appendString(builder, " has temporal specification: [", ", ", "]");
            badPropertyNames.sortThis().appendString(builder, " properties: [", ", ", "] are reserved and should not be explicit in the Model");
            throw new PureCompilationException(cls.getSourceInformation(), builder.toString());
        }
    }

    private static void validateNotMoreThanOneTemporalStereotypeSpecified(Class<?> cls, ProcessorSupport processorSupport)
    {
        ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(cls, processorSupport);
        if (temporalStereotypes.size() > 1)
        {
            StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("A Type may only have one Temporal Stereotype, '"), cls);
            temporalStereotypes.collect(MilestoningStereotype::getPurePlatformStereotypeName, Lists.mutable.ofInitialCapacity(temporalStereotypes.size()))
                    .sortThis()
                    .appendString(builder, "' has [", ", ", "]");
            throw new PureCompilationException(cls.getSourceInformation(), builder.toString());
        }
    }

    static void validateTemporalStereotypesAppliedForAllSubTypesInTemporalHierarchy(Class<?> cls, ProcessorSupport processorSupport)
    {
        if (!Type.directSubTypeOf(cls, processorSupport.type_TopType(), processorSupport))
        {
            ListIterable<MilestoningStereotypeEnum> topMostTypeMilestoningStereotypes = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(cls, processorSupport);
            MilestoningStereotype subTypeMilestoningStereotype = MilestoningFunctions.getTemporalStereoTypesExcludingParents(cls).getFirst();
            boolean subTypeIsTemporal = subTypeMilestoningStereotype != null;

            if (topMostTypeMilestoningStereotypes.notEmpty())
            {
                MilestoningStereotypeEnum topMostTypeMilestoningStereotype = topMostTypeMilestoningStereotypes.getFirst();

                boolean topMostTypeIsTemporal = topMostTypeMilestoningStereotype != null;
                if (topMostTypeIsTemporal && !subTypeIsTemporal)
                {
                    MutableSet<CoreInstance> topNonAnyTypes = Type.getTopMostNonTopTypeGeneralizations(cls, processorSupport);
                    StringBuilder builder = new StringBuilder("Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype");
                    if (topNonAnyTypes.size() != 1)
                    {
                        builder.append('s');
                    }
                    topNonAnyTypes.collect(PackageableElement::getUserPathForPackageableElement, Lists.mutable.ofInitialCapacity(topNonAnyTypes.size())).sortThis().appendString(builder, " ", ", ", " ");
                    builder.append((topNonAnyTypes.size() == 1) ? "has" : "have");
                    builder.append(" milestoning stereotype: '").append(topMostTypeMilestoningStereotype).append("'");
                    throw new PureCompilationException(cls.getSourceInformation(), builder.toString());
                }

                if (topMostTypeIsTemporal && subTypeIsTemporal && (topMostTypeMilestoningStereotype != subTypeMilestoningStereotype))
                {
                    MutableSet<CoreInstance> topNonAnyTypes = Type.getTopMostNonTopTypeGeneralizations(cls, processorSupport);
                    StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("All temporal stereotypes in a hierarchy must be the same, class "), cls);
                    builder.append(" is ").append(subTypeMilestoningStereotype).append(", top most supertype");
                    if (topNonAnyTypes.size() != 1)
                    {
                        builder.append('s');
                    }
                    topNonAnyTypes.collect(PackageableElement::getUserPathForPackageableElement, Lists.mutable.ofInitialCapacity(topNonAnyTypes.size())).sortThis().appendString(builder, " ", ", ", " ");
                    builder.append((topNonAnyTypes.size() == 1) ? "has" : "have");
                    builder.append(" milestoning stereotype: '").append(topMostTypeMilestoningStereotype).append("'");
                    throw new PureCompilationException(cls.getSourceInformation(), builder.toString());
                }
            }
            else if (subTypeIsTemporal)
            {
                MutableSet<CoreInstance> topNonAnyTypes = Type.getTopMostNonTopTypeGeneralizations(cls, processorSupport);
                StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("All temporal stereotypes in a hierarchy must be the same, class "), cls);
                builder.append(" is ").append(subTypeMilestoningStereotype).append(", top most supertype");
                if (topNonAnyTypes.size() != 1)
                {
                    builder.append('s');
                }
                topNonAnyTypes.collect(PackageableElement::getUserPathForPackageableElement, Lists.mutable.ofInitialCapacity(topNonAnyTypes.size())).sortThis().appendString(builder, " ", ", ", " ");
                builder.append((topNonAnyTypes.size() == 1) ? "is" : "are").append(" not temporal");
                throw new PureCompilationException(cls.getSourceInformation(), builder.toString());
            }
        }
    }
}
