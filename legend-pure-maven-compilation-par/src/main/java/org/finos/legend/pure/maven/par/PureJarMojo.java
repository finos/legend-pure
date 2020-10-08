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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Procedures2;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m4.exception.PureException;

import java.io.File;

@Mojo(name = "build-pure-jar")
public class PureJarMojo extends AbstractMojo
{
    @Parameter
    private String purePlatformVersion;

    @Parameter
    private String pureModelVersion;

    @Parameter(readonly = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter
    private File sourceDirectory;

    @Parameter
    private String[] repositories;

    @Parameter
    private String[] excludedRepositories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        SetIterable<String> resolvedRepositories = resolveRepositories();

        getLog().info("Pure platform version: " + this.purePlatformVersion);
        getLog().info("Pure model version: " + this.pureModelVersion);
        getLog().info(resolvedRepositories.toSortedList().makeString("Repositories: ", ", ", ""));
        getLog().info("Pure source directory: " + this.sourceDirectory);
        getLog().info("Output directory: " + this.outputDirectory);

        if (resolvedRepositories.isEmpty())
        {
            getLog().info("No repositories to serialize: nothing to do");
            return;
        }

        getLog().info("Serializing Pure jars");
        long start = System.currentTimeMillis();
        try
        {
            PureJarSerializer.writePureRepositoryJars(this.outputDirectory.toPath(), (this.sourceDirectory == null) ? null : this.sourceDirectory.toPath(), this.purePlatformVersion, this.pureModelVersion, resolvedRepositories);
        }
        catch (PureException e)
        {
            getLog().error(String.format("Serialization of Pure jars failed (%.3fs)", (System.currentTimeMillis() - start) / 1000.0), e);
            throw new MojoFailureException(e.getInfo(), e);
        }
        catch (Exception e)
        {
            getLog().error(String.format("Serialization of Pure jars failed (%.3fs)", (System.currentTimeMillis() - start) / 1000.0), e);
            throw new MojoExecutionException("Error serializing Pure jars", e);
        }
        getLog().info(String.format("Serialization of Pure jars done (%.3fs)", (System.currentTimeMillis() - start) / 1000.0));
    }

    private SetIterable<String> resolveRepositories()
    {
        MutableSet<String> result = Sets.mutable.empty();

        if (this.repositories != null)
        {
            // If repositories are specified, add them
            ArrayIterate.forEachWith(this.repositories, Procedures2.<String>addToCollection(), result);
        }
        else
        {
            // If repositories are not specified, add all non-scratch repositories
            result.addAllIterable(PureRepositoriesExternal.repositories.collect(CodeRepository.GET_NAME));
        }

        if (this.excludedRepositories != null)
        {
            // Remove excluded repositories
            ArrayIterate.forEachWith(this.excludedRepositories, Procedures2.<String>removeFromCollection(), result);
        }

        return result;
    }
}
