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
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.pct.shared.PCTTools;

public class ExclusionPackageTests implements ExclusionSpecification
{
    public String testPackage;
    public String expectedMessage;
    public MutableSet<String> adapterQualifiers;

    public ExclusionPackageTests(String testPackage, String expectedMessage)
    {
        this.testPackage = testPackage;
        this.expectedMessage = expectedMessage;
        this.adapterQualifiers = Sets.mutable.empty();
    }

    public ExclusionPackageTests(String testPackage, String expectedMessage, AdapterQualifier...adapterQualifiers)
    {
        this.testPackage = testPackage;
        this.expectedMessage = expectedMessage;
        this.adapterQualifiers = AdapterQualifier.resolveAdapterQualifiers(adapterQualifiers);
    }

    @Override
    public MutableMap<String, String> resolveExclusion(ProcessorSupport processorSupport)
    {
        MutableMap<String, String> result = Maps.mutable.empty();
        Package pack = (Package) processorSupport.package_getByUserPath(testPackage);
        pack._children().forEach(c ->
        {
            if (PCTTools.isPCTTest(c, processorSupport))
            {
                result.put(PackageableElement.getUserPathForPackageableElement(c), expectedMessage);
            }
        });
        return result;
    }

    @Override
    public MutableMap<String, MutableSet<String>> resolveQualifiers(ProcessorSupport processorSupport)
    {
        MutableMap<String, MutableSet<String>> result = Maps.mutable.empty();
        Package pack = (Package) processorSupport.package_getByUserPath(testPackage);
        pack._children().forEach(c ->
        {
            if (PCTTools.isPCTTest(c, processorSupport))
            {
                result.put(PackageableElement.getUserPathForPackageableElement(c), adapterQualifiers);
            }
        });
        return result;
    }
}