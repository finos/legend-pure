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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function f():Relation<(legal:String)>[1]\n" +
                        "{\n" +
                        "   Firm.all()->project(~legal:x|$x.legalName);\n" +
                        "}\n" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpec<{Z[1]->Any[*]},T>[1]):Relation<T>[1];"
        );
    }

    @Test
    public void testColumnFunctionCollectionInference()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Primitive x::Numeric(x:Integer[1], y:Integer[1]) extends Integer\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "   numeric:x::Numeric(1, 2)[1];\n" +
                        "}\n" +
                        "\n" +
                        "function f():Relation<(legal:String, numeric:x::Numeric(1, 2))>[1]\n" +
                        "{\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName, numeric:x|$x.numeric]);\n" +
                        "}\n" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];"
        );
    }

    @Test
    public void testRespectPotentialEmptyRow()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(x:Relation<(legal:String[1], other:Integer[0..1])>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $x->extend(over(), ~new:{p,w,r|$p->lag($r).legal + 'w'});\n" +
                        "   true;\n" +
                        "}\n" +
                        "\n" +
                        "native function over<T>():_Window<T>[1];" +
                        "native function extend<T,Z,W,R>(r:Relation<T>[1], window:_Window<T>[1], f:FuncColSpec<{Relation<T>[1],_Window<T>[1],T[1]->Any[0..1]},R>[1]):Relation<T+R>[1];" +
                        "native function lag<T>(w:Relation<T>[1],r:T[1]):T[0..1];"
        ));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:47), \"Required multiplicity: 1, found: 0..1\"", e.getMessage());
    }

    @Test
    public void testRespectPotentialEmptyRowWorking()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(x:Relation<(legal:String[1], other:Integer[0..1])>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $x->extend(over(), ~new:{p,w,r|$p->lag($r).legal + 'w'});\n" +
                        "   true;\n" +
                        "}\n" +
                        "\n" +
                        "native function over<T>():_Window<T>[1];" +
                        "native function extend<T,Z,W,R>(r:Relation<T>[1], window:_Window<T>[1], f:FuncColSpec<{Relation<T>[1],_Window<T>[1],T[1]->Any[0..1]},R>[1]):Relation<T+R>[1];" +
                        "native function lag<T>(w:Relation<T>[1],r:T[1]):T[1];"
        );
    }

    @Test
    public void testColumnMultiplicityPropagationWithSelector()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(p:Relation<(legal:String[1], other:Integer[0..1])>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $p->select(~[legal, other])->map(x|$x.legal + 'a');\n" +
                        "   true;\n" +
                        "}\n" +
                        "\n" +
                        "native function select<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];"
        );
    }

    @Test
    public void testColumnMultiplicityPropagationWithSelectorWithError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(p:Relation<(legal:String[1], other:Integer[0..1])>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $p->select(~[legal,other])->map(x|$x.other + 1);\n" +
                        "   true;\n" +
                        "}\n" +
                        "\n" +
                        "native function select<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:41), \"Required multiplicity: 1, found: 0..1\"", e.getMessage());
    }

    @Test
    public void testColumnMultiplicityPropagationWithSelectorWithErrorWithDefault()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(p:Relation<(legal:String[1], other:Integer)>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $p->select(~[legal,other])->map(x|$x.other + 1);\n" +
                        "   true;\n" +
                        "}\n" +
                        "\n" +
                        "native function select<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:41), \"Required multiplicity: 1, found: 0..1\"", e.getMessage());
    }

    @Test
    public void testColumnFunctionCollectionInference2()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Primitive x::Numeric(x:Integer[1], y:Integer[1]) extends Integer\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "   numeric:x::Numeric(1, 2)[1];\n" +
                        "}\n" +
                        "\n" +
                        "function f():Relation<(numeric:x::Numeric(1, 2))>[1]\n" +
                        "{\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName, numeric:x|$x.numeric])->groupBy(~[legal], ~[sum:x|$x.numeric : y|$y->sum()])->project(~[numeric:x|$x.sum]);\n" +
                        "}\n" +
                        "\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];\n" +
                        "native function groupBy<T,Z,K,V,R>(r:Relation<T>[1], cols:ColSpecArray<Z⊆T>[1], agg:AggColSpecArray<{T[1]->K[0..1]},{K[*]->V[0..1]}, R>[1]):Relation<Z+R>[1];\n" +
                        "native function sum(values:Integer[*]):x::Numeric(1, 2)[1];\n" +
                        "native function project<T,Z>(r:Relation<T>[1], fs:FuncColSpecArray<{T[1]->Any[*]},Z>[1]):Relation<Z>[1];"
        );
    }

    @Ignore
    @Test
    public void testColumnFunctionCollectionInference3()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function f():Boolean[1]\n" +
                        "{\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->wrapper(~[legal]);\n" +
                        //if you remove the wrapper function, ths below does compile which is functionally identical
