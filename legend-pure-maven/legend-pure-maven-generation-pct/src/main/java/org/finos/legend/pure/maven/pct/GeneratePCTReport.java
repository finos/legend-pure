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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PCTReportGenerator;

import java.util.Set;

import static org.finos.legend.pure.maven.pct.Shared.assertPresentOrNotEmpty;


@Mojo(name = "generate-pct-report", threadSafe = true)
public class GeneratePCTReport extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter()
    private Mode mode;

    @Parameter(required = true)
    private String targetDir;

    @Parameter()
    private Set<String> PCTTestSuites;

    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().info("Generating PCT report");
        assertPresentOrNotEmpty("mode", mode);
        assertPresentOrNotEmpty("PCTTestSuites", PCTTestSuites);
        for (String testClass : PCTTestSuites) 
        {
            try 
                {
                Thread.currentThread().getContextClassLoader().loadClass(testClass);
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException("PCT test class not found: " + testClass);
            }
        }
        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(Shared.buildClassLoader(this.project, savedClassLoader, getLog()));

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
}
