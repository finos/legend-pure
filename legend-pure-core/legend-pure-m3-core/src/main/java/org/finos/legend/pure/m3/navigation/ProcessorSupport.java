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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public interface ProcessorSupport
{
    boolean instance_instanceOf(CoreInstance obj, String type);

    boolean type_isPrimitiveType(CoreInstance valueForMetaPropertyToOneResolved);

    boolean valueSpecification_instanceOf(CoreInstance valueSpecification, String type);

    CoreInstance type_wrapGenericType(CoreInstance aClass);

    CoreInstance function_getFunctionType(CoreInstance function);

    CoreInstance package_getByUserPath(String path);

    CoreInstance repository_getTopLevel(String root);

    CoreInstance newGenericType(SourceInformation sourceInformation, CoreInstance source, boolean inferred);

    CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, String classifier);

    CoreInstance newEphemeralAnonymousCoreInstance(String type);

    CoreInstance newCoreInstance(String name, String typeName, SourceInformation sourceInformation);

    SetIterable<CoreInstance> function_getFunctionsForName(String functionName);

    CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation);

    ImmutableList<CoreInstance> type_getTypeGeneralizations(CoreInstance type, Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator);

    CoreInstance class_findPropertyUsingGeneralization(CoreInstance classifier, String propertyName);

    CoreInstance class_findPropertyOrQualifiedPropertyUsingGeneralization(CoreInstance classifier, String propertyName);

    /**
         * Get the simple (i.e., non-qualified) properties for the given
         * class, including those from associations and those inherited
         * from superclasses, indexed by name.
         *
         * @param classifier       class
         * @return simple properties indexed by name
         */
    MapIterable<String, CoreInstance> class_getSimplePropertiesByName(CoreInstance classifier);

    /**
     * Get the simple (i.e., non-qualified) properties for the given
     * class, including those from associations and those inherited
     * from superclasses.
     *
     * @param classifier class
     */
    RichIterable<CoreInstance> class_getSimpleProperties(CoreInstance classifier);

    /**
     * Get the qualified properties for the given class, including
     * those from associations and those inherited from superclasses,
     * indexed by name.
     * @param classifier class
     * @return qualified properties indexed by name
     */
    MapIterable<String, CoreInstance> class_getQualifiedPropertiesByName(CoreInstance classifier);

    /**
     * Get the qualified properties for the given class, including
     * those from associations and those inherited from superclasses
     * @param classifier class
     * @return qualified properties
     */
    RichIterable<CoreInstance> class_getQualifiedProperties(CoreInstance classifier);

    ListIterable<String> property_getPath(CoreInstance property);

    CoreInstance getClassifier(CoreInstance instance);

    boolean type_subTypeOf(CoreInstance type, CoreInstance possibleSuperType);

    /**
     * Return the bottom type (i.e., Nil).
     *
     * @return Nil
     */
    CoreInstance type_BottomType();

    /**
     * Return the top type (i.e., Any).
     *
     * @return Any
     */
    CoreInstance type_TopType();

    default void instance_addValueToProperty(CoreInstance owner, ListIterable<String> path, Iterable<? extends CoreInstance> values)
    {
        values.forEach(v -> owner.addKeyValue(path, v));
    }

    default void instance_setValuesForProperty(CoreInstance owner, CoreInstance property, ListIterable<? extends CoreInstance> values)
    {
        owner.setKeyValues(this.property_getPath(property), values);
    }

    default CoreInstance instance_getValueForMetaPropertyToOneResolved(CoreInstance owner, String property)
    {
        return owner.getValueForMetaPropertyToOne(property);
    }

    default ListIterable<? extends CoreInstance> instance_getValueForMetaPropertyToMany(CoreInstance owner, String propertyName)
    {
        return owner.getValueForMetaPropertyToMany(propertyName);
    }

    default ListIterable<? extends CoreInstance> instance_getValueForMetaPropertyToMany(CoreInstance owner, CoreInstance property)
    {
        return owner.getValueForMetaPropertyToMany(property);
    }
}
