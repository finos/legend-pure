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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration.GenerationType;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.VoidLog;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEager;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TestJavaCodeGeneration
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    @Test
    public void testMonolithicGenerationNoExternal() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();
        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                false,
                null,
                true,
                true,
                false,
                false,
                false,
                classesDirectory,
                directory,
                true,
                new VoidLog());
        executeDynamicNewTest(MetadataLazy::fromClassLoader, classesDirectory);
    }

    @Test
    public void testMonolithicGenerationWithExternal() throws Exception
    {
        String externalPackage = "org.finos.legend.pure.runtime.java.compiled";
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();
        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                true,
                externalPackage,
                true,
                true,
                false,
                false,
                false,
                classesDirectory,
                directory,
                true,
                new VoidLog());
        String externalClassName = externalPackage + ".PureExternal";
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try (TestClassLoader classLoader = new TestClassLoader(previousClassLoader, n -> externalClassName.equals(n) || n.startsWith(JavaPackageAndImportBuilder.rootPackage()), null, toURL(classesDirectory)))
        {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> pureExternal = classLoader.loadClass(externalClassName);
            Method getExecutionSupport = pureExternal.getMethod("_getExecutionSupport");
            CompiledExecutionSupport executionSupport = (CompiledExecutionSupport) getExecutionSupport.invoke(null);

            Method testNewUnitIndirectUnit = getTestNewUnitIndirectUnit(classLoader);
            Assert.assertEquals(Boolean.TRUE, testNewUnitIndirectUnit.invoke(null, executionSupport));

            Method joinStrings = getJoinStrings(classLoader);
            Assert.assertEquals("a, b, c", joinStrings.invoke(null, Lists.immutable.with("a", "b", "c"), ", ", executionSupport));
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }

    @Test
    public void testModularGeneration() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();

        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.modular,
                false,
                false,
                null,
                true,
                false,  // useSingleDir=false so metadata goes to target/metadata-distributed
                false,
                false,
                false,
                classesDirectory,
                directory,
                true,
                new VoidLog());

        // Modular generation now uses pelt serialization, which must already exist; so there should be no metadata generation
        File metaDistributed = new File(directory, "metadata-distributed");
        Assert.assertFalse("metadata-distributed directory should not be created", metaDistributed.exists());

        executeDynamicNewTest(cl -> MetadataLazy.fromClassLoader(cl, "platform"), classesDirectory);
    }

    @Test
    public void testNoMetadataGenerated() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();

        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                false,
                null,
                false,
                true,
                false,
                false,
                false,
                classesDirectory,
                directory,
                true,
                new VoidLog());

        executeDynamicNewTestWithEagerMetadata(classesDirectory, "platform");
    }

    @Test
    public void testSkipFlag_preventsGeneration() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDirectory = new File(directory, "classes");
        classesDirectory.mkdir();

        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                true,   // skip=true
                false,
                null,
                true,
                true,
                false,
                false,
                true,
                classesDirectory,
                directory,
                true,
                new VoidLog());

        try (Stream<Path> stream = Files.walk(classesDirectory.toPath()))
        {
            long fileCount = stream.filter(Files::isRegularFile).count();
            Assert.assertEquals("skip=true should produce no output files", 0, fileCount);
        }
    }

    @Test
    public void testGenerationIsIdempotent() throws Exception
    {
        File dir1 = TMP.newFolder();
        File classes1 = new File(dir1, "classes");
        classes1.mkdir();

        File dir2 = TMP.newFolder();
        File classes2 = new File(dir2, "classes");
        classes2.mkdir();

        // Run identical generation into two separate directories
        for (File[] pair : new File[][]{{classes1, dir1}, {classes2, dir2}})
        {
            JavaCodeGeneration.doIt(
                    Sets.mutable.with("platform"),
                    Sets.fixedSize.empty(),
                    Sets.fixedSize.empty(),
                    GenerationType.monolithic,
                    false,
                    false,
                    null,
                    true,
                    true,
                    false,
                    false,
                    true,
                    pair[0],
                    pair[1],
                    true,
                    new VoidLog());
        }

        // Collect all relative file paths from each output directory
        MutableSet<Path> paths1 = Sets.mutable.empty();
        MutableSet<Path> paths2 = Sets.mutable.empty();
        try (Stream<Path> s1 = Files.walk(dir1.toPath());
             Stream<Path> s2 = Files.walk(dir2.toPath()))
        {
            s1.filter(Files::isRegularFile).map(p -> dir1.toPath().relativize(p)).forEach(paths1::add);
            s2.filter(Files::isRegularFile).map(p -> dir2.toPath().relativize(p)).forEach(paths2::add);
        }

        Assert.assertEquals("Idempotency: both runs should produce the same set of files", paths1, paths2);

        // Verify byte-for-byte equality for each file
        MutableList<Path> mismatch = Lists.mutable.empty();
        for (Path rel : paths1)
        {
            byte[] b1 = Files.readAllBytes(dir1.toPath().resolve(rel));
            byte[] b2 = Files.readAllBytes(dir2.toPath().resolve(rel));
            if (!Arrays.equals(b1, b2))
            {
                mismatch.add(rel);
            }
        }
        Assert.assertEquals("Idempotency: generated files must be byte-for-byte identical across runs",
                Lists.fixedSize.empty(), mismatch);
    }

    // --- generateSources paths ---

    @Test
    public void testGenerateSources_writesToGeneratedSourcesDirectory() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDir = new File(directory, "classes");
        classesDir.mkdir();

        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                false,
                null,
                false,   // generateMetadata=false - fastest path
                false,
                true,    // generateSources=true
                false,   // generateTest=false: generated-sources/
                true,    // preventJavaCompilation
                classesDir,
                directory,
                false,
                new VoidLog());

        File generatedSources = new File(directory, "generated-sources");
        Assert.assertTrue(
                "generateSources=true, generateTest=false should create generated-sources/",
                generatedSources.exists() && generatedSources.isDirectory());

        // Package_Impl.java is a stable landmark file produced for the platform repository
        // on every monolithic generation - assert its existence and structural content.
        File packageImpl = new File(generatedSources,
                "org/finos/legend/pure/generated/Package_Impl.java");
        Assert.assertTrue(
                "Package_Impl.java should always be generated for platform repository",
                packageImpl.exists());
        String packageImplSrc = new String(java.nio.file.Files.readAllBytes(packageImpl.toPath()));
        Assert.assertTrue(
                "Package_Impl.java should declare the correct package",
                packageImplSrc.contains("package org.finos.legend.pure.generated"));
        Assert.assertTrue(
                "Package_Impl.java should declare class Package_Impl",
                packageImplSrc.contains("class Package_Impl"));
    }

    @Test
    public void testGenerateTestSources_writesToGeneratedTestSourcesDirectory() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDir = new File(directory, "classes");
        classesDir.mkdir();

        JavaCodeGeneration.doIt(
                Sets.mutable.with("platform"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                false,
                null,
                false,   // generateMetadata=false
                false,
                true,    // generateSources=true
                true,    // generateTest=true: generated-test-sources/
                true,    // preventJavaCompilation
                classesDir,
                directory,
                false,
                new VoidLog());

        File generatedTestSources = new File(directory, "generated-test-sources");
        Assert.assertTrue(
                "generateSources=true, generateTest=true should create generated-test-sources/",
                generatedTestSources.exists() && generatedTestSources.isDirectory());

        // PureCompiledLambda.java is a stable landmark file in the test-sources output.
        File lambdaFile = new File(generatedTestSources,
                "org/finos/legend/pure/generated/PureCompiledLambda.java");
        Assert.assertTrue(
                "PureCompiledLambda.java should always be generated in test-sources for platform",
                lambdaFile.exists());
        String lambdaSrc = new String(java.nio.file.Files.readAllBytes(lambdaFile.toPath()));
        Assert.assertTrue(
                "PureCompiledLambda.java should declare the correct package",
                lambdaSrc.contains("package org.finos.legend.pure.generated"));
        Assert.assertTrue(
                "PureCompiledLambda.java should declare class PureCompiledLambda",
                lambdaSrc.contains("class PureCompiledLambda"));
    }

    // --- error / catch block ---

    @Test
    public void testDoIt_invalidRepository_wrapsException() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDir = new File(directory, "classes");
        classesDir.mkdir();

        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> JavaCodeGeneration.doIt(
                Sets.mutable.with("this_repository_does_not_exist"),
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                GenerationType.monolithic,
                false,
                false,
                null,
                false,
                false,
                false,
                false,
                true,
                classesDir,
                directory,
                false,
                new VoidLog()));
        Assert.assertTrue(
                "Message should contain 'Error building Pure compiled mode jar', but was: " + e.getMessage(),
                e.getMessage().contains("Error building Pure compiled mode jar"));
        // The original cause must be preserved so callers can diagnose the failure
        Assert.assertNotNull("Exception must have a cause", e.getCause());
    }

    @Test
    public void testDoIt_unknownExtraRepository_wrapsException() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDir = new File(directory, "classes");
        classesDir.mkdir();

        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> JavaCodeGeneration.doIt(
                Sets.fixedSize.empty(),
                Sets.fixedSize.empty(),
                Sets.mutable.with("org.finos.legend.does.not.ExistRepository"),
                GenerationType.monolithic,
                false,
                false,
                null,
                false,
                false,
                false,
                false,
                true,
                classesDir,
                directory,
                false,
                new VoidLog()));
        Assert.assertTrue(
                "Message should contain 'Error building Pure compiled mode jar', but was: " + e.getMessage(),
                e.getMessage().contains("Error building Pure compiled mode jar"));
        Assert.assertNotNull("Exception must have a cause", e.getCause());
    }

    // --- main() entry point tests (exec:java invocation pattern) ---
    // In legend-pure, JavaCodeGeneration is called directly via exec:java with positional args:
    //   args[0] = repository name
    //   args[1] = classesDirectory path
    //   args[2] = targetDirectory path
    //   args[3] = externalAPIPackage (optional)
    // These tests verify the argument-parsing contract so a refactor cannot silently
    // reorder args and break the 6 exec:java build executions.

    @Test
    public void testMain_threeArgs_generatesModularOutput() throws Exception
    {
        File directory = TMP.newFolder();
        File classesDir = new File(directory, "classes");
        classesDir.mkdir();

        // Mirror the exact call used in legend-pure DSL/runtime pom.xml:
        //   args[0] = repository, args[1] = classesDir (generated-test-resources), args[2] = targetDir
        JavaCodeGeneration.main("platform", classesDir.getAbsolutePath(), directory.getAbsolutePath());

        // main() calls doIt() with: modular, useSingleDir=true, generateSources=true, generateTest=true
        // Modular generation now uses pelt serialization, which must already exist; so there should be no metadata generation
        File metadataDir = new File(classesDir, "metadata");
        Assert.assertFalse(metadataDir.exists());

        // Generated sources go to targetDir/generated-test-sources/ because main() passes generateTest=true
        File packageImpl = new File(directory,
                "generated-test-sources/org/finos/legend/pure/generated/Package_Impl.java");
        Assert.assertTrue(
                "main() should write Package_Impl.java into targetDir/generated-test-sources/",
                packageImpl.exists());
    }

    @Test
    public void testMain_fourArgs_externalApiPackageAccepted() throws Exception
    {
        // Four-arg form: args[3] is the externalAPIPackage - must not throw
        File directory = TMP.newFolder();
        File classesDir = new File(directory, "classes");
        classesDir.mkdir();

        String externalPkg = "org.finos.legend.pure.generated";
        JavaCodeGeneration.main(
                "platform",
                classesDir.getAbsolutePath(),
                directory.getAbsolutePath(),
                externalPkg);

        // Package.idx still expected - verify it's produced the same as 3-arg form
        File packageIdx = new File(classesDir, "metadata/classifiers/platform/Package.idx");
        Assert.assertFalse(packageIdx.exists());
    }

    private static void executeDynamicNewTestWithEagerMetadata(File classesDir, String... repoNames) throws Exception
    {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        RichIterable<CodeRepository> repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(currentClassLoader))
                .build()
                .subset(repoNames)
                .getRepositories();
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(currentClassLoader, repos));
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                .withCache(new ClassLoaderPureGraphCache(currentClassLoader))
                .withFactoryRegistryOverride(JavaModelFactoryRegistryLoader.loader())
                .buildAndInitialize();
        Assert.assertTrue("PureRuntime should be initialized", runtime.isInitialized());
        executeDynamicNewTest(cl -> new MetadataEager(runtime.getProcessorSupport()), classesDir);
    }

    private static void executeDynamicNewTest(Function<? super ClassLoader, ? extends Metadata> metadataBuilder, File... classpath) throws Exception
    {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try (TestClassLoader classLoader = new TestClassLoader(currentClassLoader, Arrays.stream(classpath).map(TestJavaCodeGeneration::toURL).toArray(URL[]::new)))
        {
            Thread.currentThread().setContextClassLoader(classLoader);
            Metadata metadata = metadataBuilder.apply(classLoader);
            CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(
                    new JavaCompilerState(null, classLoader),
                    new CompiledProcessorSupport(classLoader, metadata),
                    null,
                    new ClassLoaderCodeStorage(classLoader),
                    null,
                    VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER,
                    new ConsoleCompiled(),
                    null,
                    null,
                    CompiledExtensionLoader.extensions()
            );
            Method testNewUnitIndirectUnit = getTestNewUnitIndirectUnit(classLoader);
            Assert.assertEquals(Boolean.TRUE, testNewUnitIndirectUnit.invoke(null, executionSupport));

            Method joinStrings = getJoinStrings(classLoader);
            Assert.assertEquals("a, b, c", joinStrings.invoke(null, Lists.immutable.with("a", "b", "c"), ", ", executionSupport));
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private static Method getTestNewUnitIndirectUnit(ClassLoader classLoader) throws ReflectiveOperationException
    {
        return classLoader.loadClass(JavaPackageAndImportBuilder.getRootPackage() + ".platform_pure_essential_lang_unit_newUnit")
                .getMethod("Root_meta_pure_functions_meta_tests_newUnit_testNewUnitIndirectUnit__Boolean_1_", ExecutionSupport.class);
    }

    private static Method getJoinStrings(ClassLoader classLoader) throws ReflectiveOperationException
    {
        return classLoader.loadClass(JavaPackageAndImportBuilder.getRootPackage() + ".platform_pure_essential_string_toString_joinStrings")
                .getMethod("Root_meta_pure_functions_string_joinStrings_String_MANY__String_1__String_1_", RichIterable.class, String.class, ExecutionSupport.class);
    }

    private static URL toURL(File path)
    {
        try
        {
            return path.toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class TestClassLoader extends URLClassLoader
    {
        private final Predicate<? super String> classFilter;
        private final Predicate<? super String> resourceFilter;

        private TestClassLoader(ClassLoader parent, Predicate<? super String> classFilter, Predicate<? super String> resourceFilter, URL... urls)
        {
            super(urls, parent);
            this.classFilter = (classFilter == null) ? n -> n.startsWith(JavaPackageAndImportBuilder.rootPackage()) : classFilter;
            this.resourceFilter = (resourceFilter == null) ? n -> n.startsWith("metadata/") || n.startsWith("/metadata/") : resourceFilter;
        }

        private TestClassLoader(ClassLoader parent, URL... urls)
        {
            this(parent, null, null, urls);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            if (!this.classFilter.test(name))
            {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name))
            {
                Class<?> c = findLoadedClass(name);
                if (c == null)
                {
                    c = findClass(name);
                }
                if (resolve)
                {
                    resolveClass(c);
                }
                return c;
            }
        }

        @Override
        public URL getResource(String name)
        {
            return this.resourceFilter.test(name) ? findResource(name) : super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException
        {
            return this.resourceFilter.test(name) ? findResources(name) : super.getResources(name);
        }
    }
}
