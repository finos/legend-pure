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

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;

public class MeasureProcessor extends Processor<Measure>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Measure;
    }

    @Override
    public void process(Measure measure, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        Unit canonicalUnit = measure._canonicalUnit();
        if (canonicalUnit != null)
        {
            PostProcessor.processElement(matcher, canonicalUnit, state, processorSupport);
        }
        measure._nonCanonicalUnits().forEach(unit -> PostProcessor.processElement(matcher, unit, state, processorSupport));
    }

    @Override
    public void populateReferenceUsages(Measure measure, ModelRepository repository, ProcessorSupport processorSupport)
    {
    }
}
