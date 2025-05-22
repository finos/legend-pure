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

package org.finos.legend.pure.runtime.java.compiled.runtime.serialization;

import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.MemoryGraphLoaderPureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.cache.MemoryPureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.cache.PureGraphCache;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

public class TestGraphIsSerialized
{
    @Test
    public void testNormalCompilation()
    {
        PureRuntime runtime = new PureRuntimeBuilder(getCodeStorage()).build();
        FunctionExecutionCompiled funcExec = new FunctionExecutionCompiledBuilder().build();
        funcExec.init(runtime, new Message(""));
        runtime.loadAndCompileCore();
        runtime.loadAndCompileSystem();
        assertMetadataAccess(funcExec);
    }

    @Test
    public void testInitializedFromM4Serialization()
    {
        PureRuntime runtime = new PureRuntimeBuilder(getCodeStorage()).buildAndInitialize();

        PureGraphCache cache = new MemoryPureGraphCache();
        cache.setPureRuntime(runtime);
        cache.cacheRepoAndSources();

        runtime = new PureRuntimeBuilder(getCodeStorage()).withCache(cache).buildAndTryToInitializeFromCache();
        FunctionExecutionCompiled funcExec = new FunctionExecutionCompiledBuilder().build();
        funcExec.init(runtime, new Message(""));
        Assert.assertTrue(cache.getCacheState().getLastStackTrace(), runtime.isInitialized());
        assertMetadataAccess(funcExec);
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
        FunctionExecutionCompiled funcExec = new FunctionExecutionCompiledBuilder().build();
        funcExec.init(runtime, new Message(""));
        Assert.assertTrue(cache.getCacheState().getLastStackTrace(), runtime.isInitialized());
        assertMetadataAccess(funcExec);
    }

    private void assertMetadataAccess(FunctionExecutionCompiled functionExecution)
    {
        PureRuntime runtime = functionExecution.getRuntime();
        Metadata metadata = functionExecution.getExecutionSupport().getMetadata();
        String defaultIdPrefix = "###default###";
        IdBuilder idBuilder = IdBuilder.newIdBuilder(defaultIdPrefix, runtime.getProcessorSupport());
        GraphNodeIterable.fromModelRepository(runtime.getModelRepository()).forEach(elt ->
        {
            String id = idBuilder.buildId(elt);
            if (!id.startsWith(defaultIdPrefix))
            {
                CoreInstance found;
                try
                {
                    found = metadata.getMetadata(PackageableElement.getUserPathForPackageableElement(elt.getClassifier()), id);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error getting metadata for id: '" + id + "'", e);
                }
                Assert.assertSame(id, elt, found);
            }
        });
    }

    private MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()));
    }
}
