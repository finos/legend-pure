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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

public interface PropertyOwnerStrategy
{
    RichIterable<? extends Property<?, ?>> properties(PropertyOwner propertyOwner);

    RichIterable<? extends QualifiedProperty<?>> qualifiedProperties(PropertyOwner propertyOwner);

    RichIterable<? extends QualifiedProperty<?>> qualifiedPropertiesFromAssociations(PropertyOwner propertyOwner);

    RichIterable<? extends Property<?, ?>> originalMilestonedProperties(PropertyOwner propertyOwner);

    PropertyOwner originalMilestonedPropertiesRemove(PropertyOwner propertyOwner);

    PropertyOwner originalMilestonedPropertiesRemove(PropertyOwner propertyOwner, Property<?, ?> property);

    PropertyOwner propertiesRemove(PropertyOwner propertyOwner);

    PropertyOwner propertiesRemove(PropertyOwner propertyOwner, Property<?, ?> property);

    PropertyOwner qualifiedPropertiesRemove(PropertyOwner propertyOwner);

    PropertyOwner propertiesFromAssociationsRemove(PropertyOwner propertyOwner);

    PropertyOwner qualifiedPropertiesFromAssociationsRemove(PropertyOwner propertyOwner);

    PropertyOwner setOriginalMilestonedProperties(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values);

    PropertyOwner setProperties(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values);

    PropertyOwner setQualifiedProperties(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values);

    PropertyOwner setPropertiesFromAssociations(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values);

    PropertyOwner setQualifiedPropertiesFromAssociations(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values);

    static PropertyOwnerStrategy getPropertyOwnerStrategy(PropertyOwner propertyOwner)
    {
        if (propertyOwner instanceof Class)
        {
            return ClassPropertyOwnerStrategy.CLASS_PROPERTY_OWNER_STRATEGY;
        }
        if (propertyOwner instanceof Association)
        {
            return AssociationPropertyOwnerStrategy.ASSOCIATION_PROPERTY_OWNER_STRATEGY;
        }
        throw new IllegalArgumentException("Unsupported property owner type: " + propertyOwner.getClass().getName());
    }
}
