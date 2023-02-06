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

package org.finos.legend.pure.m2.inlinedsl.path;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPathParsing extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.delete("testSource2.pure");
    }

    @Test
    public void testPathWithNoPath()
    {
        try
        {
            compileTestSource("testSource.pure",
                    "import meta::pure::metamodel::path::*;\n" +
                            "Class TestClass\n" +
                            "{\n" +
                            "    prop : String[1];\n" +
                            "}\n" +
                            "\n" +
                            "function test():Path[1]\n" +
                            "{\n" +
                            "    #/TestClass#\n" +
                            "}\n");
            Assert.fail("Expected parser error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureParserException.class, "A path must contain at least one navigation", "testSource.pure", 9, 6, 9, 16, e);
        }
    }

    @Test
    public void testPathWithNonExistentProperty()
    {
        try
        {
            compileTestSource("testSource.pure",
                    "import meta::pure::metamodel::path::*;\n" +
                            "Class TestClass\n" +
                            "{\n" +
                            "    prop : String[1];\n" +
                            "}\n" +
                            "\n" +
                            "function test():Path[1]\n" +
                            "{\n" +
                            "    #/TestClass/nonProp#\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "The property 'nonProp' can't be found in the type 'TestClass' (or any supertype).", "testSource.pure", 9, 17, 9, 23, e);
        }

        try
        {
            compileTestSource("testSource2.pure",
                    "import meta::pure::metamodel::path::*;\n" +
                            "Class TestClass1\n" +
                            "{\n" +
                            "    prop1 : TestClass2[1];\n" +
                            "}\n" +
                            "\n" +
                            "Class TestClass2\n" +
                            "{\n" +
                            "    prop2 : String[1];\n" +
                            "}\n" +
                            "\n" +
                            "function test():Path[1]\n" +
                            "{\n" +
                            "    #/TestClass1/prop1/nonProp#\n" +
                            "}\n");
            Assert.fail("Expected compilation error");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "The property 'nonProp' can't be found in the type 'TestClass2' (or any supertype).", "testSource2.pure", 14, 24, 14, 30, e);
        }
    }
}
