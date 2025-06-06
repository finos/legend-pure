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

package org.finos.legend.pure.m3.pct.reports.config.exclusion;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

public class ExclusionOneTest implements ExclusionSpecification
{
    public String testFullPath;
    public String expectedMessage;
    public MutableSet<AdapterQualifier> adapterQualifiers;

    public ExclusionOneTest(String testFullPath, String expectedMessage, AdapterQualifier...adapterQualifiers)
    {
        this.testFullPath = testFullPath;
        this.expectedMessage = expectedMessage;
        this.adapterQualifiers = Sets.mutable.of(adapterQualifiers);
    }

    @Override
    public MutableMap<String, String> resolveExclusion(ProcessorSupport processorSupport)
    {
        return Maps.mutable.with(testFullPath, expectedMessage);
    }

    @Override
    public MutableMap<String, MutableSet<String>> resolveQualifiers(ProcessorSupport processorSupport)
    {
        return Maps.mutable.with(testFullPath, adapterQualifiers.collect(AdapterQualifier::toString).toSet());
    }
}