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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.linearization.C3Linearization;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.ArrayDeque;
import java.util.Deque;

public class Type
{
    public static CoreInstance wrapGenericType(CoreInstance type, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        return wrapGenericType(type, sourceInformation, processorSupport, false);
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

    public static boolean isExtendedPrimitiveType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return isPrimitiveType(type, processorSupport) && type.getValueForMetaPropertyToOne(M3Properties.extended) != null && PrimitiveUtilities.getBooleanValue(type.getValueForMetaPropertyToOne(M3Properties.extended));
    }

    public static boolean containsExtendedPrimitiveType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return isExtendedPrimitiveType(type, processorSupport)
                || (FunctionType.isFunctionType(type, processorSupport) && FunctionType.containsExtendedPrimitiveType(type, processorSupport))
                || (_RelationType.isRelationType(type, processorSupport) && _RelationType.containsExtendedPrimitiveType(type, processorSupport));
    }

    public static CoreInstance findPrimitiveTypeFromExtendedPrimitiveType(CoreInstance type, ProcessorSupport processorSupport)
    {
        ListIterable<CoreInstance> order = Type.getGeneralizationResolutionOrder(type, processorSupport);
        return order.detect(x -> !Type.isExtendedPrimitiveType(x, processorSupport));
    }

    public static boolean isTopPrimitiveType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return isPrimitiveType(type, processorSupport) && (type.getValueForMetaPropertyToOne(M3Properties.extended) == null || !PrimitiveUtilities.getBooleanValue(type.getValueForMetaPropertyToOne(M3Properties.extended)));
    }

    /**
     * Return whether type is a primitive type.  That is, whether
     * type is an instance of the class PrimitiveType.
     *
     * @param type Pure type
     * @param processorSupport processor support
     * @return whether type is an instance of PrimitiveType
     */
    public static boolean isPrimitiveType(CoreInstance type, ProcessorSupport processorSupport)
    {
        if (type == null)
        {
            return false;
        }
        if (type instanceof PrimitiveType)
        {
            return true;
        }
        return (!(type instanceof Any) || (type instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(type, M3Paths.PrimitiveType);
    }

    /**
     * Return whether type is the bottom type (i.e., Nil).
     *
     * @param type Pure type
     * @param processorSupport processor support
     * @return whether type is the bottom type
     */
    public static boolean isBottomType(CoreInstance type, ProcessorSupport processorSupport)
    {
        return (type != null) && (type == processorSupport.type_BottomType());
    }

    /**
     * Return whether type is the top type (i.e., Any).
     *
     * @param type Pure type
     * @param processorSupport processor support
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

    public static MutableSet<CoreInstance> getTopMostNonTopTypeGeneralizations(CoreInstance type, ProcessorSupport processorSupport)
    {
        CoreInstance topType = processorSupport.type_TopType();
        return getGeneralizationResolutionOrder(type, processorSupport).select(genl -> directSubTypeOf(genl, topType, processorSupport), Sets.mutable.empty());
    }

    /**
     * Get the direct generalizations of type.  These are returned in the
     * order that they are defined on type.
     *
     * @param type             type
     * @param processorSupport processor support
     * @return direct generalizations of type
     */
    public static ListIterable<CoreInstance> getDirectGeneralizations(CoreInstance type, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> genls = type.getValueForMetaPropertyToMany(M3Properties.generalizations);
        return genls.collect(genl -> Instance.getValueForMetaPropertyToOneResolved(genl, M3Properties.general, M3Properties.rawType, processorSupport), Lists.mutable.withInitialCapacity(genls.size()));
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
     * @param type                    type
     * @param possibleDirectSuperType possible direct super type
     * @param processorSupport        processor support
     * @return whether type is a direct subtype of possibleDirectSuperType
     */
    public static boolean directSubTypeOf(CoreInstance type, CoreInstance possibleDirectSuperType, ProcessorSupport processorSupport)
    {
        return type.getValueForMetaPropertyToMany(M3Properties.generalizations)
                .anySatisfy(rawGenl -> possibleDirectSuperType == Instance.getValueForMetaPropertyToOneResolved(rawGenl, M3Properties.general, M3Properties.rawType, processorSupport));
    }

    public static SetIterable<CoreInstance> getAllSubTypes(CoreInstance type)
    {
        MutableSet<CoreInstance> result = Sets.mutable.empty();
        Deque<CoreInstance> deque = type.getValueForMetaPropertyToMany(M3Properties.specializations).collect(spec -> spec.getValueForMetaPropertyToOne(M3Properties.specific), new ArrayDeque<>());
        while (!deque.isEmpty())
        {
            CoreInstance subtype = deque.removeLast();
            if (result.add(subtype))
            {
                subtype.getValueForMetaPropertyToMany(M3Properties.specializations).collect(spec -> spec.getValueForMetaPropertyToOne(M3Properties.specific), deque);
            }
        }
        return result;
    }
}
