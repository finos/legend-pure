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

package org.finos.legend.pure.maven.pct;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.finos.legend.pure.maven.shared.MojoTestSupport;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Parameter-validation and skip-guard tests for {@link GeneratePCTFunctions}.
 *
 * <p>These tests verify that {@code execute()} enforces its required parameter
 * ({@code scopeProviderMethod}) and honours the {@code skip} flag before
 * attempting any classloader setup or reflection. The pattern mirrors
 * {@link TestGeneratePCTReport}.</p>
 */
public class TestGeneratePCTFunctions
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // --- scopeProviderMethod validation ---

    @Test
    public void testExecute_missingScopeProviderMethod_throwsMojoExecutionException() throws Exception
    {
        GeneratePCTFunctions mojo = buildMojo(null);

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException when scopeProviderMethod is null");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue(
                    "Exception message should mention 'scopeProviderMethod', but was: " + e.getMessage(),
                    e.getMessage().contains("scopeProviderMethod"));
        }
    }

    @Test
    public void testExecute_emptyScopeProviderMethod_throwsException() throws Exception
    {
        // assertPresentOrNotEmpty only rejects null or empty Collection — an empty String
        // passes that guard and proceeds to the classloader/reflection step, which fails.
        GeneratePCTFunctions mojo = buildMojo("");

        try
        {
            mojo.execute();
            Assert.fail("Expected an exception when scopeProviderMethod is empty");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertNotNull("MojoExecutionException should have a message", e.getMessage());
        }
        catch (RuntimeException e)
        {
            // Also acceptable — reflection failures may propagate as RuntimeException
            Assert.assertNotNull("RuntimeException should have a message", e.getMessage());
        }
    }

    // --- skip flag ---

    @Test
    public void testExecute_skip_preventsExecution() throws Exception
    {
        // With skip=true, execute() should return immediately without validating scopeProviderMethod
        GeneratePCTFunctions mojo = buildMojo(null);   // null would normally throw
        MojoTestSupport.setField(mojo, "skip", true);

        // Should NOT throw even though scopeProviderMethod is null
        mojo.execute();

        // Verify nothing was written to the target directory
        File[] files = tempFolder.getRoot().listFiles();
        int fileCount = (files == null) ? 0 : files.length;
        Assert.assertEquals("skip=true should produce no output files", 0, fileCount);
    }

    @Test
    public void testExecute_skipFalse_stillValidatesScopeProviderMethod() throws Exception
    {
        GeneratePCTFunctions mojo = buildMojo(null);
        MojoTestSupport.setField(mojo, "skip", false);

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException when skip=false and scopeProviderMethod is null");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue(e.getMessage().contains("scopeProviderMethod"));
        }
    }

    // --- classloader / reflection path ---

    @Test
    public void testExecute_unknownScopeProviderMethod_throwsWrappedMojoExecutionException() throws Exception
    {
        // A well-formed method reference that points to a non-existent class — passes the
        // assertPresentOrNotEmpty guard but fails during FunctionsGeneration.generateFunctions().
        // execute() must wrap the failure as MojoExecutionException with message starting
        // "Error generating PCT functions" and preserve the original cause.
        GeneratePCTFunctions mojo = buildMojo("org.finos.legend.does.not.Exist::generateScope");

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException for unknown scope provider method");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue(
                    "Message should start with 'Error generating PCT functions', but was: " + e.getMessage(),
                    e.getMessage().startsWith("Error generating PCT functions"));
            Assert.assertNotNull(
                    "MojoExecutionException must wrap the original cause",
                    e.getCause());
        }
    }

    // --- helpers ---

    private GeneratePCTFunctions buildMojo(String scopeProviderMethod) throws Exception
    {
        GeneratePCTFunctions mojo = new GeneratePCTFunctions();
        MojoTestSupport.setField(mojo, "project",             new MavenProject());
        MojoTestSupport.setField(mojo, "skip",                false);
        MojoTestSupport.setField(mojo, "scopeProviderMethod", scopeProviderMethod);
        MojoTestSupport.setField(mojo, "targetDir",           tempFolder.getRoot().getAbsolutePath());
        return mojo;
    }
}

