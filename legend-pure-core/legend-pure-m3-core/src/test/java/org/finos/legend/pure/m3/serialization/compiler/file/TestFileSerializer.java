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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

    /**
     * Serialising the same element a second time into the same directory must
     * leave the target file completely untouched: same bytes AND same
     * last-modified timestamp.  This is the "skip-if-identical" behaviour
     * introduced in {@code FileSerializer.writeAtomically}.
     *
     * <p>Note: the check is inherently non-atomic — another thread could replace
     * the file between the content comparison and the timestamp read — but for
     * a single-threaded test this is sufficient to verify the fast-path.</p>
     */
    @Test
    public void testWriteIfModified_identicalContent_preservesTimestamp() throws IOException, InterruptedException
    {
        Path directory = TMP.newFolder().toPath();

        // Pick any element that has source information so it will be serialized
        CoreInstance element = GraphTools.getTopLevelAndPackagedElements(processorSupport)
                .select(e -> e.getSourceInformation() != null)
                .getFirst();
        Assert.assertNotNull("No serializable element found", element);

        String elementPath = PackageableElement.getUserPathForPackageableElement(element);
        Path filePath = filePathProvider.getElementFilePath(directory, elementPath);

        // First write – creates the file
        fileSerializer.serializeElement(directory, element);
        Assert.assertTrue("File should exist after first write", Files.exists(filePath));

        FileTime timestampAfterFirstWrite = Files.getLastModifiedTime(filePath);

        // Ensure the filesystem clock can advance before the second write.
        // Most filesystems have at least 1 ms resolution; 50 ms is a safe margin.
        Thread.sleep(50);

        // Second write – same content, file must not be touched
        fileSerializer.serializeElement(directory, element);

        FileTime timestampAfterSecondWrite = Files.getLastModifiedTime(filePath);

        Assert.assertEquals(
                "Re-serialising identical content must not change the file's last-modified timestamp for " + elementPath,
                timestampAfterFirstWrite,
                timestampAfterSecondWrite);
    }

    /**
     * When the on-disk file has different content from the newly serialised bytes
     * the file must be atomically replaced.  After replacement the file's content
     * must be correct and its timestamp must be newer than the original.
     */
    @Test
    public void testWriteIfModified_differentContent_replacesFile() throws IOException, InterruptedException
    {
        Path directory = TMP.newFolder().toPath();

        CoreInstance element = GraphTools.getTopLevelAndPackagedElements(processorSupport)
                .select(e -> e.getSourceInformation() != null)
                .getFirst();
        Assert.assertNotNull("No serializable element found", element);

        String elementPath = PackageableElement.getUserPathForPackageableElement(element);
        Path filePath = filePathProvider.getElementFilePath(directory, elementPath);

        // Write a known-wrong file to the target path directly
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, new byte[]{0x00, 0x01, 0x02, 0x03});

        FileTime timestampOfWrongFile = Files.getLastModifiedTime(filePath);

        // Ensure the clock can advance so a new write will have a strictly later timestamp
        Thread.sleep(50);

        // Serialise the real content – must replace the stub file
        fileSerializer.serializeElement(directory, element);

        Assert.assertTrue("File should still exist after replacement", Files.exists(filePath));

        FileTime timestampAfterReplacement = Files.getLastModifiedTime(filePath);
        Assert.assertTrue(
                "Replacing a file with different content must update the last-modified timestamp for " + elementPath,
                timestampAfterReplacement.compareTo(timestampOfWrongFile) > 0);

        // Content must now be the correctly serialised element
        Assert.assertEquals(
                elementPath,
                getExpectedDeserializedElement(element),
                fileDeserializer.deserializeElement(directory, elementPath));
    }

    private DeserializedConcreteElement getExpectedDeserializedElement(CoreInstance element)
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        elementSerializer.serialize(byteStream, element);
        return elementDeserializer.deserialize(new ByteArrayInputStream(byteStream.toByteArray()));
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
            fileSerializer.serializeModuleFunctionNameMetadata(directory, m.getFunctionNameMetadata());
        });

        allModuleMetadata.forEach(m ->
        {
            Assert.assertTrue(m.getName(), fileDeserializer.moduleManifestExists(directory, m.getName()));
            Assert.assertTrue(m.getName(), fileDeserializer.moduleSourceMetadataExists(directory, m.getName()));
            Assert.assertTrue(m.getName(), fileDeserializer.moduleExternalReferenceMetadataExists(directory, m.getName()));
            m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                    Assert.assertTrue(m.getName() + " / " + ebr.getElementPath(), fileDeserializer.moduleElementBackReferenceMetadataExists(directory, m.getName(), ebr.getElementPath())));
            Assert.assertTrue(m.getName(), fileDeserializer.moduleFunctionNameMetadataExists(directory, m.getName()));
        });
        allModuleMetadata.forEach(m ->
        {
            Assert.assertEquals(m.getName(), m.getManifest(), fileDeserializer.deserializeModuleManifest(directory, m.getName()));
            Assert.assertEquals(m.getName(), m.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(directory, m.getName()));
            Assert.assertEquals(m.getName(), m.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(directory, m.getName()));
            m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                    Assert.assertEquals(m.getName() + " / " + ebr.getElementPath(), ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(directory, m.getName(), ebr.getElementPath())));
            Assert.assertEquals(m.getName(), m.getFunctionNameMetadata(), fileDeserializer.deserializeModuleFunctionNameMetadata(directory, m.getName()));
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
                Assert.assertTrue(m.getName(), fileDeserializer.moduleFunctionNameMetadataExists(classLoader, m.getName()));
            });
            allModuleMetadata.forEach(m ->
            {
                Assert.assertEquals(m.getName(), m.getManifest(), fileDeserializer.deserializeModuleManifest(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(classLoader, m.getName()));
                m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                        Assert.assertEquals(m.getName() + " / " + ebr.getElementPath(), ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(classLoader, m.getName(), ebr.getElementPath())));
                Assert.assertEquals(m.getName(), m.getFunctionNameMetadata(), fileDeserializer.deserializeModuleFunctionNameMetadata(classLoader, m.getName()));
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
        ModuleMetadataNotFoundException e4 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleFunctionNameMetadata(directory, noSuchModule));
        Assert.assertEquals(noSuchModule, e4.getModuleName());
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
                fileSerializer.serializeModuleFunctionNameMetadata(jarStream, m.getFunctionNameMetadata());
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
                Assert.assertTrue(m.getName(), fileDeserializer.moduleFunctionNameMetadataExists(classLoader, m.getName()));
            });
            allModuleMetadata.forEach(m ->
            {
                Assert.assertEquals(m.getName(), m.getManifest(), fileDeserializer.deserializeModuleManifest(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getSourceMetadata(), fileDeserializer.deserializeModuleSourceMetadata(classLoader, m.getName()));
                Assert.assertEquals(m.getName(), m.getExternalReferenceMetadata(), fileDeserializer.deserializeModuleExternalReferenceMetadata(classLoader, m.getName()));
                m.getBackReferenceMetadata().getBackReferences().forEach(ebr ->
                        Assert.assertEquals(m.getName() + " / " + ebr.getElementPath(), ebr, fileDeserializer.deserializeModuleElementBackReferenceMetadata(classLoader, m.getName(), ebr.getElementPath())));
                Assert.assertEquals(m.getName(), m.getFunctionNameMetadata(), fileDeserializer.deserializeModuleFunctionNameMetadata(classLoader, m.getName()));
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
            ModuleMetadataNotFoundException e4 = Assert.assertThrows(ModuleMetadataNotFoundException.class, () -> fileDeserializer.deserializeModuleFunctionNameMetadata(classLoader, noSuchModule));
            Assert.assertEquals(noSuchModule, e4.getModuleName());
        }
    }
}
