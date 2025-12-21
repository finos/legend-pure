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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.finos.legend.pure.m3.generator.bootstrap.M3CoreInstanceGenerator;
import org.finos.legend.pure.m3.generator.Log;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

@Mojo(name = "generate-m3-core-instances",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class M3CoreInstanceGeneratorMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter(property = "outputDir", required = true)
    private String outputDir;

    @Parameter(property = "factoryNamePrefix", required = true)
    private String factoryNamePrefix;

    @Parameter(property = "fileNameSet", required = true)
    private Set<String> fileNameSet;

    @Parameter(property = "fileNameStartsWith")
    private String fileNameStartsWith;

    @Parameter(defaultValue = "compile")
    private String dependencyScope;

    @Override
    public void execute() throws MojoExecutionException
    {
        Log log = new Log()
        {
            @Override
            public void debug(String txt)
            {
                getLog().debug(txt);
            }

            @Override
            public void info(String txt)
            {
                getLog().info(txt);
            }

            @Override
            public void error(String txt, Throwable e)
            {
                getLog().error(txt, e);
            }

        };

        if (skip)
        {
            log.info("    Skipping M3 core instance generation");
            return;
        }
        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(buildClassLoader(savedClassLoader, log));
            M3CoreInstanceGenerator.generate(outputDir, factoryNamePrefix, SetAdapter.adapt(fileNameSet), fileNameStartsWith);
        }
        catch (Exception e)
        {
            log.error("    Failed to generate M3 core instances", e);
            throw new MojoExecutionException("Failed to generate M3 core instances", e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

    private ClassLoader buildClassLoader(ClassLoader parent, Log log) throws MojoExecutionException
    {
        MutableList<String> classpathElements = Lists.mutable.empty();

        classpathElements.add(this.project.getBuild().getOutputDirectory());

        URL[] urlsForClassLoader = classpathElements.collect(mavenCompilePath ->
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
        URL[] combinedURLs = Stream.concat(Arrays.stream(urlsForClassLoader), Arrays.stream(getDependencyURLs()))
                .toArray(URL[]::new);
        log.debug("    Project classLoader URLs " + Arrays.toString(combinedURLs));
        return new URLClassLoader(combinedURLs, parent);
    }

    private static URL toURL(Artifact artifact)
    {
        try
        {
            return artifact.getFile().toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private URL[] getDependencyURLs() throws MojoExecutionException
    {
        Set<String> dependencyFilter = getDependencyFilter();
        return this.project.getArtifacts().stream()
                .filter(artifact -> dependencyFilter == null || dependencyFilter.contains(artifact.getScope()))
                .map(M3CoreInstanceGeneratorMojo::toURL)
                .toArray(URL[]::new);
    }

    private Set<String> getDependencyFilter() throws MojoExecutionException
    {
        switch (this.dependencyScope)
        {
            case "compile":
            {
                return Sets.mutable.with("compile", "provided", "system");
            }
            case "compile+runtime":
            {
                return Sets.mutable.with("compile", "provided", "runtime", "system");
            }
            case "runtime":
            {
                return Sets.mutable.with("compile", "runtime");
            }
            case "runtime+system":
            {
                return Sets.mutable.with("compile", "runtime", "system");
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
}
