package org.finos.legend.pure.runtime.java.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.EmptyCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaStandaloneLibraryGenerator;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class TestJavaStandaloneLibraryGenerator extends AbstractPureTestWithCoreCompiled
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUp()
    {

        MutableCodeStorage codeStorage = new PureCodeStorage(null,
                new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()),
                new EmptyCodeStorage(new GenericCodeRepository("test", "test::.*", PlatformCodeRepository.NAME)));
        setUpRuntime(codeStorage, codeStorage.getAllRepositories(), Tuples.pair(
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
                        "function <<access.externalizable>> test::standalone::testWithReflection(prefix:String[1]):String[1]\n" +
                        "{\n" +
                        "    let f = testWithReflection_String_1__String_1_;\n" +
                        "    let class = TestClassA;\n" +
                        "    let association = TestAssociation;\n" +
                        "    let b = ^TestClassB(id=43, type=TestEnumeration.TYPE3, toA=^TestClassA(name=$prefix));\n" +
                        "    if($class == $association, |'ERROR', |$b.toA.name + $f.functionName->toOne());\n" +
                        "}\n"
        ));
    }

    @Test
    public void testStandaloneLibraryNoExternal() throws Exception
    {
        String metadataName = "test_metadata_name";

        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, null);
        Path classesDir = this.temporaryFolder.newFolder("classes").toPath();
        generator.serializeAndWriteDistributedMetadata(metadataName, classesDir);
        generator.compileAndWriteClasses(classesDir);
        URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, Thread.currentThread().getContextClassLoader());

        MetadataLazy metadataLazy = MetadataLazy.fromClassLoader(classLoader, metadataName);
        CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(
                new JavaCompilerState(null, classLoader),
                new CompiledProcessorSupport(classLoader, metadataLazy, null),
                null,
                runtime.getCodeStorage(),
                null,
                VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                null
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
}
