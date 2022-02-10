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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class PrintTypeInferenceObserver implements TypeInferenceObserver
{
    private final SafeAppendable appendable;
    private int tab;
    private final ProcessorSupport processorSupport;
    private final ProcessorState processorState;

    public PrintTypeInferenceObserver(Appendable appendable, ProcessorSupport processorSupport, ProcessorState processorState)
    {
        this.appendable = SafeAppendable.wrap(appendable);
        this.processorSupport = processorSupport;
        this.processorState = processorState;
    }

    public PrintTypeInferenceObserver(ProcessorSupport processorSupport, ProcessorState processorState)
    {
        this(System.out, processorSupport, processorState);
    }

    @Override
    public void resetTab()
    {
        this.tab = 0;
    }

    @Override
    public void shiftTab()
    {
        this.tab += 2;
    }

    @Override
    public void unShiftTab()
    {
        this.tab = Math.max(0, this.tab - 2);
    }

    @Override
    public void startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType)
    {
        printTab().print("Process function '").print(((FunctionDefinition<?>) functionDefinition)._name());
        SourceInformation sourceInfo = functionDefinition.getSourceInformation();
        if (sourceInfo != null)
        {
            sourceInfo.appendM4String(this.appendable);
        }
        print(" '(");
        FunctionType.print(this.appendable, functionType, this.processorSupport);
        print(')').printNewline();
    }

    @Override
    public void startProcessingFunctionBody()
    {
        printTab().print("Processing function body ").printTypeInferenceContext().printNewline();
    }

    @Override
    public void finishedProcessingFunctionBody()
    {
        printTab().print("Finished processing function body").printNewline();
    }

    @Override
    public void finishedProcessingFunction(CoreInstance functionType)
    {
        printTab().print("Finished processing function / ");
        FunctionType.print(this.appendable, functionType, this.processorSupport);
        printNewline();
    }

    @Override
    public void startProcessingFunctionExpression(CoreInstance functionExpression)
    {
        printTab().print("Process function expression for function: '").print(((FunctionExpression) functionExpression)._functionName());
        SourceInformation sourceInfo = functionExpression.getSourceInformation();
        if (sourceInfo != null)
        {
            sourceInfo.appendM4String(this.appendable);
        }
        print("' ").printTypeInferenceContext().printNewline();
    }

    @Override
    public void startFirstPassParametersProcessing()
    {
        printTab().print("- First pass parameters processing:").printNewline();
    }

    @Override
    public void processingParameter(CoreInstance functionExpression, int i, ValueSpecification value)
    {
        printTab().print("Process param: ").print(i + 1).print('/').print(((FunctionExpression) functionExpression)._parametersValues().size());
        SourceInformation sourceInfo = value.getSourceInformation();
        print(' ');
        if (value instanceof VariableExpression)
        {
            print(((VariableExpression) value)._name());
        }
        if (sourceInfo != null)
        {
            sourceInfo.appendM4String(this.appendable);
        }
        printNewline();
    }

    @Override
    public void inferenceResult(boolean success)
    {
        printTab().print("-> inference (success:").print(success).print(")").printNewline();
    }

    @Override
    public void functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType)
    {
        printTab().print("- Function matched: name:'").print(foundFunction.getName()).print("'  signature:'");
        FunctionType.print(this.appendable, foundFunctionType, this.processorSupport);
        print('\'').printNewline();
    }

    @Override
    public void firstPassInferenceFailed()
    {
        printTab().print("- Parameters inference failed").printNewline();
    }

    @Override
    public void matchTypeParamsFromFoundFunction(CoreInstance foundFunction)
    {
        printTab().print("Matching type parameters and multiplicity parameters (from the found function '").print(foundFunction.getName()).print("')").printNewline();
    }

    @Override
    public void register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        printTab().print(". Register ");
        GenericType.print(this.appendable, templateGenType, this.processorSupport);
        print(" / ");
        GenericType.print(this.appendable, valueForMetaPropertyToOne, this.processorSupport);
        print(" in ").print(context.getId()).print("/").print(targetGenericsContext.getId()).print("   ");
        printTypeInferenceContext().printNewline();
    }

    @Override
    public void registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        printTab().print(". Register Mul ");
        Multiplicity.print(this.appendable, templateMul, true);
        print(" / ");
        Multiplicity.print(this.appendable, valueMul, true);
        print(" in ").print(context.getId()).print("/").print(targetGenericsContext.getId()).print("   ");
        printTypeInferenceContext().printNewline();
    }

    @Override
    public void matchParam(int z)
    {
        printTab().print(z).print(". Match Param ").printNewline();
    }

    @Override
    public void paramInferenceFailed(int z)
    {
        printTab().print(z).print(". Failed processing").printNewline();
    }

    @Override
    public void reverseMatching()
    {
        printTab().print("Reverse matching (fill the missing type param from the instances):").printNewline();
    }

    @Override
    public void parameterInferenceSucceeded()
    {
        printTab().print("- Parameters inference succeeded, registering type parameters and multiplicity parameters: ");
        printTypeInferenceContext().printNewline();
    }

    @Override
    public void returnType(CoreInstance returnGenericType)
    {
        printTab().print("Return type '");
        GenericType.print(this.appendable, returnGenericType, this.processorSupport);
        print('\'').printNewline();
    }

    @Override
    public void returnTypeNotConcrete()
    {
        printTab().print("The return type is not concrete (and not in global scope) -> reverse matching").printNewline();
    }

    @Override
    public void reprocessingTheParameter()
    {
        printTab().print("Reprocessing the parameter").printNewline();
    }

    @Override
    public void finishedProcessParameter()
    {
        printTab().print("Finished reprocessing the parameter").printNewline();
    }

    @Override
    public void newReturnType(CoreInstance returnGenericType)
    {
        printTab().print("New return type '");
        GenericType.print(this.appendable, returnGenericType, this.processorSupport);
        print('\'').printNewline();
    }

    @Override
    public void finishedRegisteringParametersAndMultiplicities()
    {
        printTab().print("- Finished registering type parameters and multiplicity parameters.").printNewline();
    }

    @Override
    public void finishedProcessingFunctionExpression(CoreInstance functionExpression)
    {
        printTab().print("Finished processing: '").print(((FunctionExpression) functionExpression)._functionName());
        print("' ").printTypeInferenceContext().printNewline();
    }

