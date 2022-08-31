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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState.VariableContextScope;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSpecificationValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwarePropertyMappingInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.GroupByFunctionSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionInstance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AggregationAwareProcessor extends Processor<AggregationAwareSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.AggregationAwareSetImplementation;
    }

    @Override
    public void process(AggregationAwareSetImplementation instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        PropertyOwner _class = (PropertyOwner) ImportStub.withImportStubByPass(instance._classCoreInstance(), processorSupport);
        if (!(_class instanceof Class))
        {
            throw new PureCompilationException(instance.getSourceInformation(), "AggregationAware mappings are allowed only for class mappings");
        }

        SetImplementationProcessor.ensureSetImplementationHasId(instance, repository, processorSupport);

        for (AggregateSetImplementationContainer container : instance._aggregateSetImplementations())
        {
            InstanceSetImplementation setImplementation = container._setImplementation();
            setImplementation._id(instance._id() + "_Aggregate_" + container._index());
            processAggregateSpecification(container._aggregateSpecification(), setImplementation, (Class<?>) _class, state, matcher, repository, processorSupport);
            setImplementation._aggregateSpecification(container._aggregateSpecification());
            PostProcessor.processElement(matcher, setImplementation, state, processorSupport);
        }

        InstanceSetImplementation mainSetImplementation = instance._mainSetImplementation();
        mainSetImplementation._id(instance._id() + "_Main");
        PostProcessor.processElement(matcher, mainSetImplementation, state, processorSupport);

        MutableList<PropertyMapping> newPropertyMappings = mainSetImplementation._propertyMappings().collect(propertyMapping ->
                AggregationAwarePropertyMappingInstance.createPersistent(repository, null, propertyMapping._sourceSetImplementationId(), propertyMapping._targetSetImplementationId())
                        ._propertyCoreInstance(propertyMapping._propertyCoreInstance())
                        ._owner(instance),
                Lists.mutable.empty());
        instance._propertyMappings(newPropertyMappings);
    }

    @Override
    public void populateReferenceUsages(AggregationAwareSetImplementation instance, ModelRepository repository, ProcessorSupport processorSupport)
    {

    }

    private void processAggregateSpecification(AggregateSpecification aggregateSpecification, InstanceSetImplementation setImplementation, Class<?> _class, ProcessorState state, Matcher matcher, ModelRepository repository, ProcessorSupport processorSupport)
    {
        int i = 0;
        for (GroupByFunctionSpecification groupByFunctionSpecification : aggregateSpecification._groupByFunctions())
        {
            try (VariableContextScope ignore = state.withNewVariableContext())
            {
                VariableExpression thisParam = VariableExpressionInstance.createPersistent(repository, aggregateSpecification.getSourceInformation(), (GenericType) Type.wrapGenericType(_class, processorSupport), (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne), "this");
                FunctionType functionType = (FunctionType) ImportStub.withImportStubByPass(groupByFunctionSpecification._groupByFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
                functionType._parameters(Lists.mutable.<VariableExpression>withAll(functionType._parameters()).with(thisParam));
                matcher.fullMatch(groupByFunctionSpecification._groupByFn(), state);

                ValueSpecification groupByFnExpressionSequence = groupByFunctionSpecification._groupByFn()._expressionSequence().toList().getFirst();
                this.addAggregateSpecificationUsageContext(groupByFnExpressionSequence, setImplementation, i, processorSupport);
                i++;
            }
        }

        for (AggregationFunctionSpecification aggregationFunctionSpecification : aggregateSpecification._aggregateValues())
        {
            VariableExpression thisParam = VariableExpressionInstance.createPersistent(repository, aggregateSpecification.getSourceInformation(), (GenericType) Type.wrapGenericType(_class, processorSupport), (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne), "this");
            FunctionType mapFnType = (FunctionType) ImportStub.withImportStubByPass(aggregationFunctionSpecification._mapFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
            mapFnType._parameters(Lists.mutable.<VariableExpression>withAll(mapFnType._parameters()).with(thisParam));
            try (VariableContextScope ignore = state.withNewVariableContext())
            {
                matcher.fullMatch(aggregationFunctionSpecification._mapFn(), state);

                ValueSpecification mapFnExpressionSequence = aggregationFunctionSpecification._mapFn()._expressionSequence().toList().getFirst();
                this.addAggregateSpecificationUsageContext(mapFnExpressionSequence, setImplementation, i, processorSupport);
                i++;
            }

            VariableExpression mappedParam = VariableExpressionInstance.createPersistent(repository, aggregateSpecification.getSourceInformation(), mapFnType._returnType(), (Multiplicity) processorSupport.package_getByUserPath(M3Paths.ZeroMany), "mapped");
            FunctionType aggregateFnType = (FunctionType) ImportStub.withImportStubByPass(aggregationFunctionSpecification._aggregateFn()._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
            aggregateFnType._parameters(Lists.mutable.<VariableExpression>withAll(aggregateFnType._parameters()).with(mappedParam));
            try (VariableContextScope ignore = state.withNewVariableContext())
            {
                matcher.fullMatch(aggregationFunctionSpecification._aggregateFn(), state);

                ValueSpecification aggregateFnExpressionSequence = aggregationFunctionSpecification._aggregateFn()._expressionSequence().toList().getFirst();
                this.addAggregateSpecificationUsageContext(aggregateFnExpressionSequence, setImplementation, i, processorSupport);
                i++;
            }
        }
    }

    private void addAggregateSpecificationUsageContext(ValueSpecification expressionSequence, InstanceSetImplementation setImplementation, int offset, ProcessorSupport processorSupport)
    {
        if (expressionSequence != null)
        {
            AggregateSpecificationValueSpecificationContext usageContext = (AggregateSpecificationValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M2MappingPaths.AggregateSpecificationValueSpecificationContext);
            usageContext._offset(offset);
            usageContext._aggregateSetImplementation(setImplementation);
            expressionSequence._usageContext(usageContext);
        }
    }
}
