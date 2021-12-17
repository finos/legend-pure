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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningDatesPropagationFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class SimpleFunctionExpressionUnbind implements MatchRunner<SimpleFunctionExpression>
{
    @Override
    public String getClassName()
    {
        return M3Paths.SimpleFunctionExpression;
    }

    @Override
    public void run(SimpleFunctionExpression functionExpression, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        Function<?> function = (Function<?>) ImportStub.withImportStubByPass(functionExpression._funcCoreInstance(), processorSupport);
        if (function != null)
        {
            function._applicationsRemove(functionExpression);
            if (function._applications().isEmpty())
            {
                function._applicationsRemove();
            }
        }

        cleanAutoMapPropertyIfNecessary(functionExpression, state, matcher);
        MilestoningDatesPropagationFunctions.undoAutoGenMilestonedQualifier(functionExpression, modelRepository, processorSupport);

        functionExpression._funcRemove();
        functionExpression._resolvedTypeParametersRemove();
        functionExpression._resolvedMultiplicityParametersRemove();
        functionExpression._parametersValues().forEach(pv -> matcher.fullMatch(pv, state));
    }

    private static void cleanAutoMapPropertyIfNecessary(FunctionExpression functionExpression, MatcherState state, Matcher matcher)
    {
        ValueSpecification possiblePropertySfe = (ValueSpecification) Automap.getAutoMapExpressionSequence(functionExpression);
        if (possiblePropertySfe != null)
        {
            functionExpression._functionNameRemove();
            ListIterable<? extends ValueSpecification> params = functionExpression._parametersValues().toList();
            ValueSpecification firstParam = params.get(0);
            ValueSpecification secondParam = params.get(1);
            matcher.fullMatch(secondParam, state);

            RichIterable<? extends ValueSpecification> possiblePropertySfeParams = possiblePropertySfe instanceof FunctionExpression ?
                    ((FunctionExpression) possiblePropertySfe)._parametersValues() : Lists.fixedSize.empty(); //params may have changed after unbinding for the Lambda
            ((UnbindState) state).freeProcessedAndValidated(secondParam);

            GenericType secondParamGenericType = secondParam._genericType();
            if (secondParamGenericType != null)
            {
                ((UnbindState) state).freeValidated(secondParamGenericType);
            }

            functionExpression._parametersValuesRemove();

            MutableList<ValueSpecification> allVars = Lists.mutable.with(firstParam);
            //add back qualified properties params
            allVars.addAllIterable(possiblePropertySfeParams.toList().subList(1, possiblePropertySfeParams.size()));

            functionExpression._parametersValues(allVars);

            InstanceValue propertyName = possiblePropertySfe instanceof FunctionExpression ? ((FunctionExpression) possiblePropertySfe)._propertyName() : null;
            if (propertyName == null)
            {
                InstanceValue qualifiedPropertyName = possiblePropertySfe instanceof FunctionExpression ? ((FunctionExpression) possiblePropertySfe)._qualifiedPropertyName() : null;
                functionExpression._qualifiedPropertyName(qualifiedPropertyName);
            }
            else
            {
                functionExpression._propertyName(propertyName);
            }
        }
    }
}