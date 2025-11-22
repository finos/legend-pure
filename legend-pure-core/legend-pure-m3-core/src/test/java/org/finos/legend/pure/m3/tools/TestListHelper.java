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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestListHelper
{
    @Test
    public void testSortAndRemoveDuplicates()
    {
        ImmutableList<String> quickBrownFox = Lists.immutable.with("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog");

        MutableList<String> quickBrownFoxMutableList = quickBrownFox.toList();
        Assert.assertSame(quickBrownFoxMutableList, ListHelper.sortAndRemoveDuplicates(quickBrownFoxMutableList));
        Assert.assertEquals(Lists.mutable.with("brown", "dog", "fox", "jumps", "lazy", "over", "quick", "the"), quickBrownFoxMutableList);

        List<String> quickBrownFoxArrayList = new ArrayList<>(quickBrownFox.castToList());
        Assert.assertSame(quickBrownFoxArrayList, ListHelper.sortAndRemoveDuplicates(quickBrownFoxArrayList));
        Assert.assertEquals(Lists.mutable.with("brown", "dog", "fox", "jumps", "lazy", "over", "quick", "the"), quickBrownFoxArrayList);


        ImmutableList<String> quickBrownFoxWithCaps = Lists.immutable.with("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog");

        MutableList<String> quickBrownFoxWithCapsMutableList = quickBrownFoxWithCaps.toList();
        Assert.assertSame(quickBrownFoxWithCapsMutableList, ListHelper.sortAndRemoveDuplicates(quickBrownFoxWithCapsMutableList));
        Assert.assertEquals(Lists.mutable.with("The", "brown", "dog", "fox", "jumps", "lazy", "over", "quick", "the"), quickBrownFoxWithCapsMutableList);

        Assert.assertSame(quickBrownFoxWithCapsMutableList, ListHelper.sortAndRemoveDuplicates(quickBrownFoxWithCapsMutableList, String.CASE_INSENSITIVE_ORDER, null));
        Assert.assertEquals(Lists.mutable.with("brown", "dog", "fox", "jumps", "lazy", "over", "quick", "The", "the"), quickBrownFoxWithCapsMutableList);

        Assert.assertSame(quickBrownFoxWithCapsMutableList, ListHelper.sortAndRemoveDuplicates(quickBrownFoxWithCapsMutableList, String.CASE_INSENSITIVE_ORDER, String::equalsIgnoreCase));
        Assert.assertEquals(Lists.mutable.with("brown", "dog", "fox", "jumps", "lazy", "over", "quick", "The"), quickBrownFoxWithCapsMutableList);

        MutableList<String> quickBrownFoxWithCapsMutableList2 = quickBrownFoxWithCaps.toList();
        Assert.assertSame(quickBrownFoxWithCapsMutableList2, ListHelper.sortAndRemoveDuplicates(quickBrownFoxWithCapsMutableList2, Comparator.<String>naturalOrder().reversed(), null));
        Assert.assertEquals(Lists.mutable.with("the", "quick", "over", "lazy", "jumps", "fox", "dog", "brown", "The"), quickBrownFoxWithCapsMutableList2);

        MutableList<String> quickBrownFoxWithCapsMutableList3 = quickBrownFoxWithCaps.toList();
        Assert.assertSame(quickBrownFoxWithCapsMutableList3, ListHelper.sortAndRemoveDuplicates(quickBrownFoxWithCapsMutableList3, String.CASE_INSENSITIVE_ORDER.reversed(), String::equalsIgnoreCase));
        Assert.assertEquals(Lists.mutable.with("The", "quick", "over", "lazy", "jumps", "fox", "dog", "brown"), quickBrownFoxWithCapsMutableList3);
    }
}
