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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.primitive.IntToIntFunctions;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;

import java.util.Formatter;
import java.util.Set;

public class GraphStatistics
{
    private GraphStatistics()
    {
        // static utility class
    }

    public static Multimap<CoreInstance, CoreInstance> allInstancesByClassifier(ModelRepository repository)
    {
        return allInstancesByClassifier(repository, Multimaps.mutable.list.<CoreInstance, CoreInstance>empty());
    }

    public static <T extends MutableMultimap<CoreInstance, CoreInstance>> T allInstancesByClassifier(ModelRepository repository, T target)
    {
        return GraphNodeIterable.fromModelRepository(repository).groupBy(CoreInstance.GET_CLASSIFIER, target);
    }

    public static Multimap<String, CoreInstance> allInstancesByClassifierPath(ModelRepository repository)
    {
        return allInstancesByClassifierPath(repository, Multimaps.mutable.list.<String, CoreInstance>empty());
    }

    public static <T extends MutableMultimap<String, CoreInstance>> T allInstancesByClassifierPath(ModelRepository repository, T target)
    {
        return GraphNodeIterable.fromModelRepository(repository).groupBy(Functions.chain(CoreInstance.GET_CLASSIFIER, PackageableElement.GET_USER_PATH), target);
    }

    public static Bag<CoreInstance> instanceCountByClassifier(ModelRepository repository)
    {
        return GraphNodeIterable.fromModelRepository(repository).collect(CoreInstance.GET_CLASSIFIER, Bags.mutable.<CoreInstance>empty());
    }

    public static Bag<String> instanceCountByClassifierPath(ModelRepository repository)
    {
        return GraphNodeIterable.fromModelRepository(repository).collect(Functions.chain(CoreInstance.GET_CLASSIFIER, PackageableElement.GET_USER_PATH), Bags.mutable.<String>empty());
    }

    public static ObjectIntMap<CoreInstance> instanceCountByClassifierAsMap(ModelRepository repository)
    {
        return instanceCountAsMap(repository, CoreInstance.GET_CLASSIFIER);
    }

    public static ObjectIntMap<String> instanceCountByClassifierPathAsMap(ModelRepository repository)
    {
        return instanceCountAsMap(repository, Functions.chain(CoreInstance.GET_CLASSIFIER, PackageableElement.GET_USER_PATH));
    }

