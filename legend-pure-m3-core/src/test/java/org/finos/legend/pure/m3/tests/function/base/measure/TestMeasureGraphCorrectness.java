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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.Assert;
import org.junit.Test;

public class TestMeasureGraphCorrectness  extends AbstractPureTestWithCoreCompiled
{
    private static String massDefinition =
            "Measure pkg::Mass\n" +
                    "{\n" +
                    "   *Gram: x -> $x;\n" +
                    "   Kilogram: x -> $x*1000;\n" +
                    "   Pound: x -> $x*453.59;\n" +
                    "}";

    private static String distanceDefinition =
            "Measure pkg::Distance\n" +
                    "{\n" +
                    "   *Meter: x -> $x;\n" +
                    "}\n";

    private static String currencyDefinition =
            "Measure pkg::Currency\n" +
                    "{\n" +
                    "   USD;\n" +
                    "   GBP;\n" +
                    "   EUR;\n" +
                    "}\n";

    private static String currencyDefinitionWithConversions =
            "Measure pkg::Currency\n" +
                    "{\n" +
                    "   USD: x -> $x * 10;\n" +
                    "   GBP;\n" +
                    "   EUR;\n" +
                    "}\n";

    private static String currencyDefinitionWithCanonicalUnit =
            "Measure pkg::Currency\n" +
                    "{\n" +
                    "   *USD;\n" +
                    "   GBP;\n" +
                    "   EUR;\n" +
                    "}\n";

    @Test
    public void testMeasureBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", massDefinition);
        CoreInstance massCoreInstance = this.runtime.getCoreInstance("pkg::Mass");
        Assert.assertEquals("Mass", massCoreInstance.getName());
        Assert.assertTrue(massCoreInstance instanceof Measure);
        Assert.assertEquals("Measure", massCoreInstance.getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToOne("rawType").getName());
        CoreInstance canonicalUnit = massCoreInstance.getValueForMetaPropertyToOne("canonicalUnit");
        Assert.assertEquals("Mass~Gram", canonicalUnit.getName());
        Assert.assertTrue(canonicalUnit instanceof Unit);
        ListIterable<? extends CoreInstance> nonCanonicalUnits = massCoreInstance.getValueForMetaPropertyToMany("nonCanonicalUnits");
        Assert.assertEquals("Mass~Kilogram", nonCanonicalUnits.get(0).getName());
        Assert.assertTrue(nonCanonicalUnits.get(0) instanceof Unit);
        Assert.assertEquals("Mass~Pound", nonCanonicalUnits.get(1).getName());
        Assert.assertTrue(nonCanonicalUnits.get(1) instanceof Unit);
        Assert.assertEquals("pkg", massCoreInstance.getValueForMetaPropertyToOne("package").getName());
    }

    @Test
    public void testMeasureWithOnlyCanonicalUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", distanceDefinition);
        CoreInstance distanceCoreInstance = this.runtime.getCoreInstance("pkg::Distance");
        Assert.assertEquals("Distance", distanceCoreInstance.getName());
        Assert.assertTrue(distanceCoreInstance instanceof Measure);
        Assert.assertEquals("Measure", distanceCoreInstance.getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToOne("rawType").getName());
        CoreInstance canonicalUnit = distanceCoreInstance.getValueForMetaPropertyToOne("canonicalUnit");
        Assert.assertEquals("Distance~Meter", canonicalUnit.getName());
        Assert.assertTrue(canonicalUnit instanceof Unit);
        Assert.assertEquals("pkg", distanceCoreInstance.getValueForMetaPropertyToOne("package").getName());
    }

    @Test
    public void testUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", massDefinition);
        CoreInstance kilogramCoreInstance = this.runtime.getCoreInstance("pkg::Mass~Kilogram");
        Assert.assertEquals("Mass~Kilogram", kilogramCoreInstance.getName());
        Assert.assertTrue(kilogramCoreInstance instanceof Unit);
        Assert.assertEquals("Unit", kilogramCoreInstance.getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToOne("rawType").getName());
        Assert.assertTrue(kilogramCoreInstance.getValueForMetaPropertyToOne("conversionFunction") instanceof LambdaFunction);
        CoreInstance myMeasure = kilogramCoreInstance.getValueForMetaPropertyToOne("measure");
        Assert.assertEquals("Mass", myMeasure.getName());
        Assert.assertTrue(myMeasure instanceof Measure);
        CoreInstance myPackage = kilogramCoreInstance.getValueForMetaPropertyToOne("package");
        Assert.assertEquals("pkg", myPackage.getName());
    }

    @Test
    public void testNonConvertibleUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", currencyDefinition);
        CoreInstance dollarCoreInstance = this.runtime.getCoreInstance("pkg::Currency~USD");
        Assert.assertEquals("Currency~USD", dollarCoreInstance.getName());
        Assert.assertTrue(dollarCoreInstance instanceof Unit);
        Assert.assertEquals("Unit", dollarCoreInstance.getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToOne("rawType").getName());
        Assert.assertNull(dollarCoreInstance.getValueForMetaPropertyToOne("conversionFunction"));
        CoreInstance myMeasure = dollarCoreInstance.getValueForMetaPropertyToOne("measure");
        Assert.assertEquals("Currency", myMeasure.getName());
        Assert.assertTrue(myMeasure instanceof Measure);
        CoreInstance myPackage = dollarCoreInstance.getValueForMetaPropertyToOne("package");
        Assert.assertEquals("pkg", myPackage.getName());
    }

    @Test
    public void testNonConvertibleUnitWithConversionFunctionFailsToCompile()
    {
        try
        {
            compileTestSource("testSource.pure", currencyDefinitionWithConversions);
            Assert.fail("Expected parser exception");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: a valid identifier text; found: '}'", 6, 1, e);
        }
    }

    @Test
    public void testNonConvertibleUnitWithCanonicalUnitUnitFailsToCompile()
    {
        try
        {
            compileTestSource("testSource.pure", currencyDefinitionWithCanonicalUnit);
            Assert.fail("Expected parser exception");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ':' found: ';'", 3, 8, e);
        }
    }
}
