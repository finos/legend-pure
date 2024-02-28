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

package org.finos.legend.pure.runtime.java.interpreted.extension;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public interface InterpretedExtension
{
    MutableList<Pair<String, Function2<FunctionExecutionInterpreted, ModelRepository, NativeFunction>>> getExtraNatives();

    CoreInstance getExtraFunctionExecution(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<CoreInstance> function, ListIterable<? extends CoreInstance> params, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport, ProcessorSupport processorSupport, FunctionExecutionInterpreted interpreted);
}
