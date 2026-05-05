// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map;

import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.AbstractMutableMap;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.tuple.ImmutableEntry;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link MutableMap} adapter backed by a Vavr HAMT (Hash Array Mapped Trie) map.
 * <p>
 * Mutations update the internal immutable HAMT reference, giving persistent-data-structure
 * semantics while satisfying the Eclipse Collections {@code MutableMap} contract required
 * by generated code (e.g.&nbsp;{@code getIfAbsentPutWithKey}, {@code get}, {@code put}).
 * <p>
 * Read operations delegate directly to the HAMT; write operations swap the HAMT reference.
 * An optional {@link HashingStrategy} is preserved so that maps created with
 * {@code UnifiedMapWithHashingStrategy} (e.g.&nbsp;{@code PureEqualsHashingStrategy} or
 * {@code PropertyHashingStrategy}) continue to use the same equality/hashCode semantics.
 */
public class VavrHamtMutableMapAdapter<K, V> extends AbstractMutableMap<K, V>
{
    private io.vavr.collection.Map<HamtKey<K>, V> hamtMap;
    private final HashingStrategy<? super K> hashingStrategy;

    // ---- Constructors -------------------------------------------------------

    public VavrHamtMutableMapAdapter()
    {
        this(HashMap.empty(), null);
    }

    public VavrHamtMutableMapAdapter(HashingStrategy<? super K> hashingStrategy)
    {
        this(HashMap.empty(), hashingStrategy);
    }

    private VavrHamtMutableMapAdapter(io.vavr.collection.Map<HamtKey<K>, V> hamtMap, HashingStrategy<? super K> hashingStrategy)
    {
        this.hamtMap = hamtMap;
        this.hashingStrategy = hashingStrategy;
    }

    // ---- Factory methods ----------------------------------------------------

    /**
     * Build a new adapter from an existing {@link Map}, preserving the hashing
     * strategy if the source is a {@code UnifiedMapWithHashingStrategy}.
     */
    public static <K, V> VavrHamtMutableMapAdapter<K, V> fromMap(Map<K, V> source)
    {
        if (source instanceof VavrHamtMutableMapAdapter)
        {
            VavrHamtMutableMapAdapter<K, V> src = (VavrHamtMutableMapAdapter<K, V>) source;
            return new VavrHamtMutableMapAdapter<>(src.hamtMap, src.hashingStrategy);
        }
        HashingStrategy<? super K> strategy = (source instanceof UnifiedMapWithHashingStrategy) ? ((UnifiedMapWithHashingStrategy<K, V>) source).hashingStrategy() : null;
        VavrHamtMutableMapAdapter<K, V> adapter = new VavrHamtMutableMapAdapter<>(HashMap.empty(), strategy);
        source.forEach((k, v) -> adapter.hamtMap = adapter.hamtMap.put(adapter.wrap(k), v));
        return adapter;
    }

    public static <K, V> VavrHamtMutableMapAdapter<K, V> withHashingStrategy(HashingStrategy<? super K> strategy)
    {
        return new VavrHamtMutableMapAdapter<>(HashMap.empty(), strategy);
    }

    public static <K, V> VavrHamtMutableMapAdapter<K, V> withHashingStrategy(HashingStrategy<? super K> strategy, Map<K, V> source)
    {
        VavrHamtMutableMapAdapter<K, V> adapter = new VavrHamtMutableMapAdapter<>(HashMap.empty(), strategy);
        adapter.putAll(source);
        return adapter;
    }

    /**
     * Build a HAMT adapter by bulk-loading entries from a {@link Map}.
     * This constructs the HAMT in a single pass via {@code HashMap.ofAll()},
     * avoiding the overhead of individual path-copies for each entry.
     */
    public static <K, V> VavrHamtMutableMapAdapter<K, V> fromJavaMap(Map<HamtKey<K>, V> javaMap, HashingStrategy<? super K> strategy)
    {
        io.vavr.collection.Map<HamtKey<K>, V> hamt = HashMap.ofAll(javaMap);
        return new VavrHamtMutableMapAdapter<>(hamt, strategy);
    }

    // ---- HashingStrategy accessor -------------------------------------------

    public HashingStrategy<? super K> hashingStrategy()
    {
        return this.hashingStrategy;
    }

    // ---- Key wrapper that delegates equals/hashCode to the strategy ---------

    private HamtKey<K> wrap(K key)
    {
        return new HamtKey<>(key, this.hashingStrategy);
    }

    /**
     * Wraps a key using this adapter's hashing strategy.
     * Exposed for use in batch-building scenarios (e.g. {@link #fromJavaMap}).
     */
    public HamtKey<K> wrapKey(K key)
    {
        return new HamtKey<>(key, this.hashingStrategy);
    }

    @SuppressWarnings("unchecked")
    private HamtKey<K> wrapObject(Object key)
    {
        return new HamtKey<>((K) key, this.hashingStrategy);
    }

