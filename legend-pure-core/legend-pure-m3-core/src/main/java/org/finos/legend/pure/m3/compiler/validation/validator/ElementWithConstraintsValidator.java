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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ModelElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithConstraints;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ElementWithConstraintsValidator implements MatchRunner<ElementWithConstraints>
{

    @Override
    public String getClassName()
    {
        return M3Paths.ElementWithConstraints;
    }

    @Override
    public void run(ElementWithConstraints instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validateConstraints((ModelElement) instance, instance._constraints(), state, matcher);
    }

    static void validateConstraints(ModelElement instance, RichIterable<? extends Constraint> constraints, MatcherState state, Matcher matcher) throws PureCompilationException
    {
        if (constraints.isEmpty())
        {
            return;
        }

        ValidatorState validatorState = (ValidatorState) state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();
        CoreInstance booleanType = processorSupport.package_getByUserPath(M3Paths.Boolean);
        CoreInstance stringType = processorSupport.package_getByUserPath(M3Paths.String);
        MutableSet<String> ruleNames = Sets.mutable.empty();
        for (Constraint constraint : constraints)
        {
            String ruleName = constraint._name();
            if (!ruleNames.add(ruleName))
            {
                String instanceName = instance._name();
                throw new PureCompilationException(constraint.getSourceInformation(), "Constraints for " + instanceName + " must be unique, [" + ruleName + "] is duplicated");
            }

            ValueSpecification definition = constraint._functionDefinition()._expressionSequence().getFirst();
            Validator.validate(definition, validatorState, matcher, processorSupport);
            Type type = (Type) ImportStub.withImportStubByPass(definition._genericType()._rawTypeCoreInstance(), processorSupport);
            if (type != booleanType || !Multiplicity.isToOne(definition._multiplicity(), true))
            {
                throw new PureCompilationException(constraint._functionDefinition().getSourceInformation(), "A constraint must be of type Boolean and multiplicity one");
            }

            if (constraint._messageFunction() != null)
            {
                ValueSpecification message = constraint._messageFunction()._expressionSequence().getFirst();
                Validator.validate(message, validatorState, matcher, processorSupport);
                Type messageType = (Type) ImportStub.withImportStubByPass(message._genericType()._rawTypeCoreInstance(), processorSupport);
                if (messageType != stringType || !Multiplicity.isToOne(message._multiplicity(), true))
                {
                    throw new PureCompilationException(constraint._messageFunction().getSourceInformation(), "A constraint message must be of type String and multiplicity one");
                }
            }
        }
    }
}
