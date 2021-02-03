package org.finos.legend.pure.code.core;

import org.finos.legend.pure.m3.AbstractCompiledStateIntegrityTest;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestCoreCompiledStateIntegrity extends AbstractCompiledStateIntegrityTest
{
    @BeforeClass
    public static void initialize()
    {
        MutableCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository(), CodeRepository.newCoreCodeRepository()));
        initialize(codeStorage);
    }

    @Test
    @Ignore
    public void testPackageHasChildrenWithDuplicateNames()
    {
        // TODO fix this test
        super.testPackageHasChildrenWithDuplicateNames();
    }

    @Test
    @Ignore
    public void testReferenceUsages()
    {
        // TODO fix this test
        super.testReferenceUsages();
    }
}
