package org.finos.legend.pure.runtime.java.extension.dsl.graph.compiled;

import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.GraphCoreInstanceFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

public class GraphExtensionCompiled implements CompiledExtension
{
    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return GraphCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_dsl_graph";
    }

    public static CompiledExtension extension()
    {
        return new GraphExtensionCompiled();
    }
}
