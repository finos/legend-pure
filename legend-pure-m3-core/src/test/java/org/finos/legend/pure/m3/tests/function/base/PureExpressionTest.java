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

package org.finos.legend.pure.m3.tests.function.base;

import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Abstract base class for testing Pure expressions.
 */
public abstract class PureExpressionTest extends AbstractPureTestWithCoreCompiled
{
    public void assertExpressionTrue(String expression)
    {
        assertExpressionTrue(null, expression);
    }

    public void assertExpressionTrue(String message, String expression)
    {
        Assert.assertTrue(message, evaluateBooleanExpression(expression));
    }

    public void assertExpressionFalse(String expression)
    {
        assertExpressionFalse(null, expression);
    }

    public void assertExpressionFalse(String message, String expression)
    {
        Assert.assertFalse(message, evaluateBooleanExpression(expression));
    }

    public void assertExpressionRaisesPureException(String expectedInfo, String expression)
    {
        try
        {
            evaluateExpression(expression, "Any", true);
            Assert.fail("Expected exception evaluating: " + expression);
        }
        catch (Exception e)
        {
            assertPureException(expectedInfo, e);
        }
    }

    public void assertExpressionRaisesPureException(Pattern expectedInfo, String expression)
    {
        try
        {
            evaluateExpression(expression, "Any", true);
            Assert.fail("Expected exception evaluating: " + expression);
        }
        catch (Exception e)
        {
            assertPureException(expectedInfo, e);
        }
    }

    public void assertExpressionWithManyMultiplicityReturnRaisesPureException(String expectedInfo, int expectedLine, int expectedColumn, String expression)
    {
        try
        {
            evaluateExpression(expression, "Any", false);
            Assert.fail("Expected exception evaluating: " + expression);
        }
        catch (Exception e)
        {
            assertPureException(expectedInfo, expectedLine, expectedColumn, e);
        }
    }

    public void assertExpressionRaisesPureException(String expectedInfo, int expectedLine, int expectedColumn, String expression)
    {
        assertExpressionRaisesPureException(expectedInfo, expectedLine, expectedColumn, expression, null);
    }

    public void assertExpressionRaisesPureException(String expectedInfo, int expectedLine, int expectedColumn, String expression, String extraFunc)
    {
        try
        {
            evaluateExpression(expression, "Any", true, extraFunc);
            Assert.fail("Expected exception evaluating: " + expression);
        }
        catch (Exception e)
        {
            assertPureException(expectedInfo, expectedLine, expectedColumn, e);
        }
    }

    public void assertExpressionRaisesPureException(Pattern expectedInfo, int expectedLine, int expectedColumn, String expression)
    {
        try
        {
            evaluateExpression(expression, "Any", true);
            Assert.fail("Expected exception evaluating: " + expression);
        }
        catch (Exception e)
        {
            assertPureException(expectedInfo, expectedLine, expectedColumn, e);
        }
    }

    /**
     * Evaluate a Pure expression expecting a boolean value.
     *
     * @param expression boolean Pure expression
     * @return boolean value of expression
     */
    private boolean evaluateBooleanExpression(String expression)
    {
        CoreInstance value = (CoreInstance)evaluateExpression(expression, M3Paths.Boolean, true);
        String valueName = value.getName();

        if ("true".equals(valueName))
        {
            return true;
        }
        else if ("false".equals(valueName))
        {
            return false;
        }
        else
        {
            throw new RuntimeException("Expected boolean value, got: " + valueName);
        }
    }

    private Object evaluateExpression(String expression, String type, boolean isReturnMultiplicityOne)
    {
        return evaluateExpression(expression, type, isReturnMultiplicityOne, null);
    }

    /**
     * Evaluate a Pure expression and return the resulting value.
     *
     * @param expression Pure expression
     * @param type type of the expression
     * @param isReturnMultiplicityOne is return multiplicity one
     * @return value of the expression
     */
    private Object evaluateExpression(String expression, String type, boolean isReturnMultiplicityOne, String extraFunc)
    {
        if (type == null)
        {
            type = "Any";
        }

        String functionName = "test_" + UUID.randomUUID().toString().replace('-', '_');
        String testFunctionString = "function " + functionName + "():" + type + (isReturnMultiplicityOne? "[1]" : "[*]") + "\n" +
                "{\n" +
                ArrayIterate.makeString(expression.split("\\n|\\r|(\\n\\r)|(\\r\\n)"), "        ", "\n        ", "\n") +
                "}\n"+(extraFunc == null?"":extraFunc+"\n");
        compileTestSource(testFunctionString);
        if (isReturnMultiplicityOne)
        {
            return Instance.getValueForMetaPropertyToOneResolved(this.execute(functionName + "():" + type + "[1]"), M3Properties.values, this.processorSupport);
        }
        else
        {
            return Instance.getValueForMetaPropertyToManyResolved(this.execute(functionName + "():" + type + "[*]"), M3Properties.values, this.processorSupport);
        }
    }
}
