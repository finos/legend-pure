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

package org.finos.legend.pure.m3.pct.aggregate.generation;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.functions.model.FunctionDefinition;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderLoader;

import java.util.Map;

public class DocumentationGeneration
{
    public static Documentation buildDocumentation()
    {
        Documentation documentation = new Documentation();

        MutableMap<String, FunctionDocumentation> documentationBySourceId = Maps.mutable.empty();

        PCTReportProviderLoader.gatherFunctions().forEach(functions ->
                {
                    functions.functionDefinitions.forEach(function ->
                    {
                        FunctionDocumentation doc = new FunctionDocumentation();
                        doc.reportScope = functions.reportScope;
                        doc.functionDefinition = function;
                        documentationBySourceId.put(function.sourceId, doc);
                    });
                }
        );

        MutableList<AdapterReport> allReports = PCTReportProviderLoader.gatherReports();
        allReports.forEach(report ->
                {
                    report.functionTests.forEach(functionTest ->
                    {
                        FunctionDocumentation doc = documentationBySourceId.getIfAbsentPut(functionTest.sourceId, () ->
                        {
                            // Create an Empty one, accounts for source files containing only tests (function composition use case)
                            FunctionDocumentation funcDoc = new FunctionDocumentation();
                            funcDoc.functionDefinition = new FunctionDefinition(functionTest.sourceId);
                            funcDoc.reportScope = report.reportScope;
                            return funcDoc;
                        });
                        doc.functionTestResults.put(report.adapterKey, functionTest);
                    });
                }
        );

        documentation.functionsDocumentation = documentationBySourceId.valuesView().toList();

        documentation.adapters = allReports.collect(c -> c.adapterKey).distinct();

        return documentation;
    }
}
