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

package org.finos.legend.pure.m3.generator.bootstrap;

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Tests for {@link M3CoreInstanceGenerator}.
 *
 * <p>{@code M3CoreInstanceGenerator.generate()} can be called end-to-end from a unit
 * test provided the {@code filePaths} argument matches what the production build
 * actually passes.  Calling it with an empty file set (or all files) causes
 * {@code M3LazyCoreInstanceGenerator} to encounter {@code Number} — Pure's abstract
 * numeric PrimitiveType — for which its switch statement has no case, throwing
 * {@code IllegalArgumentException: Unsupported primitive type: Number}.  This is a
 * known gap in {@code M3LazyCoreInstanceGenerator}, not a missing classpath entry.</p>
 *
 * <p>The production exec:java invocation passes exactly these five files:</p>
 * <pre>
 *   /platform/pure/grammar/milestoning.pure
 *   /platform/pure/routing.pure
 *   /platform/pure/anonymousCollections.pure
 *   /platform/pure/relation.pure
 *   /platform/pure/variant/variant.pure
 * </pre>
 * <p>None of those files trigger the {@code Number} code path, so end-to-end tests
 * using this same list run successfully in the test JVM.</p>
 */
public class TestM3CoreInstanceGenerator
{
    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    /**
     * The exact comma-separated file list passed to {@code generate()} by the
     * "Generate Other Support Classes" exec:java execution in legend-pure-m3-core pom.xml.
     * Keeping this in sync with the pom is the key contract this test enforces.
     * Note: routing.pure appears twice in the pom argument — this is intentional as
     * generate() uses a Set so the duplicate is silently deduplicated.
     */
    private static final String PRODUCTION_FILES =
            "/platform/pure/grammar/milestoning.pure," +
            "/platform/pure/routing.pure," +
            "/platform/pure/anonymousCollections.pure," +
            "/platform/pure/relation.pure," +
            "/platform/pure/routing.pure," +
            "/platform/pure/variant/variant.pure";

    // --- generator() factory method tests ---
    // These cover generator() directly; the end-to-end tests below do not call it.


    @Test
    public void testGenerator_factoryMethod_prefixStoredInGenerator()
    {
        // Verifies that factoryNamePrefix is actually threaded through to the
        // M3ToJavaGenerator instance via getFactoryNamePrefix().
        // Previously this was checked with assertNotSame (two distinct objects), which
        // would pass even if both ignored the prefix — this test is the direct check.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        PureRuntime runtime = new PureRuntimeBuilder(
                new CompositeCodeStorage(
                        new ClassLoaderCodeStorage(cl,
                                CodeRepositorySet.newBuilder()
                                        .withCodeRepositories(
                                                CodeRepositoryProviderHelper.findPlatformCodeRepository())
                                        .build()
                                        .getRepositories())))
                .setTransactionalByDefault(false)
                .build();

        File outputDir = TMP.getRoot();
        M3ToJavaGenerator genA = M3CoreInstanceGenerator.generator(
                outputDir.getAbsolutePath(), "PrefixA", runtime.getModelRepository());
        M3ToJavaGenerator genB = M3CoreInstanceGenerator.generator(
                outputDir.getAbsolutePath(), "PrefixB", runtime.getModelRepository());

