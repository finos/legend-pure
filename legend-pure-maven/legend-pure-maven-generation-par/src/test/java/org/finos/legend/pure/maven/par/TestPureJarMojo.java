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

package org.finos.legend.pure.maven.par;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.finos.legend.pure.maven.shared.MojoTestSupport;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Tests for {@link PureJarMojo}.
 *
 * <p>Covers two concerns:</p>
 * <ol>
 *   <li><strong>Configuration contract</strong> — verifies that every
 *       {@code @Parameter}-annotated field exists with the expected name and type.
 *       These fields are bound by name from downstream {@code pom.xml}
 *       {@code <configuration>} blocks via Maven's reflective injection, so renaming
 *       or retyping them is a silent breaking change for consumers.</li>
 *   <li><strong>Execution behaviour</strong> — calls {@code execute()} with real
 *       field injection to verify PAR file production, content, skip behaviour,
 *       and error wrapping.</li>
 * </ol>
 */
public class TestPureJarMojo
{
    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    // --- field-structure sanity tests ---

    @Test
    public void testIsAbstractMojo()
    {
        Assert.assertTrue("PureJarMojo should extend AbstractMojo",
                AbstractMojo.class.isAssignableFrom(PureJarMojo.class));
    }

    /**
     * Verifies that every {@code @Parameter}-annotated field still exists with the
     * expected name and type.
     *
     * <p>These fields are the public configuration contract of the plugin: downstream
     * projects bind to them by name in their {@code pom.xml} {@code <configuration>}
     * blocks, and the Maven plugin framework injects values reflectively at runtime.
     * Renaming or retyping a field is therefore a <strong>breaking change</strong> for
     * any project that configures the plugin — even though the compiler will not catch
     * it. This test acts as the regression guard for that contract.</p>
     *
     * <p>Example downstream usage that would silently break on a rename:</p>
     * <pre>{@code
     * <configuration>
     *     <outputDirectory>${project.build.directory}</outputDirectory>
     *     <repositories><repository>platform</repository></repositories>
     *     <purePlatformVersion>${platform.version}</purePlatformVersion>
     * </configuration>
     * }</pre>
     */
    @Test
    public void testMojoConfigurationFields_existWithExpectedTypes()
    {
        // Each entry is a field that maps directly to a <configuration> element in
        // downstream pom.xml files. Field name = XML element name; type must match
        // what Maven's reflection-based injector expects.
        assertFieldExists("outputDirectory",            File.class);
        assertFieldExists("sourceDirectory",            File.class);
        assertFieldExists("repositories",               Set.class);
        assertFieldExists("excludedRepositories",       Set.class);
        assertFieldExists("extraRepositories",          Set.class);
        assertFieldExists("dependencyScope",            String.class);
        assertFieldExists("purePlatformVersion",        String.class);
        assertFieldExists("modelVersion",               String.class);
        assertFieldExists("projectOutputDirectory",     File.class);
        assertFieldExists("projectTestOutputDirectory", File.class);
    }

    // --- execute()-level tests ---

    @Test
    public void testExecute_generatesPlatformPar() throws Exception
    {
        File outputDir = TMP.newFolder("exec-platform");
        PureJarMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", setOf("platform"));

        mojo.execute();

        File parFile = new File(outputDir, "pure-platform.par");
        Assert.assertTrue("pure-platform.par should exist", parFile.exists());
        Assert.assertTrue("pure-platform.par should be non-empty", parFile.length() > 0);
    }

    @Test
    public void testExecute_parFileContainsPcEntries() throws Exception
    {
        File outputDir = TMP.newFolder("exec-platform-contents");
        PureJarMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", setOf("platform"));

        mojo.execute();

        File parFile = new File(outputDir, "pure-platform.par");
        Assert.assertTrue("pure-platform.par should exist", parFile.exists());
        try (ZipFile zip = new ZipFile(parFile))
        {
            Assert.assertNotNull("MANIFEST.MF should be present",
                    zip.getEntry("META-INF/MANIFEST.MF"));
            long pcCount = zip.stream()
                    .filter(e -> e.getName().endsWith(".pc"))
                    .count();
            Assert.assertTrue("PAR should contain .pc files", pcCount > 0);
        }
    }

