package org.finos.legend.pure.runtime.java.extension.dsl.graph.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.GraphCoreInstanceFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CoreExtensionCompiled;

public class GraphExtensionCompiled extends BaseCompiledExtension
{
    public GraphExtensionCompiled()
    {
        super(
                "platform_dsl_graph",
                () -> Lists.fixedSize.with(),
                Lists.fixedSize.with(),
                Lists.fixedSize.with(),
                Lists.fixedSize.with());
    }

    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return GraphCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    public static CompiledExtension extension()
    {
        return new GraphExtensionCompiled();
    }
}
