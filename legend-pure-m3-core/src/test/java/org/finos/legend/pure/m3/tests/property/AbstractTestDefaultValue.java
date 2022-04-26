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

package org.finos.legend.pure.m3.tests.property;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestDefaultValue extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final String DECLARATION = "import test::long::path::*;\n"
            + "Class my::exampleRootType\n"
            + "{\n"
            + "}\n"

            + "Class my::exampleSubType extends my::exampleRootType\n"
            + "{\n"
            + "}\n"

            + "Enum test::long::path::EnumWithDefault\n"
            + "{\n"
            + "   DefaultValue,\n"
            + "   AnotherValue\n"
            + "}\n"

            + "Class test::long::path::A\n"
            + "{\n"
            + "   stringProperty:String[1] = 'default';\n"
            + "   classProperty:my::exampleRootType[1] = ^my::exampleRootType();\n"
            + "   enumProperty:EnumWithDefault[1] = EnumWithDefault.DefaultValue;\n"
            + "   floatProperty:Float[1] = 0.12;\n"
            + "   inheritProperty:Number[1] = 0.12;\n"
            + "   booleanProperty:Boolean[1] = false;\n"
            + "   integerProperty:Integer[1] = 0;\n"
            + "   collectionProperty:String[1..*] = ['one', 'two'];\n"
            + "   enumCollection:test::long::path::EnumWithDefault[1..*] = [test::long::path::EnumWithDefault.DefaultValue, test::long::path::EnumWithDefault.AnotherValue];\n"
            + "   classCollection:my::exampleRootType[1..4] = [^my::exampleRootType(), ^my::exampleSubType()];\n"
            + "   singleProperty:String[1] = ['one'];\n"
            + "   anyProperty:Any[1] = 'anyString';\n"
            + "}\n";

    @Test
    public void testDefaultValue()
    {
        compileTestSource("defaultValueSource.pure", DECLARATION
                + "function testDefaultValue():Any[*]\n"
                + "{"
                + "   print(^test::long::path::A(),5);\n"
                + "}\n"
                + "function setAllValues():Any[*]\n"
                + "{"
                + "   print(^test::long::path::A(stringProperty='default', "
                + "classProperty=^my::exampleRootType(), "
                + "enumProperty = EnumWithDefault.DefaultValue,"
                + "floatProperty = 0.12,"
                + "inheritProperty = 0.12,"
                + "booleanProperty = false,"
                + "integerProperty = 0,"
                + "collectionProperty=['one', 'two'],"
                + "enumCollection=[EnumWithDefault.DefaultValue, EnumWithDefault.AnotherValue],"
                + "classCollection=[^my::exampleRootType(), ^my::exampleSubType()],"
                + "singleProperty=['one'],"
                + "anyProperty='anyString'),5);\n"
                + "}"

        );
        this.execute("setAllValues():Any[*]");
        String setAllValuesPrint = functionExecution.getConsole().getLine(0);
        this.execute("testDefaultValue():Any[*]");
        Assert.assertEquals(setAllValuesPrint, functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testDefaultValueOverridden()
    {
        compileTestSource("defaultValueSource.pure", "import test::*;\n"
                + "Class test::A\n"
                + "{\n"
                + "   stringProperty:String[1] = 'default';\n"
                + "   booleanProperty:Boolean[1] = false;\n"
                + "}\n"
                + "function testDefaultValueOverridden():Any[*]\n"
                + "{\n"
                + "   print(^test::A(stringProperty = 'override'),2);\n"
                + "}\n"
        );

        CoreInstance func = runtime.getFunction("testDefaultValueOverridden():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDefaultValueWithQualifiedProperty()
    {
        compileTestSource("defaultValueSource.pure", "import test::*;\n"
                + "Class test::A\n"
                + "{\n"
                + "   stringProperty:String[1] = 'default';\n"
                + "   prop(){$this.stringProperty + 'Value'}:String[1];\n"
                + "}\n"
                + "function test():Any[*]\n"
                + "{"
                + "   print(^test::A().prop(),2);\n"
                + "}\n"
        );
        this.execute("test():Any[*]");
        Assert.assertEquals("'defaultValue'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testDefaultValueWithDynamicNew()
    {
        compileTestSource("defaultValueSource.pure", DECLARATION
                + "function test():Any[*] \n{"
                + " let r = dynamicNew(A, \n"
                + "     [ ^KeyValue(key='stringProperty',value='dynamicNew')]);\n"
                + " print($r, 1);\n"
                + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDefaultValueWithKeyValuePassedAsVariableToDynamicNew()
    {
        compileTestSource("defaultValueSource.pure", DECLARATION
                + "function test():Any[*] \n{"
                + " let a = ^KeyValue(key='stringProperty',value='variable');"
                + " let b = ^KeyValue(key='enumProperty',value=EnumWithDefault.AnotherValue);"
                + " let r = dynamicNew(A, \n"
                + "     [ $a, $b ]);\n"
                + " print($r, 1);\n"
                + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
