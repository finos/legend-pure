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

package org.finos.legend.pure.m3.inlinedsl.path.inference;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionTypeInferenceInPath extends AbstractPureTestWithCoreCompiled
{
    private static final String SOURCE_ID = "/test/pathInferenceTest.pure";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void inferTypeParameterUpAndDownWithNestedFunctionArray()
    {
        compileTestSource(SOURCE_ID,
                "import meta::pure::tests::model::simple::*;\n" +
                        "Class meta::pure::tests::model::simple::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class meta::pure::tests::model::simple::Trade\n" +
                        "{\n" +
                        "   quantity : Float[1];\n" +
                        "   product : Product[0..1];\n" +
                        "}\n" +
                        "Class meta::pure::tds::TabularDataSet\n" +
                        "{\n" +
                        "}\n" +
                        "Class meta::pure::functions::collection::AggregateValue<A,B,C>\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "   mapFn : FunctionDefinition<{A[1]->B[1]}>[1];\n" +
                        "   aggregateFn : FunctionDefinition<{B[*]->C[1]}>[1];\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::collection::agg<K,L,M>(name:String[1], mapFn:FunctionDefinition<{K[1]->L[1]}>[1], aggregateFn:FunctionDefinition<{L[*]->M[1]}>[1]):meta::pure::functions::collection::AggregateValue<K,L,M>[1]\n" +
                        "{\n" +
                        "   ^meta::pure::functions::collection::AggregateValue<K,L,M>(name = $name, mapFn=$mapFn, aggregateFn=$aggregateFn);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::collection::groupBy<T,V,U>(set:T[*], functions:Function<{T[1]->Any[*]}>[*], ids:String[*], aggValues:meta::pure::functions::collection::AggregateValue<T,V,U>[*]):TabularDataSet[1]\n" +
                        "{\n" +
                        "   fail('ee');\n" +
                        "   ^TabularDataSet();\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                        "{\n" +
                        "    $numbers->plus();\n" +
                        "}\n" +
                        "\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   Trade.all()->groupBy([#/Trade/product/name#],\n" +
                        "                        ['prodName'],\n" +
                        "                        [\n" +
                        "                           meta::pure::functions::collection::agg('cnt', x|$x.quantity, y|$y->sum()),\n" +
                        "                           meta::pure::functions::collection::agg('cnt2', x|$x.quantity, y|$y->sum())\n" +
                        "                        ]);\n" +
                        "}");
    }

    @Test
    public void inferTypeParameterUpAndDownWithNestedFunction_Success()
    {
        compileTestSource(SOURCE_ID, "import meta::pure::tests::model::simple::*;\n" +
                "Class meta::pure::tests::model::simple::Product\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class meta::pure::tests::model::simple::Trade\n" +
                "{\n" +
                "   quantity : Float[1];\n" +
                "   product : Product[0..1];\n" +
                "}\n" +
                "Class meta::pure::tds::TabularDataSet\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::collection::AggregateValue<A,B,C>\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   mapFn : FunctionDefinition<{A[1]->B[1]}>[1];\n" +
                "   aggregateFn : FunctionDefinition<{B[*]->C[1]}>[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::collection::agg<K,L,M>(name:String[1], mapFn:FunctionDefinition<{K[1]->L[1]}>[1], aggregateFn:FunctionDefinition<{L[*]->M[1]}>[1]):meta::pure::functions::collection::AggregateValue<K,L,M>[1]\n" +
                "{\n" +
                "   ^meta::pure::functions::collection::AggregateValue<K,L,M>(name = $name, mapFn=$mapFn, aggregateFn=$aggregateFn);\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::collection::groupBy<T,V,U>(set:T[*], functions:Function<{T[1]->Any[*]}>[*], ids:String[*], aggValues:meta::pure::functions::collection::AggregateValue<T,V,U>[*]):TabularDataSet[1]\n" +
                "{\n" +
                "   fail('ee');\n" +
                "   ^TabularDataSet();   \n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]\n" +
                "{\n" +
                "    $numbers->plus();\n" +
                "}\n" +
                "\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   Trade.all()->groupBy([#/Trade/product/name#], ['prodName'], meta::pure::functions::collection::agg('cnt', x|$x.quantity, y|$y->sum()));\n" +
                "   'ok';\n" +
                "}");
    }

    @Test
    public void inferTypeParameterUpAndDownWithNestedFunction_Failure()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () ->
                compileTestSource(SOURCE_ID, "import meta::pure::tests::model::simple::*;\n" +
                        "Class meta::pure::tests::model::simple::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class meta::pure::tests::model::simple::Trade\n" +
                        "{\n" +
                        "   quantity : Float[1];\n" +
                        "   product : Product[0..1];\n" +
                        "}\n" +
                        "Class meta::pure::tds::TabularDataSet\n" +
                        "{\n" +
                        "}\n" +
                        "Class meta::pure::functions::collection::AggregateValue<A,B,C>\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "   mapFn : FunctionDefinition<{A[1]->B[1]}>[1];\n" +
                        "   aggregateFn : FunctionDefinition<{B[*]->C[1]}>[1];\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::collection::agg<K,L,M>(name:String[1], mapFn:FunctionDefinition<{K[1]->L[1]}>[1], aggregateFn:FunctionDefinition<{L[*]->M[1]}>[1]):meta::pure::functions::collection::AggregateValue<K,L,M>[1]\n" +
                        "{\n" +
                        "   ^meta::pure::functions::collection::AggregateValue<K,L,M>(name = $name, mapFn=$mapFn, aggregateFn=$aggregateFn);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::collection::groupBy<T,V,U>(set:T[*], functions:Function<{T[1]->Any[*]}>[*], ids:String[*], aggValues:meta::pure::functions::collection::AggregateValue<T,V,U>[*]):TabularDataSet[1]\n" +
                        "{\n" +
                        "   fail('ee');\n" +
                        "   ^TabularDataSet();   \n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   Trade.all()->groupBy([#/Trade/product/name#], ['prodName'], meta::pure::functions::collection::agg('cnt', x|$x.name, y|$y->sum()));\n" +
                        "   'ok';\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Can't find the property 'name' in the class meta::pure::tests::model::simple::Trade", SOURCE_ID, 36, 115, e);
    }

    @Test
    public void inferTheTypeOfACollectionOfLambdaAndPath_Success()
    {
        compileTestSource(SOURCE_ID,
                        "Class Person\n" +
                        "{\n" +
                        "   age:Integer[1];\n" +
                        "}\n" +
                        "function a(k:FunctionExpression[1]):Any[*]\n" +
                        "{\n" +
                        "   let f = [f:Person[1]|2, #/Person/age#];\n" +
                        "   let z = $f->at(0)->eval(^Person(age=3))+4;\n" +
                        "}\n");
    }

    @Test
    public void inferTheTypeOfACollectionOfLambdaAndPath_Failure()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () ->
                compileTestSource(SOURCE_ID,
                        "Class Person\n" +
                                "{\n" +
                                "   age:Integer[1];\n" +
                                "}\n" +
                                "function a(k:FunctionExpression[1]):Any[*]\n" +
                                "{\n" +
                                "   let f = [f:Person[1]|'2', #/Person/age#];\n" +
                                "   let z = $f->at(0)->eval(^Person(age=3))+4;\n" +
                                "}\n"));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "plus(_:Any[2])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::math::plus(Decimal[*]):Decimal[1]\n" +
                "\tmeta::pure::functions::math::plus(Float[*]):Float[1]\n" +
                "\tmeta::pure::functions::math::plus(Integer[*]):Integer[1]\n" +
                "\tmeta::pure::functions::math::plus(Number[*]):Number[1]\n" +
                "\tmeta::pure::functions::string::plus(String[*]):String[1]\n", SOURCE_ID, 8, 43, e);
    }
}
