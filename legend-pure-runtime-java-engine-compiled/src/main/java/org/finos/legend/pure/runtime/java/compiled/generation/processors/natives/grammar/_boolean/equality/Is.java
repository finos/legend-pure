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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar._boolean.equality;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class Is extends AbstractNative
{
    public Is() {
        super("is_Any_1__Any_1__Boolean_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        String left = transformedParams.get(0);
        String right = transformedParams.get(1);

        boolean isLeftPrimitiveType = Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.genericType, M3Properties.rawType, processorSupport), M3Paths.DataType, processorSupport);
        boolean isRightPrimitiveType = Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, M3Properties.rawType, processorSupport), M3Paths.DataType, processorSupport);
        boolean isLeftEnumerationType = Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.genericType, M3Properties.rawType, processorSupport), M3Paths.Enumeration, processorSupport);
        boolean isRightEnumerationType = Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, M3Properties.rawType, processorSupport), M3Paths.Enumeration, processorSupport);

        if (isLeftEnumerationType && isRightEnumerationType)
        {
            return "(" + left + ".equals( " + right + "))";
        }
        else if (isLeftPrimitiveType && (isRightEnumerationType || !isRightPrimitiveType))
        {
            return "(" + right + ".equals(" + left + "))";
        }
        else if (isRightPrimitiveType && (isLeftEnumerationType || !isLeftPrimitiveType))
        {
            return "(" + left + ".equals(" + right + "))";
        }
        else
        {
            return "(" + left + " == " + right + ")";
        }
    }
}
