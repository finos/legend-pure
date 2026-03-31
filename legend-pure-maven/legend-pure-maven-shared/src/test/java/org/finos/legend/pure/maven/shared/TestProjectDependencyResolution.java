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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;

public class TestProjectDependencyResolution
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    // --- inTestPhase() tests ---

    /**
     * Verifies the complete set of phases that {@code inTestPhase()} recognises as
     * test phases (returns {@code true}) and those it does not (returns {@code false}).
     *
     * <p>This is a contract test: {@code ProjectDependencyResolution.determineDependencyResolutionScope()}
     * delegates to {@code inTestPhase()} when no explicit scope override is supplied, so
     * the phase-to-scope mapping is a stable API boundary. Adding or removing a phase
     * from either list is a behavioural change that must be deliberate.</p>
     *
     * <p>Note: "generate-test-sources", "generate-test-resources", and
     * "process-test-resources" are semantically test phases but the current
     * implementation does not recognise them. This is a known gap tracked in
     * MAVEN_PLUGINS_REVIEW.md §4.4 and documented here so any future fix is visible.</p>
     */
    @Test
    public void testInTestPhase_recognisedTestPhases()
    {
        String[] testPhases = {"test-compile", "process-test-classes", "test"};
        for (String phase : testPhases)
        {
            Assert.assertTrue("expected inTestPhase() == true for phase: " + phase,
                    ProjectDependencyResolution.inTestPhase(executionWithPhase(phase)));
        }
    }

    @Test
    public void testInTestPhase_nonTestPhases()
    {
        // Phases that must return false — includes three semantically-test phases that
        // the current implementation does not yet recognise (marked with *).
        String[] nonTestPhases = {
                "compile",
                "process-resources",
                "package",
                "install",
                "deploy",
                "generate-test-sources",    // * known gap — see MAVEN_PLUGINS_REVIEW.md §4.4
                "generate-test-resources",  // * known gap
                "process-test-resources",   // * known gap
        };
        for (String phase : nonTestPhases)
        {
            Assert.assertFalse("expected inTestPhase() == false for phase: " + phase,
                    ProjectDependencyResolution.inTestPhase(executionWithPhase(phase)));
        }
    }

    // --- determineDependencyResolutionScope() tests ---

    @Test
    public void testDetermineDependencyResolutionScope_withOverride_compile()
    {
        Assert.assertSame(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                ProjectDependencyResolution.determineDependencyResolutionScope("compile", executionWithPhase("compile"))
        );
    }

    @Test
    public void testDetermineDependencyResolutionScope_withOverride_allScopes()
    {
        for (DependencyResolutionScope scope : DependencyResolutionScope.values())
        {
            Assert.assertSame(
                    scope,
                    ProjectDependencyResolution.determineDependencyResolutionScope(scope.getName(), executionWithPhase("compile"))
            );
        }
    }

    @Test
    public void testDetermineDependencyResolutionScope_withOverride_ignoresPhase()
    {
        // Even in a test phase, an explicit override should take precedence
        Assert.assertSame(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                ProjectDependencyResolution.determineDependencyResolutionScope("compile", executionWithPhase("test-compile"))
        );
    }

    @Test
    public void testDetermineDependencyResolutionScope_noOverride_testPhase()
    {
        Assert.assertSame(
                DependencyResolutionScope.TEST_RESOLUTION_SCOPE,
                ProjectDependencyResolution.determineDependencyResolutionScope(null, executionWithPhase("test-compile"))
        );
    }

    @Test
    public void testDetermineDependencyResolutionScope_noOverride_nonTestPhase()
    {
        Assert.assertSame(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                ProjectDependencyResolution.determineDependencyResolutionScope(null, executionWithPhase("compile"))
        );
    }

    @Test
    public void testDetermineDependencyResolutionScope_noOverride_processTestClasses()
    {
        Assert.assertSame(
                DependencyResolutionScope.TEST_RESOLUTION_SCOPE,
                ProjectDependencyResolution.determineDependencyResolutionScope(null, executionWithPhase("process-test-classes"))
        );
    }

    @Test
    public void testDetermineDependencyResolutionScope_noOverride_packagePhase()
    {
        Assert.assertSame(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                ProjectDependencyResolution.determineDependencyResolutionScope(null, executionWithPhase("package"))
        );
    }

    // --- getDependencyURLs() tests ---

    @Test
    public void testGetDependencyURLs_outputDirIncluded() throws Exception
    {
        File outputDir     = tempFolder.newFolder("classes");
        File testOutputDir = tempFolder.newFolder("test-classes");

        URL[] urls = ProjectDependencyResolution.getDependencyURLs(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                emptyProject(),
                executionWithPhase("compile"),
                null,
                outputDir,
                testOutputDir,
                MojoTestSupport.EMPTY_RESOLVER
        );

        // Compile scope + empty resolver yields exactly one URL: projectOutputDirectory
        Assert.assertEquals("Compile scope with empty resolver should yield exactly 1 URL", 1, urls.length);
        Assert.assertEquals(
                "URL should be the exact file URL for projectOutputDirectory",
                outputDir.toURI().toURL(), urls[0]);
    }

    @Test
    public void testGetDependencyURLs_testOutputDirOnlyInTestPhase() throws Exception
    {
        File outputDir     = tempFolder.newFolder("classes2");
        File testOutputDir = tempFolder.newFolder("test-classes2");

        // Compile phase + compile scope → only outputDir, testOutputDir absent
        URL[] compileUrls = ProjectDependencyResolution.getDependencyURLs(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                emptyProject(),
                executionWithPhase("compile"),
                null,
                outputDir,
                testOutputDir,
                MojoTestSupport.EMPTY_RESOLVER
        );
        Assert.assertEquals("Compile scope should yield exactly 1 URL", 1, compileUrls.length);
        Assert.assertEquals(outputDir.toURI().toURL(), compileUrls[0]);

        // Test phase + test scope → both outputDir and testOutputDir, in that order
        URL[] testUrls = ProjectDependencyResolution.getDependencyURLs(
                DependencyResolutionScope.TEST_RESOLUTION_SCOPE,
                emptyProject(),
                executionWithPhase("test-compile"),
                null,
                outputDir,
                testOutputDir,
                MojoTestSupport.EMPTY_RESOLVER
        );
        Assert.assertEquals("Test scope in test phase should yield exactly 2 URLs", 2, testUrls.length);
        Assert.assertEquals("First URL should be projectOutputDirectory",
                outputDir.toURI().toURL(), testUrls[0]);
        Assert.assertEquals("Second URL should be projectTestOutputDirectory",
                testOutputDir.toURI().toURL(), testUrls[1]);
    }

    @Test
    public void testGetDependencyURLs_urlsAreValidFileUrls() throws Exception
    {
        File outputDir     = tempFolder.newFolder("classes3");
        File testOutputDir = tempFolder.newFolder("test-classes3");

        URL[] urls = ProjectDependencyResolution.getDependencyURLs(
                DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE,
                emptyProject(),
                executionWithPhase("compile"),
                null,
                outputDir,
                testOutputDir,
                MojoTestSupport.EMPTY_RESOLVER
        );

        Assert.assertTrue("Should return at least one URL", urls.length > 0);
        for (URL url : urls)
        {
            Assert.assertEquals(
                    "Every URL should use the 'file' protocol, but got: " + url,
                    "file", url.getProtocol());
            // The URL path must resolve to an actual directory that was passed in
            java.nio.file.Path urlPath = java.nio.file.Paths.get(url.toURI());
            Assert.assertEquals(
                    "URL path should resolve to the projectOutputDirectory",
                    outputDir.toPath().toAbsolutePath(), urlPath.toAbsolutePath());
        }
    }

    // --- helpers ---

    private static MavenProject emptyProject()
    {
        return new MavenProject();
    }


    private static MojoExecution executionWithPhase(String phase)
    {
        MojoExecution execution = new MojoExecution(new MojoDescriptor(), "test-id");
        execution.setLifecyclePhase(phase);
        return execution;
    }
}

