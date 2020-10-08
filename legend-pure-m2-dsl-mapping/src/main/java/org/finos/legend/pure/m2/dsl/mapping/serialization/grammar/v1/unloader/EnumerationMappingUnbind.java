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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor.EnumerationMappingProcessor;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumValueMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class EnumerationMappingUnbind implements MatchRunner<EnumerationMapping>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.EnumerationMapping;
    }

    @Override
    public void run(EnumerationMapping enumerationMapping, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        MutableList<? extends CoreInstance> enumerations = Lists.mutable.with(enumerationMapping._enumerationCoreInstance()).withAll(EnumerationMappingProcessor.collectSourceEnumerationCoreInstances(enumerationMapping));
        Shared.cleanUpReferenceUsages(enumerations, enumerationMapping, processorSupport);
        Shared.cleanImportStubs(enumerations, processorSupport);

        for (EnumValueMapping enumValueMapping : (RichIterable<? extends EnumValueMapping>)enumerationMapping._enumValueMappings())
        {
            Shared.cleanEnumStubs(Lists.mutable.with(enumValueMapping._enumCoreInstance()).withAll(EnumerationMappingProcessor.collectSourceEnumCoreInstances(enumValueMapping)), processorSupport);
        }
    }
}
