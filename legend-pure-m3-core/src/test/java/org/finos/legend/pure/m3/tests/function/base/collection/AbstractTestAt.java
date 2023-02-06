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

package org.finos.legend.pure.m3.tests.function.base.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestAt extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testAtError()
    {
        try
        {
            compileTestSource("Class Firm{employees:Employee[*];}\n" +
                    "Class Employee{lastName:String[1];}\n" +
                    "function testAt():Nil[0]\n" +
                    "{\n" +
                    "   let set = ^Firm(employees=[^Employee(lastName='a'),^Employee(lastName='b')]);\n" +
                    "   print($set.employees->at(2), 1);\n" +
                    "}\n");
            this.execute("testAt():Nil[0]");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertException(e);
        }
    }

    protected void assertException(Exception e)
    {
        assertOriginatingPureException(PureExecutionException.class, "The system is trying to get an element at offset 2 where the collection is of size 2", 6, 26, e);
    }
}
