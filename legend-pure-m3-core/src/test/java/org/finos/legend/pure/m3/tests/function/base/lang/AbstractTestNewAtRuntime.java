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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestNewAtRuntime extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testGetterFromDynamicInstanceWithWrongProperty()
    {
        try
        {
            compileTestSource("fromString.pure","Class test::Person\n" +
                    "{\n" +
                    "   lastName:String[1];\n" +
                    "}\n" +
                    "function testGet():Nil[0]\n" +
                    "{\n" +
                    "   let p = ^test::Person(lastName='last');\n" +
                    "   print($p.wrongProperty);\n" +
                    "}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Can't find the property 'wrongProperty' in the class test::Person", 8, 13, e);
        }
    }

    @Test
    public void testNewWithInvalidProperty()
    {
        try
        {
            compileTestSource("fromString.pure","Class Person\n" +
                    "{\n" +
                    "   lastName:String[1];\n" +
                    "}\n" +
                    "function testNew():Person[1]\n" +
                    "{\n" +
                    "   ^Person(lastName='last', wrongProperty='wrong');\n" +
                    "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The property 'wrongProperty' can't be found in the type 'Person' or in its hierarchy.", 7, 29, e);
        }
    }

    @Test
    public void testNewNil() throws Exception
    {
        compileTestSource("fromString.pure","function testNewNil():Nil[1]\n" +
                "{\n" +
                "    ^Nil();\n" +
                "}");
        try
        {
            this.execute("testNewNil():Nil[1]");
            Assert.fail("Should not be able to instantiate Nil");

        }
        catch (Exception e)
        {
            assertNewNilException(e);
        }
    }

    protected void assertNewNilException(Exception e)
    {
        assertPureException(PureExecutionException.class, "Cannot instantiate meta::pure::metamodel::type::Nil", 3, 5, e);
    }

    @Test
    public void testNewWithReverseZeroToOneProperty()
    {
        this.compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($car.owner.car->size()->toString(), 1);\n" +
                "   $car;" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   car  : test::Car[0..1];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            String result = this.functionExecution.getConsole().getLine(0);
            Assert.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assert.fail("Failed to set the reverse properties for a zero-to-one association.");
        }
    }

    @Test
    public void testNewWithReverseZeroToManyProperty()
    {
        this.compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($car.owner.cars->size()->toString(), 1);\n" +
                "   $car;" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   cars  : test::Car[0..*];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            String result = this.functionExecution.getConsole().getLine(0);
            Assert.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assert.fail("Failed to set the reverse properties for a zero-to-many association.");
        }
    }

    @Test
    public void testNewWithReverseOneToOneProperty()
    {
        this.compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($car.owner.car->size()->toString(), 1);\n" +
                "   $car;" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   car  : test::Car[1];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            String result = this.functionExecution.getConsole().getLine(0);
            Assert.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assert.fail("Failed to set the reverse properties for a one-to-one association.");
        }
    }

    @Test
    public void testNewWithReverseOneToManyProperty()
    {
        this.compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($car.owner.cars->size()->toString(), 1);\n" +
                "   $car;" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   cars  : test::Car[1..*];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            String result = this.functionExecution.getConsole().getLine(0);
            Assert.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assert.fail("Failed to set the reverse properties for a one-to-many association.");
        }
    }

    @Test
    public void testNewWithChildWithReverseOneToManyProperty()
    {
        this.compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe', cars=[^test::Car(name='Audi')]));\n" +
                "   print($car.owner.cars->size()->toString(), 1);\n" +
                "   print($car.owner.cars->sortBy(c|$c.name)->at(0).name, 1);\n" +
                "   print($car.owner.cars->sortBy(c|$c.name)->at(1).name, 1);\n" +
                "   $car;" +
                "}\n" +
                "function meta::pure::functions::collection::sortBy<T,U|m>(col:T[m], key:Function<{T[1]->U[1]}>[0..1]):T[m]\n" +
                "{\n" +
                "    sort($col, $key, [])\n" +
                "}\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Owner\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Car_Owner\n" +
                "{\n" +
                "   owner : test::Owner[1];\n" +
                "   cars  : test::Car[1..*];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            Assert.assertEquals("'2'", this.functionExecution.getConsole().getLine(0));
            Assert.assertEquals("'Audi'", this.functionExecution.getConsole().getLine(1));
            Assert.assertEquals("'Bugatti'", this.functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assert.fail("Failed to set the reverse property of a child for a one-to-many association.");
        }
    }

    @Test
    public void testNewWithZeroToOneAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                "import test::*;\n" +
                        "Class test::TestClassA\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClassB\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::TestAssocAB\n" +
                        "{\n" +
                        "  toB : TestClassB[0..1];\n" +
                        "  toA : TestClassA[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  let a = ^TestClassA(name='A', toB=[]);\n" +
                        "  assert('A' == $a.name, |'');\n" +
                        "  assert($a.toB->isEmpty(), |'');\n" +
                        "}\n");
        CoreInstance func = this.runtime.getFunction("test::testFn():Any[*]");
        this.functionExecution.start(func, Lists.immutable.<CoreInstance>empty());
    }

    @Test
    public void testNewWithZeroToManyAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                "import test::*;\n" +
                        "Class test::TestClassA\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClassB\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::TestAssocAB\n" +
                        "{\n" +
                        "  toB : TestClassB[*];\n" +
                        "  toA : TestClassA[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  let a = ^TestClassA(name='A', toB=[]);\n" +
                        "  assert('A' == $a.name, |'');\n" +
                        "  assert($a.toB->isEmpty(), |'');\n" +
                        "}\n");
        CoreInstance func = this.runtime.getFunction("test::testFn():Any[*]");
        this.functionExecution.start(func, Lists.immutable.<CoreInstance>empty());
    }

    @Test
    public void testNewParametrizedClassWithEmptyPropertiesSet()
    {
        String source =
                "Class A<T1, T2> \n{ prop1:T1[*];\n prop2:T2[*]; }\n" +
                        "Class B<T> \n{ prop1:String[*];\n prop2:T[*]; }\n" +
                        "function test::testFn():Any[*] { ^A<String, Integer>(prop1=[], prop2=[]); ^B<Integer>(prop1='Hello', prop2=[]);}\n"+
                        "function test::testGenericFn<R, T>():Any[*] { ^A<R, T>(prop1=[], prop2=[]); }\n";
        this.compileTestSource("fromString.pure",source);
        this.compileAndExecute("test::testFn():Any[*]");
        // TODO should this be allowed?
//        this.compileAndExecute("test::testGenericFn():Any[*]");
    }

    @Test
    public abstract void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToOneProperty();

    @Test
    public abstract void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToManyProperty();
}
