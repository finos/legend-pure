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
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.compiler.validation.VisibilityValidation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.execution.test.TestTools;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class FunctionDefinitionValidator implements MatchRunner<FunctionDefinition<CoreInstance>>
{
    private static final ImmutableSet<String> EQUALITY_FUNCTION_NAMES = Sets.immutable.with("is", "eq", "equal");

    @Override
    public String getClassName()
    {
        return M3Paths.FunctionDefinition;
    }

    @Override
    public void run(FunctionDefinition<CoreInstance> functionDefinition, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();
        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(functionDefinition);
        RichIterable<? extends VariableExpression> parameters = functionType._parameters();
        for (VariableExpression variable : parameters)
        {
            GenericType varGenericType = variable._genericType();
            if (varGenericType == null)
            {
                StringBuilder builder = new StringBuilder("Parameter type error in ");
                writeFunctionName(builder, functionDefinition);
                builder.append(": parameter '");
                builder.append(variable._name());
                builder.append("' has no type");
                throw new PureCompilationException(functionDefinition.getSourceInformation(), builder.toString());
            }
            Validator.validate(varGenericType, validatorState, matcher, processorSupport);
        }
        GenericType returnType = functionType._returnType();
        Validator.validate(returnType, validatorState, matcher, processorSupport);

        for (ValueSpecification expression : functionDefinition._expressionSequence())
        {
            Validator.validate(expression, validatorState, matcher, processorSupport);
        }
        if (parameters.notEmpty() && TestTools.hasAnyTestStereotype(functionDefinition, processorSupport))
        {
            StringBuilder builder = new StringBuilder("Error in ");
            writeFunctionName(builder, functionDefinition);
            builder.append(": test functions may not have parameters");
            throw new PureCompilationException(functionDefinition.getSourceInformation(), builder.toString());
        }
        validateEqualityFunctions(functionDefinition);
        validateFunctionDefinitionReturnType(functionDefinition, functionType, processorSupport);
        VisibilityValidation.validateFunctionDefinition(functionDefinition, context, validatorState, processorSupport);
    }

    public static void validateFunctionReturnType(Function function, FunctionType functionType, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (function instanceof FunctionDefinition)
        {
            validateFunctionDefinitionReturnType((FunctionDefinition<CoreInstance>)function, functionType, processorSupport);
        }
    }

    private static void validateFunctionDefinitionReturnType(FunctionDefinition<CoreInstance> function, FunctionType functionType, ProcessorSupport processorSupport)
    {
        ValueSpecification lastExpression = function._expressionSequence().getLast();

        // Type
        GenericType functionReturnType = functionType._returnType();
        GenericType lastExpressionType = lastExpression._genericType();
        if (!GenericTypeMatch.genericTypeMatches(functionReturnType, lastExpressionType, true, NullMatchBehavior.MATCH_NOTHING, ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
        {
            throwReturnTypeException(lastExpression.getSourceInformation(), function, functionReturnType, lastExpressionType, processorSupport);
        }

        // Multiplicity
        Multiplicity functionReturnMultiplicity = functionType._returnMultiplicity();
        Multiplicity lastExpressionMultiplicity = lastExpression._multiplicity();
        if (!MultiplicityMatch.multiplicityMatches(functionReturnMultiplicity, lastExpressionMultiplicity, true, NullMatchBehavior.MATCH_NOTHING, ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY))
        {
            throwReturnMultiplicityException(lastExpression.getSourceInformation(), function, functionReturnMultiplicity, lastExpressionMultiplicity);
        }
    }

    private static void validateEqualityFunctions(FunctionDefinition functionDefinition)
    {
        String functionName = functionDefinition._functionName();
        if (functionName != null && EQUALITY_FUNCTION_NAMES.contains(functionName))
        {
            throw new PureCompilationException(functionDefinition.getSourceInformation(), "It is forbidden to override the function '" + functionName + "'");
        }
    }

    private static void throwReturnTypeException(SourceInformation sourceInformation, FunctionDefinition function, GenericType expected, GenericType found, ProcessorSupport processorSupport) throws PureCompilationException
    {
        StringBuilder builder = new StringBuilder("Return type error in ");
        writeFunctionName(builder, function);
        builder.append("; found: ");
        if (found == null)
        {
            builder.append("null");
        }
        else
        {
            org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, found, true, processorSupport);
        }
        if (expected != null)
        {
            builder.append("; expected: ");
            org.finos.legend.pure.m3.navigation.generictype.GenericType.print(builder, expected, true, processorSupport);
        }

        throw new PureCompilationException(sourceInformation, builder.toString());
    }

    private static void throwReturnMultiplicityException(SourceInformation sourceInformation, FunctionDefinition function, Multiplicity expected, Multiplicity found) throws PureCompilationException
    {
        StringBuilder builder = new StringBuilder("Return multiplicity error in ");
        writeFunctionName(builder, function);
        builder.append("; found: ");
        if (found == null)
        {
            builder.append("null");
        }
        else
        {
            org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(builder, found, true);
        }
        if (expected != null)
        {
            builder.append("; expected: ");
            org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(builder, expected, true);
        }

        throw new PureCompilationException(sourceInformation, builder.toString());
    }

    private static void writeFunctionName(StringBuilder builder, FunctionDefinition function)
    {
        String functionName = function._functionName();
        if (functionName == null)
        {
            builder.append("lambda function");
        }
        else
        {
            builder.append("function '");
            builder.append(functionName);
            builder.append('\'');
        }
    }
}
