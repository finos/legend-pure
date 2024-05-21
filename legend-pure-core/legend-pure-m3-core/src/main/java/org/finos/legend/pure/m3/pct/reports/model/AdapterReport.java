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

package org.finos.legend.pure.m3.pct.reports.model;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;

import java.util.List;

public class AdapterReport
{
    public ReportScope reportScope;
    public String adapter;
    public String platform;
    public List<FunctionTestResults> functionTests;

    public AdapterReport()
    {

    }

    public AdapterReport(ReportScope reportScope, String adapter, String platform, MutableList<FunctionTestResults> functionTests)
    {
        this.reportScope = reportScope;
        this.adapter = adapter;
        this.platform = platform;
        this.functionTests = functionTests;
    }
}