//    @Override
//    public void matchUntypedLambnda(CoreInstance functionExpression, CoreInstance templateGenericType)
//    {
//        printTab();
//        print("Finished processing: '");
//        print(((FunctionExpression)functionExpression)._functionName());
//        print("' ");
//        printTypeInferenceContext();
//        printNewline();
//    }

    private PrintTypeInferenceObserver printTab()
    {
        for (int i = 0; i < this.tab; i++)
        {
            this.appendable.append(' ');
        }
        return this;
    }

    private PrintTypeInferenceObserver printNewline()
    {
        this.appendable.append(System.lineSeparator());
        return this;
    }

    private PrintTypeInferenceObserver print(String string)
    {
        this.appendable.append(string);
        return this;
    }

    private PrintTypeInferenceObserver print(char character)
    {
        this.appendable.append(character);
        return this;
    }

    private PrintTypeInferenceObserver print(boolean b)
    {
        this.appendable.append(b);
        return this;
    }

    private PrintTypeInferenceObserver print(int i)
    {
        this.appendable.append(i);
        return this;
    }

    private PrintTypeInferenceObserver printTypeInferenceContext()
    {
        TypeInferenceContext typeInferenceContext = this.processorState.getTypeInferenceContext();
        if (typeInferenceContext == null)
        {
            this.appendable.append("null");
        }
        else
        {
            typeInferenceContext.print(this.appendable);
        }
        return this;
    }
}
