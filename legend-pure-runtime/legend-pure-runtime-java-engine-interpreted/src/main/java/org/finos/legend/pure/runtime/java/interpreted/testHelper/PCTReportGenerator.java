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

package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.pct.config.PCTReport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

import java.util.List;

public class PCTReportGenerator
{
    public static void main(String[] args) throws Exception
    {
        generateInterpreted(args[0], Lists.mutable.with(args).drop(1));
    }

    public static void generateInterpreted(String target, List<String> testSuites)
    {
        ProcessorSupport processorSupport = PureTestBuilderInterpreted.getFunctionExecutionInterpreted().getProcessorSupport();
        PCTReport.generateReport(target, testSuites, report ->
        {
            try
            {
                return JsonMapper.builder().build().setSerializationInclusion(JsonInclude.Include.NON_NULL).writerWithDefaultPrettyPrinter().writeValueAsString(report);
            }
            catch (JsonProcessingException e)
            {
                throw new RuntimeException(e);
            }
        }, processorSupport);
    }


}
