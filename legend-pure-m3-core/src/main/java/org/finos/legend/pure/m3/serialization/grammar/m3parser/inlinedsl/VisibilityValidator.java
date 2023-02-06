package org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public interface VisibilityValidator
{
    void validate(CoreInstance value, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException;
}
