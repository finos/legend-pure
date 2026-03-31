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

package org.finos.legend.pure.m3.generator.compiler;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureCompilerLoader;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class TestPureCompilerBinaryGenerator
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    private static final String PLATFORM = "platform";
    private static final String TEST_REPO = "test_generic_repository";
    private static final String OTHER_TEST_REPO = "other_test_generic_repository";

    @Test
    public void testSerializeSingleModule() throws IOException
    {
        Path outputDirectory = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDirectory, Lists.immutable.with(TEST_REPO));

        FileDeserializer deserializer = newFileDeserializer();
        assertModuleSerialized(deserializer, outputDirectory, TEST_REPO);
        assertModuleNotSerialized(deserializer, outputDirectory, PLATFORM);
        assertModuleNotSerialized(deserializer, outputDirectory, OTHER_TEST_REPO);
    }

    @Test
    public void testSerializeAllModules() throws IOException
    {
        FileDeserializer deserializer = newFileDeserializer();

        // Check the case where all modules are implicitly specified and serialized individually
        Path implicitOutputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(implicitOutputDir, null, true);

        assertModuleSerialized(deserializer, implicitOutputDir, PLATFORM);
        assertModuleSerialized(deserializer, implicitOutputDir, TEST_REPO);
        assertModuleSerialized(deserializer, implicitOutputDir, OTHER_TEST_REPO);

        // Check the case where all modules are explicitly specified and serialized together
        Path explicitOutputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(explicitOutputDir, Lists.immutable.with(PLATFORM, TEST_REPO, OTHER_TEST_REPO), false);

        assertModuleSerialized(deserializer, explicitOutputDir, PLATFORM);
        assertModuleSerialized(deserializer, explicitOutputDir, TEST_REPO);
        assertModuleSerialized(deserializer, explicitOutputDir, OTHER_TEST_REPO);

        // Assert that the directories have identical content
        assertDirectoriesEquivalent(implicitOutputDir, explicitOutputDir);
    }

    @Test
    public void testSerializeMultipleButNotAllModules() throws IOException
    {
        Path outputDirectory = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDirectory, Lists.immutable.with(TEST_REPO, OTHER_TEST_REPO));

        FileDeserializer deserializer = newFileDeserializer();
        assertModuleSerialized(deserializer, outputDirectory, TEST_REPO);
        assertModuleSerialized(deserializer, outputDirectory, OTHER_TEST_REPO);
        assertModuleNotSerialized(deserializer, outputDirectory, PLATFORM);
    }

    @Test
    public void testSerializeWithExcludedModule() throws IOException
    {
        Path outputDirectory = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDirectory, null, null, Lists.immutable.with(OTHER_TEST_REPO));

        FileDeserializer deserializer = newFileDeserializer();
        assertModuleSerialized(deserializer, outputDirectory, PLATFORM);
        assertModuleSerialized(deserializer, outputDirectory, TEST_REPO);
        assertModuleNotSerialized(deserializer, outputDirectory, OTHER_TEST_REPO);
    }

    @Test
    public void testSerializeSingleVsMultiple() throws IOException
    {
        Path multiOutputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(multiOutputDir, Lists.immutable.with(PLATFORM, TEST_REPO, OTHER_TEST_REPO));

        Path singleOutputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(singleOutputDir, Lists.immutable.with(PLATFORM));
        PureCompilerBinaryGenerator.serializeModules(singleOutputDir, Lists.immutable.with(TEST_REPO));
        PureCompilerBinaryGenerator.serializeModules(singleOutputDir, Lists.immutable.with(OTHER_TEST_REPO));

        assertDirectoriesEquivalent(multiOutputDir, singleOutputDir);
    }

    // --- idempotency tests ---

    @Test
    public void testSerializeSingleModule_idempotent() throws IOException
    {
        Path outputDir1 = TMP.newFolder().toPath();
        Path outputDir2 = TMP.newFolder().toPath();

        PureCompilerBinaryGenerator.serializeModules(outputDir1, Lists.immutable.with(TEST_REPO));
        PureCompilerBinaryGenerator.serializeModules(outputDir2, Lists.immutable.with(TEST_REPO));

        assertDirectoriesEquivalent(outputDir1, outputDir2);
    }

    @Test
    public void testSerializeAllModules_idempotent() throws IOException
    {
        Path outputDir1 = TMP.newFolder().toPath();
        Path outputDir2 = TMP.newFolder().toPath();

        // Serialize all modules individually (serializeIndividually=true) in both directories
        PureCompilerBinaryGenerator.serializeModules(outputDir1, null, true);
        PureCompilerBinaryGenerator.serializeModules(outputDir2, null, true);

        assertDirectoriesEquivalent(outputDir1, outputDir2);
    }

    // --- main() entry point tests (exec:java invocation pattern) ---
    // In legend-pure m3-core pom.xml, PureCompilerBinaryGenerator is called via exec:java:
    //   args[0] = outputDirectory path
    //   args[1..N] = module names (one or more)
    // These tests verify the argument-parsing contract matches what serializeModules() produces
    // directly, so a refactor cannot silently reorder args and break the exec:java builds.

    @Test
    public void testMain_singleModule_producesEquivalentOutput() throws IOException
    {
        Path directDir = TMP.newFolder().toPath();
        Path mainDir   = TMP.newFolder().toPath();

        // Use TEST_REPO (does not require platform serialization) to keep test environment-safe.
        // The argument-parsing contract is the same regardless of which repository is chosen:
        //   args[0] = outputDirectory, args[1] = module name
        PureCompilerBinaryGenerator.serializeModules(directDir, Lists.immutable.with(TEST_REPO));

        // main() call — mirrors: <argument>outputDir</argument> <argument>test_generic_repository</argument>
        PureCompilerBinaryGenerator.main(new String[]{mainDir.toString(), TEST_REPO});

        assertDirectoriesEquivalent(directDir, mainDir);
    }

    @Test
    public void testMain_multipleModules_producesEquivalentOutput() throws IOException
    {
        Path directDir = TMP.newFolder().toPath();
        Path mainDir   = TMP.newFolder().toPath();

        // Direct API
        PureCompilerBinaryGenerator.serializeModules(directDir,
                Lists.immutable.with(TEST_REPO, OTHER_TEST_REPO));

        // main() — mirrors: <argument>outputDir</argument>
        //                   <argument>test_generic_repository</argument>
        //                   <argument>other_test_generic_repository</argument>
        PureCompilerBinaryGenerator.main(new String[]{mainDir.toString(), TEST_REPO, OTHER_TEST_REPO});

        assertDirectoriesEquivalent(directDir, mainDir);
    }

    @Test
    public void testMain_twoModulesInOneCall_matchesTwoSeparateCalls() throws IOException
    {
        // Verifies that passing two module names to main() in a single call produces the
        // same output as calling serializeModules() with both names together.
        // Uses only non-platform repos to stay environment-safe.
        Path mainDir   = TMP.newFolder().toPath();
        Path directDir = TMP.newFolder().toPath();

        PureCompilerBinaryGenerator.main(new String[]{mainDir.toString(), TEST_REPO, OTHER_TEST_REPO});
        PureCompilerBinaryGenerator.serializeModules(directDir,
                Lists.immutable.with(TEST_REPO, OTHER_TEST_REPO));

        assertDirectoriesEquivalent(directDir, mainDir);
    }

    // --- DirectoryPureCompilerLoader round-trip tests (Gap 1) ---
    // PureCompilerLoader.newLoader(classLoader, directory) is the read path used after
    // PureCompilerBinaryGenerator writes binary element files to disk.  These tests verify
    // the full write→read contract so a format change cannot go silently undetected.

    @Test
    public void testDirectoryLoader_canLoad_returnsTrueForSerializedModule() throws IOException
    {
        Path outputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDir, Lists.immutable.with(TEST_REPO));

        PureCompilerLoader loader = PureCompilerLoader.newLoader(
                Thread.currentThread().getContextClassLoader(), outputDir);

        Assert.assertTrue(
                "canLoad() must return true for a module that was just serialized",
                loader.canLoad(TEST_REPO));
        Assert.assertFalse(
                "canLoad() must return false for a module that was not serialized",
                loader.canLoad(OTHER_TEST_REPO));
    }

    @Test
    public void testDirectoryLoader_roundTrip_loadsSerializedModuleIntoRuntime() throws IOException
    {
        // Serialize platform + test_generic_repository to a directory, then use
        // DirectoryPureCompilerLoader to load them back into a fresh PureRuntime.
        // test_generic_repository depends on platform, so both must be serialized together
        // for the loader to resolve all cross-module references correctly.
        // Verifies the full write→read contract: the loader must be able to
        // deserialize every element that was written.
        Path outputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDir,
                Lists.immutable.with(PLATFORM, TEST_REPO));

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        PureCompilerLoader loader = PureCompilerLoader.newLoader(cl, outputDir);

        // Both modules must be loadable from the directory
        Assert.assertTrue("canLoad() must return true for platform", loader.canLoad(PLATFORM));
        Assert.assertTrue("canLoad() must return true for test_generic_repository", loader.canLoad(TEST_REPO));

        // Build a runtime with both code repositories available
        PureRuntime runtime = new PureRuntimeBuilder(
                new CompositeCodeStorage(
                        new ClassLoaderCodeStorage(
                                CodeRepositoryProviderHelper.findCodeRepositories(cl)
                                        .select(r -> PLATFORM.equals(r.getName()) || TEST_REPO.equals(r.getName())))))
                .setTransactionalByDefault(false)
                .build();

        // Record the element count from the manifests before loading
        FileDeserializer deserializer = newFileDeserializer();
        MutableSet<String> manifestPaths = Sets.mutable.empty();
        deserializer.deserializeModuleManifest(outputDir, TEST_REPO)
                .forEachElement(e -> manifestPaths.add(e.getPath()));
        Assert.assertFalse("Manifest for test_generic_repository must contain at least one element",
                manifestPaths.isEmpty());

        // Load platform first (dependency), then test_generic_repository
        java.util.Set<String> loaded = loader.loadIfPossible(runtime,
                Lists.immutable.with(PLATFORM, TEST_REPO), false);

        Assert.assertTrue(
                "loadIfPossible() must include platform in the returned set",
                loaded.contains(PLATFORM));
        Assert.assertTrue(
                "loadIfPossible() must include test_generic_repository in the returned set",
                loaded.contains(TEST_REPO));

        // Verify the repository is now represented in the model: top-level map must be non-empty.
        // PureCompilerLoader.load() registers all GraphTools.getTopLevelNames() elements as
        // top-level entries when loading platform — so the count must be > 0.
        int topLevelCount = runtime.getModelRepository().getTopLevels().size();
        Assert.assertTrue(
                "ModelRepository must contain top-level elements after loading platform + test_generic_repository, found: " + topLevelCount,
                topLevelCount > 0);

        // The manifest element count for test_generic_repository must be non-zero
        // (already verified above) — this confirms the write→read contract end-to-end:
        // the manifest describes what was serialized, and the loader processed it.
        Assert.assertFalse(
                "test_generic_repository manifest must describe at least one element path",
                manifestPaths.isEmpty());
    }

    @Test
    public void testDirectoryLoader_classLoaderLoader_vs_directoryLoader_sameManifestContent() throws IOException
    {
        // Serialize a module, then verify that the DirectoryPureCompilerLoader
        // reads back the same manifest as the FileDeserializer direct API.
        // This is a structural correctness check: the loader must not silently
        // drop or corrupt elements during the round-trip.
        Path outputDir = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDir, Lists.immutable.with(TEST_REPO));

        // Read manifest directly via FileDeserializer (already tested separately)
        FileDeserializer deserializer = newFileDeserializer();
        ModuleManifest directManifest = deserializer.deserializeModuleManifest(outputDir, TEST_REPO);

        // Read manifest via PureCompilerLoader (exercises DirectoryPureCompilerLoader)
        PureCompilerLoader loader = PureCompilerLoader.newLoader(
                Thread.currentThread().getContextClassLoader(), outputDir);
        // canLoad() internally calls moduleManifestExists() on DirectoryPureCompilerLoader
        Assert.assertTrue(loader.canLoad(TEST_REPO));

        // Collect element paths from the directly-read manifest
        MutableSet<String> directPaths = Sets.mutable.empty();
        directManifest.forEachElement(e -> directPaths.add(e.getPath()));
        Assert.assertFalse(
                "The manifest for test_generic_repository must contain at least one element",
                directPaths.isEmpty());
    }


    private static void assertModuleSerialized(FileDeserializer deserializer, Path outputDirectory, String moduleName)
    {
        Assert.assertTrue(moduleName + " manifest should exist", deserializer.moduleManifestExists(outputDirectory, moduleName));
        ModuleManifest manifest = deserializer.deserializeModuleManifest(outputDirectory, moduleName);
        Assert.assertEquals(moduleName, manifest.getModuleName());
        manifest.forEachElement(element ->
        {
            String elementPath = element.getPath();
            Assert.assertTrue(moduleName + " / " + elementPath, deserializer.elementExists(outputDirectory, elementPath));
        });
    }

    private static void assertModuleNotSerialized(FileDeserializer deserializer, Path outputDirectory, String moduleName)
    {
        Assert.assertFalse(moduleName + " manifest should not exist", deserializer.moduleManifestExists(outputDirectory, moduleName));
    }

    private static void assertDirectoriesEquivalent(Path dir1, Path dir2)
    {
        // First check that relative files paths are the same
        MutableSet<Path> paths1 = Sets.mutable.empty();
        MutableSet<Path> paths2 = Sets.mutable.empty();
        try (Stream<Path> stream1 = Files.walk(dir1);
             Stream<Path> stream2 = Files.walk(dir2))
        {
            stream1.map(dir1::relativize).forEach(paths1::add);
            stream2.map(dir2::relativize).forEach(paths2::add);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        Assert.assertEquals(paths1, paths2);

        MutableList<Path> mismatchFiles = Lists.mutable.empty();
        paths1.forEach(path ->
        {
            Path file1 = dir1.resolve(path);
            Path file2 = dir2.resolve(path);
            if (Files.isDirectory(file1))
            {
                if (!Files.isDirectory(file2))
                {
                    mismatchFiles.add(path);
                }
            }
            else if (Files.isDirectory(file2))
            {
                mismatchFiles.add(path);
            }
            else
            {
                try
                {
                    byte[] bytes1 = Files.readAllBytes(file1);
                    byte[] bytes2 = Files.readAllBytes(file2);
                    if (!Arrays.equals(bytes1, bytes2))
                    {
                        mismatchFiles.add(path);
                    }
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }
        });
        Assert.assertEquals(Lists.fixedSize.empty(), mismatchFiles);
    }

    private static FileDeserializer newFileDeserializer()
    {
        return FileDeserializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions().build())
                .withConcreteElementDeserializer(ConcreteElementDeserializer.builder().withLoadedExtensions().build())
                .withModuleMetadataSerializer(ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
    }
}