    /**
     * A wrapper that delegates {@code equals} and {@code hashCode} to an optional
     * {@link HashingStrategy}. When no strategy is present, the natural
     * {@code Object.equals} / {@code Object.hashCode} are used.
     */
    public static final class HamtKey<K>
    {
        final K key;
        private final HashingStrategy<? super K> strategy;

        public HamtKey(K key, HashingStrategy<? super K> strategy)
        {
            this.key = key;
            this.strategy = strategy;
        }

        @Override
        public int hashCode()
        {
            if (strategy != null)
            {
                return strategy.computeHashCode(key);
            }
            return Objects.hashCode(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof HamtKey))
            {
                return false;
            }
            K otherKey = ((HamtKey<K>) obj).key;
            if (strategy != null)
            {
                return strategy.equals(key, otherKey);
            }
            return Objects.equals(key, otherKey);
        }

        @Override
        public String toString()
        {
            return String.valueOf(key);
        }
    }

    // ---- Core Map read operations -------------------------------------------

    @Override
    public V get(Object key)
    {
        return this.hamtMap.getOrElse(wrapObject(key), null);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.hamtMap.containsKey(wrapObject(key));
    }

    @Override
    public boolean containsValue(Object value)
    {
        return this.hamtMap.iterator().exists(tuple -> Objects.equals(tuple._2, value));
    }

    @Override
    public int size()
    {
        return this.hamtMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.hamtMap.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return !this.hamtMap.isEmpty();
    }

    // ---- Core Map write operations ------------------------------------------

    @Override
    public V put(K key, V value)
    {
        HamtKey<K> wrapped = wrap(key);
        io.vavr.collection.Map<HamtKey<K>, V> prev = this.hamtMap;
        this.hamtMap = prev.put(wrapped, value);
        return prev.get(wrapped).getOrElse((V) null);
    }

    /**
     * Inserts a key-value pair without returning the previous value.
     * Use this when the caller discards the return value of {@code put},
     * as it avoids a redundant HAMT traversal.
     */
    public void putNoReturn(K key, V value)
    {
        this.hamtMap = this.hamtMap.put(wrap(key), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K, ? extends V> map)
    {
        if (map instanceof VavrHamtMutableMapAdapter)
        {
            VavrHamtMutableMapAdapter<K, V> other = (VavrHamtMutableMapAdapter<K, V>) map;
            io.vavr.collection.Map<HamtKey<K>, V> current = this.hamtMap;
            if (Objects.equals(this.hashingStrategy, other.hashingStrategy))
            {
                // Same strategy — reuse HamtKey wrappers directly, no re-wrapping
                for (io.vavr.Tuple2<HamtKey<K>, V> tuple : other.hamtMap)
                {
                    current = current.put(tuple._1, tuple._2);
                }
            }
            else
            {
                // Different strategy — must re-wrap keys
                for (io.vavr.Tuple2<HamtKey<K>, V> tuple : other.hamtMap)
                {
                    current = current.put(wrap(tuple._1.key), tuple._2);
                }
            }
            this.hamtMap = current;
        }
        else
        {
            io.vavr.collection.Map<HamtKey<K>, V> current = this.hamtMap;
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
            {
                current = current.put(wrap(entry.getKey()), entry.getValue());
            }
            this.hamtMap = current;
        }
    }

    @Override
    public V removeKey(K key)
    {
        return this.remove(key);
    }

    @Override
    public V remove(Object key)
    {
        HamtKey<K> wrapped = wrapObject(key);
        V old = this.hamtMap.get(wrapped).getOrElse((V) null);
        this.hamtMap = this.hamtMap.remove(wrapped);
        return old;
    }

    @Override
    public void clear()
    {
        this.hamtMap = HashMap.empty();
    }

    // ---- Eclipse Collections MutableMap contract ----------------------------

    @Override
    public V getIfAbsentPut(K key, Function0<? extends V> function)
    {
        HamtKey<K> wrapped = wrap(key);
        io.vavr.control.Option<V> existing = this.hamtMap.get(wrapped);
        if (existing.isDefined())
        {
            return existing.get();
        }
        V value = function.value();
        this.hamtMap = this.hamtMap.put(wrapped, value);
        return value;
    }

    @Override
    public V getIfAbsentPut(K key, V value)
    {
        HamtKey<K> wrapped = wrap(key);
        io.vavr.control.Option<V> existing = this.hamtMap.get(wrapped);
        if (existing.isDefined())
        {
            return existing.get();
        }
        this.hamtMap = this.hamtMap.put(wrapped, value);
        return value;
    }

    @Override
    public <P> V getIfAbsentPutWith(K key, Function<? super P, ? extends V> function, P parameter)
    {
        HamtKey<K> wrapped = wrap(key);
        io.vavr.control.Option<V> existing = this.hamtMap.get(wrapped);
        if (existing.isDefined())
        {
            return existing.get();
        }
        V value = function.valueOf(parameter);
        this.hamtMap = this.hamtMap.put(wrapped, value);
        return value;
    }

    @Override
    public V getIfAbsentPutWithKey(K key, Function<? super K, ? extends V> function)
    {
        HamtKey<K> wrapped = wrap(key);
        io.vavr.control.Option<V> existing = this.hamtMap.get(wrapped);
        if (existing.isDefined())
        {
            return existing.get();
        }
        V value = function.valueOf(key);
        this.hamtMap = this.hamtMap.put(wrapped, value);
        return value;
    }

    // ---- Iteration ----------------------------------------------------------

    @Override
    public void forEachKeyValue(Procedure2<? super K, ? super V> procedure)
    {
        this.hamtMap.forEach(tuple -> procedure.value(tuple._1.key, tuple._2));
    }

    @Override
    public <E> MutableMap<K, V> collectKeysAndValues(Iterable<E> iterable, Function<? super E, ? extends K> keyFunction, Function<? super E, ? extends V> valueFunction)
    {
        for (E item : iterable)
        {
            this.put(keyFunction.valueOf(item), valueFunction.valueOf(item));
        }
        return this;
    }

    @Override
    public Iterator<V> iterator()
    {
        return this.hamtMap.valuesIterator();
    }

    // ---- View operations ----------------------------------------------------

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public Collection<V> values()
    {
        return new ValuesCollection();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return new EntrySet();
    }

    @Override
    public LazyIterable<V> valuesView()
    {
        return LazyIterate.adapt(this.hamtMap.values());
    }

    @Override
    public LazyIterable<Pair<K, V>> keyValuesView()
    {
        return LazyIterate.adapt(this.hamtMap).collect(tuple -> Tuples.pair(tuple._1.key, tuple._2));
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return LazyIterate.adapt(this.hamtMap).collect(tuple -> tuple._1.key);
    }

    public RichIterable<K> keysWithDuplicates()
    {
        return this.keysView();
    }

    // ---- Copy / Clone -------------------------------------------------------

    @Override
    public MutableMap<K, V> newEmpty()
    {
        return new VavrHamtMutableMapAdapter<>(HashMap.empty(), this.hashingStrategy);
    }

    @Override
    public <K1, V1> MutableMap<K1, V1> newEmpty(int capacity)
    {
        // capacity hint ignored — HAMT does not pre-allocate
        return new VavrHamtMutableMapAdapter<>(HashMap.empty(), null);
    }

    @Override
    public MutableMap<K, V> clone()
    {
        // HAMT is immutable so sharing the reference is safe
        return new VavrHamtMutableMapAdapter<>(this.hamtMap, this.hashingStrategy);
    }

    @Override
    public ImmutableMap<K, V> toImmutable()
    {
        MutableMap<K, V> copy = Maps.mutable.ofInitialCapacity(this.size());
        this.forEachKeyValue(copy::put);
        return copy.toImmutable();
    }

    // ---- equals / hashCode --------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Map))
        {
            return false;
        }
        Map<?, ?> other = (Map<?, ?>) o;
        if (this.size() != other.size())
        {
            return false;
        }
        for (Map.Entry<?, ?> entry : other.entrySet())
        {
            HamtKey<K> wrapped = wrapObject(entry.getKey());
            io.vavr.control.Option<V> val = this.hamtMap.get(wrapped);
            if (val.isEmpty() || !Objects.equals(val.get(), entry.getValue()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int h = 0;
        for (io.vavr.Tuple2<HamtKey<K>, V> tuple : this.hamtMap)
        {
            h += Objects.hashCode(tuple._1.key) ^ Objects.hashCode(tuple._2);
        }
        return h;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (io.vavr.Tuple2<HamtKey<K>, V> tuple : this.hamtMap)
        {
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(tuple._1.key).append("=").append(tuple._2);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private class KeySet extends AbstractSet<K>
    {
        @Override
        public int size()
        {
            return VavrHamtMutableMapAdapter.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return VavrHamtMutableMapAdapter.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return VavrHamtMutableMapAdapter.this.containsKey(o);
        }

        @Override
        public Iterator<K> iterator()
        {
            return VavrHamtMutableMapAdapter.this.hamtMap.keysIterator().map(k -> k.key);
        }
    }

    private class ValuesCollection extends AbstractCollection<V>
    {
        @Override
        public int size()
        {
            return VavrHamtMutableMapAdapter.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return VavrHamtMutableMapAdapter.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return VavrHamtMutableMapAdapter.this.containsValue(o);
        }

        @Override
        public Iterator<V> iterator()
        {
            return VavrHamtMutableMapAdapter.this.hamtMap.valuesIterator();
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>>
    {
        @Override
        public int size()
        {
            return VavrHamtMutableMapAdapter.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return VavrHamtMutableMapAdapter.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            if (!(o instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Option<V> value = VavrHamtMutableMapAdapter.this.hamtMap.get(wrapObject(entry.getKey()));
            return value.isDefined() && Objects.equals(value.get(), entry.getValue());
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator()
        {
            return VavrHamtMutableMapAdapter.this.hamtMap.iterator().map(t -> ImmutableEntry.of(t._1.key, t._2));
        }
    }
}
