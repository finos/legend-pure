package org.finos.legend.pure.m3.navigation;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFunctionNavigation extends AbstractPureTestWithCoreCompiledPlatform
{

    @Before
    public void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void tearDown()
    {
        tearDownRuntime();
    }

    @Test
    public void testPrettyPrintFunction()
    {
        runtime.createInMemorySource(
                "test.pure",
                "function test1::A<T,V|m>(value: T[m]): V[*] \n" +
                        "{\n" +
                        "  print('ok', 1);\n" +
                        "}\n" +
                        "Class test::A {}\n" +
                        "\n" +
                        "function test2::doSomething(param1: String[1], param2: test::A[1]): Any[*]\n" +
                        "{\n" +
                        "  ''\n" +
                        "}\n"
        );
        runtime.compile();

        String result = Function.prettyPrint(runtime.getCoreInstance("test1").getValueForMetaPropertyToMany(M3Properties.children).get(0), runtime.getProcessorSupport());
        Assert.assertEquals(result, "A<T,V|m>(value: T[m]): V[*]");

        result = Function.prettyPrint(runtime.getCoreInstance("test2").getValueForMetaPropertyToMany(M3Properties.children).get(0), runtime.getProcessorSupport());
        Assert.assertEquals(result, "doSomething(param1: String[1], param2: A[1]): Any[*]");
    }
}
