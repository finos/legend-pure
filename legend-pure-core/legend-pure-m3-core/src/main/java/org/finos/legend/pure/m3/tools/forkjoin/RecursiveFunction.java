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

package org.finos.legend.pure.m3.tools.forkjoin;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.RandomAccess;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

public class RecursiveFunction<T, V> extends RecursiveTask<ListIterable<V>>
{
    private final ListIterable<T> list;
    private final Function<? super T, ? extends V> function;
    private final int start;
    private final int end;
    private final int threshold;
    private final V[] result;

    private RecursiveFunction(ListIterable<T> list, Function<? super T, ? extends V> function, int start, int end, int threshold)
    {
        this.list = list;
        this.function = function;
        this.start = start;
        this.end = end;
        this.threshold = threshold;
        this.result = (V[])new Object[this.end - this.start];
    }

    @Override
    protected ListIterable<V> compute()
    {
        computeForRange(this.start, this.end);
        return ArrayAdapter.adapt(this.result);
    }

    private void computeForRange(int rangeStart, int rangeEnd)
    {
        int size = rangeEnd - rangeStart;
        if (size <= this.threshold)
        {
            for (int i = rangeStart; i < rangeEnd; i++)
            {
                this.result[i - this.start] = this.function.apply(this.list.get(i));
            }
        }
        else
        {
            int midPoint = rangeStart + (size / 2);
            invokeAll(new RecursiveFunctionApplication(rangeStart, midPoint), new RecursiveFunctionApplication(midPoint, rangeEnd));
        }
    }

    private class RecursiveFunctionApplication extends RecursiveAction
    {
        private final int start;
        private final int end;

        private RecursiveFunctionApplication(int start, int end)
        {
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute()
        {
            computeForRange(this.start, this.end);
        }
    }

    public static <T, V> RecursiveFunction<T, V> newRecursiveFunction(ListIterable<T> list, Function<? super T, ? extends V> function, int start, int end, int threshold)
    {
        if (list == null)
        {
            throw new IllegalArgumentException("list cannot be null");
        }
        if (function == null)
        {
            throw new IllegalArgumentException("function cannot be null");
        }
        if (start < 0)
        {
            throw new IllegalArgumentException("start index (" + start + ") must be non-negative");
        }
        if (start >= list.size())
        {
            throw new IllegalArgumentException("start index (" + start + ") is greater than list size (" + list.size() + ")");
        }
        if (end <= start)
        {
            throw new IllegalArgumentException("end index (" + end + ") is less than start index (" + start + ")");
        }
        if (threshold < 1)
        {
            throw new IllegalArgumentException("threshold must be positive");
        }
        if (!(list instanceof RandomAccess))
        {
            list = FastList.newList(list);
        }
        return new RecursiveFunction<>(list, function, start, Math.min(end, list.size()), threshold);
    }

    public static <T, V> RecursiveFunction<T, V> newRecursiveFunction(ListIterable<T> list, Function<? super T, ? extends V> function, int threshold)
    {
        return newRecursiveFunction(list, function, 0, list.size(), threshold);
    }

    public static <T, V> RecursiveFunction<T, V> newRecursiveFunction(T[] array, Function<? super T, ? extends V> function, int start, int end, int threshold)
    {
        return newRecursiveFunction(ArrayAdapter.adapt(array), function, start, end, threshold);
    }

    public static <T, V> RecursiveFunction<T, V> newRecursiveFunction(T[] array, Function<? super T, ? extends V> function, int threshold)
    {
        return newRecursiveFunction(array, function, 0, array.length, threshold);
    }

}
