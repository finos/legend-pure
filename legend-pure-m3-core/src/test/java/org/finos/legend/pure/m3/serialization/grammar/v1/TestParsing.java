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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestParsing extends AbstractPureTestWithCoreCompiledPlatform
{

    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void parsingEOFError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "function myAdd(a:String[1], b:String[1]):String[1]\n" +
                            "{\n" +
                            "   'aa';\n" +
                            "} helloeoe");
            Assert.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureParserException.class, "expected: one of {<EOF>, '^', 'native', 'function', 'Class', 'Association', 'Profile', 'Enum', 'Measure'} found: 'helloeoe'", "fromString.pure", 4, 3, e);
        }
    }

    @Test
    public void testParseEmptyList()
    {
        compileTestSource("fromString.pure", "function go():Any[*]\n" +
                "{\n" +
                "   []\n" +
                "}\n");
        CoreInstance function = this.runtime.getFunction("go():Any[*]");
        Assert.assertNotNull(function);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(function, M3Properties.expressionSequence, this.processorSupport);
        Verify.assertSize(1, expressions);
        CoreInstance expression = expressions.get(0);
        Assert.assertTrue(Instance.instanceOf(expression, M3Paths.InstanceValue, this.processorSupport));

        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(expression, M3Properties.values, this.processorSupport));

        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.genericType, this.processorSupport);
        Assert.assertSame(this.processorSupport.type_BottomType(), Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, this.processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(genericType, M3Properties.typeArguments, this.processorSupport));

        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.multiplicity, this.processorSupport);
        Assert.assertTrue(Multiplicity.isMultiplicityConcrete(multiplicity));
        Assert.assertEquals(0, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assert.assertEquals(0, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    @Test
    public void testPackageIntegrity()
    {
        try
        {
            compileTestSource("fromString.pure", "Class test::TestClass\n" +
                    "{\n" +
                    "   prop:String[1];\n" +
                    "}\n" +
                    "\n" +
                    "function test::TestClass::func(a:String[1], b:String[1]):String[1]\n" +
                    "{\n" +
                    "   $a + $b\n" +
                    "}");
            Assert.fail("Expected a compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureParserException.class, "('test::TestClass' is a Class, should be a Package) in\n'test::TestClass'", 6, 14, e);
        }
    }
}
