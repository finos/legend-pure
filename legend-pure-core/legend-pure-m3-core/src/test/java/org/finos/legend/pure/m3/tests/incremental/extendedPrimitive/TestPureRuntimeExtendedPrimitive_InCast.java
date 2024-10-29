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

package org.finos.legend.pure.m3.tests.incremental.extendedPrimitive;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeExtendedPrimitive_InCast extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.compile();
    }

    @Test
    public void testPureRuntimeExtendedPrimitiveUsedInCast()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Primitive x::SmallInt(x:Integer[1]) extends Integer [$this < $x]")
                        .createInMemorySource("userId.pure", "function test():String[1]{let b = 10->cast(@x::SmallInt(2));'ok';}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("x::SmallInt has not been defined!", "userId.pure", 1, 48)
                        .createInMemorySource("sourceId.pure", "Primitive x::SmallInt(x:Integer[1]) extends Integer [$this < $x]")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeExtendedPrimitiveUsedInCastReverse()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function test():String[1]{1->cast(@x::SmallInt(2));'ok';}")
                        .createInMemorySource("userId.pure", "Primitive x::SmallInt(x:Integer[1]) extends Integer [$this < $x]")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compile()
                        .createInMemorySource("sourceId.pure", "function test():String[1]{1->cast(@x::SmallInt(2));'ok';}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());

    }
}
