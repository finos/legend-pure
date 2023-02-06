package org.finos.legend.pure.m2.inlinedsl.path.validation;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.VisibilityValidation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m2.inlinedsl.path.M2PathPaths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PathVisibilityValidator implements VisibilityValidator
{
    @Override
    public void validate(CoreInstance value, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (processorSupport.package_getByUserPath(M2PathPaths.Path) != null && Instance.instanceOf(value, M2PathPaths.Path, processorSupport))
        {
            validatePath(value, pkg, sourceId, context, validatorState, processorSupport);
        }
    }

    //TODO: move to m2-path
    private static void validatePath(CoreInstance path, CoreInstance pkg, String sourceId, Context context, ValidatorState validatorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        VisibilityValidation.validateGenericType((GenericType) Instance.getValueForMetaPropertyToOneResolved(path, M3Properties.start, processorSupport), pkg, sourceId, context, validatorState, processorSupport, true);
        // TODO consider validating the parameters of the Path
    }
}
