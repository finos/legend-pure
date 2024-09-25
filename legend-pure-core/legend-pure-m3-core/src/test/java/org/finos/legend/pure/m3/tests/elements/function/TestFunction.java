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

package org.finos.legend.pure.m3.tests.elements.function;

import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFunction extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
    }

    @Test
    public void testFunctionTypeWithWrongTypes()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function myFunc(f:{String[1]->{String[1]->Booelean[1]}[1]}[*]):String[1]\n" +
                        "{\n" +
                        "   'ee';\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Booelean has not been defined!", 1, 43, e);
    }


    @Test
    public void testNewWithUnknownType()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function myFunc():String[1]\n" +
                        "{\n" +
                        "    ^XErrorType(name = 'ok');\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "XErrorType has not been defined!", 3, 6, e);
    }

    @Test
    public void testCastWithUnknownType()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function myFunc():String[1]\n" +
                        "{\n" +
                        "    'a'->cast(@Error);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Error has not been defined!", 3, 16, e);
    }

    @Test
    public void testToMultiplicityWithUnknownMul()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function myFunc<|o>():String[o]\n" +
                        "{\n" +
                        "    'a'->toMultiplicity(@[x]);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "The multiplicity parameter x is unknown!", 3, 25, e);
    }

    @Test
    public void testToMultiplicityWithWrongMul()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function myFunc<|o>():String[o]\n" +
                        "{\n" +
                        "    'a';\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Return multiplicity error in function 'myFunc'; found: [1]; expected: [o]", 3, 5, e);
    }


    @Test
    public void testCastWithUnknownGeneric()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class A<E>{}\n" +
                        "\n" +
                        "function myFunc():String[1]\n" +
                        "{\n" +
                        "    'a'->cast(@A<Error>);\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Error has not been defined!", 5, 18, e);
    }

    @Test
    public void testReturnTypeValidationWithTypeParameter()
    {
        // This should work because Nil is the bottom type
        compileTestSource("fromString.pure", "function test1<T>(t:T[1]):T[0..1]\n" +
                "{\n" +
                "    []\n" +
                "}");

        PureCompilationException e1 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString2.pure",
                "function test2<T>(t:T[1]):T[1]\n" +
                        "{\n" +
                        "    5\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Return type error in function 'test2'; found: Integer; expected: T", 3, 5, e1);

        PureCompilationException e2 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString3.pure",
                "function test3<T,U>(t:T[1], u:U[1]):T[1]\n" +
                        "{\n" +
                        "    $u\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Return type error in function 'test3'; found: U; expected: T", 3, 6, e2);
    }

    @Test
    public void testReturnMultiplicityValidationWithMultiplicityParameter()
    {
        PureCompilationException e1 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "function test1<|m>(a:Any[m]):Any[m]\n" +
                        "{\n" +
                        "    1\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Return multiplicity error in function 'test1'; found: [1]; expected: [m]", 3, 5, e1);

        PureCompilationException e2 = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString2.pure",
                "function test2<|m,n>(a:Any[m], b:Any[n]):Any[m]\n" +
                        "{\n" +
                        "    $b\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "Return multiplicity error in function 'test2'; found: [n]; expected: [m]", 3, 6, e2);
    }

    @Test
    public void testSimple()
    {
        compileTestSource("fromString.pure",
                "Class a::A{val:String[1];}\n" +
                        "function myFunction(func:a::A[1]):String[1]\n" +
                        "{\n" +
                        "    ^a::A(val='ok').val;\n" +
                        "}\n" +
                        "\n" +
                        "function test():Nil[0]\n" +
                        "{\n" +
                        "    print(myFunction_A_1__String_1_,1);\n" +
                        "}");

        CoreInstance func = runtime.getFunction("test():Nil[0]");
        Assert.assertEquals("test__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] ConcreteFunctionDefinition instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance FunctionType\n" +
                "                            function(Property):\n" +
                "                                [X] test__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "                            returnMultiplicity(Property):\n" +
                "                                [X] PureZero instance PackageableMultiplicity\n" +
                "                            returnType(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "    expressionSequence(Property):\n" +
                "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "            func(Property):\n" +
                "                [X] print_Any_MANY__Integer_1__Nil_0_ instance NativeFunction\n" +
                "            functionName(Property):\n" +
                "                print instance String\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    rawType(Property):\n" +
                "                        [~>] Nil instance Class\n" +
                "            importGroup(Property):\n" +
                "                [X] import_fromString_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                [X] PureZero instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] ConcreteFunctionDefinition instance Class\n" +
                "                            typeArguments(Property):\n" +
                "                                [>3] Anonymous_StripedId instance GenericType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                            functionExpression(Property):\n" +
                "                                [>3] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            offset(Property):\n" +
                "                                [>3] 0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        [~>] myFunction_A_1__String_1_ instance ConcreteFunctionDefinition\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] Integer instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                            functionExpression(Property):\n" +
                "                                [>3] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            offset(Property):\n" +
                "                                [>3] 1 instance Integer\n" +
                "                    values(Property):\n" +
                "                        1 instance Integer\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    functionDefinition(Property):\n" +
                "                        [X] test__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "                    offset(Property):\n" +
                "                        0 instance Integer\n" +
                "    functionName(Property):\n" +
                "        test instance String\n" +
                "    name(Property):\n" +
                "        test__Nil_0_ instance String\n" +
                "    package(Property):\n" +
                "        [X] Root instance Package", Printer.print(func, "", 3, runtime.getProcessorSupport()));

        Assert.assertEquals("test__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                [X] ConcreteFunctionDefinition instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance FunctionType\n" +
                "                            function(Property):\n" +
                "                                [X] test__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "                            returnMultiplicity(Property):\n" +
                "                                [X] PureZero instance PackageableMultiplicity\n" +
                "                            returnType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        [~>] Nil instance Class\n" +
                "                                    referenceUsages(Property):\n" +
                "                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                                            owner(Property):\n" +
                "                                                [_] Anonymous_StripedId instance FunctionType\n" +
                "                                            propertyName(Property):\n" +
                "                                                returnType instance String\n" +
                "    expressionSequence(Property):\n" +
                "        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "            func(Property):\n" +
                "                [X] print_Any_MANY__Integer_1__Nil_0_ instance NativeFunction\n" +
                "            functionName(Property):\n" +
                "                print instance String\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    rawType(Property):\n" +
                "                        [~>] Nil instance Class\n" +
                "            importGroup(Property):\n" +
                "                [X] import_fromString_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                [X] PureZero instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] ConcreteFunctionDefinition instance Class\n" +
                "                            typeArguments(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Anonymous_StripedId instance FunctionType\n" +
                "                                            parameters(Property):\n" +
                "                                                Anonymous_StripedId instance VariableExpression\n" +
                "                                                    genericType(Property):\n" +
                "                                                        Anonymous_StripedId instance GenericType\n" +
                "                                                            rawType(Property):\n" +
                "                                                                [~>] a::A instance Class\n" +
                "                                                    multiplicity(Property):\n" +
                "                                                        [X] PureOne instance PackageableMultiplicity\n" +
                "                                                    name(Property):\n" +
                "                                                        func instance String\n" +
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
                "                                [_] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                    values(Property):\n" +
                "                        [~>] myFunction_A_1__String_1_ instance ConcreteFunctionDefinition\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                [X] Integer instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        [X] PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                            functionExpression(Property):\n" +
                "                                [_] Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            offset(Property):\n" +
                "                                1 instance Integer\n" +
                "                    values(Property):\n" +
                "                        1 instance Integer\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    functionDefinition(Property):\n" +
                "                        [X] test__Nil_0_ instance ConcreteFunctionDefinition\n" +
                "                    offset(Property):\n" +
                "                        0 instance Integer\n" +
                "    functionName(Property):\n" +
                "        test instance String\n" +
                "    name(Property):\n" +
                "        test__Nil_0_ instance String\n" +
                "    package(Property):\n" +
                "        [X] Root instance Package", Printer.print(func, "", runtime.getProcessorSupport()));
    }

    @Test
    public void testFunction()
    {
        compileTestSource(
                "fromString.pure",
                "Class Employee {name:String[1];}" +
                        "function getValue(source:Any[1], prop:String[1]):Any[*]\n" +
                        "{\n" +
                        "    Employee.all()->filter(t:Employee[1]|$t.name == 'cool');\n" +
                        "}");

        Assert.assertEquals("getValue_Any_1__String_1__Any_MANY_ instance ConcreteFunctionDefinition\n" +
                "    classifierGenericType(Property):\n" +
                "        Anonymous_StripedId instance GenericType\n" +
                "            rawType(Property):\n" +
                "                ConcreteFunctionDefinition instance Class\n" +
                "            typeArguments(Property):\n" +
                "                Anonymous_StripedId instance GenericType\n" +
                "                    rawType(Property):\n" +
                "                        Anonymous_StripedId instance FunctionType\n" +
                "                            function(Property):\n" +
                "                                getValue_Any_1__String_1__Any_MANY_ instance ConcreteFunctionDefinition\n" +
                "                            parameters(Property):\n" +
                "                                Anonymous_StripedId instance VariableExpression\n" +
                "                                    functionTypeOwner(Property):\n" +
                "                                        Anonymous_StripedId instance FunctionType\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Anonymous_StripedId instance ImportStub\n" +
                "                                                    idOrPath(Property):\n" +
                "                                                        Any instance String\n" +
                "                                                    importGroup(Property):\n" +
                "                                                        import_fromString_pure_1 instance ImportGroup\n" +
                "                                                    resolvedNode(Property):\n" +
                "                                                        Any instance Class\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    offset(Property):\n" +
                "                                                        0 instance Integer\n" +
                "                                                    owner(Property):\n" +
                "                                                        Anonymous_StripedId instance VariableExpression\n" +
                "                                                    propertyName(Property):\n" +
                "                                                        genericType instance String\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    name(Property):\n" +
                "                                        source instance String\n" +
                "                                Anonymous_StripedId instance VariableExpression\n" +
                "                                    functionTypeOwner(Property):\n" +
                "                                        Anonymous_StripedId instance FunctionType\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                String instance PrimitiveType\n" +
                "                                            referenceUsages(Property):\n" +
                "                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                    offset(Property):\n" +
                "                                                        0 instance Integer\n" +
                "                                                    owner(Property):\n" +
                "                                                        Anonymous_StripedId instance VariableExpression\n" +
                "                                                    propertyName(Property):\n" +
                "                                                        genericType instance String\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    name(Property):\n" +
                "                                        prop instance String\n" +
                "                            returnMultiplicity(Property):\n" +
                "                                ZeroMany instance PackageableMultiplicity\n" +
                "                            returnType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Anonymous_StripedId instance ImportStub\n" +
                "                                            idOrPath(Property):\n" +
                "                                                Any instance String\n" +
                "                                            importGroup(Property):\n" +
                "                                                import_fromString_pure_1 instance ImportGroup\n" +
                "                                            resolvedNode(Property):\n" +
                "                                                Any instance Class\n" +
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
                "                filter_T_MANY__Function_1__T_MANY_ instance NativeFunction\n" +
                "            functionName(Property):\n" +
                "                filter instance String\n" +
                "            genericType(Property):\n" +
                "                Anonymous_StripedId instance InferredGenericType\n" +
                "                    rawType(Property):\n" +
                "                        Employee instance Class\n" +
                "            importGroup(Property):\n" +
                "                import_fromString_pure_1 instance ImportGroup\n" +
                "            multiplicity(Property):\n" +
                "                ZeroMany instance PackageableMultiplicity\n" +
                "            parametersValues(Property):\n" +
                "                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                    func(Property):\n" +
                "                        getAll_Class_1__T_MANY_ instance NativeFunction\n" +
                "                    functionName(Property):\n" +
                "                        getAll instance String\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance InferredGenericType\n" +
                "                            rawType(Property):\n" +
                "                                Employee instance Class\n" +
                "                    importGroup(Property):\n" +
                "                        import_fromString_pure_1 instance ImportGroup\n" +
                "                    multiplicity(Property):\n" +
                "                        ZeroMany instance PackageableMultiplicity\n" +
                "                    parametersValues(Property):\n" +
                "                        Anonymous_StripedId instance InstanceValue\n" +
                "                            genericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Class instance Class\n" +
                "                                    typeArguments(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Employee instance Class\n" +
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
                "                                        Employee instance String\n" +
                "                                    importGroup(Property):\n" +
                "                                        import_fromString_pure_1 instance ImportGroup\n" +
                "                                    resolvedNode(Property):\n" +
                "                                        Employee instance Class\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                            functionExpression(Property):\n" +
                "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            offset(Property):\n" +
                "                                0 instance Integer\n" +
                "                Anonymous_StripedId instance InstanceValue\n" +
                "                    genericType(Property):\n" +
                "                        Anonymous_StripedId instance GenericType\n" +
                "                            rawType(Property):\n" +
                "                                LambdaFunction instance Class\n" +
                "                            typeArguments(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        Anonymous_StripedId instance FunctionType\n" +
                "                                            parameters(Property):\n" +
                "                                                Anonymous_StripedId instance VariableExpression\n" +
                "                                                    genericType(Property):\n" +
                "                                                        Anonymous_StripedId instance GenericType\n" +
                "                                                            rawType(Property):\n" +
                "                                                                Anonymous_StripedId instance ImportStub\n" +
                "                                                                    idOrPath(Property):\n" +
                "                                                                        Employee instance String\n" +
                "                                                                    importGroup(Property):\n" +
                "                                                                        import_fromString_pure_1 instance ImportGroup\n" +
                "                                                                    resolvedNode(Property):\n" +
                "                                                                        Employee instance Class\n" +
                "                                                    multiplicity(Property):\n" +
                "                                                        PureOne instance PackageableMultiplicity\n" +
                "                                                    name(Property):\n" +
                "                                                        t instance String\n" +
                "                                            returnMultiplicity(Property):\n" +
                "                                                PureOne instance PackageableMultiplicity\n" +
                "                                            returnType(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    rawType(Property):\n" +
                "                                                        Boolean instance PrimitiveType\n" +
                "                    multiplicity(Property):\n" +
                "                        PureOne instance PackageableMultiplicity\n" +
                "                    usageContext(Property):\n" +
                "                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                            functionExpression(Property):\n" +
                "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                            offset(Property):\n" +
                "                                1 instance Integer\n" +
                "                    values(Property):\n" +
                "                        getValue$1$system$imports$import_fromString_pure_1$1 instance LambdaFunction\n" +
                "                            classifierGenericType(Property):\n" +
                "                                Anonymous_StripedId instance GenericType\n" +
                "                                    rawType(Property):\n" +
                "                                        LambdaFunction instance Class\n" +
                "                                    typeArguments(Property):\n" +
                "                                        Anonymous_StripedId instance GenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Anonymous_StripedId instance FunctionType\n" +
                "                                                    function(Property):\n" +
                "                                                        getValue$1$system$imports$import_fromString_pure_1$1 instance LambdaFunction\n" +
                "                                                    parameters(Property):\n" +
                "                                                        Anonymous_StripedId instance VariableExpression\n" +
                "                                                            functionTypeOwner(Property):\n" +
                "                                                                Anonymous_StripedId instance FunctionType\n" +
                "                                                            genericType(Property):\n" +
                "                                                                Anonymous_StripedId instance GenericType\n" +
                "                                                                    rawType(Property):\n" +
                "                                                                        Anonymous_StripedId instance ImportStub\n" +
                "                                                                            idOrPath(Property):\n" +
                "                                                                                Employee instance String\n" +
                "                                                                            importGroup(Property):\n" +
                "                                                                                import_fromString_pure_1 instance ImportGroup\n" +
                "                                                                            resolvedNode(Property):\n" +
                "                                                                                Employee instance Class\n" +
                "                                                                    referenceUsages(Property):\n" +
                "                                                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                                            offset(Property):\n" +
                "                                                                                0 instance Integer\n" +
                "                                                                            owner(Property):\n" +
                "                                                                                Anonymous_StripedId instance VariableExpression\n" +
                "                                                                            propertyName(Property):\n" +
                "                                                                                genericType instance String\n" +
                "                                                            multiplicity(Property):\n" +
                "                                                                PureOne instance PackageableMultiplicity\n" +
                "                                                            name(Property):\n" +
                "                                                                t instance String\n" +
                "                                                    returnMultiplicity(Property):\n" +
                "                                                        PureOne instance PackageableMultiplicity\n" +
                "                                                    returnType(Property):\n" +
                "                                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                                            rawType(Property):\n" +
                "                                                                Boolean instance PrimitiveType\n" +
                "                                                            referenceUsages(Property):\n" +
                "                                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                                    offset(Property):\n" +
                "                                                                        0 instance Integer\n" +
                "                                                                    owner(Property):\n" +
                "                                                                        Anonymous_StripedId instance FunctionType\n" +
                "                                                                    propertyName(Property):\n" +
                "                                                                        returnType instance String\n" +
                "                            expressionSequence(Property):\n" +
                "                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                    func(Property):\n" +
                "                                        equal_Any_MANY__Any_MANY__Boolean_1_ instance NativeFunction\n" +
                "                                    functionName(Property):\n" +
                "                                        equal instance String\n" +
                "                                    genericType(Property):\n" +
                "                                        Anonymous_StripedId instance InferredGenericType\n" +
                "                                            rawType(Property):\n" +
                "                                                Boolean instance PrimitiveType\n" +
                "                                    importGroup(Property):\n" +
                "                                        import_fromString_pure_1 instance ImportGroup\n" +
                "                                    multiplicity(Property):\n" +
                "                                        PureOne instance PackageableMultiplicity\n" +
                "                                    parametersValues(Property):\n" +
                "                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                            func(Property):\n" +
                "                                                name instance Property\n" +
                "                                                    aggregation(Property):\n" +
                "                                                        None instance AggregationKind\n" +
                "                                                            name(Property):\n" +
                "                                                                None instance String\n" +
                "                                                    applications(Property):\n" +
                "                                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    classifierGenericType(Property):\n" +
                "                                                        Anonymous_StripedId instance GenericType\n" +
                "                                                            multiplicityArguments(Property):\n" +
                "                                                                PureOne instance PackageableMultiplicity\n" +
                "                                                            rawType(Property):\n" +
                "                                                                Property instance Class\n" +
                "                                                            referenceUsages(Property):\n" +
                "                                                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                                    offset(Property):\n" +
                "                                                                        0 instance Integer\n" +
                "                                                                    owner(Property):\n" +
                "                                                                        name instance Property\n" +
                "                                                                    propertyName(Property):\n" +
                "                                                                        classifierGenericType instance String\n" +
                "                                                            typeArguments(Property):\n" +
                "                                                                Anonymous_StripedId instance GenericType\n" +
                "                                                                    rawType(Property):\n" +
                "                                                                        Anonymous_StripedId instance ImportStub\n" +
                "                                                                            idOrPath(Property):\n" +
                "                                                                                Employee instance String\n" +
                "                                                                            importGroup(Property):\n" +
                "                                                                                import_fromString_pure_1 instance ImportGroup\n" +
                "                                                                            resolvedNode(Property):\n" +
                "                                                                                Employee instance Class\n" +
                "                                                                    referenceUsages(Property):\n" +
                "                                                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                                            offset(Property):\n" +
                "                                                                                0 instance Integer\n" +
                "                                                                            owner(Property):\n" +
                "                                                                                Anonymous_StripedId instance GenericType\n" +
                "                                                                            propertyName(Property):\n" +
                "                                                                                typeArguments instance String\n" +
                "                                                                Anonymous_StripedId instance GenericType\n" +
                "                                                                    rawType(Property):\n" +
                "                                                                        String instance PrimitiveType\n" +
                "                                                                    referenceUsages(Property):\n" +
                "                                                                        Anonymous_StripedId instance ReferenceUsage\n" +
                "                                                                            offset(Property):\n" +
                "                                                                                1 instance Integer\n" +
                "                                                                            owner(Property):\n" +
                "                                                                                Anonymous_StripedId instance GenericType\n" +
                "                                                                            propertyName(Property):\n" +
                "                                                                                typeArguments instance String\n" +
                "                                                    genericType(Property):\n" +
                "                                                        Anonymous_StripedId instance GenericType\n" +
                "                                                            rawType(Property):\n" +
                "                                                                String instance PrimitiveType\n" +
                "                                                    multiplicity(Property):\n" +
                "                                                        PureOne instance PackageableMultiplicity\n" +
                "                                                    name(Property):\n" +
                "                                                        name instance String\n" +
                "                                                    owner(Property):\n" +
                "                                                        Employee instance Class\n" +
                "                                            genericType(Property):\n" +
                "                                                Anonymous_StripedId instance InferredGenericType\n" +
                "                                                    rawType(Property):\n" +
                "                                                        String instance PrimitiveType\n" +
                "                                            importGroup(Property):\n" +
                "                                                import_fromString_pure_1 instance ImportGroup\n" +
                "                                            multiplicity(Property):\n" +
                "                                                PureOne instance PackageableMultiplicity\n" +
                "                                            parametersValues(Property):\n" +
                "                                                Anonymous_StripedId instance VariableExpression\n" +
                "                                                    genericType(Property):\n" +
                "                                                        Anonymous_StripedId instance GenericType\n" +
                "                                                            rawType(Property):\n" +
                "                                                                Anonymous_StripedId instance ImportStub\n" +
                "                                                                    idOrPath(Property):\n" +
                "                                                                        Employee instance String\n" +
                "                                                                    importGroup(Property):\n" +
                "                                                                        import_fromString_pure_1 instance ImportGroup\n" +
                "                                                                    resolvedNode(Property):\n" +
                "                                                                        Employee instance Class\n" +
                "                                                    multiplicity(Property):\n" +
                "                                                        PureOne instance PackageableMultiplicity\n" +
                "                                                    name(Property):\n" +
                "                                                        t instance String\n" +
                "                                                    usageContext(Property):\n" +
                "                                                        Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                                            functionExpression(Property):\n" +
                "                                                                Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                            offset(Property):\n" +
                "                                                                0 instance Integer\n" +
                "                                            propertyName(Property):\n" +
                "                                                getValue$1$system$imports$import_fromString_pure_1$0 instance InstanceValue\n" +
                "                                                    genericType(Property):\n" +
                "                                                        Anonymous_StripedId instance GenericType\n" +
                "                                                            rawType(Property):\n" +
                "                                                                String instance PrimitiveType\n" +
                "                                                    multiplicity(Property):\n" +
                "                                                        PureOne instance PackageableMultiplicity\n" +
                "                                                    values(Property):\n" +
                "                                                        name instance String\n" +
                "                                            usageContext(Property):\n" +
                "                                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                                    functionExpression(Property):\n" +
                "                                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    offset(Property):\n" +
                "                                                        0 instance Integer\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                            genericType(Property):\n" +
                "                                                Anonymous_StripedId instance GenericType\n" +
                "                                                    rawType(Property):\n" +
                "                                                        String instance PrimitiveType\n" +
                "                                            multiplicity(Property):\n" +
                "                                                PureOne instance PackageableMultiplicity\n" +
                "                                            usageContext(Property):\n" +
                "                                                Anonymous_StripedId instance ParameterValueSpecificationContext\n" +
                "                                                    functionExpression(Property):\n" +
                "                                                        Anonymous_StripedId instance SimpleFunctionExpression\n" +
                "                                                    offset(Property):\n" +
                "                                                        1 instance Integer\n" +
                "                                            values(Property):\n" +
                "                                                cool instance String\n" +
                "                                    usageContext(Property):\n" +
                "                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                                            functionDefinition(Property):\n" +
                "                                                getValue$1$system$imports$import_fromString_pure_1$1 instance LambdaFunction\n" +
                "                                            offset(Property):\n" +
                "                                                0 instance Integer\n" +
                "                            referenceUsages(Property):\n" +
                "                                Anonymous_StripedId instance ReferenceUsage\n" +
                "                                    offset(Property):\n" +
                "                                        0 instance Integer\n" +
                "                                    owner(Property):\n" +
                "                                        Anonymous_StripedId instance InstanceValue\n" +
                "                                    propertyName(Property):\n" +
                "                                        values instance String\n" +
                "            usageContext(Property):\n" +
                "                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext\n" +
                "                    functionDefinition(Property):\n" +
                "                        getValue_Any_1__String_1__Any_MANY_ instance ConcreteFunctionDefinition\n" +
                "                    offset(Property):\n" +
                "                        0 instance Integer\n" +
                "    functionName(Property):\n" +
                "        getValue instance String\n" +
                "    name(Property):\n" +
                "        getValue_Any_1__String_1__Any_MANY_ instance String\n" +
                "    package(Property):\n" +
                "        Root instance Package", runtime.getCoreInstance("getValue_Any_1__String_1__Any_MANY_").printWithoutDebug("", 10));

    }

    @Test
    public void testFunctionWithUnitName()
    {
        compileTestSource(
                "fromString.pure",
                "import meta::pure::functions::meta::tests::model::*;\n" +
                        "function pkg::myTestFunction(input:RomanLength~Cubitum[1]):RomanLength~Pes[1]\n" +
                        "{\n" +
                        "    let valuePes = RomanLength~Cubitum.conversionFunction->toOne()->cast(@Function<{Number[1]->Number[1]}>)->eval($input->getUnitValue());\n" +
                        "    newUnit(RomanLength~Actus, $valuePes)->cast(@RomanLength~Pes);\n" +
                        "}\n"
        );
        CoreInstance func = runtime.getCoreInstance("pkg::myTestFunction_RomanLength$Cubitum_1__RomanLength$Pes_1_");
        Assert.assertEquals("myTestFunction_RomanLength$Cubitum_1__RomanLength$Pes_1_", func.getName());
    }
}
