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
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

import java.io.File;
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
            CodeRepositorySet resolvedRepositories = resolveRepositories();
            getLog().info("  Repositories with resolved dependencies: " + resolvedRepositories.getRepositories());
            getLog().info("  Pure platform version: " + this.purePlatformVersion);
            getLog().info("  Pure source directory: " + this.sourceDirectory);
            getLog().info("  Output directory: " + this.outputDirectory);

            getLog().info("  Starting compilation and generation of Pure PAR file(s)");
            PureJarSerializer.writePureRepositoryJars(this.outputDirectory.toPath(), (this.sourceDirectory == null) ? null : this.sourceDirectory.toPath(), this.purePlatformVersion, resolvedRepositories, getLog());
        }
        catch (Exception e)
        {
            getLog().error(String.format("  -> Pure PAR generation failed (%.9fs)", durationSinceInSeconds(start)), e);
            String baseMessage = "Error serializing Pure PAR";
            String eMessage = e.getMessage();
            throw new MojoExecutionException((eMessage == null) ? baseMessage : (baseMessage + ": " + eMessage), e);
        }
        getLog().info(String.format("  -> Finished Pure PAR generation in %.9fs", durationSinceInSeconds(start)));
    }

    private CodeRepositorySet resolveRepositories()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        if (this.extraRepositories != null)
        {
            this.extraRepositories.forEach(r -> builder.addCodeRepository(getExtraRepository(classLoader, r)));
        }
        if (this.excludedRepositories != null)
        {
            builder.withoutCodeRepositories(this.excludedRepositories);
        }
        CodeRepositorySet repositories = builder.build();
        return ((this.repositories == null) || this.repositories.isEmpty()) ? repositories : repositories.subset(this.repositories);
    }

    private GenericCodeRepository getExtraRepository(ClassLoader classLoader, String extraRepository)
    {
        // First check if this is a resource
        URL url = classLoader.getResource(extraRepository);
        if (url != null)
        {
            try
            {
                return GenericCodeRepository.build(url);
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
