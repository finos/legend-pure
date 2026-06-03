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

package org.finos.legend.pure.lsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class LegendPureSessionTest
{
    @Test
    public void shouldLoadClasspathRepository_loadsClasspathReposByDefault()
    {
        Assert.assertTrue(LegendPureSession.shouldLoadClasspathRepository(
                null, Collections.emptySet(), Collections.emptySet()));
        Assert.assertTrue(LegendPureSession.shouldLoadClasspathRepository(
                "platform", Collections.emptySet(), Collections.emptySet()));
        Assert.assertTrue(LegendPureSession.shouldLoadClasspathRepository(
                "platform_pure", Collections.emptySet(), Collections.emptySet()));
        Assert.assertTrue(LegendPureSession.shouldLoadClasspathRepository(
                "pure_ide_debug", Collections.emptySet(), Collections.emptySet()));
        Assert.assertTrue(LegendPureSession.shouldLoadClasspathRepository(
                "core", Collections.emptySet(), Collections.emptySet()));
    }

    @Test
    public void shouldLoadClasspathRepository_workspaceReposWinOverClasspathRepos()
    {
        Assert.assertFalse(LegendPureSession.shouldLoadClasspathRepository(
                "core", set("core"), set("core")));
        Assert.assertFalse(LegendPureSession.shouldLoadClasspathRepository(
                "platform", set("platform"), Collections.emptySet()));
    }

    private static Set<String> set(String... values)
    {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
