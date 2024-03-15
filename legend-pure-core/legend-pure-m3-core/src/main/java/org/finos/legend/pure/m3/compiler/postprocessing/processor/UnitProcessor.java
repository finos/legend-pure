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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState.VariableContextScope;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;

/**
* Post-processor for any Unit.
*/
public class UnitProcessor extends Processor<Unit>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Unit;
    }

    @Override
    public void process(Unit instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        FunctionDefinition<?> conversionFunction = instance._conversionFunction();
        if (conversionFunction != null)
        {
            try (VariableContextScope ignore = state.withNewVariableContext())
            {
                FunctionDefinitionProcessor.process(conversionFunction, state, matcher, repository);
            }
        }
    }

    @Override
    public void populateReferenceUsages(Unit unit, ModelRepository repository, ProcessorSupport processorSupport)
    {
    }
}
