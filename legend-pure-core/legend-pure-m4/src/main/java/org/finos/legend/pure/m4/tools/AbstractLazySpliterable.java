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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractLazySpliterable<T> extends AbstractLazyIterable<T>
{
    @Override
    public void each(Procedure<? super T> procedure)
    {
        forEach((Consumer<? super T>) procedure);
    }

    @Override
    public void forEach(Consumer<? super T> consumer)
    {
        spliterator().forEachRemaining(consumer);
    }

    @Override
    public boolean isEmpty()
    {
        return !spliterator().tryAdvance(t ->
        {
            // Nothing to do
        });
    }

    @Override
    public T getFirst()
    {
        Holder<T> holder = new Holder<>();
        spliterator().tryAdvance(holder::setValue);
        return holder.value;
    }

    @Override
    public T detect(Predicate<? super T> predicate)
    {
        // We do this instead of stream().filter(predicate).findFirst().orElse(null) to avoid a NullPointerException in case the accepted element is null
        Holder<T> holder = new Holder<>();
        Spliterator<T> spliterator = spliterator();
        while (spliterator.tryAdvance(holder::setValue))
        {
            if (predicate.accept(holder.value))
            {
                return holder.value;
            }
        }
        return null;
    }

    @Override
    public Optional<T> detectOptional(Predicate<? super T> predicate)
    {
        return stream().filter(predicate).findFirst();
    }

    @Override
    public boolean anySatisfy(Predicate<? super T> predicate)
    {
        return shortCircuit(predicate, true, true, false);
    }

    @Override
    public boolean allSatisfy(Predicate<? super T> predicate)
    {
        return shortCircuit(predicate, false, false, true);
    }

    @Override
    public boolean noneSatisfy(Predicate<? super T> predicate)
    {
        return shortCircuit(predicate, true, false, true);
    }

    @Override
    public boolean contains(Object object)
    {
        Spliterator<T> spliterator = spliterator();
        return ((object != null) || !spliterator.hasCharacteristics(Spliterator.NONNULL)) &&
                shortCircuit(spliterator, (object == null) ? Objects::isNull : object::equals, true, true, false);
    }

    protected boolean shortCircuit(java.util.function.Predicate<? super T> predicate, boolean expected, boolean onShortCircuit, boolean atEnd)
    {
        return shortCircuit(spliterator(), predicate, expected, onShortCircuit, atEnd);
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        return containsAllInternal(Sets.mutable.withAll(source));
    }

    @Override
    public boolean containsAllArguments(Object... elements)
    {
        return containsAllInternal(Sets.mutable.with(elements));
    }

    private boolean containsAllInternal(MutableSet<Object> sourceSet)
    {
        switch (sourceSet.size())
        {
            case 0:
            {
                return true;
            }
            case 1:
            {
                return contains(sourceSet.getAny());
            }
            default:
            {
                Spliterator<T> spliterator = spliterator();
                if (spliterator.hasCharacteristics(Spliterator.NONNULL) && sourceSet.contains(null))
                {
                    return false;
                }

                while (spliterator.tryAdvance(sourceSet::remove))
                {
                    if (sourceSet.isEmpty())
                    {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override
    public Iterator<T> iterator()
    {
        return Spliterators.iterator(spliterator());
    }

    @Override
    public abstract Spliterator<T> spliterator();

    public Stream<T> stream()
    {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<T> parallelStream()
    {
        return StreamSupport.stream(spliterator(), true);
    }

    protected static <T> boolean shortCircuit(Spliterator<T> spliterator, java.util.function.Predicate<? super T> predicate, boolean expected, boolean onShortCircuit, boolean atEnd)
    {
        Holder<T> holder = new Holder<>();
        while (spliterator.tryAdvance(holder::setValue))
        {
            if (predicate.test(holder.value) == expected)
            {
                return onShortCircuit;
            }
        }
        return atEnd;
    }

    private static class Holder<V>
    {
        private V value;

        void setValue(V val)
        {
            this.value = val;
        }
    }
}
