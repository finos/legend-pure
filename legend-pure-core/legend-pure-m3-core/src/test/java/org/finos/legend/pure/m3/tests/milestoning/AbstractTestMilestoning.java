// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.milestoning;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

abstract class AbstractTestMilestoning extends AbstractPureTestWithCoreCompiledPlatform
{
    CoreInstance getGeneratedMilestoningStereotype()
    {
        CoreInstance milestoningPackage = processorSupport.package_getByUserPath(M3Paths.Milestoning);
        return milestoningPackage.getValueInValueForMetaPropertyToMany(M3Properties.p_stereotypes, "generatedmilestoningproperty");
    }

    CoreInstance getGeneratedMilestoningDateStereotype()
    {
        CoreInstance milestoningPackage = processorSupport.package_getByUserPath(M3Paths.Milestoning);
        return milestoningPackage.getValueInValueForMetaPropertyToMany(M3Properties.p_stereotypes, "generatedmilestoningdateproperty");
    }
}