    @Test
    public void testExecute_generatesTestRepository() throws Exception
    {
        File outputDir = TMP.newFolder("exec-test-repo");
        PureJarMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", setOf("test_generic_repository"));

        mojo.execute();

        File parFile = new File(outputDir, "pure-test_generic_repository.par");
        Assert.assertTrue("pure-test_generic_repository.par should exist", parFile.exists());
        Assert.assertTrue("pure-test_generic_repository.par should be non-empty", parFile.length() > 0);
        try (ZipFile zip = new ZipFile(parFile))
        {
            long pcCount = zip.stream()
                    .filter(e -> e.getName().endsWith(".pc"))
                    .count();
            Assert.assertTrue("test_generic_repository PAR should contain at least 1 .pc entry", pcCount >= 1);
        }
    }

    @Test
    public void testExecute_onlyRequestedRepositoriesAreGenerated() throws Exception
    {
        // When an explicit repos set is given, only those repos (plus their transitive
        // deps) are serialised. Repos NOT in the transitive closure must be absent.
        // test_generic_repository depends on platform — both will be written.
        // other_test_generic_repository is NOT a dependency, so its PAR must be absent.
        File outputDir = TMP.newFolder("exec-explicit-only");
        PureJarMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", setOf("test_generic_repository"));

        mojo.execute();

        Assert.assertTrue("test_generic_repository.par should be present",
                new File(outputDir, "pure-test_generic_repository.par").exists());
        // other_test_generic_repository is not in the requested set nor a transitive dep
        Assert.assertFalse("pure-other_test_generic_repository.par should be absent",
                new File(outputDir, "pure-other_test_generic_repository.par").exists());
    }

    @Test
    public void testExecute_skip_preventsGeneration() throws Exception
    {
        File outputDir = TMP.newFolder("exec-skip-par");
        PureJarMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "skip", true);

        mojo.execute();

        Assert.assertFalse("skip=true should not write pure-platform.par",
                new File(outputDir, "pure-platform.par").exists());
    }

    @Test
    public void testExecute_invalidRepository_throwsMojoExecutionException() throws Exception
    {
        // An unknown repository name causes PureJarGenerator.doGeneratePAR() to throw.
        // The mojo's catch block must wrap it as MojoExecutionException with:
        //   - message containing "Error serializing Pure PAR"
        //   - a non-null cause (the original failure is preserved)
        File outputDir = TMP.newFolder("exec-error-par");
        PureJarMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", setOf("this_repository_does_not_exist"));

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException for unknown repository");
        }
        catch (org.apache.maven.plugin.MojoExecutionException e)
        {
            Assert.assertTrue(
                    "Message should contain 'Error serializing Pure PAR', but was: " + e.getMessage(),
                    e.getMessage().contains("Error serializing Pure PAR"));
            Assert.assertNotNull("Cause must be preserved through wrapping", e.getCause());
        }
    }

    // --- helpers for execute()-level tests ---

    private static final ProjectDependenciesResolver EMPTY_RESOLVER = MojoTestSupport.EMPTY_RESOLVER;

    private static PureJarMojo buildExecuteMojo(File outputDir) throws Exception
    {
        PureJarMojo mojo = new PureJarMojo();
        setField(mojo, "outputDirectory",                    outputDir);
        setField(mojo, "projectOutputDirectory",             outputDir);
        setField(mojo, "projectTestOutputDirectory",         outputDir);
        setField(mojo, "mojoExecution",                      executionWithPhase("compile"));
        setField(mojo, "mavenProject",                       new MavenProject());
        setField(mojo, "mavenRepoSession",                   null);
        setField(mojo, "mavenProjectDependenciesResolver",   EMPTY_RESOLVER);
        setField(mojo, "repositories",                       null);
        setField(mojo, "excludedRepositories",               null);
        setField(mojo, "extraRepositories",                  null);
        setField(mojo, "purePlatformVersion",                null);
        setField(mojo, "modelVersion",                       null);
        setField(mojo, "sourceDirectory",                    null);
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

    private static Set<String> setOf(String... values)
    {
        return MojoTestSupport.setOf(values);
    }

    private static void assertFieldExists(String name, Class<?> expectedType)
    {
        try
        {
            Field field = PureJarMojo.class.getDeclaredField(name);
            Assert.assertEquals("Field '" + name + "' should have type " + expectedType.getSimpleName(),
                    expectedType, field.getType());
        }
        catch (NoSuchFieldException e)
        {
            Assert.fail("Expected field '" + name + "' not found on PureJarMojo");
        }
    }
}
