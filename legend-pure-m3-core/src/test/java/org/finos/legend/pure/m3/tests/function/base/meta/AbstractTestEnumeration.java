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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestEnumeration extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testEnumeration()
    {
        compileTestSource("Enum BooleanEnum\n" +
                "{\n" +
                "   TRUE, FALSE\n" +
                "}\n" +
                "function testPrint():Nil[0]\n" +
                "{\n" +
                "    print(BooleanEnum.TRUE->id(), 1);\n" +
                "    print(BooleanEnum.FALSE->id(), 1);\n" +
                "}\n");
        this.execute("testPrint():Nil[0]");
        Assert.assertEquals("'TRUE'", this.functionExecution.getConsole().getLine(0));
        Assert.assertEquals("'FALSE'", this.functionExecution.getConsole().getLine(1));
    }

    @Test
    public void testEnumerationAsFuncParam()
    {
        compileTestSource("Enum BooleanEnum\n" +
                "{\n" +
                "   TRUE, FALSE\n" +
                "}\n" +
                "function testPrint():Nil[0]\n" +
                "{\n" +
                "    other(BooleanEnum.TRUE);\n" +
                "}\n" +
                "function other(b:BooleanEnum[1]):Nil[0]\n" +
                "{\n" +
                "   print($b->id(), 1);\n" +
                "}\n");
        this.execute("testPrint():Nil[0]");
        Assert.assertEquals("'TRUE'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testEnumerationVariable()
    {
        compileTestSource("Enum BooleanEnum\n" +
                "{\n" +
                "   TRUE, FALSE\n" +
                "}\n" +
                "function testPrint():Nil[0]\n" +
                "{\n" +
                "    let a = BooleanEnum.TRUE;\n" +
                "    print($a->genericType().rawType->at(0)->id()+'.'+$a->id(), 1);\n" +
                "}");
        this.execute("testPrint():Nil[0]");
        Assert.assertEquals("'BooleanEnum.TRUE'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testEnumerationUsedAsAPropertyType()
    {
        compileTestSource("Enum BooleanEnum\n" +
                "{\n" +
                "   TRUE, FALSE\n" +
                "}\n" +
                "\n" +
                "Class MyClass\n" +
                "{\n" +
                "   prop : BooleanEnum[1];\n" +
                "   prop2 : BooleanEnum[*];\n" +
                "}\n" +
                "\n" +
                "function testPrint():Nil[0]\n" +
                "{" +
                "    let test = ^MyClass test(prop = BooleanEnum.TRUE, prop2 = [BooleanEnum.FALSE, BooleanEnum.TRUE]);\n" +
                "    print($test.prop->id(), 1);\n" +
                "    print($test.prop2->at(0)->id(), 1);\n" +
                "}\n");
        this.execute("testPrint():Nil[0]");
        Assert.assertEquals("'TRUE'", this.functionExecution.getConsole().getLine(0));
        Assert.assertEquals("'FALSE'", this.functionExecution.getConsole().getLine(1));
    }

    @Test
    public void testInvalidEnumReference()
    {
        compileTestSource("enumDefinition.pure", "Enum test::TestEnum {VAL1, VAL2}");
        try
        {
            compileTestSource("enumReference.pure", "function test::test():test::TestEnum[1] { test::TestEnum.VAL3 }");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The enum value 'VAL3' can't be found in the enumeration test::TestEnum", "enumReference.pure", 1, 58, 1, 58, 1, 61, e);
        }
    }

    @Test
    public void testInvalidEnumReferenceInQualifiedProperty()
    {
        compileTestSource("enumDefinition.pure", "Enum test::TestEnum {VAL1, VAL2}");
        try
        {
            compileTestSource("enumReference.pure", "Class test::TestClass { test() { test::TestEnum.VAL3 }:test::TestEnum[1]; }");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The enum value 'VAL3' can't be found in the enumeration test::TestEnum", "enumReference.pure", 1, 49, 1, 49, 1, 52, e);
        }
    }

    @Test
    public void testEnumerationInCollection()
    {
        compileTestSource("/test/model.pure",
                "Enum test::Enum1\n" +
                        "{\n" +
                        "  A, B, C\n" +
                        "}\n");
        compileTestSource("/test/test.pure",
                "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  [test::Enum1]\n" +
                        "}\n");
        execute("test::testFn():Any[*]");
    }

    @Test
    public void testEnumerationInDoubleCollection()
    {
        compileTestSource("/test/model.pure",
                "Enum test::Enum1\n" +
                        "{\n" +
                        "  A, B, C\n" +
                        "}\n");
        compileTestSource("/test/test.pure",
                "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  [[test::Enum1]]\n" +
                        "}\n");
        execute("test::testFn():Any[*]");
    }

    @Test
    public void testNewEnumeration()
    {

        compileTestSource("/test/test.pure",
                "function test::testFn():Any[*]\n" +
                        "{\n" +
                        " let  testEnum =  newEnumeration('test::testEnum',['value1','value2']);" +
                        "assert($testEnum->instanceOf(Enumeration), |'');"+
                        "assert($testEnum->subTypeOf(Enum), |'');" +
                        "$testEnum->enumValues()->map(e|assert($e->instanceOf(Enum), |'')); " +
                        "$testEnum->enumValues()->map(e|$e->id())->print(1);\n" +
                        "}\n");
        execute("test::testFn():Any[*]");
        Assert.assertEquals("[\n" +
                "   'value1'\n" +
                "   'value2'\n" +
                "]", this.functionExecution.getConsole().getLine(0));
    }
}
