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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MilestoningUnbind
{
    static void undoMoveProcessedOriginalMilestonedProperties(PropertyOwner propertyOwner, Context context)
    {
        PropertyOwnerStrategy propertyOwnerStrategy = PropertyOwnerStrategy.getPropertyOwnerStrategy(propertyOwner);
        RichIterable<? extends Property<?, ?>> nonMilestonedProperties = propertyOwnerStrategy.originalMilestonedProperties(propertyOwner);
        if (nonMilestonedProperties.notEmpty())
        {
            nonMilestonedProperties.forEach(propertyToMove ->
            {
                propertyOwnerStrategy.originalMilestonedPropertiesRemove(propertyOwner, propertyToMove);
                propertyOwner.addKeyValue(M3PropertyPaths.properties, propertyToMove);
            });
            MilestoningFunctions.updateAndInvalidate(propertyOwner, context);
            propertyOwnerStrategy.originalMilestonedPropertiesRemove(propertyOwner);
        }
    }

    static <O extends PropertyOwner, T extends AbstractProperty<?>> void removeGeneratedMilestoningProperties(O owner, ProcessorSupport processorSupport, RichIterable<? extends T> properties, Consumer<? super O> removeProperty, BiConsumer<? super O, RichIterable<? extends T>> setPropertyValue)
    {
        if (properties.notEmpty())
        {
            PartitionIterable<? extends T> partition = properties.partition(p -> MilestoningFunctions.isGeneratedMilestoningProperty(p, processorSupport));
            if (partition.getSelected().notEmpty())
            {
                RichIterable<? extends T> nonGeneratedMilestoningProperties = partition.getRejected();
                if (nonGeneratedMilestoningProperties.isEmpty())
                {
                    removeProperty.accept(owner);
                }
                else
                {
                    setPropertyValue.accept(owner, nonGeneratedMilestoningProperties);
                }
            }
        }
    }
}
