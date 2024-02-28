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

package org.finos.legend.pure.runtime.java.interpreted.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSyntheticNodeCut extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
    }

    @Test
    public void testEnsurePropertySourceTypeIsNotResolved()
    {
        compileTestSource("testSource.pure", "Class A{name:String[1];}\n" +
                "function\n" +
                "   {doc.doc = 'Get the property with the given name from the given class. Note that this searches only properties defined directly on the class, not those inherited from super-classes or those which come from associations.'}\n" +
                "   meta::pure::functions::meta::classPropertyByName(class:Class<Any>[1], name:String[1]):Property<Nil,Any|*>[0..1]\n" +
                "{\n" +
                "    $class.properties->filter(p | $p.name == $name)->first()\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "    print(A->classPropertyByName('name'),10);\n" +
                "}\n");
        execute("test():Nil[0]");
        Assert.assertEquals("name instance Property\n" +
                "    aggregation(Property):\n" +
                "        None instance AggregationKind\n" +
                "            name(Property):\n" +
                "                None instance String\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            multiplicityArguments(Property):\n" +
                "                [X] PureOne instance PackageableMultiplicity\n" +
                "            rawType(Property):\n" +
                "                [X] Property instance Class\n" +
                "            referenceUsages(Property):\n" +
                "                Anonymous_StripedId instance ReferenceUsage\n" +
                "                    offset(Property):\n" +
                "                        0 instance Integer\n" +
                "                    owner(Property):\n" +
                "                        [_] name instance Property\n" +
                "                    propertyName(Property):\n" +
                "                        classifierGenericType instance String\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [~>] A instance Class\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                            owner(Property):\n" +
                "                                [_] Anonymous_StripedId instance GenericType\n" +
                "                            propertyName(Property):\n" +
                "                                typeArguments instance String\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] String instance PrimitiveType\n" +
                "                    referenceUsages(Property):\n" +
                "                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                            offset(Property):\n" +
                "                                1 instance Integer\n" +
                "                            owner(Property):\n" +
                "                                [_] Anonymous_StripedId instance GenericType\n" +
                "                            propertyName(Property):\n" +
                "                                typeArguments instance String\n" +
                "    genericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] String instance PrimitiveType\n" +
                "    multiplicity(Property):\n" +
                "        [X] PureOne instance PackageableMultiplicity\n" +
                "    name(Property):\n" +
                "        name instance String\n" +
                "    owner(Property):\n" +
                "        [X] A instance Class", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testEnsureLambdaReturnIsNotResolved()
    {
        compileTestSource("testSource.pure",
                "Class A{}\n" +
                        "function testMany():FunctionDefinition<{->A[1]}>[1]\n" +
                        "{\n" +
                        "    |^A();\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("testMany():FunctionDefinition[1]");
        Assert.assertEquals("Anonymous_StripedId instance InstanceValue\n" +
                        "    genericType(Property):\n" +
                        "        Anonymous_StripedId instance GenericType\n" +
                        "            rawType(Property):\n" +
                        "                LambdaFunction instance Class\n" +
                        "            typeArguments(Property):\n" +
                        "                Anonymous_StripedId instance GenericType\n" +
                        "                    rawType(Property):\n" +
                        "                        Anonymous_StripedId instance FunctionType\n" +
                        "                            returnMultiplicity(Property):\n" +
                        "                                PureOne instance PackageableMultiplicity\n" +
                        "                            returnType(Property):\n" +
                        "                                Anonymous_StripedId instance GenericType\n" +
                        "                                    rawType(Property):\n" +
                        "                                        Anonymous_StripedId instance ImportStub\n" +
                        "                                            idOrPath(Property):\n" +
                        "                                                A instance String\n" +
                        "                                            importGroup(Property):\n" +
                        "                                                import_testSource_pure_1 instance ImportGroup\n" +
                        "                                            resolvedNode(Property):\n" +
                        "                                                A instance Class\n" +
                        "    multiplicity(Property):\n" +
                        "        PureOne instance PackageableMultiplicity\n" +
                        "    usageContext(Property):\n" +
                        "        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                        "            functionDefinition(Property):\n" +
                        "                testMany__FunctionDefinition_1_ instance ConcreteFunctionDefinition\n" +
                        "            offset(Property):\n" +
                        "                0 instance Integer\n" +
                        "    values(Property):\n" +
                        "        testMany$1$system$imports$import_testSource_pure_1$0 instance LambdaFunction\n" +
                        "            classifierGenericType(Property):\n" +
                        "                Anonymous_StripedId instance GenericType\n" +
                        "                    rawType(Property):\n" +
                        "                        LambdaFunction instance Class\n" +
                        "                    typeArguments(Property):\n" +
                        "                        Anonymous_StripedId instance GenericType\n" +
                        "                            rawType(Property):\n" +
                        "                                Anonymous_StripedId instance FunctionType\n" +
                        "                                    function(Property):\n" +
                        "                                        testMany$1$system$imports$import_testSource_pure_1$0 instance LambdaFunction\n" +
                        "                                    returnMultiplicity(Property):\n" +
                        "                                        PureOne instance PackageableMultiplicity\n" +
                        "                                    returnType(Property):\n" +
                        "                                        Anonymous_StripedId instance InferredGenericType\n" +
                        "                                            rawType(Property):\n" +
                        "                                                Anonymous_StripedId instance ImportStub\n" +
                        "                                                    idOrPath(Property):\n" +
                        "                                                        A instance String\n" +
                        "                                                    importGroup(Property):\n" +
                        "                                                        import_testSource_pure_1 instance ImportGroup\n" +
                        "                                                    resolvedNode(Property):\n" +
                        "                                                        A instance Class\n" +
                        "                                            referenceUsages(Property):\n" +
                        "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                        "                                                    offset(Property):\n" +
                        "                                                        0 instance Integer\n" +
                        "                                                    owner(Property):\n" +
                        "                                                        Anonymous_StripedId instance FunctionType\n" +
                        "                                                    propertyName(Property):\n" +
                        "                                                        returnType instance String\n" +
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
                        "                                        A instance String\n" +
                        "                                    importGroup(Property):\n" +
                        "                                        import_testSource_pure_1 instance ImportGroup\n" +
                        "                                    resolvedNode(Property):\n" +
                        "                                        A instance Class\n" +
                        "                    importGroup(Property):\n" +
                        "                        import_testSource_pure_1 instance ImportGroup\n" +
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
                        "                                            propertyName(Property):\n" +
                        "                                                genericType instance String\n" +
                        "                                    typeArguments(Property):\n" +
                        "                                        Anonymous_StripedId instance GenericType\n" +
                        "                                            rawType(Property):\n" +
                        "                                                Anonymous_StripedId instance ImportStub\n" +
                        "                                                    idOrPath(Property):\n" +
                        "                                                        A instance String\n" +
                        "                                                    importGroup(Property):\n" +
                        "                                                        import_testSource_pure_1 instance ImportGroup\n" +
                        "                                                    resolvedNode(Property):\n" +
                        "                                                        A instance Class\n" +
                        "                                            referenceUsages(Property):\n" +
                        "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                        "                                                    offset(Property):\n" +
                        "                                                        0 instance Integer\n" +
                        "                                                    owner(Property):\n" +
                        "                                                        Anonymous_StripedId instance GenericType\n" +
                        "                                                    propertyName(Property):\n" +
                        "                                                        typeArguments instance String\n" +
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
                        "                                testMany$1$system$imports$import_testSource_pure_1$0 instance LambdaFunction\n" +
                        "                            offset(Property):\n" +
                        "                                0 instance Integer\n" +
                        "            referenceUsages(Property):\n" +
                        "                Anonymous_StripedId instance ReferenceUsage\n" +
                        "                    offset(Property):\n" +
                        "                        0 instance Integer\n" +
                        "                    owner(Property):\n" +
                        "                        Anonymous_StripedId instance InstanceValue\n" +
                        "                    propertyName(Property):\n" +
                        "                        values instance String",
                func.getValueForMetaPropertyToOne(M3Properties.expressionSequence).printWithoutDebug("", 10));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
