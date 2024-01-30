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

package org.finos.legend.pure.m3.generator.par;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.ParserService;
import org.finos.legend.pure.m3.tools.FileTools;
import org.finos.legend.pure.m3.tools.GeneratorTools;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

public class PureJarGenerator
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PureJarGenerator.class);

    public static void main(String[] args) throws Exception
    {
        MutableSet<String> repositories = Sets.mutable.with(args[1]);
        MutableSet<String> excludedRepositories = Sets.mutable.empty();
        MutableSet<String> extraRepositories = Sets.mutable.empty();
        String purePlatformVersion = args[0];
        String modelVersion = null;
        File sourceDirectory = null;
        File outputDirectory = new File(args[2]);

        Log log = new Log()
        {
            @Override
            public void info(String txt)
            {
                LOGGER.info(txt);
            }

            @Override
            public void error(String txt, Exception e)
            {
                LOGGER.error(txt, e);
            }
        };

        doGeneratePAR(new ParGenerateParams(repositories, excludedRepositories, extraRepositories, purePlatformVersion, modelVersion, sourceDirectory, outputDirectory, log));
    }


    public static void doGeneratePAR(ParGenerateParams parGenerateParams) throws Exception
    {
        long start = System.nanoTime();
        try
        {
            ParserService ps = new ParserService();

            parGenerateParams.getLog().info("Generating Pure PAR file(s)");
            parGenerateParams.getLog().info("  Requested repositories: " + parGenerateParams.getRepositories());
            parGenerateParams.getLog().info("  Excluded repositories: " + parGenerateParams.getExcludedRepositories());
            parGenerateParams.getLog().info("  Extra repositories: " + parGenerateParams.getExtraRepositories());
            CodeRepositorySet resolvedRepositories = resolveRepositories(parGenerateParams.getRepositories(), parGenerateParams.getExcludedRepositories(), parGenerateParams.getExtraRepositories(), parGenerateParams.getLog());
            parGenerateParams.getLog().info("  Specified repositories (with resolved dependencies): " + resolvedRepositories.getRepositories().collect(CodeRepository::getName).makeString("[", ",", "]"));
            parGenerateParams.getLog().info("  Register DSLs: " + ps.parsers().collect(Parser::getName).makeString(", "));
            parGenerateParams.getLog().info("  Register in-line DSLs: " + ps.inlineDSLs().collect(InlineDSL::getName).makeString(", "));
            parGenerateParams.getLog().info("  Pure platform version: " + parGenerateParams.getPurePlatformVersion());
            parGenerateParams.getLog().info("  Model version: " + parGenerateParams.getModelVersion());
            parGenerateParams.getLog().info("  Pure source directory: " + parGenerateParams.getSourceDirectory());
            parGenerateParams.getLog().info("  Output directory: " + parGenerateParams.getOutputDirectory());

            parGenerateParams.getLog().info("  Checking code last modified timestamp");
            CompositeCodeStorage codeStorage;
            if (parGenerateParams.getSourceDirectory() == null)
            {
                codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(Thread.currentThread().getContextClassLoader(), resolvedRepositories.getRepositories()));
            }
            else
            {
                MutableList<CodeRepository> repositoriesWithSource = Lists.mutable.empty();
                MutableList<CodeRepository> repositoriesWithoutSource = Lists.mutable.empty();
                resolvedRepositories.getRepositories().forEach(r -> (Files.exists(parGenerateParams.getSourceDirectory().toPath().resolve(r.getName())) ? repositoriesWithSource : repositoriesWithoutSource).add(r));

                // Code Storages
                MutableList<RepositoryCodeStorage> codeStoragesFromSource = repositoriesWithSource.collect(r -> new MutableFSCodeStorage(r, parGenerateParams.getSourceDirectory().toPath().resolve(r.getName())));
                MutableList<RepositoryCodeStorage> allCodeStorage = Lists.mutable.withAll(codeStoragesFromSource).with(new ClassLoaderCodeStorage(repositoriesWithoutSource));
                parGenerateParams.getLog().info("    *Loading the following repo from PARs: " + repositoriesWithoutSource.collect(CodeRepository::getName));

                // Build the runtime
                codeStorage = new CompositeCodeStorage(allCodeStorage.toArray(new RepositoryCodeStorage[0]));
            }
            long codeLastModified = codeStorage.lastModified();
            parGenerateParams.getLog().info("  Code repositories last modified: " + new Date(codeLastModified));

            Optional<Path> oldestOutputFile = FileTools.findOldestModified(parGenerateParams.getOutputDirectory().toPath(), f -> f.toString().endsWith(".par"));

            String modifiedMsgSummary = "oldestOutputFile=" + oldestOutputFile + "; " + (oldestOutputFile.isPresent() ? oldestOutputFile.get().toFile().lastModified() : null) + ";" + "codeLastModified=" + codeLastModified;


            if (GeneratorTools.skipGenerationForNonStaleOutput() && (oldestOutputFile.isPresent() && oldestOutputFile.get().toFile().lastModified() > codeLastModified))
            {
                parGenerateParams.getLog().info("   No changes detected, output is not is not stale - skipping generation (" + modifiedMsgSummary + ")");
            }
            else
            {
                if (oldestOutputFile.isPresent())
                {
                    GeneratorTools.assertRegenerationPermitted(modifiedMsgSummary);
                }

                parGenerateParams.getLog().info("  Changes detected - regenerating the output (" + modifiedMsgSummary + ")");
                parGenerateParams.getLog().info("  Starting compilation and generation of Pure PAR file(s)");
                PureJarSerializer.writePureRepositoryJars(parGenerateParams.getOutputDirectory().toPath(), (parGenerateParams.getSourceDirectory() == null) ? null : parGenerateParams.getSourceDirectory().toPath(), parGenerateParams.getPurePlatformVersion(), parGenerateParams.getModelVersion(), resolvedRepositories, parGenerateParams.getLog());
            }
        }
        catch (Exception e)
        {
            parGenerateParams.getLog().error(String.format("  -> Pure PAR generation failed (%.9fs)", durationSinceInSeconds(start)), e);
            throw e;
        }
        parGenerateParams.getLog().info(String.format("  -> Finished Pure PAR generation in %.9fs", durationSinceInSeconds(start)));
    }

    private static CodeRepositorySet resolveRepositories(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, Log log)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        RichIterable<CodeRepository> cpRepositories = CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true);
        log.info("  Found repositories (in the classpath): " + cpRepositories.collect(CodeRepository::getName).makeString("[", ",", "]"));
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(cpRepositories);
        if (extraRepositories != null)
        {
            extraRepositories.forEach(r -> builder.addCodeRepository(getExtraRepository(classLoader, r)));
        }
        if (excludedRepositories != null)
        {
            builder.withoutCodeRepositories(excludedRepositories);
        }
        CodeRepositorySet newRepositories = builder.build();
        return ((repositories == null) || repositories.isEmpty()) ? newRepositories : newRepositories.subset(repositories);
    }

    private static GenericCodeRepository getExtraRepository(ClassLoader classLoader, String extraRepository)
    {
        // First check if this is a resource
        URL url = classLoader.getResource(extraRepository);
        if (url != null)
        {
            try
            {
                return GenericCodeRepository.build(url);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error loading extra repository \"" + extraRepository + "\" from resource " + url, e);
            }
        }

        // If it's not a resource, assume it is a file path
        try
        {
            return GenericCodeRepository.build(Paths.get(extraRepository));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error loading extra repository \"" + extraRepository + "\"", e);
        }
    }

    private static double durationSinceInSeconds(long startNanos)
    {
        return durationInSeconds(startNanos, System.nanoTime());
    }

    private static double durationInSeconds(long startNanos, long endNanos)
    {
        return (endNanos - startNanos) / 1_000_000_000.0;
    }

    public static class ParGenerateParams
    {
        private final Set<String> repositories;
        private final Set<String> excludedRepositories;
        private final Set<String> extraRepositories;
        private final String purePlatformVersion;
        private final String modelVersion;
        private final File sourceDirectory;
        private final File outputDirectory;
        private final Log log;

        public ParGenerateParams(Set<String> repositories, Set<String> excludedRepositories, Set<String> extraRepositories, String purePlatformVersion, String modelVersion, File sourceDirectory, File outputDirectory, Log log)
        {
            this.repositories = repositories;
            this.excludedRepositories = excludedRepositories;
            this.extraRepositories = extraRepositories;
            this.purePlatformVersion = purePlatformVersion;
            this.modelVersion = modelVersion;
            this.sourceDirectory = sourceDirectory;
            this.outputDirectory = outputDirectory;
            this.log = log;
        }

        public Set<String> getRepositories()
        {
            return repositories;
        }

        public Set<String> getExcludedRepositories()
        {
            return excludedRepositories;
        }

        public Set<String> getExtraRepositories()
        {
            return extraRepositories;
        }

        public String getPurePlatformVersion()
        {
            return purePlatformVersion;
        }

        public String getModelVersion()
        {
            return modelVersion;
        }

        public File getSourceDirectory()
        {
            return sourceDirectory;
        }

        public File getOutputDirectory()
        {
            return outputDirectory;
        }

        public Log getLog()
        {
            return log;
        }
    }
}
