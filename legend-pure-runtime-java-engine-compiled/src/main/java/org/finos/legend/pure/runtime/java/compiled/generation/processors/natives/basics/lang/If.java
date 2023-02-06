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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.lang;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class If extends AbstractNative
{
    public If()
    {
        super("if_Boolean_1__Function_1__Function_1__T_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        if (!transformedParams.isEmpty())
        {
            throw new IllegalArgumentException("transformed parameters must be empty");
        }

        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        CoreInstance exprGT = functionExpression.getValueForMetaPropertyToOne(M3Properties.genericType);
        String type = TypeProcessor.typeToJavaObjectSingle(exprGT, true, processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);
        boolean returnToMany = !Multiplicity.isToZeroOrOne(multiplicity);
        boolean returnZeroToOne = Multiplicity.isZeroToOne(multiplicity);
        String test = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);

        return "((" + TypeProcessor.typeToJavaObjectWithMul(exprGT, multiplicity, true, processorSupport) + ")" + "(" + test + "?" +
                processIfExpression(topLevelElement, parametersValues, 1, type, returnToMany, returnZeroToOne, processorContext)
                + ":" +
                processIfExpression(topLevelElement, parametersValues, 2, type, returnToMany, returnZeroToOne, processorContext) + "))";
    }

    private static CoreInstance byPass(CoreInstance c, ProcessorSupport processorSupport)
    {
        return Instance.instanceOf(c, M3Paths.RoutedValueSpecification, processorSupport) ? byPass(c.getValueForMetaPropertyToOne(M3Properties.value), processorSupport) : c;
    }

    private static String processIfExpression(CoreInstance topLevelElement, ListIterable<? extends CoreInstance> parametersValues, int offset, String type, boolean returnToMany, boolean returnZeroToOne, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();

        CoreInstance lambda = byPass(parametersValues.get(offset), processorSupport);

        ListIterable<? extends CoreInstance> expressionSequence = Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(lambda, M3Properties.values, processorSupport), M3Properties.expressionSequence, processorContext.getSupport());

        if (expressionSequence.size() == 1)
        {
            CoreInstance lambdaFunction = Instance.getValueForMetaPropertyToOneResolved(lambda, M3Properties.values, processorSupport);
            CoreInstance functionType = processorSupport.function_getFunctionType(lambdaFunction);
            CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
            boolean isToZero = Multiplicity.isToZero(returnMultiplicity);
            String value = FunctionProcessor.processFunctionDefinitionContent(topLevelElement, lambdaFunction, false, processorContext, processorContext.getSupport());

            boolean shouldCast = (returnZeroToOne || "null".equals(value) || ("java.lang.Number".equals(type) && !returnToMany)) && !"java.lang.Object".equals(type);
            //Handle bug with Nil[0] which creates a List so we have to unwrap the List
            value = (isToZero && returnZeroToOne) ? "CompiledSupport.makeOne(" + value + ")" : value;
            String result = shouldCast ? "(" + type + ")" + value : value;
            if (returnToMany)
            {
                return "CompiledSupport.toPureCollection(" + result + ")";
            }
            else
            {
                return result;
            }
        }
        else
        {
            return (returnToMany ? "CompiledSupport.toPureCollection(" : "") + lambdaZero(lambda, FunctionProcessor.processFunctionDefinitionContent(topLevelElement, Instance.getValueForMetaPropertyToOneResolved(lambda, M3Properties.values, processorSupport), true, processorContext, processorContext.getSupport()), processorContext.getSupport()) + ".execute()" + (returnToMany ? ")" : "");
        }
    }

    // TODO fix this to handle let expressions
    private static String lambdaZero(CoreInstance a, String val, ProcessorSupport processorSupport)
    {
        CoreInstance last = a.getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToMany(M3Properties.expressionSequence).getLast();
        CoreInstance returnType = last.getValueForMetaPropertyToOne(M3Properties.genericType);
        CoreInstance multiplicity = last.getValueForMetaPropertyToOne(M3Properties.multiplicity);
        //CoreInstance fType = org.finos.legend.pure.m3.bootstrap.type.function.processorSupport.function_getFunctionType(a.getValueForMetaPropertyToOne(M3Properties.values), context);
        String type = TypeProcessor.typeToJavaObjectWithMul(returnType, multiplicity, processorSupport);
        return "new LambdaZero<" + type + ">(){public " + type + " execute(){ " + val + "}}";
    }


    //If function has to handle its own parameters within the build method. So we just return empty here.
    @Override
    public ListIterable<String> transformParameterValues(ListIterable<? extends CoreInstance> parametersValues, CoreInstance topLevelElement, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        return Lists.immutable.empty();
    }

    @Override
    public String buildBody()
    {

        return "new DefendedPureFunction3<Boolean, " + FullJavaPaths.Function + ", " + FullJavaPaths.Function + ", Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Boolean condition, " + FullJavaPaths.Function + " truth, " + FullJavaPaths.Function + " falsy, ExecutionSupport es)\n" +
                "            {\n" +
                "                return condition ? CoreGen.evaluate(es, truth, Lists.mutable.empty()) : CoreGen.evaluate(es, falsy, Lists.mutable.empty());\n" +
                "            }\n" +
                "        }";
    }
}
