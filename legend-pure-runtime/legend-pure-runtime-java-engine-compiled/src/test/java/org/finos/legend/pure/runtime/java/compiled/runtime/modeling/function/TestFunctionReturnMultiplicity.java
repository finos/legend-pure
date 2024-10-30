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

package org.finos.legend.pure.runtime.java.compiled.runtime.modeling.function;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunctionReturnMultiplicity extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_FILE_NAME = "fromString.pure";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(
                getFunctionExecution(),
                JavaModelFactoryRegistryLoader.loader(),
                Tuples.pair(
                        "testModel.pure",
                        "Class A\n" +
                                "{\n" +
                                "   b:Integer[1];\n" +
                                "}\n" +
                                "Class Result<T|m>\n" +
                                "{\n" +
                                "   values:T[m];\n" +
                                "}\n")
        );
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_FILE_NAME);
        runtime.compile();
    }

    @Test
    public void testReturnNull()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function process():String[*]\n" +
                        "{\n" +
                        "    ['a','b']\n" +
                        "}\n" +
                        "function makeString(any:Any[*], s:String[1]):String[1]\n" +
                        "{\n" +
                        "    $any->map(x | $x->toString())->joinStrings('', $s, '')\n" +
                        "}\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "   assertEquals('a__b', process()->makeString('__'));" +
                        "   [];\n" +
                        "}\n");
        CoreInstance result = compileAndExecute("test():Nil[0]");
        ListIterable<? extends CoreInstance> values = result.getValueForMetaPropertyToMany(M3Properties.values);
        Assert.assertEquals(Lists.fixedSize.empty(), values);
    }

    @Test
    public void testReturnManyTypeConversion()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function process():Any[*]\n" +
                        "{\n" +
                        "    ['a', 1, 2.0, %2015-03-12, %2015-03-12T23:59:00, true, Class]\n" +
                        "}\n" +
                        "\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   process();\n" +
                        "}\n");
        CoreInstance result = compileAndExecute("test():Any[*]");
        ListIterable<? extends CoreInstance> values = result.getValueForMetaPropertyToMany(M3Properties.values);
        Assert.assertEquals("a instanceOf String,1 instanceOf Integer,2.0 instanceOf Float,2015-03-12 instanceOf StrictDate,2015-03-12T23:59:00+0000 instanceOf DateTime,true instanceOf Boolean," + runtime.getCoreInstance(M3Paths.Class), values.makeString(","));
    }

    @Test
    public void testReturnExactlyOnePrimitiveTypeConversion()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function process():Any[*]\n" +
                        "{\n" +
                        "    ['a']\n" +
                        "}\n" +
                        "\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   process();\n" +
                        "}\n");
        CoreInstance result = compileAndExecute("test():Any[*]");
        CoreInstance value = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertEquals("a instanceOf String", value.toString());
    }

    @Test
    public void testReturnExactlyOneNonPrimitiveTypeConversion()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function process():Any[*]\n" +
                        "{\n" +
                        "    [Class]\n" +
                        "}\n" +
                        "\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   process();\n" +
                        "}\n");
        CoreInstance result = compileAndExecute("test():Any[*]");
        CoreInstance value = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertSame(runtime.getCoreInstance(M3Paths.Class), value);
    }

    @Test
    public void testReturnTypeMultiplicityArg()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function process<T|m>(f:FunctionDefinition<{->T[m]}>[1]):Result<T|m>[1]\n" +
                        "{\n" +
                        "    let vals = $f->eval();\n" +
                        "    ^Result<T|m>(values=$vals);\n" +
                        "}\n" +
                        "\n" +
                        "function testOne():A[1]\n" +
                        "{\n" +
                        "   process({| ^A(b=1)}).values;\n" +
                        "}\n" +
                        "\n" +
                        "function testMany():A[*]\n" +
                        "{\n" +
                        "   process({| [^A(b=1),^A(b=2)]}).values;\n" +
                        "}\n");
        CoreInstance result = this.compileAndExecute("testOne():A[1]");
        CoreInstance value = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertEquals("A", functionExecution.getProcessorSupport().getClassifier(value).getName());
        Assert.assertEquals("1", value.getValueForMetaPropertyToOne("b").getName());

        CoreInstance result2 = this.execute("testMany():A[*]");
        ListIterable<? extends CoreInstance> value2 = result2.getValueForMetaPropertyToMany(M3Properties.values);
        Assert.assertEquals("1,2", value2.collect(v -> v.getValueForMetaPropertyToOne("b").getName()).makeString(","));
    }

    @Test
    public void testReturnTypeMultiplicityArgWithLet()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function process<T|m>(f:FunctionDefinition<{->T[m]}>[1]):Result<T|m>[1]\n" +
                        "{\n" +
                        "    let vals = $f->eval();\n" +
                        "    ^Result<T|m>(values=$vals);\n" +
                        "}\n" +
                        "\n" +
                        "function testOne():A[1]\n" +
                        "{\n" +
                        "   let v = process({| ^A(b=1)}).values;\n" +
                        "   $v;\n" +
                        "}\n" +
                        "function testOne2():A[1]\n" +
                        "{\n" +
                        "   let v = process({| ^A(b=1)});\n" +
                        "   $v.values;\n" +
                        "}\n" +
                        "\n" +
                        "function testMany():A[*]\n" +
                        "{\n" +
                        "   let v = process({| [^A(b=1),^A(b=2)]}).values;\n" +
                        "   $v;\n" +
                        "}\n");
        CoreInstance result = this.compileAndExecute("testOne():A[1]");
        CoreInstance value = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assert.assertEquals("A", functionExecution.getProcessorSupport().getClassifier(value).getName());
        Assert.assertEquals("1", value.getValueForMetaPropertyToOne("b").getName());

        CoreInstance result2 = this.execute("testMany():A[*]");
        ListIterable<? extends CoreInstance> value2 = result2.getValueForMetaPropertyToMany(M3Properties.values);
        Assert.assertEquals("1,2", value2.collect(o -> o.getValueForMetaPropertyToOne("b").getName()).makeString(","));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

