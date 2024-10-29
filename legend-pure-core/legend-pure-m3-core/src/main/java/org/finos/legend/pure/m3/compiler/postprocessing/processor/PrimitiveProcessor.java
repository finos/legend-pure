// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;

public class PrimitiveProcessor extends Processor<PrimitiveType>
{
    @Override
    public String getClassName()
    {
        return M3Paths.PrimitiveType;
    }

    @Override
    public void process(PrimitiveType cls, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
    }

    @Override
    public void populateReferenceUsages(PrimitiveType instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // To ensure the InstanceValues of the general type are managed properly
        RichIterable<? extends Generalization> generalizations = instance._generalizations();
        if (generalizations.notEmpty())
        {
            Type topType = (Type)processorSupport.type_TopType();
            for (Generalization generalization : generalizations)
            {
                Type generalType = generalization._general() == null ? null : (Type) ImportStub.withImportStubByPass(generalization._general()._rawTypeCoreInstance(), processorSupport);
                if (generalType != topType)
                {
                    GenericTypeTraceability.addTraceForGeneralization(generalization, repository, processorSupport);
                }
            }
        }
    }
}
