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

package org.finos.legend.pure.runtime.java.shared.parallel;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.parallel.ParallelIterate;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

public class ParallelHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelHelper.class);

    public static <V> int calculateBatchSize(RichIterable<? extends V> collection, int parallelism)
    {
        int normalizedParallelism = Math.min(Runtime.getRuntime().availableProcessors(), parallelism);
        return Math.max(1, (int) Math.round((double)collection.size() / (double)normalizedParallelism));
    }

    public static <T, V> RichIterable<? extends T> collect(RichIterable<? extends V> collection, BiFunction<? super V, ExecutionSupport, T> function, ExecutorService executorService, int parallelism, ExecutionSupport executionSupport)
    {
        LOGGER.info("Doing parallel collect execution over collection of size {} with parallelism {}.", collection.size(), parallelism);
        MutableList<T> result = Lists.mutable.ofInitialCapacity(collection.size());
        int batchSize = calculateBatchSize(collection, parallelism);
        return ParallelIterate.collect(collection, e -> function.apply(e, executionSupport), result, batchSize, executorService, false);
    }

    public static <T, V> ListIterable<T> flatCollect(ListIterable<V> collection, Function<? super V, Collection<T>> function, ExecutorService executorService, int parallelism, ExecutionSupport executionSupport)
    {
        LOGGER.info("Doing parallel flatCollect execution over collection of size {} with parallelism {}.", collection.size(), parallelism);
        MutableList<T> result = Lists.mutable.ofInitialCapacity(collection.size());
        int batchSize = calculateBatchSize(collection, parallelism);
        return ParallelIterate.flatCollect(collection, function, result, batchSize, executorService, false);
    }

    public static <T, V> RichIterable<T> flatCollect(RichIterable<V> collection, BiFunction<? super V, ExecutionSupport, Collection<T>> function, ExecutorService executorService, int parallelism, ExecutionSupport executionSupport)
    {
        LOGGER.info("Doing parallel flatCollect execution over collection of size {} with parallelism {}.", collection.size(), parallelism);
        MutableList<T> result = Lists.mutable.ofInitialCapacity(collection.size());
        int batchSize = calculateBatchSize(collection, parallelism);
        return ParallelIterate.flatCollect(collection, e -> function.apply(e, executionSupport), result, batchSize, executorService, false);
    }
}
