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

package org.finos.legend.pure.m3.pct.config;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.pct.PCTReportProvider;
import org.finos.legend.pure.m3.pct.PCTTools;
import org.finos.legend.pure.m3.pct.config.exclusion.ExclusionOneTest;
import org.finos.legend.pure.m3.pct.config.exclusion.ExclusionPackageTests;
import org.finos.legend.pure.m3.pct.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.model.FunctionInfo;
import org.finos.legend.pure.m3.pct.model.ReportScope;
import org.finos.legend.pure.m3.pct.model.Report;
import org.finos.legend.pure.m3.pct.model.Signature;
import org.finos.legend.pure.m3.pct.model.TestInfo;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ServiceLoader;

public abstract class PCTReport
{
    public abstract ReportScope getReportScope();

    public abstract MutableList<ExclusionSpecification> expectedFailures();

    public abstract String getAdapter();

    public abstract String getPlatform();

    public String getReportName()
    {
        return this.getReportScope().module + "_" + this.getPlatform() + "_" + this.getAdapter().substring(this.getAdapter().lastIndexOf(':') + 1);
    }

    public static MutableList<Report> gatherReports()
    {
        MutableList<Report> result = Lists.mutable.empty();
        for (PCTReportProvider pctReportProvider : ServiceLoader.load(PCTReportProvider.class))
        {
            result.addAll(pctReportProvider.getReports());
        }
        return result;
    }

    public static void generateReport(String target, List<String> testSuites, Function<Report, String> serializer, ProcessorSupport processorSupport)
    {
        ListIterate.forEach(testSuites, suiteClass ->
        {
            try
            {
                PCTReport reportManager = (PCTReport) Thread.currentThread().getContextClassLoader().loadClass(suiteClass).newInstance();
                Report report = reportManager.generateReport(TestCollection.buildPCTTestCollection(reportManager.getReportScope()._package, reportManager.getReportScope().filePath, processorSupport), reportManager.getReportScope().filePath, processorSupport);
                String reportStr = serializer.apply(report);

                Path targetPath = Paths.get(target, reportManager.getReportName() + ".json");
                byte[] bytes = reportStr.getBytes(StandardCharsets.UTF_8);
                Files.createDirectories(targetPath.getParent());
                Files.write(targetPath, bytes, StandardOpenOption.CREATE);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    private Report generateReport(TestCollection testCollection, String filePath, ProcessorSupport ps)
    {
        MutableMap<String, String> explodedExpectedFailures = explodeExpectedFailures(expectedFailures(), ps);

        MutableMap<String, FunctionInfo> functionsInfo = Maps.mutable.empty();
        getPCTFunctions(testCollection.getPackage(), filePath, ps).forEach(x ->
        {
            FunctionInfo functionInfo = functionsInfo.getIfAbsentPut(x.getSourceInformation().getSourceId(), () -> new FunctionInfo(x.getSourceInformation().getSourceId()));
            // Name
            if (functionInfo.name != null && !functionInfo.name.equals(x._functionName()))
            {
                throw new RuntimeException("Error in file " + x.getSourceInformation().getSourceId() + ". The file contains multiple PCT functions: " + x._functionName() + " & " + functionInfo.name);
            }
            functionInfo.name = x._functionName();
            // Package
            String funcPackage = PackageableElement.getUserPathForPackageableElement(x._package());
            if (functionInfo._package != null && !functionInfo._package.equals(funcPackage))
            {
                throw new RuntimeException("Error in file " + x.getSourceInformation().getSourceId() + ". The file contains multiple PCT functions in different packages: " + funcPackage + " & " + functionInfo._package);
            }
            functionInfo._package = funcPackage;
            // Signatures
            functionInfo.signatures.add(new Signature(PackageableElement.getUserPathForPackageableElement(x), FunctionDescriptor.getFunctionDescriptor(x, ps), PCTTools.getDoc(x, ps), PCTTools.getGrammarDoc(x, ps)));
        });

        testCollection.getAllTestFunctions().forEach(x ->
        {
            FunctionInfo functionInfo = functionsInfo.getIfAbsentPut(x.getSourceInformation().getSourceId(), () -> new FunctionInfo(x.getSourceInformation().getSourceId()));
            PackageableFunction<?> f = (PackageableFunction<?>) x;
            String error = explodedExpectedFailures.get(PackageableElement.getUserPathForPackageableElement(f));
            functionInfo.tests.add(new TestInfo(f._functionName(), error == null, error));
        });

        return new Report(
                getReportScope(),
                getAdapter(),
                getPlatform(),
                functionsInfo.valuesView().toList()
        );

    }

    private MutableList<PackageableFunction<?>> getPCTFunctions(CoreInstance pkg, String filePath, ProcessorSupport processorSupport)
    {
        MutableList<PackageableFunction<?>> result = Lists.mutable.empty();
        getPCTFunctionsRecurse(pkg, result, filePath, processorSupport);
        return result;
    }

    private void getPCTFunctionsRecurse(CoreInstance pkg, MutableList<PackageableFunction<?>> func, String filePath, ProcessorSupport processorSupport)
    {
        for (CoreInstance child : Instance.getValueForMetaPropertyToManyResolved(pkg, M3Properties.children, processorSupport))
        {
            if (Instance.instanceOf(child, M3Paths.PackageableFunction, processorSupport))
            {
                if (PCTTools.isPCTFunction(child, processorSupport) && child.getSourceInformation().getSourceId().startsWith(filePath))
                {
                    func.add((PackageableFunction<?>) child);
                }
            }
            else if (Instance.instanceOf(child, M3Paths.Package, processorSupport))
            {
                getPCTFunctionsRecurse(child, func, filePath, processorSupport);
            }
        }
    }

    protected static ExclusionSpecification one(String fullPath, String message)
    {
        return new ExclusionOneTest(fullPath, message);
    }

    protected static ExclusionSpecification pack(String _package, String message)
    {
        return new ExclusionPackageTests(_package, message);
    }

    public static MutableMap<String, String> explodeExpectedFailures(MutableList<ExclusionSpecification> expectedFailures, ProcessorSupport processorSupport)
    {
        MutableMap<String, String> result = org.eclipse.collections.impl.factory.Maps.mutable.empty();
        expectedFailures.forEach(x -> result.putAll(x.resolveExclusion(processorSupport)));
        return result;
    }
}
