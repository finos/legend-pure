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

package org.finos.legend.pure.m3.test.interpreted.integration.runner;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.AssertFailTestStatus;
import org.finos.legend.pure.m3.execution.test.ErrorTestStatus;
import org.finos.legend.pure.m3.execution.test.SuccessTestStatus;
import org.finos.legend.pure.m3.execution.test.TestCallBack;
import org.finos.legend.pure.m3.execution.test.TestRunner;
import org.finos.legend.pure.m3.execution.test.TestStatus;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTestRunner extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    private final Function<CoreInstance, String> getPath = new Function<CoreInstance, String>()
    {
        @Override
        public String valueOf(CoreInstance instance)
        {
            return PackageableElement.getUserPathForPackageableElement(instance, "::");
        }
    };

    private final Function<CallBackGroup, String> getCallBackGroupPath = new Function<CallBackGroup, String>()
    {
        @Override
        public String valueOf(CallBackGroup group)
        {
            return getPath.valueOf(group.getFunction());
        }
    };

    @Test
    public void testRun() throws Exception
    {
        compileTestSource("fromString.pure","function <<test.Test>> a::b::test():Boolean[1]\n" +
                        "{\n" +
                        "   print('1', 1);\n"+
                        "   assert(true, |'');\n" +
                        "}\n" +
                        "\n" +
                        "function <<test.Test>> a::b::c::test():Boolean[1]\n" +
                        "{\n" +
                        "   print('2', 1);\n"+
                        "   assert(false, |'');\n" +
                        "}\n" +
                        "function <<test.Test>> a::b::d::test():Nil[0]\n" +
                        "{\n" +
                        "   print('3', 1);\n" +
                        "   print([1, 2, 3, 4]->at(5), 1);\n" +
                        "}\n" +
                        "function ok():Nil[0]\n" +
                        "{\n" +
                        "   []\n" +
                        "}");
        UnitTestTestCallBack callback = new UnitTestTestCallBack();
        TestRunner testRunner = new TestRunner("::",  this.runtime, this.functionExecution, callback, false);
        testRunner.run();

        Assert.assertEquals(Sets.immutable.with("a::b::c::test__Boolean_1_", "a::b::d::test__Nil_0_", "a::b::test__Boolean_1_"), callback.getTests().collect(this.getPath).select(c->!c.startsWith("meta")));

        MutableList<CallBackGroup> groups = callback.getGroups().sortThisBy(this.getCallBackGroupPath);

        Assert.assertEquals("a::b::c::test__Boolean_1_", PackageableElement.getUserPathForPackageableElement(groups.get(0).getFunction(), "::"));
        Assert.assertTrue(groups.get(0).getMessage().startsWith("2"));
        Verify.assertInstanceOf(AssertFailTestStatus.class, groups.get(0).getStatus());

        Assert.assertEquals("a::b::d::test__Nil_0_", PackageableElement.getUserPathForPackageableElement(groups.get(1).getFunction(), "::"));
        Assert.assertTrue(groups.get(1).getMessage().startsWith("3"));
        Verify.assertInstanceOf(ErrorTestStatus.class, groups.get(1).getStatus());

        Assert.assertEquals("a::b::test__Boolean_1_", PackageableElement.getUserPathForPackageableElement(groups.get(2).getFunction(), "::"));
        Assert.assertTrue(groups.get(2).getMessage().startsWith("1"));
        Verify.assertInstanceOf(SuccessTestStatus.class, groups.get(2).getStatus());
    }

    @Test
    public void testSetup() throws Exception
    {
        compileTestSource("function <<test.BeforePackage>> a::b::setUp():Nil[0]\n" +
                        "{\n" +
                        "   print('setup AB', 1);\n" +
                        "}\n" +
                        "function <<test.BeforePackage>> a::b::c::setUp():Nil[0]\n" +
                        "{\n" +
                        "   print('setup ABC', 1);\n" +
                        "}\n" +
                        "function <<test.Test>> a::b::test():Boolean[1]\n" +
                        "{\n" +
                        "   print('1', 1);\n"+
                        "   assert(true, |'');\n" +
                        "}\n" +
                        "\n" +
                        "function <<test.Test>> a::b::c::test():Boolean[1]\n" +
                        "{\n" +
                        "   print('2', 1);\n"+
                        "   assert(false, |'');\n" +
                        "}\n" +
                        "function <<test.Test>> a::b::d::test():Nil[0]\n" +
                        "{\n" +
                        "   print('3', 1);\n" +
                        "   print([1, 2, 3, 4]->at(5), 1);\n" +
                        "}\n" +
                        "function ok():Nil[0]\n" +
                        "{\n" +
                        "   []\n" +
                        "}");
        UnitTestTestCallBack callback = new UnitTestTestCallBack();
        TestRunner testRunner = new TestRunner("a::b::c", this.runtime, this.functionExecution, callback, false);
        testRunner.run();

        Assert.assertEquals(Sets.immutable.with("a::b::c::test__Boolean_1_"), callback.getTests().collect(this.getPath));

        MutableList<CallBackGroup> groups = callback.getGroups().sortThisBy(this.getCallBackGroupPath);

        Assert.assertEquals("a::b::c::test__Boolean_1_", PackageableElement.getUserPathForPackageableElement(groups.get(0).getFunction(), "::"));
        Assert.assertTrue(groups.get(0).getMessage().startsWith("2"));
        Verify.assertInstanceOf(AssertFailTestStatus.class, groups.get(0).getStatus());

        Assert.assertEquals("'setup AB'", this.functionExecution.getConsole().getLine(5));
        Assert.assertEquals("'setup ABC'", this.functionExecution.getConsole().getLine(6));
    }

    @Test
    public void testExclusion()
    {
        compileTestSource("fromString.pure","function <<test.Test>> a::b::test():Boolean[1]\n" +
                "{\n" +
                "   print('1', 1);\n"+
                "   assert(true, |'');\n" +
                "}\n" +
                "\n" +
                "function <<test.Test>> {test.excludePlatform = 'Interpreted'} a::b::c::test():Boolean[1]\n" +
                "{\n" +
                "   print('2', 1);\n"+
                "   assert(false, |'');\n" +
                "}\n" +
                "function <<test.Test>> a::b::d::test():Nil[0]\n" +
                "{\n" +
                "   print('3', 1);\n" +
                "   print([1, 2, 3, 4]->at(5), 1);\n" +
                "}\n" +
                "function ok():Nil[0]\n" +
                "{\n" +
                "   []\n" +
                "}");
        UnitTestTestCallBack callback = new UnitTestTestCallBack();
        TestRunner testRunner = new TestRunner("::",  this.runtime, this.functionExecution, callback, false);
        testRunner.run();

        Assert.assertEquals(Sets.immutable.with("a::b::d::test__Nil_0_", "a::b::test__Boolean_1_"), callback.getTests().collect(this.getPath).select(c->!c.startsWith("meta")));

        MutableList<CallBackGroup> groups = callback.getGroups().sortThisBy(this.getCallBackGroupPath);

        CallBackGroup group = groups.get(0);
        Assert.assertEquals("a::b::d::test__Nil_0_", PackageableElement.getUserPathForPackageableElement(group.getFunction(), "::"));
        Assert.assertTrue(group.getMessage().startsWith("3"));
        Verify.assertInstanceOf(ErrorTestStatus.class, group.getStatus());

        group = groups.get(1);
        Assert.assertEquals("a::b::test__Boolean_1_", PackageableElement.getUserPathForPackageableElement(group.getFunction(), "::"));
        Assert.assertTrue(group.getMessage().startsWith("1"));
        Verify.assertInstanceOf(SuccessTestStatus.class, group.getStatus());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    private static class UnitTestTestCallBack implements TestCallBack
    {
        private MutableSet<CoreInstance> tests;
        private final MutableList<CallBackGroup> groups = FastList.newList();

        @Override
        public void foundTests(Iterable<? extends CoreInstance> tests)
        {
            this.tests = Sets.mutable.withAll(tests);
        }

        @Override
        public void executedTest(CoreInstance function, String testParameterizationId, String console, TestStatus status)
        {
            this.groups.add(new CallBackGroup(function, testParameterizationId, console, status));
        }

        MutableList<CallBackGroup> getGroups()
        {
            return this.groups;
        }

        MutableSet<CoreInstance> getTests()
        {
            return this.tests;
        }
    }

    private static class CallBackGroup
    {
        private final CoreInstance function;
        private final String testParameterizationId;
        private final String message;
        private final TestStatus status;

        private CallBackGroup(CoreInstance function, String testParameterizationId, String message, TestStatus status)
        {
            this.function = function;
            this.testParameterizationId = testParameterizationId;
            this.message = message;
            this.status = status;
        }

        public CoreInstance getFunction()
        {
            return this.function;
        }

        String getMessage()
        {
            return this.message;
        }

        TestStatus getStatus()
        {
            return this.status;
        }

        String getTestParameterizationId()
        {
            return this.testParameterizationId;
        }
    }
}
