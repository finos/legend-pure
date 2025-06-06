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

package org.finos.legend.pure.m3.pct.reports.config;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionOneTest;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionPackageTests;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;

public abstract class PCTReportConfiguration
{
    public abstract ReportScope getReportScope();

    public abstract MutableList<ExclusionSpecification> expectedFailures();

    public abstract Adapter getAdapter();

    public abstract String getPlatform();

    protected static ExclusionSpecification one(String fullPath, String message, AdapterQualifier...adapterQualifiers)
    {
        return new ExclusionOneTest(fullPath, message, adapterQualifiers);
    }

    protected static ExclusionSpecification pack(String _package, String message, AdapterQualifier...adapterQualifiers)
    {
        return new ExclusionPackageTests(_package, message, adapterQualifiers);
    }

    public static MutableMap<String, String> explodeExpectedFailures(MutableList<ExclusionSpecification> expectedFailures, ProcessorSupport processorSupport)
    {
        MutableMap<String, String> result = org.eclipse.collections.impl.factory.Maps.mutable.empty();
        expectedFailures.forEach(x -> result.putAll(x.resolveExclusion(processorSupport)));
        return result;
    }

    public static MutableMap<String, MutableSet<String>> explodeQualifiers(MutableList<ExclusionSpecification> expectedFailures, ProcessorSupport processorSupport)
    {
        MutableMap<String, MutableSet<String>> result = org.eclipse.collections.impl.factory.Maps.mutable.empty();
        expectedFailures.forEach(x -> result.putAll(x.resolveQualifiers(processorSupport)));
        return result;
    }
}