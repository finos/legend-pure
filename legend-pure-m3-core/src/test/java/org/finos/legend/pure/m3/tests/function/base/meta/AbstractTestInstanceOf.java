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

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestInstanceOf extends PureExpressionTest
{
    @Test
    public void testInstanceOfEnumeration()
    {
        compileTestSource("Enum test::Enum1 { VALUE1, VALUE2 }\n" +
                "Enum test::Enum2 { VALUE3, VALUE4 }\n" +
                "Class test::MyClass {}\n");

        // Enum
        assertExpressionTrue("test::Enum1.VALUE1->instanceOf(test::Enum1)");
        assertExpressionFalse("test::Enum1.VALUE1->instanceOf(test::Enum2)");
        assertExpressionTrue("test::Enum1.VALUE2->instanceOf(test::Enum1)");
        assertExpressionFalse("test::Enum1.VALUE2->instanceOf(test::Enum2)");
        assertExpressionFalse("test::Enum2.VALUE3->instanceOf(test::Enum1)");
        assertExpressionTrue("test::Enum2.VALUE3->instanceOf(test::Enum2)");
        assertExpressionFalse("test::Enum2.VALUE4->instanceOf(test::Enum1)");
        assertExpressionTrue("test::Enum2.VALUE4->instanceOf(test::Enum2)");

        // Primitive
        assertExpressionFalse("'a string'->instanceOf(test::Enum1)");
        assertExpressionFalse("7->instanceOf(test::Enum2)");
        assertExpressionFalse("3.14->instanceOf(test::Enum1)");
        assertExpressionFalse("false->instanceOf(test::Enum2)");
        assertExpressionFalse("%2018-01-17->instanceOf(test::Enum2)");

        // Class instance
        assertExpressionFalse("^test::MyClass()->instanceOf(test::Enum1)");
        assertExpressionFalse("^test::MyClass()->instanceOf(test::Enum2)");
    }

    @Test
    public void testIndirectInstanceOfEnumeration()
    {
        compileTestSource("Enum test::Enum1 { VALUE1, VALUE2 }\n" +
                "Enum test::Enum2 { VALUE3, VALUE4 }\n" +
                "Class test::MyClass {}\n" +
                "function test::indirectInstanceOf(val:Any[1], type:Type[1]):Boolean[1]\n" +
                "{\n" +
                "    $val->instanceOf($type);\n" +
                "}\n");

        // Enum
        assertExpressionTrue("test::Enum1.VALUE1->test::indirectInstanceOf(test::Enum1)");
        assertExpressionFalse("test::Enum1.VALUE1->test::indirectInstanceOf(test::Enum2)");
        assertExpressionTrue("test::Enum1.VALUE2->test::indirectInstanceOf(test::Enum1)");
        assertExpressionFalse("test::Enum1.VALUE2->test::indirectInstanceOf(test::Enum2)");
        assertExpressionFalse("test::Enum2.VALUE3->test::indirectInstanceOf(test::Enum1)");
        assertExpressionTrue("test::Enum2.VALUE3->test::indirectInstanceOf(test::Enum2)");
        assertExpressionFalse("test::Enum2.VALUE4->test::indirectInstanceOf(test::Enum1)");
        assertExpressionTrue("test::Enum2.VALUE4->test::indirectInstanceOf(test::Enum2)");

        // Primitive
        assertExpressionFalse("'a string'->test::indirectInstanceOf(test::Enum1)");
        assertExpressionFalse("7->test::indirectInstanceOf(test::Enum2)");
        assertExpressionFalse("3.14->test::indirectInstanceOf(test::Enum1)");
        assertExpressionFalse("false->test::indirectInstanceOf(test::Enum2)");
        assertExpressionFalse("%2018-01-17->test::indirectInstanceOf(test::Enum2)");

        // Class instance
        assertExpressionFalse("^test::MyClass()->test::indirectInstanceOf(test::Enum1)");
        assertExpressionFalse("^test::MyClass()->test::indirectInstanceOf(test::Enum2)");
    }
}
