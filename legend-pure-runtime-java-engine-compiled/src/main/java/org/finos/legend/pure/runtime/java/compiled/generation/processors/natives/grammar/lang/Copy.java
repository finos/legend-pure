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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.SourceInfoProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Copy extends AbstractNative
{
    public Copy() {
        super("copy_T_1__String_1__T_1_", "copy_T_1__String_1__KeyExpression_MANY__T_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        SourceInformation sourceInformation = functionExpression.getSourceInformation();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.genericType, processorSupport);
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        String type = TypeProcessor.typeToJavaObjectSingle(genericType, false, processorSupport);

        String copyObject;

        if (transformedParams.size() == 3) {

            ListIterable<? extends CoreInstance> keyValues = Instance.getValueForMetaPropertyToManyResolved(parametersValues.get(2), M3Properties.values, processorSupport);
            copyObject = "CompiledSupport.<" + type + ">copy(" + transformedParams.get(0) + ", " +
                    NativeFunctionProcessor.buildM4SourceInformation(sourceInformation)
                    + ")" + InstantiationHelpers.manageKeyValues(genericType, Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.genericType, M3Properties.rawType, processorSupport), keyValues, processorContext);
        } else {
            copyObject =  "CompiledSupport.<" + type + ">copy(" + transformedParams.get(0) + ")";
        }

        return _Class.computeConstraintsInHierarchy(_class,processorSupport).isEmpty()? copyObject: "((" + type + ")Pure.handleValidation(false,"+ copyObject+"," + SourceInfoProcessor.sourceInfoToString(sourceInformation)+",es))";
    }
}
