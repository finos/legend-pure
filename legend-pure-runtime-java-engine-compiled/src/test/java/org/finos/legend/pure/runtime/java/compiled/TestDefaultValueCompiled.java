package org.finos.legend.pure.runtime.java.compiled;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.property.AbstractTestDefaultValue;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.BeforeClass;

public class TestDefaultValueCompiled extends AbstractTestDefaultValue {
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("defaultValueSource.pure");

        try
        {
            runtime.compile();
        } catch (PureCompilationException e) {
            setUp();
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
