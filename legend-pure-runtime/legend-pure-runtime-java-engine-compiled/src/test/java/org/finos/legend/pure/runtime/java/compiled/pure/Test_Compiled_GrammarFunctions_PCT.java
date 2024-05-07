// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.pure;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.config.PCTReport;
import org.finos.legend.pure.m3.pct.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled.getClassLoaderExecutionSupport;

public class Test_Compiled_GrammarFunctions_PCT extends PCTReport
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final String adapter = "meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_";
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "Assert failure"),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "Assert failure"),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "Assert failure")
    );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
    }

    @Override
    public String getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
