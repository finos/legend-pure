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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestFormat extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testFormatTooFewInputs()
    {
        try
        {
           compileTestSource("fromString.pure",
                    "function test():Nil[0]\n" +
                            "{\n" +
                            "   print('Hello %s %s'->format(['Catherine']), 1);\n" +
                            "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureExecutionException.class, "Too few arguments passed to format function. Format expression \"Hello %s %s\", number of arguments [1]", e);
        }
    }

    @Test
    public void testFormatTooManyInputs()
    {
        try
        {
           compileTestSource("fromString.pure",
                    "function test():Nil[0]\n" +
                            "{\n" +
                            "   print('Hello %s %s'->format(['Catherine', 'Joe', 'Katie']), 1);\n" +
                            "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureExecutionException.class, "Unused format args. [3] arguments provided to expression \"Hello %s %s\"", e);
        }
    }
}
