// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SystemCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class TestBinaryModelRepositorySerializer extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra(), false);
    }

    @Test
    public void testPlatformSerialization() throws IOException
    {
        // Serialize platform to a byte array
        byte[] bytes;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
        {
            BinaryModelRepositorySerializer.serialize(stream, PlatformCodeRepository.NAME, runtime);
            bytes = stream.toByteArray();
        }

        // Build expected definition index
        MutableMap<String, String> expectedDefinitionIndex = Maps.mutable.empty();
        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String binaryPath = PureRepositoryJarTools.purePathToBinaryPath(source.getId());
            for (CoreInstance instance : source.getNewInstances())
            {
                expectedDefinitionIndex.put(PackageableElement.getUserPathForPackageableElement(instance), binaryPath);
            }
            for (CoreInstance importGroup : Imports.getImportGroupsForSource(source.getId(), processorSupport))
            {
                expectedDefinitionIndex.put(PackageableElement.getUserPathForPackageableElement(importGroup), binaryPath);
            }
        }
        String m3BinaryPath = PureRepositoryJarTools.purePathToBinaryPath("/platform/pure/grammar/m3.pure");
        for (String instancePath : Lists.mutable.with("meta", "meta::pure", "meta::pure::functions", "meta::pure::functions::lang", "meta::pure::metamodel", "meta::pure::metamodel::constraint", "meta::pure::metamodel::extension", "meta::pure::metamodel::function", "meta::pure::metamodel::function::property", "meta::pure::metamodel::import", "meta::pure::metamodel::multiplicity", "meta::pure::metamodel::relationship", "meta::pure::metamodel::treepath", "meta::pure::metamodel::type", "meta::pure::metamodel::type::generics", "meta::pure::metamodel::valuespecification", "meta::pure::router", "meta::pure::tools", "system", "system::imports", "meta::pure::test"))
        {
            expectedDefinitionIndex.put(instancePath, m3BinaryPath);
        }

        // Read the jar and verify the contents
        try (JarInputStream stream = new JarInputStream(new ByteArrayInputStream(bytes)))
        {
            PureManifest manifest = PureManifest.create(stream.getManifest());
            if (Version.PLATFORM == null)
            {
                Assert.assertFalse(manifest.hasPurePlatformVersion());
                Assert.assertNull(manifest.getPurePlatformVersion());
            }
            else
            {
                Assert.assertTrue(manifest.hasPurePlatformVersion());
                Assert.assertEquals(Version.PLATFORM, manifest.getPurePlatformVersion());
            }
            Assert.assertEquals(PlatformCodeRepository.NAME, manifest.getPureRepositoryName());
            Assert.assertFalse(manifest.hasPureModelVersion());
            Assert.assertNull(manifest.getPureModelVersion());

            JarEntry definitionIndexEntry = stream.getNextJarEntry();
            Assert.assertEquals(PureRepositoryJarTools.DEFINITION_INDEX_NAME, definitionIndexEntry.getName());
            JSONObject definitionIndex = (JSONObject) JSONValue.parse(new InputStreamReader(stream));
            stream.closeEntry();
            Verify.assertMapsEqual(expectedDefinitionIndex, definitionIndex);

            JarEntry referenceIndexEntry = stream.getNextJarEntry();
            Assert.assertEquals(PureRepositoryJarTools.REFERENCE_INDEX_NAME, referenceIndexEntry.getName());
            JSONObject referenceIndex = (JSONObject) JSONValue.parse(new InputStreamReader(stream));
            stream.closeEntry();
            for (Entry<?, ?> entry : ((Map<?, ?>) referenceIndex).entrySet())
            {
                Object key = entry.getKey();
                Verify.assertInstanceOf(String.class, key);
                Source source = runtime.getSourceById(PureRepositoryJarTools.binaryPathToPurePath((String) key));
                if (source == null)
                {
                    Assert.fail("Invalid source in reference index: " + key);
                }

                Object value = entry.getValue();
                Verify.assertInstanceOf(JSONArray.class, value);
                for (Object subvalue : (JSONArray) value)
                {
                    Verify.assertInstanceOf(String.class, subvalue);
                    CoreInstance instance = runtime.getCoreInstance((String) subvalue);
                    Assert.assertNotNull("Invalid reference '" + subvalue + "' for source: " + key, instance);
                    Assert.assertTrue("No definition found for '" + subvalue + "' for source: " + key, definitionIndex.containsKey(subvalue));
                }
            }

            MutableSet<String> foundPaths = Sets.mutable.empty();
            for (JarEntry entry = stream.getNextJarEntry(); entry != null; entry = stream.getNextJarEntry())
            {
                foundPaths.add(entry.getName());
            }

            MutableSet<String> expectedPaths = runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()).collect(Source::getId).collect(PureRepositoryJarTools::purePathToBinaryPath).toSet();
            Verify.assertSetsEqual(expectedPaths, foundPaths);
        }
    }

    @Test
    public void testPlatformSerializationStability_SameRuntime() throws IOException
    {
        for (int i = 0; i < 10; i++)
        {
            byte[] bytes1;
            byte[] bytes2;

            try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
            {
                BinaryModelRepositorySerializer.serialize(stream, PlatformCodeRepository.NAME, runtime);
                bytes1 = stream.toByteArray();

                stream.reset();
                BinaryModelRepositorySerializer.serialize(stream, PlatformCodeRepository.NAME, runtime);
                bytes2 = stream.toByteArray();
            }

            this.assertJarBytesEquivalent(bytes1, bytes2);
        }
    }

    @Test
    public void testPlatformSerializationStability_DifferentRuntimes() throws IOException
    {
        for (int i = 0; i < 10; i++)
        {
            PureRuntime runtime2 = new PureRuntimeBuilder(runtime.getCodeStorage())
                    .withRuntimeStatus(getPureRuntimeStatus()).build();
            getFunctionExecution().init(runtime2, new Message(""));
            runtime2.loadAndCompileCore();

            byte[] bytes1;
            byte[] bytes2;

            try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
            {
                BinaryModelRepositorySerializer.serialize(stream, PlatformCodeRepository.NAME, runtime);
                bytes1 = stream.toByteArray();

                stream.reset();
                BinaryModelRepositorySerializer.serialize(stream, PlatformCodeRepository.NAME, runtime2);
                bytes2 = stream.toByteArray();
            }

            this.assertJarBytesEquivalent(bytes1, bytes2);
        }
    }

    @Test
    public void testUnknownRepositorySerialization() throws IOException
    {
        // Not a real repository
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
        {
            BinaryModelRepositorySerializer.serialize(stream, "not a repository at all", runtime);
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Unknown repository: not a repository at all", e.getMessage());
        }

        // Real repository but not present
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
        {
            BinaryModelRepositorySerializer.serialize(stream, SystemCodeRepository.NAME, runtime);
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Unknown repository: " + SystemCodeRepository.NAME, e.getMessage());
        }
    }

    @Test
    public void testNonStandardRepositorySerialization() throws IOException
    {
        TestCodeRepository testRepo = new TestCodeRepository("test");
        RepositoryCodeStorage classPathCodeStorage = new ClassLoaderCodeStorage(getRepositoryByName(PlatformCodeRepository.NAME), testRepo);
        PureCodeStorage codeStorage = new PureCodeStorage(null, classPathCodeStorage);
        PureRuntime runtime2 = new PureRuntimeBuilder(codeStorage).withRuntimeStatus(getPureRuntimeStatus()).buildAndInitialize();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BinaryModelRepositorySerializer.serialize(stream, "test", runtime2);

        PureRepositoryJar jar = PureRepositoryJars.get(stream);
        MutableSet<String> serializedFiles = jar.getMetadata().getExternalReferenceIndex().keysView().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty());
        MutableSet<String> expectedFiles = classPathCodeStorage.getFileOrFiles("/test").toSet();
        Verify.assertSetsEqual(expectedFiles, serializedFiles);
    }

    protected void assertJarBytesEquivalent(byte[] jarBytes1, byte[] jarBytes2)
    {
        if (!Arrays.equals(jarBytes1, jarBytes2))
        {
            try (JarInputStream jarStream1 = new JarInputStream(new ByteArrayInputStream(jarBytes1));
                 JarInputStream jarStream2 = new JarInputStream(new ByteArrayInputStream(jarBytes2)))
            {
                JarEntry entry1 = jarStream1.getNextJarEntry();
                JarEntry entry2 = jarStream2.getNextJarEntry();
                while (entry1 != null && entry2 != null)
                {
                    String name = entry1.getName();
                    Assert.assertEquals("Entry name mismatch", name, entry2.getName());

                    byte[] entry1Bytes = this.readEntryToByteArray(jarStream1);
                    byte[] entry2Bytes = this.readEntryToByteArray(jarStream2);
                    Assert.assertArrayEquals("Byte mismatch for entry " + name, entry1Bytes, entry2Bytes);

                    entry1 = jarStream1.getNextJarEntry();
                    entry2 = jarStream2.getNextJarEntry();
                }
                if (entry1 != null)
                {
                    Assert.fail("Jar 1 contains an entry not present in jar 2: " + entry1.getName());
                }
                if (entry2 != null)
                {
                    Assert.fail("Jar 2 contains an entry not present in jar 1: " + entry2.getName());
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error comparing jar bytes", e);
            }
        }
    }

    private byte[] readEntryToByteArray(InputStream inStream) throws IOException
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        for (int read = inStream.read(buffer); read != -1; read = inStream.read(buffer))
        {
            outStream.write(buffer, 0, read);
        }
        return outStream.toByteArray();
    }
}
