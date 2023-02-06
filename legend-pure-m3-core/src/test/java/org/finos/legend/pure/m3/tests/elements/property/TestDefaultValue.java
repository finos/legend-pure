// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements.property;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDefaultValue extends AbstractPureTestWithCoreCompiledPlatform {

    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("defaultValueSource.pure");
    }

    @Test
    public void testDefaultValue()
    {
        compileTestSource("defaultValueSource.pure", "import test::*;\n"
                + "Class my::exampleRootType\n"
                + "{\n"
                + "}\n"

                + "Class my::exampleSubType extends my::exampleRootType\n"
                + "{\n"
                + "}\n"

                + "Enum test::EnumWithDefault\n"
                + "{\n"
                + "   DefaultValue,\n"
                + "   AnotherValue\n"
                + "}\n"

                + "Class test::A\n"
                + "{\n"
                + "   stringProperty:String[1] = 'default';\n"
                + "   classProperty:my::exampleRootType[1] = ^my::exampleRootType();\n"
                + "   enumProperty:test::EnumWithDefault[1] = test::EnumWithDefault.DefaultValue;\n"
                + "   floatProperty:Float[1] = 0.12;\n"
                + "   inheritProperty:Number[1] = 0.12;\n"
                + "   booleanProperty:Boolean[1] = false;\n"
                + "   integerProperty:Integer[1] = 0;\n"
                + "   collectionProperty:String[1..*] = ['one', 'two'];\n"
                + "   enumCollection:EnumWithDefault[1..*] = [EnumWithDefault.DefaultValue, EnumWithDefault.AnotherValue];\n"
                + "   classCollection:my::exampleRootType[1..4] = [^my::exampleRootType(), ^my::exampleSubType()];\n"
                + "   singleProperty:String[1] = ['one'];\n"
                + "   anyProperty:Any[1] = 'anyString';\n"
                + "}\n"
                + "\n"
        );

        CoreInstance classA = this.runtime.getCoreInstance("test::A");

        CoreInstance stringProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "stringProperty");
        CoreInstance classProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "classProperty");
        CoreInstance enumProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "enumProperty");
        CoreInstance floatProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "floatProperty");
        CoreInstance inheritProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "inheritProperty");
        CoreInstance booleanProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "booleanProperty");
        CoreInstance integerProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "integerProperty");
        CoreInstance collectionProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "collectionProperty");
        CoreInstance enumCollection = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "enumCollection");
        CoreInstance classCollection = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "classCollection");
        CoreInstance singleProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "singleProperty");
        CoreInstance anyProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "anyProperty");

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        stringProperty_defaultValue_1$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        stringProperty_defaultValue_1$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                String instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                stringProperty_defaultValue_1$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        default instance String", stringProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        enumProperty_defaultValue_3$1 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        enumProperty_defaultValue_3$1 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                EnumWithDefault instance Enumeration\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                    func(Property):\n" +
                "                        extractEnumValue_Enumeration_1__String_1__T_1_ instance NativeFunction\n" +
                "                    functionName(Property):\n" +
                "                        extractEnumValue instance String\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance InferredGenericType\n" +
                "                            rawType(Property):\n" +
                "                                EnumWithDefault instance Enumeration\n" +
                "                    importGroup(Property):\n" +
                "                        import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    parametersValues(Property):\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Enumeration instance Class\n" +
                "                                    referenceUsages(Property):\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance InstanceValue\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                genericType instance String\n" +
                "                                    typeArguments(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                EnumWithDefault instance Enumeration\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                    functionExpression(Property):\n" +
                "                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                            values(Property):\n" +
                "                                Anonymous_StripedId instance ImportStub\n" +
                "                                    idOrPath(Property):\n" +
                "                                        test::EnumWithDefault instance String\n" +
                "                                    importGroup(Property):\n" +
                "                                        import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                                    resolvedNode(Property):\n" +
                "                                        EnumWithDefault instance Enumeration\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        String instance PrimitiveType\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                    functionExpression(Property):\n" +
                "                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                    offset(Property):\n" +
                "                                        1 instance Integer\n" +
                "                            values(Property):\n" +
                "                                DefaultValue instance String\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                enumProperty_defaultValue_3$1 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer", enumProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        booleanProperty_defaultValue_6$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        booleanProperty_defaultValue_6$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Boolean instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Boolean instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                booleanProperty_defaultValue_6$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        false instance Boolean", booleanProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        classProperty_defaultValue_2$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        classProperty_defaultValue_2$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Anonymous_StripedId instance ImportStub\n" +
                "                                                    [... >5]\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                    func(Property):\n" +
                "                        new_Class_1__String_1__T_1_ instance NativeFunction\n" +
                "                    functionName(Property):\n" +
                "                        new instance String\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance InferredGenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance ImportStub\n" +
                "                                    idOrPath(Property):\n" +
                "                                        my::exampleRootType instance String\n" +
                "                                    importGroup(Property):\n" +
                "                                        import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                                    resolvedNode(Property):\n" +
                "                                        exampleRootType instance Class\n" +
                "                                            classifierGenericType(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            generalizations(Property):\n" +
                "                                                Anonymous_StripedId instance Generalization\n" +
                "                                                    [... >5]\n" +
                "                                            name(Property):\n" +
                "                                                exampleRootType instance String\n" +
                "                                            package(Property):\n" +
                "                                                my instance Package\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                            specializations(Property):\n" +
                "                                                Anonymous_StripedId instance Generalization\n" +
                "                                                    [... >5]\n" +
                "                    importGroup(Property):\n" +
                "                        import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    parametersValues(Property):\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Class instance Class\n" +
                "                                    referenceUsages(Property):\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance InstanceValue\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                genericType instance String\n" +
                "                                    typeArguments(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Anonymous_StripedId instance ImportStub\n" +
                "                                                    [... >5]\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                    functionExpression(Property):\n" +
                "                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                            values(Property):\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        String instance PrimitiveType\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                    functionExpression(Property):\n" +
                "                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                    offset(Property):\n" +
                "                                        1 instance Integer\n" +
                "                            values(Property):\n" +
                "                                 instance String\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                classProperty_defaultValue_2$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer", classProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        integerProperty_defaultValue_7$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        integerProperty_defaultValue_7$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Integer instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Integer instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                integerProperty_defaultValue_7$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        0 instance Integer", integerProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        floatProperty_defaultValue_4$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        floatProperty_defaultValue_4$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Float instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Float instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                floatProperty_defaultValue_4$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        0.12 instance Float", floatProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        inheritProperty_defaultValue_5$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        inheritProperty_defaultValue_5$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Float instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Float instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                inheritProperty_defaultValue_5$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        0.12 instance Float", inheritProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        collectionProperty_defaultValue_8$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        collectionProperty_defaultValue_8$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        Anonymous_StripedId instance Multiplicity\n" +
                "                                            lowerBound(Property):\n" +
                "                                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                                    [... >5]\n" +
                "                                            upperBound(Property):\n" +
                "                                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                                    [... >5]\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                String instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        Anonymous_StripedId instance Multiplicity\n" +
                "                            lowerBound(Property):\n" +
                "                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                    value(Property):\n" +
                "                                        2 instance Integer\n" +
                "                            upperBound(Property):\n" +
                "                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                    value(Property):\n" +
                "                                        2 instance Integer\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                collectionProperty_defaultValue_8$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        String instance PrimitiveType\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                            values(Property):\n" +
                "                                one instance String\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        String instance PrimitiveType\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        1 instance Integer\n" +
                "                            values(Property):\n" +
                "                                two instance String", collectionProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        enumCollection_defaultValue_9$2 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        enumCollection_defaultValue_9$2 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        Anonymous_StripedId instance Multiplicity\n" +
                "                                            lowerBound(Property):\n" +
                "                                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                                    [... >5]\n" +
                "                                            upperBound(Property):\n" +
                "                                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                                    [... >5]\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                EnumWithDefault instance Enumeration\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                EnumWithDefault instance Enumeration\n" +
                "                    multiplicity(Property):\n" +
                "                        Anonymous_StripedId instance Multiplicity\n" +
                "                            lowerBound(Property):\n" +
                "                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                    value(Property):\n" +
                "                                        2 instance Integer\n" +
                "                            upperBound(Property):\n" +
                "                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                    value(Property):\n" +
                "                                        2 instance Integer\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                enumCollection_defaultValue_9$2 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            func(Property):\n" +
                "                                extractEnumValue_Enumeration_1__String_1__T_1_ instance NativeFunction\n" +
                "                            functionName(Property):\n" +
                "                                extractEnumValue instance String\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        EnumWithDefault instance Enumeration\n" +
                "                            importGroup(Property):\n" +
                "                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            parametersValues(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Enumeration instance Class\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                            typeArguments(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                        Anonymous_StripedId instance ImportStub\n" +
                "                                            idOrPath(Property):\n" +
                "                                                EnumWithDefault instance String\n" +
                "                                            importGroup(Property):\n" +
                "                                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                                            resolvedNode(Property):\n" +
                "                                                EnumWithDefault instance Enumeration\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                1 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                        DefaultValue instance String\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            func(Property):\n" +
                "                                extractEnumValue_Enumeration_1__String_1__T_1_ instance NativeFunction\n" +
                "                            functionName(Property):\n" +
                "                                extractEnumValue instance String\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        EnumWithDefault instance Enumeration\n" +
                "                            importGroup(Property):\n" +
                "                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            parametersValues(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Enumeration instance Class\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                            typeArguments(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                        Anonymous_StripedId instance ImportStub\n" +
                "                                            idOrPath(Property):\n" +
                "                                                EnumWithDefault instance String\n" +
                "                                            importGroup(Property):\n" +
                "                                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                                            resolvedNode(Property):\n" +
                "                                                EnumWithDefault instance Enumeration\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                1 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                        AnotherValue instance String\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        1 instance Integer", enumCollection.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        classCollection_defaultValue_10$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        classCollection_defaultValue_10$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        Anonymous_StripedId instance Multiplicity\n" +
                "                                            lowerBound(Property):\n" +
                "                                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                                    [... >5]\n" +
                "                                            upperBound(Property):\n" +
                "                                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                                    [... >5]\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                exampleRootType instance Class\n" +
                "                                                    [... >5]\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                exampleRootType instance Class\n" +
                "                                    classifierGenericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Class instance Class\n" +
                "                                            typeArguments(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                    generalizations(Property):\n" +
                "                                        Anonymous_StripedId instance Generalization\n" +
                "                                            general(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            specific(Property):\n" +
                "                                                exampleRootType instance Class\n" +
                "                                                    [... >5]\n" +
                "                                    name(Property):\n" +
                "                                        exampleRootType instance String\n" +
                "                                    package(Property):\n" +
                "                                        my instance Package\n" +
                "                                    referenceUsages(Property):\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                                    [... >5]\n" +
                "                                            propertyName(Property):\n" +
                "                                                rawType instance String\n" +
                "                                    specializations(Property):\n" +
                "                                        Anonymous_StripedId instance Generalization\n" +
                "                                            general(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                            specific(Property):\n" +
                "                                                exampleSubType instance Class\n" +
                "                                                    [... >5]\n" +
                "                    multiplicity(Property):\n" +
                "                        Anonymous_StripedId instance Multiplicity\n" +
                "                            lowerBound(Property):\n" +
                "                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                    value(Property):\n" +
                "                                        2 instance Integer\n" +
                "                            upperBound(Property):\n" +
                "                                Anonymous_StripedId instance MultiplicityValue\n" +
                "                                    value(Property):\n" +
                "                                        2 instance Integer\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                classCollection_defaultValue_10$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            func(Property):\n" +
                "                                new_Class_1__String_1__T_1_ instance NativeFunction\n" +
                "                            functionName(Property):\n" +
                "                                new instance String\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Anonymous_StripedId instance ImportStub\n" +
                "                                            idOrPath(Property):\n" +
                "                                                my::exampleRootType instance String\n" +
                "                                            importGroup(Property):\n" +
                "                                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                                            resolvedNode(Property):\n" +
                "                                                exampleRootType instance Class\n" +
                "                                                    [... >5]\n" +
                "                            importGroup(Property):\n" +
                "                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            parametersValues(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Class instance Class\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                            typeArguments(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                1 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                         instance String\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            func(Property):\n" +
                "                                new_Class_1__String_1__T_1_ instance NativeFunction\n" +
                "                            functionName(Property):\n" +
                "                                new instance String\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Anonymous_StripedId instance ImportStub\n" +
                "                                            idOrPath(Property):\n" +
                "                                                my::exampleSubType instance String\n" +
                "                                            importGroup(Property):\n" +
                "                                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                                            resolvedNode(Property):\n" +
                "                                                exampleSubType instance Class\n" +
                "                                                    [... >5]\n" +
                "                            importGroup(Property):\n" +
                "                                import_defaultValueSource_pure_1 instance ImportGroup\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            parametersValues(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Class instance Class\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "                                            typeArguments(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    [... >5]\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValue\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                            functionExpression(Property):\n" +
                "                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    [... >5]\n" +
                "                                            offset(Property):\n" +
                "                                                1 instance Integer\n" +
                "                                    values(Property):\n" +
                "                                         instance String\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        1 instance Integer", classCollection.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        singleProperty_defaultValue_11$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        singleProperty_defaultValue_11$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                String instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                singleProperty_defaultValue_11$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        String instance PrimitiveType\n" +
                "                            multiplicity(Property):\n" +
                "                                PureOne instance PackageableMultiplicity\n" +
                "                            usageContext(Property):\n" +
                "                                Anonymous_StripedId instance InstanceValueSpecificationContext\n" +
                "                                    instanceValue(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                            values(Property):\n" +
                "                                one instance String", singleProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assert.assertEquals("Anonymous_StripedId instance DefaultValue\n" +
                "    functionDefinition(Property):\n" +
                "        anyProperty_defaultValue_12$0 instance LambdaFunction\n" +
                "            classifierGenericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        LambdaFunction instance Class\n" +
                "                    typeArguments(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                Anonymous_StripedId instance FunctionType\n" +
                "                                    function(Property):\n" +
                "                                        anyProperty_defaultValue_12$0 instance LambdaFunction\n" +
                "                                    returnMultiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    returnType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    [... >5]\n" +
                "            expressionSequence(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                String instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                            functionDefinition(Property):\n" +
                "                                anyProperty_defaultValue_12$0 instance LambdaFunction\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        anyString instance String", anyProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));
    }

    @Test
    public void testDefaultValueWithUnsupportedType()
    {
        try
        {
            compileTestSource("defaultValueSource.pure", "import test::*;\n"
                    + "import meta::pure::metamodel::constraint::*;\n"
                    + "Class test::A\n"
                    + "{\n"
                    + "   stringProperty:Boolean[1] = {x: Number[1] | $x < 10};\n"
                    + "}\n"
                    + "\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: a valid identifier text; found: '{'", 5, 32, e);
        }
    }

    @Test
    public void testDefaultValueWithNotMatchingType()
    {
        try
        {
            compileTestSource("defaultValueSource.pure", "import test::*;\n"
                    + "Class test::A\n"
                    + "{\n"
                    + "   stringProperty:String[1] = false;\n"
                    + "}\n"
                    + "\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Default value for property: 'stringProperty' / Type Error: 'Boolean' not a subtype of 'String'", 4, 31, e);
        }
    }

    @Test
    public void testDefaultValueWithNotMatchingClassType()
    {
        try
        {
            compileTestSource("defaultValueSource.pure", "import test::*;\n"
                    + "Class my::A\n"
                    + "{\n"
                    + "}\n"

                    + "Class my::B \n"
                    + "{\n"
                    + "}\n"

                    + "Class test::C\n"
                    + "{\n"
                    + "   classProperty: my::A[1] = ^my::B();\n"
                    + "}\n"
                    + "\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Default value for property: 'classProperty' / Type Error: 'B' not a subtype of 'A'", 10, 30, e);
        }
    }

    @Test
    public void testDefaultValueForOptionalProperty()
    {
        try
        {
            compileTestSource("defaultValueSource.pure", "import test::*;\n"
                    + "Class test::A\n"
                    + "{\n"
                    + "   stringProperty: String[0..4] = 'optional';\n"
                    + "}\n"
                    + "\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Default values are supported only for mandatory fields, and property 'stringProperty' is optional.", 4, 4, e);
        }

        try
        {
            runtime.modify("defaultValueSource.pure", "import test::*;\n"
                    + "Class test::A\n"
                    + "{\n"
                    + "   stringProperty: String[*] = 'optional';\n"
                    + "}\n"
                    + "\n"
            );
            runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Default values are supported only for mandatory fields, and property 'stringProperty' is optional.", 4, 4, e);
        }
    }

    @Test
    public void testDefaultValueWithNotMatchingEnumType()
    {
        try
        {
            compileTestSource("defaultValueSource.pure", "import test::*;\n"
                    + "Enum example::EnumWithDefault\n"
                    + "{\n"
                    + "   DefaultValue,\n"
                    + "   AnotherValue\n"
                    + "}\n"

                    + "Enum example::DifferentEnum\n"
                    + "{\n"
                    + "   DefaultValue,\n"
                    + "   AnotherValue\n"
                    + "}\n"

                    + "Class test::C\n"
                    + "{\n"
                    + "   enumProperty: example::EnumWithDefault[1] = example::DifferentEnum.DefaultValue;\n"
                    + "}\n"
                    + "\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Default value for property: 'enumProperty' / Type Error: 'DifferentEnum' not a subtype of 'EnumWithDefault'", 14, 71, e);
        }
    }

    @Test
    public void testDefaultValueMultiplicityMatches()
    {
        try
        {
            compileTestSource("defaultValueSource.pure", "import test::*;\n"
                    + "Class test::A\n"
                    + "{\n"
                    + "   stringProperty: String[1] = ['one', 'two'];\n"
                    + "}\n"
                    + "\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The default value's multiplicity does not match the multiplicity of property 'stringProperty'.", 4, 4, e);
        }

        try
        {
            runtime.modify("defaultValueSource.pure", "import test::*;\n"
                    + "Class test::A\n"
                    + "{\n"
                    + "   stringProperty: String[1..3] = ['one', 'two', 'three', 'four'];\n"
                    + "}\n"
                    + "\n"
            );
            runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The default value's multiplicity does not match the multiplicity of property 'stringProperty'.", 4, 4, e);
        }
    }
}
