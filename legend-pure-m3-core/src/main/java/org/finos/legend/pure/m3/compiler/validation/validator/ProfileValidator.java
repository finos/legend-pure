package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Annotation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ProfileValidator implements MatchRunner<Profile>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Profile;
    }

    @Override
    public void run(Profile profile, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validateUniqueAnnotationValues(profile, profile._p_stereotypes(), "stereotype");
        validateUniqueAnnotationValues(profile, profile._p_tags(), "tag");
    }

    private void validateUniqueAnnotationValues(Profile profile, Iterable<? extends Annotation> annotations, String description)
    {
        MutableMap<String, Annotation> values = Maps.mutable.empty();
        annotations.forEach(a ->
        {
            String value = a._value();
            Annotation other = values.put(value, a);
            if (other != null)
            {
                StringBuilder builder = new StringBuilder("There is already a ").append(description).append(" named '").append(a._value()).append("' defined in ");
                PackageableElement.writeUserPathForPackageableElement(builder, profile);
                SourceInformation otherSourceInfo = other.getSourceInformation();
                if (otherSourceInfo != null)
                {
                    builder.append(" (at ").append(otherSourceInfo.getSourceId())
                            .append(" line:").append(otherSourceInfo.getLine())
                            .append(" column:").append(otherSourceInfo.getColumn())
                            .append(')');
                }
                throw new PureCompilationException(a.getSourceInformation(), builder.toString());
            }
        });
    }
}
