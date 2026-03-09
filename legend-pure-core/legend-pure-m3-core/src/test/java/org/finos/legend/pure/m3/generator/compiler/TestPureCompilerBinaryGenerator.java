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
