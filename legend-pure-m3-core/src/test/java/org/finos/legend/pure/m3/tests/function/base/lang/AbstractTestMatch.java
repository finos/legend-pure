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

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

import java.util.regex.Pattern;

public abstract class AbstractTestMatch extends PureExpressionTest
{
    @Test
    public void testMatchIntegerFail()
    {
        Pattern expectedInfo = Pattern.compile("^Match failure: 42\\(?\\d*\\)? instanceOf Integer$");
        assertExpressionRaisesPureException(expectedInfo, "42 ->match([i:Float[1] | $i])");
    }

    @Test
    public void testMatchPrimitiveTypeFail()
    {
        Pattern expectedInfo = Pattern.compile("^Match failure: 42.01\\(?\\d*\\)? instanceOf Float$");
        assertExpressionRaisesPureException(expectedInfo, "42.01 ->match([i:Integer[1] | $i])");
    }

    @Test
    public void testMatchEnumTypeFail()
    {
        Pattern expectedInfo = Pattern.compile("^Match failure: January\\(?\\d*\\)? instanceOf Month$");
        assertExpressionRaisesPureException(expectedInfo, "Month.January ->match([i:Integer[1] | $i])");
    }

    @Test
    public void testMatchIndividualFail()
    {
        Pattern expectedInfo = Pattern.compile("^Match failure: test string\\(?\\d*\\)? instanceOf String$");
        assertExpressionRaisesPureException(expectedInfo, 3, 24, "'test string'->match([i:Integer[1] | $i])");
    }

    @Test
    public void testMatchCollectionFail()
    {
        Pattern expectedInfo = Pattern.compile("^Match failure: \\[test string\\(?\\d*\\)? instanceOf String, test string 2\\(?\\d*\\)? instanceOf String\\]$");
        assertExpressionRaisesPureException(expectedInfo, 3, 43, "['test string', 'test string 2']->match([i:Integer[1] | $i])");
    }

    @Test
    public void testMatchEnum()
    {
        compileTestSource("Enum test::Enum1 { VALUE1, VALUE2 }\n" +
                "Enum test::Enum2 { VALUE3, VALUE4 }\n" +
                "Enum test::Enum3 { VALUE5 }\n");
        assertExpressionTrue("test::Enum1.VALUE1->match([e:test::Enum1[1] | true, e:test::Enum2[1] | false])");
        assertExpressionTrue("test::Enum1.VALUE2->match([e:test::Enum1[1] | true, e:test::Enum2[1] | false])");
        assertExpressionFalse("test::Enum2.VALUE3->match([e:test::Enum1[1] | true, e:test::Enum2[1] | false])");
        assertExpressionFalse("test::Enum2.VALUE4->match([e:test::Enum1[1] | true, e:test::Enum2[1] | false])");

        Pattern expectedInfo = Pattern.compile("^Match failure: VALUE5(\\(\\d+\\))? instanceOf Enum3$");
        assertExpressionRaisesPureException(expectedInfo, 3, 29, "test::Enum3.VALUE5->match([e:test::Enum1[1] | true, e:test::Enum2[1] | false])");
    }
}
