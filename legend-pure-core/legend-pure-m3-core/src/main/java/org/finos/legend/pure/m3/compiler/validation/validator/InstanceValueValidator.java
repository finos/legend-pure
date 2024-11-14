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

package org.finos.legend.pure.m3.compiler.validation.validator;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class InstanceValueValidator implements MatchRunner<InstanceValue>
{
    @Override
    public String getClassName()
    {
        return M3Paths.InstanceValue;
    }

    @Override
    public void run(InstanceValue instanceValue, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();
        RichIterable<? extends CoreInstance> values = instanceValue._valuesCoreInstance();
        GenericTypeValidator.validateGenericType(instanceValue._genericType(), processorSupport);
        if (values.size() > 1)
        {
            validateNoNestedCollections(values, processorSupport);
        }
        for (CoreInstance value : values)
        {
            Validator.validate(value, validatorState, matcher, processorSupport);
        }
    }

    private void validateNoNestedCollections(RichIterable<? extends CoreInstance> values, ProcessorSupport processorSupport) throws PureCompilationException
    {
        for (CoreInstance value : values)
        {
            Multiplicity multiplicity = value instanceof ValueSpecification ? ((ValueSpecification)value)._multiplicity() : null;
            if ((multiplicity != null) && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(multiplicity, true))
            {
                StringBuilder message = new StringBuilder("Required multiplicity: 1, found: ");
                org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(message, multiplicity, false);
                throw new PureCompilationException(value.getSourceInformation(), message.toString());
            }
        }
    }
}
