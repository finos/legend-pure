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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m4.exception.PureException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
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
    private String[] repositories;

    @Parameter
    private String[] excludedRepositories;

    @Parameter
    private String[] extraRepositories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Generating Pure PAR file(s)");
        getLog().info("  Requested repositories: " + Arrays.toString(this.repositories));
        getLog().info("  Excluded repositories: " + Arrays.toString(this.excludedRepositories));
        getLog().info("  Extra repositories: " + Arrays.toString(this.extraRepositories));
        RichIterable<CodeRepository> resolvedRepositories = resolveRepositories(this.repositories, this.excludedRepositories, this.extraRepositories);
        getLog().info("  Repositories with resolved dependencies: "+resolvedRepositories);
        getLog().info("  Pure platform version: " + this.purePlatformVersion);
        getLog().info("  Pure source directory: " + this.sourceDirectory);
        getLog().info("  Output directory: " + this.outputDirectory);

        if (resolvedRepositories.isEmpty())
        {
            getLog().info("   -> No repositories to serialize: nothing to do");
            return;
        }

        getLog().info("  Starting compilation and generation of Pure PAR file(s)");
        long start = System.currentTimeMillis();
        try
        {
            PureJarSerializer.writePureRepositoryJars(this.outputDirectory.toPath(), (this.sourceDirectory == null) ? null : this.sourceDirectory.toPath(), this.purePlatformVersion, resolvedRepositories, getLog());
        }
        catch (PureException e)
        {
            getLog().error(String.format("  -> Pure PAR generation failed (%.3fs)", (System.currentTimeMillis() - start) / 1000.0), e);
            throw new MojoFailureException(e.getInfo(), e);
        }
        catch (Exception e)
        {
            getLog().error(String.format("  -> Pure PAR generation failed (%.3fs)", (System.currentTimeMillis() - start) / 1000.0), e);
            throw new MojoExecutionException("Error serializing Pure PAR", e);
        }
        getLog().info(String.format("  -> Finished Pure PAR generation in %.3fs", (System.currentTimeMillis() - start) / 1000.0));
    }

    private RichIterable<CodeRepository> resolveRepositories(String[] repositories, String[] excludedRepositories, String[] extraRepositories)
    {
        PureRepositoriesExternal.refresh();

        getLog().info("  Repositories found: " + PureRepositoriesExternal.repositories());

        if (extraRepositories != null)
        {
            RichIterable<CodeRepository> resolvedRepositories = ArrayIterate.collect(extraRepositories, r -> {
                try
                {
                    return GenericCodeRepository.build(new FileInputStream(r));
                }
                catch (FileNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            });
            PureRepositoriesExternal.addRepositories(resolvedRepositories);
        }

        RichIterable<CodeRepository> selectedRepos = repositories == null?PureRepositoriesExternal.repositories():ArrayIterate.collect(repositories, PureRepositoriesExternal::getRepository);
        Set<String> excludedRepositoriesSet = Sets.mutable.of(excludedRepositories == null?new String[0]:excludedRepositories);
        return selectedRepos.select(r -> !excludedRepositoriesSet.contains(r.getName()));
    }

}
