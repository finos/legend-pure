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

package org.finos.legend.pure.m3.compiler.validation;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.PropertyValidator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class Validator
{
    private Validator()
    {
    }

    public static void validateM3(Iterable<? extends CoreInstance> newInstancesConsolidated, ValidationType validationType, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, CodeStorage codeStorage, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateM3(newInstancesConsolidated, validationType, parserLibrary, inlineDSLLibrary, Lists.immutable.<MatchRunner>empty(), codeStorage, modelRepository, context, processorSupport);
    }

    public static void validateM3(Iterable<? extends CoreInstance> newInstancesConsolidated, ValidationType validationType, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, Iterable<? extends MatchRunner> additionalValidators, CodeStorage codeStorage, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Post Process
        Matcher matcher = new Matcher(modelRepository, context, processorSupport);

        for (Parser parser : parserLibrary.getParsers())
        {
            for (MatchRunner parserValidator : parser.getValidators())
            {
                matcher.addMatchIfTypeIsKnown(parserValidator);
            }
        }
        for (InlineDSL dsl : inlineDSLLibrary.getInlineDSLs())
        {
            for (MatchRunner dslValidator : dsl.getValidators())
            {
                matcher.addMatchIfTypeIsKnown(dslValidator);
            }
        }
        for (MatchRunner validator : additionalValidators)
        {
            matcher.addMatchIfTypeIsKnown(validator);
        }

        ValidatorState validatorState = new ValidatorState(validationType, codeStorage, inlineDSLLibrary, processorSupport);
        for (CoreInstance instance : newInstancesConsolidated)
        {
            validate(instance, validatorState, matcher, processorSupport);
        }
    }

    public static void validate(CoreInstance coreInstance, ValidatorState validatorState, Matcher matcher, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (!coreInstance.hasBeenValidated() && !SourceMutation.isMarkedForDeletion(coreInstance))
        {
            GenericTypeValidator.validateClassifierGenericTypeForInstance(coreInstance, true, processorSupport);
            coreInstance.markValidated();
            if (!matcher.match(coreInstance, validatorState))
            {
                if (coreInstance.getClassifier() instanceof Class)
                {
                    testProperties(coreInstance, validatorState, matcher, processorSupport);
                }
            }
        }
    }

    public static void testProperties(CoreInstance coreInstance, ValidatorState validatorState, Matcher matcher, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (validatorState.getValidationType() == ValidationType.DEEP && !coreInstance.equals(processorSupport.package_getByUserPath(M3Paths.Class)))
        {
            // Test existing properties
            for (String propertyName : coreInstance.getKeys())
            {
                CoreInstance property = coreInstance.getKeyByName(propertyName);
                if (!(property instanceof Property))
                {
                    throw new RuntimeException("A key is not a property!\n" + property.print(""));
                }

                PropertyValidator.validateProperty((Property)property, processorSupport);

                ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(coreInstance, property, processorSupport);

                PropertyValidator.validateMultiplicityRange(coreInstance, property, values, processorSupport);

                for (CoreInstance instance : values)
                {
                    PropertyValidator.validateTypeRange(coreInstance, property, instance, processorSupport);
                    validate(instance, validatorState, matcher, processorSupport);
                }
            }
        }
    }
}
