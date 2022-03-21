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

package org.finos.legend.pure.m3.navigation._class;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class _Class
{
    public static final String KEY_STEREOTYPE = "Key";

    public static final ImmutableList<String> SIMPLE_PROPERTIES_PROPERTIES = Lists.immutable.with(M3Properties.properties, M3Properties.propertiesFromAssociations);
    public static final ImmutableList<String> QUALIFIED_PROPERTIES_PROPERTIES = Lists.immutable.with(M3Properties.qualifiedProperties, M3Properties.qualifiedPropertiesFromAssociations);
    public static final ImmutableList<String> ORIGINAL_MILESTONED_PROPERTIES = Lists.immutable.with(M3Properties.originalMilestonedProperties);
    public static final ImmutableList<String> ALL_PROPERTIES_PROPERTIES = SIMPLE_PROPERTIES_PROPERTIES.newWithAll(QUALIFIED_PROPERTIES_PROPERTIES).newWithAll(ORIGINAL_MILESTONED_PROPERTIES);


    /**
     * Get the qualified properties for the given class, including
     * those from associations and those inherited from superclasses.
     *
     * @param classifier class
     * @return qualified properties
     */
    public static RichIterable<CoreInstance> getQualifiedProperties(CoreInstance classifier, ProcessorSupport processorSupport)
    {
        return getQualifiedPropertiesByName(classifier, processorSupport).valuesView();
    }

    /**
     * Get the qualified properties for the given class, including
     * those from associations and those inherited from superclasses,
     * indexed by name.
     *
     * @param classifier       class
     * @param processorSupport processor support
     * @return qualified properties indexed by name
     */
    public static MapIterable<String, CoreInstance> getQualifiedPropertiesByName(CoreInstance classifier, ProcessorSupport processorSupport)
    {
        return computePropertiesByName(classifier, QUALIFIED_PROPERTIES_PROPERTIES, processorSupport);
    }

    public static ListIterable<QualifiedProperty<?>> findQualifiedPropertiesUsingGeneralization(CoreInstance classifier, String propertyName, ProcessorSupport processorSupport)
    {
        MutableList<QualifiedProperty<?>> result = Lists.mutable.empty();
        LazyIterate.flatCollect(Type.getGeneralizationResolutionOrder(classifier, processorSupport),
                t -> LazyIterate.flatCollect(QUALIFIED_PROPERTIES_PROPERTIES, p -> Instance.getValueForMetaPropertyToManyResolved(t, p, processorSupport)))
                .select(qp -> propertyName.equals(qp.getValueForMetaPropertyToOne(M3Properties.name).getName()))
                .forEach(qp -> result.add((QualifiedProperty<?>) qp));
        return result;
    }

    public static QualifiedProperty<?> findQualifiedPropertyWithNoExplicitArgsUsingGeneralization(CoreInstance classifier, String propertyName, ProcessorSupport processorSupport)
    {
        return (QualifiedProperty<?>) LazyIterate.flatCollect(Type.getGeneralizationResolutionOrder(classifier, processorSupport),
                t -> LazyIterate.flatCollect(QUALIFIED_PROPERTIES_PROPERTIES, p -> Instance.getValueForMetaPropertyToManyResolved(t, p, processorSupport)))
                .detect(qp -> propertyName.equals(qp.getValueForMetaPropertyToOne(M3Properties.name).getName()) &&
                        processorSupport.function_getFunctionType(qp).getValueForMetaPropertyToMany(M3Properties.parameters).size() == 1);
    }

    public static ListIterable<CoreInstance> getEqualityKeyProperties(CoreInstance classifier, ProcessorSupport processorSupport)
    {
        CoreInstance profile = processorSupport.package_getByUserPath(M3Paths.equality);
        CoreInstance stereotype = Profile.findStereotype(profile, KEY_STEREOTYPE);
        return processorSupport.class_getSimpleProperties(classifier).select(p -> hasStereotype(p, stereotype, processorSupport), Lists.mutable.empty());
    }

    private static boolean hasStereotype(CoreInstance property, CoreInstance stereotype, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToManyResolved(property, M3Properties.stereotypes, processorSupport).anySatisfy(st -> st == stereotype);
    }

    public static String print(CoreInstance cls)
    {
        return print(cls, false);
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance cls)
    {
        return print(appendable, cls, false);
    }

    public static String print(CoreInstance cls, boolean fullPaths)
    {
        return print(new StringBuilder(), cls, fullPaths).toString();
    }

    public static <T extends Appendable> T print(T appendable, CoreInstance cls, boolean fullPaths)
    {
        ListIterable<? extends CoreInstance> typeParameters = cls.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        ListIterable<? extends CoreInstance> multiplicityParameters = cls.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
        boolean hasTypeParams = typeParameters.notEmpty();
        boolean hasMultParams = multiplicityParameters.notEmpty();

        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        if (fullPaths)
        {
            PackageableElement.writeUserPathForPackageableElement(safeAppendable, cls);
        }
        else
        {
            safeAppendable.append(cls.getValueForMetaPropertyToOne(M3Properties.name).getName());
        }
        if (hasTypeParams || hasMultParams)
        {
            safeAppendable.append('<');
            if (hasTypeParams)
            {
                boolean first = true;
                for (CoreInstance parameter : typeParameters)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        safeAppendable.append(',');
                    }
                    safeAppendable.append(parameter.getValueForMetaPropertyToOne(M3Properties.name).getName());
                }
            }
            if (hasMultParams)
            {
                safeAppendable.append('|');
                boolean first = true;
                for (CoreInstance parameter : multiplicityParameters)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        safeAppendable.append(',');
                    }
                    safeAppendable.append(parameter.getValueForMetaPropertyToOne(M3Properties.values).getName());
                }
            }
            safeAppendable.append('>');
        }
        return appendable;
    }

    /**
     * Compute an index of properties by name, using the generalization
     * resolution order.  What counts as a property is determined by the
     * value of propertyProperties.
     *
     * @param classifier         class
     * @param propertyProperties properties that hold the relevant properties
     * @param processorSupport   processor support
     * @return properties by name
     */
    public static MutableMap<String, CoreInstance> computePropertiesByName(CoreInstance classifier, Iterable<String> propertyProperties, ProcessorSupport processorSupport)
    {
        MutableMap<String, CoreInstance> properties = Maps.mutable.with();
        Type.getGeneralizationResolutionOrder(classifier, processorSupport).forEach(type ->
                propertyProperties.forEach(propertyProperty ->
                        Instance.getValueForMetaPropertyToManyResolved(type, propertyProperty, processorSupport).forEach(property ->
                                properties.getIfAbsentPut(Property.getPropertyId(property, processorSupport), property))));
        return properties;
    }


    /**
     * Compute all constraints using reversed generalization resolution order.
     *
     * @param _class           class
     * @param processorSupport processor support
     * @return properties by name
     */
    public static ListIterable<CoreInstance> computeConstraintsInHierarchy(CoreInstance _class, ProcessorSupport processorSupport)
    {
        MutableList<CoreInstance> allConstraints = Lists.mutable.empty();
        Type.getGeneralizationResolutionOrder(_class, processorSupport).asReversed().forEach(genl -> allConstraints.addAllIterable(genl.getValueForMetaPropertyToMany(M3Properties.constraints)));
        return allConstraints;
    }
}
