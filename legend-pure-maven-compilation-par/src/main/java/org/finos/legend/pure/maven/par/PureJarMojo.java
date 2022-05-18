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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;

@Mojo(name = "build-pure-jar")
public class PureJarMojo extends AbstractMojo
{
    @Parameter
    private String purePlatformVersion;

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

    @Override
    public void execute() throws MojoExecutionException
    {
        long start = System.nanoTime();
        try
        {
            getLog().info("Generating Pure PAR file(s)");
            getLog().info("  Requested repositories: " + this.repositories);
            getLog().info("  Excluded repositories: " + this.excludedRepositories);
            getLog().info("  Extra repositories: " + this.extraRepositories);
            ListIterable<CodeRepository> resolvedRepositories = resolveRepositories();
            getLog().info("  Repositories with resolved dependencies: " + resolvedRepositories);
            getLog().info("  Pure platform version: " + this.purePlatformVersion);
            getLog().info("  Pure source directory: " + this.sourceDirectory);
            getLog().info("  Output directory: " + this.outputDirectory);

            if (resolvedRepositories.isEmpty())
            {
                getLog().info("   -> No repositories to serialize: nothing to do");
                return;
            }

            getLog().info("  Starting compilation and generation of Pure PAR file(s)");
            PureJarSerializer.writePureRepositoryJars(this.outputDirectory.toPath(), (this.sourceDirectory == null) ? null : this.sourceDirectory.toPath(), this.purePlatformVersion, resolvedRepositories, getLog());
        }
        catch (Exception e)
        {
            getLog().error(String.format("  -> Pure PAR generation failed (%.9fs)", durationSinceInSeconds(start)), e);
            throw new MojoExecutionException("Error serializing Pure PAR", e);
        }
        getLog().info(String.format("  -> Finished Pure PAR generation in %.9fs", durationSinceInSeconds(start)));
    }

    private ListIterable<CodeRepository> resolveRepositories()
    {
        PureRepositoriesExternal.refresh();

        if ((this.extraRepositories != null) && !this.extraRepositories.isEmpty())
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            PureRepositoriesExternal.addRepositories(Iterate.collect(this.extraRepositories, r -> getExtraRepository(classLoader, r), Lists.mutable.withInitialCapacity(this.extraRepositories.size())));
        }

        MutableList<CodeRepository> selectedRepos = (this.repositories == null) ?
                PureRepositoriesExternal.repositories().toList() :
                Iterate.collect(this.repositories, PureRepositoriesExternal::getRepository, Lists.mutable.withInitialCapacity(this.repositories.size()));
        if ((this.excludedRepositories != null) && !this.excludedRepositories.isEmpty())
        {
            selectedRepos.removeIf(r -> this.excludedRepositories.contains(r.getName()));
        }
        return selectedRepos;
    }

    private GenericCodeRepository getExtraRepository(ClassLoader classLoader, String extraRepository)
    {
        // First check if this is a resource
        URL url = classLoader.getResource(extraRepository);
        if (url != null)
        {
            try (InputStream stream = url.openStream())
            {
                return GenericCodeRepository.build(stream);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error loading extra repository \"" + extraRepository + "\" from resource " + url, e);
            }
        }

        // If it's not a resource, assume it is a file path
        try
        {
            return GenericCodeRepository.build(Paths.get(extraRepository));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error loading extra repository \"" + extraRepository + "\"", e);
        }
    }

    private double durationSinceInSeconds(long startNanos)
    {
        return durationInSeconds(startNanos, System.nanoTime());
    }

    private double durationInSeconds(long startNanos, long endNanos)
    {
        return (endNanos - startNanos) / 1_000_000_000.0;
    }
}
