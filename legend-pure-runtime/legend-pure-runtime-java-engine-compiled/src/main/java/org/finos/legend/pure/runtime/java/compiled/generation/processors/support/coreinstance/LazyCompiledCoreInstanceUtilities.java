// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.Function;

class LazyCompiledCoreInstanceUtilities
{
    private static final Function<Object, CoreInstance> TO_CORE_INSTANCE = ValCoreInstance::toCoreInstance;
    private static final Function<CoreInstance, Object> FROM_VAL_CORE_INSTANCE = ci -> ((ValCoreInstance) ci).getValue();
    private static final Function<CoreInstance, Object> FROM_CORE_INSTANCE = ci -> (ci instanceof ValCoreInstance) ? ((ValCoreInstance) ci).getValue() : ci;

    static Function<Object, CoreInstance> toCoreInstanceFunction()
    {
        return TO_CORE_INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <T> Function<CoreInstance, T> fromValCoreInstanceFunction()
    {
        return (Function<CoreInstance, T>) FROM_VAL_CORE_INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <T> Function<CoreInstance, T> fromCoreInstanceFunction()
    {
        return (Function<CoreInstance, T>) FROM_CORE_INSTANCE;
    }
}
