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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.lang;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.SourceInfoProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.lang.InstantiationHelpers;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;


public class DynamicNew extends AbstractNative
{
    public DynamicNew()
    {
        super("dynamicNew_Class_1__KeyValue_MANY__Any_1_", "dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_",
                "dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", "dynamicNew_Class_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Function_$0_1$__Any_1_",
                "dynamicNew_GenericType_1__KeyValue_MANY__Any_1_", "dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Any_1_",
                "dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Any_1_", "dynamicNew_GenericType_1__KeyValue_MANY__Function_$0_1$__Function_$0_1$__Any_$0_1$__Function_$0_1$__Function_$0_1$__Any_1_");
    }

    // In dynamicNew function default values must be handled
    @Override
    public ListIterable<String> transformParameterValues(ListIterable<? extends CoreInstance> parametersValues, CoreInstance topLevelElement, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        ListIterable<String> defaultValues = transformDefaultValues(parametersValues.get(0), processorSupport, processorContext);

        MutableList<String> transformedParams = Lists.mutable.ofInitialCapacity(parametersValues.size());
        parametersValues.forEachWithIndex((parameterValue, index) ->
        {
            if (defaultValues.notEmpty() && index == 1)
            {
                ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(parameterValue, M3Properties.values, processorSupport);
                String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.genericType, processorSupport), true, processorSupport);

                MutableList<String> processedValues = values.collect(v -> ValueSpecificationProcessor.processValueSpecification(topLevelElement, v, processorContext), Lists.mutable.withAll(defaultValues));
                transformedParams.add(processedValues.size() > 1 ? "Lists.mutable.<" + type + ">with(" + processedValues.makeString(",") + ")" : processedValues.makeString(","));
            }
            else
            {
                transformedParams.add(ValueSpecificationProcessor.processValueSpecification(topLevelElement, parameterValue, processorContext));
            }
        });

        return transformedParams;
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        String newObject = "CoreGen.newObject(" + transformedParams.get(0) + ", CompiledSupport.toPureCollection(" + transformedParams.get(1) + ")";


        String param3 = null;
        CoreInstance param3Instance = null;
        String param4 = null;
        CoreInstance param4Instance = null;
        String param5 = null;
        String param6 = null;

        if (parametersValues.size() > 2)
        {
            param3 = transformedParams.get(2);
            param3Instance = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(2), M3Properties.values, processorSupport);
            param4 = transformedParams.get(3);
            param4Instance = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(3), M3Properties.values, processorSupport);
            param5 = transformedParams.get(4);
        }

        String getterOverrides = parametersValues.size() < 3 ? "null,null,null,null,null" :
                param3 + "," +
                        param4 + "," +
                        param5 + "," +
                        (param3Instance == null ? "null," : "new DefendedPureFunction2<Object,Object,Object>(){" +
                                "                    public Object value(Object t1, Object t2, ExecutionSupport es){return " + IdBuilder.sourceToId(param3Instance.getSourceInformation()) + "." + FunctionProcessor.functionNameToJava(param3Instance) + "(t1,(" + FullJavaPaths.Property + "<? extends java.lang.Object,? extends java.lang.Object>)t2,es);}" +
                                "                    public Object execute(ListIterable<?> vars, ExecutionSupport es){throw new RuntimeException(\"Not Supported Yet!\");}" +
                                "                    },") +
                        (param4Instance == null ? "null" : "new DefendedPureFunction2<Object,Object,Object>(){" +
                                "                    public Object value(Object t1, Object t2, ExecutionSupport es){return " + IdBuilder.sourceToId(param4Instance.getSourceInformation()) + "." + FunctionProcessor.functionNameToJava(param4Instance) + "(t1,(" + FullJavaPaths.Property + "<? extends java.lang.Object,? extends java.lang.Object>)t2,es);}" +
                                "                    public Object execute(ListIterable<?> vars, ExecutionSupport es){throw new RuntimeException(\"Not Supported Yet!\");}" +
                                "                    }");

        if (parametersValues.size() >= 6)
        {
            param6 = transformedParams.get(5);
        }

        String newOverrideInstance = parametersValues.size() < 3 ? "null" :
                parametersValues.size() < 6 ? "new " + FullJavaPaths.GetterOverride_Impl + "(\"\")" :
                        (param3Instance == null && param4Instance == null) ? "new " + FullJavaPaths.ConstraintsOverride_Impl + "(\"\")._constraintsManager(" + param6 + ")"
                                : "new " + FullJavaPaths.ConstraintsGetterOverride_Impl + "(\"\")._constraintsManager(" + param6 + ")";

        String newObjectStatement = newObject + "," + newOverrideInstance + "," + getterOverrides + ",es)";
        return "Pure.handleValidation(false," + newObjectStatement + "," + SourceInfoProcessor.sourceInfoToString(functionExpression.getSourceInformation()) + ",es)";
    }

    private ListIterable<String> transformDefaultValues(CoreInstance instance, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, M3Properties.typeArguments, processorSupport);

        return genericType != null ? InstantiationHelpers.manageDefaultValues(this::formatDefaultValueString,
                Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport), true, processorContext).select(s -> !s.isEmpty())
                : Lists.immutable.empty();
    }

    private String formatDefaultValueString(String name, String value)
    {
        return "new org.finos.legend.pure.generated.Root_meta_pure_functions_lang_KeyValue_Impl(\"Anonymous_NoCounter\")._key(\"" + name + "\")._value(" + value + ")";
    }
}
