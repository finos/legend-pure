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

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
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

            if (value instanceof KeyExpression)
            {
                ((UnbindState) state).freeProcessedAndValidated(modelRepository.newBooleanCoreInstance(((KeyExpression)value)._add()));
                ((UnbindState) state).freeProcessedAndValidated(((KeyExpression)value)._key());
                matcher.fullMatch(((KeyExpression)value)._expression(), state);
            }
            else if (value instanceof ImportStub)
            {
                Shared.cleanImportStub(value, processorSupport);
            }
            else if (!(value instanceof PrimitiveCoreInstance) && !(value instanceof EnumStub))
            {
                matcher.fullMatch(value, state);
            }

            if (value instanceof ValueSpecification)
            {
                ((ValueSpecification)value)._usageContextRemove();
            }
        }
    }
}
