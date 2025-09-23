// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

public class TestPureCompilerSerializer extends AbstractReferenceTest
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    private static FileDeserializer fileDeserializer;
    private static ModuleMetadataGenerator moduleMetadataGenerator;
    private static PureCompilerSerializer pureCompilerSerializer;

    @BeforeClass
    public static void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getExtra());
        FilePathProvider filePathProvider = FilePathProvider.builder().withLoadedExtensions().build();
        FileSerializer fileSerializer = FileSerializer.builder()
                .withFilePathProvider(filePathProvider)
                .withSerializers(ConcreteElementSerializer.builder(processorSupport).withLoadedExtensions().build(), ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
        fileDeserializer = fileSerializer.getDeserializer();

        moduleMetadataGenerator = ModuleMetadataGenerator.fromPureRuntime(runtime);
        pureCompilerSerializer = PureCompilerSerializer.builder()
                .withFileSerializer(fileSerializer)
                .withModuleMetadataGenerator(moduleMetadataGenerator)
                .withProcessorSupport(processorSupport)
                .build();
    }

    @Test
    public void testSerializeAllToDirectory() throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        pureCompilerSerializer.serializeAll(directory);
        getAllModuleInfos().forEachKeyValue((moduleName, moduleInfo) ->
        {
            assertModuleMetadataSerialized(directory, moduleName, moduleInfo.metadata);
            moduleInfo.elements.forEachKeyValue((path, element) ->
            {
                Assert.assertTrue(path, fileDeserializer.elementExists(directory, path));
                DeserializedConcreteElement deserialized = fileDeserializer.deserializeElement(directory, path);
                Assert.assertEquals(path, element.getSourceInformation(), deserialized.getConcreteElementData().getSourceInformation());
            });
        });
    }

    @Test
    public void testSerializeAllToJar() throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        Path jarPath = directory.resolve("test.jar");
        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarPath))))
        {
            pureCompilerSerializer.serializeAll(jarStream);
        }
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null))
        {
            getAllModuleInfos().forEachKeyValue((moduleName, moduleInfo) ->
            {
                assertModuleMetadataSerialized(classLoader, moduleName, moduleInfo.metadata);
                moduleInfo.elements.forEachKeyValue((path, element) ->
                {
                    Assert.assertTrue(path, fileDeserializer.elementExists(classLoader, path));
                    DeserializedConcreteElement deserialized = fileDeserializer.deserializeElement(classLoader, path);
                    Assert.assertEquals(path, element.getSourceInformation(), deserialized.getConcreteElementData().getSourceInformation());
                });
            });
        }
    }

    @Test
    public void testSerializePlatformToDirectory() throws IOException
    {
        testSerializeModuleToDirectory("platform");
    }

    @Test
    public void testSerializePlatformToJar() throws IOException
    {
        testSerializeModuleToJar("platform");
    }

    @Test
    public void testSerializeTestModuleToDirectory() throws IOException
    {
        testSerializeModuleToDirectory("ref_test");
    }

    @Test
    public void testSerializeTestModuleToJar() throws IOException
    {
        testSerializeModuleToJar("ref_test");
    }

    @Test
    public void testSerializeModulesToDirectory() throws IOException
    {
        testSerializeModulesToDirectory("test_generic_repository", "other_test_generic_repository", "ref_test");
    }

    @Test
    public void testSerializeModulesToJar() throws IOException
    {
        testSerializeModulesToJar("test_generic_repository", "other_test_generic_repository", "ref_test");
    }

    private void testSerializeModuleToDirectory(String moduleName) throws IOException
    {
        Path directory = TMP.newFolder().toPath();

        pureCompilerSerializer.serializeModule(directory, moduleName);

        ModuleInfo moduleInfo = getModuleInfo(moduleName);
        assertModuleMetadataSerialized(directory, moduleName, moduleInfo.metadata);
        moduleInfo.elements.forEachKeyValue((path, element) -> assertElementSerialized(directory, path, element));
    }

    private void testSerializeModuleToJar(String moduleName) throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        Path jarPath = directory.resolve(moduleName + ".jar");

        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarPath))))
        {
            pureCompilerSerializer.serializeModule(jarStream, moduleName);
        }

        ModuleInfo moduleInfo = getModuleInfo(moduleName);
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null))
        {
            assertModuleMetadataSerialized(classLoader, moduleName, moduleInfo.metadata);
            moduleInfo.elements.forEachKeyValue((path, element) -> assertElementSerialized(classLoader, path, element));
        }
    }

    private void testSerializeModulesToDirectory(String... moduleNames) throws IOException
    {
        Path directory = TMP.newFolder().toPath();

        pureCompilerSerializer.serializeModules(directory, moduleNames);

        MutableMap<String, ModuleInfo> moduleInfos = getModuleInfos(moduleNames);
        Sets.mutable.with(moduleNames).forEach(moduleName ->
        {
            ModuleInfo moduleInfo = moduleInfos.get(moduleName);
            assertModuleMetadataSerialized(directory, moduleName, moduleInfo.metadata);
            moduleInfo.elements.forEachKeyValue((path, element) -> assertElementSerialized(directory, path, element));
        });
    }

    private void testSerializeModulesToJar(String... moduleNames) throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        Path jarPath = directory.resolve("test.jar");

        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarPath))))
        {
            pureCompilerSerializer.serializeModules(jarStream, moduleNames);
        }

        MutableMap<String, ModuleInfo> moduleInfos = getModuleInfos(moduleNames);
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null))
        {
            Sets.mutable.with(moduleNames).forEach(moduleName ->
            {
                ModuleInfo moduleInfo = moduleInfos.get(moduleName);
                assertModuleMetadataSerialized(classLoader, moduleName, moduleInfo.metadata);
                moduleInfo.elements.forEachKeyValue((path, element) -> assertElementSerialized(classLoader, path, element));
            });
        }
    }

    private void assertElementSerialized(Path directory, String path, CoreInstance element)
    {
        Assert.assertTrue(path, fileDeserializer.elementExists(directory, path));
        DeserializedConcreteElement deserialized = fileDeserializer.deserializeElement(directory, path);
        Assert.assertEquals(path, element.getSourceInformation(), deserialized.getConcreteElementData().getSourceInformation());
    }

    private void assertElementSerialized(ClassLoader classLoader, String path, CoreInstance element)
    {
        Assert.assertTrue(path, fileDeserializer.elementExists(classLoader, path));
        DeserializedConcreteElement deserialized = fileDeserializer.deserializeElement(classLoader, path);
        Assert.assertEquals(path, element.getSourceInformation(), deserialized.getConcreteElementData().getSourceInformation());
    }

    private void assertModuleMetadataSerialized(Path directory, String moduleName, ModuleMetadata metadata)
    {
        Assert.assertTrue(moduleName, fileDeserializer.moduleManifestExists(directory, moduleName));
        Assert.assertEquals(moduleName, metadata.getManifest(), fileDeserializer.deserializeModuleManifest(directory, moduleName));
        Assert.assertTrue(moduleName, fileDeserializer.moduleSourceMetadataExists(directory, moduleName));
        Assert.assertEquals(moduleName, metadata.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(directory, moduleName));
        Assert.assertTrue(moduleName, fileDeserializer.moduleExternalReferenceMetadataExists(directory, moduleName));
        Assert.assertEquals(moduleName, metadata.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(directory, moduleName));
        metadata.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
        {
            String elementPath = ebr.getElementPath();
            String message = moduleName + " / " + elementPath;
            Assert.assertTrue(message, fileDeserializer.moduleElementBackReferenceMetadataExists(directory, moduleName, elementPath));
            Assert.assertEquals(message, ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(directory, moduleName, elementPath));
        });
        Assert.assertTrue(moduleName, fileDeserializer.moduleFunctionNameMetadataExists(directory, moduleName));
        Assert.assertEquals(moduleName, metadata.getFunctionNameMetadata(), fileDeserializer.deserializeModuleFunctionNameMetadata(directory, moduleName));
    }

    private void assertModuleMetadataSerialized(ClassLoader classLoader, String moduleName, ModuleMetadata metadata)
    {
        Assert.assertTrue(moduleName, fileDeserializer.moduleManifestExists(classLoader, moduleName));
        Assert.assertEquals(moduleName, metadata.getManifest(), fileDeserializer.deserializeModuleManifest(classLoader, moduleName));
        Assert.assertTrue(moduleName, fileDeserializer.moduleSourceMetadataExists(classLoader, moduleName));
        Assert.assertEquals(moduleName, metadata.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(classLoader, moduleName));
        Assert.assertTrue(moduleName, fileDeserializer.moduleExternalReferenceMetadataExists(classLoader, moduleName));
        Assert.assertEquals(moduleName, metadata.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(classLoader, moduleName));
        metadata.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
        {
            String elementPath = ebr.getElementPath();
            String message = moduleName + " / " + elementPath;
            Assert.assertTrue(message, fileDeserializer.moduleElementBackReferenceMetadataExists(classLoader, moduleName, elementPath));
            Assert.assertEquals(message, ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(classLoader, moduleName, elementPath));
        });
        Assert.assertTrue(moduleName, fileDeserializer.moduleFunctionNameMetadataExists(classLoader, moduleName));
        Assert.assertEquals(moduleName, metadata.getFunctionNameMetadata(), fileDeserializer.deserializeModuleFunctionNameMetadata(classLoader, moduleName));
    }

    private ModuleInfo getModuleInfo(String moduleName)
    {
        ModuleMetadata metadata = moduleMetadataGenerator.generateModuleMetadata(moduleName);
        MutableMap<String, CoreInstance> elements = Maps.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            if (ModuleHelper.isElementInModule(element, moduleName))
            {
                elements.put(PackageableElement.getUserPathForPackageableElement(element), element);
            }
        });
        return new ModuleInfo(metadata, elements);
    }

    private MutableMap<String, ModuleInfo> getModuleInfos(String... moduleNames)
    {
        MutableMap<String, ModuleInfo> moduleInfos = Maps.mutable.ofInitialCapacity(moduleNames.length);
        moduleMetadataGenerator.generateModuleMetadata(moduleNames).forEach(m -> moduleInfos.put(m.getName(), new ModuleInfo(m)));
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            ModuleInfo moduleInfo = moduleInfos.get(ModuleHelper.getElementModule(element));
            if (moduleInfo != null)
            {
                moduleInfo.elements.put(PackageableElement.getUserPathForPackageableElement(element), element);
            }
        });
        return moduleInfos;
    }

    private MutableMap<String, ModuleInfo> getAllModuleInfos()
    {
        MutableMap<String, ModuleInfo> moduleInfos = Maps.mutable.empty();
        moduleMetadataGenerator.generateAllModuleMetadata().forEach(m -> moduleInfos.put(m.getName(), new ModuleInfo(m)));
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            ModuleInfo moduleInfo = moduleInfos.get(ModuleHelper.getElementModule(element));
            if (moduleInfo != null)
            {
                moduleInfo.elements.put(PackageableElement.getUserPathForPackageableElement(element), element);
            }
        });
        return moduleInfos;
    }

    private static class ModuleInfo
    {
        private final ModuleMetadata metadata;
        private final MutableMap<String, CoreInstance> elements;

        private ModuleInfo(ModuleMetadata metadata, MutableMap<String, CoreInstance> elements)
        {
            this.metadata = metadata;
            this.elements = elements;
        }

        private ModuleInfo(ModuleMetadata metadata)
        {
            this(metadata, Maps.mutable.empty());
        }
    }
}
