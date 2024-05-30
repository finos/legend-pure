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
import org.junit.Test;

public abstract class AbstractTestDefaultValue extends AbstractPureTestWithCoreCompiledPlatform
{
    public static final String DECLARATION = "import test::long::path::*;\n"
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
                + "function check():Boolean[1]\n"
                + "{" +
                "     let d = ^test::long::path::A();" +
                "     let s = setAllValues();"
                + "   assertEquals($s.stringProperty,$d.stringProperty);\n"
                + "   assertEquals($s.classProperty->size(),$d.classProperty->size());\n"
                + "   assertEquals($s.enumProperty,$d.enumProperty);\n"
                + "   assertEquals($s.floatProperty,$d.floatProperty);\n"
                + "   assertEquals($s.inheritProperty,$d.inheritProperty);\n"
                + "   assertEquals($s.booleanProperty,$d.booleanProperty);\n"
                + "   assertEquals($s.integerProperty,$d.integerProperty);\n"
                + "   assertEquals($s.collectionProperty,$d.collectionProperty);\n"
                + "   assertEquals($s.enumCollection,$d.enumCollection);\n"
                + "   assertEquals($s.singleProperty,$d.singleProperty);\n"
                + "   assertEquals($s.anyProperty,$d.anyProperty);\n"
                + "   assertEquals($s.classCollection->size(),$d.classCollection->size());\n"
                + "}\n"
                + "function setAllValues():A[1]\n"
                + "{"
                + " ^test::long::path::A("
                + "     stringProperty='default', "
                + "     classProperty=^my::exampleRootType(), "
                + "     enumProperty = EnumWithDefault.DefaultValue,"
                + "     floatProperty = 0.12,"
                + "     inheritProperty = 0.12,"
                + "     booleanProperty = false,"
                + "     integerProperty = 0,"
                + "     collectionProperty=['one', 'two'],"
                + "     enumCollection=[EnumWithDefault.DefaultValue, EnumWithDefault.AnotherValue],"
                + "     classCollection=[^my::exampleRootType(), ^my::exampleSubType()],"
                + "     singleProperty=['one'],"
                + "     anyProperty='anyString'" +
                "   )" +
                ";\n"
                + "}"

        );
        this.execute("check():Boolean[1]");
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
                + "   assertEquals('override', ^test::A(stringProperty = 'override').stringProperty);\n"
                + "}\n"
        );
        execute("testDefaultValueOverridden():Any[*]");
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
                + "function test():Boolean[1]\n"
                + "{"
                + "   assertEquals('defaultValue', ^test::A().prop());\n"
                + "}\n"
        );
        execute("test():Boolean[1]");
     }
}
