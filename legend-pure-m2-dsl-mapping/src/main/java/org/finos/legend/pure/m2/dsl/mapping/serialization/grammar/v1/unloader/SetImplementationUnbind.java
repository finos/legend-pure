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

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class SetImplementationUnbind implements MatchRunner<SetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.SetImplementation;
    }

    @Override
    public void run(SetImplementation setImplementation, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        CoreInstance _class = setImplementation._classCoreInstance();
        if (_class != null)
        {
            Shared.cleanUpReferenceUsage(_class, setImplementation, processorSupport);
            Shared.cleanImportStub(_class, processorSupport);
        }

        if (setImplementation instanceof InstanceSetImplementation && ((InstanceSetImplementation)setImplementation)._mappingClass() != null)
        {
            matcher.fullMatch(((InstanceSetImplementation)setImplementation)._mappingClass(), state);
            ((InstanceSetImplementation)setImplementation)._mappingClassRemove();
        }
    }
}
