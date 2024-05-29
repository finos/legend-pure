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

package org.finos.legend.pure.m3.tests.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPrimitiveUtilities extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @Test
    public void testGetPrimitiveTypes()
    {
        MutableSet<CoreInstance> expected = repository.getTopLevels().select(topLevel -> Instance.instanceOf(topLevel, M3Paths.PrimitiveType, processorSupport), Sets.mutable.empty());
        Assert.assertEquals(expected, PrimitiveUtilities.getPrimitiveTypes(repository).toSet());
        Assert.assertEquals(expected, PrimitiveUtilities.getPrimitiveTypes(repository, Sets.mutable.empty()));
        Assert.assertEquals(expected, PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet());
        Assert.assertEquals(expected, PrimitiveUtilities.getPrimitiveTypes(processorSupport, Sets.mutable.empty()));
    }

    @Test
    public void testForEachPrimitiveType()
    {
        MutableSet<CoreInstance> expected = repository.getTopLevels().select(topLevel -> Instance.instanceOf(topLevel, M3Paths.PrimitiveType, processorSupport), Sets.mutable.empty());

        MutableList<CoreInstance> actual = Lists.mutable.empty();
        PrimitiveUtilities.forEachPrimitiveType(repository, actual::add);
        Assert.assertEquals(expected, actual.toSet());
        Assert.assertEquals(actual.toSet().size(), actual.size());

        actual.clear();
        PrimitiveUtilities.forEachPrimitiveType(processorSupport, actual::add);
        Assert.assertEquals(expected, actual.toSet());
        Assert.assertEquals(actual.toSet().size(), actual.size());
    }
}
