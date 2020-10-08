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

package org.finos.legend.pure.m3.tests.literal;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestMultiplicity extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testSingleLiteralMultiplicity()
    {
        compileTestSource("function test::testFn():Any[*]\n" +
                "{\n" +
                "  1\n" +
                "}");
        ConcreteFunctionDefinition testFn = (ConcreteFunctionDefinition)this.runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        ListIterable<ValueSpecification> expressions = (ListIterable<ValueSpecification>)testFn._expressionSequence();
        Verify.assertSize(1, expressions);
        ValueSpecification expression = expressions.get(0);
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(1, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(1, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testLiteralCollectionMultiplicity()
    {
        compileTestSource("function test::testFn():Any[*]\n" +
                "{\n" +
                "  [1, 2, 3]\n" +
                "}");
        ConcreteFunctionDefinition testFn = (ConcreteFunctionDefinition)this.runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        ListIterable<ValueSpecification> expressions = (ListIterable<ValueSpecification>)testFn._expressionSequence();
        Verify.assertSize(1, expressions);
        ValueSpecification expression = expressions.get(0);
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(3, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(3, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testSingleFunctionExpressionMultiplicity()
    {
        compileTestSource("function test::testFn():Any[*]\n" +
                "{\n" +
                "  [1, 2, 3]->first();\n" +
                "}");
        ConcreteFunctionDefinition testFn = (ConcreteFunctionDefinition)this.runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        ListIterable<ValueSpecification> expressions = (ListIterable<ValueSpecification>)testFn._expressionSequence();
        Verify.assertSize(1, expressions);
        ValueSpecification expression = expressions.get(0);
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(0, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(1, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testSingleFunctionExpressionCollectionMultiplicity()
    {
        compileTestSource("function test::testFn():Any[*]\n" +
                "{\n" +
                "  [[1, 2, 3]->first()];\n" +
                "}");
        ConcreteFunctionDefinition testFn = (ConcreteFunctionDefinition)this.runtime.getFunction("test::testFn():Any[*]");
        Assert.assertNotNull(testFn);

        ListIterable<ValueSpecification> expressions = (ListIterable<ValueSpecification>)testFn._expressionSequence();
        Verify.assertSize(1, expressions);
        ValueSpecification expression = expressions.get(0);
        CoreInstance multiplicity = expression._multiplicity();
        Assert.assertEquals(0, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(1, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }
}
