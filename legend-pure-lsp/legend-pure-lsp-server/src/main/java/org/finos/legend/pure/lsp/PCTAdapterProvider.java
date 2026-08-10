// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.lsp.protocol.PCTAdapterInfo;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * Finds every element in the currently-loaded graph carrying the &lt;&lt;PCT.adapter&gt;&gt;
 * stereotype - the same discovery meta::pure::ide::testing::getPCTAdapters() performs in Pure
 * (see pct_core.pure), reimplemented here in Java so the LSP can expose it without evaluating Pure
 * code. An "adapter" is a Function substituted in as the sole argument to a &lt;&lt;PCT.test&gt;&gt;
 * test function (see TestRunner#executeTestFunc in legend-pure-core).
 */
public class PCTAdapterProvider
{
    public static List<PCTAdapterInfo> getPCTAdapters(PureRuntime runtime)
    {
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        CoreInstance adapterStereotype = Profile.findStereotype(PCTTools.PCT_PROFILE, "adapter", processorSupport, false);
        if (adapterStereotype == null)
        {
            // The PCT profile itself is not loaded in this graph (e.g. a minimal/non-test session) -
            // nothing can carry the stereotype.
            return Collections.emptyList();
        }

        CoreInstance root = runtime.getCoreInstance("::");
        if (!(root instanceof Package))
        {
            return Collections.emptyList();
        }

        List<PCTAdapterInfo> result = new ArrayList<>();
        collectAdapters((Package) root, adapterStereotype, processorSupport, result);
        return result;
    }

    private static void collectAdapters(Package pkg, CoreInstance adapterStereotype,
                                         ProcessorSupport processorSupport, List<PCTAdapterInfo> result)
    {
        ListIterable<? extends CoreInstance> children = pkg.getValueForMetaPropertyToMany(M3Properties.children);
        for (CoreInstance child : children)
        {
            if (child instanceof Package)
            {
                collectAdapters((Package) child, adapterStereotype, processorSupport, result);
                continue;
            }

            ListIterable<? extends CoreInstance> stereotypes =
                    Instance.getValueForMetaPropertyToManyResolved(child, M3Properties.stereotypes, processorSupport);
            if (stereotypes == null || !stereotypes.contains(adapterStereotype))
            {
                continue;
            }

            String path = PackageableElement.getUserPathForPackageableElement(child);
            String adapterName = Profile.getTaggedValue(child, PCTTools.PCT_PROFILE, "adapterName", processorSupport);
            String name = (adapterName == null || adapterName.isEmpty())
                    ? DocumentOutlineProvider.getSimpleFunctionName(child)
                    : adapterName;
            result.add(new PCTAdapterInfo(name, path));
        }
    }
}
