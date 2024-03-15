// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.compiler.postprocessing.observer;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMapWithHashingStrategy;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class TimingPostProcessorObserver implements PostProcessorObserver
{
    private final MutableObjectLongMap<CoreInstance> durations = ObjectLongHashMapWithHashingStrategy.newMap(HashingStrategies.identityStrategy());

    protected TimingPostProcessorObserver()
    {
    }

    @Override
    public final void startProcessing(CoreInstance instance)
    {
        long startNanoTime = System.nanoTime();
        noteProcessingStart(instance, startNanoTime);
    }

    @Override
    public final void finishProcessing(CoreInstance instance)
    {
        long endNanoTime = System.nanoTime();
        long duration = noteProcessingEnd(instance, endNanoTime);
        recordDuration(instance, duration);
    }

    @Override
    public final void finishProcessing(CoreInstance instance, Exception e)
    {
        long endNanoTime = System.nanoTime();
        long duration = noteProcessingEnd(instance, endNanoTime);
        recordDuration(instance, duration);
    }

    /**
     * Note the start of processing for an instance.
     *
     * @param instance      instance to be processed
     * @param startNanoTime start nano-time
     */
    protected abstract void noteProcessingStart(CoreInstance instance, long startNanoTime);

    /**
     * Note the end of processing for an instance, and return the duration in nanoseconds. Positive values will be added
     * to any duration that has already been recorded. Non-positive values will be ignored.
     *
     * @param instance    processed instance
     * @param endNanoTime end nano-time
     * @return duration in nanoseconds
     */
    protected abstract long noteProcessingEnd(CoreInstance instance, long endNanoTime);

    /**
     * Record a processing duration for an instance. Positive values are added to any duration that has already been
     * recorded. Non-positive values are ignored.
     *
     * @param instance      processed instance
     * @param durationNanos duration in nanoseconds
     */
    protected void recordDuration(CoreInstance instance, long durationNanos)
    {
        if (durationNanos > 0)
        {
            this.durations.addToValue(instance, durationNanos);
        }
    }

    /**
     * Get the duration in nanoseconds spent processing an instance. Returns -1 if the instance was not observed.
     *
     * @param instance instance
     * @return duration in nanoseconds or -1 if not present
     */
    public long getDurationInNanos(CoreInstance instance)
    {
        return this.durations.getIfAbsent(instance, -1L);
    }

    /**
     * Get all observed durations. The result is not sorted.
     *
     * @return all durations (unsorted)
     */
    public MutableList<Duration> getAllDurations()
    {
        MutableList<Duration> list = Lists.mutable.ofInitialCapacity(this.durations.size());
        forEachDuration(list::add);
        return list;
    }

    /**
     * Get the longest duration. If there is more than one, one is selected arbitrarily. If there are no durations,
     * null is returned.
     *
     * @return longest duration or null
     */
    public Duration getLongestDuration()
    {
        DurationBuilder builder = new DurationBuilder();
        this.durations.forEachKeyValue((instance, duration) ->
        {
            if (builder.isUnset() || (duration > builder.durationNanos))
            {
                builder.set(instance, duration);
            }
        });
        return builder.build();
    }

    /**
     * Get the n longest durations, sorted from longest to shortest. If there are fewer than n durations in total, all
     * durations will be returned. In case of multiple instances with the same duration, ordering is arbitrary. If such
     * ties occur around the nth place, it is arbitrary which will be included in the result.
     *
     * @param n number of durations
     * @return n longest durations
     */
    public MutableList<Duration> getLongestDurations(int n)
    {
        return getTopDurations(n, true);
    }

    /**
     * Get the shortest duration. If there is more than one, one is selected arbitrarily. If there are no durations,
     * null is returned.
     *
     * @return shortest duration or null
     */
    public Duration getShortestDuration()
    {
        DurationBuilder builder = new DurationBuilder();
        this.durations.forEachKeyValue((instance, duration) ->
        {
            if (builder.isUnset() || (duration < builder.durationNanos))
            {
                builder.set(instance, duration);
            }
        });
        return builder.build();
    }

    /**
     * Get the n shortest durations, sorted from shortest to longest. If there are fewer than n durations in total, all
     * durations will be returned. In case of multiple instances with the same duration, ordering is arbitrary. If such
     * ties occur around the nth place, it is arbitrary which will be included in the result.
     *
     * @param n number of durations
     * @return n shortest durations
     */
    public MutableList<Duration> getShortestDurations(int n)
    {
        return getTopDurations(n, false);
    }

    /**
     * Get all durations at least as long as minDurationNanos. The result is not sorted.
     *
     * @param minDurationNanos minimum duration in nanoseconds
     * @return all durations at least minDurationNanos
     */
    public MutableList<Duration> getDurationsAtLeast(long minDurationNanos)
    {
        MutableList<Duration> list = Lists.mutable.empty();
        this.durations.forEachKeyValue((instance, duration) ->
        {
            if (duration >= minDurationNanos)
            {
                list.add(new Duration(instance, duration));
            }
        });
        return list;
    }

    /**
     * Get all durations no longer than maxDurationNanos. The result is not sorted.
     *
     * @param maxDurationNanos maximum duration in nanoseconds
     * @return all durations at most maxDurationNanos
     */
    public MutableList<Duration> getDurationsAtMost(long maxDurationNanos)
    {
        MutableList<Duration> list = Lists.mutable.empty();
        this.durations.forEachKeyValue((instance, durationNanos) ->
        {
            if (durationNanos <= maxDurationNanos)
            {
                list.add(new Duration(instance, durationNanos));
            }
        });
        return list;
    }

    /**
     * Filter durations by the given predicate.
     *
     * @param predicate filter predicate
     * @return filtered durations
     */
    public MutableList<Duration> filterDurations(Predicate<? super Duration> predicate)
    {
        MutableList<Duration> list = Lists.mutable.empty();
        this.durations.forEachKeyValue((instance, durationNanos) ->
        {
            Duration duration = new Duration(instance, durationNanos);
            if (predicate.test(duration))
            {
                list.add(duration);
            }
        });
        return list;
    }

    private MutableList<Duration> getTopDurations(int n, boolean longerFirst)
    {
        if ((n == 0) || this.durations.isEmpty())
        {
            return Lists.mutable.empty();
        }

        if (n == 1)
        {
            Duration top = longerFirst ? getLongestDuration() : getShortestDuration();
            return Lists.mutable.with(top);
        }

        Comparator<Duration> comparator = longerFirst ? TimingPostProcessorObserver::compareLongerFirst : TimingPostProcessorObserver::compareShorterFirst;
        if ((n < 0) || (n >= this.durations.size()))
        {
            return getAllDurations().sortThis(comparator);
        }

        MutableList<Duration> list = Lists.mutable.ofInitialCapacity(n);
        this.durations.forEachKeyValue((instance, durationNanos) ->
        {
            Duration duration = new Duration(instance, durationNanos);
            if (list.size() < n)
            {
                list.add(duration);
                if (list.size() == n)
                {
                    list.sort(comparator);
                }
            }
            else if (comparator.compare(duration, list.get(n - 1)) < 0)
            {
                int search = Collections.binarySearch(list, duration, comparator);
                int index = (search < 0) ? (-search - 1) : search;
                list.remove(n - 1);
                list.add(index, duration);
            }
        });
        return list;
    }

    public void forEachDuration(Consumer<? super Duration> consumer)
    {
        this.durations.forEachKeyValue((instance, durationNanos) -> consumer.accept(new Duration(instance, durationNanos)));
    }

    public void forEachDuration(boolean longestFirst, Consumer<? super Duration> consumer)
    {
        forEachDuration(longestFirst, -1, consumer);
    }

    public void forEachDuration(boolean longestFirst, int limit, Consumer<? super Duration> consumer)
    {
        if ((limit == 0) || this.durations.isEmpty())
        {
            return;
        }

        if (limit == 1)
        {
            Duration duration = longestFirst ? getLongestDuration() : getShortestDuration();
            consumer.accept(duration);
            return;
        }

        getTopDurations(limit, longestFirst).forEach(consumer);
    }

    public static TimingPostProcessorObserver newObserver()
    {
        return newObserver(false);
    }

    public static TimingPostProcessorObserver newObserver(boolean net)
    {
        return net ? new NetTimingPostProcessorObserver() : new SimpleTimingPostProcessorObserver();
    }

    private static int compareLongerFirst(Duration one, Duration two)
    {
        return Long.compare(two.durationNanos, one.durationNanos);
    }

    private static int compareShorterFirst(Duration one, Duration two)
    {
        return Long.compare(one.durationNanos, two.durationNanos);
    }

    private static class DurationBuilder
    {
        private CoreInstance instance;
        private long durationNanos;

        private boolean isUnset()
        {
            return this.instance == null;
        }

        private void set(CoreInstance instance, long durationNanos)
        {
            this.instance = instance;
            this.durationNanos = durationNanos;
        }

        private Duration build()
        {
            return isUnset() ? null : new Duration(this.instance, this.durationNanos);
        }
    }

    public static class Duration
    {
        private final CoreInstance instance;
        private final long durationNanos;

        private Duration(CoreInstance instance, long durationNanos)
        {
            this.instance = instance;
            this.durationNanos = durationNanos;
        }

        public CoreInstance getInstance()
        {
            return this.instance;
        }

        public long getDurationNanos()
        {
            return this.durationNanos;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof Duration))
            {
                return false;
            }
            Duration that = (Duration) other;
            return (this.instance == that.instance) && (this.durationNanos == that.durationNanos);
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(this.instance) ^ Long.hashCode(this.durationNanos);
        }
    }
}
