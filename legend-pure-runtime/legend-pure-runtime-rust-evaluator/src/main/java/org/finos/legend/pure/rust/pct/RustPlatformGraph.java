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

package org.finos.legend.pure.rust.pct;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.rust.PureRustEvaluator;

/**
 * The platform graph loaded once per JVM from the {@code .par} archives on
 * the classpath (the recipe the PCT report generators use), and Rust
 * evaluators bound to it.
 */
public final class RustPlatformGraph
{
    private static PureRuntime runtime;

    private RustPlatformGraph()
    {
    }

    /**
     * The shared runtime, loaded on first use.
     *
     * @return the platform runtime
     */
    public static synchronized PureRuntime runtime()
    {
        if (runtime == null)
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
            RichIterable<CodeRepository> codeRepositories = builder.build().getRepositories();
            CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(codeRepositories));
            PureRuntime loaded = new PureRuntimeBuilder(codeStorage).build();
            Message message = new Message("");
            PureRepositoryJarLibrary jarLibrary = SimplePureRepositoryJarLibrary.newLibrary(GraphLoader.findJars(
                    Lists.mutable.withAll(codeRepositories.select(c -> (c.getName() != null) && (c.getName().startsWith("platform") || c.getName().startsWith("core"))).collect(CodeRepository::getName)),
                    classLoader, message));
            GraphLoader loader = new GraphLoader(loaded.getModelRepository(), loaded.getContext(), loaded.getIncrementalCompiler().getParserLibrary(), loaded.getIncrementalCompiler().getDslLibrary(), loaded.getSourceRegistry(), loaded.getURLPatternLibrary(), jarLibrary);
            loader.loadAll(message);
            runtime = loaded;
        }
        return runtime;
    }

    /**
     * @return the processor support of the shared runtime
     */
    public static ProcessorSupport processorSupport()
    {
        return runtime().getProcessorSupport();
    }

    /**
     * @return the model repository of the shared runtime
     */
    public static ModelRepository modelRepository()
    {
        return runtime().getModelRepository();
    }

    /**
     * A fresh Rust evaluator initialised over the shared runtime.
     *
     * @return the evaluator; the caller closes it
     */
    public static PureRustEvaluator newEvaluator()
    {
        PureRustEvaluator evaluator = new PureRustEvaluator();
        evaluator.init(runtime(), new Message(""));
        return evaluator;
    }
}
