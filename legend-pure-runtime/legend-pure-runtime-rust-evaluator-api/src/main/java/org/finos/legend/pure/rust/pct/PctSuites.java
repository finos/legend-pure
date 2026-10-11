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
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.pct.shared.model.PCTManifest;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

/**
 * Builds a PCT surveyor suite for a Rust adapter on either Java engine and
 * prints its tally, {@code <adapter> <scope>[ (compiled)]: <total - excluded> / <total>}.
 */
public final class PctSuites
{
    private PctSuites()
    {
    }

    /**
     * The corpus of a scope through the interpreted engine.
     *
     * @param adapter      the Rust adapter the tests run under
     * @param reportScope  the corpus
     * @param manifestPath the classpath path of the adapter's manifest for that scope
     * @return the suite
     */
    public static TestSuite interpreted(Adapter adapter, ReportScope reportScope, String manifestPath)
    {
        return tally(adapter, reportScope, manifestPath, "", PureTestBuilderInterpreted.buildPCTSurveyorSuite(reportScope, manifestPath));
    }

    /**
     * The corpus of a scope through the compiled engine.
     *
     * @param adapter      the Rust adapter the tests run under
     * @param reportScope  the corpus
     * @param manifestPath the classpath path of the adapter's manifest for that scope
     * @return the suite
     */
    public static TestSuite compiled(Adapter adapter, ReportScope reportScope, String manifestPath)
    {
        return tally(adapter, reportScope, manifestPath, " (compiled)", PureTestBuilderCompiled.buildPCTSurveyorSuite(reportScope, manifestPath));
    }

    private static TestSuite tally(Adapter adapter, ReportScope reportScope, String manifestPath, String platformTag, TestSuite suite)
    {
        PCTManifest manifest = PCTManifestLoader.loadFromClasspath(manifestPath);
        int total = suite.countTestCases();
        int excluded = (manifest.exclusions == null) ? 0 : manifest.exclusions.size();
        System.out.println(adapter.name + " " + reportScope.module + platformTag + ": " + (total - excluded) + " / " + total);
        return suite;
    }
}
