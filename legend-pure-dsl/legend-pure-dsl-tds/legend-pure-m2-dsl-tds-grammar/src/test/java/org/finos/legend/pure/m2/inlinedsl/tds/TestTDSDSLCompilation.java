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

package org.finos.legend.pure.m2.inlinedsl.tds;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.TDS;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTDSDSLCompilation extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
        runtime.compile();
    }

    @Test
    public void testGrammarBaselineTest()
    {
        runtime.createInMemorySource("file.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#EEW#,2);\n" +
                        "}\n");
        PureParserException e = Assert.assertThrows(PureParserException.class, runtime::compile);
        Assert.assertEquals("Parser error at (resource:file.pure line:3 column:15), expected: one of {'as', '{', '<'} found: '<EOF>'", e.getMessage());
    }

    @Test
    public void testSimpleDeclarationAndSubtypeAny()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   print(\n" +
                        "       #TDS\n" +
                        "         value, other, name\n" +
                        "         1, 3, A\n" +
                        "         2, 4, B\n" +
                        "       #\n" +
                        ", 2);\n" +
                        "}\n");
        runtime.compile();
    }

    @Test
    public void testSimpleDeclarationApplyFunction()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function <<functionType.SideEffectFunction>> rows<T>(type:TDS<T>[1]):T[*];\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   print(\n" +
                        "       #TDS\n" +
                        "         value, other, name\n" +
                        "         1, 3, A\n" +
                        "         2, 4, B\n" +
                        "       #->rows()\n" +
                        ", 2);\n" +
                        "}\n");
        runtime.compile();
    }

    @Test
    public void testSimpleDeclarationUseColumnsInLambda()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function <<functionType.SideEffectFunction>> rows<T>(type:TDS<T>[1]):T[*];\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "    print(\n" +
                        "   #TDS\n" +
                        "       value, other, name\n" +
                        "       1, 3, A\n" +
                        "       2, 4, B\n" +
                        "   #\n" +
                        "   ->rows()->map(x|$x.value->toOne() + $x.other->toOne())\n" +
                        ", 2);\n" +
                        //"    print(A.all()->map(x|$x.a), 2);\n" +
                        "}\n");
        runtime.compile();
    }

    @Test
    public void testSimpleDeclarationUseColumnsInLambdaAndMatchTDS()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function meta::pure::functions::relation::filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];\n" +
                        "\n" +
                        "function meta::pure::functions::relation::filter<T>(rel:TDS<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1]\n" +
                        "{\n" +
                        "    $rel->cast(@meta::pure::metamodel::relation::Relation<T>)->meta::pure::functions::relation::filter($f);\n" +
                        "}\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "   print(\n" +
                        "       #TDS\n" +
                        "         value, other, name\n" +
                        "         1, 3, A\n" +
                        "         2, 4, B\n" +
                        "       #->filter(x|$x.value > 1)\n" +
                        ", 2);\n" +
                        "}\n");
        runtime.compile();

        runtime.modify("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "native function <<functionType.SideEffectFunction>> rows<T>(type:Relation<T>[1]):T[*];\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "    print(\n" +
                        "   #TDS\n" +
                        "       value, other, name\n" +
                        "       1, 3, A\n" +
                        "       2, 4, B\n" +
                        "   #\n" +
                        "   ->rows()->map(x|$x.value->toOne() + $x.other->toOne())\n" +
                        ", 2);\n" +
                        "}\n");
        runtime.compile();
    }

    @Test
    public void testFunctionMatchingDeepColumn()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "function x(t:Relation<(vce:String)>[1]):Boolean[1]\n" +
                        "{\n" +
                        "  true;\n" +
                        "}\n" +
                        "\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "    #TDS\n" +
                        "      id, name, vce\n" +
                        "      1, Pierre, a\n" +
                        "      2, Ram, e\n" +
                        "      3, Neema, e#\n" +
                        //"    ->filter(t|$t.vce == 'ok')\n" +
                        "    ->x();\n" +
                        "    true;\n" +
                        "}");
        runtime.compile();
    }

    @Test
    public void testSourceInformation()
    {
        runtime.createInMemorySource("file.pure",
                "import meta::pure::metamodel::relation::*;\n" +
                        "function test():Any[*]\n" +
                        "{\n" +
                        "    #TDS\n" +
                        "      id, name, vce\n" +
                        "      1, Pierre, a\n" +
                        "      2, Ram, e\n" +
                        "      3, Neema, e#;\n" +
                        "    #TDS\n" +
                        "      id, name, vce\n" +
                        "      1, Pierre, a\n" +
                        "      2, Ram, e\n" +
                        "      3, Neema, e\n" +
                        "    #;\n" +
                        "    #TDS id, name, vce#;" +
                        "}");
        runtime.compile();

        ConcreteFunctionDefinition<?> testFn = (ConcreteFunctionDefinition<?>) runtime.getCoreInstance("test__Any_MANY_");

        ListIterable<? extends ValueSpecification> expressionSequence = ListHelper.wrapListIterable(testFn._expressionSequence());

        SourceInformation expectedSourceInfo1 = new SourceInformation("file.pure", 4, 6, 4, 6, 8, 17);
        TDS<?> tds1 = (TDS<?>) ((InstanceValue) expressionSequence.get(0))._values().getOnly();
        Assert.assertEquals(expectedSourceInfo1, tds1.getSourceInformation());
        Assert.assertEquals(expectedSourceInfo1, tds1._classifierGenericType().getSourceInformation());

        SourceInformation expectedSourceInfo2 = new SourceInformation("file.pure", 9, 6, 9, 6, 14, 4);
        TDS<?> tds2 = (TDS<?>) ((InstanceValue) expressionSequence.get(1))._values().getOnly();
        Assert.assertEquals(expectedSourceInfo2, tds2.getSourceInformation());
        Assert.assertEquals(expectedSourceInfo2, tds2._classifierGenericType().getSourceInformation());

        SourceInformation expectedSourceInfo3 = new SourceInformation("file.pure", 15, 6, 15, 6, 15, 22);
        TDS<?> tds3 = (TDS<?>) ((InstanceValue) expressionSequence.get(2))._values().getOnly();
        Assert.assertEquals(expectedSourceInfo3, tds3.getSourceInformation());
        Assert.assertEquals(expectedSourceInfo3, tds3._classifierGenericType().getSourceInformation());
    }
}