//                        "   Firm.all()->project(~[legal:x|$x.legalName])->groupBy(~[legal], ~[size: x | $x : y | $y->size()])->filter(zz | $zz.size > 1)->size() == 0;\n" +
                        // "Can't find the property 'size' in the class meta::pure::metamodel::relation::Relation"
                        "}\n" +
                        "function wrapper<Q,U>(r: Relation<Q>[1], cols:ColSpecArray<U⊆Q>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $r->groupBy($r->groupBy($cols, ~[size : x | $x : y | $y->size()])->filter(zz | $zz.size > 1)->size() == 0)->size() == 0\n" +
                        "}\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n" +
                        "native function groupBy<T,Z,K,V,R>(r:Relation<T>[1], cols:ColSpecArray<Z⊆T>[1], agg:AggColSpecArray<{T[1]->K[0..1]},{K[*]->V[0..1]}, R>[1]):Relation<Z+R>[1];" +
                        "\n" +
                        "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];" +
                        "\n"
        );
    }

    @Ignore
    @Test
    public void testColumnFunctionCollectionInference4() // Stackoverflow
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function f():Boolean[1]\n" +
                        "{\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->wrapper(~[legal]);\n" +
                        "}\n" +
                        "function wrapper<T,Z>(r: Relation<T>[1], cols:ColSpecArray<Z⊆T>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   $r->groupBy($r->groupBy($cols, ~[size : x | $x : y | $y->size()])->filter(zz | $zz.size > 1)->size() == 0)->size() == 0\n" +
                        "}\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];" +
                        "\n" +
                        "native function groupBy<T,Z,K,V,R>(r:Relation<T>[1], cols:ColSpecArray<Z⊆T>[1], agg:AggColSpecArray<{T[1]->K[0..1]},{K[*]->V[0..1]}, R>[1]):Relation<Z+R>[1];" +
                        "\n" +
                        "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];" +
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "}\n" +
                        "function f():Any[*]\n" +
                        "{\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName])->join(Firm.all()->project(~[legal:x|$x.legalName]))\n" +
                        "}\n" +
                        "\n" +
                        "native function join<Z,T>(x:Relation<Z>[1], k:Relation<T>[1]):Relation<T+Z+Z>[1];\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];\n"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:8 column:50), \"The relation contains duplicates: [legal]\"", e.getMessage());
    }

    @Test
    public void testTypeParameterDifferenceForRelationType()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName:String[1];\n" +
                        "}\n" +
                        "function f():Any[*]\n" +
                        "{\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->map(x|$x.legal3);\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->diff(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal);\n" +
                        "   Firm.all()->project(~[legal:x|$x.legalName, legal3:x|$x.legalName])->diff(Firm.all()->project(~[legal3:x|$x.legalName]))->map(x|$x.legal3);\n" +
                        "}\n" +
                        "\n" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];\n" +
                        "native function diff<Z,T>(x:Relation<T>[1], k:Relation<Z>[1]):Relation<T-Z>[1];\n" +
                        "native function project<Z,T>(cl:Z[*], x:FuncColSpecArray<{Z[1]->Any[*]},T>[1]):Relation<T>[1];\n"
        ));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:10 column:135), \"The system can't find the column legal3 in the Relation (legal:String[1])\"", e.getMessage());
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f<T>(r:Relation<T>[1]):Any[*]\n" +
                        "{\n" +
                        "   $r->cast(@Relation<(a:Integer, f:String)>)->map(c|$c.z);\n" +
                        "}" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:57), \"The system can't find the column z in the Relation (a:Integer, f:String)\"", e.getMessage());
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f<T>(r:Relation<(a:Number)>[1]):Integer[*]\n" +
                        "{\n" +
                        "   $r->tt()->map(x|$x.a);\n" +
                        "}\n" +
                        "native function tt(rel:Relation<(a:Integer)>[1]):Relation<(a:Integer)>[1];\n" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];\n"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:8), \"The system can't find a match for the function: tt(_:Relation<(a:Number)>[1])\n" +
                "\n" +
                "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                "\ttt(Relation<(a:Integer)>[1]):Relation<(a:Integer)>[1]\n" +
                "\n" +
                "No functions, in packages not imported, match the function name.\n" +
                "\"", e.getMessage());
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(r:Relation<(col1:String, col2:Integer)>[1]):Relation<(col3:String)>[1]\n" +
                        "{\n" +
                        "  $r->filter(x|$x.col2 > 1);\n" +
                        "}\n" +
                        "native function filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:7), \"Return type error in function 'f'; found: meta::pure::metamodel::relation::Relation<(col1:String, col2:Integer)>; expected: meta::pure::metamodel::relation::Relation<(col3:String)>\"", e.getMessage()
        );
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(t:Relation<(value:Integer,str:String,other:Boolean)>[1]):Relation<(value:Integer, str:String)>[1]\n" +
                        "{\n" +
                        "    $t->test(~[value, ster]);\n" +
                        "}\n" +
                        "native function test<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:14), \"The column 'ster' can't be found in the relation (value:Integer, str:String, other:Boolean)\"", e.getMessage());
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):Integer[1]\n" +
                        "{\n" +
                        "    $t->test(~str);\n" +
                        "}" +
                        "native function test<T,X>(x:Relation<X>[1], rel:ColSpec<(e:T)⊆X>[1]):T[1];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:3 column:14), \"(e:T) is not compatible with (str:String)\"", e.getMessage());
    }

    @Test
    public void testRelationTypeUsingSubsetForInferenceErrorColCount()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):Integer[1]\n" +
                        "{\n" +
                        "    $t->test(~str);\n" +
                        "}\n" +
                        "native function test<T,X>(x:Relation<X>[1], rel:ColSpec<(e:T,z:String)⊆X>[1]):T[1];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:14), \"(e:T, z:String) is not compatible with (str:String)\"", e.getMessage());
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
    public void testRelationTypeUsingUnknwonOneColumn()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):String[1]\n" +
                        "{\n" +
                        "    $t->test(~str->ascending());\n" +
                        "}" +
                        "native function test<X>(x:Relation<X>[1], rel:SortInfo<(?:?)⊆X>[1]):String[1];" +
                        "\n" +
                        "function <<functionType.NormalizeRequiredFunction>> meta::pure::functions::relation::ascending<SW> (column:ColSpec<SW>[1]):SortInfo<SW>[1]\n" +
                        "{\n" +
                        "   ^SortInfo<SW>(column=$column, direction=SortType.ASC)\n" +
                        "}"
        );
    }

    @Test
    public void testRelationTypeUsingUnknwonOneColumnError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(t:Relation<(value:Integer,str:String)>[1]):String[1]\n" +
                        "{\n" +
                        "    $t->test(~strx->ascending());\n" +
                        "}\n" +
                        "native function test<X>(x:Relation<X>[1], rel:SortInfo<(?:?)⊆X>[1]):String[1];\n" +
                        "function <<functionType.NormalizeRequiredFunction>> meta::pure::functions::relation::ascending<SW> (column:ColSpec<SW>[1]):SortInfo<SW>[1]\n" +
                        "{\n" +
                        "   ^SortInfo<T>(column=$column, direction=SortType.ASC)\n" +
                        "}"
        ));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:4 column:14), \"The column 'strx' can't be found in the relation (value:Integer, str:String)\"", e.getMessage());
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
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class A<X,Y>\n" +
                        "{\n" +
                        "}\n" +
                        "function f(t:Relation<(value:Integer)>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    $t->test();\n" +
                        "}\n" +
                        "native function test<T,X,Z>(x:Relation<(value:Integer, str:String)>[1]):Boolean[1];"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:7 column:9), \"The system can't find a match for the function: test(_:Relation<(value:Integer)>[1])\n" +
                "\n" +
                "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                "\ttest(Relation<(value:Integer, str:String)>[1]):Boolean[1]\n" +
                "\n" +
                "No functions, in packages not imported, match the function name.\n" +
                "\"", e.getMessage());
    }

    @Test
    public void testRenameUseCase()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na);\n" +
                        "}\n" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];");
    }

    @Test
    public void testRenameUseCaseWithMap()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):String[*]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na)->map(x|$x.na);\n" +
                        "}\n" +
                        "native function map<T,V>(rel:Relation<T>[1], f:Function<{T[1]->V[*]}>[1]):V[*];\n" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];");
    }

    @Test
    public void testRenameUseCaseWithHardCodedType()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na:String);\n" +
                        "}\n" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];");
    }

    @Test
    public void testRenameWithIndirectCall()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "Class TDS<T> extends Relation<T>\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->ren(~name, ~na:String);\n" +
                        "}\n" +
                        "\n" +
                        "function f2(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, na:String)>[1]\n" +
                        "{\n" +
                        "    $t->cast(@TDS<(value:Integer, name:String)>)->ren2(~name, ~na:String);\n" +
                        "}\n" +
                        "\n" +
                        "native function meta::pure::functions::relation::ren<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];\n" +
                        "\n" +
                        "function meta::pure::functions::relation::ren2<T,Z,K,V>(r:TDS<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1]\n" +
                        "{\n" +
                        "   ren($r->cast(@Relation<T>), $old, $new)\n" +
                        "}\n");
    }

    @Test
    public void testFunctionExpressionInferredTypes()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "function meta::pure::functions::relation::descending<T>(column:ColSpec<T>[1]):SortInfo<T>[1]\n" +
                        "{\n" +
                        "   ^SortInfo<T>(column=$column, direction=SortType.DESC)\n" +
                        "}\n" +
                        "function f(t:Relation<(value:Integer, name:String)>[1]):Relation<(value:Integer, name:String)>[1]\n" +
                        "{\n" +
                        "    $t->sort(descending(~value));\n" +
                        "}" +
                        "native function sort<X,T>(rel:Relation<T>[1], sortInfo:SortInfo<X⊆T>[*]):Relation<T>[1];\n");
    }

    @Test
    public void testFunctionExpressionSortAfterExtend()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;" +
                        "import meta::pure::metamodel::variant::*;" +
                        "function meta::pure::functions::relation::descending<T>(column:ColSpec<T>[1]):SortInfo<T>[1]\n" +
                        "{\n" +
                        "   ^SortInfo<T>(column=$column, direction=SortType.DESC)\n" +
                        "}\n" +
                        "function f(t:Relation<(value:Integer, name:Variant)>[1]):Relation<Any>[1]\n" +
                        "{\n" +
                        "    $t->extend(~ok:x|$x.name)->sort(descending(~ok));\n" +
                        "}" +
                        "native function sort<X,T>(rel:Relation<T>[1], sortInfo:SortInfo<X⊆T>[*]):Relation<T>[1];\n" +
                        "native function extend<T,Z>(r:Relation<T>[1], f:FuncColSpec<{T[1]->Any[0..1]},Z>[1]):Relation<T+Z>[1];\n" +
                        "native function get(variant: Variant[0..1], key: String[1]): Variant[0..1];");
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

    @Test
    public void testColumnError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function test<T,W>(r:Relation<T>[1], cols:ColSpec<W⊆T>[1]):Relation<T>[1];\n" +
                        "function x<T>(r:Relation<T>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    |$r->cast(@Relation<(a:Integer, f:String)>)->test(~id);\n" +
                        "}"));
        Assert.assertEquals("Compilation error at (resource:inferenceTest.pure line:5 column:55), \"The column 'id' can't be found in the relation (a:Integer, f:String)\"", e.getMessage());
    }

    @Test
    public void testSingleColumn()
    {
        compileInferenceTest(
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function test<T>(val:T[1], rel:Relation<(?:T)>[1]):Boolean[1];\n" +
                        "function x<T>(r:Relation<T>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    let x = {|1->test($r->cast(@Relation<(a:Integer)>))};" +
                        "    true;\n" +
                        "}"
        );
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

    @Test
    @Ignore
    // Test fails due to bug in type inference, potentially due to incorrect unbind. The type (RelationType) for the 
    // only expression in the function f() is correctly inferred after first compile. However, the type parameters 
    // remain unresolved post unbind and re-compile.
    public void testRelationTypeInferenceIntegrityWithSelect()
    {
        String functionSource = "import meta::pure::metamodel::relation::*;" +
                "function f(t:Relation<(value:Integer,str:String)>[1]):Relation<Any>[1]\n" +
                "{\n" +
                "    $t->select(~[value, str])\n" +
                "}";
        String nativeFunctionSource = "import meta::pure::metamodel::relation::*;" +
                "native function select<T,X>(x:Relation<X>[1], rel:ColSpecArray<T⊆X>[1]):Relation<T>[1];";

        ImmutableMap<String, String> sources = Maps.immutable.of("1.pure", functionSource, "2.pure", nativeFunctionSource);

        new RuntimeTestScriptBuilder().createInMemorySources(sources).compile().run(runtime, functionExecution);

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime,
                functionExecution,
                Lists.fixedSize.of(Tuples.pair("2.pure", nativeFunctionSource)),
                this.getAdditionalVerifiers()
        );
    }

    @Test
    @Ignore
    // Test fails due to bug in type inference, potentially due to incorrect unbind. The column type for the renamed 
    // column in the RelationType returned by the function is set to null post unbind and re-compile.
    public void testRelationTypeInferenceIntegrityWithRename()
    {
        String functionSource = "import meta::pure::metamodel::relation::*;" +
                "function f(t:Relation<(value:Integer,str:String)>[1]):Relation<Any>[1]\n" +
                "{\n" +
                "    $t->rename(~value, ~newValue)\n" +
                "}";
        String nativeFunctionSource = "import meta::pure::metamodel::relation::*;" +
                "native function rename<T,Z,K,V>(r:Relation<T>[1], old:ColSpec<Z=(?:K)⊆T>[1], new:ColSpec<V=(?:K)>[1]):Relation<T-Z+V>[1];";

        ImmutableMap<String, String> sources = Maps.immutable.of("1.pure", functionSource, "2.pure", nativeFunctionSource);

        new RuntimeTestScriptBuilder().createInMemorySources(sources).compile().run(runtime, functionExecution);

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime,
                functionExecution,
                Lists.fixedSize.of(Tuples.pair("2.pure", nativeFunctionSource)),
                this.getAdditionalVerifiers()
        );
    }
}