//    public static RichIterable<? extends java.lang.Object> Root_go__Any_MANY_(final ExecutionSupport es)
//    {
//        final org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends java.lang.String, ? extends java.lang.Long> _pair = (CompiledSupport.<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends java.lang.String, ? extends java.lang.Long>>castWithExceptionHandling(platform_functions_collection.Root_meta_pure_functions_collection_pair_U_1__V_1__Pair_1_("bla", 2l, es), org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair.class, new org.finos.legend.pure.m4.coreinstance.SourceInformation("inferenceTest.pure", -1, -1, 3, 15, -1, -1)));
//        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<? extends java.lang.Object> _evalProp = new PureCompiledLambda(
//                (((CompiledExecutionSupport) es).getMetadataAccessor().getLambdaFunction("go$1$system$imports$import_inferenceTest_pure_1$0")
//                ), (
//                inferenceTest.__functions.get("go$1$system$imports$import_inferenceTest_pure_1$0")
//        ));
//        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends java.lang.Object, ? extends java.lang.Object> _p = CompiledSupport.toOne(CompiledSupport.toPureCollection(CompiledSupport.<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends java.lang.Object, ? extends java.lang.Object>>castWithExceptionHandling(CompiledSupport.toPureCollection(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair>) ((CompiledExecutionSupport) es).getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::functions::collection::Pair"))._properties()), org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property.class, new org.finos.legend.pure.m4.coreinstance.SourceInformation("inferenceTest.pure", -1, -1, 5, 29, -1, -1))).select(new DefendedPredicate<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends java.lang.Object, ? extends java.lang.Object>>()
//        {
//            public boolean accept(final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends java.lang.Object, ? extends java.lang.Object> _p)
//            {
//                return CompiledSupport.equal(_p._name(), "first");
//            }
//        }), new org.finos.legend.pure.m4.coreinstance.SourceInformation("inferenceTest.pure", -1, -1, 5, 85, -1, -1));
//        return CompiledSupport.toPureCollection(((RichIterable<? extends java.lang.Object>)
//                (Object)
//                        (CompiledSupport.toPureCollection(
//                                CoreGen.evaluateToMany(
//                                        es,
//                                        _evalProp,
//                                        Lists.mutable.<org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List>with(
//                                                (CompiledSupport.<org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<? extends java.lang.Object, ? extends java.lang.Object>>>
//                                                        castWithExceptionHandling(
//                                                                platform_functions_collection.Root_meta_pure_functions_collection_list_U_MANY__List_1_(CompiledSupport.toPureCollection(_p), es),
//                                                                org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List.class,
//                                                                new org.finos.legend.pure.m4.coreinstance.SourceInformation("inferenceTest.pure", -1, -1, 5, 117, -1, -1))),
//                                                (CompiledSupport.<org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends java.lang.String, ? extends java.lang.Long>>>castWithExceptionHandling(platform_functions_collection.Root_meta_pure_functions_collection_list_U_MANY__List_1_(CompiledSupport.toPureCollection(_pair), es),
//                                                        org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List.class,
//                                                        new org.finos.legend.pure.m4.coreinstance.SourceInformation("inferenceTest.pure", -1, -1, 5, 127, -1, -1)))))))));
//    }
}