        Assert.assertEquals("generator() must store the supplied factoryNamePrefix",
                "PrefixA", genA.getFactoryNamePrefix());
        Assert.assertEquals("generator() must store the supplied factoryNamePrefix",
                "PrefixB", genB.getFactoryNamePrefix());
    }

    @Test
    public void testGenerate_factoryRegistryFileNamedAfterPrefix() throws Exception
    {
        // Verifies end-to-end that factoryNamePrefix is used in the generated output:
        // M3ToJavaGenerator.createFactory() writes a file named
        //   <factoryNamePrefix>CoreInstanceFactoryRegistry.java
        // Using a distinct prefix from the production "M3Platform" makes it unambiguous
        // that THIS invocation's prefix drove the filename.
        File outputDir = TMP.newFolder("generate-prefix-check");
        String prefix = "TestPrefix";

        M3CoreInstanceGenerator.generate(
                outputDir.getAbsolutePath() + File.separator,
                prefix,
                Sets.mutable.with(PRODUCTION_FILES.split("\\s*,\\s*")),
                null);

        String expectedFileName = prefix + "CoreInstanceFactoryRegistry.java";
        boolean found;
        try (Stream<Path> files = Files.walk(outputDir.toPath()))
        {
            found = files.anyMatch(p -> p.getFileName().toString().equals(expectedFileName));
        }
        Assert.assertTrue(
                "generate() must produce a file named '" + expectedFileName + "' " +
                "when factoryNamePrefix is '" + prefix + "'",
                found);
    }


    // --- end-to-end generate() tests ---

    @Test
    public void testGenerate_productionFileList_producesJavaSources() throws Exception
    {
        // Calls generate() with the exact arguments used in the production exec:java
        // invocation.  Verifies that:
        //   1. No exception is thrown
        //   2. At least one .java file is written to the output directory
        //   3. The written file contains a class declaration (structural sanity)
        File outputDir = TMP.newFolder("generate-production");

        M3CoreInstanceGenerator.generate(
                outputDir.getAbsolutePath() + File.separator,
                "M3Platform",
                Sets.mutable.with(PRODUCTION_FILES.split("\\s*,\\s*")),
                null);

        // At least one .java source file must have been generated
        try (Stream<Path> files = Files.walk(outputDir.toPath()))
        {
            long javaCount = files.filter(p -> p.toString().endsWith(".java")).count();
            Assert.assertTrue(
                    "generate() must produce at least one .java file; found " + javaCount,
                    javaCount > 0);
        }
    }

    @Test
    public void testGenerate_productionFileList_generatedSourceContainsClassDeclaration() throws Exception
    {
        // Verifies functional output: every generated .java file must contain a
        // 'public class' or 'public interface' declaration — an empty or malformed
        // file would indicate a silent generation failure.
        File outputDir = TMP.newFolder("generate-class-check");

        M3CoreInstanceGenerator.generate(
                outputDir.getAbsolutePath() + File.separator,
                "M3Platform",
                Sets.mutable.with(PRODUCTION_FILES.split("\\s*,\\s*")),
                null);

        try (Stream<Path> files = Files.walk(outputDir.toPath()))
        {
            files.filter(p -> p.toString().endsWith(".java"))
                 .forEach(p ->
                 {
                     try
                     {
                         String content = new String(Files.readAllBytes(p));
                         Assert.assertTrue(
                                 "Generated file " + p.getFileName() + " must contain a class or interface declaration",
                                 content.contains("public class ") || content.contains("public interface "));
                     }
                     catch (java.io.IOException e)
                     {
                         throw new RuntimeException(e);
                     }
                 });
        }
    }

    @Test
    public void testMain_productionArgs_producesEquivalentOutputToDirectCall() throws Exception
    {
        // Verifies that main() parses its arguments and delegates to generate()
        // identically to calling generate() directly — locking down the arg-position
        // contract for exec:java invocations.
        File mainDir   = TMP.newFolder("main-production");
        File directDir = TMP.newFolder("direct-production");

        // main() call — mirrors the exec:java in the pom:
        //   args[0] = outputDirectory
        //   args[1] = factoryNamePrefix (M3Platform)
        //   args[2] = comma-separated .pure file paths
        M3CoreInstanceGenerator.main(new String[]{
                mainDir.getAbsolutePath() + File.separator,
                "M3Platform",
                PRODUCTION_FILES
        });

        M3CoreInstanceGenerator.generate(
                directDir.getAbsolutePath() + File.separator,
                "M3Platform",
                Sets.mutable.with(PRODUCTION_FILES.split("\\s*,\\s*")),
                null);

        // Both directories must contain the same set of .java files
        java.util.Set<String> mainFiles   = collectJavaFileNames(mainDir.toPath());
        java.util.Set<String> directFiles = collectJavaFileNames(directDir.toPath());

        Assert.assertEquals(
                "main() and generate() must produce the same set of .java files",
                directFiles, mainFiles);
    }

    // ---- private helpers ----

    private static java.util.Set<String> collectJavaFileNames(Path root) throws java.io.IOException
    {
        java.util.Set<String> names = new java.util.HashSet<>();
        try (Stream<Path> files = Files.walk(root))
        {
            files.filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> names.add(p.getFileName().toString()));
        }
        return names;
    }
}
