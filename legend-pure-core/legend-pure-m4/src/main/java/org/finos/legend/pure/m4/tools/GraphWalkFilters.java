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

package org.finos.legend.pure.m4.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

import java.util.Objects;
import java.util.function.Function;

public class GraphWalkFilters
{
    @SuppressWarnings("unchecked")
    public static <T> Function<? super T, ? extends GraphWalkFilterResult> conjoin(Iterable<? extends Function<? super T, ? extends GraphWalkFilterResult>> filters)
    {
        MutableList<Function<? super T, ? extends GraphWalkFilterResult>> list = Lists.mutable.empty();
        filters.forEach(filter ->
        {
            if (filter instanceof ConjoinedFilter)
            {
                list.addAll(((MergedFilter<? super T>) filter).filters.castToList());
            }
            else
            {
                list.add(Objects.requireNonNull(filter));
            }
        });
        switch (list.size())
        {
            case 0:
            {
                return t -> GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
            }
            case 1:
            {
                return orDefault(list.get(0), GraphWalkFilterResult.ACCEPT_AND_CONTINUE);
            }
            default:
            {
                return new ConjoinedFilter<>(list.toImmutable());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<? super T, ? extends GraphWalkFilterResult> disjoin(Iterable<? extends Function<? super T, ? extends GraphWalkFilterResult>> filters)
    {
        MutableList<Function<? super T, ? extends GraphWalkFilterResult>> list = Lists.mutable.empty();
        filters.forEach(filter ->
        {
            if (filter instanceof DisjoinedFilter)
            {
                list.addAll(((MergedFilter<? super T>) filter).filters.castToList());
            }
            else
            {
                list.add(Objects.requireNonNull(filter));
            }
        });
        switch (list.size())
        {
            case 0:
            {
                return t -> GraphWalkFilterResult.REJECT_AND_STOP;
            }
            case 1:
            {
                return orDefault(list.get(0), GraphWalkFilterResult.REJECT_AND_STOP);
            }
            default:
            {
                return new DisjoinedFilter<>(list.toImmutable());
            }
        }
    }

    public static <T> Function<? super T, ? extends GraphWalkFilterResult> first(Iterable<? extends Function<? super T, ? extends GraphWalkFilterResult>> filters, GraphWalkFilterResult defaultValue)
    {
        Objects.requireNonNull(defaultValue);
        MutableList<Function<? super T, ? extends GraphWalkFilterResult>> list = Lists.mutable.empty();
        filters.forEach(f -> list.add(Objects.requireNonNull(f)));
        switch (list.size())
        {
            case 0:
            {
                return t -> defaultValue;
            }
            case 1:
            {
                return orDefault(list.get(0), defaultValue);
            }
            default:
            {
                return new FirstFilter<>(list.toImmutable(), defaultValue);
            }
        }
    }

    public static <T> Function<? super T, ? extends GraphWalkFilterResult> orDefault(Function<? super T, ? extends GraphWalkFilterResult> filter, GraphWalkFilterResult defaultValue)
    {
        Objects.requireNonNull(filter);
        Objects.requireNonNull(defaultValue);
        return t ->
        {
            GraphWalkFilterResult result = filter.apply(t);
            return (result == null) ? defaultValue : result;
        };
    }

    private abstract static class MergedFilter<T> implements Function<T, GraphWalkFilterResult>
    {
        private final ImmutableList<Function<? super T, ? extends GraphWalkFilterResult>> filters;

        private MergedFilter(ImmutableList<Function<? super T, ? extends GraphWalkFilterResult>> filters)
        {
            this.filters = filters;
        }

        @Override
        public final GraphWalkFilterResult apply(T t)
        {
            GraphWalkFilterResult merged = null;
            for (Function<? super T, ? extends GraphWalkFilterResult> filter : this.filters)
            {
                GraphWalkFilterResult result = filter.apply(t);
                if (result != null)
                {
                    merged = (merged == null) ? result : merge(merged, result);
                    if (isTerminal(merged))
                    {
                        return merged;
                    }
                }
            }
            return (merged == null) ? defaultResult() : merged;
        }

        protected abstract GraphWalkFilterResult merge(GraphWalkFilterResult result1, GraphWalkFilterResult result2);

        protected abstract boolean isTerminal(GraphWalkFilterResult result);

        protected abstract GraphWalkFilterResult defaultResult();
    }

    private static class ConjoinedFilter<T> extends MergedFilter<T>
    {
        private ConjoinedFilter(ImmutableList<Function<? super T, ? extends GraphWalkFilterResult>> filters)
        {
            super(filters);
        }

        @Override
        protected GraphWalkFilterResult merge(GraphWalkFilterResult result1, GraphWalkFilterResult result2)
        {
            return result1.conjoin(result2);
        }

        @Override
        protected boolean isTerminal(GraphWalkFilterResult result)
        {
            return result == GraphWalkFilterResult.REJECT_AND_STOP;
        }

        @Override
        protected GraphWalkFilterResult defaultResult()
        {
            return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
        }
    }

    private static class DisjoinedFilter<T> extends MergedFilter<T>
    {
        private DisjoinedFilter(ImmutableList<Function<? super T, ? extends GraphWalkFilterResult>> filters)
        {
            super(filters);
        }

        @Override
        protected GraphWalkFilterResult merge(GraphWalkFilterResult result1, GraphWalkFilterResult result2)
        {
            return result1.disjoin(result2);
        }

        @Override
        protected boolean isTerminal(GraphWalkFilterResult result)
        {
            return result == GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
        }

        @Override
        protected GraphWalkFilterResult defaultResult()
        {
            return GraphWalkFilterResult.REJECT_AND_STOP;
        }
    }

    private static class FirstFilter<T> extends MergedFilter<T>
    {
        private final GraphWalkFilterResult defaultValue;

        private FirstFilter(ImmutableList<Function<? super T, ? extends GraphWalkFilterResult>> filters, GraphWalkFilterResult defaultValue)
        {
            super(filters);
            this.defaultValue = defaultValue;
        }

        @Override
        protected GraphWalkFilterResult merge(GraphWalkFilterResult result1, GraphWalkFilterResult result2)
        {
            return (result1 == null) ? result2 : result1;
        }

        @Override
        protected boolean isTerminal(GraphWalkFilterResult result)
        {
            return result != null;
        }

        @Override
        protected GraphWalkFilterResult defaultResult()
        {
            return this.defaultValue;
        }
    }
}
