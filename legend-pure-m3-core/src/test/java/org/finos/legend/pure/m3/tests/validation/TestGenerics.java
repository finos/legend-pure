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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGenerics extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("fromString.pure");
         try{
            runtime.compile();
         } catch (PureCompilationException e) {
            setUp();
         }
    }

    @Test
    public void testGenericInstanceWithoutTypeArguments()
    {
        try
        {
            compileTestSource("fromString.pure","Class Address\n" +
                    "{\n" +
                    "   value:String[1];\n" +
                    "}\n" +
                    "Class Employee<E>\n" +
                    "{\n" +
                    "   name : String[1];\n" +
                    "   address:E[*];\n" +
                    "}\n" +
                    "^Employee(name='test', address = ^Address(value='coool'))");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 0): Employee", 10, 2, e);
        }
    }

    @Test
    public void testGenericInstanceTooManyTypeArguments()
    {
        try
        {
            compileTestSource("fromString.pure","Class Address\n" +
                           "{\n" +
                           "   value:String[1];\n" +
                           "}\n" +
                           "Class Employee<E>\n" +
                           "{\n" +
                           "   name : String[1];\n" +
                           "   address:E[*];\n" +
                           "}\n" +
                           "^Employee<Address,Address> emp(name='test', address = ^Address(value='coool'))");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 2): Employee<Address, Address>", 10, 2, e);
        }
    }

    @Test
    public void testGenericInstance()
    {
        Loader.parseM3("Class Address" +
                       "{" +
                       "   value:String[1];" +
                       "}" +
                       "" +
                       "Class Employee<E>" +
                       "{" +
                       "   name : String[1];" +
                       "   address:E[*];" +
                       "}" +
                       "" +
                       "^Employee<Address> emp (name='test', address = ^Address(value='coool'))", this.repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
        CoreInstance elem = this.runtime.getCoreInstance("emp");
        Assert.assertEquals("emp instance Employee\n" +
                            "    address(Property):\n" +
                            "        Anonymous_StripedId instance Address\n" +
                            "            value(Property):\n" +
                            "                coool instance String\n" +
                            "    classifierGenericType(Property):\n" +
                            "        Anonymous_StripedId instance GenericType\n" +
                            "            rawType(Property):\n" +
                            "                Employee instance Class\n" +
                            "            typeArguments(Property):\n" +
                            "                Anonymous_StripedId instance GenericType\n" +
                            "                    rawType(Property):\n" +
                            "                        Anonymous_StripedId instance ImportStub\n" +
                            "                            idOrPath(Property):\n" +
                            "                                Address instance String\n" +
                            "                            importGroup(Property):\n" +
                            "                                import_fromString_pure_1 instance ImportGroup\n" +
                            "                            resolvedNode(Property):\n" +
                            "                                Address instance Class\n" +
                            "    name(Property):\n" +
                            "        test instance String", elem.printWithoutDebug("", 10));

        setUp();
    }

    @Test
    public void testGenericInstanceUnknownType()
    {
        try
        {
            Loader.parseM3("Class Employee<E>\n" +
                           "{\n" +
                           "   name : String[1];\n" +
                           "   address:E[*];\n" +
                           "}\n" +
                           "\n" +
                           "^Employee<AddressXX> emp (name='test')", this.repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "AddressXX has not been defined!", 7, 11, e);
            setUp();
        }
    }

    @Test
    public void testGenericInstanceTypeMismatch()
    {
        try
        {
            Loader.parseM3("Class Address\n" +
                           "{\n" +
                           "   value:String[1];\n" +
                           "}\n" +
                           "Class OtherType\n" +
                           "{\n" +
                           "}\n" +
                           "Class Employee<Add>\n" +
                           "{\n" +
                           "   name : String[1];\n" +
                           "   address:Add[*];\n" +
                           "}\n" +
                           "^Employee<OtherType> emp (name='test', address = ^Address(value='coool'))", this.repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property: 'address' / Type Error: 'Address' not a subtype of 'OtherType'", 13, 51, e);
            setUp();
        }
    }

    @Test
    public void testGenericInstanceWithoutTypeArgumentsInFunction()
    {
        try
        {
            compileTestSource("fromString.pure","Class Address\n" +
                    "{\n" +
                    "   value:String[1];\n" +
                    "}\n" +
                    "Class Employee<E>\n" +
                    "{\n" +
                    "   name : String[1];\n" +
                    "   address:E[*];\n" +
                    "}\n" +
                    "function test():Any[1]\n" +
                    "{\n" +
                    "   let a = ^Employee(name='test', address = ^Address(value='coool'))\n" +
                    "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 0): Employee", 12, 13, e);
        }
    }

    @Test
    public void testGenericInstanceWithTooManyTypeArgumentsInFunction()
    {
        try
        {
            compileTestSource("fromString.pure","Class Address\n" +
                           "{\n" +
                           "   value:String[1];\n" +
                           "}\n" +
                           "" +
                           "Class Employee<E>\n" +
                           "{\n" +
                           "   name : String[1];\n" +
                           "   address:E[*];\n" +
                           "}\n" +
                           "function test():Any[1]\n" +
                           "{\n" +
                           "   let a = ^Employee<Address,Address>(name='test', address = ^Address(value='coool'))\n" +
                           "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 2): Employee<Address, Address>", 12, 13, e);
        }
    }

    @Test
    public void testGenericInstanceTypeMismatchInFunction()
    {
        try
        {
            runtime.createInMemorySource("fcdffdf.pure", "Class Address\n" +
                    "{\n" +
                    "   value:String[1];\n" +
                    "}\n" +
                    "Class OtherType\n" +
                    "{\n" +
                    "}\n" +
                    "Class Employee<E>\n" +
                    "{\n" +
                    "   name : String[1];\n" +
                    "   address:E[*];\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "   let a = ^Employee<OtherType>(name='test', address = ^Address(value='coool'));\n" +
                    "   print($a,1);\n" +
                    "}");
            runtime.compile();
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type Error: Address not a subtype of OtherType", 15, 54, e);
        }
    }

    @Test
    public void testGenericWithGeneralizationNoArguments()
    {
        try
        {
            compileTestSource("fromString.pure","Class A<E>\n" +
                    "{\n" +
                    "   value:E[1];\n" +
                    "}\n" +
                    "Class B extends A\n" +
                    "{\n" +
                    "   name : String[1];\n" +
                    "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class A<E> (expected 1, got 0): A", 5, 17, e);
        }
    }

    @Test
    public void testGenericWithGeneralization()
    {
        compileTestSource("fromString.pure","Class A<E>\n" +
                       "{\n" +
                       "   value:E[1];\n" +
                       "}\n" +
                       "Class C\n" +
                       "{\n" +
                       "}\n" +
                       "Class B extends A<C>\n" +
                       "{\n" +
                       "   name : String[1];\n" +
                       "}\n" +
                       "\n" +
                       "^B x (name='test', value=^C())");

        Assert.assertEquals("x instance B\n" +
                            "    name(Property):\n" +
                            "        test instance String\n" +
                            "    value(Property):\n" +
                            "        Anonymous_StripedId instance C", this.runtime.getCoreInstance("x").printWithoutDebug(""));
    }

    @Test
    public void testGenericWithGeneralizationWrongType()
    {
        try
        {
            Loader.parseM3("Class A<E>\n" +
                           "{\n" +
                           "   value:E[1];\n" +
                           "}\n" +
                           "Class C\n" +
                           "{\n" +
                           "}\n" +
                           "Class D\n" +
                           "{\n" +
                           "}\n" +
                           "Class B extends A<C>\n" +
                           "{\n" +
                           "   name : String[1];\n" +
                           "}\n" +
                           "^B x (name='test', value=^D())", this.repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, this.context);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property: 'value' / Type Error: 'D' not a subtype of 'C'", 15, 27, e);
        }
    }

    @Test
    public void testGenericWithGeneralizationWrongTypeInFunction()
    {
        try
        {
            compileTestSource("fromString.pure","Class A<E>\n" +
                    "{\n" +
                    "   value:E[1];\n" +
                    "}\n" +
                    "Class C\n" +
                    "{\n" +
                    "}\n" +
                    "Class D\n\n" +
                    "{\n" +
                    "}\n" +
                    "Class B extends A<C>\n" +
                    "{\n" +
                    "   name : String[1];\n" +
                    "}\n" +
                    "function test():Any[1]\n" +
                    "{\n" +
                    "   let a = ^B x (name='test', value=^D())\n" +
                    "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type Error: D not a subtype of C", 18, 36, e);
        }
    }

    @Test
    public void testGenericWithGeneralizationChainTypeParamWrongType()
    {
        try
        {
            compileTestSource("fromString.pure","Class A<T>\n" +
                    "{\n" +
                    "   value:T[1];\n" +
                    "}\n" +
                    "Class C\n" +
                    "{\n" +
                    "}\n" +
                    "Class D\n" +
                    "{\n" +
                    "}\n" +
                    "Class B<U> extends A<U>\n" +
                    "{\n" +
                    "   name : String[1];\n" +
                    "}\n" +
                    "function test():Any[1]\n" +
                    "{\n" +
                    "   let a = ^B<C> x (name='test', value=^D())\n" +
                    "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type Error: D not a subtype of C", 17, 39, e);
        }
    }

    @Test
    public void testGenericsUsedAsParameterError()
    {
        try
        {
            compileTestSource("fromString.pure","Class Person<T>{firstName:T[1];}\n" +
                           "function test(p:Person[1]):Nil[0]\n" +
                           "{\n" +
                           "   [];\n" +
                           "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Person<T> (expected 1, got 0): Person", 2, 17, e);
        }
    }

    @Test
    public void testGenericsUsedAsReturnTypeError()
    {
        try
        {
            compileTestSource("fromString.pure","Class Person<T>{firstName:T[1];}\n" +
                    "function test():Person[*]\n" +
                    "{\n" +
                    "   [];\n" +
                    "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Person<T> (expected 1, got 0): Person", 2, 17, e);
        }
    }

    @Test
    public void testGenericAsExtendsError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    "Class SuperClass<T>\n" +
                            "{\n" +
                            "  prop:T[1];\n" +
                            "}\n" +
                            "Class SubClass extends SuperClass\n" +
                            "{\n" +
                            "}\n");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class SuperClass<T> (expected 1, got 0): SuperClass", "fromString.pure", 5, 24, e);
        }
    }
}
