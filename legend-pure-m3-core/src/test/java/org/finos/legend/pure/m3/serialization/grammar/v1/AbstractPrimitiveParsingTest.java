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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;

import java.util.UUID;

public abstract class AbstractPrimitiveParsingTest extends AbstractPureTestWithCoreCompiledPlatform
{
    protected abstract String getPrimitiveTypeName();

    protected void assertParsesTo(String expectedName, String string)
    {
        CoreInstance value = parsePrimitiveValue(string);
        Assert.assertNotNull(value);
        Assert.assertNotNull(value.getClassifier());
        Assert.assertEquals(getPrimitiveTypeName(), value.getClassifier().getName());
        Assert.assertEquals(expectedName, value.getName());
    }

    protected void assertFailsToParse(String string)
    {
        assertFailsToParse(null, string);
    }

    protected void assertFailsToParse(String expectedInfo, String string)
    {
        try
        {
            parsePrimitiveValue(string);
            Assert.fail("Expected exception parsing: \"" + string + "\"");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, expectedInfo, null, 3, null, 3, null, 3, null, e);
            SourceInformation sourceInfo = PureException.findPureException(e).getSourceInformation();
            int start = 5;
            int end = string.length() + 5;
            if (sourceInfo.getStartColumn() < start)
            {
                Assert.fail("Expected start column to be at least " + start + ", got: " + sourceInfo.getStartColumn());
            }
            if (sourceInfo.getColumn() < start)
            {
                Assert.fail("Expected column to be at least " + start + ", got: " + sourceInfo.getStartColumn());
            }
            // TODO figure out why this is sometimes violated
            if (sourceInfo.getEndColumn() > end)
            {
                System.err.println("Expected end column to be no greater than " + end + ", got: " + sourceInfo.getEndColumn());
                e.printStackTrace();
            }
        }
    }

    private CoreInstance parsePrimitiveValue(String string)
    {
        String typeString = getPrimitiveTypeName();
        String functionName = "test" + UUID.randomUUID().toString().replace("-", "");
        String functionSignature = functionName + "():" + typeString + "[1]";
        String testFunctionString = "function " + functionSignature + "\n" +
                "{\n" +
                "    " + string + "\n" +
                "}\n";
        compileTestSource(testFunctionString);
        CoreInstance function = this.runtime.getFunction(functionSignature);
        Assert.assertNotNull(function);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(function, M3Properties.expressionSequence, this.processorSupport);
        Assert.assertEquals(1, expressions.size());
        return Instance.getValueForMetaPropertyToOneResolved(expressions.getFirst(), M3Properties.values, this.processorSupport);
    }
}
