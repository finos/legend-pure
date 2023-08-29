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

package org.finos.legend.pure.m3.tests.elements.function.inference;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.*;

import static org.junit.Assert.fail;

public class TestRelationTypeInference extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final boolean shouldSetTypeInferenceObserver = false;
    private static final String typeInferenceTestProperty = "pure.typeinference.test";
    private static boolean typeInferenceTestPropertySet = false;
    private static String previousTypeInferenceTest;

    private static final String inferenceTestFileName = "inferenceTest.pure";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());

        if (shouldSetTypeInferenceObserver)
        {
            previousTypeInferenceTest = System.setProperty(typeInferenceTestProperty, "true");
            typeInferenceTestPropertySet = true;
        }
    }

    @AfterClass
    public static void cleanUp()
    {
        if (typeInferenceTestPropertySet)
        {
            if (previousTypeInferenceTest == null)
            {
                System.clearProperty(typeInferenceTestProperty);
            }
            else
            {
                System.setProperty(typeInferenceTestProperty, previousTypeInferenceTest);
            }
        }
    }

    @After
    public void clearRuntime()
    {
        deleteInferenceTest();
    }


    @Test
    public void columnInference()
    {
        compileInferenceTest(
                "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Any[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName]);\n" +
                        "}" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void columnSingle()
    {
        compileInferenceTest(
                "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Any[*]" +
                        "{" +
                        "   Firm.all()->project(~legal:x|$x.legalName);\n" +
                        "}" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpec<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void columnInferenceUsingBracketLambda()
    {
        compileInferenceTest(
                "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Any[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:{x|$x.legalName}])->map(c|$c.legal);\n" +
                        "}" +
                        "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void columnInferenceUsingBracketMultiLambda()
    {
        compileInferenceTest(
                "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Any[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:{x|$x.legalName}, legalMod:{x|$x.legalName+'ok'}])->map(x|$x.legal + $x.legalMod);\n" +
                        "}" +
                        "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void columnInferenceChained()
    {
        compileInferenceTest(
                "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Any[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->map(c|$c.legal);\n" +
                        "}" +
                        "\n" +
                        "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testTypeParameterOperation()
    {
        compileInferenceTest(
                "Class Firm{legalName:String[1];}" +
                        "function f():Any[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->join(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal3)\n" +
                        "}" +
                        "\n" +
                        "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function join<Z,T>(x:meta::pure::metamodel::relation::Relation<Z>[1], k:meta::pure::metamodel::relation::Relation<T>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];" +
                        "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testTypeParameterOperationWithDups()
    {
        try
        {
            compileInferenceTest(
                    "Class Firm{legalName:String[1];}" +
                            "function f():Any[*]" +
                            "{" +
                            "   Firm.all()->project(~[legal:x|$x.legalName])->join(Firm.all()->project(~[legal:x|$x.legalName]))\n" +
                            "}" +
                            "\n" +
                            "native function join<Z,T>(x:meta::pure::metamodel::relation::Relation<Z>[1], k:meta::pure::metamodel::relation::Relation<T>[1]):meta::pure::metamodel::relation::Relation<T+Z+Z>[1];" +
                            "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                            "\n");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:1 column:102), \"The relation contains duplicates: [legal]\"", e.getMessage());
        }
    }

    @Test
    public void testTypeParameterDifferenceOperation()
    {
        try
        {
            compileInferenceTest(
                    "Class Firm{legalName:String[1];}" +
                            "function f():Any[*]" +
                            "{" +
                            "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->map(x|$x.legal3);\n" +
                            "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->diff(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal);\n" +
                            "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->diff(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal3);\n" +
                            "}" +
                            "\n" +
                            "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                            "native function diff<Z,T>(x:meta::pure::metamodel::relation::Relation<T>[1], k:meta::pure::metamodel::relation::Relation<Z>[1]):meta::pure::metamodel::relation::Relation<T-Z>[1];" +
                            "native function project<Z,T>(cl:Z[*], x:meta::pure::metamodel::relation::FuncColSpecArray<{Z[1]->Any[*]},T>[1]):meta::pure::metamodel::relation::Relation<T>[1];" +
                            "\n"
            );
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:135), \"The system can't find the column legal3 in the Relation (legal:String)\"", e.getMessage());
        }
    }

    @Test
    public void testGenericTypeOperationCopy()
    {
        compileInferenceTest(
                "native function extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "function extend<T,Z>(r:TDS<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1]\n" +
                        "{\n" +
                        "   $r->cast(@meta::pure::metamodel::relation::Relation<T>)->extend($f);\n" +
                        "}" +
                        "Class TDS<T> extends meta::pure::metamodel::relation::Relation<T> // T is of RelationType kind\n" +
                        "{\n" +
                        "    csv : String[1];\n" +
                        "}"
        );
    }

    @Test
    public void testCast()
    {
        compileInferenceTest(
                        "function extend<T>(r:meta::pure::metamodel::relation::Relation<T>[1]):Any[*]\n" +
                        "{\n" +
                        "   $r->cast(@meta::pure::metamodel::relation::Relation<(a:Integer, f:String)>)->map(c|$c.a->toString() + $c.f);\n" +
                        "}" +
                        "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testCastError()
    {
        try
        {
            compileInferenceTest(
                    "function extend<T>(r:meta::pure::metamodel::relation::Relation<T>[1]):Any[*]\n" +
                            "{\n" +
                            "   $r->cast(@meta::pure::metamodel::relation::Relation<(a:Integer, f:String)>)->map(c|$c.z);\n" +
                            "}" +
                            "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:90), \"The system can't find the column z in the Relation (a:Integer, f:String)\"", e.getMessage());
        }
    }

    private void compileInferenceTest(String source)
    {
        compileTestSource(inferenceTestFileName, source);
    }

    private void deleteInferenceTest()
    {
        runtime.delete(inferenceTestFileName);
        runtime.compile();
    }

}
