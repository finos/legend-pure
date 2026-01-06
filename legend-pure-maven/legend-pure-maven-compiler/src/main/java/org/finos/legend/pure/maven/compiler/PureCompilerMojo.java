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
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    private static final String COMPILE_RESOLUTION_SCOPE = "compile";
    private static final String COMPILE_RUNTIME_RESOLUTION_SCOPE = "compile+runtime";
    private static final String RUNTIME_RESOLUTION_SCOPE = "runtime";
    private static final String RUNTIME_SYSTEM_RESOLUTION_SCOPE = "runtime+system";
    private static final String TEST_RESOLUTION_SCOPE = "test";

    @Parameter
    private File outputDirectory;

    @Parameter
    private Set<String> repositories;

    @Parameter
    private Set<String> excludedRepositories;

    @Parameter
    private String dependencyScope;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File projectOutputDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.testOutputDirectory}")
    private File projectTestOutputDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${mojoExecution}")
    private MojoExecution execution;

    @Component
    private ProjectDependenciesResolver mavenProjectDependenciesResolver;

    @Parameter(readonly = true, required = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession mavenRepoSession;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Compiling Pure");
        File resolvedOutputDir = resolveOutputDirectory();
        getLog().debug("Output directory: " + resolvedOutputDir);
        getLog().debug("Requested repositories: " + (isNonEmpty(this.repositories) ? String.join(", ", this.repositories) : "<none>"));
        getLog().debug("Excluded repositories: " + (isNonEmpty(this.excludedRepositories) ? String.join(", ", this.excludedRepositories) : "<none>"));
        String resolvedDependencyScope = resolveDependencyScope();
        getLog().debug("Dependency scope: " + resolvedDependencyScope);

        Set<String> resolvedRepos = resolveRepositoriesToSerialize(resolvedDependencyScope);
        if ((resolvedRepos != null) && resolvedRepos.isEmpty())
        {
            getLog().warn("No repositories to serialize");
            return;
        }
        getLog().debug("Resolved repositories: " + ((resolvedRepos == null) ? "<all>" : String.join(", ", resolvedRepos)));

        try (URLClassLoader classLoader = new URLClassLoader(getDependencyURLs(resolvedDependencyScope), Thread.currentThread().getContextClassLoader()))
        {
            PureCompilerBinaryGenerator.serializeModules(resolvedOutputDir.toPath(), classLoader, resolvedRepos, this.excludedRepositories);
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

    private File resolveOutputDirectory()
    {
        if (this.outputDirectory != null)
        {
            return this.outputDirectory;
        }
        if (inTestPhase())
        {
            return this.projectTestOutputDirectory;
        }
        return this.projectOutputDirectory;
    }

    private String resolveDependencyScope()
    {
        if (this.dependencyScope != null)
        {
            return this.dependencyScope;
        }
        if (inTestPhase())
        {
            return TEST_RESOLUTION_SCOPE;
        }
        return COMPILE_RESOLUTION_SCOPE;
    }

    private Set<String> resolveRepositoriesToSerialize(String resolvedDependencyScope) throws MojoExecutionException
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
        if (inTestPhase() && isTestDependencyScope(resolvedDependencyScope))
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

    private URL[] getDependencyURLs(String resolvedDependencyScope) throws DependencyResolutionException, MojoExecutionException
    {
        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(this.mavenProject, this.mavenRepoSession)
                .setResolutionFilter(getDependencyFilter(resolvedDependencyScope));
        List<File> classpath = new ArrayList<>();
        classpath.add(this.projectOutputDirectory);
        if (inTestPhase() && isTestDependencyScope(resolvedDependencyScope))
        {
            classpath.add(this.projectTestOutputDirectory);
        }
        this.mavenProjectDependenciesResolver.resolve(request).getDependencies().forEach(dep -> classpath.add(dep.getArtifact().getFile()));
        return classpath.stream().map(PureCompilerMojo::toURL).toArray(URL[]::new);
    }

    private DependencyFilter getDependencyFilter(String resolvedDependencyScope) throws MojoExecutionException
    {
        switch (resolvedDependencyScope)
        {
            case COMPILE_RESOLUTION_SCOPE:
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "provided", "system"), null);
            }
            case COMPILE_RUNTIME_RESOLUTION_SCOPE:
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "provided", "runtime", "system"), null);
            }
            case RUNTIME_RESOLUTION_SCOPE:
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "runtime"), null);
            }
            case RUNTIME_SYSTEM_RESOLUTION_SCOPE:
            {
                return new ScopeDependencyFilter(Arrays.asList("compile", "runtime", "system"), null);
            }
            case TEST_RESOLUTION_SCOPE:
            {
                return null;
            }
            default:
            {
                throw new MojoExecutionException("Unknown scope: " + this.dependencyScope);
            }
        }
    }

    private boolean inTestPhase()
    {
        switch (this.execution.getLifecyclePhase())
        {
            case "test-compile":
            case "process-test-classes":
            case "test":
            {
                return true;
            }
            default:
            {
                return false;
            }
        }
    }

    private static boolean isTestDependencyScope(String dependencyScope)
    {
        return TEST_RESOLUTION_SCOPE.equals(dependencyScope);
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
