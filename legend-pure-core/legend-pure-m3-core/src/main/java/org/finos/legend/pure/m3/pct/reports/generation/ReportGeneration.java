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

package org.finos.legend.pure.m3.pct.reports.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.model.AdapterKey;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;
import org.finos.legend.pure.m3.pct.reports.model.FunctionTestResults;
import org.finos.legend.pure.m3.pct.reports.model.TestInfo;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.pct.shared.generation.Shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

public class ReportGeneration
{
    public static MutableList<AdapterReport> generateReport(List<String> testSuites, ProcessorSupport processorSupport)
    {
        return ListIterate.collect(testSuites, suiteClass ->
        {
            try
            {
                PCTReportConfiguration reportManager = (PCTReportConfiguration) Thread.currentThread().getContextClassLoader().loadClass(suiteClass).newInstance();
                return generateReport(TestCollection.buildPCTTestCollection(reportManager.getReportScope()._package, reportManager.getReportScope().filePath, processorSupport), reportManager, processorSupport);
            }
            catch (Exception e)
            {
                StringBuilder builder = new StringBuilder("Error generating report for ").append(suiteClass);
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw new RuntimeException(builder.toString(), e);
            }
        });
    }

    private static AdapterReport generateReport(TestCollection testCollection, PCTReportConfiguration reportManager, ProcessorSupport ps)
    {
        MutableMap<String, String> explodedExpectedFailures = PCTReportConfiguration.explodeExpectedFailures(reportManager.expectedFailures(), ps);
        MutableMap<String, MutableSet<String>> explodedQualifiers = PCTReportConfiguration.explodeQualifiers(reportManager.expectedFailures(), ps);

        MutableMap<String, FunctionTestResults> testResults = Maps.mutable.empty();

        testCollection.forEachTestFunction(x ->
        {
            FunctionTestResults functionInfo = testResults.getIfAbsentPut(x.getSourceInformation().getSourceId(), () -> new FunctionTestResults(x.getSourceInformation().getSourceId()));
            PackageableFunction<?> f = (PackageableFunction<?>) x;

            Set<String> pctQualifiers = PCTTools.getPCTQualifiers(f, ps);
            String error = explodedExpectedFailures.get(PackageableElement.getUserPathForPackageableElement(f));
            MutableSet<String> adapterQualifiers = explodedQualifiers.get(PackageableElement.getUserPathForPackageableElement(f));
            if (adapterQualifiers != null && !adapterQualifiers.isEmpty())
            {
                pctQualifiers.addAll(adapterQualifiers);
            }
            functionInfo.tests.add(new TestInfo(f._functionName(), error == null, error, pctQualifiers));
        });

        return new AdapterReport(
                reportManager.getReportScope(),
                new AdapterKey(reportManager.getAdapter(), reportManager.getPlatform()),
                Lists.mutable.withAll(testResults.values())
        );
    }

    public static void writeToTarget(String targetDir, MutableList<AdapterReport> reports)
    {
        reports.forEach(x ->
        {
            try
            {
                String reportStr = JsonMapper.builder().build().setSerializationInclusion(JsonInclude.Include.NON_NULL).writerWithDefaultPrettyPrinter().writeValueAsString(x);
                Shared.writeStringToTarget(targetDir, ReportGeneration.getReportName(x) + ".json", reportStr);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static String getReportName(AdapterReport adapterReport)
    {
        return "ADAPTER_" + adapterReport.reportScope.module + "_" + adapterReport.adapterKey.platform + "_" + adapterReport.adapterKey.adapter.name;
    }
}
