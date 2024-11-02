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

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.VisibilityValidation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AssociationValidator implements MatchRunner<Association>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Association;
    }

    @Override
    public void run(Association association, MatcherState state, Matcher matcher, ModelRepository repository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        // Validate properties
        for (Property<?, ?> property : association._properties())
        {
            Validator.validate(property, validatorState, matcher, processorSupport);
            Type propertyRawType = (Type)ImportStub.withImportStubByPass(((FunctionType)processorSupport.function_getFunctionType(property))._returnType()._rawTypeCoreInstance(), processorSupport);
            if (propertyRawType == null)
            {
                throw new PureCompilationException(property.getSourceInformation(), "Association properties must have concrete types");
            }
            if (property._defaultValue() != null)
            {
                throw new PureCompilationException(property.getSourceInformation(), "Association properties must not have default values defined");
            }
            Validator.validate(propertyRawType, validatorState, matcher, processorSupport);
        }

        // Validate qualified properties
        for (QualifiedProperty<?> qualifiedProperty : association._qualifiedProperties())
        {
            Validator.validate(qualifiedProperty, validatorState, matcher, processorSupport);
            Type propertyRawType = (Type)ImportStub.withImportStubByPass(((FunctionType)processorSupport.function_getFunctionType(qualifiedProperty))._returnType()._rawTypeCoreInstance(), processorSupport);
            if (propertyRawType == null)
            {
                throw new PureCompilationException(qualifiedProperty.getSourceInformation(), "Association properties must have concrete types");
            }
            Validator.validate(propertyRawType, validatorState, matcher, processorSupport);
        }

        // Validate visibility
        VisibilityValidation.validateAssociation(association, context, validatorState, processorSupport);
    }
}