    public static RichIterable<CoreInstance> findUnresolvedStubs(ModelRepository repository)
    {
        return GraphNodeIterable.fromModelRepository(repository).select(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance instance)
            {
                return ((instance instanceof ImportStub) && (((ImportStub)instance)._resolvedNode() == null)) ||
                        ((instance instanceof PropertyStub) && (((PropertyStub)instance)._resolvedPropertyCoreInstance() == null)) ||
                        ((instance instanceof EnumStub) && (((EnumStub)instance)._resolvedEnumCoreInstance() == null));
            }
        });
    }

    private static <T> ObjectIntMap<T> instanceCountAsMap(ModelRepository repository, Function<CoreInstance, T> keyFn)
    {
        MutableObjectIntMap<T> map = ObjectIntMaps.mutable.empty();
        IntToIntFunction increment = IntToIntFunctions.increment();
        for (CoreInstance node : GraphNodeIterable.fromModelRepository(repository))
        {
            map.updateValue(keyFn.valueOf(node), 0, increment);
        }
        return map;
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String indent, String description1, Bag<String> instanceCountsByClassifierPath1, String description2, Bag<String> instanceCountsByClassifierPath2)
    {
        writeInstanceCountsByClassifierPathDeltas(appendable, getDefaultFormatString(indent, description1, description2), instanceCountsByClassifierPath1, instanceCountsByClassifierPath2);
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String formatString, Bag<String> instanceCountsByClassifierPath1, Bag<String> instanceCountsByClassifierPath2)
    {
        final MutableSet<String> classifierPaths = Sets.mutable.empty();
        ObjectIntProcedure<String> collectClassifierPath = new ObjectIntProcedure<String>()
        {
            @Override
            public void value(String classifierPath, int count)
            {
                classifierPaths.add(classifierPath);
            }
        };
        instanceCountsByClassifierPath1.forEachWithOccurrences(collectClassifierPath);
        instanceCountsByClassifierPath2.forEachWithOccurrences(collectClassifierPath);
        IntFunction<String> countFn1 = new BagCountFunction<>(instanceCountsByClassifierPath1);
        IntFunction<String> countFn2 = new BagCountFunction<>(instanceCountsByClassifierPath2);
        writeInstanceCountsByClassifierPathDeltas(appendable, formatString, classifierPaths, countFn1, countFn2);
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String indent, String description1, ObjectIntMap<String> instanceCountsByClassifierPath1, String description2, ObjectIntMap<String> instanceCountsByClassifierPath2)
    {
        writeInstanceCountsByClassifierPathDeltas(appendable, getDefaultFormatString(indent, description1, description2), instanceCountsByClassifierPath1, instanceCountsByClassifierPath2);
    }

    public static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String formatString, ObjectIntMap<String> instanceCountsByClassifierPath1, ObjectIntMap<String> instanceCountsByClassifierPath2)
    {
        SetIterable<String> classifierPaths = instanceCountsByClassifierPath1.keysView().toSet().withAll(instanceCountsByClassifierPath2.keysView());
        IntFunction<String> countFn1 = new ObjectIntMapCountFunction<>(instanceCountsByClassifierPath1);
        IntFunction<String> countFn2 = new ObjectIntMapCountFunction<>(instanceCountsByClassifierPath2);
        writeInstanceCountsByClassifierPathDeltas(appendable, formatString, classifierPaths, countFn1, countFn2);
    }

    private static String getDefaultFormatString(String indent, String description1, String description2)
    {
        return String.format("%s%%s - %s: %%,d; %s: %%,d; delta: %%,d%%n", (indent == null) ? "" : indent, (description1 == null) ? "first" : description1, (description2 == null) ? "second" : description2);
    }

    private static void writeInstanceCountsByClassifierPathDeltas(Appendable appendable, String formatString, SetIterable<String> classifierPaths, IntFunction<String> countFn1, IntFunction<String> countFn2)
    {
        Formatter formatter = new Formatter(appendable);
        for (String classifier : classifierPaths.toSortedList())
        {
            int count1 = countFn1.intValueOf(classifier);
            int count2 = countFn2.intValueOf(classifier);
            if (count1 != count2)
            {
                formatter.format(formatString, classifier, count1, count2, (count2 - count1));
            }
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
        return allPathsBetween(startNodePaths, Predicates.equal(endNode), maxPathLength, processorSupport);
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
        return allPathsBetween(startNodePaths, Predicates.in((endNodes instanceof Set) ? endNodes : Sets.mutable.withAll(endNodes)), maxPathLength, processorSupport);
    }

    private static LazyIterable<GraphPath> allPathsBetween(Iterable<String> startNodePaths, Predicate<? super CoreInstance> isEndNode, int maxPathLength, ProcessorSupport processorSupport)
    {
        GraphPathIterable graphPathIterable = GraphPathIterable.newGraphPathIterable(startNodePaths, isEndNode, maxPathLength, processorSupport);
        return graphPathIterable.select(Predicates.attributePredicate(Functions.bind(GraphPath.RESOLVE, processorSupport), isEndNode));
    }

    public static LazyIterable<String> allTopLevelAndPackagedElementPaths(ProcessorSupport processorSupport)
    {
        final Predicate<CoreInstance> isNotPackage = new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance instance)
            {
                return !(instance instanceof Package);
            }
        };
        Function<Package, ListIterable<String>> getPackageChildrenPaths = new Function<Package, ListIterable<String>>()
        {
            @Override
            public ListIterable<String> valueOf(Package pkg)
            {
                return (ListIterable<String>) pkg._children().collectIf(isNotPackage, PackageableElement.GET_USER_PATH);
            }
        };
        return LazyIterate.concatenate(_Package.SPECIAL_TYPES, PackageTreeIterable.newPackageTreeIterable((Package)processorSupport.repository_getTopLevel(M3Paths.Root)).flatCollect(getPackageChildrenPaths));
    }

    private static class ObjectIntMapCountFunction<T> implements IntFunction<T>
    {
        private final ObjectIntMap<T> map;

        private ObjectIntMapCountFunction(ObjectIntMap<T> map)
        {
            this.map = map;
        }

        @Override
        public int intValueOf(T key)
        {
            return this.map.getIfAbsent(key, 0);
        }
    }

    private static class BagCountFunction<T> implements IntFunction<T>
    {
        private final Bag<T> bag;

        private BagCountFunction(Bag<T> bag)
        {
            this.bag = bag;
        }

        @Override
        public int intValueOf(T key)
        {
            return this.bag.occurrencesOf(key);
        }
    }
}
