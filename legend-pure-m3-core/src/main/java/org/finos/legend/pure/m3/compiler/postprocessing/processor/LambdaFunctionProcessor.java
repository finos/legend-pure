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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class LambdaFunctionProcessor implements MatchRunner<LambdaFunction>
{
    @Override
    public String getClassName()
    {
        return M3Paths.LambdaFunction;
    }

    @Override
    public void run(LambdaFunction lambda, MatcherState state, Matcher matcher, ModelRepository repository, Context context) throws PureCompilationException
    {
        process(lambda, state, matcher, repository);
    }

    public static void process(LambdaFunction lambda, MatcherState state, Matcher matcher, ModelRepository repository) throws PureCompilationException
    {
        if (TypeInference.canProcessLambda(lambda, (ProcessorState)state, state.getProcessorSupport()))
        {
            MutableSet<String> vars = Sets.mutable.empty();
            MutableSet<String> params = Sets.mutable.empty();
            processValueSpecification(lambda, params, vars, state.getProcessorSupport(), repository);
        }
    }

    private static void processValueSpecification(CoreInstance valueSpecification, MutableSet<String> params, MutableSet<String> vars, ProcessorSupport processorSupport, ModelRepository modelRepository)
    {
        if (valueSpecification instanceof FunctionExpression)
        {
            if (((FunctionExpression)valueSpecification)._functionName() != null && "letFunction".equals(((FunctionExpression)valueSpecification)._functionName()))
            {
                params.add(((InstanceValue)((FunctionExpression)valueSpecification)._parametersValues().toList().get(0))._valuesCoreInstance().toList().get(0).getName());
            }
            for (ValueSpecification param : ((FunctionExpression)valueSpecification)._parametersValues())
            {
                processValueSpecification(param, params, vars, processorSupport, modelRepository);
            }
        }
        else if (valueSpecification instanceof InstanceValue)
        {
            for (CoreInstance val : ((InstanceValue)valueSpecification)._valuesCoreInstance())
            {
                processValueSpecification(val, params, vars, processorSupport, modelRepository);
            }
        }
        else if (valueSpecification instanceof KeyExpression)
        {
            processValueSpecification(((KeyExpression)valueSpecification)._expression(), params, vars, processorSupport, modelRepository);
        }
        else if (valueSpecification instanceof LambdaFunction)
        {
            vars.addAllIterable(processLambda((LambdaFunction<CoreInstance>) valueSpecification, processorSupport, modelRepository));
        }
        else if (valueSpecification instanceof VariableExpression)
        {
            vars.add(((VariableExpression)valueSpecification)._name());
        }
    }

    private static RichIterable<String> processLambda(LambdaFunction<CoreInstance> lambda, ProcessorSupport processorSupport, ModelRepository repository)
    {
        if (lambda._openVariables().isEmpty())
        {
            MutableSet<String> vars = Sets.mutable.empty();

            // Variables from the Lambda definition
            FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(lambda);
            RichIterable<? extends VariableExpression> parameters = functionType._parameters();
            MutableSet<String> paramNames = UnifiedSet.newSet(parameters.size());
            for (VariableExpression parameter : parameters)
            {
                paramNames.add(parameter._name());
            }

            // Variables used in the Lambda
            for (ValueSpecification expression : lambda._expressionSequence())
            {
                processValueSpecification(expression, paramNames, vars, processorSupport, repository);
            }

            // Intersection
            MutableList<String> openVars = FastList.newList(vars.size());
            for (String var : vars)
            {
                if (!paramNames.contains(var))
                {
                    openVars.add(var);
                }
            }

            // Add to Lambda
            if (openVars.notEmpty())
            {
                //sort these so that they are deterministically ordered which is easier for graph validation tests
                lambda._openVariables(openVars.sortThis());
            }

            return openVars;
        }
        else
        {
            return (RichIterable<String>)lambda._openVariables();
        }
    }
}
