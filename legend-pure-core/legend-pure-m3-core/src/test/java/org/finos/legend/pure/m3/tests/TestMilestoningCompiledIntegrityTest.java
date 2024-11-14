package org.finos.legend.pure.m3.tests;

import org.junit.BeforeClass;

public class TestMilestoningCompiledIntegrityTest extends AbstractCompiledStateIntegrityTest
{
    @BeforeClass
    public static void initialize()
    {
        initialize("test_milestoning_repository");
    }
}
