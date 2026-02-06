// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.maven.compiler;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.finos.legend.pure.m3.generator.compiler.PureCompilerBinaryGenerator;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.maven.shared.DependencyResolutionScope;
import org.finos.legend.pure.maven.shared.ProjectDependencyResolution;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "compile-pure",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class PureCompilerMojo extends AbstractMojo
{
    @Parameter
    private File outputDirectory;

    @Parameter
    private Set<String> repositories;

    @Parameter
    private Set<String> excludedRepositories;

    /**
     * <p>The scope of the dependencies to resolve from the Maven module. Use names from {@link DependencyResolutionScope}.
     * If not specified, defaults to
     * <ul>
     *     <li>{@code test} if the current execution phase is a "test phase"
     *     (see {@link ProjectDependencyResolution#inTestPhase(MojoExecution)}), or</li>
     *     <li>{@code compile} otherwise.</li>
     * </ul></p>
     *
     * @see DependencyResolutionScope
     */
    @Parameter
    protected String dependencyScope;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File projectOutputDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.testOutputDirectory}")
    private File projectTestOutputDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${mojoExecution}")
    private MojoExecution mojoExecution;

    @Component
    private ProjectDependenciesResolver mavenProjectDependenciesResolver;

    @Parameter(readonly = true, required = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession mavenRepoSession;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            DependencyResolutionScope dependencyResolutionScope = ProjectDependencyResolution.determineDependencyResolutionScope(dependencyScope, mojoExecution);
            URL[] dependencyUrls = ProjectDependencyResolution.getDependencyURLs(
                    dependencyResolutionScope,
                    mavenProject,
                    mojoExecution,
                    mavenRepoSession,
                    projectOutputDirectory,
                    projectTestOutputDirectory,
                    mavenProjectDependenciesResolver
            );
            try (URLClassLoader classLoader = new URLClassLoader(dependencyUrls, Thread.currentThread().getContextClassLoader()))
            {
                Thread.currentThread().setContextClassLoader(classLoader);
                this.executeWithinClassLoader(classLoader, dependencyResolutionScope);
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Error closing classloader", e);
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(savedClassLoader);
            }
        }
        catch (DependencyResolutionException e)
        {
            throw new MojoExecutionException("Error setting up classloader with project dependencies", e);
        }
    }

    private void executeWithinClassLoader(ClassLoader classLoader, DependencyResolutionScope dependencyResolutionScope) throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Compiling Pure");
        File resolvedOutputDir = resolveOutputDirectory();
        getLog().debug("Output directory: " + resolvedOutputDir);
        getLog().debug("Requested repositories: " + (isNonEmpty(this.repositories) ? String.join(", ", this.repositories) : "<none>"));
        getLog().debug("Excluded repositories: " + (isNonEmpty(this.excludedRepositories) ? String.join(", ", this.excludedRepositories) : "<none>"));
        getLog().debug("Dependency scope: " + dependencyResolutionScope);

        Set<String> resolvedRepos = resolveRepositoriesToSerialize(dependencyResolutionScope);
        if ((resolvedRepos != null) && resolvedRepos.isEmpty())
        {
            getLog().warn("No repositories to serialize");
            return;
        }
        getLog().debug("Resolved repositories: " + ((resolvedRepos == null) ? "<all>" : String.join(", ", resolvedRepos)));

        try
        {
            PureCompilerBinaryGenerator.serializeModules(resolvedOutputDir.toPath(), classLoader, resolvedRepos, this.excludedRepositories);
        }
        catch (PureCompilationException | PureParserException e)
        {
            throw new MojoFailureException(e.getMessage(), e);
        }
        catch (UncheckedIOException e)
        {
            IOException ioException = e.getCause();
            throw new MojoExecutionException(ioException.getMessage(), ioException);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private File resolveOutputDirectory()
    {
        if (this.outputDirectory != null)
        {
            return this.outputDirectory;
        }
        if (ProjectDependencyResolution.inTestPhase(this.mojoExecution))
        {
            return this.projectTestOutputDirectory;
        }
        return this.projectOutputDirectory;
    }

    private Set<String> resolveRepositoriesToSerialize(DependencyResolutionScope resolvedDependencyScope) throws MojoExecutionException
    {
        // If the user has specified repositories, use them
        if (isNonEmpty(this.repositories))
        {
            if (isNonEmpty(this.excludedRepositories) && this.excludedRepositories.stream().anyMatch(this.repositories::contains))
            {
                throw new MojoExecutionException(this.excludedRepositories.stream()
                        .filter(this.repositories::contains)
                        .sorted()
                        .collect(Collectors.joining(", ", "Invalid repository specification; the following are both included and excluded: ", "")));
            }
            return this.repositories;
        }

        // Look for repositories defined in the project
        Set<String> foundRepositories = new HashSet<>();
        forEachRepoDefinition(this.projectOutputDirectory, foundRepositories::add);
        if (ProjectDependencyResolution.inTestPhase(this.mojoExecution) && resolvedDependencyScope.isTestScope())
        {
            forEachRepoDefinition(this.projectTestOutputDirectory, foundRepositories::add);
        }

        // If there are no repositories defined in the project, then we will serialize all repositories (minus any specified exclusions)
        if (foundRepositories.isEmpty())
        {
            return null;
        }

        // Otherwise, we will serialize the repositories defined in the project (minus any specified exclusions)
        if (isNonEmpty(this.excludedRepositories))
        {
            foundRepositories.removeAll(this.excludedRepositories);
        }
        return foundRepositories;
    }

    private void forEachRepoDefinition(File directory, Consumer<? super String> consumer)
    {
        forEachRepoDefinition(directory.toPath(), consumer);
    }

    private void forEachRepoDefinition(Path directory, Consumer<? super String> consumer)
    {
        try (Stream<Path> files = Files.list(directory))
        {
            files.filter(f -> f.toString().endsWith(".definition.json"))
                    .map(GenericCodeRepository::build)
                    .map(CodeRepository::getName)
                    .forEach(consumer);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isNonEmpty(Set<?> set)
    {
        return (set != null) && !set.isEmpty();
    }
}
