package org.finos.legend.pure.runtime.java.compiled.functions;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class TestCoreFunctions
{
    public static Test suite()
    {
        return PureTestBuilderCompiled.buildSuite("meta::pure::functions");
    }
}
