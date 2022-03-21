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

package org.finos.legend.pure.m3.tests.validation;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNewInstance extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("testModel.pure");
        runtime.delete("testFunc.pure");
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleGeneralizationUnknownProperty()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class A\n" +
                        "{\n" +
                        "   propA:String[1];\n" +
                        "}\n" +
                        "Class B extends A\n" +
                        "{\n" +
                        "   propB:String[1];\n" +
                        "}\n" +
                        "function simpleTest():B[1]\n" +
                        "{\n" +
                        "   ^B(propA='iA',propB='iB',random='oll');\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "The property 'random' can't be found in the type 'B' or in its hierarchy.", 11, 29, e);
    }

    @Test
    public void testIncompatiblePrimitiveTypes()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=1)\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: Integer not a subtype of String", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleClasses()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class C\n" +
                        "{\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=^C())\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: C not a subtype of B", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleMixedTypes1()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:B[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop='the quick brown fox')\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: String not a subtype of B", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleMixedTypes2()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=^B())\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: B not a subtype of String", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleInstanceValueMultiplicity()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=['one string', 'two string', 'red string', 'blue string'])\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [4] is not compatible with [1]", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleExpressionMultiplicity()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function someStrings():String[*]\n" +
                        "{\n" +
                        "    ['one string', 'two string', 'red string', 'blue string'];\n" +
                        "}");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=someStrings())\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [*] is not compatible with [1]", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testMissingRequiredProperty()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    optionalProp:Integer[*];\n" +
                        "    requiredProp:String[1];\n" +
                        "}");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(optionalProp=[1, 2, 3])\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Missing value(s) for required property 'requiredProp' which has a multiplicity of [1] for type A", "testFunc.pure", 3, 5, 3, 5, 3, 30, e);
    }

    @Test
    public void testChainedProperties()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class B\n" +
                        "{\n" +
                        "    propToA:A[1];\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():B[1]\n" +
                        "{\n" +
                        "    ^B(propToA.prop='string')\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Chained properties are not allowed in new expressions", "testFunc.pure", 3, 8, 3, 8, 3, 16, e);
    }

    @Test
    public void testNewBoolean()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    ^Boolean()\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Boolean", "testSource.pure", 3, 5, 3, 5, 3, 14, e);
    }

    @Test
    public void testNewDate()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    ^Date()\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Date", "testSource.pure", 3, 5, 3, 5, 3, 11, e);
    }

    @Test
    public void testNewFloat()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    ^Float()\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Float", "testSource.pure", 3, 5, 3, 5, 3, 12, e);
    }

    @Test
    public void testNewInteger()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    ^Integer()\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Integer", "testSource.pure", 3, 5, 3, 5, 3, 14, e);
    }

    @Test
    public void testNewNumber()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    ^Number()\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Number", "testSource.pure", 3, 5, 3, 5, 3, 13, e);
    }

    @Test
    public void testNewEnumeration()
    {
        compileTestSource("fromString.pure", "Enum TestEnum {VAL1, VAL2}");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    ^TestEnum()\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: TestEnum", "testSource.pure", 3, 5, 3, 5, 3, 15, e);
    }

    @Test
    public void testTimeWithStrictDateType()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:StrictDate[1];\n" +
                        "}");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=%2014-02-07T07:03:01)\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: DateTime not a subtype of StrictDate", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testTimeWithWrongStrictTimeType()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:StrictTime[1];\n" +
                        "}");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=%2014-02-07T07:03:01)\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Type Error: DateTime not a subtype of StrictTime", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testTimeWithStrictTimeType()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "    prop:StrictTime[1];\n" +
                        "}");

        compileTestSource(
                "testFunc.pure",
                "function testFunc():A[1]\n" +
                        "{\n" +
                        "    ^A(prop=%07:03:01)\n" +
                        "}");

    }

    @Test
    public void testFunctionSignature()
    {
        compileTestSource("testModel.pure",
                "Class A\n" +
                        "{\n" +
                        "}" +
                        "Class B{}" +
                        "Class C{}" +
                        "Class meta::pure::router::extension::RouterExtension\n" +
                        "{\n" +
                        "   analytics_getStoreFromSetImpl : LambdaFunction<{A[1] -> LambdaFunction<{Nil[1]->C[1]}>[1]}>[0..1];\n" +
                        "}" +
                        "" +
                        "function a():Any[0..1]" +
                        "{" +
                        " ^meta::pure::router::extension::RouterExtension(" +
                        "   analytics_getStoreFromSetImpl = a:A[1]|{b:B[1]|^C()}" +
                        " )" +
                        "}" +
                        "");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                "Class meta::pure::router::extension::RouterExtension2\n" +
                        "{\n" +
                        "   analytics_getStoreFromSetImpl : LambdaFunction<{A[1] -> LambdaFunction<{Any[1]->C[1]}>[1]}>[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "function za():Any[0..1]\n" +
                        "{\n" +
                        " ^meta::pure::router::extension::RouterExtension2(\n" +
                        "   analytics_getStoreFromSetImpl = a:A[1]|{b:B[1]|^C()}\n" +
                        " )\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Type Error: LambdaFunction<{A[1]->LambdaFunction<{B[1]->C[1]}>[1]}> not a subtype of LambdaFunction<{A[1]->LambdaFunction<{Any[1]->C[1]}>[1]}>", "testFunc.pure", 9, 34, 9, 34, 9, 34, e);
    }

    @Test
    public void testNewSourceInformation()
    {
        String text = "Class a::b::c::FirmX\n" +
                "{\n" +
                "    prop:String[1];\n" +
                "}\n" +
                "function testFunc():a::b::c::FirmX[1]\n" +
                "{\n" +
                "    ^a::b::c::FirmX(prop='one string')\n" +
                "}\n";
        compileTestSource("testModel.pure",
                text);
        CoreInstance a = runtime.getCoreInstance("a::b::c::FirmX");
        ListIterable<? extends CoreInstance> referenceUsages = a.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Verify.assertSize(3, referenceUsages);

        ListIterable<SourceInformation> sourceInformations = referenceUsages.collect(coreInstance -> coreInstance.getValueForMetaPropertyToOne(M3Properties.owner).getValueForMetaPropertyToOne(M3Properties.rawType).getSourceInformation());
        String[] lines = text.split("\n");
        Verify.assertSize(3, sourceInformations);

        for (SourceInformation sourceInformation : sourceInformations)
        {
            Assert.assertEquals("FirmX", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getEndColumn()));
        }
    }

    @Test
    public void testNewInstanceWithNonHomogenousTypeParameters()
    {
        compileTestSource("testModel.pure",
                "import a::b::c::*;\n" +
                        "Class a::b::c::ClassWithTypeParameter<T>\n" +
                        "{\n" +
                        "    value:T[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::c::SubclassWithoutTypeParameter extends ClassWithTypeParameter<String>\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class a::b::c::ClassWrapper\n" +
                        "{\n" +
                        "    classWithTypeParameter:Class<ClassWithTypeParameter<Any>>[1];\n" +
                        "}\n" +
                        "\n" +
                        "function a::b::c::testFn():Any[*]\n" +
                        "{\n" +
                        "    ^ClassWrapper(classWithTypeParameter=a::b::c::SubclassWithoutTypeParameter);\n" +
                        "}\n");
    }
}
