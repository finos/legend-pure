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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class VoidTypeInferenceObserver implements TypeInferenceObserver
{
    @Override
    public void resetTab()
    {

    }

    @Override
    public void shiftTab()
    {

    }

    @Override
    public void unShiftTab()
    {

    }

    @Override
    public void startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType)
    {

    }

    @Override
    public void startProcessingFunctionBody()
    {

    }

    @Override
    public void finishedProcessingFunctionBody()
    {

    }

    @Override
    public void finishedProcessingFunction(CoreInstance functionType)
    {

    }

    @Override
    public void startProcessingFunctionExpression(CoreInstance functionExpression)
    {

    }

    @Override
    public void startFirstPassParametersProcessing()
    {

    }

    @Override
    public void processingParameter(CoreInstance functionExpression, int i, ValueSpecification value)
    {

    }

    @Override
    public void inferenceResult(boolean success)
    {

    }

    @Override
    public void functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType)
    {

    }

    @Override
    public void firstPassInferenceFailed()
    {

    }

    @Override
    public void matchTypeParamsFromFoundFunction(CoreInstance foundFunction)
    {

    }

    @Override
    public void register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {

    }

    @Override
    public void registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {

    }

    @Override
    public void matchParam(int z)
    {

    }

    @Override
    public void paramInferenceFailed(int z)
    {

    }

    @Override
    public void reverseMatching()
    {

    }

    @Override
    public void parameterInferenceSucceeded()
    {

    }

    @Override
    public void returnType(CoreInstance returnGenericType)
    {

    }

    @Override
    public void returnTypeNotConcrete()
    {

    }

    @Override
    public void reprocessingTheParameter()
    {

    }

    @Override
    public void finishedProcessParameter()
    {

    }

    @Override
    public void newReturnType(CoreInstance returnGenericType)
    {

    }

    @Override
    public void finishedRegisteringParametersAndMultiplicities()
    {

    }

    @Override
    public void finishedProcessingFunctionExpression(CoreInstance functionExpression)
    {

    }
}
