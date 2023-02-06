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

package org.finos.legend.pure.m3.tests.elements.valueSpec.literal;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMultiplicity extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSingleLiteralMultiplicity()
    {
        compileTestSource("fromString.pure",
                "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  1\n" +
                        "}");
        ConcreteFunctionDefinition<?> testFn = (ConcreteFunctionDefinition<?>) runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        RichIterable<? extends ValueSpecification> expressions = testFn._expressionSequence();
        Assert.assertEquals(1, expressions.size());
        ValueSpecification expression = expressions.getAny();
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(1, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(1, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testLiteralCollectionMultiplicity()
    {
        compileTestSource("fromString.pure",
                "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  [1, 2, 3]\n" +
                        "}");
        ConcreteFunctionDefinition<?> testFn = (ConcreteFunctionDefinition<?>) runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        RichIterable<? extends ValueSpecification> expressions = testFn._expressionSequence();
        Assert.assertEquals(1, expressions.size());
        ValueSpecification expression = expressions.getAny();
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(3, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(3, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testSingleFunctionExpressionMultiplicity()
    {
        compileTestSource("fromString.pure", "function test::testFn():Any[*]\n" +
                "{\n" +
                "  [1, 2, 3]->first();\n" +
                "}");
        ConcreteFunctionDefinition<?> testFn = (ConcreteFunctionDefinition<?>) runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        RichIterable<? extends ValueSpecification> expressions = testFn._expressionSequence();
        Assert.assertEquals(1, expressions.size());
        ValueSpecification expression = expressions.getAny();
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(0, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(1, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testSingleFunctionExpressionCollectionMultiplicity()
    {
        compileTestSource("fromString.pure", "function test::testFn():Any[*]\n" +
                "{\n" +
                "  [[1, 2, 3]->first()];\n" +
                "}");
        ConcreteFunctionDefinition<?> testFn = (ConcreteFunctionDefinition<?>) runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        RichIterable<? extends ValueSpecification> expressions = testFn._expressionSequence();
        Assert.assertEquals(1, expressions.size());
        ValueSpecification expression = expressions.getAny();
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(0, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(1, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }
}
