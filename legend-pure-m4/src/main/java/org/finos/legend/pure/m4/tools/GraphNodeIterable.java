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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An iterable that iterates through the nodes of a graph, starting from a given set of nodes and traversing to
 * connected nodes. The traversal of the graph can be controlled by providing a
 * {@link java.util.function.Function function} from nodes to {@link NodeFilterResult NodeFilterResults}.
 */
public class GraphNodeIterable extends AbstractLazyIterable<CoreInstance>
{
    private final ImmutableList<CoreInstance> startingNodes;
    private final Function<? super CoreInstance, NodeFilterResult> filter;

    private GraphNodeIterable(Iterable<? extends CoreInstance> startingNodes, Function<? super CoreInstance, NodeFilterResult> filter)
    {
        this.startingNodes = Lists.immutable.withAll(startingNodes);
        this.filter = filter;
    }

    @Override
    public Iterator<CoreInstance> iterator()
    {
        return new GraphNodeIterator(this.startingNodes, this.filter);
    }

    @Override
    public Spliterator<CoreInstance> spliterator()
    {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.DISTINCT | Spliterator.NONNULL);
    }

    @Override
    public void each(Procedure<? super CoreInstance> procedure)
    {
        for (CoreInstance node : this)
        {
            procedure.value(node);
        }
    }

    @Override
    public void forEach(Consumer<? super CoreInstance> consumer)
    {
        for (CoreInstance node : this)
        {
            consumer.accept(node);
        }
    }

    public static GraphNodeIterable fromNode(CoreInstance startingNode)
    {
        return fromNodes(startingNode, null);
    }

    public static GraphNodeIterable fromNode(CoreInstance startingNode, Function<? super CoreInstance, NodeFilterResult> filter)
    {
        return fromNodes(Lists.immutable.with(startingNode), filter);
    }

    public static GraphNodeIterable fromNodes(CoreInstance... startingNodes)
    {
        return fromNodes(Lists.immutable.with(startingNodes));
    }

    public static GraphNodeIterable fromNodes(Iterable<? extends CoreInstance> startingNodes)
    {
        return fromNodes(startingNodes, null);
    }

    public static GraphNodeIterable fromNodes(Iterable<? extends CoreInstance> startingNodes, Function<? super CoreInstance, NodeFilterResult> filter)
    {
        return new GraphNodeIterable(Objects.requireNonNull(startingNodes, "Starting nodes may not be null"), filter);
    }

    public static GraphNodeIterable fromModelRepository(ModelRepository repository)
    {
        return fromModelRepository(repository, null);
    }

    public static GraphNodeIterable fromModelRepository(ModelRepository repository, Function<? super CoreInstance, NodeFilterResult> filter)
    {
        return fromNodes(repository.getTopLevels(), filter);
    }

    public static MutableSet<CoreInstance> allInstancesFromRepository(ModelRepository repository)
    {
        return allConnectedInstances(repository.getTopLevels());
    }

    public static MutableSet<CoreInstance> allConnectedInstances(Iterable<? extends CoreInstance> startingNodes)
    {
        return allConnectedInstances(startingNodes, null);
    }

    public static MutableSet<CoreInstance> allConnectedInstances(Iterable<? extends CoreInstance> startingNodes, Function<? super CoreInstance, NodeFilterResult> filter)
    {
        GraphNodeIterator iterator = new GraphNodeIterator(startingNodes, filter);
        while (iterator.hasNext())
        {
            iterator.next();
        }
        return iterator.visited;
    }

    private static class GraphNodeIterator implements Iterator<CoreInstance>
    {
        private final Deque<CoreInstance> deque;
        private final MutableSet<CoreInstance> visited;
        private final Function<? super CoreInstance, NodeFilterResult> filter;
        private CoreInstance next;

        private GraphNodeIterator(Iterable<? extends CoreInstance> startingNodes, Function<? super CoreInstance, NodeFilterResult> filter)
        {
            this.deque = Iterate.addAllTo(startingNodes, new ArrayDeque<>());
            this.visited = Sets.mutable.ofInitialCapacity(Math.max(this.deque.size(), 16));
            this.filter = filter;
            this.next = findNextNode();
        }

        @Override
        public boolean hasNext()
        {
            return this.next != null;
        }

        @Override
        public CoreInstance next()
        {
            CoreInstance node = this.next;
            if (node == null)
            {
                throw new NoSuchElementException();
            }
            this.next = findNextNode();
            return node;
        }

        private CoreInstance findNextNode()
        {
            while (!this.deque.isEmpty())
            {
                CoreInstance node = this.deque.pollFirst();
                if (this.visited.add(node))
                {
                    NodeFilterResult filterResult = filter(node);
                    if (filterResult.cont)
                    {
                        node.getKeys().forEach(key -> Iterate.addAllIterable(node.getValueForMetaPropertyToMany(key), this.deque));
                    }
                    if (filterResult.accept)
                    {
                        return node;
                    }
                }
            }
            return null;
        }

        private NodeFilterResult filter(CoreInstance node)
        {
            if (this.filter != null)
            {
                NodeFilterResult result = this.filter.apply(node);
                if (result != null)
                {
                    return result;
                }
            }
            return NodeFilterResult.ACCEPT_AND_CONTINUE;
        }
    }

    /**
     * Node filter result, which controls which nodes are returned during iteration and how graph traversal proceeds.
     * The default behavior is {@link #ACCEPT_AND_CONTINUE}.
     */
    public enum NodeFilterResult
    {
        /**
         * Accept the node for iteration, and continue on to connected nodes.
         */
        ACCEPT_AND_CONTINUE(true, true),

        /**
         * Accept the node for iteration, but do not continue on to connected nodes. Note that connected nodes may still
         * be reached by other paths.
         */
        ACCEPT_AND_STOP(true, false),

        /**
         * Reject the node for iteration, but continue on to connected nodes. This means that the node will not be
         * returned as part of iteration. Note that the rejection is persistent, even if the node is reached by other
         * paths.
         */
        REJECT_AND_CONTINUE(false, true),

        /**
         * Reject the node for iteration, and do not continue on to connected nodes. This means both that the node will
         * not be returned as part of iteration and that graph traversal will not continue on to connected nodes (though
         * they may still be reached by other paths). Note that the rejection is persistent, even if the node is reached
         * by other paths.
         */
        REJECT_AND_STOP(false, false);

        private final boolean accept;
        private final boolean cont;

        NodeFilterResult(boolean accept, boolean cont)
        {
            this.accept = accept;
            this.cont = cont;
        }
    }
}