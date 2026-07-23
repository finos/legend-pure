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

package org.finos.legend.pure.maven.javaCompiled;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.finos.legend.pure.maven.shared.MojoTestSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaFileObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/**
 * execute()-level tests for {@link PureCompiledJarMojo}.
 *
 * <p>All {@code @Parameter}-annotated fields are injected via reflection
 * (no {@code maven-plugin-testing-harness} required). The mojo's
 * {@code buildClassLoader()} reads {@code project.getCompileClasspathElements()},
 * which returns an empty list for a default {@code MavenProject} — exactly what we
 * want, because the parent class loader (the test JVM) already has the Pure
 * repositories on its classpath.</p>
 *
 * <p>{@code preventJavaCompilation = true} is used throughout to skip the
 * {@code javac} step and keep tests fast (3-10s per test as measured); enabling
 * javac would add substantial compilation time on top of the generation phase.</p>
 */
public class TestPureCompiledJarMojo
{
    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    // --- execute()-level tests ---

    @Test
    public void testExecute_generatesMonolithicOutput() throws Exception
    {
        File targetDir     = TMP.newFolder("exec-monolithic-target");
        File classesDir    = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        mojo.execute();

        // Monolithic generation no longer writes metadata, but relies on it to already exist
        File metaDir = new File(targetDir, "metadata-distributed");
        Assert.assertFalse("metadata-distributed directory should not be created", metaDir.exists());
    }

    @Test
    public void testExecute_skipFlag_preventsGeneration() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-skip-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "skip", true);
        mojo.execute();

        // skip=true should produce nothing under targetDir beyond the pre-created classesDir
        try (Stream<Path> stream = Files.walk(targetDir.toPath()))
        {
            long count = stream.filter(Files::isRegularFile).count();
            Assert.assertEquals("skip=true should produce no output files", 0, count);
        }
    }

    @Test
    public void testExecute_directoriesCreatedIfAbsent() throws Exception
    {
        File base        = TMP.newFolder("exec-absent-dirs");
        File targetDir   = new File(base, "non-existent-target");
        File classesDir  = new File(base, "non-existent-classes");

        Assert.assertFalse("Pre-condition: targetDir must not exist",  targetDir.exists());
        Assert.assertFalse("Pre-condition: classesDir must not exist", classesDir.exists());

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "generateSources", true);
        setField(mojo, "preventJavaCompilation", false);
        mojo.execute();

        // JavaCodeGeneration creates targetDirectory; verify it now exists
        Assert.assertTrue("targetDirectory should be created by execute()", targetDir.exists());
        Assert.assertTrue("classesDirectory should be created by execute()", classesDir.exists());
    }

    @Test
    public void testExecute_generateSources_producesJavaSources() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-sources-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "generateSources", true);
        mojo.execute();

        // With generateSources=true, JavaCodeGeneration writes to target/generated-sources
        try (Stream<Path> stream = Files.walk(targetDir.toPath()))
        {
            long count = stream.filter(Files::isRegularFile).count();
            Assert.assertTrue("generateSources=true should produce at least one file", count > 0);
        }
    }

    @Test
    public void testExecute_modularGeneration_producesOutput() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-modular-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "generationType", JavaCodeGeneration.GenerationType.modular);
        mojo.execute();

        // Modular generation now uses pelt serialization, which must already exist; so there should be no metadata generation
        File metaDir = new File(targetDir, "metadata-distributed");
        Assert.assertFalse("metadata-distributed directory should not be created", metaDir.exists());
    }

    @Test
    public void testExecute_addExternalAPI() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-extapi-target");
        File classesDir = new File(targetDir, "classes");

        String externalAPIPackage = "org.finos.legend.pure.generated";
        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "addExternalAPI",    true);
        setField(mojo, "externalAPIPackage", externalAPIPackage);
        setField(mojo, "generateSources", true);
        setField(mojo, "preventJavaCompilation", false);
        mojo.execute();

        Path expectedSource = targetDir.toPath().resolve("generated-sources/" + externalAPIPackage.replace('.', '/') + "/" + JavaSourceCodeGenerator.EXTERNAL_FUNCTIONS_CLASS_NAME + JavaFileObject.Kind.SOURCE.extension);
        Assert.assertTrue(expectedSource.toString(), Files.exists(expectedSource));
        Path expectedClass = classesDir.toPath().resolve(externalAPIPackage.replace('.', '/') + "/" + JavaSourceCodeGenerator.EXTERNAL_FUNCTIONS_CLASS_NAME + JavaFileObject.Kind.CLASS.extension);
        Assert.assertTrue(expectedClass.toString(), Files.exists(expectedClass));
    }

    @Test
    public void testExecute_generateMetadataFalse_skipsMetadata() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-no-metadata-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "generateMetadata", false);
        mojo.execute();

        // With generateMetadata=false and preventJavaCompilation=true no files go to classesDir
        try (Stream<Path> stream = Files.walk(classesDir.toPath()))
        {
            long count = stream.filter(Files::isRegularFile).count();
            Assert.assertEquals("generateMetadata=false should write no files to classesDirectory", 0, count);
        }
    }

    // --- error path ---

    @Test
    public void testExecute_unknownRepository_throwsWrappedException() throws Exception
    {
        // An unknown repository name causes JavaCodeGeneration.doIt() to throw RuntimeException,
        // which PureCompiledJarMojo wraps as MojoExecutionException. The message must identify
        // the failure clearly and preserve the original cause.
        File targetDir  = TMP.newFolder("exec-error-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("this_repository_does_not_exist"));
        MojoExecutionException e = Assert.assertThrows(MojoExecutionException.class, mojo::execute);
        Assert.assertTrue(
                "Message should contain 'Error building Pure compiled mode jar', but was: " + e.getMessage(),
                e.getMessage().contains("Error building Pure compiled mode jar"));
        Assert.assertNotNull("Cause must be preserved", e.getCause());
    }

    // --- helpers ---

    private static PureCompiledJarMojo buildMojo(File targetDir, File classesDir, Set<String> repositories) throws Exception
    {
        PureCompiledJarMojo mojo = new PureCompiledJarMojo();
        setField(mojo, "mavenProject",                      new MavenProject());
        setField(mojo, "mojoExecution",                     MojoTestSupport.executionWithPhase("compile"));
        setField(mojo, "mavenRepoSession",                  null);
        setField(mojo, "projectOutputDirectory",            classesDir != null ? classesDir : targetDir);
        setField(mojo, "projectTestOutputDirectory",        classesDir != null ? classesDir : targetDir);
        setField(mojo, "mavenProjectDependenciesResolver",  MojoTestSupport.EMPTY_RESOLVER);
        setField(mojo, "targetDirectory",                   targetDir);
        setField(mojo, "classesDirectory",         classesDir);
        setField(mojo, "skip",                     false);
        setField(mojo, "repositories",             repositories);
        setField(mojo, "excludedRepositories",     null);
        setField(mojo, "extraRepositories",        null);
        setField(mojo, "generationType",           JavaCodeGeneration.GenerationType.monolithic);
        setField(mojo, "addExternalAPI",           false);
        setField(mojo, "externalAPIPackage",       "org.finos.legend.pure.generated");
        setField(mojo, "generateMetadata",         true);
        setField(mojo, "generateSources",          false);
        setField(mojo, "preventJavaCompilation",   true);
        setField(mojo, "generatePureTests",        false);
        return mojo;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        MojoTestSupport.setField(target, fieldName, value);
    }

    private static Set<String> setOf(String... values)
    {
        return MojoTestSupport.setOf(values);
    }
}
