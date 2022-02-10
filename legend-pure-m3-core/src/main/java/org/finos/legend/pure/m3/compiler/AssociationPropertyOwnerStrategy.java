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
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;

public class AssociationPropertyOwnerStrategy implements PropertyOwnerStrategy
{
    public static final AssociationPropertyOwnerStrategy ASSOCIATION_PROPERTY_OWNER_STRATEGY = new AssociationPropertyOwnerStrategy();

    @Override
    public RichIterable<? extends Property<?, ?>> properties(PropertyOwner propertyOwner)
    {
        return ((Association) propertyOwner)._properties();
    }

    @Override
    public RichIterable<? extends QualifiedProperty<?>> qualifiedProperties(PropertyOwner propertyOwner)
    {
        return ((Association) propertyOwner)._qualifiedProperties();
    }

    @Override
    public RichIterable<? extends QualifiedProperty<?>> qualifiedPropertiesFromAssociations(PropertyOwner propertyOwner)
    {
        return Lists.immutable.empty();
    }

    @Override
    public RichIterable<? extends Property<?, ?>> originalMilestonedProperties(PropertyOwner propertyOwner)
    {
        return ((Association) propertyOwner)._originalMilestonedProperties();
    }

    @Override
    public Association originalMilestonedPropertiesRemove(PropertyOwner propertyOwner)
    {
        return ((Association) propertyOwner)._originalMilestonedPropertiesRemove();
    }

    @Override
    public Association originalMilestonedPropertiesRemove(PropertyOwner propertyOwner, Property<?, ?> property)
    {
        return ((Association) propertyOwner)._originalMilestonedPropertiesRemove(property);
    }

    @Override
    public Association propertiesRemove(PropertyOwner propertyOwner)
    {
        return ((Association) propertyOwner)._propertiesRemove();
    }

    @Override
    public Association propertiesRemove(PropertyOwner propertyOwner, Property<?, ?> property)
    {
        return ((Association) propertyOwner)._propertiesRemove(property);
    }

    @Override
    public Association qualifiedPropertiesRemove(PropertyOwner propertyOwner)
    {
        return ((Association) propertyOwner)._qualifiedPropertiesRemove();
    }

    @Override
    public Association propertiesFromAssociationsRemove(PropertyOwner propertyOwner)
    {
        throw new UnsupportedOperationException("Association does not support propertiesFromAssociations remove.");
    }

    @Override
    public Association qualifiedPropertiesFromAssociationsRemove(PropertyOwner propertyOwner)
    {
        throw new UnsupportedOperationException("Association does not support qualifiedPropertiesFromAssociations remove.");
    }

    @Override
    public Association setOriginalMilestonedProperties(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
    {
        return ((Association) propertyOwner)._originalMilestonedProperties(values);
    }

    @Override
    public Association setProperties(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
    {
        return ((Association) propertyOwner)._properties(values);
    }

    @Override
    public Association setQualifiedProperties(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values)
    {
        return ((Association) propertyOwner)._qualifiedProperties(values);
    }

    @Override
    public Association setPropertiesFromAssociations(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
    {
        throw new UnsupportedOperationException("Association does not support propertiesFromAssociations set.");
    }

    @Override
    public Association setQualifiedPropertiesFromAssociations(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values)
    {
        throw new UnsupportedOperationException("Association does not support qualifiedPropertiesFromAssociations set.");
    }
}
