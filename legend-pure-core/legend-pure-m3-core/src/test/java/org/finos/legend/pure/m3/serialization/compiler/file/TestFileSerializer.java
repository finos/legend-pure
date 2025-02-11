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

package org.finos.legend.pure.m3.serialization.compiler.file;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

public class TestFileSerializer extends AbstractPureTestWithCoreCompiled
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    private static FilePathProvider filePathProvider;
    private static ConcreteElementSerializer elementSerializer;
    private static ConcreteElementDeserializer elementDeserializer;
    private static FileSerializer fileSerializer;
    private static FileDeserializer fileDeserializer;

    @BeforeClass
    public static void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getExtra());
        filePathProvider = FilePathProvider.builder().withLoadedExtensions().build();
        elementSerializer = ConcreteElementSerializer.builder(processorSupport).withLoadedExtensions().build();
        elementDeserializer = ConcreteElementDeserializer.builder().withLoadedExtensions().build();
        fileSerializer = FileSerializer.builder()
                .withFilePathProvider(filePathProvider)
                .withSerializers(elementSerializer, ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
        fileDeserializer = FileDeserializer.builder()
                .withFilePathProvider(filePathProvider)
                .withSerializers(elementDeserializer, ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
    }

    @Test
    public void testAllElementsInDirectory() throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        MutableMap<String, DeserializedConcreteElement> expectedElements = Maps.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(processorSupport).select(e -> e.getSourceInformation() != null).forEach(element ->
        {
            String elementPath = PackageableElement.getUserPathForPackageableElement(element);
            expectedElements.put(elementPath, getExpectedDeserializedElement(element));
            fileSerializer.serializeElement(directory, element);
        });

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{directory.toUri().toURL()}, null))
        {
            expectedElements.forEachKeyValue((elementPath, expected) ->
                    Assert.assertEquals(elementPath, expected, fileDeserializer.deserializeElement(classLoader, elementPath)));
            expectedElements.forEachKey(elementPath -> Assert.assertTrue(elementPath, fileDeserializer.elementExists(classLoader, elementPath)));
        }

        expectedElements.forEachKeyValue((elementPath, expected) ->
                Assert.assertEquals(elementPath, expected, fileDeserializer.deserializeElement(directory, elementPath)));
        expectedElements.forEachKey(elementPath -> Assert.assertTrue(elementPath, fileDeserializer.elementExists(directory, elementPath)));

        String noSuchElementPath = "no::such::Element";
        ElementNotFoundException e = Assert.assertThrows(ElementNotFoundException.class, () -> fileDeserializer.deserializeElement(directory, noSuchElementPath));
        Assert.assertEquals(noSuchElementPath, e.getElementPath());
        Assert.assertEquals("Element '" + noSuchElementPath + "' not found: cannot find file " + filePathProvider.getElementFilePath(directory, noSuchElementPath), e.getMessage());
    }

    @Test
    public void testAllElementsInJar() throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        Path jarPath = directory.resolve("elements_test.jar");
        MutableMap<String, DeserializedConcreteElement> expectedElements = Maps.mutable.empty();
        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarPath))))
        {
            GraphTools.getTopLevelAndPackagedElements(processorSupport).select(e -> e.getSourceInformation() != null).forEach(element ->
            {
                String elementPath = PackageableElement.getUserPathForPackageableElement(element);
                expectedElements.put(elementPath, getExpectedDeserializedElement(element));
                fileSerializer.serializeElement(jarStream, element);
            });
        }
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null))
        {
            expectedElements.forEachKeyValue((elementPath, expected) ->
                    Assert.assertEquals(elementPath, expected, fileDeserializer.deserializeElement(classLoader, elementPath)));

            String noSuchElementPath = "no::such::Element";
            ElementNotFoundException e = Assert.assertThrows(ElementNotFoundException.class, () -> fileDeserializer.deserializeElement(classLoader, noSuchElementPath));
            Assert.assertEquals(noSuchElementPath, e.getElementPath());
            Assert.assertEquals("Element '" + noSuchElementPath + "' not found: cannot find resource " + filePathProvider.getElementResourceName(noSuchElementPath), e.getMessage());
        }
    }

    private DeserializedConcreteElement getExpectedDeserializedElement(CoreInstance element)
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        elementSerializer.serialize(BinaryWriters.newBinaryWriter(byteStream), element);
        return elementDeserializer.deserialize(BinaryReaders.newBinaryReader(byteStream.toByteArray()));
    }

    @Test
    public void testAllModulesInDirectory() throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        MutableList<ModuleMetadata> allModuleMetadata = ModuleMetadataGenerator.fromPureRuntime(runtime).generateAllModuleMetadata();
        allModuleMetadata.forEach(m ->
        {
            fileSerializer.serializeModuleManifest(directory, m.getManifest());
            fileSerializer.serializeModuleSourceMetadata(directory, m.getSourceMetadata());
            fileSerializer.serializeModuleExternalReferenceMetadata(directory, m.getExternalReferenceMetadata());
            fileSerializer.serializeModuleBackReferenceMetadata(directory, m.getBackReferenceMetadata());
        });

        allModuleMetadata.forEach(m ->
        {
            Assert.assertTrue(m.getName(), fileDeserializer.moduleManifestExists(directory, m.getName()));
            Assert.assertTrue(m.getName(), fileDeserializer.moduleSourceMetadataExists(directory, m.getName()));
            Assert.assertTrue(m.getName(), fileDeserializer.moduleExternalReferenceMetadataExists(directory, m.getName()));
            m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                    Assert.assertTrue(m.getName() + " / " + ebr.getElementPath(), fileDeserializer.moduleElementBackReferenceMetadataExists(directory, m.getName(), ebr.getElementPath())));
        });
        allModuleMetadata.forEach(m ->
        {
            Assert.assertEquals(m.getName(), m.getManifest(), fileDeserializer.deserializeModuleManifest(directory, m.getName()));
            Assert.assertEquals(m.getName(), m.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(directory, m.getName()));
            Assert.assertEquals(m.getName(), m.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(directory, m.getName()));
            m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                    Assert.assertEquals(m.getName() + " / " + ebr.getElementPath(), ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(directory, m.getName(), ebr.getElementPath())));
        });
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{directory.toUri().toURL()}, null))
        {
            allModuleMetadata.forEach(m ->
            {
                Assert.assertTrue(m.getName(), fileDeserializer.moduleManifestExists(classLoader, m.getName()));
                Assert.assertTrue(m.getName(), fileDeserializer.moduleSourceMetadataExists(classLoader, m.getName()));
                Assert.assertTrue(m.getName(), fileDeserializer.moduleExternalReferenceMetadataExists(classLoader, m.getName()));
                m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                        Assert.assertTrue(m.getName() + " / " + ebr.getElementPath(), fileDeserializer.moduleElementBackReferenceMetadataExists(classLoader, m.getName(), ebr.getElementPath())));
            });
            allModuleMetadata.forEach(m ->
            {
                Assert.assertEquals(m.getName(), m.getManifest(), fileDeserializer.deserializeModuleManifest(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(classLoader, m.getName()));
                m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                        Assert.assertEquals(m.getName() + " / " + ebr.getElementPath(), ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(classLoader, m.getName(), ebr.getElementPath())));
            });
        }

        String noSuchModule = "no_such_module";
        ModuleMetadataNotFoundException e1 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleManifest(directory, noSuchModule));
        Assert.assertEquals(noSuchModule, e1.getModuleName());
        Assert.assertEquals("Module '" + noSuchModule + "' manifest not found: cannot find file " + filePathProvider.getModuleManifestFilePath(directory, noSuchModule), e1.getMessage());
        ModuleMetadataNotFoundException e2 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleSourceMetadata(directory, noSuchModule));
        Assert.assertEquals(noSuchModule, e2.getModuleName());
        Assert.assertEquals("Module '" + noSuchModule + "' source metadata not found: cannot find file " + filePathProvider.getModuleSourceMetadataFilePath(directory, noSuchModule), e2.getMessage());
        ModuleMetadataNotFoundException e3 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleExternalReferenceMetadata(directory, noSuchModule));
        Assert.assertEquals(noSuchModule, e3.getModuleName());
        Assert.assertEquals("Module '" + noSuchModule + "' external reference metadata not found: cannot find file " + filePathProvider.getModuleExternalReferenceMetadataFilePath(directory, noSuchModule), e3.getMessage());
    }

    @Test
    public void testAllModulesInJar() throws IOException
    {
        Path directory = TMP.newFolder().toPath();
        Path jarPath = directory.resolve("modules_test.jar");
        MutableList<ModuleMetadata> allModuleMetadata = ModuleMetadataGenerator.fromPureRuntime(runtime).generateAllModuleMetadata();
        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarPath))))
        {
            allModuleMetadata.forEach(m ->
            {
                fileSerializer.serializeModuleManifest(jarStream, m.getManifest());
                fileSerializer.serializeModuleSourceMetadata(jarStream, m.getSourceMetadata());
                fileSerializer.serializeModuleExternalReferenceMetadata(jarStream, m.getExternalReferenceMetadata());
                fileSerializer.serializeModuleBackReferenceMetadata(jarStream, m.getBackReferenceMetadata());
            });
        }

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null))
        {
            allModuleMetadata.forEach(m ->
            {
                Assert.assertTrue(m.getName(), fileDeserializer.moduleManifestExists(classLoader, m.getName()));
                Assert.assertTrue(m.getName(), fileDeserializer.moduleSourceMetadataExists(classLoader, m.getName()));
                Assert.assertTrue(m.getName(), fileDeserializer.moduleExternalReferenceMetadataExists(classLoader, m.getName()));
                m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                        Assert.assertTrue(m.getName() + " / " + ebr.getElementPath(), fileDeserializer.moduleElementBackReferenceMetadataExists(classLoader, m.getName(), ebr.getElementPath())));
            });
            allModuleMetadata.forEach(m ->
            {
                Assert.assertEquals(m.getName(), m.getManifest(), fileDeserializer.deserializeModuleManifest(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(classLoader, m.getName()));
                m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                        Assert.assertEquals(m.getName() + " / " + ebr.getElementPath(), ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(classLoader, m.getName(), ebr.getElementPath())));
            });

            String noSuchModule = "no_such_module";
            ModuleMetadataNotFoundException e1 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleManifest(classLoader, noSuchModule));
            Assert.assertEquals(noSuchModule, e1.getModuleName());
            Assert.assertEquals("Module '" + noSuchModule + "' manifest not found: cannot find resource " + filePathProvider.getModuleManifestResourceName(noSuchModule), e1.getMessage());
            ModuleMetadataNotFoundException e2 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleSourceMetadata(classLoader, noSuchModule));
            Assert.assertEquals(noSuchModule, e2.getModuleName());
            Assert.assertEquals("Module '" + noSuchModule + "' source metadata not found: cannot find resource " + filePathProvider.getModuleSourceMetadataResourceName(noSuchModule), e2.getMessage());
            ModuleMetadataNotFoundException e3 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleExternalReferenceMetadata(classLoader, noSuchModule));
            Assert.assertEquals(noSuchModule, e3.getModuleName());
            Assert.assertEquals("Module '" + noSuchModule + "' external reference metadata not found: cannot find resource " + filePathProvider.getModuleExternalReferenceMetadataResourceName(noSuchModule), e3.getMessage());
        }
    }
}
