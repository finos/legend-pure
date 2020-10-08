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

package org.finos.legend.pure.m3.navigation.type;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.linearization.C3Linearization;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class Type
{
    public static CoreInstance wrapGenericType(CoreInstance type, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = processorSupport.newGenericType(sourceInformation, type, false);
        Instance.addValueToProperty(genericType, M3Properties.rawType, type, processorSupport);
        return genericType;
    }

    public static CoreInstance wrapGenericType(CoreInstance type, SourceInformation sourceInformation, ProcessorSupport processorSupport, boolean inferred)
    {
        CoreInstance genericType = processorSupport.newGenericType(sourceInformation, type, inferred);
        Instance.addValueToProperty(genericType, M3Properties.rawType, type, processorSupport);
        return genericType;
    }

    public static CoreInstance wrapGenericType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return wrapGenericType(type, null, processorSupport);
    }

    /**
     * Return whether type is a primitive type.  That is, whether
     * type is an instance of the class PrimitiveType.
     *
     * @param type    Pure type
     * @return whether type is an instance of PrimitiveType
     */
    public static boolean isPrimitiveType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return (type != null) && processorSupport.type_subTypeOf(type.getClassifier(), processorSupport.package_getByUserPath(M3Paths.PrimitiveType));
    }

    /**
     * Return whether type is the bottom type (i.e., Nil).
     *
     * @param type    Pure type
     * @return whether type is the bottom type
     */
    public static boolean isBottomType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return (type != null) && (type == processorSupport.type_BottomType());
    }

    /**
     * Return whether type is the top type (i.e., Any).
     *
     * @param type    Pure type
     * @return whether type is the top type
     */
    public static boolean isTopType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return (type != null) && (type == processorSupport.type_TopType());
    }

    /**
     * Get the resolution order for generalizations of type, including type
     * itself.  The first element of the resulting list will always be type itself.
     * If the hierarchy is inconsistent (e.g., type is a proper generalization
     * of itself), an exception is thrown.
     *
     * @param type             type
     * @param processorSupport processor support
     * @return generalization resolution order
     */
    public static ListIterable<CoreInstance> getGeneralizationResolutionOrder(CoreInstance type, ProcessorSupport processorSupport)
    {
        return C3Linearization.getTypeGeneralizationLinearization(type, processorSupport);
    }

    public static MutableSet<CoreInstance> getTopMostNonTopTypeGeneralizations(CoreInstance type, final ProcessorSupport processorSupport)
    {
        ListIterable leafNodes = getGeneralizationResolutionOrder(type, processorSupport).select(new Predicate<CoreInstance>() {
            @Override
            public boolean accept(CoreInstance cI) {
                return getDirectGeneralizations(cI, processorSupport).contains(processorSupport.type_TopType());
            }
        });
        return leafNodes.toSet();
    }

    /**
     * Get the direct generalizations of type.  These are returned in the
     * order that they are defined on type.
     *
     * @param type type
     * @param processorSupport processor support
     * @return direct generalizations of type
     */
    public static ListIterable<CoreInstance> getDirectGeneralizations(CoreInstance type, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> rawGeneralizations = Instance.getValueForMetaPropertyToManyResolved(type, M3Properties.generalizations, processorSupport);
        MutableList<CoreInstance> directGeneralizations = FastList.newList(rawGeneralizations.size());
        for (CoreInstance rawGeneralization : rawGeneralizations)
        {
            directGeneralizations.add(Instance.getValueForMetaPropertyToOneResolved(rawGeneralization, M3Properties.general, M3Properties.rawType, processorSupport));
        }
        return directGeneralizations;
    }

    /**
     * Return whether type is a subtype of possibleSuperType.  Note that Nil is
     * a subtype of all types and Any is a super type of all types.
     *
     * @param type              type
     * @param possibleSuperType possible super type
     * @param processorSupport  processor support
     * @return whether type is a subtype of possibleSuperType
     */
    public static boolean subTypeOf(CoreInstance type, CoreInstance possibleSuperType, ProcessorSupport processorSupport)
    {
        return (type == possibleSuperType) ||
                isBottomType(type, processorSupport) ||
                isTopType(possibleSuperType, processorSupport) ||
                getGeneralizationResolutionOrder(type, processorSupport).contains(possibleSuperType);
    }

    /**
     * Return whether type is a direct subtype of possibleDirectSuperType.
     *
     * @param type type
     * @param possibleDirectSuperType possible direct super type
     * @param processorSupport processor support
     * @return whether type is a direct subtype of possibleDirectSuperType
     */
    public static boolean directSubTypeOf(CoreInstance type, CoreInstance possibleDirectSuperType, ProcessorSupport processorSupport)
    {
        for (CoreInstance rawGeneralization : Instance.getValueForMetaPropertyToManyResolved(type, M3Properties.generalizations, processorSupport))
        {
            if (possibleDirectSuperType == Instance.getValueForMetaPropertyToOneResolved(rawGeneralization, M3Properties.general, M3Properties.rawType, processorSupport))
            {
                return true;
            }
        }
        return false;
    }

    public static SetIterable<CoreInstance> getAllSubTypes(CoreInstance type)
    {
        MutableSet<CoreInstance> result = Sets.mutable.empty();
        collectAllSubTypes(type, result);
        return result;
    }

    private static void collectAllSubTypes(CoreInstance type, MutableSet<CoreInstance> result)
    {
        if (result.add(type))
        {
            for (CoreInstance specialization : type.getValueForMetaPropertyToMany(M3Properties.specializations))
            {
                collectAllSubTypes(specialization.getValueForMetaPropertyToOne(M3Properties.specific), result);
            }
        }
    }
}
