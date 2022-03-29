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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.ExistingPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.PropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNodePropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ExpressionSequenceValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class RootRouteNodePostProcessor extends Processor<RootRouteNode>
{
    @Override
    public String getClassName()
    {
        return M3Paths.RootRouteNode;
    }

    @Override
    public void process(RootRouteNode treePathRoot, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        GenericType type = treePathRoot._type();
        Type _class = type == null ? null : (Type) ImportStub.withImportStubByPass(type._rawTypeCoreInstance(), processorSupport);
        MutableListMultimap<String, RouteNode> resolvedTreeNodes = Multimaps.mutable.list.empty();
        resolveTreeNode(treePathRoot, treePathRoot, _class, matcher, state, repository, context, resolvedTreeNodes);
        secondPass(treePathRoot, resolvedTreeNodes, matcher, state, processorSupport);
    }

    @Override
    public void populateReferenceUsages(RootRouteNode treePathRoot, ModelRepository repository, ProcessorSupport processorSupport)
    {
        MutableSet<RouteNode> visited = Sets.mutable.empty();
        MutableStack<RouteNode> stack = Stacks.mutable.with(treePathRoot);
        while (stack.notEmpty())
        {
            RouteNode node = stack.pop();
            if (visited.add(node))
            {
                GenericTypeTraceability.addTraceForTreePath(node, repository, processorSupport);
                addReferenceUsagesForToManyProperty(node, node._resolvedPropertiesCoreInstance(), M3Properties.resolvedProperties, repository, processorSupport);
                if (node instanceof ExistingPropertyRouteNode)
                {
                    GenericTypeTraceability.addTraceForTreePath(node, repository, processorSupport);
                    RouteNodePropertyStub propertyStub = ((ExistingPropertyRouteNode) node)._property();
                    // TODO Fix this: the reference usage should be to the RouteNodePropertyStub, not to the ExistingPropertyRouteNode
//                addReferenceUsageForToOneProperty(propertyStub, M3Properties.property, repository, context, processorSupport);
                    AbstractProperty<?> property = (AbstractProperty<?>) ImportStub.withImportStubByPass(propertyStub._propertyCoreInstance().getFirst(), processorSupport);
                    addReferenceUsage(node, property, M3Properties.property, 0, repository, processorSupport);
                }
                else if (node instanceof NewPropertyRouteNode)
                {
                    NewPropertyRouteNodeFunctionDefinition<?, ?> functionDefinition = ((NewPropertyRouteNode) node)._functionDefinition();
                    GenericTypeTraceability.addTraceForNewPropertyRouteNodeFunctionDefinition(functionDefinition, repository, processorSupport);
                }
                for (PropertyRouteNode child : node._children())
                {
                    if (!visited.contains(child))
                    {
                        stack.push(child);
                    }
                }
            }
        }
    }

    private void secondPass(final RootRouteNode root, final ListMultimap<String, RouteNode> resolvedTreeNodes, final Matcher matcher, final ProcessorState state, final ProcessorSupport processorSupport) throws PureCompilationException
    {
        resolvedTreeNodes.forEachKey(s ->
        {
            ListIterable<RouteNode> nodes = resolvedTreeNodes.get(s);
            RouteNode firstNode = nodes.get(0);
            PostProcessor.processElement(matcher, firstNode, state, processorSupport);
            if (nodes.size() > 1)
            {
                for (int i = 1; i < nodes.size(); i++)
                {
                    RouteNode currentNode = nodes.get(i);
                    copyNode(root, firstNode, currentNode);
                    PostProcessor.processElement(matcher, currentNode, state, processorSupport);
                }
            }
        });
    }

    private void copyNode(RootRouteNode root, RouteNode from, RouteNode to)
    {
        to._resolvedPropertiesCoreInstance(FastList.<CoreInstance>newList(to._resolvedPropertiesCoreInstance().size() + from._resolvedPropertiesCoreInstance().size()).withAll(to._resolvedPropertiesCoreInstance()).withAll(from._resolvedPropertiesCoreInstance()));
        to._children(FastList.<PropertyRouteNode>newList(to._children().size() + from._children().size()).withAll(to._children()).withAll(from._children()));
        if (from instanceof NewPropertyRouteNode)
        {
            ((NewPropertyRouteNode)to)._specifications(FastList.<ValueSpecification>newList(((NewPropertyRouteNode)to)._specifications().size() + ((NewPropertyRouteNode)from)._specifications().size()).withAll(((NewPropertyRouteNode)to)._specifications()).withAll(((NewPropertyRouteNode)from)._specifications()));
        }
        if (to instanceof PropertyRouteNode && ((PropertyRouteNode) to)._root() == null)
        {
            ((PropertyRouteNode) to)._root(root);
        }
    }

    private void resolveTreeNode(RootRouteNode root, RouteNode treePathNode, Type type, Matcher matcher, ProcessorState state, ModelRepository repository, Context context, MutableMultimap<String, RouteNode> resolvedTreeNodes) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        String nodeName = treePathNode._name();
        if (resolvedTreeNodes.containsKey(nodeName))
        {
            //node has already been resolved
            //throw exception if types don't match
            Type alreadyProcessedNodeType = (Type) ImportStub.withImportStubByPass(resolvedTreeNodes.get(nodeName).getFirst()._type()._rawTypeCoreInstance(), processorSupport);
            if (alreadyProcessedNodeType != type)
            {
                throw new PureCompilationException(treePathNode.getSourceInformation(), String.format("Invalid Treepath! 2 nodes with same name %s but with different types %s %s", nodeName, type.getName(), alreadyProcessedNodeType.getName()));
            }
            resolvedTreeNodes.put(nodeName, treePathNode);
            return;
        }

        resolvedTreeNodes.put(nodeName, treePathNode);

        this.resolveSimpleProperties(treePathNode, type, matcher, state, processorSupport);
        for (PropertyRouteNode childNode : treePathNode._children())
        {
            if (childNode._root() == null)
            {
                childNode._root(root);
            }
            if (childNode instanceof ExistingPropertyRouteNode)
            {
                resolveExistingPropertyNode(root, type, matcher, state, repository, context, resolvedTreeNodes, processorSupport, (ExistingPropertyRouteNode) childNode);
            }
            else
            {
                //NewProperty, not part of the original model
                resolveNewPropertyNode(root, type, matcher, state, repository, context, resolvedTreeNodes, processorSupport, (NewPropertyRouteNode) childNode);
            }
        }
    }

    private void resolveExistingPropertyNode(RootRouteNode root, Type type, Matcher matcher, ProcessorState state, ModelRepository repository, Context context, MutableMultimap<String, RouteNode> resolvedTreeNodes, ProcessorSupport processorSupport, ExistingPropertyRouteNode childNode)
    {
        RouteNodePropertyStub existingPropertyRoutNodeStub = childNode._property();
        AbstractProperty<?> property = this.resolvePropertyStub(type, existingPropertyRoutNodeStub, matcher, state, processorSupport);
        GenericType propertyGenericType = property._genericType();
        Type propertyType = (Type) ImportStub.withImportStubByPass(propertyGenericType._rawTypeCoreInstance(), processorSupport);

        if (childNode._type() == null)
        {
            GenericType genericTypeCopy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(propertyGenericType, childNode.getSourceInformation(), processorSupport);
            childNode._type(genericTypeCopy);
        }

        this.resolveTreeNode(root, childNode, propertyType, matcher, state, repository, context, resolvedTreeNodes);
    }

    private void resolveNewPropertyNode(RootRouteNode root, Type type, Matcher matcher, ProcessorState state, ModelRepository repository, Context context, MutableMultimap<String, RouteNode> resolvedTreeNodes, ProcessorSupport processorSupport, NewPropertyRouteNode childNode)
    {
        RichIterable<? extends ValueSpecification> valueSpecifications = childNode._specifications();
        if (valueSpecifications.isEmpty())
        {
            throw new PureCompilationException(childNode.getSourceInformation(), "Invalid new property defined. New properties must define a valid Value Specification");
        }

        NewPropertyRouteNodeFunctionDefinition<?, ?> functionDefinition = childNode._functionDefinition();
        if (functionDefinition._owner() == null)
        {
            functionDefinition._owner(childNode);
        }
        // TODO process the full function definition instead of just the expressionSequence
        processDerivedPropertyExpressions(childNode, type, functionDefinition._expressionSequence(), matcher, state, processorSupport);
//        PostProcessor.processElement(matcher, functionDefinition, state, context, processorSupport);

        // Set classifierGenericType for functionDefinition
        GenericType sourceType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(type, functionDefinition.getSourceInformation(), processorSupport);
        ValueSpecification lastExpression = functionDefinition._expressionSequence().getLast();
        GenericType returnType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(lastExpression._genericType(), functionDefinition.getSourceInformation(), processorSupport);
        Multiplicity returnMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.copyMultiplicity(lastExpression._multiplicity(), functionDefinition.getSourceInformation(), processorSupport);
        GenericType classifierGenericType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(functionDefinition.getClassifier(), functionDefinition.getSourceInformation(), processorSupport);
        classifierGenericType._typeArguments(Lists.immutable.with(sourceType, returnType));
        classifierGenericType._multiplicityArguments(Lists.immutable.with(returnMultiplicity));
        functionDefinition._classifierGenericType(classifierGenericType);

        processDerivedPropertyExpressions(childNode, type, valueSpecifications, matcher, state, processorSupport);
        GenericType propertyGenericType = valueSpecifications.getLast()._genericType();

        Type _class = (Type) ImportStub.withImportStubByPass(propertyGenericType._rawTypeCoreInstance(), processorSupport);
        if (childNode._type() == null)
        {
            GenericType genericTypeCopy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(propertyGenericType, childNode.getSourceInformation(), processorSupport);
            childNode._type(genericTypeCopy);
        }
        this.resolveTreeNode(root, childNode, _class, matcher, state, repository, context, resolvedTreeNodes);
    }

    private void resolveSimpleProperties(RouteNode treePathNode, Type _class, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        RichIterable<? extends RouteNodePropertyStub> includedProperties = treePathNode._included();
        RichIterable<? extends RouteNodePropertyStub> excludedProperties = treePathNode._excluded();
        MutableList<CoreInstance> resolvedProperties = FastList.newList();
        if ("true".equals(treePathNode._includeAll()))
        {
            resolvedProperties.addAllIterable(getPrimitiveOrOfGivenTypeProperties(_class, processorSupport));
            resolvedProperties.addAllIterable(getPrimitiveOrOfGivenTypeQualifiedProperties(_class, processorSupport));
        }
        else if (includedProperties.notEmpty())
        {
            for (RouteNodePropertyStub includedPropertyStub : includedProperties)
            {
                CoreInstance property = this.resolvePropertyStub(_class, includedPropertyStub, matcher, state, processorSupport);
                resolvedProperties.add(property);
            }
        }
        else if (excludedProperties.notEmpty())
        {
            //TODO cannot handle simple and qualified properties with same name
            MapIterable<String, CoreInstance> simplePropertiesByName = processorSupport.class_getSimplePropertiesByName(_class);
            MapIterable<String, CoreInstance> qualifiedPropertiesByName = _Class.getQualifiedPropertiesByName(_class, processorSupport);
            resolvedProperties.addAllIterable(getPrimitiveOrOfGivenTypeProperties(_class, processorSupport));
            resolvedProperties.addAllIterable(getPrimitiveOrOfGivenTypeQualifiedProperties(_class, processorSupport));
            for (RouteNodePropertyStub excludedPropertyStub : excludedProperties)
            {
                AbstractProperty<?> property = this.resolvePropertyStub(_class, excludedPropertyStub, matcher, state, processorSupport);
                String name = Property.getPropertyId(property, processorSupport);
                if (simplePropertiesByName.containsKey(name))
                {
                    resolvedProperties.remove(simplePropertiesByName.get(name));
                }
                if (qualifiedPropertiesByName.containsKey(name))
                {
                    resolvedProperties.remove(qualifiedPropertiesByName.get(name));
                }
            }
        }
        treePathNode._resolvedPropertiesCoreInstance(FastList.<CoreInstance>newList(treePathNode._resolvedPropertiesCoreInstance().size() + resolvedProperties.size()).withAll(treePathNode._resolvedPropertiesCoreInstance()).withAll(resolvedProperties));
    }

    private AbstractProperty<?> resolvePropertyStub(Type _class, RouteNodePropertyStub routeNodePropertyStub, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        PropertyStub propertyStubNonResolved = (PropertyStub) routeNodePropertyStub._propertyCoreInstance().getFirst();
        propertyStubNonResolved._ownerCoreInstance(_class);
        AbstractProperty<?> property = (AbstractProperty<?>) ImportStub.withImportStubByPass(routeNodePropertyStub._propertyCoreInstance().getFirst(), processorSupport);
        ListIterable<? extends VariableExpression> parameters = ((FunctionType) processorSupport.function_getFunctionType(property))._parameters().toList();
        for (VariableExpression parameter : ListHelper.tail(parameters))
        {
            //TODO match the parameters
            parameter._genericType();
        }
        PostProcessor.processElement(matcher, routeNodePropertyStub, state, processorSupport);
        return property;
    }

    private RichIterable<? extends CoreInstance> getPrimitiveOrOfGivenTypeQualifiedProperties(Type _class, ProcessorSupport processorSupport)
    {
        return _Class.getQualifiedProperties(_class, processorSupport).select(primitiveOrOfGivenTypePropertyPredicate(_class, processorSupport));
    }

    private RichIterable<CoreInstance> getPrimitiveOrOfGivenTypeProperties(Type _class, ProcessorSupport processorSupport)
    {
        return processorSupport.class_getSimpleProperties(_class).select(primitiveOrOfGivenTypePropertyPredicate(_class, processorSupport));
    }

    private Predicate<CoreInstance> primitiveOrOfGivenTypePropertyPredicate(Type _class, ProcessorSupport processorSupport)
    {
        return property ->
        {
            GenericType genericType = ((AbstractProperty<?>) property)._genericType();
            Type propertyType = genericType == null ? null : (Type) ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
            return propertyType == _class || propertyType instanceof DataType;
        };
    }

    private static void processDerivedPropertyExpressions(NewPropertyRouteNodeAccessor treePathNode, Type type, RichIterable<? extends ValueSpecification> expressions, Matcher matcher, ProcessorState processorState, ProcessorSupport processorSupport)
    {
        processorState.pushVariableContext();
        processorState.getVariableContext().buildAndRegister("this", (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(type, processorSupport), (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne), processorSupport);
        int i = 0;
        for (ValueSpecification expression : expressions)
        {
            if (expression._usageContext() == null)
            {
                ExpressionSequenceValueSpecificationContext usageContext = (ExpressionSequenceValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.ExpressionSequenceValueSpecificationContext);

                usageContext._offset(i);
                usageContext._functionDefinition(treePathNode._functionDefinition());
                expression._usageContext(usageContext);
            }
            processorState.resetVariables();
            PostProcessor.processElement(matcher, expression, processorState, processorSupport);
            i++;
        }
        processorState.popVariableContext();
    }
}
