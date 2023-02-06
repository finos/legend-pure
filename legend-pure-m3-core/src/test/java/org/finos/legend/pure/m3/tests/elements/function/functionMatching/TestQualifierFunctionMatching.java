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

package org.finos.legend.pure.m3.tests.elements.function.functionMatching;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestQualifierFunctionMatching extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("id.pure");
    }

    @Test
    public void testQualifierFunctionMatchingError()
    {
        try
        {
            compileTestSource("id.pure", "Class A\n" +
                                    "{\n" +
                                    "   t(i:Integer[1]){$i}:Integer[1];\n" +
                                    "}" +
                                    "function a():Integer[1]" +
                                    "{" +
                                    "   ^A().t('1');" +
                                    "}");
        }
        catch(Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:id.pure line:4 column:34), \"The system can't find a match for the function: t(_:A[1],_:String[1])\"", e.getMessage());
        }
    }

    @Test
    public void testQualifierFunctionMatchingNoParam()
    {
        try
        {
            compileTestSource("id.pure", "Class A\n" +
                                    "{\n" +
                                    "   t(){2}:Integer[1];\n" +
                                    "}" +
                                    "function a():Integer[1]" +
                                    "{" +
                                    "   ^A().t()+3;" +
                                    "}");
        }
        catch(Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:id.pure line:4 column:34), \"The system can't find a match for the function: t(_:A[1],_:String[1])\"", e.getMessage());
        }
    }

    @Test
    public void testQualifierFunctionMatchingUsingDistance()
    {
        compileTestSource("id.pure", "Class A\n" +
                                "{\n" +
                                "   t(i:Integer[1]){1}:Integer[1];\n" +
                                "   t(i:Number[1]){'1'}:String[1];\n" +
                                "}\n" +
                                "function a():Any[*]\n" +
                                "{\n" +
                                "   ^A().t(1)+1;\n" +
                                "   ^A().t(1.0)+'ok';\n" +
                                "}");
    }

    @Test
    public void testQualifierFunctionMatching01Source()
    {
        compileTestSource("id.pure", "Class A\n" +
                                "{\n" +
                                "   t(){2}:Integer[1];\n" +
                                "}" +
                                "function getA():A[0..1]" +
                                "{" +
                                "   ^A();" +
                                "}" +
                                "function a():Integer[1]" +
                                "{" +
                                "   getA()->toOne().t()+3;" +
                                "}");
    }


    @Test
    public void testQualifierFunctionMatchingInheritanceError()
    {
        try
        {
            compileTestSource("id.pure", "Class A\n" +
                                    "{\n" +
                                    "   t(){2}:Integer[1];\n" +
                                    "}" +
                                    "Class B extends A" +
                                    "{" +
                                    "   t(){'2'}:String[1];" +
                                    "}" +
                                    "" +
                                    "function a():Integer[1]" +
                                    "{" +
                                    "   3;" +
                                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:id.pure line:4 column:8), \"Property conflict on class B: property 't' defined on B conflicts with property 't' defined on A\"", e.getMessage());
        }
    }

    @Test
    public void testQualifierFunctionMatchingInheritance()
    {
        compileTestSource("id.pure", "Class A\n" +
                                "{\n" +
                                "   t(){2}:Number[1];\n" +
                                "}" +
                                "Class B extends A" +
                                "{" +
                                "   t(){2.0}:Float[1];" +
                                "}" +
                                "" +
                                "function a():Integer[1]" +
                                "{" +
                                "   3;" +
                                "}");
    }

    @Test
    public void testQualifierFunctionMatchingInheritanceTricky()
    {
        compileTestSource("id.pure", "Class K {}" +
                                "Class SubK extends K {name : String[1];}" +
                                "Class A\n" +
                                "{\n" +
                                "   t(s:Integer[1]){^K();}:K[1];\n" +
                                "   t(s:String[1]){'2'}:String[1];\n" +
                                "}" +
                                "Class B extends A" +
                                "{" +
                                "   t(s:Number[1]){^SubK(name='e')}:SubK[1];" +
                                "}" +
                                "" +
                                "function a():Any[*]" +
                                "{" +
                                "   ^B().t(1).name;" +
                                "   ^B().t('1')+'1';" +
                                "}");
    }


}
