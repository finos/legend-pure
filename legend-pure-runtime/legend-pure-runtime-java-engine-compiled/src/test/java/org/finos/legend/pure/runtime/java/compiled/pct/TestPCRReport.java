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

package org.finos.legend.pure.runtime.java.compiled.pct;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderLoader;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PCTReportGenerator;
import org.junit.Assert;
import org.junit.Test;

public class TestPCRReport
{
    @Test
    public void canFindPCTReport() throws Exception
    {
        MutableList<AdapterReport> adapterReports = PCTReportProviderLoader.gatherReports();
        Assert.assertEquals("Native", adapterReports.collect(c -> c.adapterKey.adapter.name).distinct().sortThis().makeString(", "));
        Assert.assertEquals(Lists.fixedSize.of("essential", "grammar", "variant"), adapterReports.collect(x -> x.reportScope.module).sortThis());
        Assert.assertTrue(adapterReports.getAny().functionTests.stream().flatMap(x -> x.tests.stream()).anyMatch(x -> x.qualifiers.contains("variant")));
    }
}
