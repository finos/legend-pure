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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.math;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

public class Times extends AbstractNativeFunctionGeneric
{
    public Times()
    {
        super(getMethod(CompiledSupport.class, "times", RichIterable.class),
                "times_Number_MANY__Number_1_", "times_Integer_MANY__Integer_1_", "times_Float_MANY__Float_1_", "times_Decimal_MANY__Decimal_1_");
    }
}
