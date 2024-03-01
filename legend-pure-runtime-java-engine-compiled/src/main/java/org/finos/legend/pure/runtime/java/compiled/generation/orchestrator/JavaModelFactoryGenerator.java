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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.m3.tools.FileTools;
import org.finos.legend.pure.m3.tools.GeneratorTools;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Generates java source code from the class path
 */
public class JavaModelFactoryGenerator
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JavaModelFactoryGenerator.class);

    public static void main(String... args) throws Exception
    {
        LOGGER.info("Starting " + JavaModelFactoryGenerator.class.getSimpleName() + " execution");

        LOGGER.info("Generating Java Factory for Parser");

        String name = args[0];
        String parserClass = args[1];
        Path output = Paths.get(args[2]);

        LOGGER.info("   name:" + name);
        LOGGER.info("   parser class:" + parserClass);
        LOGGER.info("   output:" + output);

        MutableList<String> parsers = Lists.mutable.with(parserClass.replaceAll("\\n", "").replaceAll("\\r", "").split(","));
        MutableSet<String> allTypes = Sets.mutable.empty();
        parsers.forEach(p ->
        {
            try
            {
                Object parser = Class.forName(p.trim()).getConstructor().newInstance();
                if (parser instanceof Parser)
                {
                    allTypes.addAll(((Parser) parser).getCoreInstanceFactoriesRegistry().flatCollect(CoreInstanceFactoryRegistry::allManagedTypes).toSet());
                }
                else
                {
                    allTypes.addAll(((InlineDSL) parser).getCoreInstanceFactoriesRegistry().flatCollect(CoreInstanceFactoryRegistry::allManagedTypes).toSet());
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });


        LOGGER.info("   managed types: " + allTypes);
        gen(System.nanoTime(), output, name, allTypes);
        LOGGER.info("Finished generating Java Factory");
    }

    public static void gen(long start, Path filePath, String name, MutableSet<String> allTypes)
    {
        RichIterable<CodeRepository> repositoriesForCompilation = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories())
                .build().getRepositories();
        LOGGER.info("   Repositories: " + repositoriesForCompilation.collect(CodeRepository::getName).makeString("[", ", ", "]"));
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(repositoriesForCompilation));

        long codeStorageLastModified = codeStorage.lastModified();
        Optional<Path> outputEarliestModified;
        try
        {
            outputEarliestModified = FileTools.findOldestModified(filePath);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String modifiedMsgSummary = "outputEarliestModified=" + outputEarliestModified + "; " + (outputEarliestModified.isPresent() ? outputEarliestModified.get().toFile().lastModified() : null) + ";" + "codeStorageLastModified=" + codeStorageLastModified;

        if (GeneratorTools.skipGenerationForNonStaleOutput() && (outputEarliestModified.isPresent() && outputEarliestModified.get().toFile().lastModified() > codeStorageLastModified))
        {
            LOGGER.info("      No changes detected, output is not is not stale - skipping generation (" + modifiedMsgSummary + ")");
        }
        else
        {
            if (outputEarliestModified.isPresent())
            {
                GeneratorTools.assertRegenerationPermitted(modifiedMsgSummary);
            }

            LOGGER.info("      Changes detected - regenerating the output (" + modifiedMsgSummary + ")");

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
                        LOGGER.info("      Cache initialization failure: " + lastStackTrace);
                    }
                }
                LOGGER.info("      Initialization from caches failed - compiling from scratch");
                runtime.reset();
                runtime.loadAndCompileCore();
                runtime.loadAndCompileSystem();
            }
            LOGGER.info(String.format("      Finished Pure initialization (%.6fs)%n", (System.nanoTime() - start) / 1_000_000_000.0));

            JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(runtime.getProcessorSupport(), runtime.getCodeStorage(), true, filePath, false, Sets.mutable.empty(), name, JavaPackageAndImportBuilder.externalizablePackage(), true);
            javaSourceCodeGenerator.generateCode(allTypes);
        }
    }
}
