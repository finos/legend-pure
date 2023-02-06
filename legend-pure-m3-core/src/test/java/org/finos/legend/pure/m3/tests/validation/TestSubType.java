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

package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSubType extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("src.pure");
    }

    @Test
    public void testIncompatibleTypes()
    {
        try
        {
            compileTestSource("src.pure",
                    "Class A{}\n" +
                    "Class B extends A{}\n" +
                    "Class C{}\n" +
                    "native function meta::pure::functions::lang::subType<T|m>(source:Any[m], object:T[1]):T[m];\n" +
                    "function testFunc():C[1]\n" +
                    "{\n" +
                    "   let a = ^B()->subType(@C);" +
                    "}");

            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The type B is not compatible with C", "src.pure", 7, 18, 7, 18, 7, 24, e);
        }
    }

    @Test
    public void testIncompatibleTypesSuperTypes()
    {
        try
        {
            compileTestSource("src.pure",
                    "Class A{}\n" +
                    "Class B extends A{}\n" +
                    "Class C{}\n" +
                    "native function meta::pure::functions::lang::subType<T|m>(source:Any[m], object:T[1]):T[m];\n" +
                    "function testFunc():A[1]\n" +
                    "{\n" +
                    "   let a = ^B()->subType(@A);" +
                    "}");

            this.runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The type B is not compatible with A", "src.pure", 7, 18, 7, 18, 7, 24, e);
        }
    }

    @Test
    public void testSubTypeOk()
    {
        compileTestSource("src.pure",
                "Class A{}\n" +
                "Class B extends A{}\n" +
                "Class C{}\n" +
                "native function meta::pure::functions::lang::subType<T|m>(source:Any[m], object:T[1]):T[m];\n" +
                "function testFunc():A[1]\n" +
                "{\n" +
                "   let a = ^A()->subType(@B);" +
                "}");

        this.runtime.compile();
    }
}
