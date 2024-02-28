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

package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class PureTestBuilderInterpreted
{
    public static TestSuite buildSuite(String... all)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        RichIterable<CodeRepository> codeRepositories = builder.build().getRepositories();
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(codeRepositories));
        FunctionExecutionInterpreted functionExecution = new FunctionExecutionInterpreted();
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).build();
        Message message = new Message("");
        functionExecution.init(runtime, message);
        PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(Lists.mutable.withAll(codeRepositories.select(c -> c.getName() != null && (c.getName().startsWith("platform") || c.getName().startsWith("core"))).collect(CodeRepository::getName)), Thread.currentThread().getContextClassLoader(), message));
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
