package org.finos.legend.pure.runtime.java.interpreted.functions;

import junit.framework.Test;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

public class TestCoreFunctions
{
    public static Test suite()
    {
        return PureTestBuilderInterpreted.buildSuite("meta::pure::functions");
    }
}
