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

package org.finos.legend.pure.m3.tests.function.base.asserts;

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestAssert extends PureExpressionTest
{
    @Test
    public void testSuccessWithMessageString()
    {
        assertExpressionTrue("assert(true, 'Test message')");
        assertExpressionTrue("assert(2 == 2, 'Test message')");
        assertExpressionTrue("assert((1 + 2) == 3, 'Test message')");
    }

    @Test
    public void testSuccessWithMessageFunction()
    {
        assertExpressionTrue("assert(true, | 'Test message')");
        assertExpressionTrue("assert(2 == 2, | format('Test message %s', [true]))");
        assertExpressionTrue("assert((1 + 2) == 3, | format('Test message: %s', [true]))");
    }

    @Test
    public void testFailWithMessageFunction()
    {
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assert(false, |format('Test message: %d', 2 + 3))");
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assert(1 == 2, |format('Test message: %d', 2 + 3))");
    }

    @Test
    public void testEval()
    {
        assertExpressionTrue("assert_Boolean_1__Function_1__Boolean_1_->eval(true, {|'hello'})");
    }
}
