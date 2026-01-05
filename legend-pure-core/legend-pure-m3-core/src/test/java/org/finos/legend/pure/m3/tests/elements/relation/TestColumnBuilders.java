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
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestColumnBuilders extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.compile();
    }

    @Test
    public void testSimpleColumnWithInferredType()
    {
        compileTestSource("fromString.pure",
                "function infFunc<T,X>(x:meta::pure::metamodel::relation::Relation<T>[1], c:meta::pure::metamodel::relation::ColSpec<X⊆T>[1]):meta::pure::metamodel::relation::Relation<X>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}\n" +
                        "function test():meta::pure::metamodel::relation::Relation<(colName:String)>[0..1]\n" +
                        "{\n" +
                        "   infFunc(\n" +
                        "               []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, colName:String)>)->toOne(),\n" +
                        "               ~colName\n" +
                        "           );\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithType()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpec<(name:String)>[1]\n" +
                        "{\n" +
                        "   ~name:String;\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeAndMultiplicity()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpec<(name:String)>[1]\n" +
                        "{\n" +
                        "   ~name:String[1];\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeArray()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpecArray<(name:String, id:Integer)>[1]\n" +
                        "{\n" +
                        "   ~[name:String, id:Integer];\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeFail()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpec<(name:Integer)>[1]\n" +
                        "{\n" +
                        "   ~name:String;\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "Return type error in function 'test'; found: meta::pure::metamodel::relation::ColSpec<(name:String)>; expected: meta::pure::metamodel::relation::ColSpec<(name:Integer)>",
                "fromString.pure", 3, 4, 3, 4, 3, 4,
                e);
    }

    @Test
    public void testSimpleColumnWithFunction()
    {
        compileTestSource("fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String[1])>[1]\n" +
                        "{\n" +
                        "   ~name:x|'ok';\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunction2Params()
    {
        compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{Relation<U>[1], _Window<U>[1], U[1]->Any[1]}, (name:String[1])>[1]\n" +
                        "{\n" +
                        "   ~name:{p,f,r|'ok'};\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionArray()
    {
        compileTestSource("fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpecArray<{Nil[1]->Any[*]}, (name:String[1], val:Integer[1])>[1]\n" +
                        "{\n" +
                        "   ~[name:x|'ok', val:x|1];\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionArray2Params()
    {
        compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test<U>():meta::pure::metamodel::relation::FuncColSpecArray<{Relation<U>[1], _Window<U>[1], U[1]->Any[*]}, (name:String, val:Integer)>[1]\n" +
                        "{\n" +
                        "   ~[name:{w,f,x|'ok'}, val:{w,f,x|1}];\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionArray2ParamsFail()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test<U>():meta::pure::metamodel::relation::FuncColSpecArray<{Relation<U>[1], _Window<U>[1], U[1]->Any[*]}, (name:String, val:Integer)>[1]\n" +
                        "{\n" +
                        "   ~[name:{p,f,r|'ok'}, val:{x|1}];\n" +
                        "}"));
        assertPureException(
                PureParserException.class,
                "All functions used in the col array should be of the same type.",
                "fromString.pure", 4, 4, 4, 4, 4, 34,
                e);
    }

    @Test
    public void testSimpleColumnWithFunctionFail()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]\n" +
                        "{\n" +
                        "   ~name:x|1;\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "Return type error in function 'test'; found: meta::pure::metamodel::relation::FuncColSpec<{U[1]->meta::pure::metamodel::type::Any[1]}, (name:Integer[1])>; expected: meta::pure::metamodel::relation::FuncColSpec<{U[1]->meta::pure::metamodel::type::Any[1]}, (name:String)>",
                "fromString.pure", 3, 4, 3, 4, 3, 4,
                e);
    }

    @Test
    public void testMixColumnsFail()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]\n" +
                        "{\n" +
                        "   ~[name:x|1, id:Integer];\n" +
                        "}"));
        assertPureException(
                PureParserException.class,
                "Can't mix column types",
                "fromString.pure", 3, 4, 3, 4, 3, 26,
                e);
    }

    @Test
    public void testColumnWithExtraReduceFunction()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "function test<U>():meta::pure::metamodel::relation::AggColSpec<{U[1]->Integer[0..1]}, {Integer[*]->Integer[0..1]}, (name:Integer[1])>[1]\n" +
                        "{\n" +
                        "   ~name: x|1 : y|$y->sum();\n" +
                        "}");
    }

    @Test
    public void testColumnWithExtraReduceFunction2MapParams()
    {
        compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function sum(i:Integer[*]):Integer[1];\n" +
                        "function test<U>():meta::pure::metamodel::relation::AggColSpec<{Relation<U>[1], _Window<U>[1], U[1]->Integer[0..1]}, {Integer[*]->Integer[0..1]}, (name:Integer[1])>[1]\n" +
                        "{\n" +
                        "   ~name: {p,f,r|1} : y|$y->sum();\n" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunction()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpec<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->toOne()->groupBy(~name: x|$x.ok : y|$y->sum());\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testColumnWithExtraReduceFunctionArray()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "function test<U>():meta::pure::metamodel::relation::AggColSpecArray<{Nil[1]->Any[0..1]}, {Nil[*]->Any[0..1]}, (name:Integer[1], newO:String[1])>[1]\n" +
                        "{\n" +
                        "   ~[name: x|1 : y|$y->sum(), newO: z|'a' : y|$y->joinStrings(',')]\n" +
                        "}");
    }

    @Test
    public void testColumnWithExtraReduceFunctionArray2MapParams()
    {
        compileTestSource("fromString.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function sum(i:Integer[*]):Integer[1];\n" +
                        "function test<U>():meta::pure::metamodel::relation::AggColSpecArray<{Relation<U>[1], _Window<U>[1], U[1]->Any[0..1]}, {Nil[*]->Any[0..1]}, (name:Integer, newO:String)>[1]\n" +
                        "{\n" +
                        "   ~[name: {p,f,r|1} : y|$y->sum(), newO: {p,f,r|'a'} : y|$y->joinStrings(',')]\n" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArray()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum()]);\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultiple()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->joinStrings(',')]);\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleChained()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->joinStrings(',')])->filter(x|$x.otherOne == 'boom');\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleChainedError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->joinStrings(',')])->filter(x|$x.otherXne == 'boom');\n" +
                        "   true;\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "The system can't find the column otherXne in the Relation (id:String, ok:Integer, name:Integer[1], otherOne:String[1])",
                "fromString.pure", 7, 190, 7, 190, 7, 197,
                e);
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.oke : y|$y->sum()]);\n" +
                        "   true;\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "The system can't find the column oke in the Relation (id:Integer, ok:Integer)",
                "fromString.pure", 6, 114, 6, 114, 6, 116,
                e);
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.icd : y|$y->joinStrings(',')]);\n" +
                        "   true;\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "The system can't find the column icd in the Relation (id:String, ok:Integer)",
                "fromString.pure", 6, 147, 6, 147, 6, 149,
                e);
    }


    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleAggError()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->sum()]);\n" +
                        "   true;\n" +
                        "}"));
        assertPureException(
                PureCompilationException.class,
                "The system can't find a match for the function: sum(_:String[*])\n" +
                        "\n" +
                        "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                        "\tsum(Integer[*]):Integer[1]\n" +
                        "\n" +
                        "No functions, in packages not imported, match the function name.\n",
                "fromString.pure", 6, 158, 6, 158, 6, 160,
                e);
    }


    @Test
    public void testGroupByNullableAggregation()
    {
        compileTestSource("fromString.pure",
                "native function max(i:Integer[*]):Integer[0..1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->max(), otherOne : x|$x.id : y|$y->joinStrings(',')]);\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testUnescapeSingleQuotes()
    {
        compileTestSource("fromString.pure",
                "native function max(i:Integer[*]):Integer[0..1];\n" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[0..1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, '\\'2000__|__newCol\\'':Integer)>)->toOne()->groupBy(~[name: x|$x.id : y|$y->max(), '\\'2000__|__newCol2\\'' : x|$x.'\\'2000__|__newCol\\'' : y|$y->max()])->extend(~newName:c|$c.'\\'2000__|__newCol2\\''->toOne());\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testColumnSimpleFunctionInference()
    {
        compileTestSource("fromString.pure",
                "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->extend(~name:c|$c.id->toOne());\n" +
                        "   true;\n" +
                        "}");
    }


    @Test
    public void testColumnUsingInOperator()
    {
        compileTestSource("fromString.pure",
                "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->extend(~name:c|$c.id->toOne());\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testExtendWithColumnArray()
    {
        compileTestSource("fromString.pure",
                "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpecArray<{T[1]->Any[*]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->extend(~[name:c|$c.id->toOne()]);\n" +
                        "   true;\n" +
                        "}");
    }


    @Test
    public void testUseExtendedPrimitiveTypes()
    {
        compileTestSource("fromString.pure",
                 "import test::*;\n" +
                         "import meta::pure::functions::meta::*;\n" +
                         "import meta::pure::metamodel::relation::*\n;" +
                        "\n" +
                         "Primitive test::Int8 extends Integer\n" +
                         "" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   @(x:Integer[1])->genericType().rawType->cast(@RelationType<Any>)->toOne()->addColumns(~[z:Int8[1]]);\n" +
                        "   true;\n" +
                        "}");
    }

    @Test
    public void testUseExtendedPrimitiveTypesWithTypeVariableValues()
    {
        compileTestSource("fromString.pure",
                "import test::*;\n" +
                        "import meta::pure::functions::meta::*;\n" +
                        "import meta::pure::metamodel::relation::*\n;" +
                        "\n" +
                        "Primitive meta::pure::metamodel::relation::Varchar(x:Integer[1]) extends String\n" +
                        "[\n" +
                        "  $this->length() <= $x\n" +
                        "]\n" +
                        "" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   @(x:Integer[1])->genericType().rawType->cast(@RelationType<Any>)->toOne()->addColumns(~[z:Varchar(200)[1]]);\n" +
                        "   true;\n" +
                        "}");
    }
}
