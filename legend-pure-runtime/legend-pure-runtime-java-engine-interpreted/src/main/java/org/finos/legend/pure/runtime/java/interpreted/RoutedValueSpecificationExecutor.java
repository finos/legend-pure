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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

class RoutedValueSpecificationExecutor implements Executor
{
    static final RoutedValueSpecificationExecutor INSTANCE = new RoutedValueSpecificationExecutor();

    private RoutedValueSpecificationExecutor()
    {
    }

    @Override
    public CoreInstance execute(CoreInstance instance, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, VariableContext variableContext, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, FunctionExecutionInterpreted functionExecutionInterpreted, ProcessorSupport processorSupport) throws PureExecutionException
    {
        return instance.getValueForMetaPropertyToOne(M3Properties.value);
    }
}
