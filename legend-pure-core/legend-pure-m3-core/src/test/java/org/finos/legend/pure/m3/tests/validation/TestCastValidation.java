// Copyright 2026 Goldman Sachs
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

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCastValidation extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_FUNCTION_HEADER = "import test::*;\n" +
            "function test::castTest<T>(n:Number[1], x:Any[1], t:T[1], d:Date[1], e:Enum[1]):Any[*]\n" +
            "{\n";
    private static final int EXPRESSION_LINE = 4;
    private static final String EXPRESSION_INDENT = "    ";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getExtra());
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(
                "castFixtures.pure",
                "Class test::CastA\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class test::CastC\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Enum test::CastColor\n" +
                        "{\n" +
                        "  RED, GREEN\n" +
                        "}\n" +
                        "\n" +
                        "Enum test::CastShape\n" +
                        "{\n" +
                        "  SQUARE\n" +
                        "}\n" +
                        "\n" +
                        "Primitive test::CastStringP extends String\n" +
                        "\n" +
                        "Primitive test::CastIntegerP extends Integer\n" +
                        "\n" +
                        "Measure test::CastLength\n" +
                        "{\n" +
                        "  *Metre: x -> $x;\n" +
                        "  Foot: x -> $x * 0.3048;\n" +
                        "}\n");
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPrimitiveToUnrelatedPrimitive()
    {
        assertCastRejected("'1'->cast(@Integer)", "String", "Integer");
        assertCastRejected("1->cast(@String)", "Integer", "String");
        assertCastRejected("1->cast(@Float)", "Integer", "Float");
        assertCastRejected("%2020-01-01->cast(@DateTime)", "StrictDate", "DateTime");
        assertCastRejected("['a', 'b']->cast(@Integer)", "String", "Integer");
    }

    @Test
    public void testPrimitiveToUnrelatedExtendedPrimitive()
    {
        assertCastRejected("10->cast(@CastStringP)", "Integer", "CastStringP");
    }

    @Test
    public void testEnumToUnrelatedType()
    {
        assertCastRejected("CastColor.RED->cast(@String)", "CastColor", "String");
        assertCastRejected("'RED'->cast(@CastColor)", "String", "CastColor");
        assertCastRejected("CastColor.RED->cast(@CastShape)", "CastColor", "CastShape");
    }

    @Test
    public void testClassToDataType()
    {
        assertCastRejected("^CastA()->cast(@String)", "CastA", "String");
        assertCastRejected("'a'->cast(@CastA)", "String", "CastA");
        assertCastRejected("^CastA()->cast(@CastColor)", "CastA", "CastColor");
        assertCastRejected("1->cast(@meta::pure::metamodel::relation::Relation<(a:Integer)>)", "Integer", "Relation<(a:Integer)>");
    }

    @Test
    public void testUnitToOtherUnit()
    {
        assertCastRejected("(5 test::CastLength~Metre)->cast(@test::CastLength~Foot)", "CastLength~Metre", "CastLength~Foot");
    }

    @Test
    public void testPossibleCastsCompile()
    {
        compileTestSource("fromString.pure", TEST_FUNCTION_HEADER +
                "    'a'->cast(@String);\n" +
                "    1->cast(@Number);\n" +
                "    $n->cast(@Integer);\n" +
                "    1->cast(@Any);\n" +
                "    $x->cast(@Integer);\n" +
                "    $x->cast(@CastColor);\n" +
                "    []->cast(@String);\n" +
                "    1->cast(@CastIntegerP);\n" +
                "    $d->cast(@StrictDate);\n" +
                "    CastColor.RED->cast(@Enum);\n" +
                "    $e->cast(@CastColor);\n" +
                "    ^CastA()->cast(@CastC);\n" +
                "    $t->cast(@String);\n" +
                "    [1, 'a']->cast(@String);\n" +
                "    (5 test::CastLength~Metre)->cast(@test::CastLength);\n" +
                "    fail('unreachable')->cast(@CastA);\n" +
                "}\n");
    }

    private void assertCastRejected(String expression, String sourceType, String targetType)
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure", TEST_FUNCTION_HEADER + EXPRESSION_INDENT + expression + ";\n}\n"));
        int column = EXPRESSION_INDENT.length() + expression.indexOf("cast(@") + 1;
        assertPureException(PureCompilationException.class, "Cast exception: " + sourceType + " cannot be cast to " + targetType, "fromString.pure", EXPRESSION_LINE, column, e);
        runtime.delete("fromString.pure");
        runtime.compile();
    }
}
