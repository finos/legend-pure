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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.GroupByFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AggregationAwareUnbind implements MatchRunner<AggregationAwareSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.AggregationAwareSetImplementation;
    }

    @Override
    public void run(AggregationAwareSetImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        Shared.cleanImportStub(instance._classCoreInstance(), processorSupport);
        Shared.cleanImportStub(instance._parentCoreInstance(), processorSupport);

        for (AggregateSetImplementationContainer container : instance._aggregateSetImplementations())
        {
            SetImplementation setImplementation = container._setImplementation();
            Shared.cleanImportStub(setImplementation._classCoreInstance(), processorSupport);
            Shared.cleanImportStub(setImplementation._parentCoreInstance(), processorSupport);

            for (GroupByFunctionSpecification groupByFunctionSpecification : container._aggregateSpecification()._groupByFunctions())
            {
                matcher.fullMatch(groupByFunctionSpecification._groupByFn(), state);
                FunctionType functionType = (FunctionType)ImportStub.withImportStubByPass(groupByFunctionSpecification._groupByFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
                functionType._parametersRemove();
                functionType._functionRemove();
            }

            for (AggregationFunctionSpecification aggregationFunctionSpecification : container._aggregateSpecification()._aggregateValues())
            {
                matcher.fullMatch(aggregationFunctionSpecification._mapFn(), state);
                matcher.fullMatch(aggregationFunctionSpecification._aggregateFn(), state);
                FunctionType functionType = (FunctionType)ImportStub.withImportStubByPass(aggregationFunctionSpecification._mapFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
                functionType._parametersRemove();
                functionType._functionRemove();
                functionType = (FunctionType)ImportStub.withImportStubByPass(aggregationFunctionSpecification._aggregateFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
                functionType._parametersRemove();
                functionType._functionRemove();
            }

            matcher.fullMatch(setImplementation, state);
            ((InstanceSetImplementation)setImplementation)._aggregateSpecificationRemove();
        }

        Shared.cleanImportStub(instance._mainSetImplementation()._classCoreInstance(), processorSupport);
        Shared.cleanImportStub(instance._mainSetImplementation()._parentCoreInstance(), processorSupport);
        matcher.fullMatch(instance._mainSetImplementation(), state);
        instance._propertyMappingsRemove();
    }
}
