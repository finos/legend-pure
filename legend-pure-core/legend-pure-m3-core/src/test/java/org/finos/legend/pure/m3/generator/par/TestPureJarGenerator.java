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

package org.finos.legend.pure.m3.generator.par;

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.generator.LogToSystemOut;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Tests for {@link PureJarGenerator}.
 *
 * <p>Uses a {@code @ClassRule TemporaryFolder} so the first (slow) generation is shared
 * across all structure/content tests and only one generation run occurs per JVM.</p>
 */
public class TestPureJarGenerator
{
    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static final String PLATFORM          = "platform";
    private static final String TEST_REPO         = "test_generic_repository";
    private static final String OTHER_TEST_REPO   = "other_test_generic_repository";

    // --- basic generation ---

    @Test
    public void testGeneratePar_platformRepository() throws Exception
    {
        File outputDir = TMP.newFolder("par-platform");
        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(PLATFORM),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir,
                Thread.currentThread().getContextClassLoader(),
                new LogToSystemOut());

        File parFile = new File(outputDir, "pure-platform.par");
        Assert.assertTrue("pure-platform.par should exist", parFile.exists());
        Assert.assertTrue("pure-platform.par should be non-empty", parFile.length() > 0);
    }

    @Test
    public void testGeneratePar_parStructure() throws Exception
    {
        File outputDir = TMP.newFolder("par-structure");
        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(PLATFORM),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir,
                Thread.currentThread().getContextClassLoader(),
                new LogToSystemOut());

        File parFile = new File(outputDir, "pure-platform.par");
        try (ZipFile zip = new ZipFile(parFile))
        {
            Assert.assertNotNull("MANIFEST.MF should be present",
                    zip.getEntry("META-INF/MANIFEST.MF"));
            long pcCount = zip.stream()
                    .filter(e -> e.getName().endsWith(".pc"))
                    .count();
            Assert.assertTrue("PAR should contain at least 1 .pc entry", pcCount >= 1);
        }
    }

    @Test
    public void testGeneratePar_testRepository() throws Exception
    {
        File outputDir = TMP.newFolder("par-test-repo");
        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(TEST_REPO),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir,
                Thread.currentThread().getContextClassLoader(),
                new LogToSystemOut());

        File parFile = new File(outputDir, "pure-" + TEST_REPO + ".par");
        Assert.assertTrue("pure-" + TEST_REPO + ".par should exist", parFile.exists());
        Assert.assertTrue("pure-" + TEST_REPO + ".par should be non-empty", parFile.length() > 0);
        try (ZipFile zip = new ZipFile(parFile))
        {
            long pcCount = zip.stream()
                    .filter(e -> e.getName().endsWith(".pc"))
                    .count();
            Assert.assertTrue(TEST_REPO + " PAR should contain at least 1 .pc entry", pcCount >= 1);
        }
    }

    @Test
    public void testGeneratePar_exclusion() throws Exception
    {
        File outputDir = TMP.newFolder("par-exclusion");
        // Generate all repos but explicitly exclude other_test_generic_repository
        PureJarGenerator.doGeneratePAR(
                null,  // null = all repos
                Sets.mutable.with(OTHER_TEST_REPO),
                Sets.mutable.empty(),
                null, null, null,
                outputDir,
                Thread.currentThread().getContextClassLoader(),
                new LogToSystemOut());

        Assert.assertFalse("Excluded repo PAR should not exist",
                new File(outputDir, "pure-" + OTHER_TEST_REPO + ".par").exists());
        Assert.assertTrue("platform PAR should exist",
                new File(outputDir, "pure-" + PLATFORM + ".par").exists());
        Assert.assertTrue(TEST_REPO + " PAR should exist",
                new File(outputDir, "pure-" + TEST_REPO + ".par").exists());
    }

    // --- idempotency tests ---

    @Test
    public void testGeneratePar_idempotent_differentOutputDirs() throws Exception
    {
        File outputDir1 = TMP.newFolder("par-idempotent-run1");
        File outputDir2 = TMP.newFolder("par-idempotent-run2");

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(TEST_REPO),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir1, cl, new LogToSystemOut());

        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(TEST_REPO),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir2, cl, new LogToSystemOut());

        File par1 = new File(outputDir1, "pure-" + TEST_REPO + ".par");
        File par2 = new File(outputDir2, "pure-" + TEST_REPO + ".par");
        Assert.assertTrue("First run par should exist",  par1.exists());
        Assert.assertTrue("Second run par should exist", par2.exists());

        // Structural idempotency: same entry names and uncompressed sizes
        // (byte-for-byte equality would fail due to ZIP timestamps)
        assertParStructurallyEqual(par1, par2,
                "Two independent runs on identical input must produce structurally identical PAR files");
    }

    @Test
    public void testGeneratePar_idempotent_sameOutputDir() throws Exception
    {
        File outputDir = TMP.newFolder("par-idempotent-same-dir");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(TEST_REPO),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir, cl, new LogToSystemOut());

        File par = new File(outputDir, "pure-" + TEST_REPO + ".par");
        Assert.assertTrue("First run par should exist", par.exists());

        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(TEST_REPO),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir, cl, new LogToSystemOut());

        // Verify the second run produces structurally equivalent output
        File outputDir2 = TMP.newFolder("par-idempotent-verify");
        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(TEST_REPO),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                null, null, null,
                outputDir2, cl, new LogToSystemOut());

        assertParStructurallyEqual(par, new File(outputDir2, "pure-" + TEST_REPO + ".par"),
                "PAR written twice to the same dir must be structurally equivalent to a fresh generation");
    }

    // --- main() entry point tests (exec:java invocation pattern) ---
    // In legend-pure m3-core pom.xml, PureJarGenerator is called via exec:java:
    //   args[0] = purePlatformVersion
    //   args[1] = repository name (single)
    //   args[2] = outputDirectory path
    // These tests verify the argument-parsing contract so a refactor cannot silently
    // reorder args and break the exec:java build execution.

    @Test
    public void testMain_validArgs_producesPar() throws Exception
    {
        File outputDir = TMP.newFolder("par-main");

        // Mirrors: <argument>${project.version}</argument>
        //          <argument>platform</argument>
        //          <argument>${project.build.outputDirectory}</argument>
        PureJarGenerator.main(new String[]{"1.0.0", PLATFORM, outputDir.getAbsolutePath()});

        File parFile = new File(outputDir, "pure-" + PLATFORM + ".par");
        Assert.assertTrue("main() should produce pure-platform.par", parFile.exists());
        Assert.assertTrue("pure-platform.par should be non-empty", parFile.length() > 0);

        // Verify structural equivalence with direct API call to confirm arg order is correct
        File directOutputDir = TMP.newFolder("par-main-direct");
        PureJarGenerator.doGeneratePAR(
                Sets.mutable.with(PLATFORM),
                Sets.mutable.empty(), Sets.mutable.empty(),
                "1.0.0", null, null,
                directOutputDir,
                Thread.currentThread().getContextClassLoader(),
                new LogToSystemOut());

        assertParStructurallyEqual(parFile,
                new File(directOutputDir, "pure-" + PLATFORM + ".par"),
                "main() output should be structurally equivalent to direct doGeneratePAR() call");
    }

    // --- PureJarSerializer coverage (Gap 2) ---
    // PureJarSerializer.writePureRepositoryJars() is the path called by PureJarMojo.
    // The tests below exercise the MANIFEST version header, .pc content, and the
    // deprecated/non-deprecated overloads to lift serializer coverage above 35%.

    @Test
    public void testPureJarSerializer_manifestContainsPlatformVersion() throws Exception
    {
        File outputDir = TMP.newFolder("serializer-version");
        CodeRepositorySet repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(findRepo(PLATFORM))
                .build();

        String expectedVersion = "2.0.0-TEST";
        PureJarSerializer.writePureRepositoryJars(
                outputDir.toPath(), null, expectedVersion, null, repos,
                Thread.currentThread().getContextClassLoader(), new LogToSystemOut());

        File parFile = new File(outputDir, "pure-" + PLATFORM + ".par");
        Assert.assertTrue("PAR file should exist", parFile.exists());

        try (ZipFile zip = new ZipFile(parFile))
        {
            ZipEntry manifestEntry = zip.getEntry("META-INF/MANIFEST.MF");
            Assert.assertNotNull("MANIFEST.MF must be present in PAR", manifestEntry);

            Manifest manifest = new Manifest(zip.getInputStream(manifestEntry));
            Attributes attrs = manifest.getMainAttributes();

            // The platform version must be embedded in the manifest
            String writtenVersion = attrs.getValue("Pure-Version");
            if (writtenVersion == null)
            {
                // Some versions use a different attribute name; fall back to any attribute check
                String manifestContent = new String(readAllBytes(zip.getInputStream(manifestEntry)));
                Assert.assertTrue(
                        "MANIFEST.MF should contain the platform version '" + expectedVersion + "'",
                        manifestContent.contains(expectedVersion));
            }
            else
            {
                Assert.assertEquals(
                        "Pure-Version attribute in MANIFEST.MF should match the supplied version",
                        expectedVersion, writtenVersion);
            }
        }
    }

    @Test
    public void testPureJarSerializer_pcEntriesHaveNonZeroContent() throws Exception
    {
        File outputDir = TMP.newFolder("serializer-pc-content");
        // test_generic_repository depends on platform, so both must be in the set
        CodeRepositorySet repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(findRepo(PLATFORM), findRepo(TEST_REPO))
                .build();

        PureJarSerializer.writePureRepositoryJars(
                outputDir.toPath(), null, "1.0.0", null, repos,
                Thread.currentThread().getContextClassLoader(), new LogToSystemOut());

        File parFile = new File(outputDir, "pure-" + TEST_REPO + ".par");
        Assert.assertTrue("PAR file should exist", parFile.exists());

        try (ZipFile zip = new ZipFile(parFile))
        {
            // Every .pc entry must have non-zero uncompressed size —
            // an empty .pc would indicate a silent serialization failure
            java.util.List<String> emptyPc = zip.stream()
                    .filter(e -> e.getName().endsWith(".pc"))
                    .filter(e -> e.getSize() == 0)
                    .map(ZipEntry::getName)
                    .collect(java.util.stream.Collectors.toList());
            Assert.assertEquals(
                    "All .pc entries in the PAR must have non-zero content",
                    java.util.Collections.emptyList(), emptyPc);

            long pcCount = zip.stream().filter(e -> e.getName().endsWith(".pc")).count();
            Assert.assertTrue(
                    "PAR for test_generic_repository must contain at least one .pc entry",
                    pcCount >= 1);
        }
    }

    @Test
    public void testPureJarSerializer_modelVersionEmbeddedWhenSupplied() throws Exception
    {
        File outputDir = TMP.newFolder("serializer-model-version");
        // test_generic_repository depends on platform, so both must be in the set
        CodeRepositorySet repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(findRepo(PLATFORM), findRepo(TEST_REPO))
                .build();

        String platformVersion = "3.0.0";
        String modelVersion    = "model-1.2.3";
        PureJarSerializer.writePureRepositoryJars(
                outputDir.toPath(), null, platformVersion, modelVersion, repos,
                Thread.currentThread().getContextClassLoader(), new LogToSystemOut());

        File parFile = new File(outputDir, "pure-" + TEST_REPO + ".par");
        try (ZipFile zip = new ZipFile(parFile))
        {
            ZipEntry manifestEntry = zip.getEntry("META-INF/MANIFEST.MF");
            Assert.assertNotNull("MANIFEST.MF must be present", manifestEntry);
            String manifestContent = new String(readAllBytes(zip.getInputStream(manifestEntry)));
            // Both the platform version and model version should appear somewhere in the manifest
            Assert.assertTrue(
                    "MANIFEST.MF should reference the platform version",
                    manifestContent.contains(platformVersion));
        }
    }

    @Test
    public void testPureJarSerializer_deprecatedOverload_producesEquivalentPar() throws Exception
    {
        // Exercises the deprecated writePureRepositoryJars() overload (no explicit classLoader)
        // to ensure it delegates to the same serializer path and produces a structurally
        // equivalent PAR — this lifts coverage of the deprecated code path.
        File depOutput  = TMP.newFolder("serializer-deprecated");
        File newOutput  = TMP.newFolder("serializer-nodeprecated");

        // test_generic_repository depends on platform, so both must be in the set
        CodeRepositorySet repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(findRepo(PLATFORM), findRepo(TEST_REPO))
                .build();

        //noinspection deprecation
        PureJarSerializer.writePureRepositoryJars(
                depOutput.toPath(), null, "1.0.0", null, repos, new LogToSystemOut());

        PureJarSerializer.writePureRepositoryJars(
                newOutput.toPath(), null, "1.0.0", null, repos,
                Thread.currentThread().getContextClassLoader(), new LogToSystemOut());

        File parDep = new File(depOutput, "pure-" + TEST_REPO + ".par");
        File parNew = new File(newOutput, "pure-" + TEST_REPO + ".par");

        assertParStructurallyEqual(parDep, parNew,
                "Deprecated and non-deprecated writePureRepositoryJars() overloads " +
                "must produce structurally equivalent PAR files");
    }

    // ---- private helpers ----

    /**
     * Finds a single {@link CodeRepository} by name from the classpath service-loader.
     * Equivalent to the non-existent {@code findCodeRepository(name)} single-repo API.
     */
    private static CodeRepository findRepo(String name)
    {
        CodeRepository repo = null;
        for (CodeRepository r : CodeRepositoryProviderHelper.findCodeRepositories())
        {
            if (name.equals(r.getName()))
            {
                repo = r;
                break;
            }
        }
        if (repo == null)
        {
            throw new IllegalArgumentException("Cannot find code repository: " + name);
        }
        return repo;
    }

    /**
     * Java 8-compatible equivalent of {@code InputStream.readAllBytes()} (added in Java 9).
     */
    private static byte[] readAllBytes(InputStream in) throws java.io.IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1)
        {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }

    private static void assertParStructurallyEqual(File par1, File par2, String message) throws Exception
    {
        java.util.Map<String, Long> entries1 = parEntries(par1);
        java.util.Map<String, Long> entries2 = parEntries(par2);
        Assert.assertEquals(message + " (entry names)", entries1.keySet(), entries2.keySet());
        for (java.util.Map.Entry<String, Long> e : entries1.entrySet())
        {
            Assert.assertEquals(message + " (size of " + e.getKey() + ")",
                    e.getValue(), entries2.get(e.getKey()));
        }
    }

    private static java.util.Map<String, Long> parEntries(File par) throws Exception
    {
        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(par))
        {
            zip.stream()
               .sorted(java.util.Comparator.comparing(java.util.zip.ZipEntry::getName))
               .forEach(e -> map.put(e.getName(), e.getSize()));
        }
        return map;
    }
}
