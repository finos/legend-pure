package org.finos.legend.pure.runtime.java.interpreted;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.property.AbstractTestDefaultValue;
import org.junit.After;
import org.junit.BeforeClass;

public class TestDefaultValueInterpreted extends AbstractTestDefaultValue {
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("defaultValueSource.pure");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
