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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class GenericTypeValidator implements MatchRunner<GenericType>
{
    @Override
    public String getClassName()
    {
        return M3Paths.GenericType;
    }

    @Override
    public void run(GenericType genericType, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();
        validateGenericType(genericType, true, processorSupport);
        Validator.testProperties(genericType, validatorState, matcher, processorSupport);
    }

    public static void validateGenericType(GenericType genericType, ProcessorSupport processorSupport)
    {
        validateGenericType(genericType, true, processorSupport);
    }

    public static void validateGenericType(GenericType genericType, boolean validateFullyDefined, ProcessorSupport processorSupport)
    {
        org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveImportStubs(genericType, processorSupport);
        try
        {
            validateGenericTypeRecursive(genericType, validateFullyDefined, processorSupport);
        }
        catch (PureCompilationException e)
        {
            if ((e.getSourceInformation() == null) && (genericType.getSourceInformation() != null))
            {
                StringBuilder message = new StringBuilder("Invalid generic type ");
                org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, genericType, processorSupport);
                message.append(": ");
                message.append(e.getInfo());
                throw new PureCompilationException(genericType.getSourceInformation(), message.toString(), e);
            }
            throw e;
        }
    }

    private static void validateGenericTypeRecursive(GenericType genericType,
                                                     boolean validateFullyDefined, ProcessorSupport processorSupport)
    {
        validateGenericTypeRecursive(ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport),
                (ListIterable)genericType._typeArguments(), (ListIterable)genericType._multiplicityArguments(),
                validateFullyDefined, genericType.getSourceInformation(), processorSupport);
    }

    private static void validateGenericTypeRecursive(CoreInstance rawType,
                                                     ListIterable<? extends GenericType> typeArguments,
                                                     ListIterable<? extends Multiplicity> multiplicityArguments,
                                                     boolean validateFullyDefined, SourceInformation sourceInformationForError,
                                                     ProcessorSupport processorSupport)
    {
        if (rawType != null)
        {
            if (rawType instanceof Class)
            {
                RichIterable<? extends TypeParameter> typeParameters = ((Class)rawType)._typeParameters();

                if (typeArguments.size() != typeParameters.size())
                {
                    StringBuilder message = new StringBuilder("Type argument mismatch for the class ");
                    _Class.print(message, rawType);
                    message.append(" (expected ");
                    message.append(typeParameters.size());
                    message.append(", got ");
                    message.append(typeArguments.size());
                    message.append("): ");

                    org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, rawType, typeArguments, multiplicityArguments, processorSupport);
                    throw new PureCompilationException(sourceInformationForError, message.toString());
                }
                for (GenericType typeArgument : typeArguments)
                {
                    if (typeArgument != null)
                    {
                        validateGenericTypeRecursive(typeArgument, validateFullyDefined, processorSupport);
                    }
                    else if (validateFullyDefined)
                    {
                        StringBuilder message = new StringBuilder("Generic type is not fully defined: ");
                        org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, rawType, typeArguments, multiplicityArguments, processorSupport);
                        throw new PureCompilationException(sourceInformationForError, message.toString());
                    }
                }

                RichIterable<? extends InstanceValue> multiplicityParameters = ((Class)rawType)._multiplicityParameters();
                if (multiplicityArguments.size() != multiplicityParameters.size())
                {
                    StringBuilder message = new StringBuilder("Multiplicity argument mismatch for the class ");
                    _Class.print(message, rawType);
                    message.append(" (expected ");
                    message.append(multiplicityParameters.size());
                    message.append(", got ");
                    message.append(multiplicityArguments.size());
                    message.append("): ");
                    org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, rawType, typeArguments, multiplicityArguments, processorSupport);
                    throw new PureCompilationException(sourceInformationForError, message.toString());
                }
                if (validateFullyDefined && multiplicityArguments.anySatisfy(Predicates.isNull()))
                {
                    StringBuilder message = new StringBuilder("Generic type is not fully defined: ");
                    org.finos.legend.pure.m3.navigation.generictype.GenericType.print(message, rawType, typeArguments, multiplicityArguments, processorSupport);
                    throw new PureCompilationException(sourceInformationForError, message.toString());
                }
            }
            else if (org.finos.legend.pure.m3.navigation.function.FunctionType.isFunctionType(rawType, processorSupport))
            {
                FunctionType rawTypeFunctionType = (FunctionType)rawType;
                for (VariableExpression parameter : rawTypeFunctionType._parameters())
                {
                    GenericType parameterGenericType = parameter._genericType();
                    if (parameterGenericType != null)
                    {
                        validateGenericTypeRecursive(parameterGenericType, validateFullyDefined, processorSupport);
                    }
                    if (validateFullyDefined && ((parameterGenericType == null) || parameter._multiplicity() == null))
                    {
                        StringBuilder message = new StringBuilder("Function type is not fully defined in ");
                        org.finos.legend.pure.m3.navigation.function.FunctionType.print(message, rawTypeFunctionType, processorSupport);
                        throw new PureCompilationException(sourceInformationForError, message.toString());
                    }
                }

                GenericType returnType = rawTypeFunctionType._returnType();
                if (returnType != null)
                {
                    validateGenericTypeRecursive(returnType, validateFullyDefined, processorSupport);
                }
                if (validateFullyDefined && ((returnType == null) || rawTypeFunctionType._returnMultiplicity() == null))
                {
                    StringBuilder message = new StringBuilder("Function type is not fully defined in ");
                    org.finos.legend.pure.m3.navigation.function.FunctionType.print(message, rawTypeFunctionType, processorSupport);
                    throw new PureCompilationException(sourceInformationForError, message.toString());
                }
            }
        }
    }

    public static void validateClassifierGenericTypeForInstance(CoreInstance instance, boolean validateFullyDefined, ProcessorSupport processorSupport)
    {
        GenericType genericType = (GenericType)instance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        try
        {
            if (genericType == null)
            {
                validateGenericTypeRecursive(instance.getClassifier(), Lists.fixedSize.<GenericType>empty(), Lists.fixedSize.<Multiplicity>empty(), validateFullyDefined,
                        instance.getSourceInformation(), processorSupport);
            }
            else
            {
                validateGenericType(genericType, validateFullyDefined, processorSupport);
            }
        }
        catch (PureCompilationException e)
        {
            if ((e.getSourceInformation() == null) && (instance.getSourceInformation() != null))
            {
                throw new PureCompilationException(instance.getSourceInformation(), e.getInfo(), e);
            }
            throw e;
        }
    }
}
