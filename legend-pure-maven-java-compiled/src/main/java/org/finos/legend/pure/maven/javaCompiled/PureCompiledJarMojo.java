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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Procedures2;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.cache.CacheState;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.Generate;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaStandaloneLibraryGenerator;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;

import java.io.File;
import java.nio.file.Path;

@Mojo(name = "build-pure-compiled-jar")
public class PureCompiledJarMojo extends AbstractMojo
{
    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    private File targetDirectory;

    @Parameter(readonly = true, defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;

    @Parameter
    private String[] repositories;

    @Parameter
    private String[] excludedRepositories;

    @Parameter(defaultValue = "true")
    private boolean generateMetadata;

    @Parameter(defaultValue = "false")
    private boolean useSingleDir;

    @Parameter(defaultValue = "false")
    private boolean generateSources;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        SetIterable<String> resolvedRepositories = resolveRepositories();
        getLog().info(resolvedRepositories.toSortedList().makeString("Repositories: ", ", ", ""));
        Path distributedMetadataDirectory = null;
        Path codegenDirectory = null;

        if (!this.generateMetadata)
        {
            getLog().info("Classes output directory: " + this.classesDirectory);
            getLog().info("No metadata output");
        }
        else if (this.useSingleDir)
        {
            distributedMetadataDirectory = this.classesDirectory.toPath();
            getLog().info("All in output directory: " + this.classesDirectory);
        }
        else
        {
            distributedMetadataDirectory = this.targetDirectory.toPath().resolve("metadata-distributed");
            getLog().info("Classes output directory: " + this.classesDirectory);
            getLog().info("Distributed metadata output directory: " + distributedMetadataDirectory);
        }

        if (this.generateSources)
        {
            codegenDirectory = this.targetDirectory.toPath().resolve("generated");
            getLog().info("Codegen output directory: " + codegenDirectory);
        }

        long start = System.nanoTime();

        // Initialize runtime
        PureRuntime runtime;
        try
        {
            getLog().info("Beginning Pure initialization");
            SetIterable<CodeRepository> repositoriesForCompilation = PureCodeStorage.getRepositoryDependencies(PureRepositoriesExternal.repositories, LazyIterate.collect(resolvedRepositories, (String repo) -> PureRepositoriesExternal.repositories.select(p ->p.getName().equals(repo)).getFirst()));
            PureCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(repositoriesForCompilation));
            ClassLoaderPureGraphCache graphCache = new ClassLoaderPureGraphCache();
            runtime = new PureRuntimeBuilder(codeStorage).withCache(graphCache).setTransactionalByDefault(false).buildAndTryToInitializeFromCache();
            if (!runtime.isInitialized())
            {
                CacheState cacheState = graphCache.getCacheState();
                if (cacheState != null)
                {
                    String lastStackTrace = cacheState.getLastStackTrace();
                    if (lastStackTrace != null)
                    {
                        getLog().warn("Cache initialization failure: " + lastStackTrace);
                    }
                }
                getLog().info("Initialization from caches failed - compiling from scratch");
                runtime.reset();
                runtime.loadAndCompileCore();
                runtime.loadAndCompileSystem();
            }
            getLog().info(String.format("Finished Pure initialization (%.6fs)", (System.nanoTime() - start) / 1_000_000_000.0));
        }
        catch (PureException e)
        {
            getLog().error(String.format("Error initializing Pure (%.6fs)", (System.nanoTime() - start) / 1_000_000_000.0), e);
            throw new MojoFailureException(e.getInfo(), e);
        }
        catch (Exception e)
        {
            getLog().error(String.format("Error initializing Pure (%.6fs)", (System.nanoTime() - start) / 1_000_000_000.0), e);
            throw new MojoExecutionException("Error initializing Pure", e);
        }

        if (generateMetadata)
        {
            // Write distributed metadata
            String writeMetadataStep = "writing distributed Pure metadata";
            long writeMetadataStart = startStep(writeMetadataStep);
            try
            {
                DistributedBinaryGraphSerializer.serialize(runtime, distributedMetadataDirectory);
                completeStep(writeMetadataStep, writeMetadataStart);
            }
            catch (Exception e)
            {
                throw mojoException(e, writeMetadataStep, writeMetadataStart, start);
            }
        }

        JavaStandaloneLibraryGenerator generator = JavaStandaloneLibraryGenerator.newGenerator(runtime, CompiledExtensionLoader.extensions(), false, JavaPackageAndImportBuilder.externalizablePackage());
        // Generate Java sources
        Generate generate;
        String generateStep = "Pure compiled mode Java code generation";
        long generateStart = startStep(generateStep);
        try
        {
            generate = generator.generateOnly(this.generateSources, codegenDirectory);
            completeStep(generateStep, generateStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, generateStep, generateStart, start);
        }
        // Set generator and runtime to null so the memory can be cleaned up
        generator = null;
        runtime = null;

        // Compile Java sources
        PureJavaCompiler compiler;
        String compilationStep = "Pure compiled mode Java code compilation";
        long compilationStart = startStep(compilationStep);
        try
        {
            compiler = JavaStandaloneLibraryGenerator.compileOnly(generate.getJavaSources(), generate.getExternalizableSources(), false);
            completeStep(compilationStep, compilationStart);
        }
        catch (Exception e)
        {
            throw mojoException(e, compilationStep, compilationStart, start);
        }

        // Write class files
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
        // Set compiler to null so the memory can be cleaned up
        compiler = null;

        getLog().info(String.format("Finished building Pure compiled mode jar (%.6fs)", (System.nanoTime() - start) / 1_000_000_000.0));
    }

    private long startStep(String step)
    {
        getLog().info("Beginning " + step);
        return System.nanoTime();
    }

    private void completeStep(String step, long stepStart)
    {
        getLog().info(String.format("Finished %s (%.6fs)", step, (System.nanoTime() - stepStart) / 1_000_000_000.0));
    }

    private MojoExecutionException mojoException(Exception e, String step, long stepStart, long start) throws MojoExecutionException
    {
        long failureTime = System.nanoTime();
        getLog().error(String.format("Error %s (%.6fs)", step, (failureTime - stepStart) / 1_000_000_000.0), e);
        getLog().error(String.format("FAILURE building Pure compiled mode jar (%.6fs)", (failureTime - start) / 1_000_000_000.0));
        return new MojoExecutionException("Error writing Pure compiled mode Java code and metadata", e);
    }

    private SetIterable<String> resolveRepositories()
    {
        MutableSet<String> result = Sets.mutable.empty();

        if (this.repositories != null)
        {
            // If repositories are specified, add them
            ArrayIterate.forEachWith(this.repositories, Procedures2.<String>addToCollection(), result);
        }
        else
        {
            // If repositories are not specified, add all non-scratch repositories
            result.addAllIterable(PureRepositoriesExternal.repositories.collect(CodeRepository.GET_NAME));
        }

        if (this.excludedRepositories != null)
        {
            // Remove excluded repositories
            ArrayIterate.forEachWith(this.excludedRepositories, Procedures2.<String>removeFromCollection(), result);
        }

        return result;
    }
}
