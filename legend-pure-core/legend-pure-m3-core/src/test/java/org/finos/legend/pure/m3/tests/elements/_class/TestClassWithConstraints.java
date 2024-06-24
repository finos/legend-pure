// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements._class;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClassWithConstraints extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_SOURCE_ID = "fromString.pure";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testSimpleConstraints()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "Class TestClassWithConstraints\n" +
                        "[\n" +
                        "  $this.a != $this.b,\n" +
                        "  aA: $this.a->startsWith('A')\n" +
                        "]\n" +
                        "{\n" +
                        "  a: String[1];\n" +
                        "  b: String[1];\n" +
                        "}\n");

        Class<?> testClass = getInstance("TestClassWithConstraints");
        ListIterable<? extends Constraint> constraints = ListHelper.wrapListIterable(testClass._constraints());
        Constraint const0 = constraints.get(0);
        Assert.assertEquals("0", const0._name());
        Assert.assertNull(const0._owner());
        Assert.assertNull(const0._externalId());
        Assert.assertNull(const0._enforcementLevel());

        Constraint aA = constraints.get(1);
        Assert.assertEquals("aA", aA._name());
        Assert.assertNull(aA._owner());
        Assert.assertNull(aA._externalId());
        Assert.assertNull(aA._enforcementLevel());

        Assert.assertEquals(2, constraints.size());
    }

    @Test
    public void testConstraintWithEscapedQuote()
    {
        compileTestSource(
                TEST_SOURCE_ID,
                "Class TestClassWithConstraints\n" +
                        "[\n" +
                        "  aA: $this.a->startsWith('A'),\n" +
                        "  bNotB( ~owner: Bee ~externalId: 'Bee\\'s b not B' ~function: !$this.b->startsWith('B') ~enforcementLevel: Warn ~message: 'Look out for the B\\'s!'),\n" +
                        "  $this.a != $this.b\n" +
                        "]\n" +
                        "{\n" +
                        "  a: String[1];\n" +
                        "  b: String[1];\n" +
                        "}\n");

        Class<?> testClass = getInstance("TestClassWithConstraints");
        ListIterable<? extends Constraint> constraints = ListHelper.wrapListIterable(testClass._constraints());
        Constraint aA = constraints.get(0);
        Assert.assertEquals("aA", aA._name());
        Assert.assertNull(aA._owner());
        Assert.assertNull(aA._externalId());
        Assert.assertNull(aA._enforcementLevel());

        Constraint bNotB = constraints.get(1);
        Assert.assertEquals("bNotB", bNotB._name());
        Assert.assertEquals("Bee", bNotB._owner());
        Assert.assertEquals("Bee's b not B", bNotB._externalId());
        Assert.assertEquals("Warn", bNotB._enforcementLevel());

        Constraint const2 = constraints.get(2);
        Assert.assertEquals("2", const2._name());
        Assert.assertNull(const2._owner());
        Assert.assertNull(const2._externalId());
        Assert.assertNull(const2._enforcementLevel());

        Assert.assertEquals(3, constraints.size());
    }

    @Test
    public void testNameConflict()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                TEST_SOURCE_ID,
                "Class TestClassWithConstraints\n" +
                        "[\n" +
                        "  aA: $this.a->startsWith('A'),\n" +
                        "  $this.a->length() >= $this.b->length(),\n" +
                        "  aA: $this.a != $this.b\n" +
                        "]\n" +
                        "{\n" +
                        "  a: String[1];\n" +
                        "  b: String[1];\n" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Constraints for TestClassWithConstraints must be unique, [aA] is duplicated", TEST_SOURCE_ID, 5, 3, 5, 3, 5, 24, e);
    }

    @SuppressWarnings("unchecked")
    private <T extends CoreInstance> T getInstance(String path)
    {
        T instance = (T) runtime.getCoreInstance(path);
        Assert.assertNotNull(path, instance);
        return instance;
    }
}
