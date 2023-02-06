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

package org.finos.legend.pure.m3.tests.elements.namespace;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestImportConflict extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testImportTypeConflict() throws Exception
    {
        try
        {
            compileTestSource("fromString.pure",
                    "import a::*;\n" +
                    "import b::*;\n" +
                    "Class a::Employee\n" +
                    "{\n" +
                    "   a:String[1];\n" +
                    "}\n" +
                    "Class b::Employee\n" +
                    "{\n" +
                    "    b: String[1];\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "    print(Employee);\n" +
                    "}\n");
            Assert.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Employee has been found more than one time in the imports: [a::Employee, b::Employee]", "fromString.pure", 13, 11, 13, 11, 13, 18, e);
        }
    }

    @Test
    public void testImportTypeNonConflict() throws Exception
    {
        runtime.createInMemorySource("fromString.pure",
                "import a::*;\n" +
                "import a::*;\n" +
                "Class a::Employee\n" +
                "{\n" +
                "   a:String[1];\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "    print(Employee,1);\n" +
                "}\n");
        runtime.compile();
    }
}
