package org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface MilestoningDatesVarNamesExtractor
{
    RichIterable<String> getMilestoningDatesVarNames(ListIterable<? extends CoreInstance> value, ProcessorSupport processorSupport);
}
