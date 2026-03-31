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
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

        // Monolithic generation writes metadata to target/metadata-distributed.
        // The serializer writes metadata/bin/*.bin and metadata/classifiers/*.idx inside it.
        File metaDir = new File(targetDir, "metadata-distributed");
        Assert.assertTrue("metadata-distributed directory should be created", metaDir.exists());

        // metadata/bin/ and metadata/classifiers/ are structural invariants
        File binDir        = new File(metaDir, "metadata/bin");
        File classifiersDir = new File(metaDir, "metadata/classifiers");
        Assert.assertTrue("metadata/bin should exist under metadata-distributed",        binDir.isDirectory());
        Assert.assertTrue("metadata/classifiers should exist under metadata-distributed", classifiersDir.isDirectory());

        // Package.idx is a stable landmark file always produced for the platform repository
        File packageIdx = new File(classifiersDir, "Package.idx");
        Assert.assertTrue(
                "metadata/classifiers/Package.idx should always be generated for the platform repository",
                packageIdx.exists());
        Assert.assertTrue(
                "Package.idx should be a non-empty file",
                packageIdx.length() > 0);
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
        mojo.execute();

        // JavaCodeGeneration creates targetDirectory; verify it now exists
        Assert.assertTrue("targetDirectory should be created by execute()", targetDir.exists());
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
    public void testExecute_useSingleDir_writesMetadataToClassesDirectory() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-singledir-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "useSingleDir", true);
        mojo.execute();

        // With useSingleDir=true metadata goes directly to classesDirectory.
        // The serializer writes metadata/bin/*.bin and metadata/classifiers/*.idx inside it.
        // Verify files exist under classesDir and no separate metadata-distributed was created.
        File binDir         = new File(classesDir, "metadata/bin");
        File classifiersDir = new File(classesDir, "metadata/classifiers");
        Assert.assertTrue("useSingleDir=true should write metadata/bin into classesDirectory",
                binDir.isDirectory() && binDir.list() != null && binDir.list().length > 0);
        Assert.assertFalse("useSingleDir=true should NOT create a separate metadata-distributed directory",
                new File(targetDir, "metadata-distributed").exists());
        // Package.idx is a stable landmark always produced for the platform repository
        File packageIdx = new File(classifiersDir, "Package.idx");
        Assert.assertTrue("metadata/classifiers/Package.idx should exist inside classesDirectory",
                packageIdx.exists() && packageIdx.length() > 0);
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

        // Modular generation scopes output by repository name.
        // Bins at metadata/bin/platform/ and classifiers at metadata/classifiers/platform/.
        File metaDir    = new File(targetDir, "metadata-distributed");
        Assert.assertTrue("metadata-distributed directory should be created for modular generation",
                metaDir.exists());
        File binDir     = new File(metaDir, "metadata/bin/platform");
        File packageIdx = new File(metaDir, "metadata/classifiers/platform/Package.idx");
        Assert.assertTrue("modular generation should write metadata/bin/platform/ into metadata-distributed",
                binDir.isDirectory() && binDir.list() != null && binDir.list().length > 0);
        Assert.assertTrue("metadata/classifiers/platform/Package.idx should exist for modular platform generation",
                packageIdx.exists() && packageIdx.length() > 0);
    }

    @Test
    public void testExecute_addExternalAPI_doesNotThrow() throws Exception
    {
        File targetDir  = TMP.newFolder("exec-extapi-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "addExternalAPI",    true);
        setField(mojo, "externalAPIPackage", "org.finos.legend.pure.generated");
        mojo.execute();

        try (Stream<Path> stream = Files.walk(targetDir.toPath()))
        {
            long count = stream.filter(Files::isRegularFile).count();
            Assert.assertTrue("addExternalAPI=true should produce output", count > 0);
        }
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

    // --- engine production pattern: modular + useSingleDir + generateSources + preventJavaCompilation ---

    @Test
    public void testExecute_enginePattern_modularUseSingleDirWithSources() throws Exception
    {
        // This is the exact parameter combination used by all 139 engine modules:
        //   generationType=modular, useSingleDir=true, generateSources=true, preventJavaCompilation=true
        // All output (metadata + generated sources) goes into classesDirectory rather than
        // a separate metadata-distributed/ directory.
        File targetDir  = TMP.newFolder("exec-engine-pattern-target");
        File classesDir = new File(targetDir, "classes");
        classesDir.mkdir();

        PureCompiledJarMojo mojo = buildMojo(targetDir, classesDir, setOf("platform"));
        setField(mojo, "generationType",         JavaCodeGeneration.GenerationType.modular);
        setField(mojo, "useSingleDir",           true);
        setField(mojo, "generateSources",        true);
        setField(mojo, "preventJavaCompilation", true);
        mojo.execute();

        // With useSingleDir=true, all output goes into classesDirectory — no separate metadata-distributed/
        Assert.assertFalse(
                "useSingleDir=true must not create a separate metadata-distributed/ directory",
                new File(targetDir, "metadata-distributed").exists());

        // Generated Java source — with useSingleDir=true, sources go to targetDir/generated-sources/
        // (not inside classesDir); only metadata is written into classesDir
        File packageImpl = new File(targetDir,
                "generated-sources/org/finos/legend/pure/generated/Package_Impl.java");
        Assert.assertTrue(
                "Package_Impl.java should be generated in targetDir/generated-sources/ " +
                "(useSingleDir=true routes metadata to classesDir but sources to targetDir/generated-sources/)",
                packageImpl.exists());
        String src = new String(java.nio.file.Files.readAllBytes(packageImpl.toPath()));
        Assert.assertTrue("Package_Impl.java should declare class Package_Impl", src.contains("class Package_Impl"));

        // Metadata — modular + useSingleDir writes to classesDir/metadata/classifiers/platform/
        File packageIdx = new File(classesDir, "metadata/classifiers/platform/Package.idx");
        Assert.assertTrue(
                "metadata/classifiers/platform/Package.idx should exist inside classesDirectory",
                packageIdx.exists() && packageIdx.length() > 0);
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
        setField(mojo, "useSingleDir",             false);
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
