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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;

public class BaseInterpretedExtension implements InterpretedExtension
{
    private final MutableList<Pair<String, Function2<FunctionExecutionInterpreted, ModelRepository, NativeFunction>>> extraNatives;

    public BaseInterpretedExtension(MutableList<Pair<String, Function2<FunctionExecutionInterpreted, ModelRepository, NativeFunction>>> extraNatives)
    {
        this.extraNatives = extraNatives;
    }

    @Override
    public MutableList<Pair<String, Function2<FunctionExecutionInterpreted, ModelRepository, NativeFunction>>> getExtraNatives()
    {
        return this.extraNatives;
    }
}
