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

package org.finos.legend.pure.runtime.java.compiled.generation.orchestrator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates java source code from the class path
 */
public class JavaModelFactoryGenerator
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("Generating Java Factory for Parser");
        String name = args[0];
        String parserClass = args[1];
        Path output = Paths.get(args[2]);
        System.out.println("   name:"+name);
        System.out.println("   parser class:"+parserClass);
        System.out.println("   output:"+output);
        MutableList<String> parsers = Lists.mutable.with(parserClass.replaceAll("\\n","").replaceAll("\\r","").split(","));
        MutableSet<String> allTypes = Sets.mutable.empty();
        parsers.forEach(p ->
        {
            try
            {
                Object parser = Class.forName(p.trim()).getConstructor().newInstance();
                if (parser instanceof Parser)
                {
                    allTypes.addAll(((Parser)parser).getCoreInstanceFactoriesRegistry().flatCollect(CoreInstanceFactoryRegistry::allManagedTypes).toSet());
                }
                else
                {
                    allTypes.addAll(((InlineDSL)parser).getCoreInstanceFactoriesRegistry().flatCollect(CoreInstanceFactoryRegistry::allManagedTypes).toSet());
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });


        System.out.println("   managed types: "+allTypes);
        gen(System.nanoTime(), output, name, allTypes);
        System.out.println("Finished generating Java Factory");
    }

    public static void gen(long start, Path filePath, String name, MutableSet<String> allTypes)
    {
        RichIterable<CodeRepository> repositoriesForCompilation = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories())
                .build().getRepositories();
        System.out.println("   Repositories: " + repositoriesForCompilation.collect(CodeRepository::getName).makeString("[", ", ", "]"));
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
                    System.out.println("      Cache initialization failure: " + lastStackTrace);
                }
            }
            System.out.println("      Initialization from caches failed - compiling from scratch");
            runtime.reset();
            runtime.loadAndCompileCore();
            runtime.loadAndCompileSystem();
        }
        System.out.format("      Finished Pure initialization (%.6fs)%n", (System.nanoTime() - start) / 1_000_000_000.0);

        JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(runtime.getProcessorSupport(), runtime.getCodeStorage(), true, filePath, false, Sets.mutable.empty(), name, JavaPackageAndImportBuilder.externalizablePackage(), true);
        javaSourceCodeGenerator.generateCode(allTypes);
    }
}
