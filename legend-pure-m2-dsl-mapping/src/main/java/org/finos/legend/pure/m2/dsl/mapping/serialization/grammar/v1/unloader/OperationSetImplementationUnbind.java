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
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MergeOperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementationContainer;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class OperationSetImplementationUnbind implements MatchRunner<OperationSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.OperationSetImplementation;
    }

    @Override
    public void run(OperationSetImplementation operationSetImplementation, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        Shared.cleanUpReferenceUsage(operationSetImplementation._operationCoreInstance(), operationSetImplementation, processorSupport);
        Shared.cleanImportStub(operationSetImplementation._operationCoreInstance(), processorSupport);

        for (SetImplementationContainer param : operationSetImplementation._parameters())
        {
            param._setImplementationRemove();
        }
        if (operationSetImplementation instanceof  org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MergeOperationSetImplementation)
        {
          ((MergeOperationSetImplementation) operationSetImplementation)._validationFunctionRemove();
        }

    }
}

