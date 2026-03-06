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
import java.nio.file.Path;

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
        Path outputDirectory = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDirectory, null);

        FileDeserializer deserializer = newFileDeserializer();
        assertModuleSerialized(deserializer, outputDirectory, PLATFORM);
        assertModuleSerialized(deserializer, outputDirectory, TEST_REPO);
        assertModuleSerialized(deserializer, outputDirectory, OTHER_TEST_REPO);
    }

    @Test
    public void testSerializeAllModulesExplicitlyRequested() throws IOException
    {
        Path outputDirectory = TMP.newFolder().toPath();
        PureCompilerBinaryGenerator.serializeModules(outputDirectory, Lists.immutable.with(PLATFORM, TEST_REPO, OTHER_TEST_REPO));

        FileDeserializer deserializer = newFileDeserializer();
        assertModuleSerialized(deserializer, outputDirectory, PLATFORM);
        assertModuleSerialized(deserializer, outputDirectory, TEST_REPO);
        assertModuleSerialized(deserializer, outputDirectory, OTHER_TEST_REPO);
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

    private static FileDeserializer newFileDeserializer()
    {
        return FileDeserializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions().build())
                .withConcreteElementDeserializer(ConcreteElementDeserializer.builder().withLoadedExtensions().build())
                .withModuleMetadataSerializer(ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
    }
}
