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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class InstanceValueUnbind implements MatchRunner<InstanceValue>
{
    @Override
    public String getClassName()
    {
        return M3Paths.InstanceValue;
    }

    @Override
    public void run(InstanceValue instanceValue, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        for (CoreInstance value : instanceValue._valuesCoreInstance())
        {
            if (!(value instanceof Property))
            {
                ((UnbindState)state).freeProcessedAndValidated(value);
            }
            Shared.cleanUpReferenceUsage(value, instanceValue, processorSupport);
            //TODO replace call to Instance.instanceOf once we resolve issue with Path not being available in this module - circular dependency
            if(value instanceof LambdaFunction ||
                    value instanceof SimpleFunctionExpression ||
                    Instance.instanceOf(value, M3Paths.Path, processorSupport) ||
                    value instanceof RootRouteNode)
            {
                matcher.fullMatch(value, state);
            }
            else if (Instance.instanceOf(value, M3Paths.RootGraphFetchTree, processorSupport))
            {
                matcher.fullMatch(value, state);
            }
            else if (value instanceof KeyExpression)
            {
                ((UnbindState) state).freeProcessedAndValidated(modelRepository.newBooleanCoreInstance(((KeyExpression)value)._add()));
                ((UnbindState) state).freeProcessedAndValidated(((KeyExpression)value)._key());
                matcher.fullMatch(((KeyExpression)value)._expression(), state);
            }
            else
            {
                Shared.cleanImportStub(value, processorSupport);
            }

            if (value instanceof ValueSpecification)
            {
                matcher.fullMatch(value, state);
                ((ValueSpecification)value)._usageContextRemove();
            }
        }
    }
}
