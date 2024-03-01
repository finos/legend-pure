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

package org.finos.legend.pure.runtime.java.compiled.generation.orchestrator;

import org.apache.commons.cli.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.ParserService;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.m3.tools.FileTools;
import org.finos.legend.pure.m3.tools.GeneratorTools;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.Generate;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaStandaloneLibraryGenerator;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class JavaCodeGeneration
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JavaCodeGeneration.class);

    public static void main(String... args)
    {
        LOGGER.info("Starting " + JavaCodeGeneration.class.getSimpleName() + " execution");

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

            @Override
            public void error(String msg)
            {
                LOGGER.error(msg);
            }

            @Override
            public void warn(String s)
            {
                LOGGER.warn(s);
            }
        };

        Set<String> excludedRepositories = Sets.mutable.empty();
        Set<String> extraRepositories = Sets.mutable.empty();
        GenerationType modular = GenerationType.modular;

        // For backwards compatibility
        String externalAPIPackage;
        Set<String> repositories;
        File classesDirectory;
        Path targetDirectoryForSource;
        Path targetDirectoryForMetadata;

        // Define the command line options
        Option repositoryOption = new Option("repo", "repository", true, "repository name, e.g. 'platform'");
        repositoryOption.setRequired(true);
        repositoryOption.setArgName("repositoryName");

        Option targetResourceDirectoryOption = new Option("trd", "targetResourceDir", true, "target output directory for resources");
        targetResourceDirectoryOption.setRequired(true);
        targetResourceDirectoryOption.setArgs(1);
        targetResourceDirectoryOption.setArgName("directory");

        Option targetSourceDirectoryOption = new Option("tsd", "targetSourceDir", true, "target output directory for sources");
        targetSourceDirectoryOption.setRequired(true);
        targetSourceDirectoryOption.setArgs(1);
        targetSourceDirectoryOption.setArgName("directory");

        Option targetMetadataDirectoryOption = new Option("tmd", "targetMetadataDir", true, "target output directory for metadata");
        targetMetadataDirectoryOption.setRequired(true);
        targetMetadataDirectoryOption.setArgs(1);
        targetMetadataDirectoryOption.setArgName("directory");

        Option externalApiPackageNameOption = new Option("eapn", "externalApiPackageName", true, "package name for external api");
        externalApiPackageNameOption.setRequired(false);
        externalApiPackageNameOption.setArgs(1);
        externalApiPackageNameOption.setArgName("packageName");

        // create Options object
        Options options = new Options();
        options.addOption(repositoryOption);
        options.addOption(targetResourceDirectoryOption);
        options.addOption(targetSourceDirectoryOption);
        options.addOption(targetMetadataDirectoryOption);
        options.addOption(externalApiPackageNameOption);

        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        try
        {
            if (args.length > 0 && !Arrays.stream(args).anyMatch(v -> v.startsWith("-")))
            {
                args = new String[]{
                    "-" + repositoryOption.getOpt(),
                    args[0],
                    "-" + targetResourceDirectoryOption.getOpt(),
                    args[1],
                    "-" + targetSourceDirectoryOption.getOpt(),
                    Paths.get(args[2]).resolve("generated-test-sources").toString(),
                    "-" + targetMetadataDirectoryOption.getOpt(),
                    Paths.get(args[2]).resolve("metadata-distributed").toString(),
                    };
            }

            cmd = parser.parse(options, args);

            repositories = new HashSet<>(Arrays.asList(cmd.getOptionValues(repositoryOption.getOpt())));
            classesDirectory = new File(cmd.getOptionValue(targetResourceDirectoryOption.getOpt()));
            targetDirectoryForSource = Paths.get(cmd.getOptionValue(targetSourceDirectoryOption.getOpt()));
            targetDirectoryForMetadata = Paths.get(cmd.getOptionValue(targetMetadataDirectoryOption.getOpt()));
            externalAPIPackage = cmd.getOptionValue(externalApiPackageNameOption.getOpt(), "");
        }
        catch (ParseException e)
        {
            System.out.println(e.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(JavaCodeGeneration.class.getName(), options);

            System.exit(1);
            throw new RuntimeException(e);
        }

        doIt(new GenerateParams(
                repositories,
                excludedRepositories,
                extraRepositories,
                modular,
                false,
                false,
                externalAPIPackage,
                true,
                true,
                true,
                true,
                classesDirectory,
                targetDirectoryForSource,
                targetDirectoryForMetadata,
                log
            ));
    }


    public static void doIt(GenerateParams generateParams)
    {
        // DO NOT DELETE - Needed to avoid circular calls later during static initialization
        SetIterable<String> res = JavaPackageAndImportBuilder.M3_CLASSES;
        // DO NOT DELETE - Needed to avoid circular calls later during static initialization

        Log genLogger = generateParams.getLog();

        if (generateParams.isSkip())
        {
            genLogger.info("Skipping Java Compiled JAR generation");
            return;
        }

        long start = System.nanoTime();
        genLogger.info("Generating Java Compiled JAR");
        genLogger.info("  Requested repositories: " + generateParams.getRepositories());
        genLogger.info("  Excluded repositories: " + generateParams.getExcludedRepositories());
        genLogger.info("  Extra repositories: " + generateParams.getExtraRepositories());
        genLogger.info("  Generation type: " + generateParams.getGenerationType());
        genLogger.info("  Generate External API: addExternalAPI='" + generateParams.isAddExternalAPI() + "', in package '" + generateParams.getExternalAPIPackage() + "'");
        genLogger.info("  classesDirectory: '" + generateParams.getTargetDirectoryForResources());
        genLogger.info("  targetDirectoryForSources: '" + generateParams.getTargetDirectoryForSources());
        genLogger.info("  targetDirectoryForMetadata: '" + generateParams.getTargetDirectoryForMetadata());
        genLogger.info("  Other options: " +
                " skip=" + generateParams.isSkip() +
                "; generateMetadata=" + generateParams.isGenerateMetadata() +
                "; useSingleDir=" + generateParams.isUseSingleDir() +
                "; generateSources=" + generateParams.isGenerateSources()
                );
        genLogger.info("  Working directory: '" + FileSystems.getDefault().getPath("").toAbsolutePath());

        try
        {
            CodeRepositorySet allRepositories = getAllRepositories(generateParams.getExtraRepositories());
            genLogger.info(allRepositories.getRepositories().collect(CodeRepository::getName).makeString("  Found repositories: ", ", ", ""));
            genLogger.info(new ParserService().parsers().collect(Parser::getName).makeString("  Found parsers: ", ", ", ""));
            genLogger.info(new ParserService().inlineDSLs().collect(InlineDSL::getName).makeString("  Found DSL parsers: ", ", ", ""));
            MutableSet<String> selectedRepositories = getSelectedRepositories(allRepositories, generateParams.getRepositories(), generateParams.getExcludedRepositories());
            genLogger.info(selectedRepositories.makeString("  Selected repositories: ", ", ", ""));


            genLogger.info("  Checking code last modified timestamp");
            CompositeCodeStorage codeStorage = new CompositeCodeStorage(
                    new ClassLoaderCodeStorage(Thread.currentThread().getContextClassLoader(), allRepositories.subset(selectedRepositories).getRepositories())
                    );
            long codeLastModified = codeStorage.lastModified();
            genLogger.info("  Code repositories last modified: " + new Date(codeLastModified));

            Path distributedMetadataDirectory;
            if (!generateParams.isGenerateMetadata())
            {
                distributedMetadataDirectory = null;
                genLogger.info("  Classes output directory: " + generateParams.getTargetDirectoryForResources());
                genLogger.info("  No metadata output");
            }
            else if (generateParams.isUseSingleDir())
            {
                distributedMetadataDirectory = generateParams.getTargetDirectoryForResources().toPath();
                genLogger.info("  All in output directory: " + generateParams.getTargetDirectoryForResources());
            }
            else
            {
                distributedMetadataDirectory = generateParams.getTargetDirectoryForMetadata();
                genLogger.info("  Classes output directory: " + generateParams.getTargetDirectoryForResources());
                genLogger.info("  Distributed metadata output directory: " + distributedMetadataDirectory);
            }

            Path codegenDirectory;
            if (generateParams.isGenerateSources())
            {
                codegenDirectory = generateParams.getTargetDirectoryForSources();
                genLogger.info("  Codegen output directory: " + codegenDirectory);
            }
            else
            {
                codegenDirectory = null;
            }


            Optional<Path> oldestOutputFile = FileTools.findOldestModified(
                    distributedMetadataDirectory,
                    codegenDirectory,
                    Paths.get(generateParams.getTargetDirectoryForResources().getAbsolutePath())
                    );

            String modifiedMsgSummary = "oldestOutputFile=" + oldestOutputFile + "; " + (oldestOutputFile.isPresent() ? oldestOutputFile.get().toFile().lastModified() : null) + ";" + "codeLastModified=" + codeLastModified;


            if (GeneratorTools.skipGenerationForNonStaleOutput() && (oldestOutputFile.isPresent() && oldestOutputFile.get().toFile().lastModified() > codeLastModified))
            {
                genLogger.info("  No changes detected, output is not is not stale - skipping generation (" + modifiedMsgSummary + ")");
            }
            else
            {
                if (oldestOutputFile.isPresent())
                {
                    GeneratorTools.assertRegenerationPermitted(modifiedMsgSummary);
                }

                genLogger.info("  Changes detected - regenerating the output (" + modifiedMsgSummary + ")");

                // Generate metadata and Java sources
                Generate generate = generate(
                        System.nanoTime(),
                        allRepositories,
                        selectedRepositories,
                        distributedMetadataDirectory,
                        codegenDirectory,
                        generateParams.isGenerateMetadata(),
                        generateParams.isAddExternalAPI(),
                        generateParams.getExternalAPIPackage(),
                        generateParams.getGenerationType(),
                        generateParams.isGenerateSources(),
                        genLogger
                    );

                // Compile Java sources
                if (!generateParams.isPreventJavaCompilation())
                {
                    long startCompilation = System.nanoTime();
                    genLogger.info("  Start compiling Java classes");
                    PureJavaCompiler compiler = compileJavaSources(startCompilation, generate, generateParams.isAddExternalAPI(), genLogger);
                    writeJavaClassFiles(startCompilation, compiler, generateParams.getTargetDirectoryForResources(), genLogger);
                    genLogger.info(String.format("  Finished compiling Java classes (%.9fs)", durationSinceInSeconds(startCompilation)));
                }
                else
                {
                    genLogger.info("  Java classes compilation: skipped");
                }

                // Write class files
                genLogger.info(String.format("  Finished building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
            }
        }
        catch (Exception e)
        {
            genLogger.error(String.format("    Error (%.9fs)", durationSinceInSeconds(start)), e);
            genLogger.error(String.format("    FAILURE building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
            throw new RuntimeException("Error building Pure compiled mode jar", e);
        }
    }

    private static long startStep(String step, Log log)
    {
        log.info("  Beginning " + step);
        return System.nanoTime();
    }

    private static void completeStep(String step, long stepStart, Log log)
    {
        log.info(String.format("    Finished %s (%.9fs)", step, durationSinceInSeconds(stepStart)));
    }

    private static CodeRepositorySet getAllRepositories(Set<String> extraRepositories)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        if (extraRepositories != null)
        {
            extraRepositories.forEach(r -> builder.addCodeRepository(getExtraRepository(classLoader, r)));
        }
        return builder.build();
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

    private static MutableSet<String> getSelectedRepositories(CodeRepositorySet allRepositories, Set<String> repositories, Set<String> excludedRepositories)
    {
        MutableSet<String> selected;
        if ((repositories == null) || repositories.isEmpty())
        {
            selected = allRepositories.getRepositoryNames().toSet();
        }
        else
        {
            selected = Sets.mutable.withAll(repositories);
            MutableList<String> missing = selected.reject(allRepositories::hasRepository, Lists.mutable.empty());
            if (missing.notEmpty())
            {
                throw new RuntimeException(missing.sortThis().makeString("Unknown repositories: \"", "\", \"", "\""));
            }
        }
        if (excludedRepositories != null)
        {
            selected.removeAll(excludedRepositories);
        }
        return selected;
    }

    private static Generate generate(long start, CodeRepositorySet allRepositories, SetIterable<String> selectedRepositories, Path distributedMetadataDirectory, Path codegenDirectory, boolean generateMetadata, boolean addExternalAPI, String externalAPIPackage, GenerationType generationType, boolean generateSources, Log log)
    {
        // Initialize runtime
        PureRuntime runtime = initializeRuntime(start, allRepositories, selectedRepositories, log);

        // Possibly write distributed metadata
        if (generateMetadata)
        {
            switch (generationType)
            {
                case monolithic:
                {
                    generateMetadata(start, runtime, distributedMetadataDirectory, log);
                    break;
                }
                case modular:
                {
                    generateModularMetadata(start, runtime, selectedRepositories, distributedMetadataDirectory, log);
                    break;
                }
                default:
                {
                    throw new RuntimeException("Unhandled generation type: " + generationType);
                }
            }
        }

        // Generate Java sources
        String generateStep = "Pure compiled mode Java code generation";
        long generateStart = startStep(generateStep, log);
        Generate generate;
        try
        {
            JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), addExternalAPI, externalAPIPackage, log);
            switch (generationType)
            {
                case monolithic:
                {
                    generate = generator.generateOnly(false, generateSources, codegenDirectory);
                    break;
                }
                case modular:
                {
                    generate = generator.generateOnly(selectedRepositories, true, generateSources, codegenDirectory);
                    break;
                }
                default:
                {
                    throw new RuntimeException("Unhandled generation type: " + generationType);
                }
            }
            completeStep(generateStep, generateStart, log);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return generate;
    }

    private static PureRuntime initializeRuntime(long start, CodeRepositorySet allRepositories, Iterable<String> selectedRepositories, Log log)
    {
        try
        {
            log.info("  Beginning Pure initialization");
            RichIterable<CodeRepository> repositoriesForCompilation = allRepositories.subset(selectedRepositories).getRepositories();

            Message message = new Message("")
            {
                @Override
                public void setMessage(String message)
                {
                    log.info(message);
                }
            };

            // Initialize from PAR files cache
            CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(Thread.currentThread().getContextClassLoader(), repositoriesForCompilation));
            ClassLoaderPureGraphCache graphCache = new ClassLoaderPureGraphCache(Thread.currentThread().getContextClassLoader());
            PureRuntime runtime = new PureRuntimeBuilder(codeStorage).withMessage(message).withCache(graphCache).setTransactionalByDefault(false).buildAndTryToInitializeFromCache();
            if (!runtime.isInitialized())
            {
                CacheState cacheState = graphCache.getCacheState();
                if (cacheState != null)
                {
                    String lastStackTrace = cacheState.getLastStackTrace();
                    if (lastStackTrace != null)
                    {
                        log.warn("    Cache initialization failure: " + lastStackTrace);
                    }
                }
                log.info("    Initialization from caches failed - compiling from scratch");
                runtime.reset();
                runtime.loadAndCompileCore(message);
                runtime.loadAndCompileSystem(message);
            }
            log.info(String.format("    Finished Pure initialization (%.9fs)", durationSinceInSeconds(start)));
            return runtime;
        }
        catch (Exception e)
        {
            log.error(String.format("    Error initializing Pure (%.9fs)", durationSinceInSeconds(start)), e);
            throw new RuntimeException(e);
        }
    }

    private static void generateMetadata(long start, PureRuntime runtime, Path distributedMetadataDirectory, Log log)
    {
        String writeMetadataStep = "writing distributed Pure metadata";
        long writeMetadataStart = startStep(writeMetadataStep, log);
        try
        {
            DistributedBinaryGraphSerializer.newSerializer(runtime).serializeToDirectory(distributedMetadataDirectory);
            completeStep(writeMetadataStep, writeMetadataStart, log);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void generateModularMetadata(long start, PureRuntime runtime, Iterable<String> repositoriesForMetadata, Path distributedMetadataDirectory, Log log)
    {
        String writeMetadataStep = "writing distributed Pure metadata";
        long writeMetadataStart = startStep(writeMetadataStep, log);
        try
        {
            for (String repository : repositoriesForMetadata)
            {
                generateModularMetadata(start, runtime, repository, distributedMetadataDirectory, log);
            }
            completeStep(writeMetadataStep, writeMetadataStart, log);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void generateModularMetadata(long start, PureRuntime runtime, String repository, Path distributedMetadataDirectory, Log log)
    {
        String writeMetadataStep = "writing distributed Pure metadata for " + repository;
        long writeMetadataStart = startStep(writeMetadataStep, log);
        try
        {
            DistributedBinaryGraphSerializer.newSerializer(runtime, repository).serializeToDirectory(distributedMetadataDirectory);
            completeStep(writeMetadataStep, writeMetadataStart, log);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static PureJavaCompiler compileJavaSources(long start, Generate generate, boolean addExternalAPI, Log log)
    {
        String compilationStep = "Pure compiled mode Java code compilation";
        long compilationStart = startStep(compilationStep, log);
        try
        {
            PureJavaCompiler compiler = JavaStandaloneLibraryGenerator.compileOnly(generate.getJavaSourcesByGroup(), generate.getExternalizableSources(), addExternalAPI, log);
            completeStep(compilationStep, compilationStart, log);
            return compiler;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeJavaClassFiles(long start, PureJavaCompiler compiler, File classesDirectory, Log log)
    {
        String writeClassFilesStep = "writing Pure compiled mode Java classes";
        long writeClassFilesStart = startStep(writeClassFilesStep, log);
        try
        {
            compiler.writeClassJavaSources(classesDirectory.toPath(), log);
            completeStep(writeClassFilesStep, writeClassFilesStart, log);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static double durationSinceInSeconds(long startNanos)
    {
        return durationInSeconds(startNanos, System.nanoTime());
    }

    public static double durationInSeconds(long startNanos, long endNanos)
    {
        return (endNanos - startNanos) / 1_000_000_000.0;
    }

    public enum GenerationType
    {
        monolithic, modular
    }

    public static class GenerateParams
    {
        private final Set<String> repositories;
        private final Set<String> excludedRepositories;
        private final Set<String> extraRepositories;
        private final GenerationType generationType;
        private final boolean skip;
        private final boolean addExternalAPI;
        private final String externalAPIPackage;
        private final boolean generateMetadata;
        private final boolean useSingleDir;
        private final boolean generateSources;
        private final boolean preventJavaCompilation;
        private final File targetDirectoryForResources;
        private Path targetDirectoryForSources;
        private Path targetDirectoryForMetadata;
        private final Log log;

        public GenerateParams(
                Set<String> repositories,
                Set<String> excludedRepositories,
                Set<String> extraRepositories,
                GenerationType generationType,
                boolean skip,
                boolean addExternalAPI,
                String externalAPIPackage,
                boolean generateMetadata,
                boolean useSingleDir,
                boolean generateSources,
                boolean preventJavaCompilation,
                File targetDirectoryForResources,
                Path targetDirectoryForSources,
                Path targetDirectoryForMetadata,
                Log log
                )
        {
            this.repositories = repositories;
            this.excludedRepositories = excludedRepositories;
            this.extraRepositories = extraRepositories;
            this.generationType = generationType;
            this.skip = skip;
            this.addExternalAPI = addExternalAPI;
            this.externalAPIPackage = externalAPIPackage;
            this.generateMetadata = generateMetadata;
            this.useSingleDir = useSingleDir;
            this.generateSources = generateSources;
            this.preventJavaCompilation = preventJavaCompilation;
            this.targetDirectoryForResources = targetDirectoryForResources;
            this.targetDirectoryForSources = targetDirectoryForSources;
            this.targetDirectoryForMetadata = targetDirectoryForMetadata;
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

        public GenerationType getGenerationType()
        {
            return generationType;
        }

        public boolean isSkip()
        {
            return skip;
        }

        public boolean isAddExternalAPI()
        {
            return addExternalAPI;
        }

        public String getExternalAPIPackage()
        {
            return externalAPIPackage;
        }

        public boolean isGenerateMetadata()
        {
            return generateMetadata;
        }

        public boolean isUseSingleDir()
        {
            return useSingleDir;
        }

        public boolean isGenerateSources()
        {
            return generateSources;
        }

        public boolean isPreventJavaCompilation()
        {
            return preventJavaCompilation;
        }

        public File getTargetDirectoryForResources()
        {
            return targetDirectoryForResources;
        }

        public Log getLog()
        {
            return log;
        }

        public Path getTargetDirectoryForSources()
        {
            return this.targetDirectoryForSources;
        }

        public Path getTargetDirectoryForMetadata()
        {
            return this.targetDirectoryForMetadata;
        }
    }
}
