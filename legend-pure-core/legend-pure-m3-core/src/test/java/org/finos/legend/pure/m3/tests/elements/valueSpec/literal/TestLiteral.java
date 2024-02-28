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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLiteral extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testLiteralList()
    {
        compileTestSource("fromString.pure",
                "function testMany():String[*]\n" +
                        "{\n" +
                        "    let a = ['z','k']\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("testMany():String[*]");
        Assert.assertEquals("testMany__String_MANY_ instance ConcreteFunctionDefinition\n" +
                            "    classifierGenericType(Property):\n" +
                            "        Anonymous_StripedId instance GenericType\n" +
                            "            rawType(Property):\n" +
                            "                ConcreteFunctionDefinition instance Class\n" +
                            "            typeArguments(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Anonymous_StripedId instance FunctionType\n" +
                            "                            function(Property):\n" +
                            "                                testMany__String_MANY_ instance ConcreteFunctionDefinition\n" +
                            "                            returnMultiplicity(Property):\n" +
                            "                                ZeroMany instance PackageableMultiplicity\n" +
                            "                            returnType(Property):\n" +
                            "                                Anonymous_StripedId instance GenericType\n" +
                            "                                    rawType(Property):\n" +
                            "                                        String instance PrimitiveType\n" +
                            "                                    referenceUsages(Property):\n" +
                            "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                            "                                            offset(Property):\n" +
                            "                                                0 instance Integer\n" +
                            "                                            owner(Property):\n" +
                            "                                                Anonymous_StripedId instance FunctionType\n" +
                            "                                            propertyName(Property):\n" +
                            "                                                returnType instance String\n" +
                            "    expressionSequence(Property):\n" +
                            "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                            "            func(Property):\n" +
                            "                letFunction_String_1__T_m__T_m_ instance NativeFunction\n" +
                            "            functionName(Property):\n" +
                            "                letFunction instance String\n" +
                            "            genericType(Property):\n" +
                            "                Anonymous_StripedId instance InferredGenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        String instance PrimitiveType\n" +
                            "            importGroup(Property):\n" +
                            "                import_fromString_pure_1 instance ImportGroup\n" +
                            "            multiplicity(Property):\n" +
                            "                Anonymous_StripedId instance Multiplicity\n" +
                            "                    lowerBound(Property):\n" +
                            "                        Anonymous_StripedId instance MultiplicityValue\n" +
                            "                            value(Property):\n" +
                            "                                2 instance Integer\n" +
                            "                    upperBound(Property):\n" +
                            "                        Anonymous_StripedId instance MultiplicityValue\n" +
                            "                            value(Property):\n" +
                            "                                2 instance Integer\n" +
                            "            parametersValues(Property):\n" +
                            "                Anonymous_StripedId instance InstanceValue\n" +
                            "                    genericType(Property):\n" +
                            "                        Anonymous_StripedId instance GenericType\n" +
                            "                            rawType(Property):\n" +
                            "                                String instance PrimitiveType\n" +
                            "                    multiplicity(Property):\n" +
                            "                        PureOne instance PackageableMultiplicity\n" +
                            "                    usageContext(Property):\n" +
                            "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                            "                            functionExpression(Property):\n" +
                            "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                            "                            offset(Property):\n" +
                            "                                0 instance Integer\n" +
                            "                    values(Property):\n" +
                            "                        a instance String\n" +
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
                            "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                            "                            functionExpression(Property):\n" +
                            "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                            "                            offset(Property):\n" +
                            "                                1 instance Integer\n" +
                            "                    values(Property):\n" +
                            "                        z instance String\n" +
                            "                        k instance String\n" +
                            "            usageContext(Property):\n" +
                            "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                            "                    functionDefinition(Property):\n" +
                            "                        testMany__String_MANY_ instance ConcreteFunctionDefinition\n" +
                            "                    offset(Property):\n" +
                            "                        0 instance Integer\n" +
                            "    functionName(Property):\n" +
                            "        testMany instance String\n" +
                            "    name(Property):\n" +
                            "        testMany__String_MANY_ instance String\n" +
                            "    package(Property):\n" +
                            "        Root instance Package", func.printWithoutDebug("", 10));
    }
}
