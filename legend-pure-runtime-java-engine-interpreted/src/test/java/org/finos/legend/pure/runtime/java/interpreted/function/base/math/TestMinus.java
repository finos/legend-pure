package org.finos.legend.pure.runtime.java.interpreted.function.base.math;

import org.finos.legend.pure.m3.tests.function.base.math.AbstractTestMinus;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;

public class TestMinus extends AbstractTestMinus
{
    @BeforeClass
    public static void setUp() throws Exception
    {
        setUpRuntime(new FunctionExecutionInterpreted());
    }
}
