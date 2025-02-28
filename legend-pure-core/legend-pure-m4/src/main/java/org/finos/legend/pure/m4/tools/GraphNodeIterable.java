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

package org.finos.legend.pure.m4.tools;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.SynchronizedMutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An iterable that iterates through the nodes of a graph, starting from a given set of nodes and traversing to
 * connected nodes. The traversal of the graph can be controlled by providing a
 * {@link java.util.function.Function function} from nodes to {@link GraphWalkFilterResult GraphWalkFilterResults}.
 */
public class GraphNodeIterable extends AbstractLazySpliterable<CoreInstance>
{
    private final ImmutableList<CoreInstance> startingNodes;
    private final BiPredicate<? super CoreInstance, ? super String> keyFilter;
    private final Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter;

    private GraphNodeIterable(ImmutableList<CoreInstance> startingNodes, BiPredicate<? super CoreInstance, ? super String> keyFilter, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
    {
        this.startingNodes = Lists.immutable.withAll(startingNodes);
        this.keyFilter = (keyFilter == null) ? (n, k) -> true : keyFilter;
        this.nodeFilter = nodeFilter;
    }

    @Override
    public int size()
    {
        return this.startingNodes.isEmpty() ? 0 : super.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.startingNodes.isEmpty() || ((this.nodeFilter != null) && super.isEmpty());
    }
    
    @Override
    public CoreInstance getAny()
    {
        if (this.startingNodes.isEmpty())
        {
            return null;
        }
        if (this.nodeFilter == null)
        {
            return this.startingNodes.getAny();
        }
        return super.getAny();
    }

    @Override
    public CoreInstance getFirst()
    {
        if (this.startingNodes.isEmpty())
        {
            return null;
        }
        if (this.nodeFilter == null)
        {
            return this.startingNodes.getFirst();
        }
        return super.getFirst();
    }

    @Override
    public boolean contains(Object object)
    {
        return (object instanceof CoreInstance) && super.contains(object);
    }

    @Override
    public LazyIterable<CoreInstance> distinct()
    {
        return this;
    }

    @Override
    public MutableList<CoreInstance> toList()
    {
        if (this.startingNodes.isEmpty())
        {
            return Lists.mutable.empty();
        }
        if (this.nodeFilter == null)
        {
            return Lists.mutable.withAll(computeClosure(this.startingNodes, this.keyFilter));
        }
        return computeClosure(this.startingNodes, this.keyFilter, this.nodeFilter, Lists.mutable.empty());
    }

    @Override
    public MutableSet<CoreInstance> toSet()
    {
        if (this.startingNodes.isEmpty())
        {
            return Sets.mutable.empty();
        }
        if (this.nodeFilter == null)
        {
            return computeClosure(this.startingNodes, this.keyFilter);
        }
        return computeClosure(this.startingNodes, this.keyFilter, this.nodeFilter, Sets.mutable.empty());
    }

    @Override
    public Object[] toArray()
    {
        if (this.startingNodes.isEmpty())
        {
            return new Object[0];
        }
        if (this.nodeFilter == null)
        {
            return computeClosure(this.startingNodes, this.keyFilter).toArray();
        }
        return computeClosure(this.startingNodes, this.keyFilter, this.nodeFilter, Lists.mutable.empty()).toArray();
    }

    @Override
    public <E> E[] toArray(E[] array)
    {
        if (this.startingNodes.isEmpty())
        {
            if (array.length > 0)
            {
                array[0] = null;
            }
            return array;
        }
        if (this.nodeFilter == null)
        {
            return computeClosure(this.startingNodes, this.keyFilter).toArray(array);
        }
        return computeClosure(this.startingNodes, this.keyFilter, this.nodeFilter, Lists.mutable.empty()).toArray(array);
    }

    @Override
    public Spliterator<CoreInstance> spliterator()
    {
        return new GraphNodeSpliterator(this.startingNodes, this.keyFilter, this.nodeFilter);
    }

    @Deprecated
    public static GraphNodeIterable fromNode(CoreInstance startingNode)
    {
        return builder().withStartingNode(startingNode).build();
    }

    @Deprecated
    public static GraphNodeIterable fromNode(CoreInstance startingNode, Function<? super CoreInstance, ? extends GraphWalkFilterResult> filter)
    {
        return builder().withStartingNode(startingNode).withNodeFilter(filter).build();
    }

    @Deprecated
    public static GraphNodeIterable fromNodes(CoreInstance... startingNodes)
    {
        return builder().withStartingNodes(startingNodes).build();
    }

    @Deprecated
    public static GraphNodeIterable fromNodes(Iterable<? extends CoreInstance> startingNodes)
    {
        return builder().withStartingNodes(startingNodes).build();
    }

    @Deprecated
    public static GraphNodeIterable fromNodes(Iterable<? extends CoreInstance> startingNodes, Function<? super CoreInstance, ? extends GraphWalkFilterResult> filter)
    {
        return builder().withStartingNodes(startingNodes).withNodeFilter(filter).build();
    }

    public static GraphNodeIterable fromModelRepository(ModelRepository repository)
    {
        return fromModelRepository(repository, null, null);
    }

    public static GraphNodeIterable fromModelRepository(ModelRepository repository, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
    {
        return fromModelRepository(repository, null, nodeFilter);
    }

    public static GraphNodeIterable fromModelRepository(ModelRepository repository, BiPredicate<? super CoreInstance, ? super String> keyFilter, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
    {
        return builder()
                .withStartingNodes(repository.getTopLevels())
                .withKeyFilter(keyFilter)
                .withNodeFilter(nodeFilter)
                .build();
    }

    public static MutableSet<CoreInstance> allInstancesFromRepository(ModelRepository repository)
    {
        return allConnectedInstances(repository.getTopLevels());
    }

    public static MutableSet<CoreInstance> allConnectedInstances(Iterable<? extends CoreInstance> startingNodes)
    {
        return allConnectedInstances(startingNodes, null, null);
    }

    public static MutableSet<CoreInstance> allConnectedInstances(Iterable<? extends CoreInstance> startingNodes, BiPredicate<? super CoreInstance, ? super String> keyFilter)
    {
        return allConnectedInstances(startingNodes, keyFilter, null);
    }

    public static MutableSet<CoreInstance> allConnectedInstances(Iterable<? extends CoreInstance> startingNodes, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
    {
        return allConnectedInstances(startingNodes, null, nodeFilter);
    }

    public static MutableSet<CoreInstance> allConnectedInstances(Iterable<? extends CoreInstance> startingNodes, BiPredicate<? super CoreInstance, ? super String> keyFilter, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
    {
        return (nodeFilter == null) ?
               computeClosure(startingNodes, keyFilter) :
               computeClosure(startingNodes, keyFilter, nodeFilter, Sets.mutable.empty());
    }

    private static MutableSet<CoreInstance> computeClosure(Iterable<? extends CoreInstance> startingNodes, BiPredicate<? super CoreInstance, ? super String> keyFilter)
    {
        GraphNodeSpliterator spliterator = new GraphNodeSpliterator(startingNodes, keyFilter, null);
        spliterator.forEachRemaining(n ->
        {
            // Do nothing: collect instances by side effect
        });
        return spliterator.visited;
    }

    private static <T extends Collection<? super CoreInstance>> T computeClosure(Iterable<? extends CoreInstance> startingNodes, BiPredicate<? super CoreInstance, ? super String> keyFilter, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter, T target)
    {
        GraphNodeSpliterator spliterator = new GraphNodeSpliterator(startingNodes, keyFilter, nodeFilter);
        spliterator.forEachRemaining(target::add);
        return target;
    }

    private static class GraphNodeSpliterator implements Spliterator<CoreInstance>
    {
        private final Deque<CoreInstance> deque;
        private MutableSet<CoreInstance> visited;
        private final BiPredicate<? super CoreInstance, ? super String> keyFilter;
        private final Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter;

        private GraphNodeSpliterator(Deque<CoreInstance> deque, MutableSet<CoreInstance> visited, BiPredicate<? super CoreInstance, ? super String> keyFilter, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
        {
            this.deque = deque;
            this.visited = visited;
            this.keyFilter = (keyFilter == null) ? (n, k) -> true : keyFilter;
            this.nodeFilter = nodeFilter;
        }

        private GraphNodeSpliterator(Iterable<? extends CoreInstance> startingNodes, BiPredicate<? super CoreInstance, ? super String> keyFilter, Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
        {
            this(Iterate.addAllTo(startingNodes, new ArrayDeque<>()), Sets.mutable.empty(), keyFilter, nodeFilter);
        }

        @Override
        public boolean tryAdvance(Consumer<? super CoreInstance> action)
        {
            while (!this.deque.isEmpty())
            {
                CoreInstance node = this.deque.pollFirst();
                if (this.visited.add(node))
                {
                    GraphWalkFilterResult filterResult = filterNode(node);
                    if (filterResult.shouldContinue())
                    {
                        node.getKeys().forEach(key ->
                        {
                            if (this.keyFilter.test(node, key))
                            {
                                node.getValueForMetaPropertyToMany(key).forEach(v ->
                                {
                                    if (!this.visited.contains(v))
                                    {
                                        this.deque.addLast(v);
                                    }
                                });
                            }
                        });
                    }
                    if (filterResult.shouldAccept())
                    {
                        action.accept(node);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Spliterator<CoreInstance> trySplit()
        {
            if (this.deque.size() < 2)
            {
                return null;
            }

            // If we are going to split, we need to make sure the visited set is synchronized
            if (!(this.visited instanceof SynchronizedMutableSet))
            {
                this.visited = SynchronizedMutableSet.of(this.visited, this.visited);
            }
            return new GraphNodeSpliterator(splitDeque(this.deque), this.visited, this.keyFilter, this.nodeFilter);
        }

        @Override
        public long estimateSize()
        {
            return this.deque.isEmpty() ? 0L : Long.MAX_VALUE;
        }

        @Override
        public long getExactSizeIfKnown()
        {
            return this.deque.isEmpty() ? 0L : -1L;
        }

        @Override
        public int characteristics()
        {
            return NONNULL | DISTINCT;
        }

        private GraphWalkFilterResult filterNode(CoreInstance node)
        {
            if (this.nodeFilter != null)
            {
                GraphWalkFilterResult result = this.nodeFilter.apply(node);
                if (result != null)
                {
                    return result;
                }
            }
            return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final MutableList<CoreInstance> startNodes = Lists.mutable.empty();
        private BiPredicate<? super CoreInstance, ? super String> keyFilter;
        private Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter;

        private Builder()
        {
        }

        public Builder withKeyFilter(BiPredicate<? super CoreInstance, ? super String> keyFilter)
        {
            this.keyFilter = keyFilter;
            return this;
        }

        public Builder withNodeFilter(Function<? super CoreInstance, ? extends GraphWalkFilterResult> nodeFilter)
        {
            this.nodeFilter = nodeFilter;
            return this;
        }

        public Builder withStartingNode(CoreInstance node)
        {
            this.startNodes.add(Objects.requireNonNull(node));
            return this;
        }

        public Builder withStartingNodes(Iterable<? extends CoreInstance> nodes)
        {
            nodes.forEach(this::withStartingNode);
            return this;
        }

        public Builder withStartingNodes(CoreInstance... startingNodes)
        {
            return withStartingNodes(Arrays.asList(startingNodes));
        }

        public GraphNodeIterable build()
        {
            return new GraphNodeIterable(this.startNodes.toImmutable(), this.keyFilter, this.nodeFilter);
        }
    }
}
