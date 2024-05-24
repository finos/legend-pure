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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;

import java.util.function.BiFunction;

public abstract class BaseInterpretedExtension implements InterpretedExtension
{
    private final Iterable<? extends Pair<String, ? extends BiFunction<? super FunctionExecutionInterpreted, ? super ModelRepository, ? extends NativeFunction>>> extraNatives;

    protected BaseInterpretedExtension(Iterable<? extends Pair<String, ? extends BiFunction<? super FunctionExecutionInterpreted, ? super ModelRepository, ? extends NativeFunction>>> extraNatives)
    {
        this.extraNatives = extraNatives;
    }

    protected BaseInterpretedExtension(Pair<String, ? extends BiFunction<? super FunctionExecutionInterpreted, ? super ModelRepository, ? extends NativeFunction>>... extraNatives)
    {
        this(Lists.immutable.with(extraNatives));
    }

    protected BaseInterpretedExtension(String extraNativeFunctionId, BiFunction<? super FunctionExecutionInterpreted, ? super ModelRepository, ? extends NativeFunction> extraNativeFunctionGenerator)
    {
        this(Lists.immutable.with(Tuples.pair(extraNativeFunctionId, extraNativeFunctionGenerator)));
    }

    protected BaseInterpretedExtension()
    {
        this(Lists.immutable.empty());
    }

    @Override
    public Iterable<? extends Pair<String, ? extends BiFunction<? super FunctionExecutionInterpreted, ? super ModelRepository, ? extends NativeFunction>>> getExtraNatives()
    {
        return this.extraNatives;
    }
}
