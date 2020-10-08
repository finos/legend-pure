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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

public interface PropertyOwnerStrategy
{
    Function<PropertyOwner, PropertyOwnerStrategy> PROPERTY_OWNER_STRATEGY_FUNCTION = new Function<PropertyOwner, PropertyOwnerStrategy>()
    {
        @Override
        public PropertyOwnerStrategy valueOf(PropertyOwner propertyOwner)
        {
            if (propertyOwner instanceof Class)
            {
                return ClassPropertyOwnerStrategy.CLASS_PROPERTY_OWNER_STRATEGY;
            }
            else if (propertyOwner instanceof Association)
            {
                return AssociationPropertyOwnerStrategy.ASSOCIATION_PROPERTY_OWNER_STRATEGY;
            }
            else
            {
                return null;
            }
        }
    };

    Procedure<PropertyOwner> PROPERTIES_REMOVE = new Procedure<PropertyOwner>()
    {
        @Override
        public void value(PropertyOwner propertyOwner)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).propertiesRemove(propertyOwner);
        }
    };

    Procedure2<PropertyOwner, RichIterable<? extends Property<?, ?>>> PROPERTIES_SET = new Procedure2<PropertyOwner, RichIterable<? extends Property<?, ?>>>()
    {
        @Override
        public void value(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).setProperties(propertyOwner,values);
        }
    };

    Procedure<PropertyOwner> QUALIFIED_PROPERTIES_REMOVE = new Procedure<PropertyOwner>()
    {
        @Override
        public void value(PropertyOwner propertyOwner)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).qualifiedPropertiesRemove(propertyOwner);
        }
    };

    Procedure2<PropertyOwner, RichIterable<? extends QualifiedProperty<?>>> QUALIFIED_PROPERTIES_SET = new Procedure2<PropertyOwner, RichIterable<? extends QualifiedProperty<?>>>()
    {
        @Override
        public void value(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).setQualifiedProperties(propertyOwner, values);
        }
    };

    Procedure<PropertyOwner> ORIGINAL_MILESTONED_PROPERTIES_REMOVE = new Procedure<PropertyOwner>()
    {
        @Override
        public void value(PropertyOwner propertyOwner)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).originalMilestonedPropertiesRemove(propertyOwner);
        }
    };

    Procedure2<PropertyOwner, RichIterable<? extends Property<?, ?>>> ORIGINAL_MILESTONED_PROPERTIES_SET = new Procedure2<PropertyOwner, RichIterable<? extends Property<?, ?>>>()
    {
        @Override
        public void value(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).setOriginalMilestonedProperties(propertyOwner, values);
        }
    };

    Procedure<PropertyOwner> PROPERTIES_FROM_ASSOCIATION_REMOVE = new Procedure<PropertyOwner>()
    {
        @Override
        public void value(PropertyOwner propertyOwner)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).propertiesFromAssociationsRemove(propertyOwner);
        }
    };

    Procedure2<PropertyOwner, RichIterable<? extends Property<?, ?>>> PROPERTIES_FROM_ASSOCIATION_SET = new Procedure2<PropertyOwner, RichIterable<? extends Property<?, ?>>>()
    {
        @Override
        public void value(PropertyOwner propertyOwner, RichIterable<? extends Property<?, ?>> values)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).setPropertiesFromAssociations(propertyOwner, values);
        }
    };

    Procedure<PropertyOwner> QUALIFIED_PROPERTIES_FROM_ASSOCIATION_REMOVE = new Procedure<PropertyOwner>()
    {
        @Override
        public void value(PropertyOwner propertyOwner)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).qualifiedPropertiesFromAssociationsRemove(propertyOwner);
        }
    };

    Procedure2<PropertyOwner, RichIterable<? extends QualifiedProperty<?>>> QUALIFIED_PROPERTIES_FROM_ASSOCIATION_SET = new Procedure2<PropertyOwner, RichIterable<? extends QualifiedProperty<?>>>()
    {
        @Override
        public void value(PropertyOwner propertyOwner, RichIterable<? extends QualifiedProperty<?>> values)
        {
            PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(propertyOwner).setQualifiedPropertiesFromAssociations(propertyOwner, values);
        }
    };

    RichIterable<? extends Property<?, ?>> properties(PropertyOwner propertyOwner);

    RichIterable<? extends QualifiedProperty> qualifiedProperties(PropertyOwner propertyOwner);

    RichIterable<? extends QualifiedProperty> qualifiedPropertiesFromAssociations(PropertyOwner propertyOwner);

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
}
