// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime.serialization.binary;

import org.finos.legend.pure.runtime.java.compiled.serialization.binary.StringCacheOrIndex;
import org.junit.Assert;
import org.junit.Test;

public class TestStringCacheOrIndex
{
    @Test
    public void testClassifierIdStringIndexToId()
    {
        int expectedId = -1;
        for (int index = 0; index < Integer.MAX_VALUE; index++)
        {
            int id = StringCacheOrIndex.classifierIdStringIndexToId(index);
            if (id >= 0)
            {
                Assert.fail("Expected id to be negative, got: " + id);
            }

            if (id != expectedId)
            {
                Assert.assertEquals("classifier id string index: " + index, expectedId, id);
            }
            expectedId = id - 1;

            int idIndex = StringCacheOrIndex.classifierIdStringIdToIndex(id);
            if (index != idIndex)
            {
                Assert.assertEquals("classifier id string id: " + id, index, idIndex);
            }
        }
        Assert.assertEquals(Integer.MIN_VALUE, expectedId);
    }

    @Test
    public void testClassifierIdStringIdToIndex()
    {
        int expectedIndex = Integer.MAX_VALUE;
        for (int id = Integer.MIN_VALUE; id < 0; id++)
        {
            int index = StringCacheOrIndex.classifierIdStringIdToIndex(id);
            if (index < 0)
            {
                Assert.fail("Expected index for " + id + " to be non-negative, got: " + index);
            }

            if (index != expectedIndex)
            {
                Assert.assertEquals("classifier id string id: " + id, expectedIndex, index);
            }
            expectedIndex = index - 1;

            int indexId = StringCacheOrIndex.classifierIdStringIndexToId(index);
            if (id != indexId)
            {
                Assert.assertEquals("classifier id string index: " + index, id, indexId);
            }
        }
        // We expect this to go negative after the final loop
        Assert.assertEquals(-1, expectedIndex);
    }

    @Test
    public void testOtherStringIndexToId()
    {
        int expectedId = 1;
        for (int index = 0; index < Integer.MAX_VALUE; index++)
        {
            int id = StringCacheOrIndex.otherStringIndexToId(index);
            if (id <= 0)
            {
                Assert.fail("Expected id to be positive, got: " + id);
            }

            if (id != expectedId)
            {
                Assert.assertEquals("other string index: " + index, expectedId, id);
            }
            expectedId = id + 1;

            int idIndex = StringCacheOrIndex.otherStringIdToIndex(id);
            if (index != idIndex)
            {
                Assert.assertEquals("other string id: " + id, index, idIndex);
            }
        }
        // We expect this to wrap around after the final loop
        Assert.assertEquals(Integer.MIN_VALUE, expectedId);
    }

    @Test
    public void testOtherStringIdToIndex()
    {
        int expectedIndex = 0;
        for (int id = 1; id < Integer.MAX_VALUE; id++)
        {
            int index = StringCacheOrIndex.otherStringIdToIndex(id);
            if (index < 0)
            {
                Assert.fail("Expected index for " + id + " to be non-negative, got: " + index);
            }

            if (index != expectedIndex)
            {
                Assert.assertEquals("other string id: " + id, expectedIndex, index);
            }
            expectedIndex = index + 1;

            int indexId = StringCacheOrIndex.otherStringIndexToId(index);
            if (id != indexId)
            {
                Assert.assertEquals("other string index: " + index, id, indexId);
            }
        }
        Assert.assertEquals(expectedIndex, StringCacheOrIndex.otherStringIdToIndex(Integer.MAX_VALUE));
        Assert.assertEquals(Integer.MAX_VALUE, StringCacheOrIndex.otherStringIndexToId(expectedIndex));
    }
}
