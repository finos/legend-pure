// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.function.relation;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;

public class TestTestTDS extends AbstractPureTestWithCoreCompiled
{

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.compile();
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    @org.junit.Test
    public void testSort()
    {
        String initialTDS = "id, name, otherOne\n" +
                "4, Simple, D\n" +
                "4, Simple, A\n" +
                "3, Ephrim, C\n" +
                "2, Bla, B\n" +
                "3, Ok, D\n" +
                "3, Nop, E\n" +
                "2, Neema, F\n" +
                "1, Pierre, F";
        TestTDS tds = new TestTDS(initialTDS, repository, processorSupport);

        TestTDS t = tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.ASC), new SortInfo("name", SortDirection.ASC)));
        Assert.assertEquals("id, name, otherOne\n" +
                "1, Pierre, F\n" +
                "2, Bla, B\n" +
                "2, Neema, F\n" +
                "3, Ephrim, C\n" +
                "3, Nop, E\n" +
                "3, Ok, D\n" +
                "4, Simple, D\n" +
                "4, Simple, A", t.toString());

        TestTDS t2 = tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.ASC), new SortInfo("name", SortDirection.ASC), new SortInfo("otherOne", SortDirection.ASC)));
        Assert.assertEquals("id, name, otherOne\n" +
                "1, Pierre, F\n" +
                "2, Bla, B\n" +
                "2, Neema, F\n" +
                "3, Ephrim, C\n" +
                "3, Nop, E\n" +
                "3, Ok, D\n" +
                "4, Simple, A\n" +
                "4, Simple, D", t2.toString());

        TestTDS t3 = tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.DESC), new SortInfo("name", SortDirection.ASC), new SortInfo("otherOne", SortDirection.DESC)));
        Assert.assertEquals("id, name, otherOne\n" +
                "4, Simple, D\n" +
                "4, Simple, A\n" +
                "3, Ephrim, C\n" +
                "3, Nop, E\n" +
                "3, Ok, D\n" +
                "2, Bla, B\n" +
                "2, Neema, F\n" +
                "1, Pierre, F", t3.toString());

        Assert.assertEquals(initialTDS, tds.toString());
    }

    @org.junit.Test
    public void testSortWithNull()
    {
        String resTDS = "id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2";

        String leftTDS = "id, name\n" +
                "1, George\n" +
                "3, Sachin\n" +
                "2, Pierre\n" +
                "4, David";

        TestTDS res = new TestTDS(resTDS, repository, processorSupport);
        TestTDS left = new TestTDS(leftTDS, repository, processorSupport);

        TestTDS t = left.compensateLeft(res).sort(new SortInfo("id", SortDirection.ASC));
        Assert.assertEquals("id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "1, 1, More George 2, George, 2\n" +
                "2, NULL, NULL, Pierre, NULL\n" +
                "3, NULL, NULL, Sachin, NULL\n" +
                "4, 4, More David, David, 1", t.toString());

        Assert.assertEquals(resTDS, res.toString());
        Assert.assertEquals(leftTDS, left.toString());
    }

    @org.junit.Test
    public void testSlice()
    {
        String initialTDS = "id, name, otherOne\n" +
                "4, Simple, D\n" +
                "4, Simple, A\n" +
                "3, Ephrim, C\n" +
                "2, Bla, B\n" +
                "3, Ok, D\n" +
                "3, Nop, E\n" +
                "2, Neema, F\n" +
                "1, Pierre, F";
        TestTDS tds = new TestTDS(initialTDS, repository, processorSupport);

        TestTDS t = tds.slice(1, 3);
        Assert.assertEquals("id, name, otherOne\n" +
                "4, Simple, A\n" +
                "3, Ephrim, C", t.toString());

        Assert.assertEquals(initialTDS, tds.toString());
    }

    @org.junit.Test
    public void testSliceWithNull()
    {
        String resTDS = "id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2";

        String leftTDS = "id, name\n" +
                "1, George\n" +
                "3, Sachin\n" +
                "2, Pierre\n" +
                "4, David";

        TestTDS res = new TestTDS(resTDS, repository, processorSupport);
        TestTDS left = new TestTDS(leftTDS, repository, processorSupport);

        TestTDS t = left.compensateLeft(res).slice(2, 4);
        Assert.assertEquals("id, id2, extra, name, extraInt\n" +
                "1, 1, More George 2, George, 2\n" +
                "2, NULL, NULL, Pierre, NULL", t.toString());

        Assert.assertEquals(resTDS, res.toString());
        Assert.assertEquals(leftTDS, left.toString());
    }


    @org.junit.Test
    public void testConcatenate()
    {
        String initialTDS1 = "id, name, otherOne\n" +
                "4, Simple, D\n" +
                "4, Simple, A\n" +
                "3, Ephrim, C\n" +
                "2, Bla, B\n" +
                "3, Ok, D\n" +
                "3, Nop, E\n" +
                "2, Neema, F\n" +
                "1, Pierre, F";

        String initialTDS2 = "id, name, otherOne\n" +
                "1, SimpleAA, D\n" +
                "3, SimpleEE, A\n" +
                "4, EphrimWW, C";

        TestTDS tds1 = new TestTDS(initialTDS1, repository, processorSupport);
        TestTDS tds2 = new TestTDS(initialTDS2, repository, processorSupport);

        TestTDS t = tds1.concatenate(tds2);
        Assert.assertEquals("id, name, otherOne\n" +
                "4, Simple, D\n" +
                "4, Simple, A\n" +
                "3, Ephrim, C\n" +
                "2, Bla, B\n" +
                "3, Ok, D\n" +
                "3, Nop, E\n" +
                "2, Neema, F\n" +
                "1, Pierre, F\n" +
                "1, SimpleAA, D\n" +
                "3, SimpleEE, A\n" +
                "4, EphrimWW, C", t.toString());

        Assert.assertEquals(initialTDS1, tds1.toString());
        Assert.assertEquals(initialTDS2, tds2.toString());
    }

    @org.junit.Test
    public void testConcatenateWithNull()
    {
        String resTDS = "id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2";

        String leftTDS = "id, name\n" +
                "1, George\n" +
                "3, Sachin\n" +
                "2, Pierre\n" +
                "4, David";

        TestTDS res = new TestTDS(resTDS, repository, processorSupport);
        TestTDS left = new TestTDS(leftTDS, repository, processorSupport);

        TestTDS t = res.concatenate(left.compensateLeft(res));

        Assert.assertEquals("id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2\n" +
                "2, NULL, NULL, Pierre, NULL\n" +
                "3, NULL, NULL, Sachin, NULL", t.toString());

        Assert.assertEquals(resTDS, res.toString());
        Assert.assertEquals(leftTDS, left.toString());
    }

    @org.junit.Test
    public void testJoin()
    {
        String initialTds1 = "id, name\n" +
                "1, A\n" +
                "2, B\n" +
                "3, C";

        String initialTds2 = "extra\n" +
                "X\n" +
                "Y\n" +
                "Z";

        TestTDS tds1 = new TestTDS(initialTds1, repository, processorSupport);
        TestTDS tds2 = new TestTDS(initialTds2, repository, processorSupport);

        TestTDS t = tds1.join(tds2);

        Assert.assertEquals("id, extra, name\n" +
                "1, X, A\n" +
                "1, Y, A\n" +
                "1, Z, A\n" +
                "2, X, B\n" +
                "2, Y, B\n" +
                "2, Z, B\n" +
                "3, X, C\n" +
                "3, Y, C\n" +
                "3, Z, C", t.toString());

        Assert.assertEquals(initialTds1, tds1.toString());
        Assert.assertEquals(initialTds2, tds2.toString());
    }

    @org.junit.Test
    public void testJoinWithNull()
    {
        String initialRes = "id, id2, extra, name, extraInt\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2";

        String initialLeft = "id, name\n" +
                "1, George\n" +
                "3, Sachin";

        String initialThird = "id, boom\n" +
                "1, A1\n" +
                "3, A3";

        TestTDS res = new TestTDS(initialRes, repository, processorSupport);
        TestTDS left = new TestTDS(initialLeft, repository, processorSupport);
        TestTDS third = new TestTDS(initialThird, repository, processorSupport);

        TestTDS t = left.compensateLeft(res).join(third);

        Assert.assertEquals("id, id2, extra, name, boom, extraInt\n" +
                "1, 4, More David, David, A1, 1\n" +
                "3, 4, More David, David, A3, 1\n" +
                "1, 1, More George 2, George, A1, 2\n" +
                "3, 1, More George 2, George, A3, 2\n" +
                "1, NULL, NULL, Sachin, A1, NULL\n" +
                "3, NULL, NULL, Sachin, A3, NULL", t.toString());

        Assert.assertEquals(initialRes, res.toString());
        Assert.assertEquals(initialLeft, left.toString());
        Assert.assertEquals(initialThird, third.toString());
    }

    @org.junit.Test
    public void testCompensateLeft()
    {
        String resTDS = "id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2";

        String leftTDS = "id, name\n" +
                "1, George\n" +
                "3, Sachin\n" +
                "2, Pierre\n" +
                "4, David";

        TestTDS res = new TestTDS(resTDS, repository, processorSupport);
        TestTDS left = new TestTDS(leftTDS, repository, processorSupport);
        TestTDS t = left.compensateLeft(res);

        Assert.assertEquals("id, id2, extra, name, extraInt\n" +
                "1, 1, More George 1, George, 1\n" +
                "4, 4, More David, David, 1\n" +
                "1, 1, More George 2, George, 2\n" +
                "2, NULL, NULL, Pierre, NULL\n" +
                "3, NULL, NULL, Sachin, NULL", t.toString());

        Assert.assertEquals(resTDS, res.toString());
        Assert.assertEquals(leftTDS, left.toString());
    }
}