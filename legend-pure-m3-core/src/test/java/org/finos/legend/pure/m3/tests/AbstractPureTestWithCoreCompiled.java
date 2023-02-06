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

package org.finos.legend.pure.m3.tests;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.VoidFunctionExecution;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.*;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.AfterClass;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class AbstractPureTestWithCoreCompiled
{
    protected static PureRuntime runtime;
    protected static ModelRepository repository;
    protected static Context context;
    protected static ProcessorSupport processorSupport;
    protected static FunctionExecution functionExecution;

    @Deprecated
    protected static RichIterable<? extends CodeRepository> codeRepositories;

    public CodeRepository getRepositoryByName(String name)
    {
        return runtime.getCodeStorage().getRepository(name);
    }

    public static void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(FunctionExecution execution)
    {
        setUpRuntime(execution, getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(FunctionExecution execution, RichIterable<? extends CodeRepository> codeRepositories, CoreInstanceFactoryRegistry registry)
    {
        setUpRuntime(execution, PureCodeStorage.createCodeStorage(getCodeStorageRoot(), codeRepositories), registry, getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(MutableCodeStorage codeStorage)
    {
        setUpRuntime(getFunctionExecution(), codeStorage, getFactoryRegistryOverride(), getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry)
    {
        setUpRuntime(getFunctionExecution(), codeStorage, registry, getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(Pair<String, String> extra)
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, CoreInstanceFactoryRegistry registry)
    {
        setUpRuntime(execution, getCodeStorage(), registry, getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry)
    {
        setUpRuntime(execution, codeStorage, registry, getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry, Pair<String, String> extra)
    {
        setUpRuntime(execution, codeStorage, registry, getOptions(), extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage)
    {
        setUpRuntime(execution, codeStorage, getFactoryRegistryOverride(), getOptions(), getExtra(), true);
    }

    public static void setUpRuntime(FunctionExecution execution, Pair<String, String> extra)
    {
        setUpRuntime(execution, getCodeStorage(), getFactoryRegistryOverride(), getOptions(), extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, RuntimeOptions options, CoreInstanceFactoryRegistry registry)
    {
        setUpRuntime(execution, getCodeStorage(), registry, options, getExtra(), true);
    }

    public static void setUpRuntime(FunctionExecution execution, RuntimeOptions options)
    {
        setUpRuntime(execution, getCodeStorage(), getFactoryRegistryOverride(), options, getExtra(), true);
    }

    public static void setUpRuntime(MutableCodeStorage codeStorage, Pair<String, String> extra)
    {
        setUpRuntime(getFunctionExecution(), codeStorage, getFactoryRegistryOverride(), getOptions(), extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, Pair<String, String> extra)
    {
        setUpRuntime(execution, codeStorage, getFactoryRegistryOverride(), getOptions(), extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, CoreInstanceFactoryRegistry registry, Pair<String, String> extra)
    {
        setUpRuntime(execution, getCodeStorage(), registry, getOptions(), extra, true);
    }

    @Deprecated
    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, RichIterable<? extends CodeRepository> repositories)
    {
        setUpRuntime(execution, codeStorage, getFactoryRegistryOverride(), getOptions(), getExtra(), repositories);
    }

    @Deprecated
    public static void setUpRuntime(MutableCodeStorage codeStorage, RichIterable<? extends CodeRepository> repositories, Pair<String, String> extra)
    {
        setUpRuntime(getFunctionExecution(), codeStorage, getFactoryRegistryOverride(), getOptions(), extra, repositories);
    }

    @Deprecated
    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry, RuntimeOptions options, Pair<String, String> extra, RichIterable<? extends CodeRepository> repositories)
    {
        if ((repositories != null) && !repositories.toSet().equals(codeStorage.getAllRepositories().toSet()))
        {
            throw new IllegalArgumentException("Conflict between specified repositories (" + repositories.collect(CodeRepository::getName) + ") and those from code storage (" + codeStorage.getAllRepoNames() + ")");
        }
        setUpRuntime(execution, codeStorage, registry, options, extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry, RuntimeOptions options, Pair<String, String> extra)
    {
        setUpRuntime(execution, codeStorage, registry, options, extra, true);
    }

    public static void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry, RuntimeOptions options, Pair<String, String> extra, boolean loadFromCache)
    {
        codeRepositories = codeStorage.getAllRepositories();
        functionExecution = execution;
        runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(getPureRuntimeStatus())
                .withFactoryRegistryOverride(registry)
                .setTransactionalByDefault(isTransactionalByDefault())
                .withOptions(options)
                .build();
        Message message = new Message("")
        {
            @Override
            public void setMessage(String message)
            {
//                System.out.println(message);
            }
        };

        functionExecution.init(runtime, message);

        if (loadFromCache)
        {
            PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(Lists.mutable.withAll(codeRepositories.select(c->c.getName().startsWith("platform") || c.getName().startsWith("core")).collect(CodeRepository::getName)), Thread.currentThread().getContextClassLoader(), message));
            GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
            loader.loadAll(message);
        }
        else
        {
            runtime.loadAndCompileCore();
        }

        runtime.loadAndCompileSystem();
        if (extra != null)
        {
            runtime.createInMemoryAndCompile(Lists.immutable.with(extra));
        }
        repository = runtime.getModelRepository();
        context = runtime.getContext();
        processorSupport = functionExecution.getProcessorSupport() == null ? runtime.getProcessorSupport() : functionExecution.getProcessorSupport();
        if (functionExecution.getConsole() != null)
        {
            functionExecution.getConsole().enableBufferLines();
        }
    }

    public static Pair<String, String> getExtra()
    {
        return null;
    }

    @AfterClass
    public static void tearDownRuntime()
    {
        if (runtime != null)
        {
            runtime.reset();
        }
        if (repository != null)
        {
            repository.clear();
        }
        runtime = null;
        repository = null;
        context = null;
        processorSupport = null;
        functionExecution = null;
    }

    protected static boolean isTransactionalByDefault()
    {
        return true;
    }

    /**
     * Get the function execution to be passed to the runtime
     * during set-up.
     *
     * @return function execution
     */
    protected static FunctionExecution getFunctionExecution()
    {
        return VoidFunctionExecution.VOID_FUNCTION_EXECUTION;
    }

    /**
     * Get the Pure runtime status to be passed to the runtime
     * during set-up.
     *
     * @return Pure runtime status
     */
    protected static PureRuntimeStatus getPureRuntimeStatus()
    {
        return VoidPureRuntimeStatus.VOID_PURE_RUNTIME_STATUS;
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        return PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories());
    }

    protected static Path getCodeStorageRoot()
    {
        return Paths.get("..", "pure-code", "local");
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        return builder.build().getRepositories();
    }

    protected static CoreInstanceFactoryRegistry getFactoryRegistryOverride()
    {
        return null;
    }

    protected static RuntimeOptions getOptions()
    {
        return RuntimeOptions.noOptionsSet();
    }

    /**
     * Compile test source from a named resource. If pureSourceName is not specified, resourceName will be used as
     * the Pure source name.
     *
     * @param resourceName   resource which holds the code
     * @param pureSourceName Pure source name to compile with (default to resourceName)
     */
    protected void compileTestSourceFromResource(String resourceName, String pureSourceName)
    {
        String code = readTextResource(resourceName);
        compileTestSource((pureSourceName == null) ? resourceName : pureSourceName, code);
    }

    /**
     * Compile test source from a named resource.
     *
     * @param resourceName resource name
     */
    protected void compileTestSourceFromResource(String resourceName)
    {
        compileTestSourceFromResource(resourceName, null);
    }

    protected static String readTextResource(String resourceName)
    {
        return readTextResource(resourceName, Thread.currentThread().getContextClassLoader());
    }

    protected static String readTextResource(String resourceName, ClassLoader classLoader)
    {
        URL url = classLoader.getResource(resourceName);
        if (url == null)
        {
            throw new RuntimeException("Could not find resource: " + resourceName);
        }
        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
        {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1)
            {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Error reading resource from " + url, e);
        }
    }

    /**
     * Compile the given source with the given id.
     *
     * @param sourceId source id
     * @param source   source code
     */
    protected static SourceMutation compileTestSource(String sourceId, String source)
    {
        return runtime.createInMemoryAndCompile(Tuples.pair(sourceId, source));
    }

    protected SourceMutation compileTestSourceM3(String sourceId, String source)
    {
        return compileTestSource(sourceId, source);
    }

    /**
     * Compile the given source with a randomly generated
     * source id.
     *
     * @param source source code
     */
    protected static void compileTestSource(String source)
    {
        compileTestSource("testSource_" + UUID.randomUUID().toString().replace('-', '_') + CodeStorage.PURE_FILE_EXTENSION, source);
    }

    protected void compileTestSourceM3(String source)
    {
        compileTestSourceM3("testSource_" + UUID.randomUUID().toString().replace('-', '_') + CodeStorage.PURE_FILE_EXTENSION, source);
    }

    protected CoreInstance compileAndExecute(String functionIdOrDescriptor, CoreInstance... parameters)
    {
        runtime.compile();
        return this.execute(functionIdOrDescriptor, parameters);
    }

    protected CoreInstance execute(String functionIdOrDescriptor, CoreInstance... parameters)
    {
        CoreInstance function = runtime.getFunction(functionIdOrDescriptor);
        if (function == null)
        {
            throw new RuntimeException("The function '" + functionIdOrDescriptor + "' can't be found");
        }
        functionExecution.getConsole().clear();
        return functionExecution.start(function, ArrayAdapter.adapt(parameters));
    }

    // assertOriginatingPureException variants

    protected void assertOriginatingPureException(String expectedInfo, Exception exception)
    {
        assertOriginatingPureException(expectedInfo, null, null, exception);
    }

    protected void assertOriginatingPureException(Pattern expectedInfo, Exception exception)
    {
        assertOriginatingPureException(expectedInfo, null, null, exception);
    }

    protected void assertOriginatingPureException(String expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertOriginatingPureException(null, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertOriginatingPureException(Pattern expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertOriginatingPureException(null, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, String expectedInfo, Exception exception)
    {
        assertOriginatingPureException(expectedClass, expectedInfo, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, Exception exception)
    {
        assertOriginatingPureException(expectedClass, expectedInfo, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, String expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertOriginatingPureException(expectedClass, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertOriginatingPureException(expectedClass, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertOriginatingPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertOriginatingPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, Exception exception)
    {
        PureException pe = PureException.findPureException(exception);
        Assert.assertNotNull("No Pure exception", pe);
        assertOriginatingPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, pe);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, Exception exception)
    {
        PureException pe = PureException.findPureException(exception);
        Assert.assertNotNull("No Pure exception", pe);
        assertOriginatingPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, pe);
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, PureException exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, exception.getOriginatingPureException());
    }

    protected void assertOriginatingPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, PureException exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, exception.getOriginatingPureException());
    }

    // assertPureException variants

    protected void assertPureException(String expectedInfo, Exception exception)
    {
        assertPureException(null, expectedInfo, null, null, null, null, null, exception);
    }

    protected void assertPureException(Pattern expectedInfo, Exception exception)
    {
        assertPureException(null, expectedInfo, null, null, null, null, null, exception);
    }

    protected void assertPureException(String expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertPureException(null, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertPureException(Pattern expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertPureException(null, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, null, null, null, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, null, null, null, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, null, null, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, null, null, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, null, expectedLine, expectedColumn, null, null, exception);
    }

    protected static void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, null, null, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Exception exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, null, null, exception);
    }

    protected static void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, Exception exception)
    {
        PureException pe = PureException.findPureException(exception);
        Assert.assertNotNull("No Pure exception", pe);
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, pe);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, Exception exception)
    {
        PureException pe = PureException.findPureException(exception);
        Assert.assertNotNull("No Pure exception", pe);
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, pe);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedStartLine, Integer expectedStartColumn, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, Exception exception)
    {
        PureException pe = PureException.findPureException(exception);
        Assert.assertNotNull("No Pure exception", pe);
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedStartLine, expectedStartColumn, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, pe);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedStartLine, Integer expectedStartColumn, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, Exception exception)
    {
        PureException pe = PureException.findPureException(exception);
        Assert.assertNotNull("No Pure exception", pe);
        assertPureException(expectedClass, expectedInfo, expectedSource, expectedStartLine, expectedStartColumn, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, pe);
    }

    protected static void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, PureException exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, null, null, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, exception);
    }

    protected void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, PureException exception)
    {
        assertPureException(expectedClass, expectedInfo, expectedSource, null, null, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, exception);
    }

    protected static void assertPureException(Class<? extends PureException> expectedClass, String expectedInfo, String expectedSource, Integer expectedStartLine, Integer expectedStartColumn, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, PureException exception)
    {
        // Check class
        if (expectedClass != null)
        {
            Assert.assertTrue("Expected an exception of type " + expectedClass.getCanonicalName() + ", got: " + exception.getClass().getCanonicalName() + " message:" + exception.getMessage(),
                    expectedClass.isInstance(exception));
        }

        // Check info
        if (expectedInfo != null)
        {
            Assert.assertEquals(expectedInfo, exception.getInfo());
        }

        // Check source information
        assertSourceInformation(expectedSource, expectedStartLine, expectedStartColumn, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, exception.getSourceInformation());
    }

    protected static void assertPureException(Class<? extends PureException> expectedClass, Pattern expectedInfo, String expectedSource, Integer expectedStartLine, Integer expectedStartColumn, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, PureException exception)
    {
        // Check class
        if (expectedClass != null)
        {
            Assert.assertTrue("Expected an exception of type " + expectedClass.getCanonicalName() + ", got: " + exception.getClass().getCanonicalName(),
                    expectedClass.isInstance(exception));
        }

        // Check info
        if ((expectedInfo != null) && !expectedInfo.matcher(exception.getInfo()).matches())
        {
            Assert.fail("Expected exception info matching:\n\t" + expectedInfo.pattern() + "\ngot:\n\t" + exception.getInfo());
        }

        // Check source information
        assertSourceInformation(expectedSource, expectedStartLine, expectedStartColumn, expectedLine, expectedColumn, expectedEndLine, expectedEndColumn, exception.getSourceInformation());
    }

    protected static void assertSourceInformation(String expectedSource, Integer expectedStartLine, Integer expectedStartColumn, Integer expectedLine, Integer expectedColumn, Integer expectedEndLine, Integer expectedEndColumn, SourceInformation sourceInfo)
    {
        if (expectedSource != null)
        {
            Assert.assertEquals("Wrong source", expectedSource, sourceInfo.getSourceId());
        }
        if (expectedStartLine != null)
        {
            Assert.assertEquals("Wrong start line", expectedStartLine.intValue(), sourceInfo.getStartLine());
        }
        if (expectedStartColumn != null)
        {
            Assert.assertEquals("Wrong start column", expectedStartColumn.intValue(), sourceInfo.getStartColumn());
        }
        if (expectedLine != null)
        {
            Assert.assertEquals("Wrong line", expectedLine.intValue(), sourceInfo.getLine());
        }
        if (expectedColumn != null)
        {
            Assert.assertEquals("Wrong column", expectedColumn.intValue(), sourceInfo.getColumn());
        }
        if (expectedEndLine != null)
        {
            Assert.assertEquals("Wrong end line", expectedEndLine.intValue(), sourceInfo.getEndLine());
        }
        if (expectedEndColumn != null)
        {
            Assert.assertEquals("Wrong end column", expectedEndColumn.intValue(), sourceInfo.getEndColumn());
        }
    }

    protected ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getAdditionalVerifiers()
    {
        return Lists.fixedSize.empty();
    }

    protected void assertRepoExists(String repositoryName)
    {
        Assert.assertNotNull("This test relies on the " + repositoryName + " repository", getRepositoryByName(repositoryName));
    }
}
