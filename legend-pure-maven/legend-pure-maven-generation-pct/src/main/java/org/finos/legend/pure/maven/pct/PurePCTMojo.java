// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.maven.pct;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PCTReportGenerator;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;


@Mojo(name = "generate-pct", threadSafe = true)
public class PurePCTMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(required = true)
    private Mode mode;

    @Parameter(required = true)
    private String targetDir;

    @Parameter(required = true)
    private Set<String> PCTTestSuites;

    @Override
    public void execute() throws MojoExecutionException
    {
        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            getLog().info("Generating PCT report");
            Thread.currentThread().setContextClassLoader(buildClassLoader(this.project, savedClassLoader, getLog()));
            if (mode.equals(Mode.Compiled))
            {
                PCTReportGenerator.generateCompiled(targetDir, Lists.mutable.withAll(PCTTestSuites));
            }
            else
            {
                org.finos.legend.pure.runtime.java.interpreted.testHelper.PCTReportGenerator.generateInterpreted(targetDir, Lists.mutable.withAll(PCTTestSuites));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

    public ClassLoader buildClassLoader(MavenProject project, ClassLoader parent, Log log) throws DependencyResolutionRequiredException
    {

        // Add the project output to the plugin classloader
        URL[] urlsForClassLoader =
                Lists.mutable
                        .withAll(project.getCompileClasspathElements())
                        .withAll(project.getTestClasspathElements())
                        .collect(mavenCompilePath ->
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
