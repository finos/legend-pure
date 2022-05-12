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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public interface PureFunction1<T, V> extends SharedPureFunction<V>, Function2<T, ExecutionSupport, V>
{
    @SuppressWarnings("unchecked")
    @Override
    default V execute(ListIterable<?> vars, ExecutionSupport es)
    {
        return value((T) vars.get(0), es);
    }
}
