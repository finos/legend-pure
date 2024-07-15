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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.treepath;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.ExistingPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.PropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNodePropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RootRouteNodeValidator implements MatchRunner<RootRouteNode>
{
    @Override
    public String getClassName()
    {
        return M3Paths.RootRouteNode;
    }

    @Override
    public void run(RootRouteNode treePathNode, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        GenericType genericType = treePathNode._type();
        Validator.validate(genericType, (ValidatorState)state, matcher, processorSupport);
        MutableSet<String> nodesValidated = Sets.mutable.empty();
        validateTreePathNode(treePathNode, nodesValidated, processorSupport);
    }

    private void validateTreePathNode(RouteNode treePathNode, MutableSet<String> nodesValidated, ProcessorSupport processorSupport)
    {
        if (!nodesValidated.contains(treePathNode._name()))
        {
            nodesValidated.add(treePathNode._name());
            for (RouteNodePropertyStub propertyStub : treePathNode._included())
            {
                validatePropertyStub(propertyStub, processorSupport);
            }

            for (RouteNodePropertyStub propertyStub : treePathNode._excluded())
            {
                validatePropertyStub(propertyStub, processorSupport);
            }

            if (treePathNode instanceof ExistingPropertyRouteNode)
            {
                validatePropertyStub(((ExistingPropertyRouteNode)treePathNode)._property(), processorSupport);
            }

            for (PropertyRouteNode childNode : treePathNode._children())
            {
                validateTreePathNode(childNode, nodesValidated, processorSupport);
            }
        }
    }

    private void validatePropertyStub(RouteNodePropertyStub routeNodePropertyStub, ProcessorSupport processorSupport)
    {
        AbstractProperty property = (AbstractProperty)ImportStub.withImportStubByPass(routeNodePropertyStub._propertyCoreInstance().getFirst(), processorSupport);
        RichIterable<? extends VariableExpression> valueSpecifications = ((FunctionType)processorSupport.function_getFunctionType(property))._parameters();
        ListIterable<? extends VariableExpression> parameterSpecifications = valueSpecifications.toList().subList(1, valueSpecifications.size());

        ListIterable<? extends InstanceValue> stubParameters = routeNodePropertyStub._parameters().toList();

        if (parameterSpecifications.size() != stubParameters.size())
        {
            throw new PureCompilationException(routeNodePropertyStub.getSourceInformation(), "Error finding match for function '" + property._functionName() + "'. Incorrect number of parameters, function expects " + parameterSpecifications.size() + " parameters");
        }

        for (int i = 0; i < parameterSpecifications.size(); i++)
        {
            VariableExpression valueSpecification = parameterSpecifications.get(i);

            InstanceValue stubParameter = stubParameters.get(i);

            GenericType stubParameterGenericType = stubParameter._genericType();

            GenericType propertyParameterGenericType = valueSpecification._genericType();

            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.genericTypesEqual(stubParameterGenericType, propertyParameterGenericType, processorSupport))
            {
                throw new PureCompilationException(routeNodePropertyStub.getSourceInformation(), "Parameter type mismatch for function '" + property._functionName()
                        + "'. Expected:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyParameterGenericType, processorSupport)
                        + ", Found:" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(stubParameterGenericType, processorSupport));
            }

            Multiplicity stubParamMultiplicity = stubParameter._multiplicity();
            Multiplicity propertyParamMultiplicity = valueSpecification._multiplicity();
            if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.multiplicitiesEqual(propertyParamMultiplicity, stubParamMultiplicity))
            {
                throw new PureCompilationException(routeNodePropertyStub.getSourceInformation(), "Parameter multiplicity mismatch for function '" + property._functionName()
                        + "'. Expected:" + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(propertyParamMultiplicity)
                        + ", Found:" + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(stubParamMultiplicity));
            }
        }
    }
}
