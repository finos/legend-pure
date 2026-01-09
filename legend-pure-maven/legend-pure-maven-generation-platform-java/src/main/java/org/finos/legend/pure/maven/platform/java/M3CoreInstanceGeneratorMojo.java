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

package org.finos.legend.pure.maven.platform.java;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
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
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.finos.legend.pure.m3.generator.bootstrap.M3CoreInstanceGenerator;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Mojo(name = "generate-m3-core-instances",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class M3CoreInstanceGeneratorMojo extends AbstractMojo
{
    private static final String COMPILE_RESOLUTION_SCOPE = "compile";
    private static final String COMPILE_RUNTIME_RESOLUTION_SCOPE = "compile+runtime";
    private static final String RUNTIME_RESOLUTION_SCOPE = "runtime";
    private static final String RUNTIME_SYSTEM_RESOLUTION_SCOPE = "runtime+system";
    private static final String TEST_RESOLUTION_SCOPE = "test";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter
    private File outputDir;

    @Parameter(defaultValue = "true")
    private boolean addOutputDirectoryAsSource;

    @Parameter(property = "factoryNamePrefix", required = true)
    private String factoryNamePrefix;

    @Parameter(property = "fileNameSet", required = true)
    private Set<String> fileNameSet;

    @Parameter(property = "fileNameStartsWith")
    private String fileNameStartsWith;

    @Parameter(defaultValue = "compile")
    private String dependencyScope;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    private MojoExecution execution;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}")
    private File projectBuildDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File projectOutputDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.testOutputDirectory}")
    private File projectTestOutputDirectory;

    @Component
    private ProjectDependenciesResolver mavenProjectDependenciesResolver;

    @Parameter(readonly = true, required = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession mavenRepoSession;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (this.skip)
        {
            getLog().info("Skipping M3 core instance generation");
            return;
        }

        Path resolvedOutputDir = resolveOutputDirectory();
        String resolvedDependencyScope = resolveDependencyScope();

        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader classLoader = new URLClassLoader(getDependencyURLs(resolvedDependencyScope), savedClassLoader))
        {
            Thread.currentThread().setContextClassLoader(classLoader);
            M3CoreInstanceGenerator.generate(resolvedOutputDir.toString() + resolvedOutputDir.getFileSystem().getSeparator(), this.factoryNamePrefix, SetAdapter.adapt(this.fileNameSet), this.fileNameStartsWith);
        }
        catch (Exception e)
        {
            getLog().error("Failed to generate M3 core instances", e);
            throw new MojoExecutionException("Failed to generate M3 core instances", e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }

        if (this.addOutputDirectoryAsSource)
        {
            String newSourceDirectory = resolvedOutputDir.toAbsolutePath().toString();
            this.project.addCompileSourceRoot(newSourceDirectory);
            getLog().debug("Added source directory: " + newSourceDirectory);
        }
    }

    private URL[] getDependencyURLs(String resolvedDependencyScope) throws DependencyResolutionException, MojoExecutionException
    {
        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(this.project, this.mavenRepoSession)
                .setResolutionFilter(getDependencyFilter(resolvedDependencyScope));
        List<File> classpath = new ArrayList<>();
        classpath.add(this.projectOutputDirectory);
        if (inTestPhase() && isTestDependencyScope(resolvedDependencyScope))
        {
            classpath.add(this.projectTestOutputDirectory);
        }
        this.mavenProjectDependenciesResolver.resolve(request).getDependencies().forEach(dep -> classpath.add(dep.getArtifact().getFile()));
        URL[] urls = classpath.stream().map(M3CoreInstanceGeneratorMojo::toURL).toArray(URL[]::new);
        getLog().debug("Dependency URLs: " + Arrays.toString(urls));
        return urls;
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

    private Path resolveOutputDirectory()
    {
        return (this.outputDir == null) ?
               this.projectBuildDirectory.toPath().resolve(inTestPhase() ? "generated-test-sources" : "generated-sources") :
               this.outputDir.toPath();
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
