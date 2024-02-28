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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractTestCopyAtRuntime extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testCopyWithReverseZeroToOneProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($newCar.owner.car->size()->toString(), 1);\n" +
                "   print($newCar.owner->size()->toString(), 1);\n" +
                "   print($newCar.name, 1);\n" +
                "   $car;\n" +
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
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assert.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse properties for a zero-to-one association.");
        }
    }

    @Test
    public void testCopyIncrementIntegerProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', accidents=0);\n" +
                "   let newCar = ^$car(accidents = $car.accidents + 1);\n" +
                "   print($newCar.accidents, 1);\n" +
                "   $car;\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "test::Car\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   accidents: Integer[1];\n" +
                "}\n" +
                "\n");
        try
        {
            this.execute("test():Any[*]");
            Assert.assertEquals("1", functionExecution.getConsole().getLine(0).substring(0,1));
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            Assert.fail("Assert.failed to increment the Integer property on Copy.");
        }
    }

    @Test
    public void testCopyWithReverseZeroToManyProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($newCar.owner.cars->size()->toString(), 1);\n" +
                "   print($newCar.owner->size()->toString(), 1);\n" +
                "   print($newCar.name, 1);\n" +
                "   $car;\n" +
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
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assert.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse properties for a zero-to-many association.");
        }
    }

    @Test
    public void testCopyWithReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($newCar.owner.car->size()->toString(), 1);\n" +
                "   print($newCar.owner->size()->toString(), 1);\n" +
                "   print($newCar.name, 1);\n" +
                "   $car;\n" +
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
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assert.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse properties for a one-to-one association.");
        }
    }

    @Test
    public void testCopyWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));\n" +
                "   print($newCar.owner.cars->size()->toString(), 1);\n" +
                "   print($newCar.owner->size()->toString(), 1);\n" +
                "   print($newCar.name, 1);\n" +
                "   $car;\n" +
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
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assert.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse properties for a one-to-many association.");
        }
    }

    @Test
    public void testCopyWithChildWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe', cars=[^test::Car(name='Audi')]));\n" +
                "   let newCar = ^$car(name='Veyron');\n" +
                "   print($newCar.owner.cars->size()->toString(), 1);\n" +
                "   print($newCar.owner.cars->sortBy(c|$c.name)->at(0).name, 1);\n" +
                "   print($newCar.owner.cars->sortBy(c|$c.name)->at(1).name, 1);\n" +
                "   print($newCar.owner.cars->sortBy(c|$c.name)->at(2).name, 1);\n" +
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
            Assert.assertEquals("'3'", functionExecution.getConsole().getLine(0));
            Assert.assertEquals("'Audi'", functionExecution.getConsole().getLine(1));
            Assert.assertEquals("'Bugatti'", functionExecution.getConsole().getLine(2));
            Assert.assertEquals("'Veyron'", functionExecution.getConsole().getLine(3));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse property of a child for a one-to-many association.");
        }
    }

    @Test
    public void testCopyWithRedefinedManyToManyAssociation()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let john = ^test::Owner(firstName='John', lastName='Roe');\n" +
                "   let pierre = ^$john(firstName='Pierre', lastName='Doe');\n" +
                "   \n" +
                "   let audi = ^test::Car(name='Audi', owners=[$john]);\n" +
                "   let bugatti = ^$audi(name='Bugatti', owners=[$pierre]);\n" +
                "   \n" +
                "   print($john.cars->size()->toString(), 1);\n" +
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
                "   owners : test::Owner[0..*];\n" +
                "   cars  : test::Car[0..*];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            Assert.assertEquals("'1'", functionExecution.getConsole().getLine(0));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse property of a child for a many-to-many association.");
        }
    }

    @Ignore
    @Test
    public void testCopyWithRedefinedOneToOneAssociation()
    {
        compileTestSource("fromString.pure","function test(): Any[*]\n" +
                "{\n" +
                "   let audi = ^test::Car(name='Audi');\n" +
                "   let bugatti = ^$audi(name='Bugatti');\n" +
                "   \n" +
                "   let john = ^test::Owner(firstName='John', lastName='Roe', car=$audi);\n" +
                "   let pierre = ^$john(firstName='Pierre', lastName='Doe', car=$bugatti);\n" +
                "\n" +
                "   print($audi.owner->isEmpty()->toString());\n" +
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
                "   owner : test::Owner[0..1];\n" +
                "   car  : test::Car[0..1];\n" +
                "}");
        try
        {
            this.execute("test():Any[*]");
            Assert.assertEquals("false", functionExecution.getConsole().getLine(0));
        }
        catch (Exception e)
        {
            Assert.fail("Assert.failed to set the reverse property of a child for a many-to-many association.");
        }
    }

    @Test
    public void testCopyParametrizedClassWithEmptyPropertiesSet()
    {
        String source =
                "Class A<T1, T2> \n{ prop1:T1[*];\n prop2:T2[*]; }\n" +
                        "Class B<T> \n{ prop1:String[*];\n prop2:T[*]; }\n" +
                        "function test::testFn():Any[*] { let a = ^A<String, Integer>(prop1='string', prop2=[1,2]); let a1 = ^$a(prop2=[]); ^B<Integer>(prop1='Hello', prop2=[]);}\n";
        compileTestSource("fromString.pure",source);
        this.compileAndExecute("test::testFn():Any[*]");
    }

    @Test
    public void testSourceInformationCopy()
    {
        String source =
                        "function test::testFn():Any[*] {" +
                                "   let x0 = meta::pure::functions::collection::removeDuplicates_T_MANY__T_MANY_->evaluateAndDeactivate();\n" +
                                "   let x1 = ^$x0();\n" +
                                "   let x2 = ^$x0(expressionSequence = $x0.expressionSequence);\n" +
                                " \n" +
                                "   assert($x0->sourceInformation().source == $x1->sourceInformation().source, |'');\n" +
                                "   assert($x0->sourceInformation().source != $x2->sourceInformation().source, |'');" +
                                "}\n";
        compileTestSource("fromString.pure",source);
        this.compileAndExecute("test::testFn():Any[*]");
    }
}
