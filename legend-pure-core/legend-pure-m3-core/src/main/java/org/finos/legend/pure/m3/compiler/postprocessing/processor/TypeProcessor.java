// Copyright 2025 Goldman Sachs
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
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class TypeProcessor extends Processor<Type>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Type;
    }

    @Override
    public void process(Type type, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        type._generalizations().forEach(generalization -> PostProcessor.processElement(matcher, generalization._general(), state, processorSupport));
    }

    @Override
    public void populateReferenceUsages(Type type, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // We don't process reference usages for generalizations yet. We wait for the concrete subclasses to do their processing.
    }

    public static void processGeneralizationReferenceUsages(Type type, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends Generalization> generalizations = type._generalizations();
        if (generalizations.notEmpty())
        {
            CoreInstance topType = processorSupport.type_TopType();
            generalizations.forEach(generalization ->
            {
                GenericType general = generalization._general();
                if (general != null)
                {
                    CoreInstance generalType = ImportStub.withImportStubByPass(generalization._general()._rawTypeCoreInstance(), processorSupport);
                    if (generalType != topType)
                    {
                        GenericTypeTraceability.addTraceForGeneralization(generalization, repository, processorSupport);
                    }
                }
            });
        }
    }
}
