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

package org.finos.legend.pure.m3;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.VoidFunctionExecution;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m3.serialization.runtime.VoidPureRuntimeStatus;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.AfterClass;
import org.junit.Assert;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class AbstractPureTestWithCoreCompiled
{
    protected static PureRuntime runtime;
    protected static ModelRepository repository;
    protected static Context context;
    protected static ProcessorSupport processorSupport;
    protected static FunctionExecution functionExecution;
    protected static RichIterable<? extends CodeRepository> codeRepositories;

    public CodeRepository getRepositoryByName(String name)
    {
        return codeRepositories.select(c -> c.getName().equals(name)).getFirst();
    }

    public static final void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra(), getCodeRepositories());
    }

    public static final void setUpRuntime(FunctionExecution execution)
    {
        setUpRuntime(execution, getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra(), getCodeRepositories());
    }

    public static final void setUpRuntime(Pair<String, String> extra)
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), extra, getCodeRepositories());
    }

    public static final void setUpRuntime(FunctionExecution execution, CoreInstanceFactoryRegistry registry)
    {
        setUpRuntime(execution, getCodeStorage(), registry, getOptions(), getExtra(), getCodeRepositories());
    }

    public static final void setUpRuntime(FunctionExecution execution, Pair<String, String> extra)
    {
        setUpRuntime(execution, getCodeStorage(), getFactoryRegistryOverride(), getOptions(), extra, getCodeRepositories());
    }

    public static final void setUpRuntime(FunctionExecution execution, RuntimeOptions options)
    {
        setUpRuntime(execution, getCodeStorage(), getFactoryRegistryOverride(), options, getExtra(), getCodeRepositories());
    }

    public static final void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, RichIterable<? extends CodeRepository> repositories)
    {
        setUpRuntime(execution, codeStorage, getFactoryRegistryOverride(), getOptions(), getExtra(), repositories);
    }

    public static final void setUpRuntime(MutableCodeStorage codeStorage, RichIterable<? extends CodeRepository> repositories, Pair<String, String> extra)
    {
        setUpRuntime(getFunctionExecution(), codeStorage, getFactoryRegistryOverride(), getOptions(), extra, repositories);
    }


//    @Before
    public static final void setUpRuntime(FunctionExecution execution, MutableCodeStorage codeStorage, CoreInstanceFactoryRegistry registry, RuntimeOptions options, Pair<String, String> extra, RichIterable<? extends CodeRepository> repositories)
    {
        codeRepositories = repositories;
        functionExecution = execution;
        runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(getPureRuntimeStatus())
                .withFactoryRegistryOverride(registry).setTransactionalByDefault(isTransactionalByDefault())
                .withOptions(options)
                .build();
        functionExecution.init(runtime, new Message(""));
        runtime.loadAndCompileCore();
        if (extra != null)
        {
            runtime.createInMemoryAndCompile(extra);
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
    public static final void tearDownRuntime()
    {
        if (runtime != null)
        {
            runtime.reset();
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
        return PureCodeStorage.createCodeStorage(getCodeStorageRoot(), Lists.immutable.with(CodeRepository.newPlatformCodeRepository()));
    }

    protected static Path getCodeStorageRoot()
    {
        return Paths.get("..", "pure-code", "local");
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository());
    }

    protected static CoreInstanceFactoryRegistry getFactoryRegistryOverride()
    {
        return null;
    }

    protected static RuntimeOptions getOptions()
    {
        return new RuntimeOptions()
        {
            @Override
            public boolean isOptionSet(String name)
            {
                return false;
            }
        };
    }

    /**
     * Compile test source from a named resource.
     *
     * @param resourceName resource name
     */
    protected void compileTestSourceFromResource(String resourceName)
    {
        InputStream inStream = getClass().getResourceAsStream(resourceName);
        if (inStream == null)
        {
            inStream = getClass().getClassLoader().getResourceAsStream(resourceName);
            if (inStream == null)
            {
                throw new RuntimeException("Could not find resource: " + resourceName);
            }
        }
        Scanner srcScanner = new Scanner(inStream, "UTF-8").useDelimiter("\\A");
        compileTestSource(resourceName, srcScanner.next());
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
        return this.compileTestSource(sourceId, source);
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
        this.runtime.compile();
        return this.execute(functionIdOrDescriptor, parameters);
    }

    protected CoreInstance execute(String functionIdOrDescriptor, CoreInstance... parameters)
    {
        CoreInstance function = this.runtime.getFunction(functionIdOrDescriptor);
        if (function == null)
        {
            throw new RuntimeException("The function '" + functionIdOrDescriptor + "' can't be found");
        }
        this.functionExecution.getConsole().clear();
        return this.functionExecution.start(function, ArrayAdapter.adapt(parameters));
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
        return Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of();
    }

    protected void assertRepoExists(String repositoryName)
    {
        Assert.assertNotNull("This test relies on the " + repositoryName + " repository", codeRepositories.select(c->c.getName().equals(repositoryName)).getFirst());
    }

}
