package org.finos.legend.pure.runtime.java.extension.dsl.diagram.compiled;

import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.DiagramCoreInstanceFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

public class DiagramExtensionCompiled implements CompiledExtension
{
    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return DiagramCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_dsl_diagram";
    }

    public static CompiledExtension extension()
    {
        return new DiagramExtensionCompiled();
    }
}
