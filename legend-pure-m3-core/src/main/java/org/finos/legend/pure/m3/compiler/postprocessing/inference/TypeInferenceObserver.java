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

public interface TypeInferenceObserver
{
    default TypeInferenceObserver resetTab()
    {
        return this;
    }

    default TypeInferenceObserver shiftTab()
    {
        return shiftTab(1);
    }

    default TypeInferenceObserver shiftTab(int i)
    {
        return this;
    }

    default TypeInferenceObserver unShiftTab()
    {
        return shiftTab(-1);
    }

    default TypeInferenceObserver unShiftTab(int i)
    {
        return shiftTab(-i);
    }

    default TypeInferenceObserver startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType)
    {
        return this;
    }

    default TypeInferenceObserver startProcessingFunctionBody()
    {
        return this;
    }

    default TypeInferenceObserver finishedProcessingFunctionBody()
    {
        return this;
    }

    default TypeInferenceObserver finishedProcessingFunction(CoreInstance functionType)
    {
        return this;
    }

    default TypeInferenceObserver startProcessingFunctionExpression(CoreInstance functionExpression)
    {
        return this;
    }

    default TypeInferenceObserver startFirstPassParametersProcessing()
    {
        return this;
    }

    default TypeInferenceObserver processingParameter(CoreInstance functionExpression, int i, ValueSpecification value)
    {
        return this;
    }

    default TypeInferenceObserver inferenceResult(boolean success)
    {
        return this;
    }

    default TypeInferenceObserver functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType)
    {
        return this;
    }

    default TypeInferenceObserver firstPassInferenceFailed()
    {
        return this;
    }

    default TypeInferenceObserver matchTypeParamsFromFoundFunction(CoreInstance foundFunction)
    {
        return this;
    }

    default TypeInferenceObserver register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        return this;
    }

    default TypeInferenceObserver registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        return this;
    }

    default TypeInferenceObserver matchParam(int z)
    {
        return this;
    }

    default TypeInferenceObserver paramInferenceFailed(int z)
    {
        return this;
    }

    default TypeInferenceObserver reverseMatching()
    {
        return this;
    }

    default TypeInferenceObserver parameterInferenceSucceeded()
    {
        return this;
    }

    default TypeInferenceObserver returnType(CoreInstance returnGenericType)
    {
        return this;
    }

    default TypeInferenceObserver returnTypeNotConcrete()
    {
        return this;
    }

    default TypeInferenceObserver reprocessingTheParameter()
    {
        return this;
    }

    default TypeInferenceObserver finishedProcessParameter()
    {
        return this;
    }

    default TypeInferenceObserver newReturnType(CoreInstance returnGenericType)
    {
        return this;
    }

    default TypeInferenceObserver finishedRegisteringParametersAndMultiplicities()
    {
        return this;
    }

    default TypeInferenceObserver finishedProcessingFunctionExpression(CoreInstance functionExpression)
    {
        return this;
    }
}
