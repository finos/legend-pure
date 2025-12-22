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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.impl.factory.Lists;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

public class Shared
{
    public static ClassLoader buildClassLoader(MavenProject project, ClassLoader parent, Log log) throws DependencyResolutionRequiredException
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

        log.debug("    Project classLoader URLs " + Arrays.toString(urlsForClassLoader));
        return new URLClassLoader(urlsForClassLoader, parent);
    }

    public static void assertPresentOrNotEmpty(String name, Object value) throws MojoExecutionException
    {
        if (value == null || (value instanceof Collection && ((Collection<?>) value).isEmpty()))
        {
            throw new MojoExecutionException("The property " + name + " must be defined");
        }
    }
}
