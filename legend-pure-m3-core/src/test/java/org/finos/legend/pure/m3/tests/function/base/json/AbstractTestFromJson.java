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

package org.finos.legend.pure.m3.tests.function.base.json;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public abstract class AbstractTestFromJson extends AbstractPureTestWithCoreCompiled
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private void runShouldFailTestCase(String testName, String expectedType, String actualJson, String expectedExceptionSnippet)
    {
        this.runShouldFailTestCase(testName, expectedType, actualJson, expectedExceptionSnippet, "");
    }

    private void assertException(PureException e, String expectedInfo)
    {
        // The cause of the exception should be considered the root level fromJson call so that it contains all of the information about the nested JSON structure.
        assertEquals(e.getInfo(), e.getOriginatingPureException().getInfo());
        assertEquals(expectedInfo, e.getInfo());
    }

    /**
     * Ensure that assigning a json value of one type to a pure property of another, incompatible, type throws an exception.
     *
     * @param testName                 Unique name for the test case
     * @param expectedType             Expected PURE type (with multiplicity)
     * @param actualJson               Example of JSON type
     * @param expectedExceptionSnippet Expected-found snippet of exception e.g. "Expected Boolean, found String"
     */
    private void runShouldFailTestCase(String testName, String expectedType, String actualJson, String expectedExceptionSnippet, String additionalPureCode)
    {
        String[] rawSource = {
                "import meta::json::*;\nimport meta::pure::functions::json::tests::*;",
                additionalPureCode,
                "Class meta::pure::functions::json::tests::" + testName,
                "{\n  testField : " + expectedType + ";\n}",
                "function " + testName + "():Any[*]",
                "{\n  let json = '{\"testField\": " + actualJson + "}';",
                "  $json -> fromJson(" + testName + ", ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        try
        {
            this.compileTestSource(testName + CodeStorage.PURE_FILE_EXTENSION, source);
            CoreInstance func = this.runtime.getFunction(testName + "():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            String exceptionDetails = "".equals(expectedExceptionSnippet) ? "" : ": \n" + expectedExceptionSnippet;
            this.assertException(e, "Error populating property 'testField' on class 'meta::pure::functions::json::tests::" + testName + "'" + exceptionDetails);
            runtime.delete(testName + CodeStorage.PURE_FILE_EXTENSION);
        }
    }

    private void runShouldPassTestCase(String testName, String expectedType, String actualJson, String additionalPureCode, String result)
    {
        String[] rawSource = {
                "import meta::json::*;\nimport meta::pure::functions::json::tests::*;",
                additionalPureCode,
                "Class meta::pure::functions::json::tests::" + testName,
                "{\n  testField : " + expectedType + ";\n}",
                "function " + testName + "():Any[*]",
                "{\n  let json = '{\"testField\": " + actualJson + "}';",
                "     let jsonAsPure = $json -> fromJson(" + testName + ", ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "     assertEquals(" + result + ", $jsonAsPure.testField, 'Output does match expected');",
                "\n}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource(testName + CodeStorage.PURE_FILE_EXTENSION, source);
        CoreInstance func = this.runtime.getFunction(testName + "():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
        runtime.delete(testName + CodeStorage.PURE_FILE_EXTENSION);
    }

    @Test
    public void typeCheck_JsonStringToBooleanProperty()
    {
        this.runShouldFailTestCase("StringToBoolean", "Boolean[1]", "\"foo\"",
                "Expected Boolean, found String");
    }

    @Test
    public void typeCheck_JsonBooleanToStringProperty()
    {
        this.runShouldFailTestCase("BooleanToString", "String[1]", "true",
                "Expected String, found Boolean");
    }

    @Test
    public void typeCheck_JsonStringToFloatProperty()
    {
        this.runShouldFailTestCase("StringToFloat", "Float[1]", "\"3.0\"",
                "Expected Number, found String");
    }

    @Test
    public void typeCheck_JsonFloatToIntegerProperty()
    {
        this.runShouldFailTestCase("FloatToInteger", "Integer[1]", "3.0",
                "Expected Integer, found Float");
    }

    @Test
    public void typeCheck_JsonIntegerToDateProperty()
    {
        this.runShouldFailTestCase("IntegerToDate", "Date[1]", "3",
                "Expected Date, found Integer");
    }

    @Test
    public void typeCheck_JsonStringToDateProperty()
    {
        this.runShouldFailTestCase("StringToDate", "Date[1]", "\"foo\"",
                "Expected Date, found String");
    }

    @Test
    public void typeCheck_JsonIntegerToEnumProperty()
    {
        this.runShouldFailTestCase("IntegerToEnum", "meta::pure::functions::json::tests::TestingEnum[1]",
                "2",
                "Expected meta::pure::functions::json::tests::TestingEnum, found Integer",
                "Enum meta::pure::functions::json::tests::TestingEnum { Value1, Value2 } ");
    }

    @Test
    public void typeCheck_JsonStringToEnumProperty()
    {
        this.runShouldFailTestCase("StringToEnum", "meta::pure::functions::json::tests::TestingEnum[1]",
                "\"foo\"",
                "Unknown enum: meta::pure::functions::json::tests::TestingEnum.foo",
                "Enum meta::pure::functions::json::tests::TestingEnum { Value1, Value2 } ");
    }

    @Test
    public void typeCheck_JsonIntegerToObjectProperty()
    {
        this.runShouldPassTestCase("IntegerToObject", "meta::pure::functions::json::tests::someClass[1]",
                "3",
                "Class meta::pure::functions::json::tests::someClass {  } ",
                "[]"
        );
    }

    @Test
    public void typeCheck_JsonObjectToIntegerProperty()
    {
        this.runShouldFailTestCase("ObjectToInteger", "Integer[1]",
                "{}",
                "Expected Integer, found JSON Object");
    }

    @Test
    public void typeCheck_JsonIntegerToFloatProperty()
    {
        this.runShouldPassTestCase("IntegerToFloat", "Float[1]",
                "1", "", "1.0");
    }

    @Test
    public void typeCheck_JsonIntegerToAnyProperty()
    {
        this.runShouldPassTestCase("IntegerToAny", "Any[1]",
                "1", "", "1");
    }

    @Test
    public void typeCheck_JsonFloatToAnyProperty()
    {
        this.runShouldPassTestCase("FloatToAny", "Any[1]",
                "2.0", "", "2.0");
    }

    @Test
    public void typeCheck_JsonStringToAnyProperty()
    {
        this.runShouldPassTestCase("StringToAny", "Any[1]",
                "\"Hello\"", "", "'Hello'");
    }

    @Test
    public void typeCheck_JsonBooleanToAnyProperty()
    {
        this.runShouldPassTestCase("BooleanToAny", "Any[1]",
                "true", "", "true");
    }

    @Test
    public void typeCheck_JsonObjectToAnyProperty()
    {
        this.runShouldFailTestCase("ObjectToAny", "Any[1]",
                "{}", "Deserialization of Any currently only supported on primitive values!");
    }

    @Test
    public void typeCheck_JsonFloatToDecimalProperty()
    {
        this.runShouldPassTestCase("FloatToDecimal", "Decimal[1]",
                "3.14", "", "3.14D");
    }

    @Test
    public void typeCheck_JsonIntegerToDecimalProperty()
    {
        this.runShouldPassTestCase("IntegerToDecimal", "Decimal[1]",
                "3", "", "3D");
    }

    @Test
    public void typeCheck_JsonStringToDecimalProperty()
    {
        this.runShouldFailTestCase("StringToDecimal", "Float[1]", "\"3.0\"",
                "Expected Number, found String");
    }

    @Test
    public void multiplicityIsInRange_TwoWhenExpectingThree()
    {
        this.runShouldFailTestCase("TwoToThree", "Integer[3]",
                "[1,2]",
                "Expected value(s) of multiplicity [3], found 2 value(s).");
    }

    @Test
    public void multiplicityIsInRange_FourWhenExpectingThree()
    {
        this.runShouldFailTestCase("FourToThree", "Integer[3]",
                "[1,2,3,4]",
                "Expected value(s) of multiplicity [3], found 4 value(s).");
    }

    @Test
    public void multiplicityIsInRange_TwoWhenExpectingOne()
    {
        this.runShouldFailTestCase("TwoToOne", "Integer[1]",
                "[1,2]",
                "Expected value(s) of multiplicity [1], found 2 value(s).");
    }

    @Test
    public void multiplicityIsInRange_OneWhenExpectingTwo()
    {
        this.runShouldFailTestCase("OneToTwo", "Integer[2]",
                "1",
                "Expected value(s) of multiplicity [2], found 1 value(s).");
    }

    @Test
    public void multiplicityIsInRange_NullToSingletonProperty()
    {
        this.runShouldFailTestCase("NullToSingleton", "Float[1]",
                "null",
                "Expected value(s) of multiplicity [1], found 0 value(s).",
                "");
    }

    @Test
    public void multiplicityIsInRange_EmptyArrayToSingletonProperty()
    {
        this.runShouldFailTestCase("EmptyToSingleton", "Float[1]",
                "[]",
                "Expected value(s) of multiplicity [1], found 0 value(s).",
                "");
    }

    @Test
    public void multiplicityIsInRange_SingleValueToSingletonProperty()
    {
        this.runShouldPassTestCase("SingleToSingleton", "Float[1]",
                "3.0", "", "3.0");
    }

    @Test
    public void multiplicityIsInRange_SingleArrayToSingletonProperty()
    {
        this.runShouldPassTestCase("SingleArrayToSingleton", "Float[1]",
                "[3.0]", "", "3.0");
    }

    @Test
    public void multiplicityIsInRange_NullToOptionalProperty()
    {
        this.runShouldPassTestCase("NullToOptional", "Float[0..1]",
                "null", "", "[]");
    }

    @Test
    public void multiplicityIsInRange_EmptyArrayToOptionalProperty()
    {
        this.runShouldPassTestCase("EmptyToOptional", "Float[0..1]",
                "[]", "", "[]");
    }

    @Test
    public void multiplicityIsInRange_NullToManyProperty()
    {
        this.runShouldPassTestCase("NullToMany", "Float[*]",
                "null", "", "[]");
    }

    @Test
    public void multiplicityIsInRange_EmptyArrayToManyProperty()
    {
        this.runShouldPassTestCase("EmptyToMany", "Float[*]",
                "[]", "", "[]");
    }

    @Test
    public void multiplicityIsInRange_SingleValueToManyProperty()
    {
        this.runShouldPassTestCase("SingleToMany", "Float[*]",
                "3.0", "", "[3.0]");
    }

    @Test
    public void multiplicityIsInRange_SingleArrayToManyProperty()
    {
        this.runShouldPassTestCase("SingleArrayToMany", "Float[*]",
                "[3.0]", "", "[3.0]");
    }


    @Test
    public void multiplicityIsInRange_Association()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::Employment",
                "{",
                "employer : Firm[1];",
                "employees : Person[7];",
                "}",
                "Class meta::pure::functions::json::tests::Person {}",
                "Class meta::pure::functions::json::tests::Firm {}",
                "function Association():Any[*]",
                "{",
                // this json describes a Firm with one employee, but the association states all Firms must have exactly 7 employees.
                "let json = '{\"employees\":[{\"employer\":{\"employees\":[{}]}}]}';",
                "$json -> fromJson(Firm, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", associationSource);
            CoreInstance func = this.runtime.getFunction("Association():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + associationSource);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'employees' on class 'meta::pure::functions::json::tests::Firm': \nExpected value(s) of multiplicity [7], found 1 value(s).");
        }
    }

    @Test
    public void multiplicityIsInRange_Association_nestedClasses()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::Employment",
                "{",
                "employer : Firm[1];",
                "employees : Person[7];",
                "}",
                "Class meta::pure::functions::json::tests::Person {}",
                "Class meta::pure::functions::json::tests::Firm {}",
                "Class meta::pure::functions::json::tests::OfficeBuilding {",
                "firms: Firm[1];",
                "}",
                "function Association():Any[*]",
                "{",
                "let json = '{\"firms\": {\"employees\":[{\"employer\":{\"employees\":{}}}]}}';",
                "$json -> fromJson(OfficeBuilding, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", associationSource);
            CoreInstance func = this.runtime.getFunction("Association():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + associationSource);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'firms' on class 'meta::pure::functions::json::tests::OfficeBuilding': \nError populating property 'employees' on class 'meta::pure::functions::json::tests::Firm': \nExpected value(s) of multiplicity [7], found 1 value(s).");
        }
    }

    @Test
    public void multiplicityIsInRange_Association_multipleAssociations()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::Employment",
                "{",
                "employer : Firm[1];",
                "employees : Person[7];",
                "}",
                "Association meta::pure::functions::json::tests::Occupancy",
                "{",
                "building : OfficeBuilding[1];",
                "occupant : Firm[1];",
                "}",
                "Class meta::pure::functions::json::tests::Person {}",
                "Class meta::pure::functions::json::tests::Firm {}",
                "Class meta::pure::functions::json::tests::OfficeBuilding {}",
                "function Association():Any[*]",
                "{",
                "let json = '{\"occupant\": {\"building\": {\"occupant\": {}}, \"employees\":[{\"employer\":{\"employees\":{}}}]}}';",
                "$json -> fromJson(OfficeBuilding, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", associationSource);
            CoreInstance func = this.runtime.getFunction("Association():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + associationSource);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'occupant' on class 'meta::pure::functions::json::tests::OfficeBuilding': \n" +
                    "Error populating property 'employees' on class 'meta::pure::functions::json::tests::Firm': \n" +
                    "Expected value(s) of multiplicity [7], found 1 value(s).");
        }
    }

    @Test
    public void multiplicityIsInRange_Association_jsonLooksLikeAssociation()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::Foo {" +
                        "bar : Bar[1];\n" +
                        "check : String[1];\n" +
                        "}",
                "Class meta::pure::functions::json::tests::Bar {" +
                        "foo : Foo[*];\n" +
                        "}",
                "function mimic():Any[*]",
                "{",
                "let json = '{\"check\": \"passes here\", \"bar\": {\"foo\": {\"check\": \"and here\", \"bar\": {}}}}';",
                "$json -> fromJson(Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("mimic():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void multiplicityIsInRange_Association_inferredCycleFromModel()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::Occupancy",
                "{",
                "building : OfficeBuilding[1];",
                "occupant : Firm[1];",
                "}",
                "Class meta::pure::functions::json::tests::Firm {}",
                "Class meta::pure::functions::json::tests::OfficeBuilding {}",
                "Class meta::pure::functions::json::tests::Campus {" +
                        "buildings : OfficeBuilding[1];" +
                        "}",
                "function foo():Any[*]",
                "{",
                "let json = '{\"buildings\": {\"occupant\": {\"building\": {}}}}';",
                "$json -> fromJson(Campus, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void propertiesFromAssociationOnSuperclass()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::SuperclassAssoc",
                "{",
                "a : A[1];",
                "b : B[1];",
                "}",
                "Class meta::pure::functions::json::tests::A {",
                "   str : String[1];",
                "}",
                "Class meta::pure::functions::json::tests::B {}",
                "Class meta::pure::functions::json::tests::C extends A {}",
                "function foo():Any[*]",
                "{",
                "let json = '{\"str\": \"bar\", \"b\": {\"a\": {\"str\": \"foo\"}}}';",
                "let o = $json -> fromJson(C, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "assert($o.str == 'bar', |'');",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void propertiesFromAssociationOnSuperclass_NestedObjectHasSuperfluous()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::SuperclassAssoc",
                "{",
                "a : A[1];",
                "b : B[1];",
                "}",
                "Class meta::pure::functions::json::tests::A {}",
                "Class meta::pure::functions::json::tests::B {}",
                "Class meta::pure::functions::json::tests::C extends A {",
                "   str : String[1];",
                "}",
                "function foo():Any[*]",
                "{",
                "let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true);\n",
                "let json = '{\"str\": \"foo\", \"b\": {\"a\": {\"str\": \"foo\"}}}';",
                "let o = $json -> fromJson(C, $config);",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", associationSource);
            CoreInstance func = this.runtime.getFunction("foo():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + associationSource);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'b' on class 'meta::pure::functions::json::tests::C': \n" +
                    "Error populating property 'a' on class 'meta::pure::functions::json::tests::B': \n" +
                    "Property 'str' can't be found in class meta::pure::functions::json::tests::A. ");
        }
    }

    @Test
    public void multiplicityIsInRange_toOne_missing()
    {
        String[] rawInRangeToOneSources = {
                "import meta::json::*;\nimport meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::must",
                "{\n  testField : String[1];\n}",
                "function InRangeToOne():Any[*]",
                "{\n  let json = '{}';",
                "  $json -> fromJson(must, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n}"
        };
        String inRangeToOneSources = StringUtils.join(rawInRangeToOneSources, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", inRangeToOneSources);
            CoreInstance func = this.runtime.getFunction("InRangeToOne():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + inRangeToOneSources);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'testField' on class 'meta::pure::functions::json::tests::must': \nExpected value(s) of multiplicity [1], found 0 value(s).");
        }
    }

    @Test
    public void multiplicityIsInRange_givenValueExpectingArray()
    {
        this.runShouldFailTestCase("givenValueExpectingArray", "String[2]", "\"foo\"",
                "Expected value(s) of multiplicity [2], found 1 value(s).",
                "");
    }

    @Test
    public void failOnMissingField()
    {
        String[] rawSource = {
                "import meta::json::*;\nimport meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::MissingData",
                "{\n  missing : Integer[1];\n}",
                "function missing():Any[*]",
                "{\n  let json = '{}';",
                "  $json -> fromJson(MissingData, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", source);
            CoreInstance func = this.runtime.getFunction("missing():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'missing' on class 'meta::pure::functions::json::tests::MissingData': \nExpected value(s) of multiplicity [1], found 0 value(s).");
        }
    }

    @Test
    public void deserializationConfig_failOnUnknownProperties()
    {
        String[] rawSource = {
                "import meta::json::*;\nimport meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::failUnknown",
                "{\n  testField : Integer[1];\n}",
                "function failUnknown():Any[*]",
                "{\n  let json = '{\"testField\": 1,\"secondProperty\":2}';",
                "let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true);\n",
                "  $json -> fromJson(failUnknown, $config);\n}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", source);
            CoreInstance func = this.runtime.getFunction("failUnknown():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Property 'secondProperty' can't be found in class meta::pure::functions::json::tests::failUnknown. ");
        }
    }

    @Test
    public void deserializationConfig_wrongTypeKeyName()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;\nimport meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::failUnknown",
                "{\n  testField : Integer[1];\n}",
                "function TypeKey():Any[*]",
                "{\n  let json = '{\"testField\": 1,\"__TYPE\":\"failUnknown\"}';",
                "let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true);\n",
                "  $json -> fromJson(failUnknown, $config);\n}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        try
        {
            this.compileTestSource("fromString.pure", associationSource);
            CoreInstance func = this.runtime.getFunction("TypeKey():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + associationSource);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Property '__TYPE' can't be found in class meta::pure::functions::json::tests::failUnknown. ");
        }
    }

    @Test
    public void testFromJsonThrowsValidationErrors()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;",
                    "Class meta::pure::functions::json::tests::A",
                    "[ TEST_CONTROL: $this.a == 'dave' ]",
                    "{ a:String[1]; }",
                    "",
                    "function go():Any[*]",
                    "{ let json='{ \"a\": \"fred\" }'->meta::json::fromJson(meta::pure::functions::json::tests::A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                    " assert(!$json->isEmpty(), |''); ",
                    " assert(\'fred\' == $json.a, |''); ",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Could not create new instance of meta::pure::functions::json::tests::A: \nConstraint :[TEST_CONTROL] violated in the Class A");
        }
    }

    @Test
    public void testFromJsonDoesNotThrowValidationErrors()
    {
        String[] source = {
                "import meta::json::*;",
                "function myFunc(o:Any[1]):Any[1]\n",
                "{\n",
                "   $o;\n",
                "}\n",
                "Class meta::pure::functions::json::tests::A",
                "[ TEST_CONTROL: $this.a == 'dave' ]",
                "{ a:String[1]; }",
                "",
                "function go():Any[*]",
                "{\n" +
                        " let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true, constraintsHandler=^ConstraintsOverride(constraintsManager=myFunc_Any_1__Any_1_));\n",
                " let json='{ \"a\": \"fred\" }'->meta::json::fromJson(meta::pure::functions::json::tests::A, $config);",
                " assert(!$json->isEmpty(), |''); ",
                " assert(\'fred\' == $json.a, |''); ",
                "}"
        };

        this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void testFromJsonThrowsValidationErrors_ConstraintOnRoot()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;",
                    "Class meta::pure::functions::json::tests::A {}",
                    "Class meta::pure::functions::json::tests::B",
                    "[ TEST_CONTROL: $this.str == 'dave' ]",
                    "{ ",
                    "   a:meta::pure::functions::json::tests::A[1]; ",
                    "   str:String[1];",
                    "}",
                    "function go():Any[*]",
                    "{ let json='{ \"str\": \"fred\", \"a\": {} }'->meta::json::fromJson(meta::pure::functions::json::tests::B, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                    " assert(!$json->isEmpty(), |''); ",
                    " assert(\'fred\' == $json.str, |''); ",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Could not create new instance of meta::pure::functions::json::tests::B: \nConstraint :[TEST_CONTROL] violated in the Class B");
        }
    }

    @Test
    public void testFromJsonThrowsValidationErrors_ConstraintOnProperty()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;",
                    "Class meta::pure::functions::json::tests::A",
                    "[ TEST_CONTROL: $this.a == 'dave' ]",
                    "{ a:String[1]; }",
                    "Class meta::pure::functions::json::tests::B",
                    "{ b:meta::pure::functions::json::tests::A[1]; }",
                    "function go():Any[*]",
                    "{ let json='{ \"b\": {\"a\": \"fred\" } }'->meta::json::fromJson(meta::pure::functions::json::tests::B, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                    " assert(!$json->isEmpty(), |''); ",
                    " assert(\'fred\' == $json.b.a, |''); ",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'b' on class 'meta::pure::functions::json::tests::B': \nCould not create new instance of meta::pure::functions::json::tests::A: \nConstraint :[TEST_CONTROL] violated in the Class A");
        }
    }

    @Test
    public void testFromJsonThrowsValidationErrors_RootConstraintOnPropertyClass()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;",
                    "Class meta::pure::functions::json::tests::A",
                    "{ a:String[1]; }",
                    "Class meta::pure::functions::json::tests::B",
                    "[ TEST_CONTROL: $this.b.a == 'dave' ]",
                    "{ b:meta::pure::functions::json::tests::A[1]; }",
                    "function go():Any[*]",
                    "{\n",
                    "let json='{ \"b\": {\"a\": \"fred\" } }'->meta::json::fromJson(meta::pure::functions::json::tests::B, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                    " assert(!$json->isEmpty(), |''); ",
                    " assert(\'fred\' == $json.b.a, |''); ",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Could not create new instance of meta::pure::functions::json::tests::B: \nConstraint :[TEST_CONTROL] violated in the Class B");
        }
    }

    @Test
    public void testFromJsonThrowsValidationErrors_Association()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;",
                    "Class meta::pure::functions::json::tests::a",
                    "{ string : String[1]; }",
                    "Class meta::pure::functions::json::tests::b",
                    "[ TEST_CONTROL: $this.A.string == 'fred' ]",
                    "{ string : String[1]; }",
                    "Association meta::pure::functions::json::tests::Assoc",
                    "{",
                    "   A : meta::pure::functions::json::tests::a[1];",
                    "   B : meta::pure::functions::json::tests::b[1];",
                    "}",
                    "function go():Any[*]",
                    "{\n",
                    " let json='{ \"string\": \"dave\", \"B\": {\"string\": \"fred\", \"A\": { \"string\": \"dave\" } } }'->meta::json::fromJson(meta::pure::functions::json::tests::a, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'B' on class 'meta::pure::functions::json::tests::a': \nCould not create new instance of meta::pure::functions::json::tests::b: \nConstraint :[TEST_CONTROL] violated in the Class b");
        }
    }

    @Test
    public void recursiveStructures()
    {
        String basePattern = "{\"INT\": 1}";
        String recursivePattern = "{\"INT\": 0, \"FOO\": %s}";
        String json = recursivePattern;
        int recursiveDepth = 2;
        for (int i = 0; i < recursiveDepth - 1; i++)
        {
            json = String.format(json, recursivePattern);
        }
        json = String.format(json, basePattern);


        String foo = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::Foo\n" +
                "{\n" +
                "    FOO : Foo[0..1];\n" +
                "    INT : Integer[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "    '" + json + "' -> fromJson(Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";

        this.compileTestSource("fromString.pure", StringUtils.join(foo, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void idConflicts()
    {
        String idKeysSource = StringUtils.join(new String[]{
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::Foo {",
                "   FOO : Foo[1];",
                "   str : String[1];",
                "}",
                "function foo():Any[*]",
                "{",
                "   let fromJson = '{\"@id\": 1, \"str\": \"one\", \"FOO\": {\"@id\": 1, \"str\": \"two\", \"FOO\": 1}}' -> fromJson(Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "   assert($fromJson.FOO.FOO == [], |'');",
                "}"
        }, "\n") + "\n";

        this.compileTestSource("fromString.pure", idKeysSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void idKeysWithAssociation()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::_assoc",
                "{",
                "a : _A[1];",
                "b : _B[1];",
                "}",
                "Class meta::pure::functions::json::tests::_A {",
                "str : String[1];",
                "}",
                "Class meta::pure::functions::json::tests::_B {}",
                "function foo():Any[*]",
                "{",
                "let json = '{\"@id\": 1, \"str\": \"foo\", \"b\": {\"a\": 1}}';",
                "let o = $json -> fromJson(_A, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "let _b = $o.b;",
                "assert($_b == $_b.a.b, |'');",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void idKeysWithExplicitReferences()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::ClassOne {",
                "two : ClassTwo[1];",
                "str : String[1];",
                "}",
                "Class meta::pure::functions::json::tests::ClassTwo {",
                "   one : ClassOne[1];",
                "}",
                "function foo():Any[*]",
                "{",
                "let json = '{\"@id\": 1, \"str\": \"foo\", \"two\": {\"one\": 1}}';",
                "let one = $json -> fromJson(ClassOne, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "let two = $one.two;",
                "assert($two.one == [], |'');",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void regularAssociation()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Association meta::pure::functions::json::tests::assoc",
                "{",
                "a : A[1];",
                "b : B[1];",
                "}",
                "Class meta::pure::functions::json::tests::A {",
                "str : String[1];",
                "}",
                "Class meta::pure::functions::json::tests::B {}",
                "function foo():Any[*]",
                "{",
                "let json = '{\"str\": \"foo\", \"b\": {}}';",
                "let o = $json -> fromJson(A, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "let _b = $o.b -> toOne();",
                "assert($_b == $_b.a.b, |'');",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void typeResolving_jsonArrayOfDifferentTypes()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::Parent {" +
                        "   a : A[2];" +
                        "}",
                "Class meta::pure::functions::json::tests::A {}",
                "Class meta::pure::functions::json::tests::B extends A ",
                "{",
                "   float : Float[1];",
                "}",
                "Class meta::pure::functions::json::tests::C extends A ",
                "{",
                "   string : String[1];",
                "}",
                "function foo():Any[*]",
                "{",
                "let json = '{\"a\": [{\"@type\": \"B\", \"float\": 4167}, {\"@type\": \"C\", \"string\": \"foo\"}]}';",
                "let o = $json -> fromJson(Parent, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "assert(4167.0 == $o.a -> first() -> cast(@B).float, |'');",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void typeResolving_customTypeKeyName()
    {
        String[] rawAssociationSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::B",
                "{",
                "   float : Float[1];",
                "}",
                "function foo():Any[*]",
                "{",
                " let config = ^JSONDeserializationConfig(typeKeyName='__TYPE', failOnUnknownProperties=true);\n",
                "let json = '{\"__TYPE\": \"B\", \"float\": 4167}';",
                "let o = $json -> fromJson(B, $config);",
                "assert(4167.0 == $o.float, |'');",
                "}"
        };
        String associationSource = StringUtils.join(rawAssociationSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", associationSource);
        CoreInstance func = this.runtime.getFunction("foo():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void typeResolving_noValidType()
    {
        String[] rawSource = {
                "import meta::json::*;",
                "import meta::pure::functions::json::tests::*;",
                "Class meta::pure::functions::json::tests::Parent {" +
                        "   a : A[1];" +
                        "}",
                "Class meta::pure::functions::json::tests::A {}",
                "Class meta::pure::functions::json::tests::B extends A {}",
                "function go():Any[*]",
                "{",
                "let json = '{\"a\": {\"float\": 4167}}';",
                "let o = $json -> fromJson(Parent, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void missingData()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;",
                    "Class meta::pure::functions::json::tests::a",
                    "{ string : String[1]; }",
                    "function go():Any[*]",
                    "{\n",
                    " let json='{}'->fromJson(meta::pure::functions::json::tests::a, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'string' on class 'meta::pure::functions::json::tests::a': \nExpected value(s) of multiplicity [1], found 0 value(s).");
        }
    }

    @Test
    public void oneAndMany()
    {
        String[] source = {
                "import meta::json::*;",
                "Class meta::pure::functions::json::tests::a",
                "{ string : String[1..*]; }",
                "function go():Any[*]",
                "{\n",
                " let json='{\"string\": [\"foo\", \"bar\"]}'->fromJson(meta::pure::functions::json::tests::a, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };

        this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void specifyingTheExactTypeGivenInTheModel()
    {
        String[] source = {
                "import meta::json::*;",
                "Class meta::pure::functions::json::tests::Foo",
                "{}",
                "function go():Any[*]",
                "{\n",
                "  '{\"@type\": \"Foo\"}'->fromJson(meta::pure::functions::json::tests::Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };

        this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void passingEnum()
    {
        String[] source = {
                "import meta::json::*;",
                "Enum MyEnum { Foo, Bar }",
                "Class meta::pure::functions::json::tests::Foo",
                "{",
                "e : MyEnum[1];",
                "}",
                "function go():Any[*]",
                "{\n",
                "  '{\"e\": \"Foo\"}'->fromJson(meta::pure::functions::json::tests::Foo, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));",
                "}"
        };

        this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void invalidTypeSpecified()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;\n",
                    "Class meta::json::test::Foo {}\n",
                    "Class meta::json::test::Bar {}\n",
                    "function go():Any[*]\n",
                    "{\n",
                    "'{\"@type\": \"Foo\"}'->fromJson(meta::json::test::Bar, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Could not find a sub-type of \"meta::json::test::Bar\" with name \"Foo\".");
        }
    }

    @Test
    public void multipleObjectsString()
    {
        try
        {
            String[] source = {
                    "import meta::json::*;\n",
                    "Class meta::json::test::Foo {}\n",
                    "function go():Any[*]\n",
                    "{\n",
                    "'[]'->fromJson(meta::json::test::Foo, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n",
                    "}"
            };

            this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
            CoreInstance func = this.runtime.getFunction("go():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + source);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Can only deserialize root-level JSONObjects i.e. serialized single instances of PURE classes. Cannot deserialize collections of multiple PURE objects.");
        }
    }


    @Test
    public void testLookup()
    {
        String[] source = {
                "import meta::json::*;\n",
                "Class meta::json::test::Foo" +
                        "{" +
                        "  name : String[1];" +
                        "}\n",
                "function go():Any[*]\n",
                "{\n",
                "'{\"_type\":\"z\",\"name\":\"bla\"}'->fromJson(meta::json::test::Foo, ^meta::json::JSONDeserializationConfig(failOnUnknownProperties=true, typeKeyName='_type', typeLookup = [pair('z','meta::json::test::Foo')]));\n",
                "}"
        };

        this.compileTestSource("fromString.pure", StringUtils.join(source, "\n") + "\n");
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());

    }

    @Test
    public void testDeserializeUnitInstanceAsClassProperty()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        this.compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[1];\n" +
                        "}\n" +
                        "function testUnitToJson():Any[*]\n" +
                        "{\n" +
                        "   let res ='{\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5.5}}'\n->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                        "   $res.myWeight;\n" +
                        "}\n");

        CoreInstance func = this.runtime.getFunction("testUnitToJson():Any[*]");
        CoreInstance result = this.functionExecution.start(func, FastList.<CoreInstance>newList());
        Assert.assertTrue("Mass~Kilogram".equals(GenericType.print(result.getValueForMetaPropertyToOne("genericType"), this.processorSupport)));
        Assert.assertEquals("5.5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeUnitInstanceAsClassPropertyMany()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        this.compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[*];\n" +
                        "}\n" +
                        "function testUnitToJson():Any[*]\n" +
                        "{\n" +
                        "   let res ='{\"myWeight\":[{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5},{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":1}]}'\n->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                        "   $res.myWeight->at(0);\n" +
                        "}\n");

        CoreInstance func = this.runtime.getFunction("testUnitToJson():Any[*]");
        CoreInstance result = this.functionExecution.start(func, FastList.<CoreInstance>newList());
        Assert.assertTrue("Mass~Kilogram".equals(GenericType.print(result.getValueForMetaPropertyToOne("genericType"), this.processorSupport)));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeUnitInstanceAsSuperTypeProperty()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        this.compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass[1];\n" +
                        "}\n" +
                        "function testUnitToJson():Any[*]\n" +
                        "{\n" +
                        "   let res ='{\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5.5}}'\n->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                        "   $res.myWeight;\n" +
                        "}\n");

        CoreInstance func = this.runtime.getFunction("testUnitToJson():Any[*]");
        CoreInstance result = this.functionExecution.start(func, FastList.<CoreInstance>newList());
        Assert.assertTrue("Mass~Kilogram".equals(GenericType.print(result.getValueForMetaPropertyToOne("genericType"), this.processorSupport)));
        Assert.assertEquals("5.5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeClassWithOptionalUnitPropertyNotRemoved()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        this.compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[1];\n" +
                        "myOptionalWeight : Mass~Kilogram[0..1];\n" +
                        "}\n" +
                        "function testUnitToJsonWithType():Any[*]\n" +
                        "{\n" +
                        "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5.5}, \"myOptionalWeight\":[]}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                        "   $res.myWeight;\n" +
                        "}\n");

        CoreInstance funcTwo = this.runtime.getFunction("testUnitToJsonWithType():Any[*]");
        CoreInstance resultTwo = this.functionExecution.start(funcTwo, FastList.<CoreInstance>newList());
        Assert.assertTrue("Mass~Kilogram".equals(GenericType.print(resultTwo.getValueForMetaPropertyToOne("genericType"), this.processorSupport)));
        Assert.assertEquals("5.5", resultTwo.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeWrongUnitTypeInJsonThrowsError()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

       String testSourceStr = "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[1];\n" +
                        "}\n" +
                        "function testUnitToJsonWithType():Any[*]\n" +
                        "{\n" +
                        "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"badtype\",\"exponentValue\":1}],\"value\":5.5}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                        "   $res.myWeight;\n" +
                        "}\n";

        try
        {
            this.compileTestSource("fromString.pure", testSourceStr);
            CoreInstance func = this.runtime.getFunction("testUnitToJsonWithType():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + testSourceStr);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'myWeight' on class 'A': \n" +
                    "Could not create new instance of Unit");
        }
    }

    @Test
    public void testDeserializeWrongUnitValueTypeInJsonThrowsError()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        String testSourceStr = "import pkg::*;\n" +
                massDefinition +
                "Class A\n" +
                "{\n" +
                "myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":\"5.5\"}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight;\n" +
                "}\n";

        try
        {
            this.compileTestSource("fromString.pure", testSourceStr);
            CoreInstance func = this.runtime.getFunction("testUnitToJsonWithType():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + testSourceStr);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'myWeight' on class 'A': \n" +
                    "Value from unitValue field must be of Number type, getting java.lang.String type instead.");
        }
    }

    @Test
    public void testDeserializeNonOneExponentInJsonThrowsError()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        String testSourceStr = "import pkg::*;\n" +
                massDefinition +
                "Class A\n" +
                "{\n" +
                "myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":3}],\"value\":5}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";

        try
        {
            this.compileTestSource("fromString.pure", testSourceStr);
            CoreInstance func = this.runtime.getFunction("testUnitToJsonWithType():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + testSourceStr);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'myWeight' on class 'A': \n" +
                    "Currently non-one exponent for unit is not supported. Got: 3.");
        }
    }

    @Test
    public void testDeserializeCompositeJsonResultThrowsError()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        String testSourceStr = "import pkg::*;\n" +
                massDefinition +
                "Class A\n" +
                "{\n" +
                "myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1},{\"unitId\":\"pkg::Mass~Gram\",\"exponentValue\":1}],\"value\":5}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";

        try
        {
            this.compileTestSource("fromString.pure", testSourceStr);
            CoreInstance func = this.runtime.getFunction("testUnitToJsonWithType():Any[*]");
            this.functionExecution.start(func, FastList.<CoreInstance>newList());

            Assert.fail("Expected exception evaluating: \n" + testSourceStr);
        }
        catch (PureExecutionException e)
        {
            this.assertException(e, "Error populating property 'myWeight' on class 'A': \n" +
                    "Currently composite units are not supported.");
        }
    }
}