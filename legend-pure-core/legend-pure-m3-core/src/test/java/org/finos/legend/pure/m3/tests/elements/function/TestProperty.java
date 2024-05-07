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

package org.finos.legend.pure.m3.tests.elements.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProperty extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
    }

    @Test
    public void testNewWithProperty()
    {
        try
        {
            compileTestSource("fromString.pure", "Class A{name : String[1];}\n" +
                    "function myFunc():A[1]\n" +
                    "{\n" +
                    "    ^A(nameError = 'ok');\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The property 'nameError' can't be found in the type 'A' or in its hierarchy.", 4, 8, e);
        }
    }

    @Test
    public void testNewWithPropertySpaceError()
    {
        try
        {
            compileTestSource("fromString.pure", "Class A{'first name' : String[1];}\n" +
                    "function myFunc():A[1]\n" +
                    "{\n" +
                    "    ^A('firstname' = 'ok');\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The property 'firstname' can't be found in the type 'A' or in its hierarchy.", 4, 8, e);
        }
    }

    @Test
    public void testNewWithPropertySpaceOk()
    {
        compileTestSource("fromString.pure", "Class A{'first name' : String[1];}\n" +
                "function myFunc():A[1]\n" +
                "{\n" +
                "    ^A('first name' = 'ok');\n" +
                "}\n");

    }

    @Test
    public void testPropertyAccessOk()
    {
        compileTestSource("fromString.pure", "Class A{'first name' : String[1];}\n" +
                "function myFunc():A[1]\n" +
                "{\n" +
                "    A.all()->filter(a|$a.'first name' == 'ok')->toOne();\n" +
                "}\n");

    }

    @Test
    public void testPropertyAccessError()
    {
        try
        {
            compileTestSource("fromString.pure", "Class A{'first name' : String[1];}\n" +
                    "function myFunc():A[1]\n" +
                    "{\n" +
                    "    A.all()->filter(a|$a.'first _name' == 'ok')->toOne();\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Can't find the property 'first _name' in the class A", 4, 26, e);
        }
    }

}
