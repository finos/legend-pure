package org.finos.legend.pure.m2.inlinedsl.path;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.MilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class PathMilestoningDatesVarNamesExtractor implements MilestoningDatesVarNamesExtractor
{
    public static final String PATH_MILESTONING_DATES_VARIABLE_NAME = "p_milestoning_dates";

    @Override
    public ImmutableList<String> getMilestoningDatesVarNames(ListIterable< ? extends CoreInstance> values, ProcessorSupport processorSupport)
    {
        if(processorSupport.package_getByUserPath(M2PathPaths.Path) != null)
        {
            if (values.size() >= 1 && Instance.instanceOf(values.getFirst(), M2PathPaths.Path, processorSupport))
            {
                return Lists.immutable.with(PATH_MILESTONING_DATES_VARIABLE_NAME);
            }
        }
        return Lists.immutable.empty();
    }
}
