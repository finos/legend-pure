package org.finos.legend.pure.runtime.java.extension.dsl.path.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.coreinstance.PathCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;

import java.util.function.Function;

public class PathExtensionCompiled implements CompiledExtension
{
    @Override
    public RichIterable<? extends Pair<String, Function<? super CoreInstance, String>>> getExtraIdBuilders(ProcessorSupport processorSupport)
    {
        if (processorSupport.package_getByUserPath(M2PathPaths.Path) != null)
        {
            return Lists.immutable.with(Tuples.pair(M2PathPaths.Path, PathExtensionCompiled::buildIdForPath));
        }
        return Lists.immutable.empty();
    }

    @Override
    public PureFunction1<Object, Object> getExtraFunctionEvaluation(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        if (func instanceof Path)
        {
            return new PureFunction1<Object, Object>()
            {
                @Override
                public Object execute(ListIterable<?> vars, ExecutionSupport es)
                {
                    return value(vars.getFirst(), es);
                }

                @Override
                public Object value(Object o, ExecutionSupport es)
                {
                    RichIterable<?> result = ((Path<?, ?>) func)._path().injectInto(CompiledSupport.toPureCollection(o), (mutableList, path) ->
                    {
                        if (!(path instanceof PropertyPathElement))
                        {
                            throw new PureExecutionException("Only PropertyPathElement is supported yet!");
                        }
                        return mutableList.flatCollect(instance ->
                        {
                            MutableList<Object> parameters = ((PropertyPathElement) path)._parameters().collect(o1 -> o1 instanceof InstanceValue ? ((InstanceValue) o1)._values() : null, Lists.mutable.with(instance));
                            return CompiledSupport.toPureCollection(Pure.evaluate(es, ((PropertyPathElement) path)._property(), bridge, parameters.toArray()));
                        });
                    });
                    Multiplicity mult = func._classifierGenericType()._multiplicityArguments().getFirst();
                    return Pure.hasToOneUpperBound(mult) ? result.getFirst() : result;
                }
            };
        }
        return null;
    }

    private static String buildIdForPath(CoreInstance path)
    {
        SourceInformation sourceInfo = path.getSourceInformation();
        return sourceInfo.getMessage();
    }

    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return PathCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_dsl_path";
    }

    public static CompiledExtension extension()
    {
        return new PathExtensionCompiled();
    }
}
