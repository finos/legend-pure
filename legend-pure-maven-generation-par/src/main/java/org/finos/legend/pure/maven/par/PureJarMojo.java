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
import org.finos.legend.pure.m3.generator.par.Log;
import org.finos.legend.pure.m3.generator.par.PureJarGenerator;

import java.io.File;
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
        try
        {
            PureJarGenerator.doGeneratePAR(this.repositories,
                    this.excludedRepositories,
                    this.extraRepositories,
                    this.purePlatformVersion,
                    this.sourceDirectory,
                    this.outputDirectory,
                    new Log()
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
                    });
        }
        catch (Exception e)
        {
            String baseMessage = "Error serializing Pure PAR";
            String eMessage = e.getMessage();
            throw new MojoExecutionException((eMessage == null) ? baseMessage : (baseMessage + ": " + eMessage), e);
        }
    }


}
