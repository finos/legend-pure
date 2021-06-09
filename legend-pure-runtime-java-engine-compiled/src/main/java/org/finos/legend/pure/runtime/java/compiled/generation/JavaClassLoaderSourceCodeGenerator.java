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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates java source code from the class path
 */
public class JavaClassLoaderSourceCodeGenerator
{
    public static void main(String[] args) throws Exception
    {
        MutableList<CompiledExtension> extensions = 1 < args.length && args[1] != null ? ArrayAdapter.adapt(args[1].split(",")).collect(new CheckedFunction<String, CompiledExtension>(){
            @Override
            public CompiledExtension safeValueOf(String v) throws Exception
            {
                return (CompiledExtension)Class.forName(v.trim()).getMethod("extension").invoke(null);
            }
        }) : Lists.mutable.empty();
        gen(System.nanoTime(), Paths.get(args[0]), args[2], extensions);
    }

    public static void gen(long start, Path filePath, String name, MutableList<CompiledExtension> extensions)
    {
        RichIterable<CodeRepository> repositoriesForCompilation = Lists.fixedSize.of(CodeRepository.newPlatformCodeRepository());
        PureCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(repositoriesForCompilation));
        ClassLoaderPureGraphCache graphCache = new ClassLoaderPureGraphCache();
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).withCache(graphCache).setTransactionalByDefault(false).buildAndTryToInitializeFromCache();
        if (!runtime.isInitialized())
        {
            CacheState cacheState = graphCache.getCacheState();
            if (cacheState != null)
            {
                String lastStackTrace = cacheState.getLastStackTrace();
                if (lastStackTrace != null)
                {
                    System.out.println("Cache initialization failure: " + lastStackTrace);
                }
            }
            System.out.println("Initialization from caches failed - compiling from scratch");
            runtime.reset();
            runtime.loadAndCompileCore();
            runtime.loadAndCompileSystem();
        }
        System.out.format("Finished Pure initialization (%.6fs)%n", (System.nanoTime() - start) / 1_000_000_000.0);

        JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(runtime.getProcessorSupport(), runtime.getCodeStorage(), true, filePath, false, extensions, name, JavaPackageAndImportBuilder.externalizablePackage());
        javaSourceCodeGenerator.generateCode();
        javaSourceCodeGenerator.generatePureCoreHelperClasses(new ProcessorContext(runtime.getProcessorSupport(), false));
    }
}
