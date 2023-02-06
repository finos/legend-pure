package org.finos.legend.pure.runtime.java.compiled.generation.orchestrator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.ParserService;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.Generate;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaStandaloneLibraryGenerator;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class JavaCodeGeneration
{
    public static void main(String args[]) throws Exception
    {
        Log log = new Log()
        {
            @Override
            public void info(String txt)
            {
                System.out.println(txt);
            }

            @Override
            public void error(String txt, Exception e)
            {
                System.out.println(txt);
                e.printStackTrace();
            }

            @Override
            public void error(String format)
            {
                System.out.println(format);
            }

            @Override
            public void warn(String s)
            {
                System.out.println(s);
            }
        };
        doIt(
                Sets.mutable.with(args[0]),
                Sets.mutable.empty(),
                Sets.mutable.empty(),
                GenerationType.modular,
                false,
                true,
                true,
                true,
                true,
                true,
                new File(args[1]),
                new File(args[2]),
                log
        );
    }


    public static void doIt(Set<String> repositories,
                            Set<String> excludedRepositories,
                            Set<String> extraRepositories,
                            JavaCodeGeneration.GenerationType generationType,
                            boolean skip,
                            boolean generateMetadata,
                            boolean useSingleDir,
                            boolean generateSources,
                            boolean generateTest,
                            boolean preventJavaCompilation,
                            File classesDirectory,
                            File targetDirectory,
                            Log log)
    {
        // DO NOT DELETE - Needed to avoid circular calls later during static initialization
        SetIterable<String> res = JavaPackageAndImportBuilder.M3_CLASSES;
        // DO NOT DELETE - Needed to avoid circular calls later during static initialization

        if (skip)
        {
            log.info("Skipping Java Compiled JAR generation");
            return;
        }

        long start = System.nanoTime();
        log.info("Generating Java Compiled JAR");
        log.info("  Requested repositories: " + repositories);
        log.info("  Excluded repositories: " + excludedRepositories);
        log.info("  Extra repositories: " + extraRepositories);
        log.info("  Generation type: " + generationType);

        try
        {
            CodeRepositorySet allRepositories = getAllRepositories(extraRepositories);
            log.info(allRepositories.getRepositories().collect(CodeRepository::getName).makeString("  Found repositories: ", ", ", ""));
            log.info(new ParserService().parsers().collect(Parser::getName).makeString("  Found parsers: ", ", ", ""));
            log.info(new ParserService().inlineDSLs().collect(InlineDSL::getName).makeString("  Found DSL parsers: ", ", ", ""));
            MutableSet<String> selectedRepositories = getSelectedRepositories(allRepositories, repositories, excludedRepositories);
            log.info(selectedRepositories.makeString("  Selected repositories: ", ", ", ""));

            Path distributedMetadataDirectory;
            if (!generateMetadata)
            {
                distributedMetadataDirectory = null;
                log.info("  Classes output directory: " + classesDirectory);
                log.info("  No metadata output");
            }
            else if (useSingleDir)
            {
                distributedMetadataDirectory = classesDirectory.toPath();
                log.info("  All in output directory: " + classesDirectory);
            }
            else
            {
                distributedMetadataDirectory = targetDirectory.toPath().resolve("metadata-distributed");
                log.info("  Classes output directory: " + classesDirectory);
                log.info("  Distributed metadata output directory: " + distributedMetadataDirectory);
            }

            Path codegenDirectory;
            if (generateSources)
            {
                codegenDirectory = targetDirectory.toPath().resolve(generateTest ? "generated-test-sources" : "generated-sources");
                log.info("  Codegen output directory: " + codegenDirectory);
            }
            else
            {
                codegenDirectory = null;
            }

            // Generate metadata and Java sources
            long startGenerating = System.nanoTime();
            log.info("  Start generating Java classes");
            Generate generate = generate(startGenerating, allRepositories, selectedRepositories, distributedMetadataDirectory, codegenDirectory, generateMetadata, generationType, generateSources, log);
            log.info(String.format("  Finished generating Java classes (%.9fs)", durationSinceInSeconds(startGenerating)));


            // Compile Java sources
            if (!preventJavaCompilation)
            {
                long startCompilation = System.nanoTime();
                log.info("  Start compiling Java classes");
                PureJavaCompiler compiler = compileJavaSources(startCompilation, generate, log);
                writeJavaClassFiles(startCompilation, compiler, classesDirectory, log);
                log.info(String.format("  Finished compiling Java classes (%.9fs)", durationSinceInSeconds(startCompilation)));
            }
            else
            {
                log.info("  Java classes compilation: skipped");
            }

            // Write class files
            log.info(String.format("  Finished building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
        }
        catch (Exception e)
        {
            log.error(String.format("    Error (%.9fs)", durationSinceInSeconds(start)), e);
            log.error(String.format("    FAILURE building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
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

    private static Generate generate(long start, CodeRepositorySet allRepositories, SetIterable<String> selectedRepositories, Path distributedMetadataDirectory, Path codegenDirectory, boolean generateMetadata, GenerationType generationType, boolean generateSources, Log log)
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
            JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, JavaPackageAndImportBuilder.externalizablePackage());
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

            // Initialize from PAR files cache
            PureCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(Thread.currentThread().getContextClassLoader(), repositoriesForCompilation));
            ClassLoaderPureGraphCache graphCache = new ClassLoaderPureGraphCache(Thread.currentThread().getContextClassLoader());
            PureRuntime runtime = new PureRuntimeBuilder(codeStorage).withCache(graphCache).setTransactionalByDefault(false).buildAndTryToInitializeFromCache();
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
                runtime.loadAndCompileCore();
                runtime.loadAndCompileSystem();
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

    private static PureJavaCompiler compileJavaSources(long start, Generate generate, Log log)
    {
        String compilationStep = "Pure compiled mode Java code compilation";
        long compilationStart = startStep(compilationStep, log);
        try
        {
            PureJavaCompiler compiler = JavaStandaloneLibraryGenerator.compileOnly(generate.getJavaSourcesByGroup(), generate.getExternalizableSources(), false);
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
            compiler.writeClassJavaSources(classesDirectory.toPath());
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
}
