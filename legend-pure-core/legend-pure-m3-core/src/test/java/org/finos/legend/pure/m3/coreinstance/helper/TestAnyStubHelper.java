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

package org.finos.legend.pure.m3.coreinstance.helper;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAnyStubHelper extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @Test
    public void testStubClasses()
    {
        MutableList<String> missingClasses = AnyStubHelper.STUB_CLASSES.select(path -> processorSupport.package_getByUserPath(path) == null, Lists.mutable.empty());
        Assert.assertEquals(Lists.fixedSize.empty(), missingClasses);
    }

    @Test
    public void testGetStubClasses()
    {
        MutableSet<CoreInstance> expected = AnyStubHelper.STUB_CLASSES.collect(processorSupport::package_getByUserPath, Sets.mutable.empty());
        Assert.assertEquals(expected, AnyStubHelper.getStubClasses(processorSupport, Sets.mutable.empty()));
    }

    @Test
    public void testForEachStubClass()
    {
        MutableSet<CoreInstance> expected = AnyStubHelper.STUB_CLASSES.collect(processorSupport::package_getByUserPath, Sets.mutable.empty());
        MutableList<CoreInstance> actual = Lists.mutable.empty();
        AnyStubHelper.forEachStubClass(processorSupport, actual::add);
        Assert.assertEquals(expected, actual.toSet());
        Assert.assertEquals(actual.toSet().size(), actual.size());
    }
}
