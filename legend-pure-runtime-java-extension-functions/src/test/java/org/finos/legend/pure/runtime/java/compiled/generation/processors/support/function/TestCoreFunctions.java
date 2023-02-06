package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function;

import junit.framework.Test;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class TestCoreFunctions
{
    public static Test suite()
    {
        return PureTestBuilderCompiled.buildSuite("meta::pure::functions");
    }
}
