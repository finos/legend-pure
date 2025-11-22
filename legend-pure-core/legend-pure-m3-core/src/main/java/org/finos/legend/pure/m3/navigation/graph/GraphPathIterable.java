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

package org.finos.legend.pure.m3.navigation.graph;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.AbstractLazySpliterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.finos.legend.pure.m4.tools.GraphWalkFilters;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class GraphPathIterable extends AbstractLazySpliterable<ResolvedGraphPath>
{
    private final ImmutableSet<ResolvedGraphPath> startPaths;
    private final Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> pathFilter;
    private final BiPredicate<? super ResolvedGraphPath, ? super String> propertyFilter;

    private GraphPathIterable(ImmutableSet<ResolvedGraphPath> startPaths, Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> pathFilter, BiPredicate<? super ResolvedGraphPath, ? super String> propertyFilter)
    {
        this.startPaths = startPaths;
        this.pathFilter = pathFilter;
        this.propertyFilter = propertyFilter;
    }

    @Override
    public boolean isEmpty()
    {
        return this.startPaths.isEmpty() || ((this.pathFilter != null) && super.isEmpty());
    }

    @Override
    public ResolvedGraphPath getAny()
    {
        if (this.startPaths.isEmpty())
        {
            return null;
        }
        if (this.pathFilter == null)
        {
            return this.startPaths.getAny();
        }
        return super.getAny();
    }

    @Override
    public ResolvedGraphPath getFirst()
    {
        return this.startPaths.isEmpty() ? null : super.getFirst();
    }

    @Override
    public boolean contains(Object object)
    {
        if (!(object instanceof ResolvedGraphPath))
        {
            return false;
        }

        ResolvedGraphPath rgp = (ResolvedGraphPath) object;
        if ((this.pathFilter == null) && this.startPaths.contains(rgp))
        {
            return true;
        }

        ImmutableSet<ResolvedGraphPath> possibleStarts = this.startPaths.select(rgp::startsWith);
        if (possibleStarts.isEmpty())
        {
            return false;
        }

        Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> startsWithFilter = p -> !rgp.startsWith(p) ? GraphWalkFilterResult.REJECT_AND_STOP : null;
        Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> newFilter = (this.pathFilter == null) ? startsWithFilter : GraphWalkFilters.conjoin(Lists.immutable.with(startsWithFilter, this.pathFilter));
        return shortCircuit(new GraphPathIterable(possibleStarts, newFilter, this.propertyFilter).spliterator(), rgp::equals, true, true, false);
    }

    @Override
    public LazyIterable<ResolvedGraphPath> distinct()
    {
        return this;
    }

    @Override
    public Spliterator<ResolvedGraphPath> spliterator()
    {
        return new GraphPathSpliterator();
    }

    private boolean isStartPath(ResolvedGraphPath resolvedGraphPath)
    {
        return this.startPaths.contains(resolvedGraphPath);
    }

    private GraphWalkFilterResult filterPath(ResolvedGraphPath resolvedGraphPath)
    {
        if (this.pathFilter != null)
        {
            GraphWalkFilterResult result = this.pathFilter.apply(resolvedGraphPath);
            if (result != null)
            {
                return result;
            }
        }
        return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
    }

    private boolean filterProperty(ResolvedGraphPath resolvedGraphPath, String property)
    {
        return (this.propertyFilter == null) || this.propertyFilter.test(resolvedGraphPath, property);
    }

    private class GraphPathSpliterator implements Spliterator<ResolvedGraphPath>
    {
        private final Deque<ResolvedGraphPath> deque;

        private GraphPathSpliterator(Deque<ResolvedGraphPath> deque)
        {
            this.deque = deque;
        }

        private GraphPathSpliterator()
        {
            this(new ArrayDeque<>(GraphPathIterable.this.startPaths.castToSet()));
        }

        @Override
        public boolean tryAdvance(Consumer<? super ResolvedGraphPath> action)
        {
            while (!this.deque.isEmpty())
            {
                ResolvedGraphPath resolvedPath = this.deque.pollFirst();
                GraphWalkFilterResult filterResult = filterPath(resolvedPath);
                if (filterResult.shouldContinue())
                {
                    continueFromPath(resolvedPath);
                }
                if (filterResult.shouldAccept())
                {
                    action.accept(resolvedPath);
                    return true;
                }
            }
            return false;
        }

        @Override
        public Spliterator<ResolvedGraphPath> trySplit()
        {
            return (this.deque.size() < 2) ? null : new GraphPathSpliterator(splitDeque(this.deque));
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
            return DISTINCT | NONNULL;
        }

        private void continueFromPath(ResolvedGraphPath resolvedPath)
        {
            GraphPath path = resolvedPath.getGraphPath();
            ImmutableList<CoreInstance> pathNodeList = resolvedPath.getResolvedNodes();
            CoreInstance finalNode = pathNodeList.getLast();
            Collection<CoreInstance> pathNodeSet = (pathNodeList.size() > 8) ? pathNodeList.toSet() : pathNodeList.castToList();
            finalNode.getKeys().forEach(key ->
            {
                if (filterProperty(resolvedPath, key))
                {
                    ListIterable<? extends CoreInstance> values = finalNode.getValueForMetaPropertyToMany(key);
                    if (values.size() == 1)
                    {
                        CoreInstance value = values.get(0);
                        if (!pathNodeSet.contains(value))
                        {
                            possiblyEnqueue(path.withToOneProperty(key, false), pathNodeList.newWith(value));
                        }
                    }
                    else if (values.notEmpty())
                    {
                        values.forEachWithIndex((value, i) ->
                        {
                            if (!pathNodeSet.contains(value))
                            {
                                possiblyEnqueue(path.withToManyPropertyValueAtIndex(key, i, false), pathNodeList.newWith(value));
                            }
                        });
                    }
                }
            });
        }

        private void possiblyEnqueue(GraphPath path, ImmutableList<CoreInstance> resolvedNodes)
        {
            ResolvedGraphPath resolvedGraphPath = new ResolvedGraphPath(path, resolvedNodes);
            if (!isStartPath(resolvedGraphPath))
            {
                this.deque.addLast(resolvedGraphPath);
            }
        }
    }

    public static GraphPathIterable build(String startNodePath, Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> pathFilter, BiPredicate<? super ResolvedGraphPath, ? super String> propertyFilter, ProcessorSupport processorSupport)
    {
        return builder(processorSupport)
                .withStartNodePath(startNodePath)
                .withPathFilter(pathFilter)
                .withPropertyFilter(propertyFilter)
                .build();
    }

    public static GraphPathIterable build(CoreInstance startNode, Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> pathFilter, BiPredicate<? super ResolvedGraphPath, ? super String> propertyFilter, ProcessorSupport processorSupport)
    {
        return builder(processorSupport)
                .withStartNode(startNode)
                .withPathFilter(pathFilter)
                .withPropertyFilter(propertyFilter)
                .build();
    }

    public static Builder builder(ProcessorSupport processorSupport)
    {
        return new Builder(processorSupport);
    }

    public static class Builder
    {
        private final MutableSet<ResolvedGraphPath> startPaths = Sets.mutable.empty();
        private Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> pathFilter = null;
        private BiPredicate<? super ResolvedGraphPath, ? super String> propertyFilter = null;
        private final ProcessorSupport processorSupport;

        private Builder(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        public Builder withStartPath(GraphPath path)
        {
            this.startPaths.add(path.resolveFully(this.processorSupport));
            return this;
        }

        public Builder withStartPaths(GraphPath... paths)
        {
            return withStartPaths(Arrays.asList(paths));
        }

        public Builder withStartPaths(Iterable<? extends GraphPath> paths)
        {
            paths.forEach(this::withStartPath);
            return this;
        }

        public Builder withStartPath(String pathDescription)
        {
            return withStartPath(GraphPath.parse(pathDescription));
        }

        public Builder withStartPaths(String... pathDescriptions)
        {
            return withStartPathDescriptions(Arrays.asList(pathDescriptions));
        }

        public Builder withStartPathDescriptions(Iterable<? extends String> pathDescriptions)
        {
            pathDescriptions.forEach(this::withStartPath);
            return this;
        }

        public Builder withStartNode(CoreInstance element)
        {
            if (!GraphPath.isPackagedOrTopLevel(element, this.processorSupport))
            {
                throw new IllegalArgumentException("Invalid start node: " + element);
            }
            String path = PackageableElement.getUserPathForPackageableElement(element);
            this.startPaths.add(new ResolvedGraphPath(GraphPath.buildPath(path, false), Lists.immutable.with(element)));
            return this;
        }

        public Builder withStartNodes(CoreInstance... elements)
        {
            return withStartNodes(Arrays.asList(elements));
        }

        public Builder withStartNodes(Iterable<? extends CoreInstance> elements)
        {
            elements.forEach(this::withStartNode);
            return this;
        }

        public Builder withStartNodePath(String path)
        {
            return withStartPath(GraphPath.buildPath(path));
        }

        public Builder withStartNodePaths(String... paths)
        {
            return withStartNodePaths(Arrays.asList(paths));
        }

        public Builder withStartNodePaths(Iterable<String> paths)
        {
            paths.forEach(this::withStartNodePath);
            return this;
        }

        public Builder withPathFilter(Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> pathFilter)
        {
            this.pathFilter = pathFilter;
            return this;
        }

        public Builder withPropertyFilter(BiPredicate<? super ResolvedGraphPath, ? super String> propertyFilter)
        {
            this.propertyFilter = propertyFilter;
            return this;
        }

        public GraphPathIterable build()
        {
            return new GraphPathIterable(this.startPaths.toImmutable(), this.pathFilter, this.propertyFilter);
        }
    }
}
