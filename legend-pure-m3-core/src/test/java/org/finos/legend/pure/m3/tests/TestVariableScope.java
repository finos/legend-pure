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

package org.finos.legend.pure.m3.tests;

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestVariableScope extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testImmutableLetVariable()
    {
        try
        {
            compileTestSource("fromString.pure", "function test():Integer[1]\n" +
                    "{\n" +
                    "    let a = 5;\n" +
                    "    let a = 8;\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "'a' has already been defined!", "fromString.pure", 4, 5, e);
        }
    }

    @Test
    public void testImmutableFunctionVariable()
    {
        try
        {
            compileTestSource("fromString.pure", "function func(s:String[1]):String[1]\n" +
                                                       "{\n" +
                                                       "    let s = 'New';\n" +
                                                       "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "'s' has already been defined!", "fromString.pure", 3, 5, e);
        }
    }

    @Test
    public void testUnknownVariable()
    {
        try
        {
            compileTestSource("function testUnknownVariable():Nil[0]\n" +
                    "{\n" +
                    "    print($var);\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assert.assertNotNull(pe);
            Assert.assertTrue(pe instanceof PureCompilationException);
            Assert.assertEquals("The variable 'var' is unknown!", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assert.assertNotNull(sourceInfo);
            Assert.assertEquals(3, sourceInfo.getLine());
            Assert.assertEquals(12, sourceInfo.getColumn());
        }
    }
}
