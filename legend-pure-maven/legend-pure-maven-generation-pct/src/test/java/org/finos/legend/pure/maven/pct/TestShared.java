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

package org.finos.legend.pure.maven.pct;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import java.net.URLClassLoader;
import java.util.Collections;

public class TestShared
{
    // --- assertPresentOrNotEmpty ---

    @Test(expected = MojoExecutionException.class)
    public void testAssertPresentOrNotEmpty_null() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("testParam", null);
    }

    @Test(expected = MojoExecutionException.class)
    public void testAssertPresentOrNotEmpty_emptyList() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("testParam", Collections.emptyList());
    }

    @Test(expected = MojoExecutionException.class)
    public void testAssertPresentOrNotEmpty_emptySet() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("testParam", Collections.emptySet());
    }

    @Test
    public void testAssertPresentOrNotEmpty_nonEmptyCollection() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("testParam", Collections.singletonList("a"));
    }

    @Test
    public void testAssertPresentOrNotEmpty_nonEmptySet() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("testParam", Collections.singleton("value"));
    }

    @Test
    public void testAssertPresentOrNotEmpty_nonCollectionObject() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("testParam", "hello");
    }

    @Test
    public void testAssertPresentOrNotEmpty_nonCollectionEnum() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("mode", Mode.Compiled);
    }

    @Test
    public void testAssertPresentOrNotEmpty_exceptionMessage()
    {
        try
        {
            Shared.assertPresentOrNotEmpty("myParamName", null);
            Assert.fail("Expected MojoExecutionException");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue(
                    "Exception message should contain parameter name, but was: " + e.getMessage(),
                    e.getMessage().contains("myParamName")
            );
        }
    }

    @Test
    public void testAssertPresentOrNotEmpty_integerValue() throws MojoExecutionException
    {
        Shared.assertPresentOrNotEmpty("count", 42);
    }

    // --- buildClassLoader ---

    @Test
    public void testBuildClassLoader_returnsNonNull() throws Exception
    {
        MavenProject project = new MavenProject();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader cl = Shared.buildClassLoader(project, parent, new SystemStreamLog()))
        {
            Assert.assertNotNull("buildClassLoader should return a non-null classloader", cl);
        }
    }

    @Test
    public void testBuildClassLoader_parentIsSet() throws Exception
    {
        MavenProject project = new MavenProject();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader cl = Shared.buildClassLoader(project, parent, new SystemStreamLog()))
        {
            Assert.assertSame("Parent classloader should be the one passed in",
                    parent, cl.getParent());
        }
    }

    @Test
    public void testBuildClassLoader_emptyProjectHasNoExtraURLs() throws Exception
    {
        MavenProject project = new MavenProject();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader cl = Shared.buildClassLoader(project, parent, new SystemStreamLog()))
        {
            // A fresh MavenProject has no compile or test classpath elements,
            // so the classloader should have zero extra URLs of its own.
            Assert.assertEquals("Empty project should produce zero extra classpath URLs",
                    0, cl.getURLs().length);
        }
    }

    // --- Mode enum ---

    @Test
    public void testMode_compiledValue()
    {
        Assert.assertEquals(Mode.Compiled, Mode.valueOf("Compiled"));
    }

    @Test
    public void testMode_interpretedValue()
    {
        Assert.assertEquals(Mode.Interpreted, Mode.valueOf("Interpreted"));
    }

    @Test
    public void testMode_enumValues_containsBoth()
    {
        Mode[] values = Mode.values();
        Assert.assertEquals(2, values.length);
        Assert.assertNotNull(Mode.Compiled);
        Assert.assertNotNull(Mode.Interpreted);
    }
}

