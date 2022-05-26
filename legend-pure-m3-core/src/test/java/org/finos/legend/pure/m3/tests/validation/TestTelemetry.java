package org.finos.legend.pure.m3.tests.validation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTelemetry extends AbstractPureTestWithCoreCompiledPlatform
{
        @BeforeClass
        public static void setUp()
        {
            setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
        }

        protected static RichIterable<? extends CodeRepository> getCodeRepositories()
        {
            return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                    GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME),
                    GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system"));
        }

    @Test
    public void testTelemetry()
    {
        compileTestSource("fromString.pure", "import meta::pure::profiles::*;\n" +
                "\n" +
                "function {telemetry.metricGroup = 'Testing telemetry tag'} pkg::func():String[1]\n" +
                "{\n" +
                "   'testString'\n" +
                "}\n" );

        Assert.assertNotNull(runtime.getFunction("pkg::func():String[1]"));
    }
}
