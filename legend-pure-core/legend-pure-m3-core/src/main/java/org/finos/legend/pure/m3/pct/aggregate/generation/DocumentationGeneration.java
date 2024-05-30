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
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderLoader;

import java.util.Map;

public class DocumentationGeneration
{
    public static Documentation buildDocumentation()
    {
        Documentation documentation = new Documentation();

        Map<String, FunctionDocumentation> documentationBySourceId = Maps.mutable.empty();

        PCTReportProviderLoader.gatherFunctions().forEach(functions ->
                {
                    functions.functionDefinitions.forEach(function ->
                    {
                        FunctionDocumentation doc = new FunctionDocumentation();
                        doc.module = functions.reportScope.module;
                        doc.functionDefinition = function;
                        documentationBySourceId.put(function.sourceId, doc);
                    });
                }
        );

        PCTReportProviderLoader.gatherReports().forEach(report ->
                {
                    report.functionTests.forEach(functionTest ->
                    {
                        FunctionDocumentation doc = documentationBySourceId.get(functionTest.sourceId);
                        if (doc != null)
                        {
                            doc.functionTestResults.put(report.adapterKey, functionTest);
                        }
                    });
                }
        );

        documentationBySourceId.values().forEach(v ->
                {
                    documentation.documentationByName.put(v.functionDefinition.name, v);
                }
        );

        return documentation;
    }
}
