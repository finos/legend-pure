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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.Consumer;

public class LambdaFunctionProcessor extends Processor<LambdaFunction<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.LambdaFunction;
    }

    @Override
    public void process(LambdaFunction<?> lambda, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        process(lambda, state, matcher, repository);
    }

    @Override
    public void populateReferenceUsages(LambdaFunction<?> instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
    }

    public static void process(LambdaFunction<?> lambda, ProcessorState state, Matcher matcher, ModelRepository repository)
    {
        process(lambda, state);
    }

    public static void process(LambdaFunction<?> lambda, ProcessorState state)
    {
        if (TypeInference.canProcessLambda(lambda, state, state.getProcessorSupport()))
        {
            processLambda(lambda, state.getProcessorSupport());
        }
    }

    private static RichIterable<? extends String> processLambda(LambdaFunction<?> lambda, ProcessorSupport processorSupport)
    {
        RichIterable<? extends String> currentOpenVars = lambda._openVariables();
        if (currentOpenVars.isEmpty())
        {
            // Variables from the Lambda definition
            FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(lambda);
            MutableSet<String> declaredVars = functionType._parameters().collect(VariableExpression::_name, Sets.mutable.empty());

            // Variables used in the Lambda
            MutableSet<String> otherVars = Sets.mutable.empty();
            lambda._expressionSequence().forEach(expression -> processValue(expression, declaredVars::add, otherVars::add, processorSupport));

            // Compute open variables
            otherVars.removeAll(declaredVars);
            if (otherVars.notEmpty())
            {
                // Add to Lambda (sort these so that they are deterministically ordered which is easier for graph validation tests)
                MutableList<String> openVars = otherVars.toSortedList();
                lambda._openVariables(openVars);
                return openVars;
            }
        }
        return currentOpenVars;
    }

    private static void processValue(CoreInstance valueSpecification, Consumer<String> declaredVarConsumer, Consumer<String> otherVarConsumer, ProcessorSupport processorSupport)
    {
        if (valueSpecification instanceof FunctionExpression)
        {
            FunctionExpression functionExpression = (FunctionExpression) valueSpecification;
            if ("letFunction".equals(functionExpression._functionName()))
            {
                InstanceValue firstParamValue = (InstanceValue) ListHelper.wrapListIterable(functionExpression._parametersValues()).get(0);
                String letVarName = PrimitiveUtilities.getStringValue(ListHelper.wrapListIterable(firstParamValue._valuesCoreInstance()).get(0));
                declaredVarConsumer.accept(letVarName);
            }
            functionExpression._parametersValues().forEach(p -> processValue(p, declaredVarConsumer, otherVarConsumer, processorSupport));
        }
        else if (valueSpecification instanceof InstanceValue)
        {
            ((InstanceValue) valueSpecification)._valuesCoreInstance().forEach(v -> processValue(v, declaredVarConsumer, otherVarConsumer, processorSupport));
        }
        else if (valueSpecification instanceof KeyExpression)
        {
            processValue(((KeyExpression) valueSpecification)._expression(), declaredVarConsumer, otherVarConsumer, processorSupport);
        }
        else if (valueSpecification instanceof LambdaFunction)
        {
            processLambda((LambdaFunction<?>) valueSpecification, processorSupport).forEach(otherVarConsumer);
        }
        else if (valueSpecification instanceof VariableExpression)
        {
            otherVarConsumer.accept(((VariableExpression) valueSpecification)._name());
        }
    }
}
