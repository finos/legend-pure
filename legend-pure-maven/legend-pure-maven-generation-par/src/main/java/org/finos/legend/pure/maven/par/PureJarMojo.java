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

package org.finos.legend.pure.maven.par;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.finos.legend.pure.m3.generator.Log;
import org.finos.legend.pure.m3.generator.par.PureJarGenerator;
import org.finos.legend.pure.maven.shared.DependencyResolutionScope;
import org.finos.legend.pure.maven.shared.ProjectDependencyResolution;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

@Mojo(
        name = "build-pure-jar",
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class PureJarMojo extends AbstractMojo
{
    @Parameter
    private String purePlatformVersion;

    @Parameter
    private String modelVersion;

    @Parameter(readonly = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter
    private File sourceDirectory;

    @Parameter
    private Set<String> repositories;

    @Parameter
    private Set<String> excludedRepositories;

    @Parameter
    private Set<String> extraRepositories;

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

    @Parameter(readonly = true, required = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession mavenRepoSession;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    @Component
    private ProjectDependenciesResolver mavenProjectDependenciesResolver;

    @Override
    public void execute() throws MojoExecutionException
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
                this.executeWithinClassLoader(classLoader);
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

    private void executeWithinClassLoader(ClassLoader classLoader) throws MojoExecutionException
    {
        try
        {
            PureJarGenerator.doGeneratePAR(
                    new PureJarGenerator.ParGenerateParams(this.repositories, this.excludedRepositories, this.extraRepositories, this.purePlatformVersion, this.modelVersion, this.sourceDirectory, this.outputDirectory, new Log()
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
                    }));
        }
        catch (Exception e)
        {
            String baseMessage = "Error serializing Pure PAR";
            String eMessage = e.getMessage();
            throw new MojoExecutionException((eMessage == null) ? baseMessage : (baseMessage + ": " + eMessage), e);
        }
    }
}
