package org.finos.legend.pure.m3.tests.function.base.math;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueAccessor;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public abstract class AbstractTestMinus extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testNegativeInteger()
    {
        testThis("-1", -1L);
        testThis("-(1)", -1L);
        testThis("-(-1)", 1L);
        testThis("-9223372036854775808", Long.MIN_VALUE);
    }

    @Test
    public void testNegativeFloat()
    {
        testThis("-1.0f", -1.0);
        testThis("-(1.1)", -1.1);
        testThis("-(-1.5)", 1.5);
    }

    @Test
    public void testNegativeDecimal()
    {
        testThis("-1.0D", new BigDecimal("-1.0"));
        testThis("-(1.1D)", new BigDecimal("-1.1"));
        testThis("-(-1.5D)", new BigDecimal("1.5"));
    }

    private void testThis(String expression, Number expectedResult) {
        compileTestSource("fromString.pure",
                "function test():Number[1]\n" +
                        "{\n" +
                        expression + ";\n" +
                        "}\n");
        CoreInstance result = this.execute("test():Number[1]");
        Assert.assertEquals(expectedResult, ((InstanceValueAccessor) result)._values().getAny());
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSubtraction()
    {
        testThis("2-1", 1L);
        testThis("2--1", 3L);
        testThis("-(2-1)", -1L);
        testThis("-1-2", -3L);
        testThis("-2--1", -1L);
        testThis("-(-2--1)", 1L);
        testThis("-(-2---1)", 3L);
    }
}
