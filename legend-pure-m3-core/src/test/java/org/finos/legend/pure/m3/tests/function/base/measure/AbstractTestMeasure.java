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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestMeasure extends AbstractPureTestWithCoreCompiled
{
    private static final String massDefinition =
            "Measure pkg::Mass\n" +
                    "{\n" +
                    "   *Gram: x -> $x;\n" +
                    "   Kilogram: x -> $x*1000;\n" +
                    "   Pound: x -> $x*453.59;\n" +
                    "}";

    private static final String plusFunction =
            "function meta::pure::functions::math::sum(numbers:Number[*]):Number[1]\n" +
                    "{\n" +
                    "    $numbers->plus();\n" +
                    "}" +
                    "function meta::pure::functions::math::plus(masses: Mass[*]):Mass~Gram[1]\n" +
                    "{\n" +
                    "   let cv = $masses->map(m|let cv = $m->type()->cast(@Unit).conversionFunction->cast(@Function<{Number[1]->Number[1]}>)->toOne()->eval(getUnitValue($m)););\n" +
                    "   let resultNumeric = $cv->sum();\n" +
                    "   newUnit(Mass~Gram, $resultNumeric)->cast(@Mass~Gram);\n" +
                    "}\n";

    private static final String minusFunction =
            "function meta::pure::functions::math::minus(masses: Mass[*]):Mass~Gram[1]\n" +
                    "{\n" +
                    "   let cv = $masses->map(m|let cv = $m->type()->cast(@Unit).conversionFunction->cast(@Function<{Number[1]->Number[1]}>)->toOne()->eval(getUnitValue($m)););\n" +
                    "   let resultNumeric = $cv->minus();\n" +
                    "   newUnit(Mass~Gram, $resultNumeric)->cast(@Mass~Gram);\n" +
                    "}\n";

    private static final String multFunction =
            "function meta::pure::unit::massScalarTimes(mass: Mass[1], nums: Number[*]):Mass~Gram[1]\n" +
                    "{\n" +
                    "   let convertedValue = $mass->type()->cast(@Unit).conversionFunction->cast(@Function<{Number[1]->Number[1]}>)->toOne()->eval(getUnitValue($mass));" +
                    "   let numsMultResult = $nums->times();\n" +
                    "   let myValue = [$convertedValue, $numsMultResult]->times();\n" +
                    "   newUnit(Mass~Gram, $myValue)->cast(@Mass~Gram);\n" +
                    "}\n";

    private static final String divFunction =
            "function meta::pure::unit::massScalarDivision(mass: Mass[1], nums: Number[*]):Mass~Gram[1]\n" +
                    "{\n" +
                    "   let convertedValue = $mass->type()->cast(@Unit).conversionFunction->cast(@Function<{Number[1]->Number[1]}>)->toOne()->eval(getUnitValue($mass));" +
                    "   let numsMultResult = $nums->times();\n" +
                    "   assert($numsMultResult != 0, 'Cannot divide by zero.');\n" +
                    "   let myValue = $convertedValue->divide($numsMultResult);\n" +
                    "   newUnit(Mass~Gram, $myValue)->cast(@Mass~Gram);\n" +
                    "}\n";

    @After
    public void cleanRuntime()
    {
        runtime.delete("testModel.pure");
        runtime.delete("testFunc.pure");
        runtime.compile();
    }

    @Test
    public void testUnitInstantiatesAndReturnsFromFunction()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Mass~Pound[0..1]\n" +
                        "{\n" +
                        "   1 Mass~Pound;\n" +
                        "}\n");
        CoreInstance result = execute("testFunc():Mass~Pound[0..1]");
        Assert.assertEquals("Mass~Pound", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("1", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testInstantiateStringValuedUnitThrowsParserError()
    {
        compileTestSource("testModel.pure", massDefinition);

        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Any[*]\n" +
                        "{\n" +
                        "   let a = 'IamAString' Mass~Pound;" +
                        "}"));
        assertPureException(PureParserException.class, "expected: one of {'->', '}', '.', ';', '&&', '||', '==', '!=', '+', '*', '-', '/', '<', '<=', '>', '>='} found: 'Mass'", e);
    }

    @Test
    public void testInstantiateBooleanValuedUnitThrowsParserError()
    {
        compileTestSource("testModel.pure", massDefinition);
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Any[*]\n" +
                        "{\n" +
                        "   let a = false Mass~Pound;" +
                        "}"));
        assertPureException(PureParserException.class, "expected: one of {'->', '}', '.', ';', '&&', '||', '==', '!=', '+', '*', '-', '/', '<', '<=', '>', '>='} found: 'Mass'", e);
    }

    @Test
    public void testInstantiateClassValuedUnitThrowsParserError()
    {
        compileTestSource("testModel.pure", "Class A\n" +
                "{\n" +
                "   name: String[1];\n" +
                "}");
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Any[*]\n" +
                        "{\n" +
                        "   let a = ^A(name='className');\n" +
                        "   let b = $a Mass~Pound;" +
                        "}"));
        assertPureException(PureParserException.class, "expected: one of {'->', '.', ';', '&&', '||', '==', '!=', '+', '*', '-', '/', '<', '<=', '>', '>='} found: 'Mass'", e);
    }

    @Test
    public void testUnitPassedInFunctionAsUnitReturnsUnit()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testUnits(k: Mass~Kilogram[1]):Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   $k;\n" +
                        "}\n" +
                        "\n" +
                        "function testUnitsWrapper():Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   testUnits(5 Mass~Kilogram);\n" +
                        "}");
        CoreInstance result = execute("testUnitsWrapper():Mass~Kilogram[1]");
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUsingConversionFunction()
    {
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "function testConversionFunc():Number[1]\n" +
                        "{\n" +
                        "   let num = 5 Mass~Gram->type()->cast(@Unit).conversionFunction->cast(@Function<{Number[1]->Number[1]}>)->toOne()->eval(5);\n" +
                        "}\n");
        CoreInstance result = execute("testConversionFunc():Number[1]");
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testGettingCanonicalUnitAndUseConversionFunction()
    {
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "function testGetCanonical():Number[1]\n" +
                        "{\n" +
                        "   let canonical = 5 Mass~Kilogram->type()->cast(@Unit).measure.canonicalUnit.conversionFunction->cast(@Function<{Number[1]->Number[1]}>)->toOne()->eval(5);\n" +
                        "}\n");
        CoreInstance result = execute("testGetCanonical():Number[1]");
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUnitPassedInFunctionAsUnitReturnsMeasure()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testUnits(k: Mass~Kilogram[1]):Mass[1]\n" +
                        "{\n" +
                        "   $k;\n" +
                        "}\n" +
                        "\n" +
                        "function testUnitsWrapper():Mass[1]\n" +
                        "{\n" +
                        "   testUnits(5 Mass~Kilogram);\n" +
                        "}");
        CoreInstance result = execute("testUnitsWrapper():Mass[1]");
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUnitPassedInFunctionAsMeasureReturnsMeasure()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testUnits(k: Mass[1]):Mass[1]\n" +
                        "{\n" +
                        "   $k;\n" +
                        "}\n" +
                        "\n" +
                        "function testUnitsWrapper():Mass[1]\n" +
                        "{\n" +
                        "   testUnits(5 Mass~Kilogram);\n" +
                        "}");
        CoreInstance result = execute("testUnitsWrapper():Mass[1]");
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUnitPassedInFunctionAsMeasureReturnsUnit()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testUnits(k: Mass[1]):Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   $k->cast(@Mass~Kilogram);\n" +
                        "}\n" +
                        "\n" +
                        "function testUnitsWrapper():Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   testUnits(5 Mass~Kilogram);\n" +
                        "}");
        CoreInstance result = execute("testUnitsWrapper():Mass~Kilogram[1]");
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testCastKilogramToKilogram()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testUnitsWrapper():Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   5 Mass~Kilogram->cast(@Mass~Kilogram);\n" +
                        "}");
        CoreInstance result = execute("testUnitsWrapper():Mass~Kilogram[1]");
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testCastKilogramToMass()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testUnitsWrapper():Mass[1]\n" +
                        "{\n" +
                        "   5 Mass~Kilogram->cast(@Mass);\n" +
                        "}");
        CoreInstance result = execute("testUnitsWrapper():Mass[1]");
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramInstanceOfKilogram()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testInstanceOf():Boolean[1]\n" +
                        "{\n" +
                        "   5 Mass~Kilogram->instanceOf(Mass~Kilogram);\n" +
                        "}");
        CoreInstance result = execute("testInstanceOf():Boolean[1]");
        Assert.assertEquals("true", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramInstanceOfMass()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testInstanceOf():Boolean[1]\n" +
                        "{\n" +
                        "   5 Mass~Kilogram->instanceOf(Mass);\n" +
                        "}");
        CoreInstance result = execute("testInstanceOf():Boolean[1]");
        Assert.assertEquals("true", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testInstantiateUnitWithNewUnit()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Mass~Pound[1]\n" +
                        "{\n" +
                        "   let teddy = newUnit(Mass~Pound, 10)->cast(@Mass~Pound);\n" +
                        "   $teddy;" +
                        "}");
        CoreInstance result = execute("testFunc():Mass~Pound[1]");
        Assert.assertEquals("Mass~Pound", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("10", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testGetUnitValue()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testGetUnitValue():Number[1]\n" +
                        "{\n" +
                        "getUnitValue(5 Mass~Kilogram);\n" +
                        "}\n");
        CoreInstance result = execute("testGetUnitValue():Number[1]");
        Assert.assertEquals("Integer", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testCrossUnitAddition()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        plusFunction +
                        "function testFunc():Any[1]\n" +
                        "{\n" +
                        "   let a = 500 Mass~Gram;\n" +
                        "   let b = 5 Mass~Kilogram;\n" +
                        "   let result = $a + $b; \n" +
                        "   let result2 = $result + 50 Mass~Gram;\n" +
                        "}");
        CoreInstance result = execute("testFunc():Any[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5550", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testOperationWithUnitAsMass()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        plusFunction +
                        "function testParams(lb:Mass~Pound[1]):Mass[1]\n" +
                        "{\n" +
                        "   $lb\n" +
                        "}\n" +
                        "\n" +
                        "function testParamsWrapper():Mass~Gram[1]\n" +
                        "{\n" +
                        "   let y = testParams(2 Mass~Pound);\n" +
                        "   $y + 5 Mass~Kilogram;\n" +
                        "}");
        CoreInstance result = execute("testParamsWrapper():Mass~Gram[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5907.18", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testCrossUnitSubtraction()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        minusFunction +
                        "function testFunc():Any[1]\n" +
                        "{\n" +
                        "   let a = 5 Mass~Kilogram;\n" +
                        "   let b = 300 Mass~Gram;\n" +
                        "   let result = $a - $b; \n" +
                        "   let result2 = $result - 50 Mass~Gram;\n" +
                        "}");
        CoreInstance result = execute("testFunc():Any[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("4650", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramInstanceAsClassProperty()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "Class A {lb : Mass~Pound[1];\n}" +
                        "function testKilogramInstanceAsClassProperty():Mass~Pound[1]\n" +
                        "{\n" +
                        "   let a = ^A(lb = 5 Mass~Pound);\n" +
                        "   $a.lb;\n" +
                        "}");
        CoreInstance lb = execute("testKilogramInstanceAsClassProperty():Mass~Pound[1]");
        Assert.assertEquals("Mass~Pound", GenericType.print(lb.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("5", lb.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testInvokingClassQualifiedPropertyWithUnit()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        plusFunction +
                        "Class A {\n" +
                        "   myUnit: Unit[1];\n" +
                        "   lb : Mass~Pound[1];\n" +
                        "   kgs : Mass~Kilogram[*];\n" +
                        "   \n" +
                        "   doStuff(){\n" +
                        "     $this.lb + $this.kgs->at(0) \n" +
                        "   }:Mass~Gram[1];\n" +
                        "}" +
                        "function testQPWrapper():Mass~Gram[1]\n" +
                        "{\n" +
                        "   let newA = ^A(myUnit=Mass~Gram, lb=1 Mass~Pound, kgs=[2 Mass~Kilogram, 1.5 Mass~Kilogram]);\n" +
                        "   $newA.doStuff();\n" +
                        "}\n");
        CoreInstance result = execute("testQPWrapper():Mass~Gram[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("2453.59", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testPlusWithAtFunctionOnSingletonUnitInstance()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        plusFunction +
                        "function testParams(lb:Mass~Pound[1], m:Mass~Kilogram[*]):Mass~Gram[1]\n" +
                        "{\n" +
                        "   $m->at(0) + $lb\n" +
                        "}\n" +
                        "\n" +
                        "function testParamsWrapper():Mass~Gram[1]\n" +
                        "{\n" +
                        "   testParams(3 Mass~Pound, 5 Mass~Kilogram);\n" +
                        "}\n");
        CoreInstance result = execute("testParamsWrapper():Mass~Gram[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("6360.77", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUnitScalarMultiplication()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        multFunction +
                        "function testMult(k:Mass~Kilogram[1]):Mass~Gram[1]\n" +
                        "{\n" +
                        "   meta::pure::unit::massScalarTimes($k, 3)\n" +
                        "}\n" +
                        "\n" +
                        "function testMultWrapper():Mass~Gram[1]\n" +
                        "{\n" +
                        "   testMult(3 Mass~Kilogram);\n" +
                        "}\n");
        CoreInstance result = execute("testMultWrapper():Mass~Gram[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("9000", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUnitScalarDivision()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        divFunction +
                        "function testDiv(k:Mass~Kilogram[1]):Mass~Gram[1]\n" +
                        "{\n" +
                        "   meta::pure::unit::massScalarDivision($k, 3)\n" +
                        "}\n" +
                        "\n" +
                        "function testDivWrapper():Mass~Gram[1]\n" +
                        "{\n" +
                        "   testDiv(9 Mass~Kilogram);\n" +
                        "}\n");
        CoreInstance result = execute("testDivWrapper():Mass~Gram[1]");
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("3000.0", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramPropertyListedInClassProperties()
    {
        String massAndClassDefinition =
                massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "   myKilo: pkg::Mass~Kilogram[1];\n" +
                        "}\n";

        compileTestSource("testModel.pure", massAndClassDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Any[*]\n" +
                        "{\n" +
                        "   A.properties->size();\n" +
                        "}\n");
        CoreInstance result = execute("testFunc():Any[*]");
        Assert.assertEquals("1", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testMultipleUnitRelatedPropertiesListedInClassProperties()
    {
        String massAndClassDefinition =
                massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "   myKilo: pkg::Mass~Kilogram[1];\n" +
                        "   mySecondKilo: pkg::Mass[1];\n" +
                        "   myUnit: Unit[1];\n" +
                        "   myMeasure: Measure[1];\n" +
                        "}\n";

        compileTestSource("testModel.pure", massAndClassDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Any[*]\n" +
                        "{\n" +
                        "   A.properties->size();\n" +
                        "}\n");
        CoreInstance result = execute("testFunc():Any[*]");
        Assert.assertEquals("4", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testUnitPropertyAddAnotherUnit()
    {
        String massAndClassDefinition =
                massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "   myKilo: pkg::Mass~Kilogram[1];\n" +
                        "}\n";

        compileTestSource("testModel.pure", massAndClassDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        plusFunction +
                        "function testFunc():Mass~Gram[1]\n" +
                        "{\n" +
                        "   ^A(myKilo=3 Mass~Kilogram).myKilo + 1.5 Mass~Pound; \n" +
                        "}\n");
        CoreInstance result = execute("testFunc():Mass~Gram[1]");
        Assert.assertTrue(Measure.isUnitOrMeasureInstance(result, processorSupport));
        Assert.assertEquals("Mass~Gram", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
        Assert.assertEquals("3680.385", result.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testClassPropertiesListCanBeInstantiatedWithUnitProperty()
    {
        String massAndClassDefinition =
                massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "   myKilo: pkg::Mass~Kilogram[1];\n" +
                        "}\n";

        compileTestSource("testModel.pure", massAndClassDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testFunc():Any[*]\n" +
                        "{\n" +
                        "   ^List<Property<Nil,Any|*>>(values=A.properties);\n" +
                        "}\n");
        CoreInstance result = execute("testFunc():Any[*]");
        Assert.assertEquals("List<Property<Nil, Any|*>>", GenericType.print(result.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
    }

    @Test
    public void testKilogramInstanceMatchesKilogram()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testKilogramMatchesKilogram():Integer[1]\n" +
                        "{\n" +
                        "   let x = 5 Mass~Kilogram;\n" +
                        "   $x->match([m:Mass~Kilogram[1] | 1]);\n" +
                        "}");
        CoreInstance result = execute("testKilogramMatchesKilogram():Integer[1]");
        Assert.assertEquals("1", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramInstanceMatchesKilogramMultiplicityMany()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testKilogramMatchesKilogram():Integer[1]\n" +
                        "{\n" +
                        "   let x = [5 Mass~Kilogram, 10 Mass~Kilogram];\n" +
                        "   $x->match([m:Mass~Kilogram[*] | 1]);\n" +
                        "}");
        CoreInstance result = execute("testKilogramMatchesKilogram():Integer[1]");
        Assert.assertEquals("1", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramInstanceMatchesMass()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testKilogramMatchesMass():Integer[1]\n" +
                        "{\n" +
                        "   let x = 5 Mass~Kilogram;\n" +
                        "   $x->match([m:Mass[1] | 1]);\n" +
                        "}");
        CoreInstance result = execute("testKilogramMatchesMass():Integer[1]");
        Assert.assertEquals("1", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }

    @Test
    public void testKilogramInstanceMatchesMassMultiplicityMany()
    {
        compileTestSource("testModel.pure", massDefinition);
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function testKilogramMatchesMass():Integer[1]\n" +
                        "{\n" +
                        "   let x = [5 Mass~Kilogram, 10 Mass~Kilogram];\n" +
                        "   $x->match([m:Mass[*] | 1]);\n" +
                        "}");
        CoreInstance result = execute("testKilogramMatchesMass():Integer[1]");
        Assert.assertEquals("1", result.getValueForMetaPropertyToOne(M3Properties.values).getName());
    }
}