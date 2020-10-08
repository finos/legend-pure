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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;

public class _Class
{
    public static final String KEY_STEREOTYPE = "Key";

    private static final Function<CoreInstance, String> GET_NAME_FOR_PRINT = new Function<CoreInstance, String>()
    {
        @Override
        public String valueOf(CoreInstance instance)
        {
            return instance.getValueForMetaPropertyToOne(M3Properties.name).getName();
        }
    };

    private static final Predicate2<CoreInstance, String> PROPERTY_HAS_NAME = new Predicate2<CoreInstance, String>()
    {
        @Override
        public boolean accept(CoreInstance property, String name)
        {
            return property.getValueForMetaPropertyToOne(M3Properties.name).getName().equals(name);
        }
    };

    public static final ImmutableList<String> SIMPLE_PROPERTIES_PROPERTIES = Lists.immutable.with(M3Properties.properties, M3Properties.propertiesFromAssociations);
    public static final ImmutableList<String> QUALIFIED_PROPERTIES_PROPERTIES = Lists.immutable.with(M3Properties.qualifiedProperties, M3Properties.qualifiedPropertiesFromAssociations);
    public static final ImmutableList<String> ORIGINAL_MILESTONED_PROPERTIES = Lists.immutable.with(M3Properties.originalMilestonedProperties);
    public static final ImmutableList<String> ALL_PROPERTIES_PROPERTIES = SIMPLE_PROPERTIES_PROPERTIES.newWithAll(QUALIFIED_PROPERTIES_PROPERTIES.newWithAll(ORIGINAL_MILESTONED_PROPERTIES));


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

    public static ListIterable<CoreInstance> findQualifiedPropertiesUsingGeneralization(CoreInstance classifier, String propertyName, ProcessorSupport processorSupport)
    {
        MutableList<CoreInstance> result = Lists.mutable.empty();
        for (CoreInstance type : Type.getGeneralizationResolutionOrder(classifier, processorSupport))
        {
            for (String propertyProperty : QUALIFIED_PROPERTIES_PROPERTIES)
            {
                ListIterable<CoreInstance> properties = (ListIterable<CoreInstance>) Instance.getValueForMetaPropertyToManyResolved(type, propertyProperty, processorSupport);
                properties.selectWith(PROPERTY_HAS_NAME, propertyName, result);
            }
        }
        return result;
    }

    public static CoreInstance findQualifiedPropertyWithNoExplicitArgsUsingGeneralization(CoreInstance classifier, String propertyName, ProcessorSupport processorSupport)
    {
        for (CoreInstance type : Type.getGeneralizationResolutionOrder(classifier, processorSupport))
        {
            for (String propertyProperty : QUALIFIED_PROPERTIES_PROPERTIES)
            {
                ListIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(type, propertyProperty, processorSupport);
                for (CoreInstance property : properties)
                {
                    if (property.getValueForMetaPropertyToOne(M3Properties.name).getName().equals(propertyName))
                    {
                        CoreInstance funcType = processorSupport.function_getFunctionType(property);
                        if (funcType.getValueForMetaPropertyToMany(M3Properties.parameters).size() == 1)
                        {
                            return property;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static ListIterable<CoreInstance> getEqualityKeyProperties(CoreInstance classifier, ProcessorSupport processorSupport)
    {
        CoreInstance profile = processorSupport.package_getByUserPath(M3Paths.equality);
        CoreInstance stereotype = Profile.findStereotype(profile, KEY_STEREOTYPE);

        MutableList<CoreInstance> keys = Lists.mutable.with();
        for (CoreInstance property : processorSupport.class_getSimpleProperties(classifier))
        {
            if (hasStereotype(property, stereotype, processorSupport))
            {
                keys.add(property);
            }
        }
        return keys;
    }

    private static boolean hasStereotype(CoreInstance property, CoreInstance stereotype, ProcessorSupport processorSupport)
    {
        for (CoreInstance st : Instance.getValueForMetaPropertyToManyResolved(property, M3Properties.stereotypes, processorSupport))
        {
            if (st == stereotype)
            {
                return true;
            }
        }
        return false;
    }

    public static String print(CoreInstance cls)
    {
        return print(cls, false);
    }

    public static void print(Appendable appendable, CoreInstance cls)
    {
        print(appendable, cls, false);
    }

    public static String print(CoreInstance cls, boolean fullPaths)
    {
        StringBuilder builder = new StringBuilder();
        print(builder, cls, fullPaths);
        return builder.toString();
    }

    public static void print(Appendable appendable, CoreInstance cls, boolean fullPaths)
    {
        ListIterable<? extends CoreInstance> typeParameters = cls.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        ListIterable<? extends CoreInstance> multiplicityParameters = cls.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
        boolean hasTypeParams = typeParameters.notEmpty();
        boolean hasMultParams = multiplicityParameters.notEmpty();

        try
        {
            if (fullPaths)
            {
                PackageableElement.writeUserPathForPackageableElement(appendable, cls);
            }
            else
            {
                appendable.append(cls.getValueForMetaPropertyToOne(M3Properties.name).getName());
            }
            if (hasTypeParams || hasMultParams)
            {
                appendable.append('<');
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
                            appendable.append(',');
                        }
                        appendable.append(parameter.getValueForMetaPropertyToOne(M3Properties.name).getName());
                    }
                }
                if (hasMultParams)
                {
                    appendable.append('|');
                    boolean first = true;
                    for (CoreInstance parameter : multiplicityParameters)
                    {
                        if (first)
                        {
                            first = false;
                        }
                        else
                        {
                            appendable.append(',');
                        }
                        appendable.append(parameter.getValueForMetaPropertyToOne(M3Properties.values).getName());
                    }
                }
                appendable.append('>');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
        for (CoreInstance type : Type.getGeneralizationResolutionOrder(classifier, processorSupport))
        {
            for (String propertyProperty : propertyProperties)
            {
                for (CoreInstance property : Instance.getValueForMetaPropertyToManyResolved(type, propertyProperty, processorSupport))
                {
                    String name = Property.getPropertyId(property, processorSupport);
                    if (!properties.containsKey(name))
                    {
                        properties.put(name, property);
                    }
                }
            }
        }
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
        for (CoreInstance genl : Type.getGeneralizationResolutionOrder(_class, processorSupport).asReversed())
        {
            allConstraints.addAllIterable(genl.getValueForMetaPropertyToMany(M3Properties.constraints));
        }
        return allConstraints;
    }
}
