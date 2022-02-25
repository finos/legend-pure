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

package org.finos.legend.pure.m3.navigation;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Instance
{
    /**
     * Return whether instance is an instance of the named type.
     *
     * @param instance         instance
     * @param typeName         type path
     * @param processorSupport processor support
     * @return whether instance is an instance of type
     */
    public static boolean instanceOf(CoreInstance instance, String typeName, ProcessorSupport processorSupport)
    {
        CoreInstance type = processorSupport.package_getByUserPath(typeName);
        if (type == null)
        {
            throw new RuntimeException(typeName + " is unknown!");
        }
        return instanceOf(instance, type, processorSupport);
    }

    /**
     * Return whether all instances are instances of the named type. Returns
     * true if the iterable of instances is empty.
     *
     * @param instances        instances
     * @param typeName         type path
     * @param processorSupport processor support
     * @return whether all instances are instances of type
     */
    public static boolean instancesOf(Iterable<? extends CoreInstance> instances, String typeName, ProcessorSupport processorSupport)
    {
        if (Iterate.isEmpty(instances))
        {
            return true;
        }

        CoreInstance type = processorSupport.package_getByUserPath(typeName);
        if (type == null)
        {
            throw new RuntimeException(typeName + " is unknown!");
        }
        return Iterate.allSatisfy(instances, i -> instanceOf(i, type, processorSupport));
    }

    /**
     * Return whether instance is an instance of the given type.
     *
     * @param instance instance
     * @param type     type
     * @return whether instance is an instance of type
     */
    public static boolean instanceOf(CoreInstance instance, CoreInstance type, ProcessorSupport processorSupport)
    {
        return processorSupport.type_subTypeOf(processorSupport.getClassifier(instance), type);
    }

    public static CoreInstance extractGenericTypeFromInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        CoreInstance classifierGenericType = getValueForMetaPropertyToOneResolved(instance, M3Properties.classifierGenericType, processorSupport);
        return classifierGenericType == null ? Type.wrapGenericType(processorSupport.getClassifier(instance), processorSupport) : classifierGenericType;
    }

    public static void addValueToProperty(CoreInstance owner, String keyName, CoreInstance value, ProcessorSupport processorSupport)
    {
        owner.addKeyValue(processorSupport.property_getPath(findProperty(owner, keyName, processorSupport)), value);
    }

    public static void addValueToProperty(CoreInstance owner, String keyName, Iterable<? extends CoreInstance> values, ProcessorSupport processorSupport)
    {
        ListIterable<String> path = processorSupport.property_getPath(findProperty(owner, keyName, processorSupport));
        values.forEach(v -> owner.addKeyValue(path, v));
    }

    public static void removeProperty(CoreInstance owner, String keyName, ProcessorSupport processorSupport)
    {
        owner.removeProperty(findProperty(owner, keyName, processorSupport));
    }

    public static void addPropertyWithEmptyList(CoreInstance owner, String keyName, ProcessorSupport processorSupport)
    {
        owner.addKeyWithEmptyList(processorSupport.property_getPath(findProperty(owner, keyName, processorSupport)));
    }

    public static void setValueAtForProperty(CoreInstance owner, String property, int i, CoreInstance value)
    {
        owner.modifyValueForToManyMetaProperty(property, i, value);
    }

    public static void setValueForProperty(CoreInstance owner, String property, CoreInstance value, ProcessorSupport processorSupport)
    {
        setValuesForProperty(owner, property, Lists.immutable.with(value), processorSupport);
    }

    public static void setValuesForProperty(CoreInstance owner, String property, ListIterable<? extends CoreInstance> values, ProcessorSupport processorSupport)
    {
        owner.setKeyValues(processorSupport.property_getPath(findProperty(owner, property, processorSupport)), values);
    }

    public static CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance owner, String property, ProcessorSupport processorSupport)
    {
        CoreInstance coreInstance = owner.getValueForMetaPropertyToOne(property);
        return ImportStub.withImportStubByPass(coreInstance, processorSupport);
    }

    public static CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance owner, String property1, String property2, ProcessorSupport processorSupport)
    {
        CoreInstance value = getValueForMetaPropertyToOneResolved(owner, property1, processorSupport);
        return (value == null) ? null : getValueForMetaPropertyToOneResolved(value, property2, processorSupport);
    }

    public static CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance owner, String property1, String property2, String property3, ProcessorSupport processorSupport)
    {
        CoreInstance value = getValueForMetaPropertyToOneResolved(owner, property1, processorSupport);
        if (value != null)
        {
            value = getValueForMetaPropertyToOneResolved(value, property2, processorSupport);
            if (value != null)
            {
                value = getValueForMetaPropertyToOneResolved(value, property3, processorSupport);
            }
        }
        return value;
    }

    public static CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance owner, String property1, String property2, String property3, String property4, ProcessorSupport processorSupport)
    {
        CoreInstance value = getValueForMetaPropertyToOneResolved(owner, property1, processorSupport);
        if (value != null)
        {
            value = getValueForMetaPropertyToOneResolved(value, property2, processorSupport);
            if (value != null)
            {
                value = getValueForMetaPropertyToOneResolved(value, property3, processorSupport);
                if (value != null)
                {
                    value = getValueForMetaPropertyToOneResolved(value, property4, processorSupport);
                }
            }
        }
        return value;
    }

    public static CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance owner, String property, ProcessorSupport processorSupport, String... moreProperties)
    {
        CoreInstance value = getValueForMetaPropertyToOneResolved(owner, property, processorSupport);
        for (int i = 0; (value != null) && (i < moreProperties.length); i++)
        {
            value = getValueForMetaPropertyToOneResolved(value, moreProperties[i], processorSupport);
        }
        return value;
    }

    public static CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance owner, CoreInstance property, ProcessorSupport processorSupport)
    {
        return ImportStub.withImportStubByPass(owner.getValueForMetaPropertyToOne(property), processorSupport);
    }

    public static Function<CoreInstance, CoreInstance> getValueForMetaPropertyToOneResolvedFunction(String property, ProcessorSupport processorSupport)
    {
        return i -> getValueForMetaPropertyToOneResolved(i, property, processorSupport);
    }

    public static ListIterable<? extends CoreInstance> getValueForMetaPropertyToManyResolved(CoreInstance owner, String propertyName, ProcessorSupport processorSupport)
    {
        return ImportStub.withImportStubByPasses(owner.getValueForMetaPropertyToMany(propertyName), processorSupport);
    }

    public static ListIterable<? extends CoreInstance> getValueForMetaPropertyToManyResolved(CoreInstance owner, CoreInstance property, ProcessorSupport processorSupport)
    {
        return ImportStub.withImportStubByPasses(owner.getValueForMetaPropertyToMany(property), processorSupport);
    }

    private static CoreInstance findProperty(CoreInstance owner, String propertyName, ProcessorSupport processorSupport)
    {
        CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(processorSupport.getClassifier(owner), propertyName);
        if (property == null)
        {
            throw new RuntimeException("Cannot find property '" + propertyName + "' on " + PackageableElement.getUserPathForPackageableElement(owner.getClassifier()));
        }
        return property;
    }
}
