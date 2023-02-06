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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.Log;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import static org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration.durationSinceInSeconds;

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
    private JavaCodeGeneration.GenerationType generationType;

    @Parameter(defaultValue = "false")
    private boolean useSingleDir;

    @Parameter(defaultValue = "false")
    private boolean generateSources;

    @Parameter(defaultValue = "false")
    private boolean preventJavaCompilation;

    @Override
    public void execute() throws MojoExecutionException
    {
        Log log = new Log()
        {
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

        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        long start = System.nanoTime();
        try
        {
            Thread.currentThread().setContextClassLoader(buildClassLoader(this.project, savedClassLoader, log));
            JavaCodeGeneration.doIt(repositories, excludedRepositories, extraRepositories, generationType, skip, generateMetadata, useSingleDir, generateSources, false, preventJavaCompilation, classesDirectory, targetDirectory, log);
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



    private ClassLoader buildClassLoader(MavenProject project, ClassLoader parent, Log log) throws DependencyResolutionRequiredException
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
        log.info("    Project classLoader URLs " + Arrays.toString(urlsForClassLoader));
        return new URLClassLoader(urlsForClassLoader, parent);
    }

}
