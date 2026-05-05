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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TestVavrHamtMutableMapAdapter
{
    private VavrHamtMutableMapAdapter<String, Integer> map;

    private static final HashingStrategy<String> CASE_INSENSITIVE_STRATEGY = new HashingStrategy<String>()
    {
        @Override
        public int computeHashCode(String object)
        {
            return object == null ? 0 : object.toLowerCase().hashCode();
        }

        @Override
        public boolean equals(String object1, String object2)
        {
            if (object1 == null)
            {
                return object2 == null;
            }
            return object1.equalsIgnoreCase(object2);
        }
    };

    @Before
    public void setUp()
    {
        map = new VavrHamtMutableMapAdapter<>();
    }

    // ---- Constructor tests --------------------------------------------------

    @Test
    public void testDefaultConstructor()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>();
        Assert.assertTrue(m.isEmpty());
        Assert.assertEquals(0, m.size());
        Assert.assertNull(m.hashingStrategy());
    }

    @Test
    public void testConstructorWithHashingStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        Assert.assertTrue(m.isEmpty());
        Assert.assertSame(CASE_INSENSITIVE_STRATEGY, m.hashingStrategy());
    }

    // ---- Factory method tests -----------------------------------------------

    @Test
    public void testFromMapWithJavaMap()
    {
        MutableMap<String, Integer> source = Maps.mutable.empty();
        source.put("a", 1);
        source.put("b", 2);

        VavrHamtMutableMapAdapter<String, Integer> adapter = VavrHamtMutableMapAdapter.fromMap(source);
        Assert.assertEquals(2, adapter.size());
        Assert.assertEquals(Integer.valueOf(1), adapter.get("a"));
        Assert.assertEquals(Integer.valueOf(2), adapter.get("b"));
        Assert.assertNull(adapter.hashingStrategy());
    }

    @Test
    public void testFromMapWithVavrAdapter()
    {
        map.put("x", 10);
        map.put("y", 20);

        VavrHamtMutableMapAdapter<String, Integer> copy = VavrHamtMutableMapAdapter.fromMap(map);
        Assert.assertEquals(2, copy.size());
        Assert.assertEquals(Integer.valueOf(10), copy.get("x"));
        Assert.assertEquals(Integer.valueOf(20), copy.get("y"));

        // Verify it's a separate copy
        copy.put("z", 30);
        Assert.assertNull(map.get("z"));
    }

    @Test
    public void testFromMapWithVavrAdapterPreservesStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> original = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        original.put("Hello", 1);

        VavrHamtMutableMapAdapter<String, Integer> copy = VavrHamtMutableMapAdapter.fromMap(original);
        Assert.assertSame(CASE_INSENSITIVE_STRATEGY, copy.hashingStrategy());
        Assert.assertEquals(Integer.valueOf(1), copy.get("hello"));
    }

    @Test
    public void testFromMapWithUnifiedMapWithHashingStrategy()
    {
        UnifiedMapWithHashingStrategy<String, Integer> unified = new UnifiedMapWithHashingStrategy<>(CASE_INSENSITIVE_STRATEGY);
        unified.put("Key", 42);

        VavrHamtMutableMapAdapter<String, Integer> adapter = VavrHamtMutableMapAdapter.fromMap(unified);
        Assert.assertSame(CASE_INSENSITIVE_STRATEGY, adapter.hashingStrategy());
        Assert.assertEquals(Integer.valueOf(42), adapter.get("key"));
        Assert.assertEquals(Integer.valueOf(42), adapter.get("KEY"));
    }

    @Test
    public void testWithHashingStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = VavrHamtMutableMapAdapter.withHashingStrategy(CASE_INSENSITIVE_STRATEGY);
        Assert.assertTrue(m.isEmpty());
        Assert.assertSame(CASE_INSENSITIVE_STRATEGY, m.hashingStrategy());
    }

    @Test
    public void testWithHashingStrategyAndSource()
    {
        MutableMap<String, Integer> source = Maps.mutable.empty();
        source.put("Abc", 1);
        source.put("Def", 2);

        VavrHamtMutableMapAdapter<String, Integer> m = VavrHamtMutableMapAdapter.withHashingStrategy(CASE_INSENSITIVE_STRATEGY, source);
        Assert.assertEquals(2, m.size());
        Assert.assertEquals(Integer.valueOf(1), m.get("abc"));
        Assert.assertEquals(Integer.valueOf(2), m.get("DEF"));
    }

    @Test
    public void testFromJavaMap()
    {
        MutableMap<VavrHamtMutableMapAdapter.HamtKey<String>, Integer> javaMap = Maps.mutable.empty();
        VavrHamtMutableMapAdapter<String, Integer> temp = new VavrHamtMutableMapAdapter<>();
        javaMap.put(temp.wrapKey("a"), 1);
        javaMap.put(temp.wrapKey("b"), 2);

        VavrHamtMutableMapAdapter<String, Integer> adapter = VavrHamtMutableMapAdapter.fromJavaMap(javaMap, null);
        Assert.assertEquals(2, adapter.size());
        Assert.assertEquals(Integer.valueOf(1), adapter.get("a"));
        Assert.assertEquals(Integer.valueOf(2), adapter.get("b"));
    }

    @Test
    public void testFromJavaMapWithStrategy()
    {
        MutableMap<VavrHamtMutableMapAdapter.HamtKey<String>, Integer> javaMap = Maps.mutable.empty();
        VavrHamtMutableMapAdapter<String, Integer> temp = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        javaMap.put(temp.wrapKey("Hello"), 1);

        VavrHamtMutableMapAdapter<String, Integer> adapter = VavrHamtMutableMapAdapter.fromJavaMap(javaMap, CASE_INSENSITIVE_STRATEGY);
        Assert.assertEquals(1, adapter.size());
        Assert.assertSame(CASE_INSENSITIVE_STRATEGY, adapter.hashingStrategy());
    }

    // ---- Core read operations -----------------------------------------------

    @Test
    public void testGetExistingKey()
    {
        map.put("key", 42);
        Assert.assertEquals(Integer.valueOf(42), map.get("key"));
    }

    @Test
    public void testGetNonExistentKey()
    {
        Assert.assertNull(map.get("missing"));
    }

    @Test
    public void testContainsKeyTrue()
    {
        map.put("exists", 1);
        Assert.assertTrue(map.containsKey("exists"));
    }

    @Test
    public void testContainsKeyFalse()
    {
        Assert.assertFalse(map.containsKey("nope"));
    }

    @Test
    public void testContainsValueTrue()
    {
        map.put("a", 100);
        Assert.assertTrue(map.containsValue(100));
    }

    @Test
    public void testContainsValueFalse()
    {
        map.put("a", 100);
        Assert.assertFalse(map.containsValue(999));
    }

    @Test
    public void testSize()
    {
        Assert.assertEquals(0, map.size());
        map.put("a", 1);
        Assert.assertEquals(1, map.size());
        map.put("b", 2);
        Assert.assertEquals(2, map.size());
        map.put("a", 3); // overwrite
        Assert.assertEquals(2, map.size());
    }

    @Test
    public void testIsEmpty()
    {
        Assert.assertTrue(map.isEmpty());
        map.put("a", 1);
        Assert.assertFalse(map.isEmpty());
    }

    @Test
    public void testNotEmpty()
    {
        Assert.assertFalse(map.notEmpty());
        map.put("a", 1);
        Assert.assertTrue(map.notEmpty());
    }

    // ---- Core write operations ----------------------------------------------

    @Test
    public void testPutReturnsOldValue()
    {
        Assert.assertNull(map.put("a", 1));
        Assert.assertEquals(Integer.valueOf(1), map.put("a", 2));
        Assert.assertEquals(Integer.valueOf(2), map.get("a"));
    }

    @Test
    public void testPutNoReturn()
    {
        map.putNoReturn("fast", 99);
        Assert.assertEquals(Integer.valueOf(99), map.get("fast"));
        map.putNoReturn("fast", 100);
        Assert.assertEquals(Integer.valueOf(100), map.get("fast"));
    }

    @Test
    public void testPutAllFromJavaMap()
    {
        MutableMap<String, Integer> source = Maps.mutable.empty();
        source.put("x", 10);
        source.put("y", 20);

        map.put("z", 30);
        map.putAll(source);

        Assert.assertEquals(3, map.size());
        Assert.assertEquals(Integer.valueOf(10), map.get("x"));
        Assert.assertEquals(Integer.valueOf(20), map.get("y"));
        Assert.assertEquals(Integer.valueOf(30), map.get("z"));
    }

    @Test
    public void testPutAllFromVavrAdapterSameStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> other = new VavrHamtMutableMapAdapter<>();
        other.put("a", 1);
        other.put("b", 2);

        map.put("c", 3);
        map.putAll(other);

        Assert.assertEquals(3, map.size());
        Assert.assertEquals(Integer.valueOf(1), map.get("a"));
        Assert.assertEquals(Integer.valueOf(2), map.get("b"));
        Assert.assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testPutAllFromVavrAdapterDifferentStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> other = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        other.put("Hello", 1);

        VavrHamtMutableMapAdapter<String, Integer> target = new VavrHamtMutableMapAdapter<>();
        target.putAll(other);

        Assert.assertEquals(1, target.size());
        Assert.assertEquals(Integer.valueOf(1), target.get("Hello"));
        // Without case-insensitive strategy on target, "hello" shouldn't match
        Assert.assertNull(target.get("hello"));
    }

    @Test
    public void testRemove()
    {
        map.put("a", 1);
        map.put("b", 2);

        Assert.assertEquals(Integer.valueOf(1), map.remove("a"));
        Assert.assertEquals(1, map.size());
        Assert.assertNull(map.get("a"));
    }

    @Test
    public void testRemoveNonExistent()
    {
        map.put("a", 1);
        Assert.assertNull(map.remove("missing"));
        Assert.assertEquals(1, map.size());
    }

    @Test
    public void testRemoveKey()
    {
        map.put("a", 1);
        Assert.assertEquals(Integer.valueOf(1), map.removeKey("a"));
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void testClear()
    {
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        Assert.assertTrue(map.isEmpty());
        Assert.assertEquals(0, map.size());
    }

    // ---- Eclipse Collections MutableMap contract ----------------------------

    @Test
    public void testGetIfAbsentPutWithFunction0_absent()
    {
        Counter callCount = new Counter(0);
        Integer result = map.getIfAbsentPut("key", () ->
        {
            callCount.increment();
            return 42;
        });
        Assert.assertEquals(Integer.valueOf(42), result);
        Assert.assertEquals(1, callCount.getCount());
        Assert.assertEquals(Integer.valueOf(42), map.get("key"));
    }

    @Test
    public void testGetIfAbsentPutWithFunction0_present()
    {
        map.put("key", 10);
        Counter callCount = new Counter(0);
        Integer result = map.getIfAbsentPut("key", () ->
        {
            callCount.increment();
            return 42;
        });
        Assert.assertEquals(Integer.valueOf(10), result);
        Assert.assertEquals(0, callCount.getCount());
    }

    @Test
    public void testGetIfAbsentPutWithValue_absent()
    {
        Integer result = map.getIfAbsentPut("key", 42);
        Assert.assertEquals(Integer.valueOf(42), result);
        Assert.assertEquals(Integer.valueOf(42), map.get("key"));
    }

    @Test
    public void testGetIfAbsentPutWithValue_present()
    {
        map.put("key", 10);
        Integer result = map.getIfAbsentPut("key", 42);
        Assert.assertEquals(Integer.valueOf(10), result);
    }

    @Test
    public void testGetIfAbsentPutWith_absent()
    {
        Integer result = map.getIfAbsentPutWith("key", String::length, "hello");
        Assert.assertEquals(Integer.valueOf(5), result);
        Assert.assertEquals(Integer.valueOf(5), map.get("key"));
    }

    @Test
    public void testGetIfAbsentPutWith_present()
    {
        map.put("key", 10);
        Integer result = map.getIfAbsentPutWith("key", String::length, "hello");
        Assert.assertEquals(Integer.valueOf(10), result);
    }

    @Test
    public void testGetIfAbsentPutWithKey_absent()
    {
        Integer result = map.getIfAbsentPutWithKey("abc", String::length);
        Assert.assertEquals(Integer.valueOf(3), result);
        Assert.assertEquals(Integer.valueOf(3), map.get("abc"));
    }

    @Test
    public void testGetIfAbsentPutWithKey_present()
    {
        map.put("abc", 99);
        Integer result = map.getIfAbsentPutWithKey("abc", String::length);
        Assert.assertEquals(Integer.valueOf(99), result);
    }

    // ---- Iteration ----------------------------------------------------------

    @Test
    public void testForEachKeyValue()
    {
        map.put("a", 1);
        map.put("b", 2);

        MutableMap<String, Integer> collected = Maps.mutable.empty();
        map.forEachKeyValue(collected::put);

        Assert.assertEquals(2, collected.size());
        Assert.assertEquals(Integer.valueOf(1), collected.get("a"));
        Assert.assertEquals(Integer.valueOf(2), collected.get("b"));
    }

    @Test
    public void testCollectKeysAndValues()
    {
        map.collectKeysAndValues(Lists.mutable.with("abc", "de", "fghi"), s -> s, String::length);

        Assert.assertEquals(3, map.size());
        Assert.assertEquals(Integer.valueOf(3), map.get("abc"));
        Assert.assertEquals(Integer.valueOf(2), map.get("de"));
        Assert.assertEquals(Integer.valueOf(4), map.get("fghi"));
    }

    @Test
    public void testIterator()
    {
        map.put("a", 1);
        map.put("b", 2);

        MutableSet<Integer> values = Sets.mutable.empty();
        Iterator<Integer> it = map.iterator();
        while (it.hasNext())
        {
            values.add(it.next());
        }
        Assert.assertEquals(Sets.mutable.with(1, 2), values);
    }

    // ---- View operations ----------------------------------------------------

    @Test
    public void testKeySet()
    {
        map.put("a", 1);
        map.put("b", 2);

        Set<String> keys = map.keySet();
        Assert.assertEquals(2, keys.size());
        Assert.assertTrue(keys.contains("a"));
        Assert.assertTrue(keys.contains("b"));
    }

    @Test
    public void testValues()
    {
        map.put("a", 1);
        map.put("b", 2);

        Collection<Integer> vals = map.values();
        Assert.assertEquals(2, vals.size());
        Assert.assertTrue(vals.contains(1));
        Assert.assertTrue(vals.contains(2));
    }

    @Test
    public void testEntrySet()
    {
        map.put("a", 1);
        map.put("b", 2);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Assert.assertEquals(2, entries.size());

        MutableMap<String, Integer> fromEntries = Maps.mutable.empty();
        for (Map.Entry<String, Integer> e : entries)
        {
            fromEntries.put(e.getKey(), e.getValue());
        }
        Assert.assertEquals(Integer.valueOf(1), fromEntries.get("a"));
        Assert.assertEquals(Integer.valueOf(2), fromEntries.get("b"));
    }

    @Test
    public void testValuesView()
    {
        map.put("a", 1);
        map.put("b", 2);

        LazyIterable<Integer> view = map.valuesView();
        MutableSet<Integer> collected = Sets.mutable.empty();
        view.forEach(collected::add);
        Assert.assertEquals(Sets.mutable.with(1, 2), collected);
    }

    @Test
    public void testKeyValuesView()
    {
        map.put("a", 1);
        map.put("b", 2);

        LazyIterable<Pair<String, Integer>> view = map.keyValuesView();
        MutableMap<String, Integer> collected = Maps.mutable.empty();
        view.forEach(p -> collected.put(p.getOne(), p.getTwo()));

        Assert.assertEquals(Integer.valueOf(1), collected.get("a"));
        Assert.assertEquals(Integer.valueOf(2), collected.get("b"));
    }

    @Test
    public void testKeysView()
    {
        map.put("a", 1);
        map.put("b", 2);

        LazyIterable<String> view = map.keysView();
        MutableSet<String> collected = Sets.mutable.empty();
        view.forEach(collected::add);
        Assert.assertEquals(Sets.mutable.with("a", "b"), collected);
    }

    @Test
    public void testKeysWithDuplicates()
    {
        map.put("a", 1);
        map.put("b", 2);

        MutableSet<String> collected = Sets.mutable.empty();
        map.keysWithDuplicates().forEach(collected::add);
        Assert.assertEquals(Sets.mutable.with("a", "b"), collected);
    }

    // ---- Copy / Clone -------------------------------------------------------

    @Test
    public void testNewEmpty()
    {
        map.put("a", 1);
        MutableMap<String, Integer> empty = map.newEmpty();
        Assert.assertTrue(empty.isEmpty());
        Assert.assertTrue(empty instanceof VavrHamtMutableMapAdapter);
    }

    @Test
    public void testNewEmptyPreservesStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> stratMap = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        MutableMap<String, Integer> empty = stratMap.newEmpty();
        Assert.assertTrue(empty instanceof VavrHamtMutableMapAdapter);
        Assert.assertSame(CASE_INSENSITIVE_STRATEGY, ((VavrHamtMutableMapAdapter<String, Integer>) empty).hashingStrategy());
    }

    @Test
    public void testNewEmptyWithCapacity()
    {
        MutableMap<Integer, String> empty = map.newEmpty(100);
        Assert.assertTrue(empty.isEmpty());
        Assert.assertTrue(empty instanceof VavrHamtMutableMapAdapter);
    }

    @Test
    public void testClone()
    {
        map.put("a", 1);
        map.put("b", 2);

        MutableMap<String, Integer> cloned = map.clone();
        Assert.assertEquals(2, cloned.size());
        Assert.assertEquals(Integer.valueOf(1), cloned.get("a"));
        Assert.assertEquals(Integer.valueOf(2), cloned.get("b"));

        // Mutations on clone should not affect original
        cloned.put("c", 3);
        Assert.assertNull(map.get("c"));
    }

    @Test
    public void testToImmutable()
    {
        map.put("a", 1);
        map.put("b", 2);

        ImmutableMap<String, Integer> immutable = map.toImmutable();
        Assert.assertEquals(2, immutable.size());
        Assert.assertEquals(Integer.valueOf(1), immutable.get("a"));
        Assert.assertEquals(Integer.valueOf(2), immutable.get("b"));
    }

    // ---- equals / hashCode --------------------------------------------------

    @Test
    public void testEqualsWithSameInstance()
    {
        map.put("a", 1);
        Assert.assertEquals(map, map);
    }

    @Test
    public void testEqualsWithJavaHashMap()
    {
        map.put("a", 1);
        map.put("b", 2);

        MutableMap<String, Integer> javaMap = Maps.mutable.empty();
        javaMap.put("a", 1);
        javaMap.put("b", 2);

        Assert.assertEquals(map, javaMap);
        Assert.assertEquals(javaMap, map);
    }

    @Test
    public void testEqualsWithDifferentValues()
    {
        map.put("a", 1);

        MutableMap<String, Integer> javaMap = Maps.mutable.empty();
        javaMap.put("a", 2);

        Assert.assertNotEquals(map, javaMap);
    }

    @Test
    public void testEqualsWithDifferentSizes()
    {
        map.put("a", 1);

        MutableMap<String, Integer> javaMap = Maps.mutable.empty();
        javaMap.put("a", 1);
        javaMap.put("b", 2);

        Assert.assertNotEquals(map, javaMap);
    }

    @Test
    public void testEqualsWithNonMap()
    {
        Assert.assertNotEquals(map, "not a map");
    }

    @Test
    public void testEqualsWithMissingKey()
    {
        map.put("a", 1);

        MutableMap<String, Integer> javaMap = Maps.mutable.empty();
        javaMap.put("b", 1);

        Assert.assertNotEquals(map, javaMap);
    }

    @Test
    public void testHashCodeConsistency()
    {
        map.put("a", 1);
        map.put("b", 2);

        MutableMap<String, Integer> javaMap = Maps.mutable.empty();
        javaMap.put("a", 1);
        javaMap.put("b", 2);

        Assert.assertEquals(javaMap.hashCode(), map.hashCode());
    }

    @Test
    public void testHashCodeEmpty()
    {
        Assert.assertEquals(0, map.hashCode());
    }

    // ---- toString -----------------------------------------------------------

    @Test
    public void testToStringEmpty()
    {
        Assert.assertEquals("{}", map.toString());
    }

    @Test
    public void testToStringSingleEntry()
    {
        map.put("a", 1);
        Assert.assertEquals("{a=1}", map.toString());
    }

    @Test
    public void testToStringMultipleEntries()
    {
        map.put("a", 1);
        map.put("b", 2);
        String str = map.toString();
        Assert.assertTrue(str.startsWith("{"));
        Assert.assertTrue(str.endsWith("}"));
        Assert.assertTrue(str.contains("a=1"));
        Assert.assertTrue(str.contains("b=2"));
    }

    // ---- Hashing strategy behavior ------------------------------------------

    @Test
    public void testCaseInsensitiveGetAndPut()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        m.put("Hello", 1);

        Assert.assertEquals(Integer.valueOf(1), m.get("hello"));
        Assert.assertEquals(Integer.valueOf(1), m.get("HELLO"));
        Assert.assertEquals(Integer.valueOf(1), m.get("Hello"));
    }

    @Test
    public void testCaseInsensitiveOverwrite()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        m.put("Hello", 1);
        m.put("hello", 2);

        Assert.assertEquals(1, m.size());
        Assert.assertEquals(Integer.valueOf(2), m.get("HELLO"));
    }

    @Test
    public void testCaseInsensitiveContainsKey()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        m.put("Key", 1);

        Assert.assertTrue(m.containsKey("key"));
        Assert.assertTrue(m.containsKey("KEY"));
        Assert.assertTrue(m.containsKey("Key"));
    }

    @Test
    public void testCaseInsensitiveRemove()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        m.put("Key", 1);

        Assert.assertEquals(Integer.valueOf(1), m.remove("key"));
        Assert.assertTrue(m.isEmpty());
    }

    @Test
    public void testCaseInsensitiveGetIfAbsentPutWithKey()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        m.put("Hello", 1);

        Integer result = m.getIfAbsentPutWithKey("hello", String::length);
        Assert.assertEquals(Integer.valueOf(1), result);
        Assert.assertEquals(1, m.size());
    }

    // ---- HamtKey tests ------------------------------------------------------

    @Test
    public void testHamtKeyEqualsWithoutStrategy()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", null);
        VavrHamtMutableMapAdapter.HamtKey<String> k2 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", null);
        VavrHamtMutableMapAdapter.HamtKey<String> k3 = new VavrHamtMutableMapAdapter.HamtKey<>("xyz", null);

        Assert.assertEquals(k1, k2);
        Assert.assertNotEquals(k1, k3);
    }

    @Test
    public void testHamtKeyEqualsWithStrategy()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", CASE_INSENSITIVE_STRATEGY);
        VavrHamtMutableMapAdapter.HamtKey<String> k2 = new VavrHamtMutableMapAdapter.HamtKey<>("ABC", CASE_INSENSITIVE_STRATEGY);

        Assert.assertEquals(k1, k2);
    }

    @Test
    public void testHamtKeyEqualsSameInstance()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", null);
        Assert.assertEquals(k1, k1);
    }

    @Test
    public void testHamtKeyEqualsNonHamtKey()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", null);
        Assert.assertNotEquals(k1, "abc");
    }

    @Test
    public void testHamtKeyHashCodeWithoutStrategy()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", null);
        VavrHamtMutableMapAdapter.HamtKey<String> k2 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", null);

        Assert.assertEquals(k1.hashCode(), k2.hashCode());
    }

    @Test
    public void testHamtKeyHashCodeWithStrategy()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>("abc", CASE_INSENSITIVE_STRATEGY);
        VavrHamtMutableMapAdapter.HamtKey<String> k2 = new VavrHamtMutableMapAdapter.HamtKey<>("ABC", CASE_INSENSITIVE_STRATEGY);

        Assert.assertEquals(k1.hashCode(), k2.hashCode());
    }

    @Test
    public void testHamtKeyToString()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k = new VavrHamtMutableMapAdapter.HamtKey<>("hello", null);
        Assert.assertEquals("hello", k.toString());
    }

    @Test
    public void testHamtKeyToStringNull()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k = new VavrHamtMutableMapAdapter.HamtKey<>(null, null);
        Assert.assertEquals("null", k.toString());
    }

    @Test
    public void testHamtKeyHashCodeNull()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k = new VavrHamtMutableMapAdapter.HamtKey<>(null, null);
        Assert.assertEquals(0, k.hashCode());
    }

    @Test
    public void testHamtKeyEqualsNullKeys()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> k1 = new VavrHamtMutableMapAdapter.HamtKey<>(null, null);
        VavrHamtMutableMapAdapter.HamtKey<String> k2 = new VavrHamtMutableMapAdapter.HamtKey<>(null, null);
        Assert.assertEquals(k1, k2);
    }

    // ---- wrapKey public accessor --------------------------------------------

    @Test
    public void testWrapKey()
    {
        VavrHamtMutableMapAdapter.HamtKey<String> wrapped = map.wrapKey("test");
        Assert.assertEquals("test", wrapped.key);
    }

    // ---- Null value support -------------------------------------------------

    @Test
    public void testPutNullValue()
    {
        map.put("key", null);
        Assert.assertTrue(map.containsKey("key"));
        Assert.assertNull(map.get("key"));
        Assert.assertEquals(1, map.size());
    }

    @Test
    public void testGetNullKeyWithoutStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> m = new VavrHamtMutableMapAdapter<>();
        m.put(null, 42);
        Assert.assertEquals(Integer.valueOf(42), m.get(null));
        Assert.assertTrue(m.containsKey(null));
    }

    // ---- Large map ----------------------------------------------------------

    @Test
    public void testLargeMap()
    {
        int count = 10_000;
        for (int i = 0; i < count; i++)
        {
            map.put("key" + i, i);
        }
        Assert.assertEquals(count, map.size());
        for (int i = 0; i < count; i++)
        {
            Assert.assertEquals(Integer.valueOf(i), map.get("key" + i));
        }
    }

    // ---- Overwrite semantics ------------------------------------------------

    @Test
    public void testPutOverwriteReturnsPrevious()
    {
        map.put("a", 1);
        Integer old = map.put("a", 2);
        Assert.assertEquals(Integer.valueOf(1), old);
        Assert.assertEquals(Integer.valueOf(2), map.get("a"));
    }

    // ---- PutAll from VavrAdapter with same strategy -------------------------

    @Test
    public void testPutAllFromVavrAdapterWithSameNonNullStrategy()
    {
        VavrHamtMutableMapAdapter<String, Integer> source = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        source.put("Alpha", 1);
        source.put("Beta", 2);

        VavrHamtMutableMapAdapter<String, Integer> target = new VavrHamtMutableMapAdapter<>(CASE_INSENSITIVE_STRATEGY);
        target.putAll(source);

        Assert.assertEquals(2, target.size());
        Assert.assertEquals(Integer.valueOf(1), target.get("alpha"));
        Assert.assertEquals(Integer.valueOf(2), target.get("BETA"));
    }

    // ---- Edge cases ---------------------------------------------------------

    @Test
    public void testContainsValueNull()
    {
        map.put("a", null);
        Assert.assertTrue(map.containsValue(null));
    }

    @Test
    public void testRemoveFromEmptyMap()
    {
        Assert.assertNull(map.remove("nonexistent"));
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testClearAlreadyEmpty()
    {
        map.clear();
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetEmpty()
    {
        Assert.assertTrue(map.keySet().isEmpty());
    }

    @Test
    public void testValuesEmpty()
    {
        Assert.assertTrue(map.values().isEmpty());
    }

    @Test
    public void testEntrySetEmpty()
    {
        Assert.assertTrue(map.entrySet().isEmpty());
    }
}
