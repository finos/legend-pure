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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
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

    @Override
    public void execute() throws MojoExecutionException
    {
        ClassLoader savedClassLoader  = Thread.currentThread().getContextClassLoader();
        try
        {
            getLog().info("Generating Java Compiled JAR");
            getLog().info("  Requested repositories: " + this.repositories);
            getLog().info("  Excluded repositories: " + this.excludedRepositories);
            getLog().info("  Extra repositories: " + this.extraRepositories);
            getLog().info("  Generation type: " + this.generationType);

            Thread.currentThread().setContextClassLoader(this.buildClassLoader(this.project));

            PureRepositoriesExternal.refresh();

            ListIterable<CodeRepository> resolvedRepositories = resolveRepositories();
            getLog().info(resolvedRepositories.asLazy().collect(CodeRepository::getName).makeString("  Resolved repositories: ", ", ", ""));

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
                codegenDirectory = this.targetDirectory.toPath().resolve("generated");
                getLog().info("  Codegen output directory: " + codegenDirectory);
            }
            else
            {
                codegenDirectory = null;
            }

            long start = System.nanoTime();

            // Generate metadata and Java sources
            Generate generate = generate(start, resolvedRepositories, distributedMetadataDirectory, codegenDirectory);

            // Compile Java sources
            PureJavaCompiler compiler = compileJavaSources(start, generate);

            // Write class files
            writeJavaClassFiles(start, compiler);

            getLog().info(String.format("  Finished building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("error", e);
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

    private ListIterable<CodeRepository> resolveRepositories()
    {
        if ((this.extraRepositories != null) && !this.extraRepositories.isEmpty())
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            PureRepositoriesExternal.addRepositories(Iterate.collect(this.extraRepositories, r -> GenericCodeRepository.build(classLoader, r), Lists.mutable.withInitialCapacity(excludedRepositories.size())));
        }

        MutableList<CodeRepository> selectedRepos = (this.repositories == null) ?
                PureRepositoriesExternal.repositories().toList() :
                Iterate.collect(this.repositories, PureRepositoriesExternal::getRepository, Lists.mutable.withInitialCapacity(this.repositories.size()));
        if ((this.excludedRepositories != null) && !this.excludedRepositories.isEmpty())
        {
            selectedRepos.removeIf(r -> this.excludedRepositories.contains(r.getName()));
        }
        return selectedRepos;
    }

    private Generate generate(long start, ListIterable<CodeRepository> resolvedRepositories, Path distributedMetadataDirectory, Path codegenDirectory) throws MojoExecutionException
    {
        // Initialize runtime
        PureRuntime runtime = initializeRuntime(start, resolvedRepositories);

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
                    generateModularMetadata(start, runtime, resolvedRepositories, distributedMetadataDirectory);
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
                    generate = generator.generateOnly(resolvedRepositories.collect(CodeRepository::getName), true, this.generateSources, codegenDirectory);
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

    private PureRuntime initializeRuntime(long start, RichIterable<CodeRepository> resolvedRepositories) throws MojoExecutionException
    {
        try
        {
            getLog().info("  Beginning Pure initialization");
            SetIterable<CodeRepository> repositoriesForCompilation = PureCodeStorage.getRepositoryDependencies(PureRepositoriesExternal.repositories(), resolvedRepositories);

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

    private void generateModularMetadata(long start, PureRuntime runtime, ListIterable<CodeRepository> resolvedRepositories, Path distributedMetadataDirectory) throws MojoExecutionException
    {
        String writeMetadataStep = "writing distributed Pure metadata";
        long writeMetadataStart = startStep(writeMetadataStep);
        try
        {
            for (CodeRepository repository : resolvedRepositories)
            {
                generateModularMetadata(start, runtime, repository.getName(), distributedMetadataDirectory);
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

    private ClassLoader buildClassLoader(MavenProject project) throws DependencyResolutionRequiredException
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
        return new URLClassLoader(urlsForClassLoader, Thread.currentThread().getContextClassLoader());
    }
}
