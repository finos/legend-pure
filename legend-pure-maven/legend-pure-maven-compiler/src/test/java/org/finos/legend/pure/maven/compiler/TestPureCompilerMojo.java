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

package org.finos.legend.pure.maven.compiler;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.maven.shared.DependencyResolutionScope;
import org.finos.legend.pure.maven.shared.MojoTestSupport;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestPureCompilerMojo
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // --- resolveOutputDirectory() tests ---

    @Test
    public void testResolveOutputDirectory_explicitOutputDir() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File customDir = tempFolder.newFolder("custom-output");
        setField(mojo, "outputDirectory", customDir);
        setField(mojo, "mojoExecution", executionWithPhase("compile"));
        setField(mojo, "projectOutputDirectory", tempFolder.newFolder("classes"));
        setField(mojo, "projectTestOutputDirectory", tempFolder.newFolder("test-classes"));

        Assert.assertEquals(customDir, mojo.resolveOutputDirectory());
    }

    @Test
    public void testResolveOutputDirectory_testPhase_noExplicit() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File testClassesDir = tempFolder.newFolder("test-classes");
        setField(mojo, "outputDirectory", null);
        setField(mojo, "mojoExecution", executionWithPhase("test-compile"));
        setField(mojo, "projectOutputDirectory", tempFolder.newFolder("classes"));
        setField(mojo, "projectTestOutputDirectory", testClassesDir);

        Assert.assertEquals(testClassesDir, mojo.resolveOutputDirectory());
    }

    @Test
    public void testResolveOutputDirectory_nonTestPhase_noExplicit() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File classesDir = tempFolder.newFolder("classes");
        setField(mojo, "outputDirectory", null);
        setField(mojo, "mojoExecution", executionWithPhase("compile"));
        setField(mojo, "projectOutputDirectory", classesDir);
        setField(mojo, "projectTestOutputDirectory", tempFolder.newFolder("test-classes"));

        Assert.assertEquals(classesDir, mojo.resolveOutputDirectory());
    }

    // --- shouldSerializeIndividually() tests ---

    @Test
    public void testShouldSerializeIndividually_explicitTrue() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "compileIndividually", Boolean.TRUE);

        Assert.assertTrue(mojo.shouldSerializeIndividually(MojoTestSupport.setOf("a", "b")));
    }

    @Test
    public void testShouldSerializeIndividually_explicitFalse() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "compileIndividually", Boolean.FALSE);

        Assert.assertFalse(mojo.shouldSerializeIndividually(MojoTestSupport.setOf("a", "b")));
    }

    @Test
    public void testShouldSerializeIndividually_noExplicit_withRepos() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "compileIndividually", null);

        Assert.assertFalse(mojo.shouldSerializeIndividually(MojoTestSupport.setOf("a", "b")));
    }

    @Test
    public void testShouldSerializeIndividually_noExplicit_nullRepos() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "compileIndividually", null);

        Assert.assertTrue(mojo.shouldSerializeIndividually(null));
    }

    @Test
    public void testShouldSerializeIndividually_noExplicit_emptyRepos() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "compileIndividually", null);

        Assert.assertTrue(mojo.shouldSerializeIndividually(Collections.emptySet()));
    }

    // --- resolveRepositoriesToSerialize() tests ---

    @Test
    public void testResolveRepos_explicitRepositories() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        Set<String> repos = MojoTestSupport.setOf("a", "b");
        setField(mojo, "repositories", repos);
        setField(mojo, "excludedRepositories", null);

        Set<String> result = mojo.resolveRepositoriesToSerialize(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE);
        Assert.assertEquals(repos, result);
    }

    @Test
    public void testResolveRepos_explicitWithExclusion_noOverlap() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "repositories", MojoTestSupport.setOf("a", "b"));
        setField(mojo, "excludedRepositories", MojoTestSupport.setOf("c"));

        Set<String> result = mojo.resolveRepositoriesToSerialize(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE);
        Assert.assertEquals(MojoTestSupport.setOf("a", "b"), result);
    }

    @Test
    public void testResolveRepos_explicitWithExclusion_overlap() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "repositories", MojoTestSupport.setOf("a", "b"));
        setField(mojo, "excludedRepositories", MojoTestSupport.setOf("a"));

        MojoExecutionException e = Assert.assertThrows(MojoExecutionException.class, () -> mojo.resolveRepositoriesToSerialize(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE));
        Assert.assertEquals("Invalid repository specification; the following are both included and excluded: a", e.getMessage());
    }

    @Test
    public void testResolveRepos_noExplicit_noDefinitionFiles() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File outputDir = tempFolder.newFolder("empty-output");
        setField(mojo, "repositories", null);
        setField(mojo, "excludedRepositories", null);
        setField(mojo, "projectOutputDirectory", outputDir);
        setField(mojo, "mojoExecution", executionWithPhase("compile"));

        Set<String> result = mojo.resolveRepositoriesToSerialize(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE);
        Assert.assertNull("Expected null (meaning 'all') when no definition files found", result);
    }

    @Test
    public void testResolveRepos_noExplicit_withDefinitionFiles() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File outputDir = tempFolder.newFolder("output-with-defs");

        // Write valid .definition.json files
        writeDefinitionJson(outputDir, "my_repo", "my_repo", "(meta)(::.*)?", "platform");
        writeDefinitionJson(outputDir, "other_repo", "other_repo", "(meta)(::.*)?", "my_repo");

        setField(mojo, "repositories", null);
        setField(mojo, "excludedRepositories", null);
        setField(mojo, "projectOutputDirectory", outputDir);
        setField(mojo, "mojoExecution", executionWithPhase("compile"));

        Set<String> result = mojo.resolveRepositoriesToSerialize(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MojoTestSupport.setOf("my_repo", "other_repo"), result);
    }

    @Test
    public void testResolveRepos_noExplicit_withDefinitionFiles_andExclusion() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File outputDir = tempFolder.newFolder("output-with-defs-excl");

        writeDefinitionJson(outputDir, "my_repo", "my_repo", "(meta)(::.*)?", "platform");
        writeDefinitionJson(outputDir, "other_repo", "other_repo", "(meta)(::.*)?", "my_repo");

        setField(mojo, "repositories", null);
        setField(mojo, "excludedRepositories", MojoTestSupport.setOf("other_repo"));
        setField(mojo, "projectOutputDirectory", outputDir);
        setField(mojo, "mojoExecution", executionWithPhase("compile"));

        Set<String> result = mojo.resolveRepositoriesToSerialize(DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MojoTestSupport.setOf("my_repo"), result);
    }

    // --- forEachRepoDefinition() tests ---

    @Test
    public void testForEachRepoDefinition_emptyDir() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File emptyDir = tempFolder.newFolder("empty");

        List<String> collected = new ArrayList<>();
        mojo.forEachRepoDefinition(emptyDir, collected::add);

        Assert.assertEquals("Expected no repo names from empty directory", Collections.emptyList(), collected);
    }

    @Test
    public void testForEachRepoDefinition_noJsonFiles() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File dir = tempFolder.newFolder("no-json");
        Files.write(dir.toPath().resolve("readme.txt"), "hello".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.toPath().resolve("data.xml"), "<xml/>".getBytes(StandardCharsets.UTF_8));

        List<String> collected = new ArrayList<>();
        mojo.forEachRepoDefinition(dir, collected::add);

        Assert.assertEquals("Expected no repo names when no .definition.json files", Collections.emptyList(), collected);
    }

    @Test
    public void testForEachRepoDefinition_withDefinitionFiles() throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        File dir = tempFolder.newFolder("with-defs");

        writeDefinitionJson(dir, "repo_alpha", "repo_alpha", "(meta)(::.*)?", "platform");
        writeDefinitionJson(dir, "repo_beta", "repo_beta", "(meta)(::.*)?", "platform");

        // Also add a non-.definition.json file that should be ignored
        Files.write(dir.toPath().resolve("readme.txt"), "ignore me".getBytes(StandardCharsets.UTF_8));

        Set<String> collected = new HashSet<>();
        mojo.forEachRepoDefinition(dir, collected::add);

        Assert.assertEquals(MojoTestSupport.setOf("repo_alpha", "repo_beta"), collected);
    }

    // --- execute()-level tests ---

    @Test
    public void testExecute_compilesTestRepository() throws Exception
    {
        File outputDir = tempFolder.newFolder("exec-test-repo");
        PureCompilerMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", MojoTestSupport.setOf("test_generic_repository"));

        mojo.execute();

        FileDeserializer deserializer = newFileDeserializer();
        Assert.assertTrue("test_generic_repository manifest should be written",
                deserializer.moduleManifestExists(outputDir.toPath(), "test_generic_repository"));
        ModuleManifest manifest = deserializer.deserializeModuleManifest(outputDir.toPath(), "test_generic_repository");
        Assert.assertEquals("test_generic_repository", manifest.getModuleName());
    }

    @Test
    public void testExecute_respectsExplicitRepositorySet() throws Exception
    {
        File outputDir = tempFolder.newFolder("exec-explicit-repos");
        PureCompilerMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", MojoTestSupport.setOf("test_generic_repository"));

        mojo.execute();

        FileDeserializer deserializer = newFileDeserializer();
        Assert.assertTrue("test_generic_repository should be serialized",
                deserializer.moduleManifestExists(outputDir.toPath(), "test_generic_repository"));
        Assert.assertFalse("platform should NOT be serialized when not in explicit set",
                deserializer.moduleManifestExists(outputDir.toPath(), "platform"));
    }

    @Test
    public void testExecute_outputDirectoryIsCreatedIfAbsent() throws Exception
    {
        File base = tempFolder.newFolder("exec-absent-parent");
        File outputDir = new File(base, "non-existent-subdir");
        Assert.assertFalse("Pre-condition: output dir must not exist", outputDir.exists());

        PureCompilerMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories", MojoTestSupport.setOf("test_generic_repository"));

        mojo.execute();

        Assert.assertTrue("execute() must create the output directory", outputDir.exists());
        FileDeserializer deserializer = newFileDeserializer();
        Assert.assertTrue("test_generic_repository manifest should exist",
                deserializer.moduleManifestExists(outputDir.toPath(), "test_generic_repository"));
    }

    @Test
    public void testExecute_respectsExcludedRepositories() throws Exception
    {
        // Use explicit repositories including test repos; exclude other_test_generic_repository.
        // test_generic_repository and platform should be compiled; other_test_generic_repository should not.
        File outputDir = tempFolder.newFolder("exec-excluded");
        PureCompilerMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "repositories",        MojoTestSupport.setOf("test_generic_repository"));
        setField(mojo, "excludedRepositories", MojoTestSupport.setOf("other_test_generic_repository"));

        mojo.execute();

        FileDeserializer deserializer = newFileDeserializer();
        Assert.assertTrue("test_generic_repository should be serialized",
                deserializer.moduleManifestExists(outputDir.toPath(), "test_generic_repository"));
        Assert.assertFalse("other_test_generic_repository should NOT be serialized",
                deserializer.moduleManifestExists(outputDir.toPath(), "other_test_generic_repository"));
    }

    @Test
    public void testExecute_skip_preventsGeneration() throws Exception
    {
        File outputDir = tempFolder.newFolder("exec-skip");
        PureCompilerMojo mojo = buildExecuteMojo(outputDir);
        setField(mojo, "skip", true);

        mojo.execute();

        // Nothing should be written when skip=true
        FileDeserializer deserializer = newFileDeserializer();
        Assert.assertFalse("skip=true should produce no manifest output",
                deserializer.moduleManifestExists(outputDir.toPath(), "platform"));
    }

    // --- helpers for execute()-level tests ---

    private static final ProjectDependenciesResolver EMPTY_RESOLVER = MojoTestSupport.EMPTY_RESOLVER;

    private PureCompilerMojo buildExecuteMojo(File outputDir) throws Exception
    {
        PureCompilerMojo mojo = new PureCompilerMojo();
        setField(mojo, "outputDirectory",                    outputDir);
        setField(mojo, "projectOutputDirectory",             outputDir);
        setField(mojo, "projectTestOutputDirectory",         outputDir);
        setField(mojo, "mojoExecution",                      executionWithPhase("compile"));
        setField(mojo, "mavenProject",                       new MavenProject());
        setField(mojo, "mavenRepoSession",                   null);
        setField(mojo, "mavenProjectDependenciesResolver",   EMPTY_RESOLVER);
        setField(mojo, "repositories",                       null);
        setField(mojo, "excludedRepositories",               null);
        setField(mojo, "compileIndividually",                null);
        return mojo;
    }

    private static FileDeserializer newFileDeserializer()
    {
        return FileDeserializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions().build())
                .withConcreteElementDeserializer(ConcreteElementDeserializer.builder().withLoadedExtensions().build())
                .withModuleMetadataSerializer(ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
    }

    private static MojoExecution executionWithPhase(String phase)
    {
        return MojoTestSupport.executionWithPhase(phase);
    }

    // --- helpers ---

    private static void writeDefinitionJson(File directory, String fileName, String repoName, String pattern, String... dependencies) throws IOException
    {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\" : \"").append(repoName).append("\",\n");
        json.append("  \"pattern\" : \"").append(pattern).append("\",\n");
        json.append("  \"dependencies\" : [");
        for (int i = 0; i < dependencies.length; i++)
        {
            if (i > 0)
            {
                json.append(", ");
            }
            json.append("\"").append(dependencies[i]).append("\"");
        }
        json.append("]\n");
        json.append("}\n");
        Files.write(directory.toPath().resolve(fileName + ".definition.json"), json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        MojoTestSupport.setField(target, fieldName, value);
    }
}
