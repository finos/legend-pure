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
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.finos.legend.pure.maven.shared.DependencyResolutionScope;
import org.finos.legend.pure.maven.shared.ProjectDependencyResolution;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.Log;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import static org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration.durationSinceInSeconds;

@Mojo(name = "build-pure-compiled-jar", threadSafe = true)
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

    @Parameter(defaultValue = "false")
    private boolean addExternalAPI;

    @Parameter(defaultValue = "org.finos.legend.pure.generated")
    private String externalAPIPackage;

    @Parameter(defaultValue = "true")
    private boolean generateMetadata;

    @Parameter(defaultValue = "monolithic")
    private JavaCodeGeneration.GenerationType generationType;

    @Parameter(defaultValue = "false")
    private boolean useSingleDir;

    @Parameter(defaultValue = "false")
    private boolean generateSources;

    @Parameter(defaultValue = "false")
    private boolean preventJavaCompilation;

    @Parameter(defaultValue = "true")
    private boolean generatePureTests;

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

    @Parameter(readonly = true, required = true, defaultValue = "${mojoExecution}")
    private MojoExecution mojoExecution;

    @Parameter(readonly = true, required = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession mavenRepoSession;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File projectOutputDirectory;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.testOutputDirectory}")
    private File projectTestOutputDirectory;

    @Component
    private ProjectDependenciesResolver mavenProjectDependenciesResolver;

    @Override
    public void execute() throws MojoExecutionException
    {
        long start = System.nanoTime();

        URL[] dependencyUrls;
        try
        {
            DependencyResolutionScope dependencyResolutionScope = ProjectDependencyResolution.determineDependencyResolutionScope(this.dependencyScope, this.mojoExecution);
            dependencyUrls = ProjectDependencyResolution.getDependencyURLs(
                    dependencyResolutionScope,
                    this.mavenProject,
                    this.mojoExecution,
                    this.mavenRepoSession,
                    this.projectOutputDirectory,
                    this.projectTestOutputDirectory,
                    this.mavenProjectDependenciesResolver
            );
        }
        catch (DependencyResolutionException e)
        {
            throw new MojoExecutionException("Error setting up classloader with project dependencies", e);
        }

        Log log = buildLog();

        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader cl = new URLClassLoader(dependencyUrls, savedClassLoader))
        {
            Thread.currentThread().setContextClassLoader(cl);
            JavaCodeGeneration.doIt(this.repositories, this.excludedRepositories, this.extraRepositories, this.generationType, this.skip, this.addExternalAPI, this.externalAPIPackage, this.generateMetadata, this.useSingleDir, this.generateSources, false, this.preventJavaCompilation, this.classesDirectory, this.targetDirectory, this.generatePureTests, log);
        }
        catch (Exception e)
        {
            log.error(String.format("    Error (%.9fs)", durationSinceInSeconds(start)), e);
            log.error(String.format("    FAILURE building Pure compiled mode jar (%.9fs)", durationSinceInSeconds(start)));
            throw new MojoExecutionException("Error building Pure compiled mode jar", e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

    private Log buildLog()
    {
        return new Log()
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
            public void error(String txt, Exception e)
            {
                getLog().error(txt, e);
            }

            @Override
            public void error(String format)
            {
                getLog().error(format);
            }

            @Override
            public void warn(String s)
            {
                getLog().warn(s);
            }
        };
    }
}
