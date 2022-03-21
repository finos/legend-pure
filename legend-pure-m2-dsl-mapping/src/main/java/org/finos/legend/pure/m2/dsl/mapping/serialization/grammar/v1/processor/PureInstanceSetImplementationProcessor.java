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
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class PureInstanceSetImplementationProcessor extends Processor<PureInstanceSetImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.PureInstanceSetImplementation;
    }

    @Override
    public void process(PureInstanceSetImplementation classMapping, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        Type srcClass = (Type) ImportStub.withImportStubByPass(classMapping._srcClassCoreInstance(), processorSupport);
        SourceInformation srcGenericTypeSourceInfo = null;

        LambdaFunction<?> filter = classMapping._filter();
        if (filter != null)
        {
            state.pushVariableContext();
            if (srcClass != null)
            {
                FunctionType fType = getFunctionType(filter, processorSupport);
                fType._parameters(Lists.mutable.<VariableExpression>withAll(fType._parameters()).with(createSrcParameter(srcClass, srcGenericTypeSourceInfo, filter.getSourceInformation(), processorSupport)));
            }
            matcher.fullMatch(filter, state);
            state.popVariableContext();
        }

        int i = 0;
        for (PropertyMapping propertyMapping : classMapping._propertyMappings())
        {
            state.pushVariableContext();

            if (((PurePropertyMapping) propertyMapping)._transformerCoreInstance() != null)
            {
                GrammarInfoStub transformerStub = (GrammarInfoStub) ((PurePropertyMapping) propertyMapping)._transformerCoreInstance();
                EnumerationMappingProcessor.processsEnumerationTransformer(transformerStub, propertyMapping, processorSupport);
            }

            LambdaFunction<?> transform = ((PurePropertyMapping) propertyMapping)._transform();
            if (srcClass != null)
            {
                FunctionType fType = getFunctionType(transform, processorSupport);
                fType._parameters(Lists.mutable.<VariableExpression>withAll(fType._parameters()).with(createSrcParameter(srcClass, srcGenericTypeSourceInfo, propertyMapping.getSourceInformation(), processorSupport)));
            }
            matcher.fullMatch(transform, state);

            ValueSpecification expressionSequence = transform._expressionSequence().getFirst();
            if (expressionSequence != null)
            {
                PropertyMappingValueSpecificationContext usageContext = (PropertyMappingValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M2MappingPaths.PropertyMappingValueSpecificationContext);
                usageContext._offset(i);
                usageContext._propertyMapping(propertyMapping);
                expressionSequence._usageContext(usageContext);
            }
            i++;

            state.popVariableContext();
        }
    }

    @Override
    public void populateReferenceUsages(PureInstanceSetImplementation classMapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.possiblyAddReferenceUsageForToOneProperty(classMapping, classMapping._srcClassCoreInstance(), M2MappingProperties.srcClass, repository, processorSupport);
    }

    private VariableExpression createSrcParameter(Type srcClass, SourceInformation genericTypeSourceInfo, SourceInformation parameterSourceInformation, ProcessorSupport processorSupport)
    {
        GenericType genericType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(srcClass, genericTypeSourceInfo, processorSupport);
        VariableExpression srcParam = (VariableExpression) processorSupport.newAnonymousCoreInstance(parameterSourceInformation, M3Paths.VariableExpression);
        srcParam._name("src");
        srcParam._genericType(genericType);
        srcParam._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));
        return srcParam;
    }

    private FunctionType getFunctionType(LambdaFunction<?> lambdaFunction, ProcessorSupport processorSupport)
    {
        GenericType typeArgument = lambdaFunction._classifierGenericType()._typeArguments().getAny();
        return (FunctionType) ImportStub.withImportStubByPass(typeArgument._rawTypeCoreInstance(), processorSupport);
    }
}
