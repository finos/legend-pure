// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ValueSpecificationUnbind implements MatchRunner<ValueSpecification>
{
    @Override
    public String getClassName()
    {
        return M3Paths.ValueSpecification;
    }

    @Override
    public void run(ValueSpecification valueSpecification, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        GenericType genericType = valueSpecification._genericType();
        if (genericType != null)
        {
            // We do that in order to clean up the referenceUsages...
            Shared.cleanUpGenericType(genericType, (UnbindState) state, state.getProcessorSupport());
        }

        if (!shouldKeepGenericTypeAndMultiplicity(valueSpecification))
        {
            valueSpecification._genericTypeRemove();
            valueSpecification._multiplicityRemove();
        }
    }

    // Keep for empty InstanceValues (such as those used for cast) and Unit instances
    private boolean shouldKeepGenericTypeAndMultiplicity(ValueSpecification valueSpecification)
    {
        if (valueSpecification instanceof InstanceValue)
        {
            InstanceValue instanceValue = (InstanceValue) valueSpecification;
            return instanceValue._valuesCoreInstance().isEmpty() || Measure.isUnitInstanceValueNoResolution(instanceValue);
        }
        return false;
    }
}
