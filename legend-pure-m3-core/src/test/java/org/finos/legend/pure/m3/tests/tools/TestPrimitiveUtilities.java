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

import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPrimitiveUtilities extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testGetPrimitiveTypes()
    {
        MutableSet<CoreInstance> expected = Sets.mutable.empty();
        for (CoreInstance topLevel : this.repository.getTopLevels())
        {
            if (Instance.instanceOf(topLevel, M3Paths.PrimitiveType, this.processorSupport))
            {
                expected.add(topLevel);
            }
        }
        Verify.assertSetsEqual(expected, PrimitiveUtilities.getPrimitiveTypes(this.repository).toSet());
    }
}
