// Copyright 2025 Goldman Sachs
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
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

public class TestAbstractLazySpliterable
{
    @Test
    public void testSize()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        Assert.assertEquals(9, spliterable.size());
    }

    @Test
    public void testEach()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        MutableList<Integer> collected = Lists.mutable.empty();
        spliterable.each(collected::add);
        Assert.assertEquals(Lists.mutable.with(1, 2, 3, 4, 5, 6, 7, 8, 9), collected);
    }

    @Test
    public void testForEach()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        MutableList<Integer> collected = Lists.mutable.empty();
        spliterable.forEach(collected::add);
        Assert.assertEquals(Lists.mutable.with(1, 2, 3, 4, 5, 6, 7, 8, 9), collected);
    }

    @Test
    public void testIsEmpty()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        Assert.assertFalse(spliterable.isEmpty());
        Assert.assertTrue(emptySpliterable.isEmpty());
    }

    @Test
    public void testGetFirst()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        Assert.assertEquals(Integer.valueOf(1), spliterable.getFirst());
        Assert.assertNull(emptySpliterable.getFirst());
    }

    @Test
    public void testGetAny()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        Assert.assertEquals(Integer.valueOf(1), spliterable.getAny());
        Assert.assertNull(emptySpliterable.getAny());
    }

    @Test
    public void testDetect()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        StringSpliterable spliterableWithNull = new StringSpliterable("the", null, "brown", "fox");
        Assert.assertEquals(Integer.valueOf(5), spliterable.detect(i -> i > 4));
        Assert.assertNull(spliterable.detect(i -> i > 10));
        Assert.assertNull(emptySpliterable.detect(i -> true));
        Assert.assertNull(spliterableWithNull.detect(Objects::isNull));
    }

    @Test
    public void testDetectOptional()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        StringSpliterable spliterableWithNull = new StringSpliterable("the", null, "brown", "fox");
        Assert.assertEquals(Optional.of(5), spliterable.detectOptional(i -> i > 4));
        Assert.assertFalse(spliterable.detectOptional(i -> i > 10).isPresent());
        Assert.assertFalse(emptySpliterable.detectOptional(i -> true).isPresent());
        Assert.assertThrows(NullPointerException.class, () -> spliterableWithNull.detectOptional(Objects::isNull));
    }

    @Test
    public void testAnySatisfy()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        Assert.assertTrue(spliterable.anySatisfy(i -> i > 0));
        Assert.assertTrue(spliterable.anySatisfy(i -> i > 5));
        Assert.assertFalse(spliterable.anySatisfy(i -> i > 10));
    }

    @Test
    public void testAllSatisfy()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        Assert.assertTrue(spliterable.allSatisfy(i -> i > 0));
        Assert.assertTrue(spliterable.allSatisfy(i -> i < 10));
        Assert.assertFalse(spliterable.allSatisfy(i -> i > 5));
        Assert.assertFalse(spliterable.allSatisfy(i -> i < 9));
        Assert.assertFalse(spliterable.allSatisfy(i -> i > 10));
    }

    @Test
    public void testNoneSatisfy()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        Assert.assertFalse(spliterable.noneSatisfy(i -> i > 0));
        Assert.assertFalse(spliterable.noneSatisfy(i -> i < 10));
        Assert.assertFalse(spliterable.noneSatisfy(i -> i > 5));
        Assert.assertFalse(spliterable.noneSatisfy(i -> i < 9));
        Assert.assertTrue(spliterable.noneSatisfy(i -> i > 10));
        Assert.assertTrue(spliterable.noneSatisfy(i -> i < 0));
    }

    @Test
    public void testContains()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        StringSpliterable spliterableWithNull = new StringSpliterable("the", null, "brown", "fox");

        Assert.assertFalse(spliterable.contains(0));
        Assert.assertTrue(spliterable.contains(1));
        Assert.assertTrue(spliterable.contains(2));
        Assert.assertTrue(spliterable.contains(3));
        Assert.assertTrue(spliterable.contains(4));
        Assert.assertTrue(spliterable.contains(5));
        Assert.assertTrue(spliterable.contains(6));
        Assert.assertTrue(spliterable.contains(7));
        Assert.assertTrue(spliterable.contains(8));
        Assert.assertTrue(spliterable.contains(9));
        Assert.assertFalse(spliterable.contains(10));
        Assert.assertFalse(spliterable.contains("the"));
        Assert.assertFalse(spliterable.contains(null));
        Assert.assertFalse(spliterable.contains("brown"));
        Assert.assertFalse(spliterable.contains("fox"));

        Assert.assertFalse(emptySpliterable.contains(0));
        Assert.assertFalse(emptySpliterable.contains(1));
        Assert.assertFalse(emptySpliterable.contains(2));
        Assert.assertFalse(emptySpliterable.contains(3));
        Assert.assertFalse(emptySpliterable.contains(4));
        Assert.assertFalse(emptySpliterable.contains(5));
        Assert.assertFalse(emptySpliterable.contains(6));
        Assert.assertFalse(emptySpliterable.contains(7));
        Assert.assertFalse(emptySpliterable.contains(8));
        Assert.assertFalse(emptySpliterable.contains(9));
        Assert.assertFalse(emptySpliterable.contains(10));
        Assert.assertFalse(emptySpliterable.contains("the"));
        Assert.assertFalse(emptySpliterable.contains(null));
        Assert.assertFalse(emptySpliterable.contains("brown"));
        Assert.assertFalse(emptySpliterable.contains("fox"));

        Assert.assertFalse(spliterableWithNull.contains(0));
        Assert.assertFalse(spliterableWithNull.contains(1));
        Assert.assertFalse(spliterableWithNull.contains(2));
        Assert.assertFalse(spliterableWithNull.contains(3));
        Assert.assertFalse(spliterableWithNull.contains(4));
        Assert.assertFalse(spliterableWithNull.contains(5));
        Assert.assertFalse(spliterableWithNull.contains(6));
        Assert.assertFalse(spliterableWithNull.contains(7));
        Assert.assertFalse(spliterableWithNull.contains(8));
        Assert.assertFalse(spliterableWithNull.contains(9));
        Assert.assertFalse(spliterableWithNull.contains(10));
        Assert.assertTrue(spliterableWithNull.contains("the"));
        Assert.assertTrue(spliterableWithNull.contains(null));
        Assert.assertTrue(spliterableWithNull.contains("brown"));
        Assert.assertTrue(spliterableWithNull.contains("fox"));
    }

    @Test
    public void testContainsAllIterable()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        StringSpliterable spliterableWithNull = new StringSpliterable("the", null, "brown", "fox");

        Assert.assertTrue(spliterable.containsAllIterable(Lists.mutable.with()));
        Assert.assertTrue(spliterable.containsAllIterable(Lists.mutable.with(2, 3, 4, 5)));
        Assert.assertFalse(spliterable.containsAllIterable(Lists.mutable.with(2, 3, 4, 5, 11)));
        Assert.assertFalse(spliterable.containsAllIterable(Lists.mutable.with(2, 3, null, 4, 5)));
        Assert.assertFalse(spliterable.containsAllIterable(Lists.mutable.with("the", "brown", "fox")));
        Assert.assertFalse(spliterable.containsAllIterable(Lists.mutable.with((Integer) null)));

        Assert.assertTrue(emptySpliterable.containsAllIterable(Lists.mutable.with()));
        Assert.assertFalse(emptySpliterable.containsAllIterable(Lists.mutable.with(2, 3, 4, 5)));
        Assert.assertFalse(emptySpliterable.containsAllIterable(Lists.mutable.with(2, 3, 4, 5, 11)));
        Assert.assertFalse(emptySpliterable.containsAllIterable(Lists.mutable.with(2, 3, null, 4, 5)));
        Assert.assertFalse(emptySpliterable.containsAllIterable(Lists.mutable.with("the", "brown", "fox")));
        Assert.assertFalse(emptySpliterable.containsAllIterable(Lists.mutable.with((Integer) null)));

        Assert.assertTrue(spliterableWithNull.containsAllIterable(Lists.mutable.with()));
        Assert.assertFalse(spliterableWithNull.containsAllIterable(Lists.mutable.with(2, 3, 4, 5)));
        Assert.assertFalse(spliterableWithNull.containsAllIterable(Lists.mutable.with(2, 3, 4, 5, 11)));
        Assert.assertFalse(spliterableWithNull.containsAllIterable(Lists.mutable.with(2, 3, null, 4, 5)));
        Assert.assertTrue(spliterableWithNull.containsAllIterable(Lists.mutable.with("the", "brown", "fox")));
        Assert.assertTrue(spliterableWithNull.containsAllIterable(Lists.mutable.with((Integer) null)));
    }

    @Test
    public void testContainsAllArguments()
    {
        RangeSpliterable spliterable = new RangeSpliterable(1, 10);
        RangeSpliterable emptySpliterable = new RangeSpliterable(1, 1);
        StringSpliterable spliterableWithNull = new StringSpliterable("the", null, "brown", "fox");

        Assert.assertTrue(spliterable.containsAllArguments());
        Assert.assertTrue(spliterable.containsAllArguments(2, 3, 4, 5));
        Assert.assertFalse(spliterable.containsAllArguments(2, 3, 4, 5, 11));
        Assert.assertFalse(spliterable.containsAllArguments(2, 3, null, 4, 5));
        Assert.assertFalse(spliterable.containsAllArguments("the", "brown", "fox"));
        Assert.assertFalse(spliterable.containsAllArguments((Integer) null));

        Assert.assertTrue(emptySpliterable.containsAllArguments());
        Assert.assertFalse(emptySpliterable.containsAllArguments(2, 3, 4, 5));
        Assert.assertFalse(emptySpliterable.containsAllArguments(2, 3, 4, 5, 11));
        Assert.assertFalse(emptySpliterable.containsAllArguments(2, 3, null, 4, 5));
        Assert.assertFalse(emptySpliterable.containsAllArguments("the", "brown", "fox"));
        Assert.assertFalse(emptySpliterable.containsAllArguments((Integer) null));

        Assert.assertTrue(spliterableWithNull.containsAllArguments());
        Assert.assertFalse(spliterableWithNull.containsAllArguments(2, 3, 4, 5));
        Assert.assertFalse(spliterableWithNull.containsAllArguments(2, 3, 4, 5, 11));
        Assert.assertFalse(spliterableWithNull.containsAllArguments(2, 3, null, 4, 5));
        Assert.assertTrue(spliterableWithNull.containsAllArguments("the", "brown", "fox"));
        Assert.assertTrue(spliterableWithNull.containsAllArguments((Integer) null));
    }

    @Test
    public void testSplitDeque()
    {
        Deque<String> original = new ArrayDeque<>(Arrays.asList("the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog"));
        Deque<String> split = AbstractLazySpliterable.splitDeque(original);
        Assert.assertEquals(Arrays.asList("the", "brown", "jumped", "the", "dog"), new ArrayList<>(original));
        Assert.assertEquals(Arrays.asList("quick", "fox", "over", "lazy"), new ArrayList<>(split));
    }

    private static class RangeSpliterable extends AbstractLazySpliterable<Integer>
    {
        private final int start;
        private final int end;

        private RangeSpliterable(int start, int end)
        {
            this.start = start;
            this.end = end;
        }

        @Override
        public Spliterator<Integer> spliterator()
        {
            return new RangeSpliterator(this.start, this.end);
        }
    }

    private static class RangeSpliterator implements Spliterator<Integer>
    {
        private int current;
        private int end;

        private RangeSpliterator(int start, int end)
        {
            this.current = start;
            this.end = end;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Integer> action)
        {
            if (this.current >= this.end)
            {
                return false;
            }
            action.accept(this.current++);
            return true;
        }

        @Override
        public Spliterator<Integer> trySplit()
        {
            int remaining = this.end - this.current;
            if (remaining <= 1)
            {
                return null;
            }

            int mid = this.current + (remaining / 2);
            int tmpEnd = this.end;
            this.end = mid;
            return new RangeSpliterator(mid, tmpEnd);
        }

        @Override
        public long estimateSize()
        {
            return getExactSizeIfKnown();
        }

        @Override
        public long getExactSizeIfKnown()
        {
            return Math.max(0L, this.end - this.current);
        }

        @Override
        public int characteristics()
        {
            return SIZED | SUBSIZED | NONNULL | DISTINCT;
        }
    }

    private static class StringSpliterable extends AbstractLazySpliterable<String>
    {
        private final List<String> strings;

        private StringSpliterable(String... strings)
        {
            this.strings = Arrays.asList(strings);
        }

        @Override
        public Spliterator<String> spliterator()
        {
            return this.strings.spliterator();
        }
    }
}
