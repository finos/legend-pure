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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;

public class GeneratedMilestonedProperties
{
    private final Property<?, ?> sourceMilestonedProperty;
    private final MutableList<AbstractProperty<?>> qualifiedProperties = Lists.mutable.empty();
    private AbstractProperty<?> edgePointProperty;

    GeneratedMilestonedProperties(Property<?, ?> sourceMilestonedProperty)
    {
        this.sourceMilestonedProperty = sourceMilestonedProperty;
    }

    void addQualifiedProperties(Iterable<? extends AbstractProperty<?>> qualifiedProperties)
    {
        this.qualifiedProperties.addAllIterable(qualifiedProperties);
    }

    void setEdgePointProperty(AbstractProperty<?> edgePointProperty)
    {
        this.edgePointProperty = edgePointProperty;
    }

    public Property<?, ?> getSourceMilestonedProperty()
    {
        return this.sourceMilestonedProperty;
    }

    public AbstractProperty<?> getEdgePointProperty()
    {
        return this.edgePointProperty;
    }

    public ListIterable<AbstractProperty<?>> getQualifiedProperties()
    {
        return this.qualifiedProperties.asUnmodifiable();
    }

    public boolean hasGeneratedProperties()
    {
        return this.edgePointProperty != null || this.qualifiedProperties.notEmpty();
    }

    public ListIterable<AbstractProperty<?>> getAllGeneratedProperties()
    {
        MutableList<AbstractProperty<?>> allGeneratedProperties = Lists.mutable.ofInitialCapacity(this.qualifiedProperties.size() + 1);
        if (this.edgePointProperty != null)
        {
            allGeneratedProperties.add(this.edgePointProperty);
        }
        allGeneratedProperties.addAll(this.qualifiedProperties);
        return allGeneratedProperties;
    }
}
