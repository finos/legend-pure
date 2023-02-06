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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class AbstractPureRepositoryJarLibraryTest extends AbstractPureTestWithCoreCompiledPlatform
{
    protected PureRepositoryJarLibrary library;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @Before
    public void setUpLibrary() throws IOException
    {
        this.library = buildLibrary(runtime, PlatformCodeRepository.NAME);
    }

    @Test
    public void testGetPlatformVersion()
    {
        Assert.assertEquals(Version.PLATFORM, this.library.getPlatformVersion());
    }

    @Test
    public void testIsKnownRepository()
    {
        Assert.assertTrue(this.library.isKnownRepository("platform"));
        Assert.assertFalse(this.library.isKnownRepository("not a repo"));
    }

    @Test
    public void testIsKnownFile()
    {
        Assert.assertTrue(this.library.isKnownFile("platform/pure/grammar/m3.pc"));
        Assert.assertTrue(this.library.isKnownFile("platform/pure/grammar/functions/lang/all.pc"));

        Assert.assertFalse(this.library.isKnownFile("not a file at all"));
        Assert.assertFalse(this.library.isKnownFile("datamart_datamt/something/somethingelse.pure"));
    }

    @Test
    public void testIsKnownInstance()
    {
        Assert.assertTrue(this.library.isKnownInstance("meta::pure::metamodel::type::Class"));
        Assert.assertTrue(this.library.isKnownInstance("meta::pure::metamodel::type::Type"));
        Assert.assertTrue(this.library.isKnownInstance("meta::pure::metamodel::multiplicity::PureOne"));

        Assert.assertFalse(this.library.isKnownInstance("not an instance"));
        Assert.assertFalse(this.library.isKnownInstance("meta::pure::metamodel::multiplicity::PureOneThousand"));
    }

    @Test
    public void testReadFile()
    {
        ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();
        Writer writer = BinaryWriters.newBinaryWriter(expectedStream);
        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);

            expectedStream.reset();
            BinaryModelSourceSerializer.serialize(writer, source, runtime);

            Assert.assertArrayEquals("Byte mismatch for " + purePath, expectedStream.toByteArray(), this.library.readFile(binPath));
        }
    }

    @Test
    public void testReadFiles()
    {
        testReadFiles("platform/pure/grammar/m3.pc", "platform/pure/grammar/functions/lang/all.pc", "platform/pure/grammar/functions/collection/filter.pc");
    }

    protected void testReadFiles(String... files)
    {
        testReadFiles(runtime, this.library, files);
    }

    @Test
    public void testReadRepository()
    {
        testReadRepositories(runtime, this.library, PlatformCodeRepository.NAME);
    }

    @Test
    public void testReadAll()
    {
        testReadAll(runtime, this.library);
    }

    @Test
    public void testGetAllFiles()
    {
        MutableSet<String> expected = runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()).collect(s -> PureRepositoryJarTools.purePathToBinaryPath(s.getId()), Sets.mutable.empty());
        Verify.assertSetsEqual(expected, this.library.getAllFiles().toSet());
    }

    @Test
    public void testGetDirectoryFiles()
    {
        MutableSet<String> allFiles = this.library.getAllFiles().toSet();
        Verify.assertSetsEqual(allFiles, this.library.getDirectoryFiles("/").toSet());
        Verify.assertSetsEqual(allFiles, this.library.getDirectoryFiles("").toSet());

        MutableSet<String> platformFiles = this.library.getRepositoryFiles("platform").toSet();
        Verify.assertNotEmpty(platformFiles);
        Verify.assertSetsEqual(platformFiles, this.library.getDirectoryFiles("platform").toSet());
        Verify.assertSetsEqual(platformFiles, this.library.getDirectoryFiles("/platform").toSet());
        Verify.assertSetsEqual(platformFiles, this.library.getDirectoryFiles("platform/").toSet());
        Verify.assertSetsEqual(platformFiles, this.library.getDirectoryFiles("/platform/").toSet());

//        MutableSet<String> coreFunctionsFiles = runtime.getSourceRegistry().getSources().collect(Source.SOURCE_ID).selectWith(StringPredicates2.startsWith(), "/platform/pure/corefunctions").collect(PureRepositoryJarTools.PURE_PATH_TO_BINARY_PATH).toSet();
//        Verify.assertNotEmpty(coreFunctionsFiles);
//        Verify.assertSetsEqual(coreFunctionsFiles, this.library.getDirectoryFiles("platform/pure/corefunctions").toSet());
//        Verify.assertSetsEqual(coreFunctionsFiles, this.library.getDirectoryFiles("/platform/pure/corefunctions").toSet());
//        Verify.assertSetsEqual(coreFunctionsFiles, this.library.getDirectoryFiles("platform/pure/corefunctions/").toSet());
//        Verify.assertSetsEqual(coreFunctionsFiles, this.library.getDirectoryFiles("/platform/pure/corefunctions/").toSet());

        Verify.assertEmpty(this.library.getDirectoryFiles("/notADir"));
        Verify.assertEmpty(this.library.getDirectoryFiles("/platform/notADir"));
    }

    @Test
    public void testGetRequiredFiles_SingleInstance()
    {
        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);
            MutableSet<String> expected = this.library.getFileDependencies(binPath).toSet();
            for (CoreInstance instance : source.getNewInstances())
            {
                String instancePath = PackageableElement.getUserPathForPackageableElement(instance);
                SetIterable<String> actual = this.library.getRequiredFiles(instancePath);
                Verify.assertSetsEqual(instancePath, expected, actual.toSet());
            }
        }
    }

    @Test
    public void testGetRequiredFiles_MultipleInstances()
    {
        ListIterable<String> instancePaths = Lists.immutable.with("meta::pure::metamodel::type::Class", "meta::pure::functions::collection::map_T_$0_1$__Function_1__V_$0_1$_", "meta::pure::profiles::equality");

        MutableSet<String> instanceFiles = Sets.mutable.empty();
        for (String instancePath : instancePaths)
        {
            CoreInstance instance = runtime.getCoreInstance(instancePath);
            Assert.assertNotNull("Could not find " + instancePath, instance);
            instanceFiles.add(PureRepositoryJarTools.purePathToBinaryPath(instance.getSourceInformation().getSourceId()));
        }
        SetIterable<String> expected = this.library.getFileDependencies(instanceFiles);
        SetIterable<String> actual = this.library.getRequiredFiles(instancePaths);
        Verify.assertSetsEqual(expected.toSet(), actual.toSet());
    }

    @Test
    public void testFileDependencies_SingleFile()
    {
        String m3BinPath = "platform/pure/grammar/m3.pc";
        SetIterable<String> m3Dependencies = this.library.getFileDependencies(m3BinPath);
        Verify.assertSetsEqual(Sets.mutable.with(m3BinPath), m3Dependencies.toSet());

        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);
            SetIterable<String> dependencies = this.library.getFileDependencies(binPath);
            Verify.assertContainsAll(dependencies, binPath, m3BinPath);
        }
    }

    @Test
    public void testFileDependencies_MultipleFiles()
    {
        String m3BinPath = "platform/pure/grammar/m3.pc";
        String collectionBinPath = "platform/pure/grammar/functions/lang/all.pc";

        MutableSet<String> m3Dependencies = this.library.getFileDependencies(m3BinPath).toSet();
        Verify.assertSetsEqual(Sets.mutable.with(m3BinPath), m3Dependencies);

        Verify.assertSetsEqual(m3Dependencies, this.library.getFileDependencies(m3Dependencies).toSet());

        MutableSet<String> collectionDependencies = this.library.getFileDependencies(collectionBinPath).toSet();
        Verify.assertContains(m3BinPath, collectionDependencies);
        Verify.assertContains(collectionBinPath, collectionDependencies);

        Verify.assertSetsEqual(collectionDependencies, this.library.getFileDependencies(m3BinPath, collectionBinPath).toSet());
    }

    @Test
    public void testDependentFiles_SingleFile()
    {
        String m3BinPath = "platform/pure/grammar/m3.pc";
        Verify.assertSetsEqual(this.library.getAllFiles().toSet(), this.library.getDependentFiles(m3BinPath).toSet());

        MutableSetMultimap<String, String> allExpectedDependents = Multimaps.mutable.set.empty();
        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);
            for (String dependency : this.library.getFileDependencies(binPath))
            {
                allExpectedDependents.put(dependency, binPath);
            }
        }

        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);
            MutableSet<String> dependents = this.library.getDependentFiles(binPath).toSet();
            Verify.assertSetsEqual(allExpectedDependents.get(binPath), dependents);
            Verify.assertContains(binPath, dependents);
            if (!m3BinPath.equals(binPath))
            {
                Verify.assertNotContains(m3BinPath, dependents);
            }
        }
    }

    @Test
    public void testDependentFiles_MultipleFiles()
    {
        String m3BinPath = "platform/pure/grammar/m3.pc";
        String collectionBinPath = "platform/pure/grammar/functions/lang/all.pc";
        String dateBinPath = "platform/pure/grammar/functions/string/plus.pc";

        MutableSetMultimap<String, String> allExpectedDependents = Multimaps.mutable.set.empty();
        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);
            for (String dependency : this.library.getFileDependencies(binPath))
            {
                allExpectedDependents.put(dependency, binPath);
            }
        }

        MutableSet<String> expected = allExpectedDependents.get(collectionBinPath).union(allExpectedDependents.get(dateBinPath));
        MutableSet<String> dependents = this.library.getDependentFiles(collectionBinPath, dateBinPath).toSet();
        Verify.assertSetsEqual(expected, dependents);

        MutableSet<String> collectionDependents = this.library.getDependentFiles(collectionBinPath).toSet();
        MutableSet<String> dateDependents = this.library.getDependentFiles(dateBinPath).toSet();
        Verify.assertSetsEqual(dependents, collectionDependents.union(dateDependents));
    }

    protected PureRepositoryJarLibrary buildLibrary(PureRuntime runtime, String... repositoryNames) throws IOException
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        MutableList<PureRepositoryJar> jars = Lists.mutable.empty();
        for (String repositoryName : repositoryNames)
        {
            stream.reset();
            BinaryModelRepositorySerializer.serialize(stream, repositoryName, runtime);
            jars.add(PureRepositoryJars.get(stream));
        }
        return newLibrary(jars);
    }

    protected abstract PureRepositoryJarLibrary newLibrary(RichIterable<PureRepositoryJar> jars);

    protected static void testReadFiles(PureRuntime runtime, PureRepositoryJarLibrary library, String... files)
    {
        MapIterable<String, byte[]> bytesByFile = library.readFiles(files);

        MutableMap<String, byte[]> expected = Maps.mutable.ofInitialCapacity(files.length);
        ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();
        Writer writer = BinaryWriters.newBinaryWriter(expectedStream);
        for (String binPath : files)
        {
            String purePath = PureRepositoryJarTools.binaryPathToPurePath(binPath);
            Source source = runtime.getSourceById(purePath);
            Assert.assertNotNull("Could not find source: " + purePath, source);

            expectedStream.reset();
            BinaryModelSourceSerializer.serialize(writer, source, runtime);
            expected.put(binPath, expectedStream.toByteArray());
        }

        assertFileByteMapsEqual(expected, bytesByFile);
    }

    protected static void testReadRepositories(PureRuntime runtime, PureRepositoryJarLibrary library, String... repositories)
    {
        SetIterable<String> repositorySet = Sets.immutable.with(repositories);

        MapIterable<String, byte[]> bytesByFile = library.readRepositoryFiles(repositorySet);

        MutableMap<String, byte[]> expected = Maps.mutable.empty();
        ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();
        Writer writer = BinaryWriters.newBinaryWriter(expectedStream);
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            String purePath = source.getId();
            String repo = getRepo(purePath);
            if ((repo != null) && repositorySet.contains(repo))
            {
                String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);

                expectedStream.reset();
                BinaryModelSourceSerializer.serialize(writer, source, runtime);
                expected.put(binPath, expectedStream.toByteArray());
            }
        }

        assertFileByteMapsEqual(expected, bytesByFile);
    }

    protected static void testReadAll(PureRuntime runtime, PureRepositoryJarLibrary library)
    {
        MapIterable<String, byte[]> bytesByFile = library.readAllFiles();

        MutableMap<String, byte[]> expected = Maps.mutable.empty();
        ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();
        Writer writer = BinaryWriters.newBinaryWriter(expectedStream);
        for (Source source : runtime.getSourceRegistry().getSources().select(s -> !s.isInMemory()))
        {
            String purePath = source.getId();
            String repo = getRepo(purePath);
            if (repo != null)
            {
                String binPath = PureRepositoryJarTools.purePathToBinaryPath(purePath);
                expectedStream.reset();
                BinaryModelSourceSerializer.serialize(writer, source, runtime);
                expected.put(binPath, expectedStream.toByteArray());
            }
        }
        assertFileByteMapsEqual(expected, bytesByFile);
    }

    protected static void assertFileByteMapsEqual(MapIterable<String, byte[]> expected, MapIterable<String, byte[]> actual)
    {
        Verify.assertSetsEqual("file paths", expected.keysView().toSet(), actual.keysView().toSet());
        for (Pair<String, byte[]> expectedPair : expected.keyValuesView())
        {
            String filePath = expectedPair.getOne();
            byte[] expectedBytes = expectedPair.getTwo();
            byte[] actualBytes = actual.get(filePath);
            Assert.assertArrayEquals("bytes differ for " + filePath, expectedBytes, actualBytes);
        }
    }

    private static String getRepo(String purePath)
    {
        int slashIndex = purePath.indexOf('/', 1);
        return (slashIndex == -1) ? null : purePath.substring(1, slashIndex);
    }
}
