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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AggregationAwareValidator implements MatchRunner<AggregationAwareSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.AggregationAwareSetImplementation;
    }

    @Override
    public void run(AggregationAwareSetImplementation instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        for (AggregateSetImplementationContainer container : instance._aggregateSetImplementations())
        {
            for (AggregationFunctionSpecification aggregationFunctionSpecification : container._aggregateSpecification()._aggregateValues())
            {
                FunctionType mapFnType = (FunctionType)ImportStub.withImportStubByPass(aggregationFunctionSpecification._mapFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
                Type mapFnReturnType = (Type)ImportStub.withImportStubByPass(mapFnType._returnType()._rawTypeCoreInstance(), processorSupport);
                if (!Instance.instanceOf(mapFnReturnType, M3Paths.DataType, processorSupport))
                {
                    throw new PureCompilationException(aggregationFunctionSpecification._mapFn().getSourceInformation(), "An aggregate specification's mapFunction return type should be a DataType (primitive type/enumeration)");
                }

                FunctionType aggregateFnType = (FunctionType)ImportStub.withImportStubByPass(aggregationFunctionSpecification._aggregateFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
                Type aggregateFnReturnType = (Type)ImportStub.withImportStubByPass(aggregateFnType._returnType()._rawTypeCoreInstance(), processorSupport);
                if (!Instance.instanceOf(aggregateFnReturnType, M3Paths.DataType, processorSupport))
                {
                    throw new PureCompilationException(aggregationFunctionSpecification._aggregateFn().getSourceInformation(), "An aggregate specification's aggregateFunction return type should be a DataType (primitive type/enumeration)");
                }
            }
            matcher.fullMatch(container._setImplementation(), state);
        }
        matcher.fullMatch(instance._mainSetImplementation(), state);
    }
}
