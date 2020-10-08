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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.validator.MilestoningClassValidator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;

public class MilestoningClassProcessor
{
    private MilestoningClassProcessor()
    {
    }

    public static void process(Class cls, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        MutableList<AbstractProperty<?>> properties = cls._properties().toList();
        ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesExcludingParents(cls, processorSupport);
        if (temporalStereotypes.notEmpty())
        {
            MilestoningClassValidator.runValidations(cls, properties, processorSupport);
        }
    }

    public static void addMilestoningProperty(Class cls, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        MutableList<AbstractProperty<?>> properties = cls._properties().toList();
        ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesExcludingParents(cls, processorSupport);

        if (temporalStereotypes.notEmpty())
        {
            MilestoningStereotype stereotype = temporalStereotypes.getFirst();
            ListIterable<AbstractProperty<?>> synthesizedMilestonedProperties = PropertyInstanceBuilder.createMilestonedProperties(cls, stereotype.getDatePropertyCodeBlocks(cls.getSourceInformation()), context, processorSupport, modelRepository);
            properties.addAllIterable(synthesizedMilestonedProperties);
            cls._properties(properties);
            context.update(cls);
        }
    }
}
