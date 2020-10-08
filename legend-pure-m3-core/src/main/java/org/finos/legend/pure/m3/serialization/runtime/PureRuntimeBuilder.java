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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.cache.PureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.cache.VoidPureGraphCache;

import java.util.concurrent.ForkJoinPool;

/**
 * Builds a pure runtime
 */
public class PureRuntimeBuilder
{
    private final MutableCodeStorage codeStorage;
    private PureGraphCache cache = VoidPureGraphCache.VOID_PURE_GRAPH_CACHE;
    private PureRuntimeStatus pureRuntimeStatus = VoidPureRuntimeStatus.VOID_PURE_RUNTIME_STATUS;
    private Message message = new Message("");
    private final MutableList<Function<PureRuntime, CompilerEventHandler>> compilerEventHandlerFactoryFunctions = Lists.mutable.empty();
    private CoreInstanceFactoryRegistry factoryRegistryOverride;
    private ForkJoinPool incrementalCompilerForkJoinPool;
    private boolean isTransactionalByDefault = true;
    private boolean useFastCompiler = true;
    private ExecutedTestTracker executedTestTracker;
    private RuntimeOptions options;

    public PureRuntimeBuilder(MutableCodeStorage codeStorage)
    {
        this.codeStorage = codeStorage;

        this.options = new RuntimeOptions()
        {
            @Override
            public boolean isOptionSet(String name)
            {
                String propertyName = "pure.option." + name;
                return System.getProperty(propertyName) != null && Boolean.getBoolean(propertyName);
            }
        };
    }

    public PureRuntimeBuilder withCache(PureGraphCache cache)
    {
        this.cache = cache;
        return this;
    }

    public PureRuntimeBuilder withRuntimeStatus(PureRuntimeStatus pureRuntimeStatus)
    {
        this.pureRuntimeStatus = pureRuntimeStatus;
        return this;
    }

    public PureRuntimeBuilder withMessage(Message message)
    {
        this.message = message;
        return this;
    }

    public PureRuntimeBuilder withCompilerEventHandler(Function<PureRuntime, CompilerEventHandler> compilerEventHandlerFactoryFunction)
    {
        this.compilerEventHandlerFactoryFunctions.add(compilerEventHandlerFactoryFunction);
        return this;
    }

    public PureRuntimeBuilder withFactoryRegistryOverride(CoreInstanceFactoryRegistry factoryRegistryOverride)
    {
        this.factoryRegistryOverride = factoryRegistryOverride;
        return this;
    }

    public PureRuntimeBuilder withIncrementalCompilerForkJoinPool(ForkJoinPool forkJoinPool)
    {
        this.incrementalCompilerForkJoinPool = forkJoinPool;
        return this;
    }

    public PureRuntimeBuilder setTransactionalByDefault(boolean isTransactionalByDefault)
    {
        this.isTransactionalByDefault = isTransactionalByDefault;
        return this;
    }


    public PureRuntimeBuilder setUseFastCompiler(boolean useFastCompiler)
    {
        this.useFastCompiler = useFastCompiler;
        return this;
    }

    public PureRuntimeBuilder withExecutedTestTracker(ExecutedTestTracker executedTestTracker)
    {
        this.executedTestTracker = executedTestTracker;
        return this;
    }

    public PureRuntimeBuilder withOptions(RuntimeOptions options)
    {
        this.options = options;
        return this;
    }

    public PureRuntime build()
    {
        PureRuntime runtime = new PureRuntime(this.codeStorage, this.cache, this.pureRuntimeStatus, this.message, this.factoryRegistryOverride,
                                              this.incrementalCompilerForkJoinPool, this.isTransactionalByDefault, this.useFastCompiler, this.executedTestTracker, this.options);

        for (Function<PureRuntime, CompilerEventHandler> compilerEventHandlerFunction : this.compilerEventHandlerFactoryFunctions)
        {
            CompilerEventHandler compilerEventHandler = compilerEventHandlerFunction.valueOf(runtime);
            runtime.getIncrementalCompiler().addCompilerEventHandler(compilerEventHandler);
        }

        return runtime;
    }

    public PureRuntime buildAndInitialize()
    {
        PureRuntime runtime = this.build();
        runtime.initialize(this.message);
        return runtime;
    }

    public PureRuntime buildAndTryToInitializeFromCache()
    {
        PureRuntime runtime = this.build();
        try
        {
            runtime.initializeFromCache(this.message, false);
        }
        catch (Exception e)
        {
            // Ignore exceptions so that even if initialization fails, the PureRuntime will still be created
        }
        return runtime;
    }
}
