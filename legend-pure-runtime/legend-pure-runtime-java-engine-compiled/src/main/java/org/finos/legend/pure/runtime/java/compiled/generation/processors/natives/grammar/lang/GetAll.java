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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class GetAll extends AbstractNative
{
    public GetAll()
    {
        super("getAll_Class_1__T_MANY_",
                "getAllVersions_Class_1__T_MANY_",
                "getAll_Class_1__Date_1__T_MANY_",
                "getAll_Class_1__Date_1__Date_1__T_MANY_",
                "getAllVersionsInRange_Class_1__Date_1__Date_1__T_MANY_",
                "deepFetchGetAll_Class_1__DeepFetchTempTable_1__T_MANY_"
        );
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        final ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());
        ProcessorSupport processorSupport = processorContext.getSupport();

        String name = processorSupport.instance_instanceOf(parametersValues.get(0), M3Paths.VariableExpression) ?
                ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext) :
                MetadataJavaPaths.buildMetadataKeyFromType(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.values, processorSupport));

        String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), true, processorSupport);

        return "((RichIterable<" + type + ">)Lists.mutable.ofAll(((CompiledExecutionSupport)es).getMetadata(\"" + name + "\").valuesView()))";
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction1<Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object o, final ExecutionSupport es)\n" +
                "            {\n" +
                "                String name = org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths.buildMetadataKeyFromType((org.finos.legend.pure.m4.coreinstance.CoreInstance)o);\n" +
                "                return ((RichIterable)Lists.mutable.ofAll(((CompiledExecutionSupport)es).getMetadata(name).valuesView()));\n" +
                "            }\n" +
                "        }";
    }
}
