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
    void resetTab();
    void shiftTab();
    void unShiftTab();

    void startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType);

    void startProcessingFunctionBody();

    void finishedProcessingFunctionBody();

    void finishedProcessingFunction(CoreInstance functionType);

    void startProcessingFunctionExpression(CoreInstance functionExpression);

    void startFirstPassParametersProcessing();

    void processingParameter(CoreInstance functionExpression, int i, ValueSpecification value);

    void inferenceResult(boolean success);

    void functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType);

    void firstPassInferenceFailed();

    void matchTypeParamsFromFoundFunction(CoreInstance foundFunction);

    void register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext);

    void registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext);

    void matchParam(int z);

    void paramInferenceFailed(int z);

    void reverseMatching();

    void parameterInferenceSucceeded();

    void returnType(CoreInstance returnGenericType);

    void returnTypeNotConcrete();

    void reprocessingTheParameter();

    void finishedProcessParameter();

    void newReturnType(CoreInstance returnGenericType);

    void finishedRegisteringParametersAndMultiplicities();

    void finishedProcessingFunctionExpression(CoreInstance functionExpression);
}
