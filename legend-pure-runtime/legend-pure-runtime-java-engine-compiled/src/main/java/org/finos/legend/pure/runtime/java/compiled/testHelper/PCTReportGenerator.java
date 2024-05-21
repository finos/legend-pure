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

package org.finos.legend.pure.runtime.java.compiled.testHelper;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.pct.reports.generation.ReportGeneration;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;

import java.util.List;

public class PCTReportGenerator
{
    public static void main(String[] args) throws Exception
    {
        generateCompiled(args[0], Lists.mutable.with(args).drop(1));
    }

    public static void generateCompiled(String targetDir, List<String> testSuites)
    {
        MutableList<AdapterReport> result = ReportGeneration.generateReport(testSuites, PureTestBuilderCompiled.getClassLoaderExecutionSupport(Thread.currentThread().getContextClassLoader()).getProcessorSupport());
        ReportGeneration.writeToTarget(targetDir, result);
    }
}
