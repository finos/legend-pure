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

package org.finos.legend.pure.runtime.java.compiled.serialization;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.MemoryGraphLoaderPureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.cache.MemoryPureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.cache.PureGraphCache;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Formatter;

public class TestGraphIsSerialized
{
    @Test
    public void testNormalCompilation()
    {
        PureRuntime runtime = new PureRuntimeBuilder(getCodeStorage()).build();
        new FunctionExecutionCompiledBuilder().build().init(runtime, new Message(""));
        runtime.loadAndCompileCore();
        runtime.loadAndCompileSystem();
        assertAllInstancesMarkedSerialized(runtime);
    }

    @Test
    public void testInitializedFromM4Serialization()
    {
        PureRuntime runtime = new PureRuntimeBuilder(getCodeStorage()).buildAndInitialize();

        PureGraphCache cache = new MemoryPureGraphCache();
        cache.setPureRuntime(runtime);
        cache.cacheRepoAndSources();

        runtime = new PureRuntimeBuilder(getCodeStorage()).withCache(cache).buildAndTryToInitializeFromCache();
        new FunctionExecutionCompiledBuilder().build().init(runtime, new Message(""));
        Assert.assertTrue(cache.getCacheState().getLastStackTrace(), runtime.isInitialized());
        assertAllInstancesMarkedSerialized(runtime);
    }

    @Test
    public void testInitializedFromGraphLoaderSerialization()
    {
        PureRuntime runtime = new PureRuntimeBuilder(getCodeStorage()).buildAndInitialize();

        PureGraphCache cache = new MemoryGraphLoaderPureGraphCache();
        cache.setPureRuntime(runtime);
        cache.cacheRepoAndSources();

        runtime = new PureRuntimeBuilder(getCodeStorage())
                .withCache(cache).buildAndTryToInitializeFromCache();
        new FunctionExecutionCompiledBuilder().build().init(runtime, new Message(""));
        Assert.assertTrue(cache.getCacheState().getLastStackTrace(), runtime.isInitialized());
        assertAllInstancesMarkedSerialized(runtime);
    }

    private void assertAllInstancesMarkedSerialized(final PureRuntime runtime)
    {
        SetIterable<CoreInstance> ignoredClassifiers = ModelRepository.PRIMITIVE_TYPE_NAMES.newWithAll(Lists.immutable.with(M3Paths.ImportStub, M3Paths.EnumStub, M3Paths.PropertyStub)).collect(runtime::getCoreInstance);
        Predicate<CoreInstance> isAppropriatelySerialized = instance -> instance.hasCompileState(MetadataBuilder.SERIALIZED) || ignoredClassifiers.contains(instance.getClassifier());
        MutableList<CoreInstance> missing = Iterate.reject(GraphNodeIterable.fromModelRepository(runtime.getModelRepository()), isAppropriatelySerialized, Lists.mutable.empty());
        int missingSize = missing.size();
        if (missingSize > 0)
        {
            StringBuilder message = new StringBuilder();
            Formatter formatter = new Formatter(message);
            formatter.format("%,d instances not marked as serialized:", missingSize);
            int toPrint = Math.min(missingSize, 10);
            int i = 0;
            for (CoreInstance instance : missing)
            {
                formatter.format("%n\t%s", instance);
                i++;
                if (i >= toPrint)
                {
                    break;
                }
            }
            if (toPrint < missingSize)
            {
                formatter.format("%n\t...");
            }
            Assert.fail(message.toString());
        }
    }

    private MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()));
    }
}
