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

package org.finos.legend.pure.m3.compiler;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

public class ClassPropertyOwnerStrategy implements PropertyOwnerStrategy
{
    public static final ClassPropertyOwnerStrategy CLASS_PROPERTY_OWNER_STRATEGY = new ClassPropertyOwnerStrategy();

    @Override
    public RichIterable<? extends Property<?, ?>> properties(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._properties();
    }

    @Override
    public RichIterable<? extends QualifiedProperty<?>> qualifiedProperties(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._qualifiedProperties();
    }

    @Override
    public RichIterable<? extends QualifiedProperty<?>> qualifiedPropertiesFromAssociations(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._qualifiedPropertiesFromAssociations();
    }

    @Override
    public RichIterable<? extends Property<?, ?>> originalMilestonedProperties(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._originalMilestonedProperties();
    }

    @Override
    public Class<?> originalMilestonedPropertiesRemove(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._originalMilestonedPropertiesRemove();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<?> originalMilestonedPropertiesRemove(PropertyOwner propertyOwner, Property<?, ?> property)
    {
        return ((Class<?>) propertyOwner)._originalMilestonedPropertiesRemove((Property) property);
    }

    @Override
    public Class<?> propertiesRemove(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._propertiesRemove();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<?> propertiesRemove(PropertyOwner propertyOwner, Property<?, ?> property)
    {
        return ((Class<?>) propertyOwner)._propertiesRemove((Property) property);
    }

    @Override
    public Class<?> qualifiedPropertiesRemove(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._qualifiedPropertiesRemove();
    }

    @Override
    public Class<?> propertiesFromAssociationsRemove(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._propertiesFromAssociationsRemove();
    }

    @Override
    public Class<?> qualifiedPropertiesFromAssociationsRemove(PropertyOwner propertyOwner)
    {
        return ((Class<?>) propertyOwner)._qualifiedPropertiesFromAssociationsRemove();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<?> setOriginalMilestonedProperties(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
    {
        return ((Class) propertyOwner)._originalMilestonedProperties(values);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<?> setProperties(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
    {
        return ((Class) propertyOwner)._properties(values);
    }

    @Override
    public Class<?> setQualifiedProperties(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values)
    {
        return ((Class<?>) propertyOwner)._qualifiedProperties(values);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<?> setPropertiesFromAssociations(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
    {
        return ((Class) propertyOwner)._propertiesFromAssociations(values);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<?> setQualifiedPropertiesFromAssociations(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values)
    {
        return ((Class) propertyOwner)._qualifiedPropertiesFromAssociations(values);
    }
}
