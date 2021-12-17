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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.VariableContext;
import org.finos.legend.pure.m3.compiler.postprocessing.VariableContext.VariableNameConflictException;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.compiler.validation.validator.FunctionDefinitionValidator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ExpressionSequenceValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class FunctionDefinitionProcessor extends Processor<FunctionDefinition<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.FunctionDefinition;
    }

    @Override
    public void process(FunctionDefinition<?> instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        process(instance, state, matcher, repository);
    }

    @Override
    public void populateReferenceUsages(FunctionDefinition<?> instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
    }

    public static void process(FunctionDefinition<?> functionDefinition, ProcessorState state, Matcher matcher, ModelRepository repository)
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        VariableContext variableContext = state.getVariableContext();
        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(functionDefinition);

        state.getObserver().startProcessingFunction(functionDefinition, functionType);

        URLPatternLibrary urlPatternLibrary = state.getURLPatternLibrary();
        if (urlPatternLibrary != null)
        {
            urlPatternLibrary.possiblyRegister(functionDefinition, processorSupport);
        }

        boolean shouldSetTypeInferenceContext = (functionDefinition instanceof ConcreteFunctionDefinition) && (functionDefinition._classifierGenericType() != null) && (functionDefinition._classifierGenericType()._rawTypeCoreInstance() != null) && "ConcreteFunctionDefinition".equals(functionDefinition._classifierGenericType()._rawTypeCoreInstance().getName());
        if (shouldSetTypeInferenceContext)
        {
            state.newTypeInferenceContext(functionType);
        }

        functionType._parameters().forEach(var ->
        {
            try
            {
                variableContext.registerValue(var._name(), var);
            }
            catch (VariableNameConflictException e)
            {
                throw new PureCompilationException(functionDefinition.getSourceInformation(), e.getMessage());
            }
            GenericType propertyType = var._genericType();
            // The property type may be null if it's a lambda expression...
            if (propertyType != null)
            {
                // We resolve because we want to fail fast if a given type is unknown...
                org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveGenericTypeUsingImports(propertyType, repository, processorSupport);
            }
        });

        ListIterable<? extends ValueSpecification> expressions = functionDefinition._expressionSequence().toList();
        if (expressions.isEmpty())
        {
            throw new PureCompilationException(functionDefinition.getSourceInformation(), "Function definition must contain at least one expression");
        }

        // We can only perform the analysis if the type of the function parameters have been specified or inferred.
        // We have to skip if the function parameter is a Lambda and it is processed (as part of an InstanceValue bound variable) before the functionExpression is matched to a Function and we have enough information to infer...
        // The function is going to be processed again after inference
        if (TypeInference.canProcessLambda(functionDefinition, state, processorSupport))
        {
            state.getObserver().shiftTab();
            state.getObserver().startProcessingFunctionBody();
            processExpressions(functionDefinition, expressions, matcher, state, processorSupport);
            findReturnTypesForLambda(functionDefinition, functionType, processorSupport);
            FunctionDefinitionValidator.validateFunctionReturnType(functionDefinition, functionType, processorSupport);
            state.getObserver().finishedProcessingFunctionBody();
            state.getObserver().unShiftTab();
            state.addFunctionDefinition(functionDefinition);
        }

        if (shouldSetTypeInferenceContext)
        {
            state.deleteTypeInferenceContext();
        }

        state.getVariableContext().buildAndRegister("return", functionType._returnType(), functionType._returnMultiplicity(), processorSupport);

        RichIterable<? extends Constraint> constraints = functionDefinition._preConstraints();
        if (constraints.notEmpty())
        {
            processConstraints(functionDefinition, constraints.toList(), matcher, state, processorSupport);
        }
        RichIterable<? extends Constraint> postConstraints = functionDefinition._postConstraints();
        if (postConstraints.notEmpty())
        {
            processConstraints(functionDefinition, postConstraints.toList(), matcher, state, processorSupport);
        }

        state.getObserver().finishedProcessingFunction(functionType);
    }

    private static void processExpressions(FunctionDefinition<?> functionDefinition, ListIterable<? extends ValueSpecification> expressions, Matcher matcher, ProcessorState processorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        expressions.forEachWithIndex((expression, i) -> processExpression(functionDefinition, matcher, processorState, processorSupport, i, expression));
    }

    private static void processConstraints(FunctionDefinition<?> functionDefinition, ListIterable<? extends Constraint> constraints, Matcher matcher, ProcessorState processorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        constraints.forEachWithIndex((constraint, i) -> processExpression(functionDefinition, matcher, processorState, processorSupport, i, constraint._functionDefinition()._expressionSequence().getOnly()));
    }

    private static void processExpression(FunctionDefinition<?> functionDefinition, Matcher matcher, ProcessorState processorState, ProcessorSupport processorSupport, int i, ValueSpecification expression)
    {
        processorState.resetVariables();
        PostProcessor.processElement(matcher, expression, processorState, processorSupport);

        if (expression._usageContext() == null)
        {
            ExpressionSequenceValueSpecificationContext usageContext = (ExpressionSequenceValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.ExpressionSequenceValueSpecificationContext);
            usageContext._offset(i);
            usageContext._functionDefinition(functionDefinition);
            expression._usageContext(usageContext);
        }
    }

    public static boolean shouldInferTypesForFunctionParameters(FunctionType functionType)
    {
        return functionType._parameters().anySatisfy(p -> p._genericType() == null);
    }

    private static void findReturnTypesForLambda(FunctionDefinition<?> function, FunctionType functionType, ProcessorSupport processorSupport) throws PureCompilationException
    {
        ValueSpecification lastExpression = function._expressionSequence().toList().getLast();
        if (functionType._returnType() == null)
        {
            GenericType lastExpressionGenericType = lastExpression._genericType();
            if (lastExpressionGenericType == null)
            {
                throw new PureCompilationException(lastExpression.getSourceInformation(), "Final expression has no generic type");
            }
            GenericType lambdaReturnType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericTypeAsInferredGenericType(lastExpressionGenericType, function.getSourceInformation(), processorSupport);
            if (function._classifierGenericType() != null && function._classifierGenericType()._typeArguments() != null && function._classifierGenericType()._typeArguments().size() > 0 && function._classifierGenericType()._typeArguments().toList().getFirst()._rawTypeCoreInstance() != null)
            {
                ((FunctionType) ImportStub.withImportStubByPass(function._classifierGenericType()._typeArguments().toList().getFirst()._rawTypeCoreInstance(), processorSupport))._returnType(lambdaReturnType);
            }
        }
        if (functionType._returnMultiplicity() == null)
        {
            Multiplicity lastExpressionMultiplicity = lastExpression._multiplicity();
            if (lastExpressionMultiplicity == null)
            {
                throw new PureCompilationException(lastExpression.getSourceInformation(), "Final expression has no multiplicity");
            }
            if (function._classifierGenericType() != null && function._classifierGenericType()._typeArguments() != null && function._classifierGenericType()._typeArguments().size() > 0 && function._classifierGenericType()._typeArguments().toList().getFirst()._rawTypeCoreInstance() != null)
            {
                ((FunctionType) ImportStub.withImportStubByPass(function._classifierGenericType()._typeArguments().toList().getFirst()._rawTypeCoreInstance(), processorSupport))._returnMultiplicity(lastExpressionMultiplicity);
            }
        }
    }
}
