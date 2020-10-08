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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.ExistingPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.PropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNodePropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RootRouteNodeUnbind implements MatchRunner<RootRouteNode>
{
    @Override
    public String getClassName()
    {
        return M3Paths.RootRouteNode;
    }

    @Override
    public void run(RootRouteNode treeRoot, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        GenericType rootGenericType = treeRoot._type();
        Shared.cleanUpGenericType(rootGenericType, (UnbindState)state, processorSupport);
        unbindTreePathNode(treeRoot, (UnbindState)state, matcher, processorSupport);
    }

    public void unbindTreePathNode(RouteNode treeNode, UnbindState state, Matcher matcher, ProcessorSupport processorSupport)
    {
        if (treeNode instanceof ExistingPropertyRouteNode)
        {
            unbindExistingPropertyNode((ExistingPropertyRouteNode)treeNode, state, processorSupport);
        }

        if (treeNode instanceof NewPropertyRouteNode)
        {
            unbindNewPropertyNode((NewPropertyRouteNode)treeNode, state, matcher, processorSupport);
        }

        unbindResolvedProperties(treeNode, processorSupport);

        for (RouteNodePropertyStub routeNodePropertyStub : treeNode._included())
        {
            cleanRouteNodePropertyStub(routeNodePropertyStub, state, processorSupport);
            matcher.fullMatch(routeNodePropertyStub, state);
        }

        for (RouteNodePropertyStub routeNodePropertyStub : treeNode._excluded())
        {
            cleanRouteNodePropertyStub(routeNodePropertyStub, state, processorSupport);
        }

        for (PropertyRouteNode childNode : treeNode._children())
        {
            unbindTreePathNode(childNode, state, matcher, processorSupport);
            matcher.fullMatch(childNode, state);
        }
    }

    public void cleanRouteNodePropertyStub(RouteNodePropertyStub routeNodePropertyStub, UnbindState state, ProcessorSupport processorSupport)
    {
        PropertyStub propertyStub = (PropertyStub)routeNodePropertyStub._propertyCoreInstance().getFirst();
        Shared.cleanPropertyStub(propertyStub, processorSupport);
        propertyStub._ownerRemove();
        for (InstanceValue parameter : routeNodePropertyStub._parameters())
        {
            GenericType parameterGenericType = parameter._genericType();
            Shared.cleanUpGenericType(parameterGenericType, state, processorSupport);
        }
    }

    private void unbindExistingPropertyNode(ExistingPropertyRouteNode treeNode, UnbindState state, ProcessorSupport processorSupport)
    {
        GenericType type = treeNode._type();
        if (type != null)
        {
            RouteNodePropertyStub existingPropertyRouteNodeStub = treeNode._property();
            PropertyStub propertyStub = (PropertyStub)existingPropertyRouteNodeStub._propertyCoreInstance().getFirst();
            Shared.cleanUpReferenceUsage(propertyStub._resolvedPropertyCoreInstance(), treeNode, processorSupport);
            this.cleanRouteNodePropertyStub(existingPropertyRouteNodeStub, state, processorSupport);
            Shared.cleanUpGenericType(treeNode._type(), state, processorSupport);
            treeNode._typeRemove();
            treeNode._rootRemove();
        }
    }

    private void unbindNewPropertyNode(NewPropertyRouteNode treeNode, UnbindState state, Matcher matcher, ProcessorSupport processorSupport)
    {
        GenericType type = treeNode._type();
        if (type != null)
        {
            Shared.cleanUpGenericType(type, state, processorSupport);
            treeNode._typeRemove();

            for (ValueSpecification valueSpecification : treeNode._specifications())
            {
                matcher.fullMatch(valueSpecification, state);
            }
            treeNode._rootRemove();
            NewPropertyRouteNodeFunctionDefinition<?, ?> functionDefinition = treeNode._functionDefinition();
            functionDefinition._ownerRemove();
            GenericType functionDefinitionClassifierGenericType = functionDefinition._classifierGenericType();
            if (functionDefinitionClassifierGenericType != null)
            {
                Shared.cleanUpGenericType(functionDefinitionClassifierGenericType, state, processorSupport);
                functionDefinition._classifierGenericTypeRemove();
            }
            for (ValueSpecification valueSpecification : functionDefinition._expressionSequence())
            {
                matcher.fullMatch(valueSpecification, state);
            }
        }
    }

    private void unbindResolvedProperties(RouteNode treeNode, ProcessorSupport processorSupport)
    {
        for (CoreInstance property : treeNode._resolvedPropertiesCoreInstance())
        {
            Shared.cleanUpReferenceUsage(property, treeNode, processorSupport);
        }
        treeNode._resolvedPropertiesRemove();
    }
}
