// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.maven.shared;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Util class for implementing Mojos that need access to the dependencies resolved from the current Maven module.</p>
 *
 * <p>NOTE: in your {@code Mojo} annotation, you <i>must</i> set {@code Mojo#requiresDependencyResolution()} to
 * {@code ResolutionScope.TEST}. Set it to {@code TEST} even if you only intend to use
 * it for {@code COMPILE} - we do our own filtering of dependencies, for which all the
 * dependencies must be available from Maven.</p>
 *
 * <p>Adding dependencies (e.g. Pure dependencies) to the module's dependencies, rather than the plugin dependencies
 * is preferred because:
 * <ul>
 *     <li>it prevents Maven from having to create new {@link ClassRealm}s for
 *     each plugin, and</li>
 *     <li>it is arguably more readable and intuitive</li>
 * </ul>
 * </p>
 */
public class ProjectDependencyResolution
{
    private ProjectDependencyResolution()
    {
    }

    public static URL[] getDependencyURLs(
            DependencyResolutionScope dependencyResolutionScope,
            MavenProject mavenProject,
            MojoExecution mojoExecution,
            RepositorySystemSession mavenRepoSession,
            File projectOutputDirectory,
            File projectTestOutputDirectory,
            ProjectDependenciesResolver mavenProjectDependenciesResolver
    ) throws DependencyResolutionException
    {
        Objects.requireNonNull(mavenProject);
        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(mavenProject, mavenRepoSession)
                .setResolutionFilter(dependencyResolutionScope.getScopeDependencyFilter());

        List<File> classpath = new ArrayList<>();
        classpath.add(projectOutputDirectory);
        if (inTestPhase(mojoExecution) && dependencyResolutionScope.isTestScope())
        {
            classpath.add(projectTestOutputDirectory);
        }
        mavenProjectDependenciesResolver.resolve(request).getDependencies().forEach(dep -> classpath.add(dep.getArtifact().getFile()));
        return classpath.stream().map(ProjectDependencyResolution::toURL).toArray(URL[]::new);
    }

    public static DependencyResolutionScope determineDependencyResolutionScope(String scopeOverride, MojoExecution execution)
    {
        if (scopeOverride != null)
        {
            return DependencyResolutionScope.fromName(scopeOverride);
        }
        else
        {
            if (inTestPhase(execution))
            {
                return DependencyResolutionScope.TEST_RESOLUTION_SCOPE;
            }
            else
            {
                return DependencyResolutionScope.COMPILE_RESOLUTION_SCOPE;
            }
        }
    }

    public static boolean inTestPhase(MojoExecution mojoExecution)
    {
        switch (mojoExecution.getLifecyclePhase())
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
