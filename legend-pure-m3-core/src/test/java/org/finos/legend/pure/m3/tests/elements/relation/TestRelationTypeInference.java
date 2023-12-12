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

package org.finos.legend.pure.m3.tests.elements.relation;

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
    public void testColumnFunctionSingleInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Relation<(legal:String)>[1]" +
                        "{" +
                        "   Firm.all()->project(~legal:x|$x.legalName);\n" +
                        "}" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpec<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testColumnFunctionCollectionInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():Relation<(legal:String)>[1]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName]);\n" +
                        "}" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testColumnFunctionCollectionChainedWithNewFunctionInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():String[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->map(c|$c.legal);\n" +
                        "}" +
                        "\n" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testColumnFunctionCollectionChainedWithNewFunctionInferenceWrapped()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():String[*]" +
                        "{" +
                        "   execute(|Firm.all()->project(~[legal:x|$x.legalName]));\n" +
                        "}" +
                        "\n" +
                        "native function execute(rel:Function<Any>[1]):String[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testColumnTypeInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f():Relation<(col:String)>[1]" +
                        "{" +
                        "   test('eee')" +
                        "}" +
                        "\n" +
                        "native function test<T>(val:T[1]):Relation<(col:T)>[1];"
        );
    }

    @Test
    public void testColumnTypeInferenceReverse()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f():String[1]" +
                        "{" +
                        "   test(~col:String)" +
                        "}" +
                        "\n" +
                        "native function test<T>(val:ColSpec<(col:T)>[1]):T[1];"
        );
    }

    @Test
    public void testColumnFunctionUsingBracketLambdaCollectionInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():String[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->map(c|$c.legal);\n" +
                        "}" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testColumnFunctionUsingBracketLambdaRealCollectionInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "\n" +
                        "function f():String[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName, legalMod:x|$x.legalName+'ok'])->map(x|$x.legal->toOne() + $x.legalMod->toOne());\n" +
                        "}" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }


    @Test
    public void testTypeParameterUnionForRelationType()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class Firm{legalName:String[1];}" +
                        "function f():String[*]" +
                        "{" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->join(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal3)\n" +
                        "}" +
                        "\n" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function join<Z,T>(x:Relation<Z>[1], k:Relation<T>[1]):Relation<T+Z>[1];" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Test
    public void testTypeParameterUnionForRelationTypeWithManyOperations()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "Class Firm{legalName:String[1];}" +
                            "function f():Any[*]" +
                            "{" +
                            "   Firm.all()->project(~[legal:x|$x.legalName])->join(Firm.all()->project(~[legal:x|$x.legalName]))\n" +
                            "}" +
                            "\n" +
                            "native function join<Z,T>(x:Relation<Z>[1], k:Relation<T>[1]):Relation<T+Z+Z>[1];" +
                            "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                            "\n");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:1 column:144), \"The relation contains duplicates: [legal]\"", e.getMessage());
        }
    }

    @Test
    public void testTypeParameterDifferenceForRelationType()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "Class Firm{legalName:String[1];}" +
                            "function f():Any[*]" +
                            "{" +
                            "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->map(x|$x.legal3);\n" +
                            "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->diff(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal);\n" +
                            "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->diff(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal3);\n" +
                            "}" +
                            "\n" +
                            "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                            "native function diff<Z,T>(x:Relation<T>[1], k:Relation<Z>[1]):Relation<T-Z>[1];" +
                            "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                            "\n"
            );
            fail();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:135), \"The system can't find the column legal3 in the Relation (legal:String)\"", e.getMessage());
        }
    }

    @Test
    public void testTypeParameterTriggeringRelationTypeCopy()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "\n" +
                        "function f<T,Z>(r:TDS<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1]\n" +
                        "{\n" +
                        "   $r->cast(@meta::pure::metamodel::relation::Relation<T>)->extend($f);\n" +
                        "}" +
                        "\n" +
                        "native function extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
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
                "import meta::pure::metamodel::relation::*;" +
                        "function f<T>(r:meta::pure::metamodel::relation::Relation<T>[1]):String[*]\n" +
                        "{\n" +
                        "   $r->cast(@meta::pure::metamodel::relation::Relation<(a:Integer, f:String)>)->map(c|$c.a->toOne()->toString() + $c.f->toOne());\n" +
                        "}" +
                        "native function map<T,V>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testCastError()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "function f<T>(r:Relation<T>[1]):Any[*]\n" +
                            "{\n" +
                            "   $r->cast(@Relation<(a:Integer, f:String)>)->map(c|$c.z);\n" +
                            "}" +
                            "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:57), \"The system can't find the column z in the Relation (a:Integer, f:String)\"", e.getMessage());
        }
    }

    @Test
    public void testTypeEqualityForMatching()
    {

        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f<T>(r:Relation<T>[1]):Integer[*]\n" +
                        "{\n" +
                        "   concat($r->cast(@Relation<(a:Integer, f:String)>), $r->cast(@Relation<(a:Integer, f:String)>))->map(x|$x.a);\n" +
                        "}" +
                        "native function concat<T>(rel:Relation<T>[1], rel2:Relation<T>[1]):Relation<T>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");

    }

    @Test
    public void testTypeEqualityForMatchingFail()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f<T>(r:Relation<T>[1]):Relation<Any>[1]\n" +
                        "{\n" +
                        "   concat($r->cast(@Relation<(ae:String, f:String)>), $r->cast(@Relation<(a:Integer, f:String)>));" +
                        "}" +
                        "native function concat<T>(rel:Relation<T>[1], rel2:Relation<T>[1]):Relation<T>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testRelationTypeInParamsEqualsMatching()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(r:Relation<(a:Integer)>[1]):Integer[*]\n" +
                        "{\n" +
                        "   $r->tt()->map(x|$x.a);\n" +
                        "}" +
                        "native function tt(rel:Relation<(a:Integer)>[1]):Relation<(a:Integer)>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testRelationTypeInParamsCompatibleEqualSubsetMatching()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(r:Relation<(a:Integer)>[1]):Integer[*]\n" +
                        "{\n" +
                        "   $r->cast(@Relation<(a:Integer,b:Float)>)->tt()->map(x|$x.a);\n" +
                        "}" +
                        "native function tt(rel:Relation<(a:Integer)>[1]):Relation<(a:Integer)>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testRelationTypeInParamsCompatibleSubsumesTypeMatching()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(r:Relation<(a:Integer)>[1]):Integer[*]\n" +
                        "{\n" +
                        "   $r->tt()->map(x|$x.a);\n" +
                        "}" +
                        "native function tt(rel:Relation<(a:Number)>[1]):Relation<(a:Integer)>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testRelationTypeInParamsAnyTypeMatching()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f<T>(r:Relation<(a:Integer)>[1]):Integer[*]\n" +
                        "{\n" +
                        "   $r->tt()->map(x|$x.a);\n" +
                        "}" +
                        "native function tt(rel:Relation<Any>[1]):Relation<(a:Integer)>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
    }

    @Test
    public void testRelationTypeInParamsCompatibleSubsumesTypeMatchingFail()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "function f<T>(r:Relation<(a:Number)>[1]):Integer[*]\n" +
                            "{\n" +
                            "   $r->tt()->map(x|$x.a);\n" +
                            "}" +
                            "native function tt(rel:Relation<(a:Integer)>[1]):Relation<(a:Integer)>[1];" +
                            "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");
            fail();

        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:8), \"The system can't find a match for the function: tt(_:Relation<(a:Number)>[1])\n" +
                    "\n" +
                    "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                    "\ttt(Relation<(a:Integer)>[1]):Relation<(a:Integer)>[1]\n" +
                    "\n" +
                    "No functions, in packages not imported, match the function name.\n" +
                    "\"", e.getMessage());
        }
    }

    @Test
    public void testRelationTypeInParamsAndReturn()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(r:Relation<(col1:String, col2:Integer)>[1]):Relation<(col1:String, col2:Integer)>[1]\n" +
                        "{\n" +
                        "  $r->filter(x|$x.col2 > 1);\n" +
                        "}" +
                        "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];");
    }

    @Test
    public void testRelationTypeInParamsAndCompatibleReturn()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(r:Relation<(col1:String, col2:Integer)>[1]):Relation<(col1:String)>[1]\n" +
                        "{\n" +
                        "  $r->filter(x|$x.col2 > 1);\n" +
                        "}" +
                        "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];");
    }

    @Test
    public void testRelationTypeInParamsAndInCompatibleReturn()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "function f(r:Relation<(col1:String, col2:Integer)>[1]):Relation<(col3:String)>[1]\n" +
                            "{\n" +
                            "  $r->filter(x|$x.col2 > 1);\n" +
                            "}" +
                            "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:7), \"Return type error in function 'f'; found: meta::pure::metamodel::relation::Relation<(col1:String, col2:Integer)>; expected: meta::pure::metamodel::relation::Relation<(col3:String)>\"", e.getMessage()
            );
        }
    }

    @Test
    public void testRelationTypeAdvancedUnionSubstraction()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    let res = $t->rename(~str:String, ~newStr:String);\n" +
                        "    assertEquals('stuff', $res->map(c|$c.newStr)->joinStrings(''));\n" +
                        "}" +
                        "native function meta::pure::functions::relation::rename<T,Z,V>(r:Relation<T>[1], old:ColSpec<Z>[1], new:ColSpec<V>[1]):Relation<T-Z+V>[1];\n" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];");

    }

    @Test
    public void testRelationTypeUsingSubsetForInferenceCollection()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(t:Relation<(value:Integer,str:String,other:Boolean)>[1]):Relation<(value:Integer, str:String)>[1]\n" +
                        "{\n" +
                        "    $t->test(~[value, str]);\n" +
                        "}" +
                        "native function test<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];");
    }

    @Test
    public void testRelationTypeUsingSubsetForInferenceCollectionError()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "function f(t:Relation<(value:Integer,str:String,other:Boolean)>[1]):Relation<(value:Integer, str:String)>[1]\n" +
                            "{\n" +
                            "    $t->test(~[value, ster]);\n" +
                            "}" +
                            "native function test<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:14), \"The column 'ster' can't be found in the relation (value:Integer, str:String, other:Boolean)\"", e.getMessage());
        }
    }

    @Test
    public void testRelationTypeUsingSubsetForInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):Integer[1]\n" +
                        "{\n" +
                        "    $t->test(~value);\n" +
                        "}" +
                        "native function test<T,X>(x:Relation<X>[1], rel:ColSpec<(value:T)⊆X>[1]):T[1];");
    }

    @Test
    public void testRelationTypeUsingSubsetForInferenceErrorColName()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "function f(t:Relation<(value:Integer,str:String)>[1]):Integer[1]\n" +
                            "{\n" +
                            "    $t->test(~str);\n" +
                            "}" +
                            "native function test<T,X>(x:Relation<X>[1], rel:ColSpec<(e:T)⊆X>[1]):T[1];");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:14), \"(e:T) is not compatible with (str:String)\"", e.getMessage());
        }
    }

    @Test
    public void testRelationTypeUsingSubsetForInferenceErrorColCount()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "function f(t:Relation<(value:Integer,str:String)>[1]):Integer[1]\n" +
                            "{\n" +
                            "    $t->test(~str);\n" +
                            "}" +
                            "native function test<T,X>(x:Relation<X>[1], rel:ColSpec<(e:T,z:String)⊆X>[1]):T[1];");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:14), \"(e:T, z:String) is not compatible with (str:String)\"", e.getMessage());
        }
    }

    @Test
    public void testRelationTypeUsingSubsetAndAnonymousColumn()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):String[1]\n" +
                        "{\n" +
                        "    $t->test(~str);\n" +
                        "}" +
                        "native function test<T,X>(x:Relation<X>[1], rel:ColSpec<(?:T)⊆X>[1]):T[1];");

    }

    @Test
    public void testRelationTypeUsingSubsetAndAnonymousColumnAndAssignment()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class A<X,Y>{}\n" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):A<(str:String),String>[1]\n" +
                        "{\n" +
                        "    $t->test(~str);\n" +
                        "}" +
                        "native function test<T,X,Z>(x:Relation<X>[1], rel:ColSpec<Z=(?:T)⊆X>[1]):A<Z,T>[1];");

    }

    @Test
    public void testRelationPartialMatchError()
    {
        try
        {
            compileInferenceTest(
                    "import meta::pure::metamodel::relation::*;" +
                            "Class A<X,Y>{}\n" +
                            "function f(t:Relation<(value:Integer)>[1]):Boolean[1]\n" +
                            "{\n" +
                            "    $t->test();\n" +
                            "}" +
                            "native function test<T,X,Z>(x:Relation<(value:Integer, str:String)>[1]):Boolean[1];");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:9), \"The system can't find a match for the function: test(_:Relation<(value:Integer)>[1])\n" +
                    "\n" +
                    "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                    "\ttest(Relation<(value:Integer, str:String)>[1]):Boolean[1]\n" +
                    "\n" +
                    "No functions, in packages not imported, match the function name.\n" +
                    "\"", e.getMessage());
        }

    }

    @Test
    public void testRenameUseCase()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class A<X,Y>{}\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na);\n" +
                        "}" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];");

    }

    @Test
    public void testRenameUseCaseWithMap()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class A<X,Y>{}\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):String[*]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na)->map(x|$x.na);\n" +
                        "}" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];");

    }

    @Test
    public void testRenameUseCaseWithHardCodedType()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class A<X,Y>{}\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na:String);\n" +
                        "}" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];");

    }

    @Test
    public void testRenameWithIndirectCall()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class A<X,Y>{}" +
                        "Class TDS<T> extends Relation<T>{}\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na:String);\n" +
                        "}" +
                        "function f2(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->cast(@TDS<(value:Integer, name:String)>)->ren2(~name, ~na:String);\n" +
                        "}" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];" +
                        "function meta::pure::functions::relation::ren2<T,Z,K,V>(r:TDS<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1]" +
                        "{" +
                        "   ren($r->cast(@Relation<T>), $old, $new)" +
                        "}");
    }

    @Test
    public void testFunctionExpressionInferredTypes()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "Class meta::pure::functions::relation::SortInfo<T>\n" +
                        "{\n" +
                        "   column : ColSpec<T>[1];\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::relation::descending<T>(column:ColSpec<T>[1]):SortInfo<T>[1]\n" +
                        "{\n" +
                        "   ^SortInfo<T>(column=$column)\n" +
                        "}\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, name:String)>[1]\n" +
                        "{\n" +
                        "    $t->sort(descending(~value));\n" +
                        "}" +
                        "native function sort<X,T>(rel:Relation<T>[1], sortInfo:SortInfo<X⊆T>[*]):Relation<T>[1];\n");
    }

    @Test
    public void testDistinctError()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "native function meta::pure::functions::relation::distinct<X,T>(rel:Relation<T>[1], columns:ColSpecArray<X⊆T>[1]):Relation<T>[1];\n" +
                        "Class TDS<T> extends Relation<T>{}\n" +
                        "function meta::pure::functions::relation::distinct<X,T>(rel:TDS<T>[1], columns:ColSpecArray<X⊆T>[1]):Relation<T>[1]\n" +
                        "{\n" +
                        "    $rel->cast(@meta::pure::metamodel::relation::Relation<T>)->meta::pure::functions::relation::distinct($columns);\n" +
                        "}" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, name:String)>[1]\n" +
                        "{\n" +
                        "    $t->distinct(~[value]);\n" +
                        "}");
    }


    @Test
    public void testSort()
    {
        compileInferenceTest(
                "function <<test.Test>> eat::testFormatList():Boolean[1]\n" +
                        "{\n" +
                        "    assertEq('the quick brown fox jumps over the lazy [dog, [cat, mouse]]', format('the quick brown %s jumps over the lazy %s', ['fox', ^List<Any>(values=['dog', ^List<String>(values=['cat', 'mouse'])])]));\n" +
                        "}");
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
