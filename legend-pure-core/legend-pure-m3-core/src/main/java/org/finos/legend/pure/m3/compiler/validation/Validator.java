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
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.compiler.validation.validator.PropertyValidator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class Validator
{
    private Validator()
    {
    }

    public static void validateM3(Iterable<? extends CoreInstance> newInstancesConsolidated, ValidationType validationType, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, RepositoryCodeStorage codeStorage, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        validateM3(newInstancesConsolidated, validationType, parserLibrary, inlineDSLLibrary, Lists.immutable.empty(), codeStorage, modelRepository, context, processorSupport);
    }

    public static void validateM3(Iterable<? extends CoreInstance> newInstancesConsolidated, ValidationType validationType, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, Iterable<? extends MatchRunner> additionalValidators, RepositoryCodeStorage codeStorage, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Post Process
        Matcher matcher = new Matcher(modelRepository, context, processorSupport);

        parserLibrary.getParsers().forEach(p -> p.getValidators().forEach(matcher::addMatchIfTypeIsKnown));
        inlineDSLLibrary.getInlineDSLs().forEach(d -> d.getValidators().forEach(matcher::addMatchIfTypeIsKnown));
        additionalValidators.forEach(matcher::addMatchIfTypeIsKnown);

        ValidatorState validatorState = new ValidatorState(validationType, codeStorage, inlineDSLLibrary, processorSupport);
        newInstancesConsolidated.forEach(i -> validate(i, validatorState, matcher, processorSupport));
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

    private static void validateClassifier(CoreInstance coreInstance)
    {
        CoreInstance classifier = coreInstance.getClassifier();
        if (classifier == null)
        {
            throw new PureCompilationException(coreInstance.getSourceInformation(), "Instance has no classifier: " + coreInstance);
        }

        if (classifier.getValueForMetaPropertyToMany(M3Properties.typeVariables).notEmpty())
        {
            StringBuilder builder = new StringBuilder("Instance has a classifier with type variables, which is currently not supported: ");
            _Class.print(builder, classifier, true);
            throw new PureCompilationException(coreInstance.getSourceInformation(), builder.toString());
        }
    }

    public static void testProperties(CoreInstance coreInstance, ValidatorState validatorState, Matcher matcher, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (validatorState.getValidationType() == ValidationType.DEEP && !coreInstance.equals(processorSupport.package_getByUserPath(M3Paths.Class)))
        {
            // Test existing properties
            coreInstance.getKeys().forEach(propertyName ->
            {
                CoreInstance property = coreInstance.getKeyByName(propertyName);
                if (!(property instanceof Property))
                {
                    StringBuilder builder = new StringBuilder("Key '").append(propertyName).append("' is not a property!");
                    if (property == null)
                    {
                        throw new PureCompilationException(coreInstance.getSourceInformation(), builder.toString());
                    }
                    property.print(builder.append('\n'), "");
                    SourceInformation propertySourceInfo = property.getSourceInformation();
                    throw new PureCompilationException((propertySourceInfo == null) ? coreInstance.getSourceInformation() : propertySourceInfo, builder.toString());
                }

                PropertyValidator.validateProperty((Property<?, ?>) property, processorSupport);
                ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(coreInstance, property, processorSupport);
                PropertyValidator.validateMultiplicityRange(coreInstance, property, values, processorSupport);
                values.forEach(value ->
                {
                    PropertyValidator.validateTypeRange(coreInstance, property, value, processorSupport);
                    validate(value, validatorState, matcher, processorSupport);
                });
            });
        }
    }
}
