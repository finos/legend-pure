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

public class TestTestFunction extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testFile.pure");
    }

    @Test
    public void testTestFunction()
    {
        try
        {
            compileTestSourceM3("testFile.pure",
                    "function <<test.Test>> testFn(arg:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "  assert($arg == 'the quick brown fox', |'')\n" +
                            "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error in function 'testFn': test functions may not have parameters", "testFile.pure", 1, 1, 1, 24, 4, 1, e);
        }
    }

    @Test
    public void testBeforePackageFunction()
    {
        try
        {
            compileTestSourceM3("testFile.pure",
                    "function <<test.BeforePackage>> testFn(arg:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "  assert($arg == 'the quick brown fox', |'')\n" +
                            "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error in function 'testFn': test functions may not have parameters", "testFile.pure", 1, 1, 1, 33, 4, 1, e);
        }
    }

    @Test
    public void testAfterPackageFunction()
    {
        try
        {
            compileTestSourceM3("testFile.pure",
                    "function <<test.AfterPackage>> testFn(arg:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "  assert($arg == 'the quick brown fox', |'')\n" +
                            "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error in function 'testFn': test functions may not have parameters", "testFile.pure", 1, 1, 1, 32, 4, 1, e);
        }
    }

    @Test
    public void testToFixFunction()
    {
        try
        {
            compileTestSourceM3("testFile.pure",
                    "function <<test.ToFix>> testFn(arg:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "  assert($arg == 'the quick brown fox', |'')\n" +
                            "}" );
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error in function 'testFn': test functions may not have parameters", "testFile.pure", 1, 1, 1, 25, 4, 1, e);
        }
    }
}
