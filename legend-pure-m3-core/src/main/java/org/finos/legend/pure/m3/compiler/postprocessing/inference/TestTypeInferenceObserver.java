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

package org.finos.legend.pure.m3.compiler.postprocessing.inference;

import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Stack;

public class TestTypeInferenceObserver implements TypeInferenceObserver
{
    private static final TypeInferenceObserver VOID_OBSERVER = new VoidTypeInferenceObserver();

    private final TypeInferenceObserver printObserver;
    private final Stack<TypeInferenceObserver> stack = new Stack<>();

    public TestTypeInferenceObserver(ProcessorSupport processorSupport, ProcessorState processorState)
    {
        this.printObserver = new PrintTypeInferenceObserver(processorSupport, processorState);
        this.stack.push(VOID_OBSERVER);
    }

    private TypeInferenceObserver activeObserver()
    {
        return stack.peek();
    }

    @Override
    public void startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType)
    {
        SourceInformation sourceInfo = functionDefinition.getSourceInformation();
        stack.push(sourceInfo != null && sourceInfo.getSourceId().equals("inferenceTest.pure") ? printObserver : stack.peek());
        activeObserver().startProcessingFunction(functionDefinition, functionType);
    }

    @Override
    public void finishedProcessingFunction(CoreInstance functionType)
    {
        activeObserver().finishedProcessingFunction(functionType);
        stack.pop();
    }

    public boolean isInATest()
    {
        return stack.peek() == printObserver;
    }

    @Override public void resetTab() { activeObserver().resetTab(); }
    @Override public void shiftTab() { activeObserver().shiftTab(); }
    @Override public void unShiftTab() { activeObserver(). unShiftTab(); }
    @Override public void startProcessingFunctionBody() { activeObserver().startProcessingFunctionBody(); }
    @Override public void finishedProcessingFunctionBody() { activeObserver().finishedProcessingFunctionBody(); }
    @Override public void startProcessingFunctionExpression(CoreInstance functionExpression) { activeObserver().startProcessingFunctionExpression(functionExpression); }
    @Override public void startFirstPassParametersProcessing() { activeObserver().startFirstPassParametersProcessing(); }
    @Override public void processingParameter(CoreInstance functionExpression, int i, ValueSpecification value) { activeObserver().processingParameter(functionExpression, i, value); }
    @Override public void inferenceResult(boolean success) { activeObserver().inferenceResult(success);  }
    @Override public void functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType) { activeObserver().functionMatched(foundFunction, foundFunctionType); }
    @Override public void firstPassInferenceFailed() { activeObserver().firstPassInferenceFailed(); }
    @Override public void matchTypeParamsFromFoundFunction(CoreInstance foundFunction) { activeObserver().matchTypeParamsFromFoundFunction(foundFunction); }
    @Override public void register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext) { activeObserver().register(templateGenType, valueForMetaPropertyToOne, context, targetGenericsContext); }
    @Override public void registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext) { activeObserver().registerMul(templateMul, valueMul, context, targetGenericsContext); }
    @Override public void matchParam(int z) { activeObserver().matchParam(z); }
    @Override public void paramInferenceFailed(int z) { activeObserver().paramInferenceFailed(z); }
    @Override public void reverseMatching() { activeObserver().reverseMatching(); }
    @Override public void parameterInferenceSucceeded() { activeObserver().parameterInferenceSucceeded(); }
    @Override public void returnType(CoreInstance returnGenericType) { activeObserver().returnType(returnGenericType); }
    @Override public void returnTypeNotConcrete() { activeObserver().returnTypeNotConcrete(); }
    @Override public void reprocessingTheParameter() { activeObserver().reprocessingTheParameter(); }
    @Override public void finishedProcessParameter() { activeObserver().finishedProcessParameter(); }
    @Override public void newReturnType(CoreInstance returnGenericType) { activeObserver().newReturnType(returnGenericType); }
    @Override public void finishedRegisteringParametersAndMultiplicities() { activeObserver().finishedRegisteringParametersAndMultiplicities(); }
    @Override public void finishedProcessingFunctionExpression(CoreInstance functionExpression) { activeObserver().finishedProcessingFunctionExpression(functionExpression); }
}
