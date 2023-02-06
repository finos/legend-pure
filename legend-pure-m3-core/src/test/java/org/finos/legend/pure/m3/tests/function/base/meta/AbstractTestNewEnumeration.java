package org.finos.legend.pure.m3.tests.function.base.meta;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestNewEnumeration extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testNewEnumeration()
    {

        compileTestSource("/test/test.pure",
                "function test::testFn():Any[*]\n" +
                        "{\n" +
                        " let  testEnum =  newEnumeration('test::testEnum',['value1','value2']);" +
                        "assert($testEnum->instanceOf(Enumeration), |'');" +
                        "assert($testEnum->subTypeOf(Enum), |'');" +
                        "$testEnum->enumValues()->map(e|assert($e->instanceOf(Enum), |'')); " +
                        "$testEnum->enumValues()->map(e|$e->id())->print(1);\n" +
                        "}\n");
        execute("test::testFn():Any[*]");
        Assert.assertEquals("[\n" +
                "   'value1'\n" +
                "   'value2'\n" +
                "]", functionExecution.getConsole().getLine(0));
    }

}
