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

package org.finos.legend.pure.maven.shared;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class TestDependencyResolutionScope
{
    @Test
    public void testFromName_allValidNames()
    {
        Assert.assertSame(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("compile"));
        Assert.assertSame(DependencyResolutionScope.COMPILE_RUNTIME_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("compile+runtime"));
        Assert.assertSame(DependencyResolutionScope.RUNTIME_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("runtime"));
        Assert.assertSame(DependencyResolutionScope.RUNTIME_SYSTEM_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("runtime+system"));
        Assert.assertSame(DependencyResolutionScope.TEST_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("test"));
    }

    @Test
    public void testFromName_caseInsensitive()
    {
        Assert.assertSame(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("COMPILE"));
        Assert.assertSame(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("Compile"));
        Assert.assertSame(DependencyResolutionScope.COMPILE_RUNTIME_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("COMPILE+RUNTIME"));
        Assert.assertSame(DependencyResolutionScope.RUNTIME_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("Runtime"));
        Assert.assertSame(DependencyResolutionScope.RUNTIME_SYSTEM_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("RUNTIME+SYSTEM"));
        Assert.assertSame(DependencyResolutionScope.TEST_RESOLUTION_SCOPE, DependencyResolutionScope.fromName("TEST"));
    }

    @Test
    public void testFromName_invalidName()
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> DependencyResolutionScope.fromName("invalid"));
        Assert.assertEquals("Unknown dependency resolution scope: 'invalid'; valid values: [compile, compile+runtime, runtime, runtime+system, test]", e.getMessage());
    }

    @Test
    public void testFromName_null()
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> DependencyResolutionScope.fromName(null));
        Assert.assertEquals("Unknown dependency resolution scope: null; valid values: [compile, compile+runtime, runtime, runtime+system, test]", e.getMessage());
    }

    @Test
    public void testGetScopeDependencyFilter_nonNullForNonTestScopes()
    {
        Assert.assertNotNull(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE.getScopeDependencyFilter());
        Assert.assertNotNull(DependencyResolutionScope.COMPILE_RUNTIME_RESOLUTION_SCOPE.getScopeDependencyFilter());
        Assert.assertNotNull(DependencyResolutionScope.RUNTIME_RESOLUTION_SCOPE.getScopeDependencyFilter());
        Assert.assertNotNull(DependencyResolutionScope.RUNTIME_SYSTEM_RESOLUTION_SCOPE.getScopeDependencyFilter());
    }

    @Test
    public void testGetScopeDependencyFilter_nullForTestScope()
    {
        Assert.assertNull(DependencyResolutionScope.TEST_RESOLUTION_SCOPE.getScopeDependencyFilter());
    }

    @Test
    public void testIsTestScope()
    {
        for (DependencyResolutionScope scope : DependencyResolutionScope.values())
        {
            if (scope == DependencyResolutionScope.TEST_RESOLUTION_SCOPE)
            {
                Assert.assertTrue(scope.getName() + " should be test scope", scope.isTestScope());
            }
            else
            {
                Assert.assertFalse(scope.getName() + " should not be test scope", scope.isTestScope());
            }
        }
    }

    @Test
    public void testGetName()
    {
        Assert.assertEquals("compile", DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE.getName());
        Assert.assertEquals("compile+runtime", DependencyResolutionScope.COMPILE_RUNTIME_RESOLUTION_SCOPE.getName());
        Assert.assertEquals("runtime", DependencyResolutionScope.RUNTIME_RESOLUTION_SCOPE.getName());
        Assert.assertEquals("runtime+system", DependencyResolutionScope.RUNTIME_SYSTEM_RESOLUTION_SCOPE.getName());
        Assert.assertEquals("test", DependencyResolutionScope.TEST_RESOLUTION_SCOPE.getName());
    }

    @Test
    public void testAllValuesHaveUniqueName()
    {
        Set<String> names = new HashSet<>();
        for (DependencyResolutionScope scope : DependencyResolutionScope.values())
        {
            Assert.assertTrue("Duplicate name: " + scope.getName(), names.add(scope.getName()));
        }
        Assert.assertEquals(DependencyResolutionScope.values().length, names.size());
    }
}
