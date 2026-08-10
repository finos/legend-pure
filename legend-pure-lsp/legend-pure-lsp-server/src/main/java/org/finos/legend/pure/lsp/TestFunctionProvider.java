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
import org.finos.legend.pure.lsp.protocol.TestFunctionInfo;
import org.finos.legend.pure.m3.execution.test.TestTools;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Finds real compiled functions carrying the meta::pure::profiles::test "Test" stereotype, the PCT
 * "test" stereotype (meta::pure::test::pct::PCT.test), or the "BeforePackage"/"AfterPackage"
 * stereotypes in a given source - the same semantic check Pure IDE's own
 * meta::pure::ide::testing::isTest() uses, not a text/regex scan for the literal annotation. That
 * distinction matters: a stereotype can be commented out, aliased through a different qualification,
 * or accompany other stereotypes, none of which change whether the compiled function is actually a
 * test. A PCT test (TestFunctionInfo#isPCTTest) additionally requires an adapter to run (see
 * ExecuteFunctionParams#pctAdapterPath, legend/getPCTAdapters); a Before/After function
 * (TestFunctionInfo#isBeforeFunction/isAfterFunction) is included so it can be run/debugged directly
 * from its own gutter marker - each distinguished by its flag so a client can offer the right actions.
 */
public class TestFunctionProvider
{
    public static List<TestFunctionInfo> getTestFunctions(PureRuntime runtime, String sourceId)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }

        ListIterable<? extends CoreInstance> instances = source.getNewInstances();
        if (instances == null || instances.isEmpty())
        {
            return Collections.emptyList();
        }

        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        List<TestFunctionInfo> result = new ArrayList<>();
        for (CoreInstance instance : instances)
        {
            if (!isFunction(instance))
            {
                continue;
            }
            boolean isPCTTest = PCTTools.isPCTTest(instance, processorSupport);
            boolean isBeforeFunction = TestTools.hasBeforePackageStereotype(instance, processorSupport);
            boolean isAfterFunction = TestTools.hasAfterPackageStereotype(instance, processorSupport);
            if (!isPCTTest && !isBeforeFunction && !isAfterFunction && !TestTools.hasTestStereotype(instance, processorSupport))
            {
                continue;
            }

            SourceInformation si = instance.getSourceInformation();
            if (si == null)
            {
                continue;
            }

            String functionPath = PackageableElement.getUserPathForPackageableElement(instance);
            String simpleName = DocumentOutlineProvider.getSimpleFunctionName(instance);
            result.add(new TestFunctionInfo(functionPath, simpleName, si.getStartLine(), isPCTTest, isBeforeFunction, isAfterFunction));
        }
        return result;
    }

    private static boolean isFunction(CoreInstance instance)
    {
        String classifierName = instance.getClassifier() == null ? null : instance.getClassifier().getName();
        return "ConcreteFunctionDefinition".equals(classifierName) || "NativeFunction".equals(classifierName);
    }
}
