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

    @Deprecated
    public static void process(Class<?> cls, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        process(cls, processorSupport);
    }

    public static void process(Class<?> cls, ProcessorSupport processorSupport)
    {
        ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesExcludingParents(cls);
        if (temporalStereotypes.notEmpty())
        {
            MilestoningClassValidator.runValidations(cls, cls._properties(), processorSupport);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addMilestoningProperty(Class<?> cls, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        ListIterable<MilestoningStereotypeEnum> temporalStereotypes = MilestoningFunctions.getTemporalStereoTypesExcludingParents(cls);
        if (temporalStereotypes.notEmpty())
        {
            MilestoningStereotype stereotype = temporalStereotypes.getFirst();
            ListIterable<AbstractProperty<?>> synthesizedMilestonedProperties = PropertyInstanceBuilder.createMilestonedProperties(cls, stereotype.getDatePropertyCodeBlocks(cls.getSourceInformation()), context, processorSupport, modelRepository);
            ((Class) cls)._properties(Lists.mutable.<AbstractProperty<?>>withAll(cls._properties()).withAll(synthesizedMilestonedProperties));
            context.update(cls);
        }
    }
}
