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
    private int tabCount;
    private final String tab;
    private final ProcessorState processorState;

    public PrintTypeInferenceObserver(Appendable appendable, String tab, ProcessorState processorState)
    {
        this.appendable = SafeAppendable.wrap(appendable);
        this.tab = tab;
        this.processorState = processorState;
    }

    public PrintTypeInferenceObserver(Appendable appendable, ProcessorState processorState)
    {
        this(appendable, "  ", processorState);
    }

    public PrintTypeInferenceObserver(ProcessorState processorState)
    {
        this(System.out, processorState);
    }

    @Deprecated
    public PrintTypeInferenceObserver(Appendable appendable, ProcessorSupport processorSupport, ProcessorState processorState)
    {
        this(appendable, processorState);
    }

    @Deprecated
    public PrintTypeInferenceObserver(ProcessorSupport processorSupport, ProcessorState processorState)
    {
        this(processorState);
    }

    @Override
    public TypeInferenceObserver resetTab()
    {
        this.tabCount = 0;
        return this;
    }

    @Override
    public TypeInferenceObserver shiftTab(int i)
    {
        this.tabCount = Math.max(0, this.tabCount + i);
        return this;
    }

    @Override
    public TypeInferenceObserver startProcessingFunction(CoreInstance functionDefinition, CoreInstance functionType)
    {
        printTab().print("Process function '").print(((FunctionDefinition<?>) functionDefinition)._name());
        SourceInformation sourceInfo = functionDefinition.getSourceInformation();
        if (sourceInfo != null)
        {
            sourceInfo.appendM4String(this.appendable);
        }
        print(" '(");
        FunctionType.print(this.appendable, functionType, this.processorState.getProcessorSupport());
        return print(')').printNewline();
    }

    @Override
    public TypeInferenceObserver startProcessingFunctionBody()
    {
        return printTab().print("Processing function body ").printTypeInferenceContext().printNewline();
    }

    @Override
    public TypeInferenceObserver finishedProcessingFunctionBody()
    {
        return printTab().print("Finished processing function body").printNewline();
    }

    @Override
    public TypeInferenceObserver finishedProcessingFunction(CoreInstance functionType)
    {
        printTab().print("Finished processing function / ");
        FunctionType.print(this.appendable, functionType, this.processorState.getProcessorSupport());
        return printNewline();
    }

    @Override
    public TypeInferenceObserver startProcessingFunctionExpression(CoreInstance functionExpression)
    {
        printTab().print("Process function expression for function: '").print(((FunctionExpression) functionExpression)._functionName());
        SourceInformation sourceInfo = functionExpression.getSourceInformation();
        if (sourceInfo != null)
        {
            sourceInfo.appendM4String(this.appendable);
        }
        return print("' ").printTypeInferenceContext().printNewline();
    }

    @Override
    public TypeInferenceObserver startFirstPassParametersProcessing()
    {
        return printTab().print("- First pass parameters processing:").printNewline();
    }

    @Override
    public TypeInferenceObserver processingParameter(CoreInstance functionExpression, int i, ValueSpecification value)
    {
        printTab().print("Process param: ").print(i + 1).print('/').print(((FunctionExpression) functionExpression)._parametersValues().size());
        if (value instanceof VariableExpression)
        {
            print(' ').print(((VariableExpression) value)._name());
        }
        SourceInformation sourceInfo = value.getSourceInformation();
        if (sourceInfo != null)
        {
            print(' ');
            sourceInfo.appendM4String(this.appendable);
        }
        return printNewline();
    }

    @Override
    public TypeInferenceObserver inferenceResult(boolean success)
    {
        return printTab().print("-> inference (success:").print(success).print(")").printNewline();
    }

    @Override
    public TypeInferenceObserver functionMatched(CoreInstance foundFunction, CoreInstance foundFunctionType)
    {
        printTab().print("- Function matched: name:'").print(foundFunction.getName()).print("'  signature:'");
        FunctionType.print(this.appendable, foundFunctionType, this.processorState.getProcessorSupport());
        return print('\'').printNewline();
    }

    @Override
    public TypeInferenceObserver firstPassInferenceFailed()
    {
        return printTab().print("- Parameters inference failed").printNewline();
    }

    @Override
    public TypeInferenceObserver matchTypeParamsFromFoundFunction(CoreInstance foundFunction)
    {
        return printTab().print("Matching type parameters and multiplicity parameters (from the found function '").print(foundFunction.getName()).print("')").printNewline();
    }

    @Override
    public TypeInferenceObserver register(CoreInstance templateGenType, CoreInstance valueForMetaPropertyToOne, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        printTab().print(". Register ");
        GenericType.print(this.appendable, templateGenType, this.processorState.getProcessorSupport());
        print(" / ");
        GenericType.print(this.appendable, valueForMetaPropertyToOne, this.processorState.getProcessorSupport());
        print(" in ").print(context.getId()).print("/").print(targetGenericsContext.getId()).print("   ");
        return printTypeInferenceContext().printNewline();
    }

    @Override
    public TypeInferenceObserver registerMul(CoreInstance templateMul, CoreInstance valueMul, TypeInferenceContext context, TypeInferenceContext targetGenericsContext)
    {
        printTab().print(". Register Mul ");
        Multiplicity.print(this.appendable, templateMul, true);
        print(" / ");
        Multiplicity.print(this.appendable, valueMul, true);
        print(" in ").print(context.getId()).print("/").print(targetGenericsContext.getId()).print("   ");
        return printTypeInferenceContext().printNewline();
    }

    @Override
    public TypeInferenceObserver matchParam(int i)
    {
        return printTab().print(i + 1).print(". Match Param ").printNewline();
    }

    @Override
    public TypeInferenceObserver paramInferenceFailed(int i)
    {
        return printTab().print(i + 1).print(". Failed processing").printNewline();
    }

    @Override
    public TypeInferenceObserver reverseMatching()
    {
        return printTab().print("Reverse matching (fill the missing type param from the instances):").printNewline();
    }

    @Override
    public TypeInferenceObserver parameterInferenceSucceeded()
    {
        printTab().print("- Parameters inference succeeded, registering type parameters and multiplicity parameters: ");
        return printTypeInferenceContext().printNewline();
    }

    @Override
    public TypeInferenceObserver returnType(CoreInstance returnGenericType)
    {
        printTab().print("Return type '");
        GenericType.print(this.appendable, returnGenericType, this.processorState.getProcessorSupport());
        return print('\'').printNewline();
    }

    @Override
    public TypeInferenceObserver returnTypeNotConcrete()
    {
        return printTab().print("The return type is not concrete (and not in global scope) -> reverse matching").printNewline();
    }

    @Override
    public TypeInferenceObserver reprocessingTheParameter()
    {
        return printTab().print("Reprocessing the parameter").printNewline();
    }

    @Override
    public TypeInferenceObserver finishedProcessParameter()
    {
        return printTab().print("Finished reprocessing the parameter").printNewline();
    }

    @Override
    public TypeInferenceObserver newReturnType(CoreInstance returnGenericType)
    {
        printTab().print("New return type '");
        GenericType.print(this.appendable, returnGenericType, this.processorState.getProcessorSupport());
        return print('\'').printNewline();
    }

    @Override
    public TypeInferenceObserver finishedRegisteringParametersAndMultiplicities()
    {
        return printTab().print("- Finished registering type parameters and multiplicity parameters.").printNewline();
    }

    @Override
    public TypeInferenceObserver finishedProcessingFunctionExpression(CoreInstance functionExpression)
    {
        printTab().print("Finished processing: '").print(((FunctionExpression) functionExpression)._functionName());
        return print("' ").printTypeInferenceContext().printNewline();
    }

//    @Override
//    public TypeInferenceObserver matchUntypedLambnda(CoreInstance functionExpression, CoreInstance templateGenericType)
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
        for (int i = 0; i < this.tabCount; i++)
        {
            this.appendable.append(this.tab);
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
