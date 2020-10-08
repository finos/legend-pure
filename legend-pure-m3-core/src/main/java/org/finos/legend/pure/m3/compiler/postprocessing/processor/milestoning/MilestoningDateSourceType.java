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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.NativeFunctionIdentifier;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

enum MilestoningDateSourceType implements MilestoningDateSource{

    VariableExpression{
        @Override
        public boolean isDataSourceType(final CoreInstance fe, ModelRepository repository, final ProcessorSupport processorSupport) {
            return fe instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
        }

        @Override
        public MilestoningDates getMilestonedDates(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport) {
            String lambdaInputAlias = fe instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression ? ((VariableExpression)fe)._name() : null;
            return state.getMilestoningDates(lambdaInputAlias);
        }
    },

    MilestonedQualifiedPropertyWithDateParam{
        @Override
        public boolean isDataSourceType(CoreInstance fe, ModelRepository repository, ProcessorSupport processorSupport) {
            CoreInstance func = fe instanceof FunctionExpression ? ((FunctionExpression)fe)._funcCoreInstance() : null;
            return func != null && MilestoningFunctions.isGeneratedQualifiedPropertyWithWithAllMilestoningDatesSpecified(func, processorSupport);
        }

        @Override
        public MilestoningDates getMilestonedDates(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport) {
            return MilestoningDatesPropagationFunctions.getMilestonedDates(fe, processorSupport);
        }
    },

    MilestonedGetAll{
        @Override
        public boolean isDataSourceType(CoreInstance fe, ModelRepository repository, ProcessorSupport processorSupport) {
            CoreInstance inputFunction = fe instanceof FunctionExpression ? ((FunctionExpression)fe)._funcCoreInstance() : null;
            return inputFunction != null && (NativeFunctionIdentifier.MilestonedAllSingleDate.ofType(inputFunction) || NativeFunctionIdentifier.MilestonedAllBiTemporal.ofType(inputFunction));
        }

        @Override
        public MilestoningDates getMilestonedDates(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport) {
            ValueSpecification parameterValue = fe instanceof FunctionExpression ? ((FunctionExpression)fe)._parametersValues().getFirst() : null;
            CoreInstance propertyReturnType = parameterValue instanceof InstanceValue ? ImportStub.withImportStubByPass(((InstanceValue)parameterValue)._valuesCoreInstance().getFirst(), processorSupport) : null;
            MilestoningStereotype milestoningStereotype = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(propertyReturnType, processorSupport).getFirst();
            return new MilestoningDates(milestoningStereotype, ListHelper.<ValueSpecification>tail(fe instanceof FunctionExpression ? (RichIterable<ValueSpecification>)((FunctionExpression)fe)._parametersValues() : Lists.immutable.<ValueSpecification>empty()).toList());
        }
    },

    AutoMap{
        @Override
        public boolean isDataSourceType(final CoreInstance fe, ModelRepository repository, final ProcessorSupport processorSupport) {
            return  Automap.getAutoMapExpressionSequence(fe) != null;
        }

        @Override
        public MilestoningDates getMilestonedDates(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport) {
            return getAutoMapMilestonedPropertyQualifiedDates(fe, repository, processorSupport);
        }

        private MilestoningDates getAutoMapMilestonedPropertyQualifiedDates(CoreInstance functionExpression, ModelRepository repository, ProcessorSupport processorSupport){
            MilestoningDates milestoningDates = null;
            CoreInstance exprSeq = Automap.getAutoMapExpressionSequence(functionExpression);
            if(exprSeq != null) {
                CoreInstance func = exprSeq instanceof FunctionExpression ? ((FunctionExpression)exprSeq)._funcCoreInstance() : null;
                if (func != null && MilestoningFunctions.isGeneratedQualifiedPropertyWithWithAllMilestoningDatesSpecified(func, processorSupport)) {
                    milestoningDates = MilestoningDatesPropagationFunctions.getMilestonedDates(exprSeq, processorSupport);
                }
            }
            return milestoningDates;
        }
    },

    Filter{
        @Override
        public boolean isDataSourceType(final CoreInstance fe, ModelRepository repository, final ProcessorSupport processorSupport) {
            CoreInstance func = fe instanceof FunctionExpression ? ((FunctionExpression)fe)._funcCoreInstance() : null;
            return func != null && NativeFunctionIdentifier.Filter.ofType(func);
        }

        @Override
        public MilestoningDates getMilestonedDates(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport) {
            return MilestoningDatesPropagationFunctions.getMilestoningDatesForValidMilestoningDataSourceTypes(fe instanceof FunctionExpression ? ((FunctionExpression)fe)._parametersValues().getFirst() : null, state, repository, context, processorSupport);
        }
    },

    SubType{
        @Override
        public boolean isDataSourceType(final CoreInstance fe, ModelRepository repository, final ProcessorSupport processorSupport) {
            CoreInstance func = fe instanceof FunctionExpression ? ((FunctionExpression)fe)._funcCoreInstance() : null;;
            return func != null && NativeFunctionIdentifier.SubType.ofType(func);
        }

        @Override
        public MilestoningDates getMilestonedDates(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport) {
            return MilestoningDatesPropagationFunctions.getMilestoningDatesForValidMilestoningDataSourceTypes(fe instanceof FunctionExpression ? ((FunctionExpression)fe)._parametersValues().getFirst() : null, state, repository, context, processorSupport);
        }
    };
}
