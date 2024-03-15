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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public class PureFunction2Wrapper<T1, T2, R> implements PureFunction2<T1, T2, R>
{
    private final PureFunction2<T1, T2, R> pureFunction2;
    private final ExecutionSupport executionSupport;

    public PureFunction2Wrapper(PureFunction2<T1, T2, R> pureFunction2, ExecutionSupport executionSupport)
    {
        this.pureFunction2 = pureFunction2;
        this.executionSupport = executionSupport;
    }


    @Override
    public R execute(ListIterable vars, ExecutionSupport es)
    {
        return this.pureFunction2.execute(vars, this.executionSupport);
    }


    @Override
    public R value(T1 argument1, T2 argument2, ExecutionSupport argument3)
    {
        return this.pureFunction2.value(argument1, argument2, this.executionSupport);
    }

    public ExecutionSupport getExecutionSupport()
    {
        return this.executionSupport;
    }
}
