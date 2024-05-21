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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.finos.legend.pure.m3.pct.functions.generation.FunctionsGeneration;

import static org.finos.legend.pure.maven.pct.Shared.assertPresentOrNotEmpty;

@Mojo(name = "generate-pct-functions", threadSafe = true)
public class GeneratePCTFunctions extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter()
    private String scopeProviderMethod;

    @Parameter(required = true)
    private String targetDir;

    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().info("Generating PCT functions");
        assertPresentOrNotEmpty("scopeProviderMethod", scopeProviderMethod);

        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(Shared.buildClassLoader(this.project, savedClassLoader, getLog()));
            FunctionsGeneration.generateFunctions(targetDir, scopeProviderMethod);
        }
        catch (Exception e)
        {
            getLog().error(e);
            throw new RuntimeException(e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

}
