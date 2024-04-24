// Copyright 2024 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.finos.legend.pure.m4.tools.GraphWalkFilters;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class GraphPathFilters
{
    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getPathLengthFilter(IntFunction<? extends GraphWalkFilterResult> lengthFunction)
    {
        return resolvedGraphPath -> lengthFunction.apply(resolvedGraphPath.getGraphPath().getEdgeCount());
    }

    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getMaxPathLengthFilter(int maxPathLength)
    {
        return getPathLengthFilter(l -> (l > maxPathLength) ? GraphWalkFilterResult.REJECT_AND_STOP : ((l == maxPathLength) ? GraphWalkFilterResult.ACCEPT_AND_STOP : GraphWalkFilterResult.ACCEPT_AND_CONTINUE));
    }

    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getStopAtNodeFilter(CoreInstance node)
    {
        return getStopAtNodeFilter(node::equals);
    }

    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getStopAtNodeFilter(CoreInstance... nodes)
    {
        return (nodes.length == 1) ? getStopAtNodeFilter(nodes[0]) : getStopAtNodeFilter(Sets.mutable.with(nodes));
    }

    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getStopAtNodeFilter(Iterable<? extends CoreInstance> nodes)
    {
        Set<? extends CoreInstance> set = (nodes instanceof Set) ? (Set<? extends CoreInstance>) nodes : Sets.mutable.withAll(nodes);
        return getStopAtNodeFilter(set::contains);
    }

    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getStopAtNodeFilter(Predicate<? super CoreInstance> predicate)
    {
        return resolvedGraphPath -> predicate.test(resolvedGraphPath.getLastResolvedNode()) ? GraphWalkFilterResult.ACCEPT_AND_STOP : GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
    }

    public static Function<ResolvedGraphPath, GraphWalkFilterResult> getStopAtPackagedOrTopLevel(ProcessorSupport processorSupport)
    {
        return rgp -> ((rgp.getResolvedNodes().size() > 1) && GraphPath.isPackagedOrTopLevel(rgp.getLastResolvedNode(), processorSupport)) ? GraphWalkFilterResult.ACCEPT_AND_STOP : GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final MutableList<Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult>> filters = Lists.mutable.empty();

        private Builder()
        {
        }

        public Builder withMaxPathLength(int length)
        {
            return withFilter(getMaxPathLengthFilter(length));
        }

        public Builder stopAtNode(CoreInstance node)
        {
            return withFilter(getStopAtNodeFilter(node));
        }

        public Builder stopAtNode(CoreInstance... nodes)
        {
            return withFilter(getStopAtNodeFilter(nodes));
        }

        public Builder stopAtNode(Iterable<? extends CoreInstance> nodes)
        {
            return withFilter(getStopAtNodeFilter(nodes));
        }

        public Builder stopAtNode(Predicate<? super CoreInstance> predicate)
        {
            return withFilter(getStopAtNodeFilter(predicate));
        }

        public Builder stopAtPackagedOrTopLevel(ProcessorSupport processorSupport)
        {
            return withFilter(getStopAtPackagedOrTopLevel(processorSupport));
        }

        public Builder withFilter(Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> filter)
        {
            this.filters.add(Objects.requireNonNull(filter));
            return this;
        }

        public Builder withFilters(Iterable<? extends Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult>> filters)
        {
            filters.forEach(this::withFilter);
            return this;
        }

        public Function<? super ResolvedGraphPath, ? extends GraphWalkFilterResult> build()
        {
            return GraphWalkFilters.conjoin(this.filters);
        }
    }
}
