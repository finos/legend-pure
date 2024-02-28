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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction3;

public abstract class DefendedPureFunction3<T1, T2, T3, R> implements PureFunction3<T1, T2, T3, R>
{
    @Override
    public R execute(ListIterable<?> vars, ExecutionSupport es)
    {
        return value((T1) vars.get(0), (T2) vars.get(1), (T3) vars.get(2), es);
    }
}
