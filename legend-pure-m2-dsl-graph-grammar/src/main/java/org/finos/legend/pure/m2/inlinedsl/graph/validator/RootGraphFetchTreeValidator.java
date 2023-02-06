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

package org.finos.legend.pure.m2.inlinedsl.graph.validator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.inlinedsl.graph.M2GraphPaths;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.PropertyGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
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

public class RootGraphFetchTreeValidator implements MatchRunner<RootGraphFetchTree>
{
    @Override
    public String getClassName()
    {
        return M2GraphPaths.RootGraphFetchTree;
    }

    @Override
    public void run(RootGraphFetchTree instance, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        final ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        Function<CoreInstance, CoreInstance> extractGenericTypeFunction = new Function<CoreInstance, CoreInstance>()
        {
            @Override
            public CoreInstance valueOf(CoreInstance instance)
            {
                return Instance.instanceOf(instance, M3Paths.ValueSpecification, processorSupport)
                        ? Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, processorSupport)
                        : Instance.extractGenericTypeFromInstance(instance, processorSupport);
            }
        };

        for (GraphFetchTree child : instance._subTrees())
        {
            this.validatePropertyGraphFetchTrees((PropertyGraphFetchTree) child, processorSupport, extractGenericTypeFunction);
        }
    }

    private void validatePropertyGraphFetchTrees(PropertyGraphFetchTree propertyGraphFetchTree, ProcessorSupport processorSupport, Function<CoreInstance, CoreInstance> extractGenericTypeFunction)
    {
        AbstractProperty property = (AbstractProperty)ImportStub.withImportStubByPass(propertyGraphFetchTree._propertyCoreInstance(), processorSupport);
        RichIterable<? extends VariableExpression> valueSpecifications = ((FunctionType)processorSupport.function_getFunctionType(property))._parameters();
        ListIterable<? extends VariableExpression> parameterSpecifications = valueSpecifications.toList().subList(1, valueSpecifications.size());
        ListIterable<? extends ValueSpecification> parameters = propertyGraphFetchTree._parameters().toList();

        if (parameterSpecifications.size() != parameters.size())
        {
            throw new PureCompilationException(propertyGraphFetchTree.getSourceInformation(), "Error finding match for property '" + property._name() + "'. Incorrect number of parameters, function expects " + parameterSpecifications.size() + " parameters");
        }

        int i = 0;
        for (VariableExpression valueSpecification : parameterSpecifications)
        {
            ValueSpecification parameter = parameters.get(i);

            if (parameter instanceof InstanceValue)
            {
                ListIterable<? extends CoreInstance> values = ImportStub.withImportStubByPasses(((InstanceValue)parameter)._valuesCoreInstance().toList(), processorSupport);
                GenericType genericTypeSpecified = valueSpecification._genericType();

                CoreInstance type = values.size() == 1 ?
                        extractGenericTypeFunction.valueOf(values.getFirst()) :
                        org.finos.legend.pure.m3.navigation.generictype.GenericType.findBestCommonGenericType(values.collect(extractGenericTypeFunction), true, false, processorSupport);

                if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.subTypeOf(type, genericTypeSpecified, processorSupport))
                {
                    throw new PureCompilationException(propertyGraphFetchTree.getSourceInformation(), "Parameter type mismatch for property '" + property._functionName()
                            + "'. Expected:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(genericTypeSpecified, processorSupport)
                            + ", Found:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(type, processorSupport));
                }
            }
            i++;
        }

        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(property);
        CoreInstance returnType = ImportStub.withImportStubByPass(functionType._returnType()._rawTypeCoreInstance(), processorSupport);
        CoreInstance subTypeClass = ImportStub.withImportStubByPass(propertyGraphFetchTree._subTypeCoreInstance(), processorSupport);
        if (subTypeClass != null)
        {
            if(!Type.subTypeOf(subTypeClass, returnType, processorSupport))
            {
                throw new PureCompilationException(propertyGraphFetchTree._subTypeCoreInstance().getSourceInformation(),  "The type "+ subTypeClass.getName() + " is not compatible with " + returnType.getName());
            }
        }

        for (GraphFetchTree child : propertyGraphFetchTree._subTrees())
        {
            this.validatePropertyGraphFetchTrees((PropertyGraphFetchTree) child, processorSupport, extractGenericTypeFunction);
        }
    }
}
