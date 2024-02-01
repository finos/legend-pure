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
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

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
    }

    @Test
    public void testSimpleColumnWithInferredType()
    {
        compileTestSource("fromString.pure",
                "" +
                        "function infFunc<T,X>(x:meta::pure::metamodel::relation::Relation<T>[1], c:meta::pure::metamodel::relation::ColSpec<X⊆T>[1]):meta::pure::metamodel::relation::Relation<X>[0..1]" +
                        "{" +
                        "   [];" +
                        "}" +
                        "function test():meta::pure::metamodel::relation::Relation<(colName:String)>[0..1]" +
                        "{" +
                        "   infFunc(" +
                        "               []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, colName:String)>)->toOne()," +
                        "               ~colName" +
                        "           );" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithType()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpec<(name:String)>[1]" +
                        "{" +
                        "   ~name:String;" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeArray()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::ColSpecArray<(name:String, id:Integer)>[1]" +
                        "{" +
                        "   ~[name:String, id:Integer];" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeFail()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test():meta::pure::metamodel::relation::ColSpec<(name:Integer)>[1]" +
                            "{" +
                            "   ~name:String;" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:1 column:80), \"Return type error in function 'test'; found: meta::pure::metamodel::relation::ColSpec<(name:String)>; expected: meta::pure::metamodel::relation::ColSpec<(name:Integer)>\"", e.getMessage());
        }
    }

    @Test
    public void testSimpleColumnWithFunction()
    {
        compileTestSource("fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]" +
                        "{" +
                        "   ~name:x|'ok';" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionArray()
    {
        compileTestSource("fromString.pure",
                "function test<U>():meta::pure::metamodel::relation::FuncColSpecArray<{Nil[1]->Any[*]}, (name:String, val:Integer)>[1]" +
                        "{" +
                        "   ~[name:x|'ok', val:x|1];" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithFunctionFail()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]" +
                            "{" +
                            "   ~name:x|1;" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:1 column:102), \"Return type error in function 'test'; found: meta::pure::metamodel::relation::FuncColSpec<{U[1]->meta::pure::metamodel::type::Any[1]}, (name:Integer)>; expected: meta::pure::metamodel::relation::FuncColSpec<{U[1]->meta::pure::metamodel::type::Any[1]}, (name:String)>\"", e.getMessage());
        }
    }

    @Test
    public void testMixColumnsFail()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]" +
                            "{" +
                            "   ~[name:x|1, id:Integer];" +
                            "}");
            fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Parser error at (resource:fromString.pure line:-2), (Compilation error at ??, \"Can't mix column types\") in\n" +
                    "'\n" +
                    "function test<U>():meta::pure::metamodel::relation::FuncColSpec<{U[1]->Any[1]}, (name:String)>[1]{   ~[name:x|1, id:Integer];}'", e.getMessage());
        }
    }

    @Test
    public void testColumnWithExtraReduceFunction()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];" +
                        "function test<U>():meta::pure::metamodel::relation::AggColSpec<{U[1]->Integer[0..1]}, {Integer[*]->Integer[1]}, (name:Integer)>[1]" +
                        "{" +
                        "   ~name: x|1 : y|$y->sum();" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunction()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpec<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->toOne()->groupBy(~name: x|$x.ok : y|$y->sum());" +
                        "   true;" +
                        "}");
    }

    @Test
    public void testColumnWithExtraReduceFunctionArray()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];" +
                        "function test<U>():meta::pure::metamodel::relation::AggColSpecArray<{Nil[1]->Any[0..1]}, {Nil[*]->Any[1]}, (name:Integer, newO:String)>[1]" +
                        "{" +
                        "   ~[name: x|1 : y|$y->sum(), newO: z|'a' : y|$y->joinStrings(',')]" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArray()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum()]);" +
                        "   true;" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultiple()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->joinStrings(',')]);" +
                        "   true;" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleChained()
    {
        compileTestSource("fromString.pure",
                "native function sum(i:Integer[*]):Integer[1];" +
                        "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->joinStrings(',')])->filter(x|$x.otherOne == 'boom');" +
                        "   true;" +
                        "}");
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleChainedError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "native function sum(i:Integer[*]):Integer[1];" +
                            "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                            "native function meta::pure::functions::relation::filter<T>(rel:meta::pure::metamodel::relation::Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):meta::pure::metamodel::relation::Relation<T>[1];\n" +
                            "\n" +
                            "" +
                            "function test():Boolean[1]" +
                            "{" +
                            "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->joinStrings(',')])->filter(x|$x.otherXne == 'boom');" +
                            "   true;" +
                            "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:4 column:217), \"The system can't find the column otherXne in the Relation (id:String, ok:Integer, name:Integer, otherOne:String)\"", e.getMessage());
        }
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "native function sum(i:Integer[*]):Integer[1];" +
                            "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                            "\n" +
                            "" +
                            "function test():Boolean[1]" +
                            "{" +
                            "   []->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.oke : y|$y->sum()]);" +
                            "   true;" +
                            "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:3 column:141), \"The system can't find the column oke in the Relation (id:Integer, ok:Integer)\"", e.getMessage());
        }
    }

    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "native function sum(i:Integer[*]):Integer[1];" +
                            "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                            "\n" +
                            "" +
                            "function test():Boolean[1]" +
                            "{" +
                            "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.icd : y|$y->joinStrings(',')]);" +
                            "   true;" +
                            "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:3 column:174), \"The system can't find the column icd in the Relation (id:String, ok:Integer)\"", e.getMessage());
        }
    }


    @Test
    public void testColumnFunctionInferenceWithExtraReduceFunctionArrayMultipleAggError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "native function sum(i:Integer[*]):Integer[1];" +
                            "native function meta::pure::functions::relation::groupBy<U,T,K,R>(r:meta::pure::metamodel::relation::Relation<U>[1], agg:meta::pure::metamodel::relation::AggColSpecArray<{U[1]->T[0..1]},{T[*]->K[1]}, R>[1]):meta::pure::metamodel::relation::Relation<U+R>[1];\n" +
                            "\n" +
                            "" +
                            "function test():Boolean[1]" +
                            "{" +
                            "   []->cast(@meta::pure::metamodel::relation::Relation<(id:String, ok:Integer)>)->toOne()->groupBy(~[name: x|$x.ok : y|$y->sum(), otherOne : x|$x.id : y|$y->sum()]);" +
                            "   true;" +
                            "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:fromString.pure line:3 column:185), \"The system can't find a match for the function: sum(_:String[*])\n" +
                    "\n" +
                    "These functions, in packages already imported, would match the function call if you changed the parameters.\n" +
                    "\tsum(Integer[*]):Integer[1]\n" +
                    "\n" +
                    "No functions, in packages not imported, match the function name.\n" +
                    "\"", e.getMessage());
        }
    }


    @Test
    public void testColumnSimpleFunctionInference()
    {
        compileTestSource("fromString.pure",
                "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->extend(~name:c|$c.id->toOne());" +
                        "   true;" +
                        "}");
    }


    @Test
    public void testColumnUsingInOperator()
    {
        compileTestSource("fromString.pure",
                "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpec<{T[1]->Any[0..1]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->extend(~name:c|$c.id->toOne());" +
                        "   true;" +
                        "}");
    }


    @Test
    public void testExtendWithColumnArray()
    {
        compileTestSource("fromString.pure",
                "native function meta::pure::functions::relation::extend<T,Z>(r:meta::pure::metamodel::relation::Relation<T>[1], f:meta::pure::metamodel::relation::FuncColSpecArray<{T[1]->Any[*]},Z>[1]):meta::pure::metamodel::relation::Relation<T+Z>[1];\n" +
                        "\n" +
                        "" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   []->toOne()->cast(@meta::pure::metamodel::relation::Relation<(id:Integer, ok:Integer)>)->extend(~[name:c|$c.id->toOne()]);" +
                        "   true;" +
                        "}");
    }

}
