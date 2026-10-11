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

package org.finos.legend.pure.rust.pct;

import junit.framework.TestSuite;
import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.pct.shared.model.PCTManifest;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

/**
 * Builds a {@code Rust_JNI} surveyor suite and prints its tally,
 * {@code total - excluded} passes.
 */
final class RustJniSuites
{
    private RustJniSuites()
    {
    }

    static TestSuite build(ReportScope reportScope, String manifestPath)
    {
        return tally(reportScope, manifestPath, "", PureTestBuilderInterpreted.buildPCTSurveyorSuite(reportScope, manifestPath));
    }

    static TestSuite buildCompiled(ReportScope reportScope, String manifestPath)
    {
        return tally(reportScope, manifestPath, " (compiled)", PureTestBuilderCompiled.buildPCTSurveyorSuite(reportScope, manifestPath));
    }

    private static TestSuite tally(ReportScope reportScope, String manifestPath, String platformTag, TestSuite suite)
    {
        PCTManifest manifest = PCTManifestLoader.loadFromClasspath(manifestPath);
        int total = suite.countTestCases();
        int excluded = (manifest.exclusions == null) ? 0 : manifest.exclusions.size();
        System.out.println("Rust_JNI " + reportScope.module + platformTag + ": " + (total - excluded) + " / " + total);
        return suite;
    }
}
