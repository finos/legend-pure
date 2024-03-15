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

package org.finos.legend.pure.m3.tests.function.base.string;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestFormat extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testFormatTooFewInputs()
    {
        compileTestSource(
                "fromString.pure",
                "function test():Nil[0]\n" +
                        "{\n" +
                        "   print('Hello %s %s'->format(['Catherine']), 1);\n" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Nil[0]"));
        assertPureException(PureExecutionException.class, "Too few arguments passed to format function. Format expression \"Hello %s %s\", number of arguments [1]", e);
    }

    @Test
    public void testFormatTooManyInputs()
    {
        compileTestSource(
                "fromString.pure",
                "function test():Nil[0]\n" +
                        "{\n" +
                        "   print('Hello %s %s'->format(['Catherine', 'Joe', 'Katie']), 1);\n" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Nil[0]"));
        assertPureException(PureExecutionException.class, "Unused format args. [3] arguments provided to expression \"Hello %s %s\"", e);
    }

    @Test
    public void testFormatInEval()
    {
        compileTestSource(
                "fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    assertEq('the quick brown fox jumps over the lazy dog', format_String_1__Any_MANY__String_1_->eval('the quick brown %s jumps over the lazy %s', ['fox', 'dog']));\n" +
                        "}\n");
        CoreInstance result = execute("test():Boolean[1]");
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }

    @Test
    public void testFormatInEvaluate()
    {
        compileTestSource(
                "fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    assertEquals('the quick brown fox jumps over the lazy dog', format_String_1__Any_MANY__String_1_->evaluate([^List<String>(values='the quick brown %s jumps over the lazy %s'), ^List<Any>(values=['fox', 'dog'])]));\n" +
                        "}\n");
        CoreInstance result = execute("test():Boolean[1]");
        Assert.assertTrue(PrimitiveUtilities.getBooleanValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }
}
