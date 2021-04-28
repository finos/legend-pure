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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.RandomAccess;
import java.util.concurrent.RecursiveAction;

public class RecursiveProcedure<T> extends RecursiveAction
{
    private final ListIterable<T> list;
    private final Procedure<? super T> procedure;
    private final int start;
    private final int end;
    private final int threshold;

    private RecursiveProcedure(ListIterable<T> list, Procedure<? super T> procedure, int start, int end, int threshold)
    {
        this.list = list;
        this.procedure = procedure;
        this.start = start;
        this.end = end;
        this.threshold = threshold;
    }

    @Override
    protected void compute()
    {
        int size = this.end - this.start;
        if (size <= this.threshold)
        {
            for (int i = this.start; i < this.end; i++)
            {
                this.procedure.value(this.list.get(i));
            }
        }
        else
        {
            int midPoint = this.start + (size / 2);
            invokeAll(new RecursiveProcedure<>(this.list, this.procedure, this.start, midPoint, this.threshold), new RecursiveProcedure<>(this.list, this.procedure, midPoint, this.end, this.threshold));
        }
    }

    public static <T> RecursiveProcedure<T> newRecursiveProcedure(ListIterable<T> list, Procedure<? super T> procedure, int start, int end, int threshold)
    {
        if (list == null)
        {
            throw new IllegalArgumentException("list cannot be null");
        }
        if (procedure == null)
        {
            throw new IllegalArgumentException("procedure cannot be null");
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
        return new RecursiveProcedure<>(list, procedure, start, Math.min(end, list.size()), threshold);
    }

    public static <T> RecursiveProcedure<T> newRecursiveProcedure(ListIterable<T> list, Procedure<? super T> procedure, int threshold)
    {
        return newRecursiveProcedure(list, procedure, 0, list.size(), threshold);
    }

    public static <T> RecursiveProcedure<T> newRecursiveProcedure(T[] array, Procedure<? super T> procedure, int start, int end, int threshold)
    {
        return newRecursiveProcedure(ArrayAdapter.adapt(array), procedure, start, end, threshold);
    }

    public static <T> RecursiveProcedure<T> newRecursiveProcedure(T[] array, Procedure<? super T> procedure, int threshold)
    {
        return newRecursiveProcedure(array, procedure, 0, array.length, threshold);
    }
}
