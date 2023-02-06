package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProfileValidation extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final String SOURCE_ID = "/test/profileTest.pure";

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete(SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testStereotypeNameConflict()
    {
        String code = "Profile test::BadProfile\n" +
                "{\n" +
                "    stereotypes : [abc, def, abc, ghi, def];\n" +
                "}\n";
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(SOURCE_ID, code));
        Assert.assertEquals(new SourceInformation(SOURCE_ID, 3, 30, 3, 30, 3, 32), e.getSourceInformation());
        Assert.assertEquals("There is already a stereotype named 'abc' defined in test::BadProfile (at " + SOURCE_ID + " line:3 column:20)", e.getInfo());
    }

    @Test
    public void testTagNameConflict()
    {
        String code = "Profile test::BadProfile\n" +
                "{\n" +
                "    tags : [abc, xyz, def, xyz, abc];\n" +
                "}\n";
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(SOURCE_ID, code));
        Assert.assertEquals(new SourceInformation(SOURCE_ID, 3, 28, 3, 28, 3, 30), e.getSourceInformation());
        Assert.assertEquals("There is already a tag named 'xyz' defined in test::BadProfile (at " + SOURCE_ID + " line:3 column:18)", e.getInfo());
    }
}
