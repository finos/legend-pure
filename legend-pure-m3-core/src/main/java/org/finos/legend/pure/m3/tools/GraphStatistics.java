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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;

import java.util.Formatter;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class GraphStatistics
{
    private GraphStatistics()
    {
        // static utility class
    }

    public static Multimap<CoreInstance, CoreInstance> allInstancesByClassifier(ModelRepository repository)
    {
        return allInstancesByClassifier(repository, Multimaps.mutable.list.empty());
    }

    public static <T extends MutableMultimap<CoreInstance, CoreInstance>> T allInstancesByClassifier(ModelRepository repository, T target)
    {
        return GraphNodeIterable.fromModelRepository(repository).groupBy(CoreInstance::getClassifier, target);
    }

    public static Multimap<String, CoreInstance> allInstancesByClassifierPath(ModelRepository repository)
    {
        return allInstancesByClassifierPath(repository, Multimaps.mutable.list.empty());
    }

    public static <T extends MutableMultimap<String, CoreInstance>> T allInstancesByClassifierPath(ModelRepository repository, T target)
    {
        return GraphNodeIterable.fromModelRepository(repository).groupBy(n -> PackageableElement.getUserPathForPackageableElement(n.getClassifier()), target);
    }

    public static Bag<CoreInstance> instanceCountByClassifier(ModelRepository repository)
    {
        return GraphNodeIterable.fromModelRepository(repository).collect(CoreInstance::getClassifier, Bags.mutable.empty());
    }

    public static Bag<String> instanceCountByClassifierPath(ModelRepository repository)
    {
        return GraphNodeIterable.fromModelRepository(repository).collect(n -> PackageableElement.getUserPathForPackageableElement(n.getClassifier()), Bags.mutable.empty());
    }

    public static ObjectIntMap<CoreInstance> instanceCountByClassifierAsMap(ModelRepository repository)
    {
        return instanceCountAsMap(repository, CoreInstance::getClassifier);
    }

    public static ObjectIntMap<String> instanceCountByClassifierPathAsMap(ModelRepository repository)
    {
        return instanceCountAsMap(repository, n -> PackageableElement.getUserPathForPackageableElement(n.getClassifier()));
    }

    public static RichIterable<CoreInstance> findUnresolvedStubs(ModelRepository repository)
    {
        return GraphNodeIterable.fromModelRepository(repository).select(AnyStubHelper::isUnresolvedStub);
    }

    private static <T> ObjectIntMap<T> instanceCountAsMap(ModelRepository repository, Function<CoreInstance, T> keyFn)
    {
        MutableObjectIntMap<T> map = ObjectIntMaps.mutable.empty();
        GraphNodeIterable.fromModelRepository(repository).forEach(node -> map.addToValue(keyFn.apply(node), 1));
        return map;
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String indent, String description1, Bag<String> instanceCountsByClassifierPath1, String description2, Bag<String> instanceCountsByClassifierPath2)
    {
        writeInstanceCountsByClassifierPathDeltas(appendable, getDefaultFormatString(indent, description1, description2), instanceCountsByClassifierPath1, instanceCountsByClassifierPath2);
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String formatString, Bag<String> instanceCountsByClassifierPath1, Bag<String> instanceCountsByClassifierPath2)
    {
        MutableSet<String> classifierPaths = Sets.mutable.empty();
        instanceCountsByClassifierPath1.forEachWithOccurrences((classifierPath, count) -> classifierPaths.add(classifierPath));
        instanceCountsByClassifierPath2.forEachWithOccurrences((classifierPath, count) -> classifierPaths.add(classifierPath));
        writeInstanceCountsByClassifierPathDeltas(appendable, formatString, classifierPaths, instanceCountsByClassifierPath1::occurrencesOf, instanceCountsByClassifierPath2::occurrencesOf);
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String indent, String description1, ObjectIntMap<String> instanceCountsByClassifierPath1, String description2, ObjectIntMap<String> instanceCountsByClassifierPath2)
    {
        writeInstanceCountsByClassifierPathDeltas(appendable, getDefaultFormatString(indent, description1, description2), instanceCountsByClassifierPath1, instanceCountsByClassifierPath2);
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String formatString, ObjectIntMap<String> instanceCountsByClassifierPath1, ObjectIntMap<String> instanceCountsByClassifierPath2)
    {
        SetIterable<String> classifierPaths = instanceCountsByClassifierPath1.keysView().toSet().withAll(instanceCountsByClassifierPath2.keysView());
        writeInstanceCountsByClassifierPathDeltas(appendable, formatString, classifierPaths, instanceCountsByClassifierPath1::get, instanceCountsByClassifierPath2::get);
    }

    private static String getDefaultFormatString(String indent, String description1, String description2)
    {
        return String.format("%s%%s - %s: %%,d; %s: %%,d; delta: %%,d%%n", (indent == null) ? "" : indent, (description1 == null) ? "first" : description1, (description2 == null) ? "second" : description2);
    }

    private static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String formatString, SetIterable<String> classifierPaths, ToIntFunction<String> countFn1, ToIntFunction<String> countFn2)
    {
        try (Formatter formatter = new Formatter(appendable))
        {
            classifierPaths.toSortedList().forEach(classifier ->
            {
                int count1 = countFn1.applyAsInt(classifier);
                int count2 = countFn2.applyAsInt(classifier);
                if (count1 != count2)
                {
                    formatter.format(formatString, classifier, count1, count2, (count2 - count1));
                }
            });
        }
    }

    public static LazyIterable<GraphPath> allPathsBetween(String startNodePath, CoreInstance endNode, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNode, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(String startNodePath, CoreInstance endNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNode, maxPathLength, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(Iterable<String> startNodePaths, CoreInstance endNode, ProcessorSupport processorSupport)
    {
        return allPathsBetween(startNodePaths, endNode, -1, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(Iterable<String> startNodePaths, CoreInstance endNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        return allPathsBetween(startNodePaths, endNode::equals, maxPathLength, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(String startNodePath, Iterable<? extends CoreInstance> endNodes, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNodes, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(String startNodePath, Iterable<? extends CoreInstance> endNodes, int maxPathLength, ProcessorSupport processorSupport)
    {
        return allPathsBetween(Sets.immutable.with(startNodePath), endNodes, maxPathLength, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(Iterable<String> startNodePaths, Iterable<? extends CoreInstance> endNodes, ProcessorSupport processorSupport)
    {
        return allPathsBetween(startNodePaths, endNodes, -1, processorSupport);
    }

    public static LazyIterable<GraphPath> allPathsBetween(Iterable<String> startNodePaths, Iterable<? extends CoreInstance> endNodes, int maxPathLength, ProcessorSupport processorSupport)
    {
        Set<?> endNodesSet = (endNodes instanceof Set) ? (Set<?>) endNodes : Sets.mutable.withAll(endNodes);
        return allPathsBetween(startNodePaths, endNodesSet::contains, maxPathLength, processorSupport);
    }

    private static LazyIterable<GraphPath> allPathsBetween(Iterable<String> startNodePaths, Predicate<? super CoreInstance> isEndNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        return GraphPathIterable.newGraphPathIterable(startNodePaths, isEndNode, maxPathLength, processorSupport)
                .asResolvedGraphPathIterable()
                .select(path -> isEndNode.test(path.getLastResolvedNode()))
                .collect(GraphPathIterable.ResolvedGraphPath::getGraphPath);
    }

    public static LazyIterable<String> allTopLevelAndPackagedElementPaths(ProcessorSupport processorSupport)
    {
        LazyIterable<String> packagedElements = PackageTreeIterable.newRootPackageTreeIterable(processorSupport)
                .flatCollect(Package::_children)
                .reject(c -> c instanceof Package)
                .collect(PackageableElement::getUserPathForPackageableElement);
        return _Package.SPECIAL_TYPES.asLazy().concatenate(packagedElements);
    }
}