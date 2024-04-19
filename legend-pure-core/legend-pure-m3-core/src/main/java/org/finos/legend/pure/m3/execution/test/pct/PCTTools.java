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

package org.finos.legend.pure.m3.execution.test.pct;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class PCTTools
{
    public static final String PCT_PROFILE = "meta::pure::test::pct::PCT";

    public static boolean isPCT(CoreInstance node, ProcessorSupport processorSupport)
    {
        CoreInstance testStereotype = Profile.findStereotype(processorSupport.package_getByUserPath(PCT_PROFILE), "test");
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes, processorSupport);
        return stereotypes.detect(x -> x == testStereotype) != null;
    }
}
