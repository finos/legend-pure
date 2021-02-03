package org.finos.legend.pure.m3;

import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.BeforeClass;

public class TestM3CoreCompiledStateIntegrity extends AbstractCompiledStateIntegrityTest
{
    @BeforeClass
    public static void initialize()
    {
        MutableCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()));
        initialize(codeStorage);
    }
}
