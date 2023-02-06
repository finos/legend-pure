package org.finos.legend.pure.m2.inlinedsl.path;

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

import java.util.regex.Pattern;

public abstract class AbstractTestMatch extends PureExpressionTest
{
    @Test
    public void testMatchPureObjectTypeFail()
    {
        Pattern expectedInfo = Pattern.compile("^Match failure: (\\W*\\d*\\w*)\\(?\\d*\\)? instanceOf PathElement$");
        assertExpressionRaisesPureException(expectedInfo, "^meta::pure::metamodel::path::PathElement() ->match([i:Integer[1] | $i])");
    }
}
