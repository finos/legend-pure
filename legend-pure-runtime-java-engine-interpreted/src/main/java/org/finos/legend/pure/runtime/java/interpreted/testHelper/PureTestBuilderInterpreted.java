package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import java.nio.file.Paths;

public class PureTestBuilderInterpreted
{
    public static TestSuite buildSuite(String... all)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        RichIterable<CodeRepository> codeRepositories = builder.build().getRepositories();
        PureCodeStorage codeStorage = PureCodeStorage.createCodeStorage(Paths.get("..", "pure-code", "local"), codeRepositories);
        FunctionExecutionInterpreted functionExecution = new FunctionExecutionInterpreted();
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).build();
        Message message = new Message("");
        functionExecution.init(runtime, message);
        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(Lists.mutable.withAll(codeRepositories.select(c -> c.getName().startsWith("platform") || c.getName().startsWith("core")).collect(CodeRepository::getName)), Thread.currentThread().getContextClassLoader(), message));
        GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), runtime.getURLPatternLibrary(), jarLibrary);
        loader.loadAll(message);
        ExecutionSupport executionSupport = new ExecutionSupport();

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> p = (a, b) -> functionExecution.start(a, Lists.mutable.empty());
        TestSuite suite = new TestSuite();
        ArrayIterate.forEach(all, (path) ->
                {
                    TestCollection col = TestCollection.collectTests(path, functionExecution.getProcessorSupport(), ci -> PureTestBuilder.satisfiesConditionsInterpreted(ci, functionExecution.getProcessorSupport()));
                    suite.addTest(PureTestBuilder.buildSuite(col, p, executionSupport));
                }
        );
        return suite;
    }
}
