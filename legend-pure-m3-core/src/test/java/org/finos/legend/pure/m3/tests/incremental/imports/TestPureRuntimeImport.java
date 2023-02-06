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

package org.finos.legend.pure.m3.tests.incremental.imports;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeImport extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("other.pure");
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testPureRuntimeImport_Modify()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "import my::a::*;\n" +
                        "import my::a::b::*;\n" +
                        "function testPkg::test(inputs:meta::pure::metamodel::function::Function<Any>[*]):Any[*]{" +
                        "$inputs->filter(f | $f->genericType().typeArguments->at(0).rawType->toOne()->cast(@FunctionType).returnType.rawType == B)->map(f | $f->cast(@meta::pure::metamodel::function::Function<{->B[1]}>)->eval());" +
                        "}" +
                        "function my::a::b::test():Any[*]{let b = ^B()}")
                        .createInMemorySource("other.pure", "import my::a::*;\n" +
                                "Class my::a::B{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("other.pure",
                                "////Comment\n" + "import my::a::*;\n" + "Class my::a::B{}")
                        .compile()
                        .updateSource("other.pure", "import my::a::*;\n" +
                                "Class my::a::B{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeImport_Delete()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "import my::a::*;\n" +
                        "function testPkg::test():Any[*]{let b = ^B()}")
                        .createInMemorySource("other.pure", "import my::a::*;\n" +
                                "Class my::a::B{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("other.pure")
                        .compileWithExpectedCompileFailure("B has not been defined!", "sourceId.pure", 2, 42)
                        .createInMemorySource("other.pure", "import my::a::*;\n" +
                                "Class my::a::B{}")
                        .compile()
                        .updateSource("other.pure", "import my::a::*;\n" +
                                "Class my::a::B{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }
}
