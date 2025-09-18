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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.finos.legend.pure.m3.generator.compiler.PureCompilerBinaryGenerator;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "compile-pure",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class PureCompilerMojo extends AbstractMojo
{
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter
    private Set<String> repositories;

    @Parameter
    private Set<String> excludedRepositories;

    @Component
    private ProjectDependenciesResolver mavenProjectDependenciesResolver;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession mavenRepoSession;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "compile")
    private String dependencyScope;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Compiling Pure");
        getLog().info("Output directory: " + this.outputDirectory);
        getLog().info("Requested repositories: " + (isNonEmpty(this.repositories) ? String.join(", ", this.repositories) : "<none>"));
        getLog().info("Excluded repositories: " + (isNonEmpty(this.excludedRepositories) ? String.join(", ", this.excludedRepositories) : "<none>"));

        Set<String> resolvedRepos = resolveRepositoriesToSerialize();
        if ((resolvedRepos != null) && resolvedRepos.isEmpty())
        {
            getLog().warn("No repositories to serialize");
            return;
        }
        getLog().info("Resolved repositories: " + ((resolvedRepos == null) ? "<all>" : String.join(", ", resolvedRepos)));

        try (URLClassLoader classLoader = new URLClassLoader(getDependencyURLs(), Thread.currentThread().getContextClassLoader()))
        {
            PureCompilerBinaryGenerator.serializeModules(this.outputDirectory.toPath(), classLoader, resolvedRepos, this.excludedRepositories);
        }
        catch (MojoExecutionException e)
        {
            throw e;
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

    private Set<String> resolveRepositoriesToSerialize() throws MojoExecutionException
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
        Set<String> foundRepositories;
        try (Stream<Path> files = Files.list(this.outputDirectory.toPath()))
        {
            foundRepositories = files.filter(f -> f.toString().endsWith(".definition.json"))
                    .map(GenericCodeRepository::build)
                    .map(CodeRepository::getName)
                    .collect(Collectors.toSet());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
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

    private URL[] getDependencyURLs() throws DependencyResolutionException, MojoExecutionException
    {
        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(this.mavenProject, this.mavenRepoSession)
                .setResolutionFilter(getDependencyFilter());
        return Stream.concat(
                        Stream.of(this.outputDirectory),
                        this.mavenProjectDependenciesResolver.resolve(request).getDependencies().stream()
                                .map(Dependency::getArtifact)
                                .map(Artifact::getFile))
                .map(PureCompilerMojo::toURL)
                .toArray(URL[]::new);
    }

    private DependencyFilter getDependencyFilter() throws MojoExecutionException
    {
        switch (this.dependencyScope)
        {
            case "compile":
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "provided", "system"), null);
            }
            case "compile+runtime":
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "provided", "runtime", "system"), null);
            }
            case "runtime":
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "runtime"), null);
            }
            case "runtime+system":
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "runtime", "system"), null);
            }
            case "test":
            {
                return null;
            }
            default:
            {
                throw new MojoExecutionException("Unknown scope: " + this.dependencyScope);
            }
        }
    }

    private static boolean isNonEmpty(Set<?> set)
    {
        return (set != null) && !set.isEmpty();
    }

    private static URL toURL(File file)
    {
        try
        {
            return file.toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
