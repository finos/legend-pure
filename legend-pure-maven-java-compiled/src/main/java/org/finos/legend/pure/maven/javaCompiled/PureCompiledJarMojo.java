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

package org.finos.legend.pure.maven.javaCompiled;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

@Mojo(name = "build-pure-compiled-jar")
public class PureCompiledJarMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    private File targetDirectory;

    @Parameter(readonly = true, defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter
    private Set<String> repositories;

    @Parameter
    private Set<String> excludedRepositories;

    @Parameter
    private Set<String> extraRepositories;

    @Parameter(defaultValue = "true")
    private boolean generateMetadata;

    @Parameter(defaultValue = "monolithic")
    private GenerationType generationType;

    @Parameter(defaultValue = "false")
    private boolean useSingleDir;

    @Parameter(defaultValue = "false")
    private boolean generateSources;

    @Parameter(defaultValue = "false")
    private boolean preventJavaCompilation;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (this.skip)
        {
            getLog().info("Skipping Java Compiled JAR generation");
            return;
        }

        long start = System.nanoTime();
        getLog().info("Generating Java Compiled JAR");
        getLog().info("  Requested repositories: " + this.repositories);
        getLog().info("  Excluded repositories: " + this.excludedRepositories);
        getLog().info("  Extra repositories: " + this.extraRepositories);
        getLog().info("  Generation type: " + this.generationType);

        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(buildClassLoader(this.project, savedClassLoader));

            CodeRepositorySet allRepositories = getAllRepositories();
            MutableSet<String> selectedRepositories = getSelectedRepositories(allRepositories);
            getLog().info(selectedRepositories.makeString("  Resolved repositories: ", ", ", ""));

            Path distributedMetadataDirectory;
            if (!this.generateMetadata)
            {
                distributedMetadataDirectory = null;
                getLog().info("  Classes output directory: " + this.classesDirectory);
                getLog().info("  No metadata output");
            }
            else if (this.useSingleDir)
            {
                distributedMetadataDirectory = this.classesDirectory.toPath();
                getLog().info("  All in output directory: " + this.classesDirectory);
            }
            else
            {
                distributedMetadataDirectory = this.targetDirectory.toPath().resolve("metadata-distributed");
                getLog().info("  Classes output directory: " + this.classesDirectory);
                getLog().info("  Distributed metadata output directory: " + distributedMetadataDirectory);
            }

            Path codegenDirectory;
            if (this.generateSources)
            {
                codegenDirectory = this.targetDirectory.toPath().resolve("generated-sources");
                getLog().info("  Codegen output directory: " + codegenDirectory);
            }
            else
            {
                codegenDirectory = null;
            }

            // Generate metadata and Java sources
            long startGenerating = System.nanoTime();
            getLog().info("  Start generating Java classes");
            Generate generate = generate(startGenerating, allRepositories, selectedRepositories, distributedMetadataDirectory, codegenDirectory);
            getLog().info(String.format("  Finished generating Java classes (%.9fs)", durationSinceInSeconds(startGenerating)));

            // Compile Java sources
            if (!this.preventJavaCompilation)
            {
                long startCompilation = System.nanoTime();
                getLog().info("  Start compiling Java classes");
                PureJavaCompiler compiler = compileJavaSources(startCompilation, generate);
                writeJavaClassFiles(startCompilation, compiler);
                getLog().info(String.format("  Finished compiling Java classes (%.9fs)", durationSinceInSeconds(startCompilation)));
            }
            else
            {
                getLog().info("  Java classes compilation: skipped");
            }

            // Write class files
            getLog().info(String.format("  Finished building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
        }
        catch (MojoExecutionException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            getLog().error(String.format("    Error (%.9fs)", durationSinceInSeconds(start)), e);
            getLog().error(String.format("    FAILURE building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
            throw new MojoExecutionException("Error building Pure compiled mode jar", e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

    private long startStep(String step)
    {
        getLog().info("  Beginning " + step);
        return System.nanoTime();
    }

    private void completeStep(String step, long stepStart)
    {
        getLog().info(String.format("    Finished %s (%.9fs)", step, durationSinceInSeconds(stepStart)));
    }

    private CodeRepositorySet getAllRepositories()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        if (this.extraRepositories != null)
        {
            this.extraRepositories.forEach(r -> builder.addCodeRepository(GenericCodeRepository.build(classLoader, r)));
        }
        return builder.build();
    }

    private MutableSet<String> getSelectedRepositories(CodeRepositorySet allRepositories)
    {
        MutableSet<String> selected;
        if ((this.repositories == null) || this.repositories.isEmpty())
        {
            selected = allRepositories.getRepositoryNames().toSet();
        }
        else
        {
            selected = Sets.mutable.withAll(this.repositories);
            MutableList<String> missing = selected.reject(allRepositories::hasRepository, Lists.mutable.empty());
            if (missing.notEmpty())
            {
                throw new RuntimeException(missing.sortThis().makeString("Unknown repositories: \"", "\", \"", "\""));
            }
        }
        if (this.excludedRepositories != null)
        {
            selected.removeAll(this.excludedRepositories);
        }
        return selected;
    }

    private Generate generate(long start, CodeRepositorySet allRepositories, SetIterable<String> selectedRepositories, Path distributedMetadataDirectory, Path codegenDirectory) throws MojoExecutionException
    {
        // Initialize runtime
        PureRuntime runtime = initializeRuntime(start, allRepositories, selectedRepositories);

        // Possibly write distributed metadata
        if (this.generateMetadata)
        {
            switch (this.generationType)
            {
                case monolithic:
                {
                    generateMetadata(start, runtime, distributedMetadataDirectory);
                    break;
                }
                case modular:
                {
                    generateModularMetadata(start, runtime, selectedRepositories, distributedMetadataDirectory);
                    break;
                }
                default:
                {
                    throw new MojoExecutionException("Unhandled generation type: " + this.generationType);
                }
            }
        }

        // Generate Java sources
        String generateStep = "Pure compiled mode Java code generation";
        long generateStart = startStep(generateStep);
        Generate generate;
        try
        {
            JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, JavaPackageAndImportBuilder.externalizablePackage());
            switch (this.generationType)
            {
                case monolithic:
                {
                    generate = generator.generateOnly(false, this.generateSources, codegenDirectory);
                    break;
                }
                case modular:
                {
                    generate = generator.generateOnly(selectedRepositories, true, this.generateSources, codegenDirectory);
                    break;
                }
                default:
                {
                    throw new MojoExecutionException("Unhandled generation type: " + this.generationType);
                }
            }
            completeStep(generateStep, generateStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, generateStep, generateStart, start);
        }
        return generate;
    }

    private PureRuntime initializeRuntime(long start, CodeRepositorySet allRepositories, Iterable<String> selectedRepositories) throws MojoExecutionException
    {
        try
        {
            getLog().info("  Beginning Pure initialization");
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
                        getLog().warn("    Cache initialization failure: " + lastStackTrace);
                    }
                }
                getLog().info("    Initialization from caches failed - compiling from scratch");
                runtime.reset();
                runtime.loadAndCompileCore();
                runtime.loadAndCompileSystem();
            }
            getLog().info(String.format("    Finished Pure initialization (%.9fs)", durationSinceInSeconds(start)));
            return runtime;
        }
        catch (Exception e)
        {
            getLog().error(String.format("    Error initializing Pure (%.9fs)", durationSinceInSeconds(start)), e);
            throw new MojoExecutionException("Error initializing Pure", e);
        }
    }

    private void generateMetadata(long start, PureRuntime runtime, Path distributedMetadataDirectory) throws MojoExecutionException
    {
        String writeMetadataStep = "writing distributed Pure metadata";
        long writeMetadataStart = startStep(writeMetadataStep);
        try
        {
            DistributedBinaryGraphSerializer.newSerializer(runtime).serializeToDirectory(distributedMetadataDirectory);
            completeStep(writeMetadataStep, writeMetadataStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, writeMetadataStep, writeMetadataStart, start);
        }
    }

    private void generateModularMetadata(long start, PureRuntime runtime, Iterable<String> repositoriesForMetadata, Path distributedMetadataDirectory) throws MojoExecutionException
    {
        String writeMetadataStep = "writing distributed Pure metadata";
        long writeMetadataStart = startStep(writeMetadataStep);
        try
        {
            for (String repository : repositoriesForMetadata)
            {
                generateModularMetadata(start, runtime, repository, distributedMetadataDirectory);
            }
            completeStep(writeMetadataStep, writeMetadataStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, writeMetadataStep, writeMetadataStart, start);
        }
    }

    private void generateModularMetadata(long start, PureRuntime runtime, String repository, Path distributedMetadataDirectory) throws MojoExecutionException
    {
        String writeMetadataStep = "writing distributed Pure metadata for " + repository;
        long writeMetadataStart = startStep(writeMetadataStep);
        try
        {
            DistributedBinaryGraphSerializer.newSerializer(runtime, repository).serializeToDirectory(distributedMetadataDirectory);
            completeStep(writeMetadataStep, writeMetadataStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, writeMetadataStep, writeMetadataStart, start);
        }
    }

    private PureJavaCompiler compileJavaSources(long start, Generate generate) throws MojoExecutionException
    {
        String compilationStep = "Pure compiled mode Java code compilation";
        long compilationStart = startStep(compilationStep);
        try
        {
            PureJavaCompiler compiler = JavaStandaloneLibraryGenerator.compileOnly(generate.getJavaSourcesByGroup(), generate.getExternalizableSources(), false);
            completeStep(compilationStep, compilationStart);
            return compiler;
        }
        catch (Exception e)
        {
            throw mojoException(e, compilationStep, compilationStart, start);
        }
    }

    private void writeJavaClassFiles(long start, PureJavaCompiler compiler) throws MojoExecutionException
    {
        String writeClassFilesStep = "writing Pure compiled mode Java classes";
        long writeClassFilesStart = startStep(writeClassFilesStep);
        try
        {
            compiler.writeClassJavaSources(this.classesDirectory.toPath());
            completeStep(writeClassFilesStep, writeClassFilesStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, writeClassFilesStep, writeClassFilesStart, start);
        }
    }

    private MojoExecutionException mojoException(Exception e, String step, long stepStart, long start)
    {
        long failureTime = System.nanoTime();
        getLog().error(String.format("    Error %s (%.9fs)", step, durationInSeconds(stepStart, failureTime)), e);
        getLog().error(String.format("    FAILURE building Pure compiled mode jar (%.9fs)", durationInSeconds(start, failureTime)));
        return (e instanceof MojoExecutionException) ? (MojoExecutionException) e : new MojoExecutionException("Error writing Pure compiled mode Java code and metadata", e);
    }

    private double durationSinceInSeconds(long startNanos)
    {
        return durationInSeconds(startNanos, System.nanoTime());
    }

    private double durationInSeconds(long startNanos, long endNanos)
    {
        return (endNanos - startNanos) / 1_000_000_000.0;
    }

    public enum GenerationType
    {
        monolithic, modular
    }

    private ClassLoader buildClassLoader(MavenProject project, ClassLoader parent) throws DependencyResolutionRequiredException
    {
        // Add the project output to the plugin classloader
        URL[] urlsForClassLoader = ListIterate.collect(project.getCompileClasspathElements(), mavenCompilePath ->
        {
            try
            {
                return Paths.get(mavenCompilePath).toUri().toURL();
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }
        }).toArray(new URL[0]);
        getLog().info("    Project classLoader URLs " + Arrays.toString(urlsForClassLoader));
        return new URLClassLoader(urlsForClassLoader, parent);
    }
}
