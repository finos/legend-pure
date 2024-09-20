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

package org.finos.legend.pure.m3.tests.function.base.measure;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMeasureGraphCorrectness extends AbstractPureTestWithCoreCompiled
{
    private static final String massDefinition =
            "Measure pkg::Mass\n" +
                    "{\n" +
                    "   *Gram: x -> $x;\n" +
                    "   Kilogram: x -> $x*1000;\n" +
                    "   Pound: x -> $x*453.59;\n" +
                    "}";

    private static final String distanceDefinition =
            "Measure pkg::Distance\n" +
                    "{\n" +
                    "   *Meter: x -> $x;\n" +
                    "}\n";

    private static final String currencyDefinition =
            "Measure pkg::Currency\n" +
                    "{\n" +
                    "   USD;\n" +
                    "   GBP;\n" +
                    "   EUR;\n" +
                    "}\n";

    private static final String currencyDefinitionWithConversions =
            "Measure pkg::Currency\n" +
                    "{\n" +
                    "   USD: x -> $x * 10;\n" +
                    "   GBP;\n" +
                    "   EUR;\n" +
                    "}\n";

    private static final String currencyDefinitionWithCanonicalUnit =
            "Measure pkg::Currency\n" +
                    "{\n" +
                    "   *USD;\n" +
                    "   GBP;\n" +
                    "   EUR;\n" +
                    "}\n";

    private static CoreInstance measureClass;
    private static CoreInstance unitClass;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
        measureClass = runtime.getCoreInstance(M3Paths.Measure);
        unitClass = runtime.getCoreInstance(M3Paths.Unit);
        Assert.assertNotNull(measureClass);
        Assert.assertNotNull(unitClass);
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("testModel.pure");
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    @Test
    public void testMeasureBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", massDefinition);
        Measure mass = getAndAssertMeasure("pkg::Mass");

        assertUnit(mass, "Gram", true, mass._canonicalUnit());

        ListIterable<? extends Unit> nonCanonicalUnits = mass._nonCanonicalUnits().toList();
        assertUnit(mass, "Kilogram", true, nonCanonicalUnits.get(0));
        assertUnit(mass, "Pound", true, nonCanonicalUnits.get(1));
        Assert.assertEquals(2, nonCanonicalUnits.size());
    }

    @Test
    public void testMeasureWithOnlyCanonicalUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", distanceDefinition);
        Measure distance = getAndAssertMeasure("pkg::Distance");

        assertUnit(distance, "Meter", true, distance._canonicalUnit());

        Assert.assertEquals(Lists.fixedSize.empty(), distance._nonCanonicalUnits().toList());
    }

    @Test
    public void testNonConvertibleUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", currencyDefinition);

        Measure currency = getAndAssertMeasure("pkg::Currency");

        assertUnit(currency, "USD", false, currency._canonicalUnit());

        ListIterable<? extends Unit> nonCanonicalUnits = currency._nonCanonicalUnits().toList();
        assertUnit(currency, "GBP", false, nonCanonicalUnits.get(0));
        assertUnit(currency, "EUR", false, nonCanonicalUnits.get(1));
        Assert.assertEquals(2, nonCanonicalUnits.size());
    }

    @Test
    public void testNonConvertibleUnitWithConversionFunctionFailsToCompile()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource("testSource.pure", currencyDefinitionWithConversions));
        assertPureException(PureParserException.class, "expected: a valid identifier text; found: '}'", 6, 1, e);
    }

    @Test
    public void testNonConvertibleUnitWithCanonicalUnitUnitFailsToCompile()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource("testSource.pure", currencyDefinitionWithCanonicalUnit));
        assertPureException(PureParserException.class, "expected: ':' found: ';'", 3, 8, e);
    }

    private Measure getAndAssertMeasure(String path)
    {
        CoreInstance rawInstance = runtime.getCoreInstance(path);
        Assert.assertTrue(path, rawInstance instanceof Measure);
        Measure measure = (Measure) rawInstance;

        int lastSep = path.lastIndexOf("::");
        String name = (lastSep == -1) ? path : path.substring(lastSep + 2);
        String pkg = (lastSep == -1) ? "::" : path.substring(0, lastSep);
        assertMeasure(name, pkg, measure);

        return measure;
    }

    private void assertMeasure(String expectedName, String expectedPackage, Measure measure)
    {
        Assert.assertEquals(expectedName, measure.getName());
        Assert.assertEquals(expectedName, measure._name());
        Assert.assertEquals(expectedName, measureClass, measure.getClassifier());
        Assert.assertEquals(expectedName, measureClass, measure._classifierGenericType()._rawType());

        CoreInstance pkg = runtime.getCoreInstance(expectedPackage);
        Assert.assertTrue(expectedPackage, pkg instanceof Package);
        Assert.assertEquals(pkg, measure._package());
    }

    private void assertUnit(Measure measure, String name, boolean hasConversionFn, Unit unit)
    {
        Assert.assertEquals(name, unit.getName());
        Assert.assertEquals(name, unit._name());
        Assert.assertEquals(measure, unit._measure());
        Assert.assertEquals(name, unitClass, unit.getClassifier());
        Assert.assertEquals(name, unitClass, unit._classifierGenericType()._rawType());
        if (hasConversionFn)
        {
            Assert.assertNotNull(name, unit._conversionFunction());
        }
        else
        {
            Assert.assertNull(name, unit._conversionFunction());
        }
    }
}
