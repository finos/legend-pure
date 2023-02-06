// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.mulitplicity;

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

public class ToOneManyWithMessage extends AbstractNativeFunctionGeneric
{
    public ToOneManyWithMessage()
    {
        super(getMethod(CompiledSupport.class, "toOneManyWithMessage", Object.class, String.class, SourceInformation.class),
                true, false, false, "toOneMany_T_MANY__String_1__T_$1_MANY$_");
    }
}
