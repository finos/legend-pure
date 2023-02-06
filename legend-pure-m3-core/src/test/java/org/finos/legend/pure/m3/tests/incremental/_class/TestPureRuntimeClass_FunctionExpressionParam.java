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

package org.finos.legend.pure.m3.tests.incremental._class;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeClass_FunctionExpressionParam extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("userId.pure");
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeClassAsFunctionExpressionParameter() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function f(c:Class<Any>[1]):String[0..1]{$c.name}" +
                                "function test():Boolean[1]{assert('A' == f(A), |'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 93)
                        .createInMemorySource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassAsFunctionExpressionParameterError() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class A{}")
                        .createInMemorySource("userId.pure", "function f(c:Class<Any>[1]):String[0..1]{$c.name}" +
                                "function test():Boolean[1]{assert('A' == f(A),|'')}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 93)
                        .createInMemorySource("sourceId.pure", "Class B{}")
                        .compileWithExpectedCompileFailure("A has not been defined!", "userId.pure", 1, 93)
                        .updateSource("sourceId.pure", "Class A{}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPureRuntimeClassParameterUsageCleanUp() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "function f(c:Class<Any>[1]):Any[1]{$c} function k():Nil[0]{f(A);[];}")
                        .createInMemorySource("userId.pure", "Class A{}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .compile()
                        .createInMemorySource("sourceId.pure", "function f(c:Class<Any>[1]):Any[1]{$c} function k():Nil[0]{f(A);[];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

        Assert.assertEquals("A instance Class\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            [... >0]\n" +
                "    generalizations(Property):\n" +
                "        Anonymous_StripedId instance Generalization\n" +
                "            [... >0]\n" +
                "    name(Property):\n" +
                "        A instance String\n" +
                "    package(Property):\n" +
                "        Root instance Package\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            [... >0]", this.runtime.getCoreInstance("A").printWithoutDebug("", 0));
    }

    @Test
    public void testPureRuntimeClassUsedInNew() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("other.pure", "Class XX{ok:String[1];}")
                        .createInMemorySource("sourceId.pure", "function test():Nil[0]{^XX(ok='1');[];}" +
                                "function go():Nil[0]{assert(test__Nil_0_.referenceUsages->size() == 1, |'');[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("other.pure")
                        .createInMemorySource("other.pure", "Class XX{ok:String[1];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeClassUsedInNewReverse() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("other.pure", "Class XX{ok:String[1];}")
                        .createInMemorySource("sourceId.pure", "function test():Nil[0]{^XX(ok='1');[];}" +
                                "function go():Nil[0]{assert(3 == XX.referenceUsages->size(), |'');[];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function test():Nil[0]{^XX(ok='1');[];}" +
                                "function go():Nil[0]{assert(3 == XX.referenceUsages->size(), |'');[];}")
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeClassUsedInNewOther() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("other.pure", "function test():Nil[0]{^XX(ok='1');[];}")
                        .createInMemorySource("sourceId.pure", "Class XX{ok:String[1];}\n" +
                                "\n" +
                                "function meta::pure::functions::meta::functionType(f:Function<Any>[1]):FunctionType[1]\n" +
                                "{\n" +
                                "   assert($f->instanceOf(FunctionDefinition) || $f->instanceOf(NativeFunction), | 'functionType is not supported yet for this subtype of function '+$f->type()->id());\n" +
                                "   $f.classifierGenericType->toOne().typeArguments->at(0).rawType->toOne()->cast(@FunctionType);\n" +
                                "}\n" +
                                "function go():Boolean[1]\n" +
                                "{\n" +
                                "   assert(1 == test__Nil_0_->functionType().returnType.referenceUsages->size(), |'');\n" +
                                "}"

                        )
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "Class XX{ok:String[1];}\n" +
                                "function meta::pure::functions::meta::functionType(f:Function<Any>[1]):FunctionType[1]\n" +
                                "{\n" +
                                "   assert($f->instanceOf(FunctionDefinition) || $f->instanceOf(NativeFunction), | 'functionType is not supported yet for this subtype of function '+$f->type()->id());\n" +
                                "   $f.classifierGenericType->toOne().typeArguments->at(0).rawType->toOne()->cast(@FunctionType);\n" +
                                "}\n" +
                                "function go():Boolean[1]\n" +
                                "{\n" +
                                "   assert(1 == test__Nil_0_->functionType().returnType.referenceUsages->size(), |'');\n" +
                                "}"
                        )
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());

    }

    @Test
    public void testPureRuntimeClassProperty_GenericsLambdaModify() throws Exception
    {
        String sourceId2 = "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   ProductSynonym.all()->filter([p | $p.value == 'A']);\n" +
                "}\n";
        String source = "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +

                "\n" +
                "Class meta::relational::tests::mapping::enumeration::model::domain::ProductSynonym\n" +
                "{\n" +
                "   value:String[1];\n" +
                "}\n" +
                "\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source)
                        .compile().createInMemorySource("sourceId2.pure", sourceId2).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "////Comment\n" + source)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassProperty_NestedGenericsModify() throws Exception
    {
        String sourceId2 = "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +
                "\n" +
                "Class meta::pure::functions::collection::AggregateValue<T,V,U>\n" +
                "{\n" +
                "   mapFn : FunctionDefinition<{T[1]->V[*]}>[1];\n" +
                "   aggregateFn : FunctionDefinition<{V[*]->U[0..1]}>[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::collection::count(s:Any[*]):Integer[1]\n" +
                "{\n" +
                "   $s->size();\n" +
                "}" +
                "function meta::pure::functions::collection::agg<T,V,U>(mapFn:FunctionDefinition<{T[1]->V[*]}>[1], aggregateFn:FunctionDefinition<{V[*]->U[0..1]}>[1]):meta::pure::functions::collection::AggregateValue<T,V,U>[1]\n" +
                "{\n" +
                "   ^meta::pure::functions::collection::AggregateValue<T,V,U>(mapFn=$mapFn, aggregateFn=$aggregateFn);\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::collection::groupBy<T,V,U>(set:T[*], functions:meta::pure::metamodel::function::Function<{T[1]->Any[*]}>[*], aggValues:meta::pure::functions::collection::AggregateValue<T,V,U>[*], ids:String[*]):String[1]\n" +
                "{\n" +
                "\n" +
                "   'hello';\n" +
                "}\n" +
                "function go():String[*]\n" +
                "{\n" +
                "   ProductSynonym.all()->groupBy([p | $p.value]\n" +
                "                   ,agg(x|$x.value,y|$y->count())\n" +
                "                   ,['syn', 'count']);\n" +
                "}\n";
        String source = "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +

                "\n" +
                "Class meta::relational::tests::mapping::enumeration::model::domain::ProductSynonym\n" +
                "{\n" +
                "   value:String[1];\n" +
                "}\n" +
                "\n";

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source)
                        .compile().createInMemorySource("sourceId2.pure", sourceId2).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "////Comment\n" + source)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void testPureRuntimeClassProperty_NestedGenericsModifyProject() throws Exception
    {
        String sourceId2 = "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +
                "Class meta::pure::tds::TabularDataSet\n" +
                "{\n" +
                "   rows : TDSRow[*];\n" +
                "}\n" +
                "Class meta::pure::tds::TDSRow\n" +
                "{\n" +
                "   parent : TabularDataSet[0..1];\n" +
                "   values : Any[*];\n" +
                "}\n" +
                "    Class meta::pure::tds::ColumnSpecification<T>\n" +
                "    {\n" +
                "       func : Function<{T[1]->Any[*]}>[1];\n" +
                "       name : String[1];\n" +
                "       documentation : String[0..1];\n" +
                "    }\n" +
                "    \n" +
                "    function meta::pure::tds::col<T>(func : Function<{T[1]->Any[*]}>[1], name : String[1]):meta::pure::tds::ColumnSpecification<T>[1]\n" +
                "    {\n" +
                "       ^ColumnSpecification<T>\n" +
                "       (\n" +
                "          func = $func,\n" +
                "          name = $name\n" +
                "       )\n" +

                "    }\n" +
                "   function meta::pure::tds::project<T>(set:T[*], columnSpecifications:ColumnSpecification<T>[*]):TabularDataSet[1]" +
                "   {" +
                "      ^TabularDataSet();\n" +
                "   }\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "let synoynms = [^ProductSynonym(value='ABC'), ^ProductSynonym(value='DEF')];\n" +
                "   assert([] == $synoynms->project(col(p|$p.value, 'Syn')).rows->map(r|$r.values), |'');\n" +
                "}\n";
        String source = "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +

                "\n" +
                "Class meta::relational::tests::mapping::enumeration::model::domain::ProductSynonym\n" +
                "{\n" +
                "   value:String[1];\n" +
                "}\n" +
                "\n";


        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", source)
                        .compile().createInMemorySource("sourceId2.pure", sourceId2).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("sourceId.pure", "////Comment\n" + source)
                        .compile(),
                this.runtime, this.functionExecution, this.getAdditionalVerifiers());
    }

    protected ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getAdditionalVerifiers()
    {
        return Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of();
    }
}
