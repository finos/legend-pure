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

package org.finos.legend.pure.m3.tests.function.base.multiplicity;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestToMultiplicity extends PureExpressionTest
{

    @Test
    public void testErrorFromToOne()
    {
        assertExpressionRaisesPureException("Cannot cast a collection of size 0 to multiplicity [1]", 3, 13, "[]->toMultiplicity(@[1])");
        assertExpressionRaisesPureException("Cannot cast a collection of size 3 to multiplicity [1]", 3, 26, "['a', 'b', 'c']->toMultiplicity(@[1])");
    }

    @Test
    public void testErrorFromToOneMany()
    {
        assertExpressionRaisesPureException("Cannot cast a collection of size 0 to multiplicity [1..*]", 3, 13, "[]->toMultiplicity(@[1..*]); 1;");
    }

    @Test
    public void testMatchExact()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]" +
                        "{" +
                        "   x(|'1')" +
                        "}" +
                        "function x<|o>(f:Function<{->Any[o]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   assert([1] == [1]->toMultiplicity(@[o]));\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testBigger()
    {
        compileTestSource("fromString.pure",
                "Class Firm{}" +
                        "function test():Boolean[1]" +
                        "{" +
                        "   x(|Firm.all())" +
                        "}" +
                        "function x<|o>(f:Function<{->Any[o]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   assert([1] == [1]->toMultiplicity(@[o]));\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testError()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]" +
                        "{" +
                        "   x(|'1')" +
                        "}" +
                        "function x<|o>(f:Function<{->Any[o]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "   assert([1] == [1,2]->toMultiplicity(@[o]));\n" +
                        "}\n");

        try
        {
            this.execute("test():Boolean[1]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            assertException(e);
        }
    }

    protected void assertException(Exception e)
    {
        assertOriginatingPureException(PureExecutionException.class, "Cannot cast a collection of size 2 to multiplicity [1]", 3, 25, e);
    }
}
