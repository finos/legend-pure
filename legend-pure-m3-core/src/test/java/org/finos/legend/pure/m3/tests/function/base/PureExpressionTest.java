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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.After;
import org.junit.Assert;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Abstract base class for testing Pure expressions.
 */
public abstract class PureExpressionTest extends AbstractPureTestWithCoreCompiled
{
    private final MutableList<String> sourcesToDelete = Lists.mutable.empty();

    @After
    public void deleteTestSources()
    {
        this.sourcesToDelete.forEach(runtime::delete);
        runtime.compile();
    }

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
        PureException e = Assert.assertThrows(PureException.class, () -> evaluateExpression(expression, "Any", true));
        assertPureException(expectedInfo, e);
    }

    public void assertExpressionRaisesPureException(Pattern expectedInfo, String expression)
    {
        PureException e = Assert.assertThrows(PureException.class, () -> evaluateExpression(expression, "Any", true));
        assertPureException(expectedInfo, e);
    }

    public void assertExpressionWithManyMultiplicityReturnRaisesPureException(String expectedInfo, int expectedLine, int expectedColumn, String expression)
    {
        PureException e = Assert.assertThrows(PureException.class, () -> evaluateExpression(expression, "Any", false));
        assertPureException(expectedInfo, expectedLine, expectedColumn, e);
    }

    public void assertExpressionRaisesPureException(String expectedInfo, int expectedLine, int expectedColumn, String expression)
    {
        PureException e = Assert.assertThrows(PureException.class, () -> evaluateExpression(expression, "Any", true));
        assertPureException(expectedInfo, expectedLine, expectedColumn, e);
    }

    public void assertExpressionRaisesPureException(Pattern expectedInfo, int expectedLine, int expectedColumn, String expression)
    {
        PureException e = Assert.assertThrows(PureException.class, () -> evaluateExpression(expression, "Any", true));
        assertPureException(expectedInfo, expectedLine, expectedColumn, e);
    }

    /**
     * Evaluate a Pure expression expecting a boolean value.
     *
     * @param expression boolean Pure expression
     * @return boolean value of expression
     */
    private boolean evaluateBooleanExpression(String expression)
    {
        CoreInstance value = (CoreInstance) evaluateExpression(expression, M3Paths.Boolean, true);
        if (value instanceof BooleanCoreInstance)
        {
            return ((BooleanCoreInstance) value).getValue();
        }

        String valueName = value.getName();
        if (ModelRepository.BOOLEAN_TRUE.equals(valueName))
        {
            return true;
        }
        if (ModelRepository.BOOLEAN_FALSE.equals(valueName))
        {
            return false;
        }

        throw new RuntimeException("Expected boolean value, got: " + valueName);
    }

    /**
     * Evaluate a Pure expression and return the resulting value.
     *
     * @param expression              Pure expression
     * @param type                    type of the expression
     * @param isReturnMultiplicityOne is return multiplicity one
     * @return value of the expression
     */
    private Object evaluateExpression(String expression, String type, boolean isReturnMultiplicityOne)
    {
        String functionName = "test_" + UUID.randomUUID().toString().replace('-', '_');
        String functionSignature = functionName + "():" + ((type == null) ? "Any" : type) + (isReturnMultiplicityOne ? "[1]" : "[*]");
        String testFunctionString = "function " + functionSignature + "\n" +
                "{\n" +
                ArrayIterate.makeString(expression.split("\\R"), "        ", "\n        ", "\n") +
                "}\n";
        String sourceName = functionName + CodeStorage.PURE_FILE_EXTENSION;
        this.sourcesToDelete.add(sourceName);
        compileTestSource(sourceName, testFunctionString);
        CoreInstance result = execute(functionSignature);
        return isReturnMultiplicityOne ?
                Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, processorSupport) :
                Instance.getValueForMetaPropertyToManyResolved(result, M3Properties.values, processorSupport);
    }
}
