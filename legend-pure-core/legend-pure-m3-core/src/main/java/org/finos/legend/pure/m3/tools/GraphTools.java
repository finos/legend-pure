// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.graph.GraphPathIterable;
import org.finos.legend.pure.m3.navigation.graph.ResolvedGraphPath;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class GraphTools
{
    public static RichIterable<String> getTopLevelNames()
    {
        MutableList<String> result = Lists.mutable.ofInitialCapacity(_Package.SPECIAL_TYPES.size() + 1);
        result.addAll(_Package.SPECIAL_TYPES.castToSet());
        result.add(M3Paths.Root);
        return result;
    }

    public static boolean isTopLevelName(String name)
    {
        return M3Paths.Root.equals(name) || _Package.SPECIAL_TYPES.contains(name);
    }

    public static RichIterable<CoreInstance> getTopLevels(ProcessorSupport processorSupport)
    {
        return getTopLevelNames().collect(processorSupport::repository_getTopLevel);
    }

    public static LazyIterable<String> getTopLevelAndPackagedElementPaths(ModelRepository repository)
    {
        return getTopLevelAndPackagedElements(repository).collect(PackageableElement::getUserPathForPackageableElement);
    }

    public static LazyIterable<String> getTopLevelAndPackagedElementPaths(ProcessorSupport processorSupport)
    {
        return getTopLevelAndPackagedElements(processorSupport).collect(PackageableElement::getUserPathForPackageableElement);
    }

    public static PackageableElementIterable getTopLevelAndPackagedElements(ModelRepository repository)
    {
        return PackageableElementIterable.builder().withTopLevels(repository).build();
    }

    public static PackageableElementIterable getTopLevelAndPackagedElements(ProcessorSupport processorSupport)
    {
        return PackageableElementIterable.builder().withTopLevels(processorSupport).build();
    }

    public static LazyIterable<CoreInstance> getComponentInstances(CoreInstance element, ProcessorSupport processorSupport)
    {
        Objects.requireNonNull(element.getSourceInformation(), "element source information may not be null");
        return GraphNodeIterable.builder()
                .withStartingNode(element)
                .withKeyFilter(GraphTools::internalPropertyFilter)
                .withNodeFilter(node -> internalNodeFilter(element, node, processorSupport))
                .build();
    }

    public static GraphPathIterable internalGraphPaths(CoreInstance element, ProcessorSupport processorSupport)
    {
        Objects.requireNonNull(element.getSourceInformation(), "element source information may not be null");
        return GraphPathIterable.builder(processorSupport)
                .withStartNode(element)
                .withPropertyFilter(GraphTools::internalPropertyFilter)
                .withPathFilter(rgp -> internalPathFilter(element, rgp, processorSupport))
                .build();
    }

    public static ResolvedGraphPath findPathToInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return findPathToInstance(instance, processorSupport, false);
    }

    public static ResolvedGraphPath findPathToInstance(CoreInstance instance, ProcessorSupport processorSupport, boolean allowPathToExternalInstance)
    {
        return getTopLevelAndPackagedElements(processorSupport)
                .flatCollect(e -> Lists.immutable.with(findPathToInstanceWithinElement(e, instance, processorSupport, allowPathToExternalInstance)))
                .detect(Objects::nonNull);
    }

    public static ResolvedGraphPath findPathToInstanceWithinElement(CoreInstance element, CoreInstance instance, ProcessorSupport processorSupport)
    {
        return findPathToInstanceWithinElement(element, instance, processorSupport, false);
    }

    public static ResolvedGraphPath findPathToInstanceWithinElement(CoreInstance element, CoreInstance instance, ProcessorSupport processorSupport, boolean allowPathToExternalInstance)
    {
        if (element == instance)
        {
            return GraphPathIterable.builder(processorSupport)
                    .withStartNode(element)
                    .withPropertyFilter((rgp, prop) -> false)
                    .build()
                    .getAny();
        }
        if ((element.getSourceInformation() == null) || (!allowPathToExternalInstance && isExternalNode(element, instance, processorSupport)))
        {
            return null;
        }
        MutableSet<CoreInstance> visited = Sets.mutable.empty();
        return GraphPathIterable.builder(processorSupport)
                .withStartNode(element)
                .withPropertyFilter(GraphTools::internalPropertyFilter)
                .withPathFilter(rgp ->
                {
                    CoreInstance node = rgp.getLastResolvedNode();
                    return (node == instance) ?
                           GraphWalkFilterResult.ACCEPT_AND_STOP :
                           GraphWalkFilterResult.reject(visited.add(node) && !isExternalNode(element, node, processorSupport));
                })
                .build()
                .getAny();
    }

    public static LazyIterable<ResolvedGraphPath> getPathsToInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return getPathsToInstance(instance, processorSupport, false);
    }

    public static LazyIterable<ResolvedGraphPath> getPathsToInstance(CoreInstance instance, ProcessorSupport processorSupport, boolean allowPathsToExternalInstance)
    {
        return getTopLevelAndPackagedElements(processorSupport).flatCollect(e -> getPathsToInstanceWithinElement(e, instance, processorSupport, allowPathsToExternalInstance));
    }

    public static GraphPathIterable getPathsToInstanceWithinElement(CoreInstance element, CoreInstance instance, ProcessorSupport processorSupport)
    {
        return getPathsToInstanceWithinElement(element, instance, processorSupport, false);
    }

    public static GraphPathIterable getPathsToInstanceWithinElement(CoreInstance element, CoreInstance instance, ProcessorSupport processorSupport, boolean allowPathsToExternalInstance)
    {
        if (element == instance)
        {
            return GraphPathIterable.builder(processorSupport)
                    .withStartNode(element)
                    .withPropertyFilter((rgp, prop) -> false)
                    .build();
        }
        if ((element.getSourceInformation() == null) || (!allowPathsToExternalInstance && isExternalNode(element, instance, processorSupport)))
        {
            return GraphPathIterable.builder(processorSupport).build();
        }
        return GraphPathIterable.builder(processorSupport)
                .withStartNode(element)
                .withPropertyFilter(GraphTools::internalPropertyFilter)
                .withPathFilter(rgp ->
                {
                    CoreInstance node = rgp.getLastResolvedNode();
                    return (node == instance) ?
                           GraphWalkFilterResult.ACCEPT_AND_STOP :
                           GraphWalkFilterResult.reject(!isExternalNode(element, node, processorSupport));
                })
                .build();
    }

    private static boolean isExternalNode(CoreInstance element, CoreInstance node, ProcessorSupport processorSupport)
    {
        if (node == element)
        {
            return false;
        }

        SourceInformation nodeSourceInfo = node.getSourceInformation();
        return ((nodeSourceInfo == null) ? _Package.isPackage(node, processorSupport) : !element.getSourceInformation().subsumes(nodeSourceInfo));
    }

    private static boolean internalPropertyFilter(CoreInstance node, String key)
    {
        switch (key)
        {
            case M3Properties._package:
            {
                return !M3PropertyPaths._package.equals(node.getRealKeyByName(key));
            }
            case M3Properties.children:
            {
                return !M3PropertyPaths.children.equals(node.getRealKeyByName(key));
            }
            default:
            {
                return true;
            }
        }
    }

    private static boolean internalPropertyFilter(ResolvedGraphPath resolvedGraphPath, String property)
    {
        return internalPropertyFilter(resolvedGraphPath.getLastResolvedNode(), property);
    }

    private static GraphWalkFilterResult internalNodeFilter(CoreInstance element, CoreInstance node, ProcessorSupport processorSupport)
    {
        return isExternalNode(element, node, processorSupport) ? GraphWalkFilterResult.REJECT_AND_STOP : GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
    }

    private static GraphWalkFilterResult internalPathFilter(CoreInstance element, ResolvedGraphPath resolvedGraphPath, ProcessorSupport processorSupport)
    {
        return internalNodeFilter(element, resolvedGraphPath.getLastResolvedNode(), processorSupport);
    }

    public static GraphPathIterable allPathsBetween(String startNodePath, CoreInstance endNode, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNode, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(String startNodePath, CoreInstance endNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNode, maxPathLength, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(Iterable<String> startNodePaths, CoreInstance endNode, ProcessorSupport processorSupport)
    {
        return allPathsBetween(startNodePaths, endNode, -1, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(Iterable<String> startNodePaths, CoreInstance endNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        return allPathsBetween(startNodePaths, endNode::equals, maxPathLength, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(String startNodePath, Iterable<? extends CoreInstance> endNodes, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNodes, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(String startNodePath, Iterable<? extends CoreInstance> endNodes, int maxPathLength, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNodes, maxPathLength, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(Iterable<String> startNodePaths, Iterable<? extends CoreInstance> endNodes, ProcessorSupport processorSupport)
    {
        return allPathsBetween(startNodePaths, endNodes, -1, processorSupport);
    }

    public static GraphPathIterable allPathsBetween(Iterable<String> startNodePaths, Iterable<? extends CoreInstance> endNodes, int maxPathLength, ProcessorSupport processorSupport)
    {
        Set<?> endNodesSet = (endNodes instanceof Set) ? (Set<?>) endNodes : Sets.mutable.withAll(endNodes);
        return allPathsBetween(startNodePaths, endNodesSet::contains, maxPathLength, processorSupport);
    }

    private static GraphPathIterable allPathsBetween(Iterable<String> startNodePaths, Predicate<? super CoreInstance> isEndNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        return GraphPathIterable.builder(processorSupport)
                .withStartNodePaths(startNodePaths)
                .withPathFilter(rgp -> isEndNode.test(rgp.getLastResolvedNode()) ?
                                       GraphWalkFilterResult.ACCEPT_AND_STOP :
                                       GraphWalkFilterResult.reject(rgp.getGraphPath().getEdgeCount() < maxPathLength))
                .build();
    }
}
