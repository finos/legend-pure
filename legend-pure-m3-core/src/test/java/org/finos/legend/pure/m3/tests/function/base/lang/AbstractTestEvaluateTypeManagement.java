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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Test;

public abstract class AbstractTestEvaluateTypeManagement extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testTypeManagementForEvaluate() throws Exception
    {
        compileTestSource("inferenceTest.pure", "Class Result<S|v>{}" +
                "function alloyExecute<T|m>(f:FunctionDefinition<{->T[m]}>[1], host:String[1], port:Integer[1]):Result<T|m>[1]" +
                "{" +
                "   print('ok',1);" +
                "   ^Result<T|m>();" +
                "}\n" +
                "function meta::alloy::test::mayExecuteAlloyTest<X|k>(f1:meta::pure::metamodel::function::Function<{String[1], String[1], String[1], Integer[1]->X[k]}>[1],\n" +
                "                                                            f2:meta::pure::metamodel::function::Function<{->X[k]}>[1]):X[k]" +
                "{" +
                "   $f2->eval();" +
                "}" +
                "function meta::pure::functions::collection::list<U>(vals:U[*]):List<U>[1]\n" +
                "{\n" +
                "   ^List<U>(values = $vals);\n" +
                "}" +
                "function meta::pure::router::execute<R|y>(f:FunctionDefinition<{->R[y]}>[1]):Result<R|y>[0..1]\n" +
                "{\n" +
                "   meta::alloy::test::mayExecuteAlloyTest(\n" +
                "      {v, vv, host, port | let zz = 'alloyExecute_FunctionDefinition_1__String_1__Integer_1__Result_1_'->pathToElement()->cast(@FunctionDefinition<{->Any[*]}>);" +
                "                    $zz->evaluate([$f, $host, $port]->map(z|list($z)))->toOne()->cast(@Result<R|y>);},\n" +
                "      {| let zz = 'alloyExecute_FunctionDefinition_1__String_1__Integer_1__Result_1_'->pathToElement()->cast(@FunctionDefinition<Any>);" +
                "                    $zz->evaluate([$f, 'ww', 200]->map(z|list($z)))->toOne()->cast(@Result<R|y>);}\n" +
                "   );\n" +
                "}" +
                "Class A{}" +
                "function go():Any[*]\n" +
                "{\n" +
                "     execute(|A.all());\n" +
                "}\n" +
                "");
        this.compileAndExecute("go():Any[*]");
    }

    @Test
    public void testTypeManagementForEvaluateProperty() throws Exception
    {
        compileTestSource("inferenceTest.pure",
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let pair = pair('bla', 2);\n" +
                        "   let evalProp = {p:Property<Nil,Any|*>[1], k:Any[1]|let values = $p->eval($k);};\n" +
                        "   let p = Pair.properties->cast(@Property<Nil,Any|*>)->filter(p|$p.name=='first')->toOne();" +
                        "   $evalProp->evaluate([list($p), list($pair)]);\n" +
                        "}\n" +
                        "function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1]\n" +
                        "{\n" +
                        "   ^Pair<U,V>(first=$first, second=$second);\n" +
                        "}" +
                        "function meta::pure::functions::collection::list<U>(vals:U[*]):List<U>[1]\n" +
                        "{\n" +
                        "   ^List<U>(values = $vals);\n" +
                        "}");
        this.compileAndExecute("go():Any[*]");
    }

    @Test
    public void testTypeManagementForList() throws Exception
    {
        compileTestSource("inferenceTest.pure",
                "function go():Any[*] {\n" +
                        "        list('ok');\n" +
                        "}\n" +
                        "function meta::pure::functions::collection::list<U>(vals:U[*]):List<U>[1]\n" +
                        "{\n" +
                        "   ^List<U>(values = $vals);\n" +
                        "}" +
                        "");
        this.compileAndExecute("go():Any[*]");
    }

    @Test
    public void testTypeManagementForEvaluateMul() throws Exception
    {
        compileTestSource("inferenceTest.pure", "Class Result<S|v>{}" +
                "function alloyExecute<T|m>(f:FunctionDefinition<{->T[m]}>[1], host:String[1], port:Integer[1]):Result<T|m>[1]" +
                "{" +
                "   print('ok',1);" +
                "   ^Result<T|m>();" +
                "}" +
                "function meta::alloy::test::mayExecuteAlloyTest<X|k>(f1:meta::pure::metamodel::function::Function<{String[1], String[1], String[1], Integer[1]->X[k]}>[1],\n" +
                "                                                            f2:meta::pure::metamodel::function::Function<{->X[k]}>[1]):X[k]" +
                "{" +
                "   $f2->eval();" +
                "}" +
                "function meta::pure::functions::collection::list<U>(vals:U[*]):List<U>[1]\n" +
                "{\n" +
                "   ^List<U>(values = $vals);\n" +
                "}" +
                "function meta::pure::router::execute<R|y>(f:FunctionDefinition<{->R[y]}>[1]):Result<R|y>[0..1]\n" +
                "{\n" +
                "   meta::alloy::test::mayExecuteAlloyTest(\n" +
                "      {v, vv, host, port | let zz = 'alloyExecute_FunctionDefinition_1__String_1__Integer_1__Result_1_'->pathToElement()->cast(@FunctionDefinition<{->Any[*]}>);" +
                "                    $zz->evaluate([$f, $host, $port]->map(z|list($z)))->toOne()->cast(@Result<R|y>);},\n" +
                "      {|^RoutingQuery<R|y>(fn=$f);[]->cast(@Result<R|y>);}\n" +
                "   );\n" +
                "}" +
                "Class A{}" +
                "\n" +
                "Class meta::pure::router::RoutingQuery<T|m>\n" +
                "{\n" +
                "   fn : FunctionDefinition<{->T[m]}>[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "     execute(|A.all());\n" +
                "}\n" +
                "");
        this.compileAndExecute("go():Any[*]");
    }


}
