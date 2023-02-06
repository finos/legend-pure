package org.finos.legend.pure.runtime.java.compiled.incremental.graph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.inlinedsl.graph.incremental.TestPureRuntimeGraph;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.runtime.java.compiled.CompiledClassloaderStateVerifier;
import org.finos.legend.pure.runtime.java.compiled.CompiledMetadataStateVerifier;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;

public class TestPureRuntimeGraphCompiled extends TestPureRuntimeGraph
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    protected ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getAdditionalVerifiers()
    {
        return Lists.fixedSize.of(new CompiledMetadataStateVerifier(), new CompiledClassloaderStateVerifier());
    }
}
