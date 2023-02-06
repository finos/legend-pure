package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class TestDistributedMetadataSpecification
{
    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    @Test
    public void testWithNoDependencies()
    {
        DistributedMetadataSpecification metadata = DistributedMetadataSpecification.newSpecification("abc");
        Assert.assertEquals("abc", metadata.getName());
        Assert.assertEquals(Collections.emptySet(), metadata.getDependencies());
        Assert.assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().add("something"));
    }

    @Test
    public void testWithDependenciesAsVarArgs()
    {
        DistributedMetadataSpecification metadata = DistributedMetadataSpecification.newSpecification("def", "ghi", "jkl");
        Assert.assertEquals("def", metadata.getName());
        Assert.assertEquals(Sets.mutable.with("ghi", "jkl"), metadata.getDependencies());
        Assert.assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().add("something"));
    }

    @Test
    public void testWithDependenciesAsIterable()
    {
        DistributedMetadataSpecification metadata = DistributedMetadataSpecification.newSpecification("mno", Sets.mutable.with("pqr", "stu"));
        Assert.assertEquals("mno", metadata.getName());
        Assert.assertEquals(Sets.mutable.with("pqr", "stu"), metadata.getDependencies());
        Assert.assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().add("something"));
    }

    @Test
    public void testInvalidMetadataName()
    {
        String[] invalidNames = {"", "$$%", "invalid name"};
        for (String name : invalidNames)
        {
            IllegalArgumentException e1 = Assert.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(name));
            Assert.assertEquals(name, "Invalid metadata name: \"" + name + "\"", e1.getMessage());

            IllegalArgumentException e2 = Assert.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(name, "some_dependency"));
            Assert.assertEquals(name, "Invalid metadata name: \"" + name + "\"", e2.getMessage());
        }

        IllegalArgumentException e1 = Assert.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(null));
        Assert.assertEquals("Invalid metadata name: null", e1.getMessage());

        IllegalArgumentException e2 = Assert.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(null, "some_other_dependency"));
        Assert.assertEquals("Invalid metadata name: null", e2.getMessage());
    }

    @Test
    public void testInvalidDependencies()
    {
        IllegalArgumentException e1 = Assert.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification("a", "b", "c", null, "d"));
        Assert.assertEquals("Invalid dependency: null", e1.getMessage());

        IllegalArgumentException e2 = Assert.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification("a", "b", "999abc#", null, "$", "d"));
        Assert.assertEquals("Invalid dependencies: null, \"$\", \"999abc#\"", e2.getMessage());
    }

    @Test
    public void testReadWrite() throws IOException
    {
        Set<DistributedMetadataSpecification> metadata = Sets.mutable.with(DistributedMetadataSpecification.newSpecification("abc"), DistributedMetadataSpecification.newSpecification("def", "abc"), DistributedMetadataSpecification.newSpecification("ghi", "abc", "def"));

        Path directory = TMP.newFolder().toPath();
        List<String> paths = DistributedMetadataSpecification.writeSpecifications(directory, metadata);
        Assert.assertEquals(Sets.mutable.with("metadata/specs/abc.json", "metadata/specs/def.json", "metadata/specs/ghi.json"), Sets.mutable.withAll(paths));

        for (DistributedMetadataSpecification m : metadata)
        {
            Path file = directory.resolve(Paths.get("metadata", "specs", m.getName() + ".json"));
            DistributedMetadataSpecification loaded = DistributedMetadataSpecification.readSpecification(file);
            Assert.assertEquals(m.getName(), m, loaded);
        }
    }

    @Test
    public void testLoadMetadata_CurrentClassLoader()
    {
        Assert.assertEquals(Collections.emptyList(), ListIterate.select(DistributedMetadataSpecification.loadAllSpecifications(Thread.currentThread().getContextClassLoader()), x -> !x.getName().equals("platform")));

        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(Thread.currentThread().getContextClassLoader(), "non_existent"));
        Assert.assertEquals("Cannot find metadata \"non_existent\" (resource name \"metadata/specs/non_existent.json\")", e.getMessage());
    }

    @Test
    public void testLoadMetadata_Directories() throws IOException
    {
        List<DistributedMetadataSpecification> dir1Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("abc"), DistributedMetadataSpecification.newSpecification("def", "abc"));
        List<DistributedMetadataSpecification> dir2Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("ghi", "def"), DistributedMetadataSpecification.newSpecification("jkl", "xyz"));

        Path dir1 = TMP.newFolder().toPath();
        DistributedMetadataSpecification.writeSpecifications(dir1, dir1Metadata);

        Path dir2 = TMP.newFolder().toPath();
        DistributedMetadataSpecification.writeSpecifications(dir2, dir2Metadata);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{dir1.toUri().toURL(), dir2.toUri().toURL()}))
        {
            Assert.assertEquals(Sets.mutable.withAll(dir1Metadata).withAll(dir2Metadata), Sets.mutable.withAll(DistributedMetadataSpecification.loadAllSpecifications(classLoader)).select(r -> !r.getName().equals("platform")));

            Assert.assertEquals(Lists.mutable.with(DistributedMetadataSpecification.newSpecification("abc")), DistributedMetadataSpecification.loadSpecifications(classLoader, "abc"));

            Assert.assertEquals(Sets.mutable.withAll(dir1Metadata).with(dir2Metadata.get(0)), Sets.mutable.withAll(DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi")));

            RuntimeException e1 = Assert.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "jkl"));
            Assert.assertEquals("Cannot find metadata \"xyz\" (resource name \"metadata/specs/xyz.json\")", e1.getMessage());

            RuntimeException e2 = Assert.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "mno"));
            Assert.assertEquals("Cannot find metadata \"mno\" (resource name \"metadata/specs/mno.json\")", e2.getMessage());
        }
    }

    @Test
    public void testLoadMetadata_Jars() throws IOException
    {
        List<DistributedMetadataSpecification> jar1Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("abc"), DistributedMetadataSpecification.newSpecification("def", "abc"));
        List<DistributedMetadataSpecification> jar2Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("ghi", "def"), DistributedMetadataSpecification.newSpecification("jkl", "xyz"));

        Path dir = TMP.newFolder().toPath();
        Path jar1 = dir.resolve("jar1.jar");
        Path jar2 = dir.resolve("jar2.jar");

        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar1))))
        {
            jarStream.putNextEntry(new ZipEntry("metadata/"));
            jarStream.closeEntry();
            jarStream.putNextEntry(new ZipEntry("metadata/specs/"));
            jarStream.closeEntry();
            DistributedMetadataSpecification.writeSpecifications(jarStream, jar1Metadata);
        }

        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar2))))
        {
            jarStream.putNextEntry(new ZipEntry("metadata/"));
            jarStream.closeEntry();
            jarStream.putNextEntry(new ZipEntry("metadata/specs/"));
            jarStream.closeEntry();
            DistributedMetadataSpecification.writeSpecifications(jarStream, jar2Metadata);
        }

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar1.toUri().toURL(), jar2.toUri().toURL()}))
        {
            Assert.assertEquals(Sets.mutable.withAll(jar1Metadata).withAll(jar2Metadata), Sets.mutable.withAll(DistributedMetadataSpecification.loadAllSpecifications(classLoader)).select(r -> !r.getName().equals("platform")));

            Assert.assertEquals(Lists.mutable.with(DistributedMetadataSpecification.newSpecification("abc")), DistributedMetadataSpecification.loadSpecifications(classLoader, "abc"));

            Assert.assertEquals(Sets.mutable.withAll(jar1Metadata).with(jar2Metadata.get(0)), Sets.mutable.withAll(DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi")));

            RuntimeException e1 = Assert.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "jkl"));
            Assert.assertEquals("Cannot find metadata \"xyz\" (resource name \"metadata/specs/xyz.json\")", e1.getMessage());

            RuntimeException e2 = Assert.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "mno"));
            Assert.assertEquals("Cannot find metadata \"mno\" (resource name \"metadata/specs/mno.json\")", e2.getMessage());
        }
    }
}
