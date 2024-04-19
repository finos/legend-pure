// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.generictype;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestTypeResolutionForIf extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testIfWithAny()
    {
        compileTestSource(
                "fromString.pure",
                "function a():Any[1]\n" +
                        "{\n" +
                        "   if (true, |1, |'String');\n" +
                        "}\n");
    }

    @Test
    public void testIfWithError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function a():Integer[1]\n" +
                        "{\n" +
                        "   if (true, |1, |'String');\n" +
                        "}\n"));
        Assert.assertEquals("Compilation error at (resource:fromString.pure line:3 column:4), \"Return type error in function 'a'; found: meta::pure::metamodel::type::Any; expected: Integer\"", e.getMessage());
    }

    @Test
    public void testIfWithGenerics()
    {
        compileTestSource(
                "fromString.pure",
                "function a<K>(x:K[*]):Any[1]\n" +
                        "{\n" +
                        "   if (true, |$x->at(0), |true);\n" +
                        "}\n");
    }

    @Test
    @Ignore
    @ToFix
    public void testIfWithGenericsError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function a<K>(x:K[*]):K[1]\n" +
                        "{\n" +
                        "   if (true, |$x->at(0), |true);\n" +
                        "}\n"));
        Assert.assertEquals("Compilation error at (resource:fromString.pure line:3 column:4), \"Return type error in function 'a'; found: meta::pure::metamodel::type::Any; expected: K\"", e.getMessage());
    }

    @Test
    public void testIfWithGenerics2()
    {
        compileTestSource(
                "fromString.pure",
                "function a<K>(x:K[*]):Any[1]\n" +
                        "{\n" +
                        "   if (true, |true, |$x->at(0));\n" +
                        "}\n");
    }

    @Test
    @Ignore
    @ToFix
    public void testIfWithGenerics2Error()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function a<K>(x:K[*]):K[1]\n" +
                        "{\n" +
                        "   if (true, |true, |$x->at(0));\n" +
                        "}\n"));
        Assert.assertEquals("Compilation error at (resource:fromString.pure line:3 column:4), \"Return type error in function 'a'; found: meta::pure::metamodel::type::Any; expected: K\"", e.getMessage());
    }
}
