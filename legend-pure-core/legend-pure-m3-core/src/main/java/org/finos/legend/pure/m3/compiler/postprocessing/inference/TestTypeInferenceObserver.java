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

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class TestTypeInferenceObserver implements TypeInferenceObserver
{
    private final TypeInferenceObserver printObserver;
    private final MutableStack<TypeInferenceObserver> stack = Stacks.mutable.empty();

    public TestTypeInferenceObserver(ProcessorState processorState)
    {
        this.printObserver = new PrintTypeInferenceObserver(processorState);
        this.stack.push(new VoidTypeInferenceObserver());
    }

    @Deprecated
    public TestTypeInferenceObserver(ProcessorSupport processorSupport, ProcessorState processorState)
    {
        this(processorState);
    }

    private TypeInferenceObserver activeObserver()
    {
        return this.stack.peek();
    }

    @Override
    public TypeInferenceObserver startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType)
    {
        SourceInformation sourceInfo = functionDefinition.getSourceInformation();
        this.stack.push(((sourceInfo != null) && "inferenceTest.pure".equals(sourceInfo.getSourceId())) ? this.printObserver : this.stack.peek());
        activeObserver().startProcessingFunction(functionDefinition, functionType);
        return this;
    }

    @Override
    public TypeInferenceObserver finishedProcessingFunction(CoreInstance functionType)
    {
        activeObserver().finishedProcessingFunction(functionType);
        this.stack.pop();
        return this;
    }

    public boolean isInATest()
    {
        return this.stack.peek() == this.printObserver;
    }

    @Override
    public TypeInferenceObserver resetTab()
    {
        activeObserver().resetTab();
        return this;
    }

    @Override
    public TypeInferenceObserver shiftTab(int i)
    {
        activeObserver().shiftTab(i);
        return this;
    }

    @Override
    public TypeInferenceObserver startProcessingFunctionBody()
    {
        activeObserver().startProcessingFunctionBody();
        return this;
    }

    @Override
    public TypeInferenceObserver finishedProcessingFunctionBody()
    {
        activeObserver().finishedProcessingFunctionBody();
        return this;
    }

    @Override
    public TypeInferenceObserver startProcessingFunctionExpression(CoreInstance functionExpression)
    {
        activeObserver().startProcessingFunctionExpression(functionExpression);
        return this;
    }

    @Override
    public TypeInferenceObserver startFirstPassParametersProcessing()
    {
        activeObserver().startFirstPassParametersProcessing();
        return this;
    }

    @Override
    public TypeInferenceObserver processingParameter(CoreInstance functionExpression, int i, ValueSpecification value)
    {
        activeObserver().processingParameter(functionExpression, i, value);
        return this;
    }

    @Override
    public TypeInferenceObserver inferenceResult(boolean success)
    {
        activeObserver().inferenceResult(success);
        return this;
    }

    @Override
    public TypeInferenceObserver functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType)
    {
        activeObserver().functionMatched(foundFunction, foundFunctionType);
        return this;
    }

    @Override
    public TypeInferenceObserver firstPassInferenceFailed()
    {
        activeObserver().firstPassInferenceFailed();
        return this;
    }

    @Override
    public TypeInferenceObserver matchTypeParamsFromFoundFunction(CoreInstance foundFunction)
    {
        activeObserver().matchTypeParamsFromFoundFunction(foundFunction);
        return this;
    }

    @Override
    public TypeInferenceObserver register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        activeObserver().register(templateGenType, valueForMetaPropertyToOne, context, targetGenericsContext);
        return this;
    }

    @Override
    public TypeInferenceObserver registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        activeObserver().registerMul(templateMul, valueMul, context, targetGenericsContext);
        return this;
    }

    @Override
    public TypeInferenceObserver matchParam(int z)
    {
        activeObserver().matchParam(z);
        return this;
    }

    @Override
    public TypeInferenceObserver paramInferenceFailed(int z)
    {
        activeObserver().paramInferenceFailed(z);
        return this;
    }

    @Override
    public TypeInferenceObserver reverseMatching()
    {
        activeObserver().reverseMatching();
        return this;
    }

    @Override
    public TypeInferenceObserver parameterInferenceSucceeded()
    {
        activeObserver().parameterInferenceSucceeded();
        return this;
    }

    @Override
    public TypeInferenceObserver returnType(CoreInstance returnGenericType)
    {
        activeObserver().returnType(returnGenericType);
        return this;
    }

    @Override
    public TypeInferenceObserver returnTypeNotConcrete()
    {
        activeObserver().returnTypeNotConcrete();
        return this;
    }

    @Override
    public TypeInferenceObserver reprocessingTheParameter()
    {
        activeObserver().reprocessingTheParameter();
        return this;
    }

    @Override
    public TypeInferenceObserver finishedProcessParameter()
    {
        activeObserver().finishedProcessParameter();
        return this;
    }

    @Override
    public TypeInferenceObserver newReturnType(CoreInstance returnGenericType)
    {
        activeObserver().newReturnType(returnGenericType);
        return this;
    }

    @Override
    public TypeInferenceObserver finishedRegisteringParametersAndMultiplicities()
    {
        activeObserver().finishedRegisteringParametersAndMultiplicities();
        return this;
    }

    @Override
    public TypeInferenceObserver finishedProcessingFunctionExpression(CoreInstance functionExpression)
    {
        activeObserver().finishedProcessingFunctionExpression(functionExpression);
        return this;
    }
}
