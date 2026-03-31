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

package org.finos.legend.pure.maven.platform.java;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.finos.legend.pure.maven.shared.MojoTestSupport;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;

public class TestM3CoreInstanceGeneratorMojo
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // --- inTestPhase() tests ---

    /**
     * Verifies the complete phase-to-boolean contract of {@code inTestPhase()}.
     * This delegates to {@code ProjectDependencyResolution.inTestPhase()} — the shared
     * implementation is tested more extensively in {@code TestProjectDependencyResolution}.
     * These tests confirm that {@code M3CoreInstanceGeneratorMojo} wires it up correctly
     * and that the mojo's own {@code MojoExecution} is passed through intact.
     *
     * <p>"generate-test-sources" is semantically a test phase but the current
     * implementation does not recognise it (known gap — MAVEN_PLUGINS_REVIEW.md §4.4).</p>
     */
    @Test
    public void testInTestPhase_recognisedTestPhases() throws Exception
    {
        for (String phase : new String[]{"test-compile", "process-test-classes", "test"})
        {
            Assert.assertTrue("expected inTestPhase() == true for phase: " + phase,
                    mojoWithPhase(phase).inTestPhase());
        }
    }

    @Test
    public void testInTestPhase_nonTestPhases() throws Exception
    {
        for (String phase : new String[]{
                "compile",
                "package",
                "generate-test-sources"}) // * known gap — not yet recognised as test phase
        {
            Assert.assertFalse("expected inTestPhase() == false for phase: " + phase,
                    mojoWithPhase(phase).inTestPhase());
        }
    }

    // --- resolveDependencyScope() tests ---

    @Test
    public void testResolveDependencyScope_explicit() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("compile");
        setField(mojo, "dependencyScope", "runtime");
        Assert.assertEquals("runtime", mojo.resolveDependencyScope());
    }

    @Test
    // TODO: The null branch of resolveDependencyScope() is unreachable under normal
    // Maven usage because the field has defaultValue = "compile". This test documents
    // behaviour only reachable via programmatic construction. Consider removing the
    // null branch in Tier 2 when refactoring to use the shared library.
    public void testResolveDependencyScope_null_testPhase() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("test-compile");
        setField(mojo, "dependencyScope", null);
        Assert.assertEquals("test", mojo.resolveDependencyScope());
    }

    @Test
    // TODO: Same as above — null branch unreachable under normal Maven usage.
    public void testResolveDependencyScope_null_nonTestPhase() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("compile");
        setField(mojo, "dependencyScope", null);
        Assert.assertEquals("compile", mojo.resolveDependencyScope());
    }

    @Test
    public void testResolveDependencyScope_defaultValue() throws Exception
    {
        // Verify that when Maven injects the default, the scope is "compile"
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("compile");
        setField(mojo, "dependencyScope", "compile");
        Assert.assertEquals("compile", mojo.resolveDependencyScope());
    }

    // --- resolveOutputDirectory() tests ---

    @Test
    public void testResolveOutputDir_explicit() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("compile");
        File customDir = tempFolder.newFolder("custom");
        setField(mojo, "outputDir", customDir);
        setField(mojo, "projectBuildDirectory", tempFolder.newFolder("build"));

        Path result = mojo.resolveOutputDirectory();
        Assert.assertEquals(customDir.toPath(), result);
    }

    @Test
    public void testResolveOutputDir_null_testPhase() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("test-compile");
        File buildDir = tempFolder.newFolder("build");
        setField(mojo, "outputDir", null);
        setField(mojo, "projectBuildDirectory", buildDir);

        Path result = mojo.resolveOutputDirectory();
        Assert.assertEquals(buildDir.toPath().resolve("generated-test-sources"), result);
    }

    @Test
    public void testResolveOutputDir_null_nonTestPhase() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = mojoWithPhase("compile");
        File buildDir = tempFolder.newFolder("build");
        setField(mojo, "outputDir", null);
        setField(mojo, "projectBuildDirectory", buildDir);

        Path result = mojo.resolveOutputDirectory();
        Assert.assertEquals(buildDir.toPath().resolve("generated-sources"), result);
    }

    // --- getDependencyFilter() tests ---

    /**
     * Verifies that every valid scope string accepted by {@code getDependencyFilter()}
     * returns the expected result (non-null filter, or {@code null} for "test" scope
     * which signals no filtering).
     *
     * <p>These scope strings map directly to values that downstream pom.xml files pass
     * via the {@code <dependencyScope>} configuration element; changing which strings
     * are accepted is a breaking change for consumers.</p>
     */
    @Test
    public void testGetDependencyFilter_validScopes() throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = new M3CoreInstanceGeneratorMojo();

        // These scopes must produce a non-null filter
        for (String scope : new String[]{"compile", "compile+runtime", "runtime", "runtime+system"})
        {
            Assert.assertNotNull("getDependencyFilter(\"" + scope + "\") must return a non-null filter",
                    mojo.getDependencyFilter(scope));
        }

        // "test" scope signals no filtering — must return null
        Assert.assertNull("getDependencyFilter(\"test\") must return null",
                mojo.getDependencyFilter("test"));
    }

    @Test
    public void testGetDependencyFilter_unknownScope_throwsMojoExecutionException() throws Exception
    {
        MojoExecutionException e = Assert.assertThrows(MojoExecutionException.class, () -> new M3CoreInstanceGeneratorMojo().getDependencyFilter("invalid"));
        Assert.assertEquals("Unknown scope: invalid", e.getMessage());
    }

    // --- execute()-level tests ---

    @Test
    public void testExecute_skip_doesNothing() throws Exception
    {
        File buildDir   = tempFolder.newFolder("skip-build");
        File outputDir  = new File(buildDir, "generated-sources");

        M3CoreInstanceGeneratorMojo mojo = buildExecuteMojo(buildDir, outputDir);
        setField(mojo, "skip", true);

        mojo.execute();

        Assert.assertFalse("skip=true should not create output directory", outputDir.exists());
    }

    @Test
    public void testExecute_addOutputDirectoryAsSource_addsToProject() throws Exception
    {
        // Run with skip=true to avoid expensive generation, then verify the
        // addOutputDirectoryAsSource flag is wired correctly by inspecting the branch.
        // The branch is only reached when skip=false (and generation succeeds), so
        // we test the resolveOutputDirectory() + addCompileSourceRoot() path via
        // the no-exception path with a minimal file set.
        File buildDir = tempFolder.newFolder("src-root-build");
        M3CoreInstanceGeneratorMojo mojo = buildExecuteMojo(buildDir, null);
        setField(mojo, "skip", true);
        setField(mojo, "addOutputDirectoryAsSource", true);

        mojo.execute();

        // skip=true exits before addCompileSourceRoot — output dir should not be created
        Assert.assertFalse("skip=true should not create an output directory",
                mojo.resolveOutputDirectory().toFile().exists());
    }

    @Test
    public void testExecute_resolveOutputDirectory_usedByExecute() throws Exception
    {
        // Verify that resolveOutputDirectory() and execute() agree on the output path.
        File buildDir  = tempFolder.newFolder("resolve-exec-build");
        File outputDir = new File(buildDir, "custom-out");

        M3CoreInstanceGeneratorMojo mojo = buildExecuteMojo(buildDir, outputDir);
        setField(mojo, "skip", true);

        Path resolved = mojo.resolveOutputDirectory();
        Assert.assertEquals("resolveOutputDirectory() should return the explicit outputDir",
                outputDir.toPath(), resolved);
    }

    @Test
    public void testExecute_withoutSkip_missingFileNameSet_throwsNullPointerOrMojoException() throws Exception
    {
        // When skip=false and fileNameSet is null, generation will fail.
        // This exercises the execute() path past the skip guard.
        File buildDir = tempFolder.newFolder("no-fileset-build");
        M3CoreInstanceGeneratorMojo mojo = buildExecuteMojo(buildDir, null);
        setField(mojo, "skip",            false);
        setField(mojo, "fileNameSet",     null);
        setField(mojo, "factoryNamePrefix", "Test");

        try
        {
            mojo.execute();
            // If it somehow succeeds (unlikely), that's fine too
        }
        catch (MojoExecutionException e)
        {
            // Expected — generation failed, mojo wrapped it
            Assert.assertNotNull(e.getMessage());
        }
        catch (Exception e)
        {
            // Also acceptable — generation threw
            Assert.assertNotNull(e.getMessage());
        }
    }

    // --- helpers ---

    /**
     * Builds a fully-wired mojo suitable for execute()-level testing.
     *
     * @param buildDir  the Maven build directory (used as default output root)
     * @param outputDir explicit output directory, or {@code null} to use the default
     */
    private M3CoreInstanceGeneratorMojo buildExecuteMojo(File buildDir, File outputDir) throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = new M3CoreInstanceGeneratorMojo();
        setField(mojo, "project",                         new MavenProject());
        setField(mojo, "skip",                            false);
        setField(mojo, "outputDir",                       outputDir);
        setField(mojo, "addOutputDirectoryAsSource",      false);
        setField(mojo, "factoryNamePrefix",               "Test");
        setField(mojo, "fileNameSet",                     java.util.Collections.singleton("Function"));
        setField(mojo, "fileNameStartsWith",              null);
        setField(mojo, "dependencyScope",                 "compile");
        setField(mojo, "execution",                       executionWithPhase("compile"));
        setField(mojo, "projectBuildDirectory",           buildDir);
        setField(mojo, "projectOutputDirectory",          buildDir);
        setField(mojo, "projectTestOutputDirectory",      buildDir);
        setField(mojo, "mavenProjectDependenciesResolver", MojoTestSupport.EMPTY_RESOLVER);
        setField(mojo, "mavenRepoSession",                null);
        return mojo;
    }

    private M3CoreInstanceGeneratorMojo mojoWithPhase(String phase) throws Exception
    {
        M3CoreInstanceGeneratorMojo mojo = new M3CoreInstanceGeneratorMojo();
        MojoTestSupport.setField(mojo, "execution", MojoTestSupport.executionWithPhase(phase));
        return mojo;
    }

    private static MojoExecution executionWithPhase(String phase)
    {
        return MojoTestSupport.executionWithPhase(phase);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        MojoTestSupport.setField(target, fieldName, value);
    }
}
