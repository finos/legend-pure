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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Get extends AbstractNative
{
    public Get()
    {
        super("get_Map_1__U_1__V_$0_1$_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);
        return "((" + type + ")((PureMap)" + transformedParams.get(0) + ").getMap().get(" + transformedParams.get(1) + "))";
    }

    @Override
    public String buildBody() {

        return "new DefendedPureFunction2<PureMap, Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(PureMap map, Object key, ExecutionSupport es)\n" +
                "            {\n" +
                "                return map.getMap().get(key);\n" +
                "            }\n" +
                "        }";
    }
}
