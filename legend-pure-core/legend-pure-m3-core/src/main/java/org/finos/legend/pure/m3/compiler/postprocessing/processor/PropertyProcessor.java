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
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.DefaultValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;

public class PropertyProcessor extends Processor<Property<?,?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Property;
    }

    @Override
    public void process(Property<?, ?> property, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        DefaultValue defaultValue = property._defaultValue();
        if (defaultValue != null)
        {
            FunctionDefinition<?> defaultValueFn = defaultValue._functionDefinition();
            ValueSpecification defaultValueFnExpressionSequence = defaultValueFn._expressionSequence().toList().getFirst();

            PostProcessor.processElement(matcher, defaultValueFnExpressionSequence, state, processorSupport);
            PostProcessor.processElement(matcher, defaultValueFn, state, processorSupport);
        }
    }

    @Override
    public void populateReferenceUsages(Property<?, ?> property, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GenericTypeTraceability.addTraceForProperty(property, repository, processorSupport);
    }
}
