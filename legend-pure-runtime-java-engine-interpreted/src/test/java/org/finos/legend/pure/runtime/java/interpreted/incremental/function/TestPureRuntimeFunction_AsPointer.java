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

package org.finos.legend.pure.runtime.java.interpreted.incremental.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeFunction_AsPointer extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeFunctionPointer() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.createInMemorySource("userId.pure", "function go():Nil[0]{print(sourceFunction__String_1_, 10)}\n");
        this.compileAndExecute("go():Nil[0]");
        int size = this.runtime.getModelRepository().serialize().length;
        Assert.assertEquals("sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] ConcreteFunctionDefinition instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance FunctionType\n" +
                "                            function(Property):\n" +
                "                                [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                "                            returnMultiplicity(Property):\n" +
                "                                [X] PureOne instance PackageableMultiplicity\n" +
                "                            returnType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        [X] String instance PrimitiveType\n" +
                "                                    referenceUsages(Property):\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                [_] Anonymous_StripedId instance FunctionType\n" +
                "                                            propertyName(Property):\n" +
                "                                                returnType instance String\n" +
                "    expressionSequence(Property):\n" +
                "        Anonymous_StripedId instance InstanceValue\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        [X] String instance PrimitiveType\n" +
                "            multiplicity(Property):\n" +
                "                [X] PureOne instance PackageableMultiplicity\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    functionDefinition(Property):\n" +
                "                        [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                "                    offset(Property):\n" +
                "                        0 instance Integer\n" +
                "            values(Property):\n" +
                "                theFunc instance String\n" +
                "    functionName(Property):\n" +
                "        sourceFunction instance String\n" +
                "    name(Property):\n" +
                "        sourceFunction__String_1_ instance String\n" +
                "    package(Property):\n" +
                "        [X] Root instance Package\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                0 instance Integer\n" +
                "            owner(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] ConcreteFunctionDefinition instance Class\n" +
                "                            typeArguments(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Anonymous_StripedId instance FunctionType\n" +
                "                                            returnMultiplicity(Property):\n" +
                "                                                [X] PureOne instance PackageableMultiplicity\n" +
                "                                            returnType(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    rawType(Property):\n" +
                "                                                        [X] String instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                            functionExpression(Property):\n" +
                "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                    func(Property):\n" +
                "                                        [X] print_Any_MANY__Integer_1__Nil_0_ instance NativeFunction\n" +
                "                                    functionName(Property):\n" +
                "                                        print instance String\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                [~>] Nil instance Class\n" +
                "                                    importGroup(Property):\n" +
                "                                        [X] import_userId_pure_1 instance ImportGroup\n" +
                "                                    multiplicity(Property):\n" +
                "                                        [X] PureZero instance PackageableMultiplicity\n" +
                "                                    parametersValues(Property):\n" +
                "                                        [_] Anonymous_StripedId instance InstanceValue\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                            genericType(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    rawType(Property):\n" +
                "                                                        [X] Integer instance PrimitiveType\n" +
                "                                            multiplicity(Property):\n" +
                "                                                [X] PureOne instance PackageableMultiplicity\n" +
                "                                            usageContext(Property):\n" +
                "                                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                                    functionExpression(Property):\n" +
                "                                                        [_] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    offset(Property):\n" +
                "                                                        1 instance Integer\n" +
                "                                            values(Property):\n" +
                "                                                10 instance Integer\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                                            functionDefinition(Property):\n" +
                "                                                [X] go__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        [~>] sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                "            propertyName(Property):\n" +
                "                values instance String", this.functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 28, e);
            }

            this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'beuh!'}");
            this.compileAndExecute("go():Nil[0]");
            Assert.assertEquals("sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                    "    classifierGenericType(Property):\n" +
                    "        Anonymous_StripedId instance GenericType\n" +
                    "            rawType(Property):\n" +
                    "                [X] ConcreteFunctionDefinition instance Class\n" +
                    "            typeArguments(Property):\n" +
                    "                Anonymous_StripedId instance GenericType\n" +
                    "                    rawType(Property):\n" +
                    "                        Anonymous_StripedId instance FunctionType\n" +
                    "                            function(Property):\n" +
                    "                                [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                    "                            returnMultiplicity(Property):\n" +
                    "                                [X] PureOne instance PackageableMultiplicity\n" +
                    "                            returnType(Property):\n" +
                    "                                Anonymous_StripedId instance GenericType\n" +
                    "                                    rawType(Property):\n" +
                    "                                        [X] String instance PrimitiveType\n" +
                    "                                    referenceUsages(Property):\n" +
                    "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                    "                                            offset(Property):\n" +
                    "                                                0 instance Integer\n" +
                    "                                            owner(Property):\n" +
                    "                                                [_] Anonymous_StripedId instance FunctionType\n" +
                    "                                            propertyName(Property):\n" +
                    "                                                returnType instance String\n" +
                    "    expressionSequence(Property):\n" +
                    "        Anonymous_StripedId instance InstanceValue\n" +
                    "            genericType(Property):\n" +
                    "                Anonymous_StripedId instance GenericType\n" +
                    "                    rawType(Property):\n" +
                    "                        [X] String instance PrimitiveType\n" +
                    "            multiplicity(Property):\n" +
                    "                [X] PureOne instance PackageableMultiplicity\n" +
                    "            usageContext(Property):\n" +
                    "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                    "                    functionDefinition(Property):\n" +
                    "                        [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                    "                    offset(Property):\n" +
                    "                        0 instance Integer\n" +
                    "            values(Property):\n" +
                    "                beuh! instance String\n" +
                    "    functionName(Property):\n" +
                    "        sourceFunction instance String\n" +
                    "    name(Property):\n" +
                    "        sourceFunction__String_1_ instance String\n" +
                    "    package(Property):\n" +
                    "        [X] Root instance Package\n" +
                    "    referenceUsages(Property):\n" +
                    "        Anonymous_StripedId instance ReferenceUsage\n" +
                    "            offset(Property):\n" +
                    "                0 instance Integer\n" +
                    "            owner(Property):\n" +
                    "                Anonymous_StripedId instance InstanceValue\n" +
                    "                    genericType(Property):\n" +
                    "                        Anonymous_StripedId instance GenericType\n" +
                    "                            rawType(Property):\n" +
                    "                                [X] ConcreteFunctionDefinition instance Class\n" +
                    "                            typeArguments(Property):\n" +
                    "                                Anonymous_StripedId instance GenericType\n" +
                    "                                    rawType(Property):\n" +
                    "                                        Anonymous_StripedId instance FunctionType\n" +
                    "                                            returnMultiplicity(Property):\n" +
                    "                                                [X] PureOne instance PackageableMultiplicity\n" +
                    "                                            returnType(Property):\n" +
                    "                                                Anonymous_StripedId instance GenericType\n" +
                    "                                                    rawType(Property):\n" +
                    "                                                        [X] String instance PrimitiveType\n" +
                    "                    multiplicity(Property):\n" +
                    "                        [X] PureOne instance PackageableMultiplicity\n" +
                    "                    usageContext(Property):\n" +
                    "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                    "                            functionExpression(Property):\n" +
                    "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                    "                                    func(Property):\n" +
                    "                                        [X] print_Any_MANY__Integer_1__Nil_0_ instance NativeFunction\n" +
                    "                                    functionName(Property):\n" +
                    "                                        print instance String\n" +
                    "                                    genericType(Property):\n" +
                    "                                        Anonymous_StripedId instance InferredGenericType\n" +
                    "                                            rawType(Property):\n" +
                    "                                                [~>] Nil instance Class\n" +
                    "                                    importGroup(Property):\n" +
                    "                                        [X] import_userId_pure_1 instance ImportGroup\n" +
                    "                                    multiplicity(Property):\n" +
                    "                                        [X] PureZero instance PackageableMultiplicity\n" +
                    "                                    parametersValues(Property):\n" +
                    "                                        [_] Anonymous_StripedId instance InstanceValue\n" +
                    "                                        Anonymous_StripedId instance InstanceValue\n" +
                    "                                            genericType(Property):\n" +
                    "                                                Anonymous_StripedId instance GenericType\n" +
                    "                                                    rawType(Property):\n" +
                    "                                                        [X] Integer instance PrimitiveType\n" +
                    "                                            multiplicity(Property):\n" +
                    "                                                [X] PureOne instance PackageableMultiplicity\n" +
                    "                                            usageContext(Property):\n" +
                    "                                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                    "                                                    functionExpression(Property):\n" +
                    "                                                        [_] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                    "                                                    offset(Property):\n" +
                    "                                                        1 instance Integer\n" +
                    "                                            values(Property):\n" +
                    "                                                10 instance Integer\n" +
                    "                                    usageContext(Property):\n" +
                    "                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                    "                                            functionDefinition(Property):\n" +
                    "                                                [X] go__Nil_0_ instance ConcreteFunctionDefinition\n" +
                    "                                            offset(Property):\n" +
                    "                                                0 instance Integer\n" +
                    "                            offset(Property):\n" +
                    "                                0 instance Integer\n" +
                    "                    values(Property):\n" +
                    "                        [~>] sourceFunction__String_1_ instance ConcreteFunctionDefinition\n" +
                    "            propertyName(Property):\n" +
                    "                values instance String", this.functionExecution.getConsole().getLine(0));
        }

        this.runtime.delete("sourceId.pure");
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }

    @Test
    public void testPureRuntimeFunctionPointerError() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.createInMemorySource("userId.pure", "function go():Nil[0]{print(sourceFunction__String_1_,1)}\n");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 28, e);
            }
        }
        this.runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        this.runtime.compile();
        Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
    }


    @Test
    public void testPureRuntimeFunctionPointerAsParamOfAFunction() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "Class A{}");
        this.runtime.createInMemorySource("userId.pure", "function other(a:FunctionDefinition<{->A[1]}>[1]):Nil[0]{[]} function go():Nil[0]{other(sourceFunction__A_1_)}\n");
        this.runtime.createInMemorySource("other.pure", " function sourceFunction():A[1]{^A()}");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "A has not been defined!", e);
            }

            this.runtime.createInMemorySource("sourceId.pure", "Class A{}");
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
        }
    }


    @Test
    public void testPureRuntimeFunctionPointerAsParamOfAFunctionError() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", "Class A{}");
        this.runtime.createInMemorySource("userId.pure", "function other(a:FunctionDefinition<{->A[1]}>[1]):Nil[0]{[]} function go():Nil[0]{other(sourceFunction__A_1_)}\n");
        this.runtime.createInMemorySource("other.pure", " function sourceFunction():A[1]{^A()}");
        this.runtime.compile();
        int size = this.runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "A has not been defined!", e);
            }

            try
            {
                this.runtime.createInMemorySource("sourceId.pure", "Class B{}");
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertTrue("Compilation error at (resource:other.pure line:1 column:28), \"A has not been defined!\"".equals(e.getMessage()) || "Compilation error at (resource:userId.pure line:1 column:40), \"A has not been defined!\"".equals(e.getMessage()));
            }

            this.runtime.modify("sourceId.pure", "Class A{}");
            this.runtime.compile();
            Assert.assertEquals("Graph size mismatch", size, this.repository.serialize().length);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
