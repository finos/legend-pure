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
import java.util.Collections;

/**
 * Parameter-validation tests for {@link GeneratePCTReport}.
 *
 * <p>These tests verify that {@code execute()} enforces its required parameters
 * ({@code mode} and {@code PCTTestSuites}) before attempting any class loading
 * or generation. The tests exercise the {@code assertPresentOrNotEmpty} guard
 * paths in {@code execute()}, which are the cheapest code paths to test
 * without standing up a full PCT runtime.</p>
 */
public class TestGeneratePCTReport
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // --- mode validation ---

    @Test
    public void testExecute_missingMode_throwsMojoExecutionException() throws Exception
    {
        GeneratePCTReport mojo = buildMojo(null, Collections.singleton("some.TestClass"));

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException when mode is null");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue("Exception message should mention 'mode', but was: " + e.getMessage(),
                    e.getMessage().contains("mode"));
        }
    }

    // --- PCTTestSuites validation ---

    @Test
    public void testExecute_missingPCTTestSuites_throwsMojoExecutionException() throws Exception
    {
        GeneratePCTReport mojo = buildMojo(Mode.Compiled, null);

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException when PCTTestSuites is null");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue("Exception message should mention 'PCTTestSuites', but was: " + e.getMessage(),
                    e.getMessage().contains("PCTTestSuites"));
        }
    }

    @Test
    public void testExecute_emptyPCTTestSuites_throwsMojoExecutionException() throws Exception
    {
        GeneratePCTReport mojo = buildMojo(Mode.Compiled, Collections.emptySet());

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException when PCTTestSuites is empty");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue("Exception message should mention 'PCTTestSuites', but was: " + e.getMessage(),
                    e.getMessage().contains("PCTTestSuites"));
        }
    }

    // --- class loading validation ---

    @Test
    public void testExecute_unknownTestClass_throwsWrappedMojoExecutionException() throws Exception
    {
        // Both required parameters present, but the class doesn't exist on the classpath.
        // execute() should throw MojoExecutionException wrapping ClassNotFoundException.
        File targetDir = tempFolder.newFolder("pct-report");
        GeneratePCTReport mojo = buildMojo(Mode.Compiled,
                Collections.singleton("org.finos.legend.does.not.Exist"));
        setField(mojo, "targetDir", targetDir.getAbsolutePath());

        try
        {
            mojo.execute();
            Assert.fail("Expected exception for unknown test class");
        }
        catch (MojoExecutionException e)
        {
            // Expected: "PCT test class not found" or wrapped ClassNotFoundException
            Assert.assertNotNull("Exception should have a message", e.getMessage());
        }
        catch (RuntimeException e)
        {
            // Also acceptable — GeneratePCTReport wraps exceptions in RuntimeException
            Assert.assertNotNull("RuntimeException should have a message", e.getMessage());
        }
    }

    @Test
    public void testExecute_bothModeAndSuitesMissing_throwsMojoExecutionException() throws Exception
    {
        // mode is checked first; verify it is the first guard hit
        GeneratePCTReport mojo = buildMojo(null, null);

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException");
        }
        catch (MojoExecutionException e)
        {
            // mode is validated before PCTTestSuites in execute()
            Assert.assertTrue("First guard is 'mode'; message should mention it. Was: " + e.getMessage(),
                    e.getMessage().contains("mode"));
        }
    }

    // --- skip parameter ---

    @Test
    public void testExecute_skip_preventsExecution() throws Exception
    {
        // With skip=true, execute() should return immediately without validating mode/suites
        GeneratePCTReport mojo = new GeneratePCTReport();
        setField(mojo, "project",       new MavenProject());
        setField(mojo, "mode",          null);          // would normally throw
        setField(mojo, "PCTTestSuites", null);          // would normally throw
        setField(mojo, "targetDir",     tempFolder.getRoot().getAbsolutePath());
        setField(mojo, "skip",          true);

        // Should NOT throw even though mode and PCTTestSuites are null
        mojo.execute();

        // Verify the targetDir is still empty (nothing was generated)
        File[] files = tempFolder.getRoot().listFiles();
        int fileCount = (files == null) ? 0 : files.length;
        Assert.assertEquals("skip=true should produce no output files", 0, fileCount);
    }

    @Test
    public void testExecute_skipFalse_stillValidatesMode() throws Exception
    {
        GeneratePCTReport mojo = buildMojo(null, Collections.singleton("some.Class"));
        setField(mojo, "skip", false);

        try
        {
            mojo.execute();
            Assert.fail("Expected MojoExecutionException when mode is null and skip=false");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertTrue(e.getMessage().contains("mode"));
        }
    }

    // --- modes ---

    @Test
    public void testExecute_unknownTestClass_compiled_throwsMojoExecutionException() throws Exception
    {
        File targetDir = tempFolder.newFolder("pct-compiled");
        GeneratePCTReport mojo = buildMojo(Mode.Compiled,
                Collections.singleton("org.finos.legend.does.not.Exist"));
        setField(mojo, "targetDir", targetDir.getAbsolutePath());

        try
        {
            mojo.execute();
            Assert.fail("Expected exception");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertNotNull(e.getMessage());
        }
        catch (RuntimeException e)
        {
            Assert.fail("Should throw MojoExecutionException, not RuntimeException: " + e);
        }
    }

    @Test
    public void testExecute_unknownTestClass_interpreted_throwsMojoExecutionException() throws Exception
    {
        File targetDir = tempFolder.newFolder("pct-interpreted");
        GeneratePCTReport mojo = buildMojo(Mode.Interpreted,
                Collections.singleton("org.finos.legend.does.not.Exist"));
        setField(mojo, "targetDir", targetDir.getAbsolutePath());

        try
        {
            mojo.execute();
            Assert.fail("Expected exception");
        }
        catch (MojoExecutionException e)
        {
            Assert.assertNotNull(e.getMessage());
        }
        catch (RuntimeException e)
        {
            Assert.fail("Should throw MojoExecutionException, not RuntimeException: " + e);
        }
    }

    // --- helpers ---

    private GeneratePCTReport buildMojo(Mode mode, java.util.Set<String> pctTestSuites) throws Exception
    {
        GeneratePCTReport mojo = new GeneratePCTReport();
        MojoTestSupport.setField(mojo, "project",       new MavenProject());
        MojoTestSupport.setField(mojo, "mode",          mode);
        MojoTestSupport.setField(mojo, "PCTTestSuites", pctTestSuites);
        MojoTestSupport.setField(mojo, "targetDir",     tempFolder.getRoot().getAbsolutePath());
        return mojo;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        MojoTestSupport.setField(target, fieldName, value);
    }
}

