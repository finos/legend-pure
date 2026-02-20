// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.empty.EmptyCodeStorage;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.Generate;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaStandaloneLibraryGenerator;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.VoidLog;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataPelt;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestJavaStandaloneLibraryGenerator extends AbstractPureTestWithCoreCompiled
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    @BeforeClass
    public static void setUp()
    {
        RichIterable<? extends CodeRepository> repositories = getCodeRepositories();
        MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(
                new ClassLoaderCodeStorage(repositories),
                new EmptyCodeStorage(new GenericCodeRepository("test", "test::.*", "platform", "core_functions_unclassified"), new GenericCodeRepository("other", "other::.*", "test")));

        setUpRuntime(codeStorage, JavaModelFactoryRegistryLoader.loader());

        runtime.createInMemorySource(
                "/test/standalone/tests.pure",
                "import test::standalone::*;\n" +
                        "\n" +
                        "Enum test::standalone::TestEnumeration\n" +
                        "{\n" +
                        "    TYPE1, TYPE2, TYPE3\n" +
                        "}\n" +
                        "\n" +
                        "Class test::standalone::TestClassA\n" +
                        "{\n" +
                        "    name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::standalone::TestClassB\n" +
                        "{\n" +
                        "    id:Integer[1];\n" +
                        "    type:TestEnumeration[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association test::standalone::TestAssociation\n" +
                        "{\n" +
                        "    toA:TestClassA[1];\n" +
                        "    toB:TestClassB[1];\n" +
                        "}\n" +
                        "\n" +
                        "function <<access.externalizable>> test::standalone::joinWithCommas(strings:String[*]):String[1]\n" +
                        "{\n" +
                        "    $strings->joinStrings(', ');\n" +
                        "}\n" +
                        "\n" +
                        "function <<test.Test>> test::standalone::simplePureTestWithApplication():Boolean[1]\n" +
                        "{\n" +
                        "    true;\n" +
                        "}\n" +
                        "function <<test.Test>> test::standalone::simplePureTest():Boolean[1]\n" +
                        "{\n" +
                        "    true;\n" +
                        "}\n" +
                        "function test::standalone::simplePureTestReference():Boolean[1]\n" +
                        "{\n" +
                        "    test::standalone::simplePureTestWithApplication();\n" +
                        "}\n" +
                        "function <<access.externalizable>> test::standalone::testWithReflection(prefix:String[1]):String[1]\n" +
                        "{\n" +
                        "    let f = testWithReflection_String_1__String_1_;\n" +
                        "    let class = TestClassA;\n" +
                        "    let association = TestAssociation;\n" +
                        "    let b = ^TestClassB(id=43, type=TestEnumeration.TYPE3, toA=^TestClassA(name=$prefix));\n" +
                        "    if($class == $association, |'ERROR', |$b.toA.name + $f.functionName->toOne());\n" +
                        "}\n"
        );
        runtime.createInMemorySource(
                "/other/standalone/other.pure",
                "Class other::standalone::TestClassC extends test::standalone::TestClassA\n" +
                        "{\n" +
                        "}\n"
        );
        runtime.compile();
    }

    @Test
    public void testStandaloneLibraryNoExternalDistributedMetadata() throws Exception
    {
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, null, new VoidLog());
        Path classesDir = TMP.newFolder().toPath();
        generator.serializeAndWriteDistributedMetadata(classesDir);
        generator.compileAndWriteClasses(classesDir, new VoidLog());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            Metadata metadata = MetadataLazy.fromClassLoader(classLoader);
            testStandaloneLibraryNoExternal(classLoader, metadata);
        }
    }

    @Test
    public void testStandaloneLibraryNoExternalPeltMetadata() throws Exception
    {
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, null, new VoidLog());
        Path classesDir = TMP.newFolder().toPath();
        generator.serializeAndWriteMetadata(classesDir, runtime.getCodeStorage().getAllRepositories().collectIf(CodeRepositoryProviderHelper.notPlatformAndCore, CodeRepository::getName, Sets.mutable.empty()));
        generator.compileAndWriteClasses(classesDir, new VoidLog());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            Metadata metadata = MetadataPelt.fromClassLoader(classLoader, runtime.getCodeStorage().getAllRepositories().asLazy().collect(CodeRepository::getName));
            testStandaloneLibraryNoExternal(classLoader, metadata);
        }
    }

    private void testStandaloneLibraryNoExternal(ClassLoader classLoader, Metadata metadata) throws Exception
    {
        CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(
                new JavaCompilerState(null, classLoader),
                new CompiledProcessorSupport(classLoader, metadata),
                null,
                runtime.getCodeStorage(),
                null,
                VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER,
                new ConsoleCompiled(),
                null,
                null,
                CompiledExtensionLoader.extensions()
        );

        String className = JavaPackageAndImportBuilder.getRootPackage() + ".test_standalone_tests";
        Class<?> testClass = classLoader.loadClass(className);

        Method joinWithCommas = testClass.getMethod("Root_test_standalone_joinWithCommas_String_MANY__String_1_", RichIterable.class, ExecutionSupport.class);
        Object result1 = joinWithCommas.invoke(null, Lists.immutable.with("a", "b", "c"), executionSupport);
        Assert.assertEquals("a, b, c", result1);

        Method testWithReflection = testClass.getMethod("Root_test_standalone_testWithReflection_String_1__String_1_", String.class, ExecutionSupport.class);
        Object result2 = testWithReflection.invoke(null, "_*_", executionSupport);
        Assert.assertEquals("_*_testWithReflection", result2);
    }

    @Test
    public void testGenerateOnly_allRepos() throws Exception
    {
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, null, new VoidLog());
        Path rootFolder = TMP.newFolder().toPath();
        Path sourcesDir = Files.createDirectories(rootFolder.resolve("src").resolve("java"));
        Assert.assertEquals(Lists.fixedSize.empty(), Files.list(sourcesDir).collect(Collectors.toList()));

        Generate generate = generator.generateOnly(true, sourcesDir);
        Assert.assertEquals(Lists.fixedSize.with("test_generic_repository", "other_test_generic_repository", "test", "other", "platform").sortThis(), generate.getJavaSourcesByGroup().keysView().toList().sortThis());
        Assert.assertNotEquals(Lists.immutable.empty(), generate.getJavaSourcesByGroup().get("test"));
        Assert.assertNotEquals(Lists.immutable.empty(), generate.getJavaSourcesByGroup().get("other"));

        List<Path> files = Files.walk(sourcesDir).filter(Files::isRegularFile).collect(Collectors.toList());
        Assert.assertNotEquals(Lists.fixedSize.empty(), files);
        Pattern pattern = Pattern.compile("org/finos/legend/pure/generated/(CoreGen|LambdaZero|PureCompiledLambda|PureEnum(_(LazyImpl|LazyComponent))?|(Package_(Impl|LazyImpl|LazyConcrete|LazyVirtual))|((test|platform|other)_\\w++)|(Root_(meta|test|other)_\\w++(\\$\\w++)?))\\.java");
        Assert.assertEquals(Collections.emptyList(), ListIterate.reject(files, f -> pattern.matcher(Iterate.makeString(sourcesDir.relativize(f), "/")).matches()));
    }

    @Test
    public void testGenerateOnly_oneRepo() throws Exception
    {
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, null, new VoidLog());
        Path rootFolder = TMP.newFolder().toPath();
        Path sourcesDir = Files.createDirectories(rootFolder.resolve("src").resolve("java"));
        Assert.assertEquals(Lists.fixedSize.empty(), Files.list(sourcesDir).collect(Collectors.toList()));

        Generate generate = generator.generateOnly("test", true, sourcesDir);
        Assert.assertEquals(Lists.fixedSize.with("test"), generate.getJavaSourcesByGroup().keysView().toList());
        Assert.assertNotEquals(Lists.immutable.empty(), generate.getJavaSourcesByGroup().get("test"));

        List<Path> files = Files.walk(sourcesDir).filter(Files::isRegularFile).collect(Collectors.toList());
        Assert.assertNotEquals(Lists.fixedSize.empty(), files);
        Pattern pattern = Pattern.compile("org/finos/legend/pure/generated/((CoreGen|LambdaZero|PureCompiledLambda|PureEnum(_(LazyImpl|LazyComponent))?)|(test_\\w++(\\$\\w++)?)|(Root_test_\\w++(\\$\\w++)?))\\.java");
        Assert.assertEquals(Lists.fixedSize.empty(), ListIterate.reject(files, f -> pattern.matcher(Iterate.makeString(sourcesDir.relativize(f), "/")).matches()));
        Assert.assertTrue(generate.getJavaSourcesByGroup().get("test").stream().filter(s -> s.toUri().getPath().equals("/org/finos/legend/pure/generated/test_standalone_tests.java")).collect(Collectors.toList()).get(0).getCode().contains("Root_test_standalone_simplePureTest__Boolean_1_"));
    }

    @Test
    public void testPureTestsAreSkipped() throws Exception
    {
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, null, false, new VoidLog());
        Path rootFolder = TMP.newFolder().toPath();
        Path sourcesDir = Files.createDirectories(rootFolder.resolve("src").resolve("java"));
        Generate generate = generator.generateOnly("test", false, sourcesDir);
        Assert.assertFalse(generate.getJavaSourcesByGroup().get("test").stream().filter(s -> s.toUri().getPath().equals("/org/finos/legend/pure/generated/test_standalone_tests.java")).collect(Collectors.toList()).get(0).getCode().contains("Root_test_standalone_simplePureTest__Boolean_1_"));
        Assert.assertTrue(generate.getJavaSourcesByGroup().get("test").stream().filter(s -> s.toUri().getPath().equals("/org/finos/legend/pure/generated/test_standalone_tests.java")).collect(Collectors.toList()).get(0).getCode().contains("Root_test_standalone_simplePureTestWithApplication__Boolean_1_"));
    }

    @Test
    public void testStandaloneLibraryExternalExecutionDistributedMetadata() throws Exception
    {
        String externalPackage = "org.finos.legend.pure.runtime.java.compiled";
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), true, externalPackage, new VoidLog());
        Path classesDir = TMP.newFolder().toPath();
        generator.serializeAndWriteDistributedMetadata(classesDir);
        generator.compileAndWriteClasses(classesDir, new VoidLog());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            testStandaloneLibraryExternalExecution(classLoader, externalPackage);
        }
    }

    @Test
    public void testStandaloneLibraryExternalExecutionPeltMetadata() throws Exception
    {
        String externalPackage = "org.finos.legend.pure.runtime.java.compiled";
        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), true, externalPackage, false, false, new VoidLog());
        Path classesDir = TMP.newFolder().toPath();
        generator.serializeAndWriteMetadata(classesDir);
        generator.compileAndWriteClasses(classesDir, new VoidLog());
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, Thread.currentThread().getContextClassLoader()))
        {
            testStandaloneLibraryExternalExecution(classLoader, externalPackage);
        }
    }

    private void testStandaloneLibraryExternalExecution(ClassLoader classLoader, String externalPackage) throws Exception
    {
        String className = externalPackage + ".PureExternal";
        Class<?> testClass = classLoader.loadClass(className);

        Method joinWithCommas = testClass.getMethod("joinWithCommas", RichIterable.class);
        Assert.assertEquals("a, b, c", joinWithCommas.invoke(null, Lists.immutable.with("a", "b", "c")));

        Method testWithReflection = testClass.getMethod("testWithReflection", String.class);
        Assert.assertEquals("_*_testWithReflection", testWithReflection.invoke(null, "_*_"));
    }
}
