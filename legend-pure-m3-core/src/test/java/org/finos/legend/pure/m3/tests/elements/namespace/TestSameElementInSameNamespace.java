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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSameElementInSameNamespace extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
        runtime.delete("/test/testSource.pure");
        runtime.delete("/test/testSource1.pure");
        runtime.delete("/test/testSource2.pure");
    }

    @Test
    public void testClass()
    {
        try
        {
            compileTestSource("fromString.pure", "Class model::test::Person\n" +
                    "{\n" +
                    "   lastName:String[1];\n" +
                    "}\n" +
                    "Class model::test::Person\n" +
                    "{\n" +
                    "   otherName:String[1];\n" +
                    "}\n");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            assertPureException(PureParserException.class, "The element 'Person' already exists in the package 'model::test'", "fromString.pure", 5, 20, e);
        }
    }


    @Test
    public void testFunction()
    {
        try
        {
            compileTestSource("fromString.pure", "function go():Nil[0]{[];}\n" +
                    "function go():Nil[0]{[];}\n");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "The function 'go__Nil_0_' is defined more than once in the package '::' at: fromString.pure (line:1 column:10), fromString.pure (line:2 column:10)", "fromString.pure", 2, 10, e);
        }
    }

    @Test
    public void testDuplicateFunctionsInDifferentSections()
    {
        try
        {
            compileTestSource("/test/testSource.pure",
                    "###Pure\n" +
                            "function go():Nil[0]\n" +
                            "{\n" +
                            "   [];\n" +
                            "}\n" +
                            "###Pure\n" +
                            "function go():Nil[0]\n" +
                            "{\n" +
                            "   [];\n" +
                            "}");
            Assert.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "The function 'go__Nil_0_' is defined more than once in the package '::' at: /test/testSource.pure (line:2 column:10), /test/testSource.pure (line:7 column:10)", e);
        }
    }

    @Test
    public void testDuplicateFunctionsInDifferentFiles()
    {
        compileTestSource("/test/testSource1.pure",
                "function go():Nil[0]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}\n");
        try
        {
            compileTestSource("/test/testSource2.pure",
                            "function go():Nil[0]\n" +
                            "{\n" +
                            "   [];\n" +
                            "}");
            Assert.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "The function 'go__Nil_0_' is defined more than once in the package '::' at: /test/testSource1.pure (line:1 column:10), /test/testSource2.pure (line:1 column:10)", "/test/testSource2.pure", 1, 10, e);
        }
    }

    @Test
    public void testAssociation()
    {
        try
        {
            compileTestSource("fromString.pure", "Class Firm {}" +
                    "Class Person {}\n" +
                    "Association arg::myAsso {firm:Firm[1]; employees:Person[*];}\n" +
                    "Association arg::myAsso {firm:Firm[1]; employees:Person[*];}\n");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            assertPureException(PureParserException.class, "The element 'myAsso' already exists in the package 'arg'", "fromString.pure", 3, 18, e);
        }
    }

    @Test
    public void testEnum()
    {
        try
        {
            compileTestSource("fromString.pure", "Enum myEnum {CUSIP, SEDOL}\n" +
                    "Enum myEnum {CUSIP, SEDOL}");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            assertPureException(PureParserException.class, "The element 'myEnum' already exists in the package '::'", "fromString.pure", 2, 6, e);
        }
    }
}
