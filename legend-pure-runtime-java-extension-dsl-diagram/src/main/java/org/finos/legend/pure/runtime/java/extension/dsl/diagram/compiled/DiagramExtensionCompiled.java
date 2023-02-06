package org.finos.legend.pure.runtime.java.extension.dsl.diagram.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.coreinstance.DiagramCoreInstanceFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

public class DiagramExtensionCompiled extends BaseCompiledExtension
{
    public DiagramExtensionCompiled()
    {
        super(
                "platform_dsl_diagram",
                () -> Lists.fixedSize.with(),
                Lists.fixedSize.with(),
                Lists.fixedSize.with(),
                Lists.fixedSize.with());
    }

    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return DiagramCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    public static CompiledExtension extension()
    {
        return new DiagramExtensionCompiled();
    }

}
