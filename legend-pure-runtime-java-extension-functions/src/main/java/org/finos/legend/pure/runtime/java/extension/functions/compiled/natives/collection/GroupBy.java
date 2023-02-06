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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class GroupBy extends AbstractNative implements Native
{
    public GroupBy()
    {
        super("groupBy_X_MANY__Function_1__Map_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();

        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        boolean isLambdaFunction = Instance.instanceOf(parametersValues.get(1), M3Paths.InstanceValue, processorSupport) && ValueSpecification.instanceOf(parametersValues.get(1), M3Paths.LambdaFunction, processorSupport);

        CoreInstance keyFn = isLambdaFunction ? Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport) : parametersValues.get(1);
        CoreInstance functionType = processorSupport.function_getFunctionType(keyFn);
        CoreInstance parameter = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst();

        String keyType = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), processorSupport);
        String valueType = TypeProcessor.typeToJavaObjectWithMul(parameter.getValueForMetaPropertyToOne(M3Properties.genericType), parameter.getValueForMetaPropertyToOne(M3Properties.multiplicity), processorSupport);

        String keyFnString;
        String keyValueString;
        if (isLambdaFunction)
        {
            String paramName = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport), M3Properties.parameters, processorSupport).get(0), M3Properties.name, processorSupport).getName();
            String keyFnType = "DefendedFunction<" + valueType + ", " + keyType + ">";
            keyFnString = keyFnType + " keyFn = new " + keyFnType + "()" +
                    "{" +
                        "@Override " +
                        "public " + keyType + " valueOf(final " + valueType + " _" + paramName + ")" +
                        "{" +
                            FunctionProcessor.processFunctionDefinitionContent(topLevelElement, Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport), true, processorContext, processorSupport) +
                        "}" +
                    "}";
            keyValueString = "keyFn.valueOf(input)";
        }
        else
        {
            String keyFnType = "PureFunction1<" + valueType + "," + keyType + ">";
            keyFnString = keyFnType + " keyFn = (" + keyFnType + ") CoreGen.getSharedPureFunction(" + transformedParams.get(1) + ", es)";
            keyValueString = "keyFn.value(input, es)";
        }

        String inputString = transformedParams.get(0);
        String listEmpty = "new Root_meta_pure_functions_collection_List_Impl<" + valueType + ">(\"Anonymous\")";
        String listType = FullJavaPaths.List + "<" + valueType + ">";
        String mapEmpty = "new org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy<" + keyType + ", " + listType + ">(org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy.HASHING_STRATEGY)";
        String mapType = "org.eclipse.collections.api.map.MutableMap<" + keyType + ", " + listType + ">";
        String injectIntoFnType = "DefendedFunction2<" + mapType + ", " + valueType + ", " + mapType + ">";

        return "new PureMap(CompiledSupport.toPureCollection(" + inputString + ").injectInto(" + mapEmpty + ", new " + injectIntoFnType + "()\n" +
                "{\n" +
                "   " + keyFnString + ";\n" +
                "    @Override\n" +
                "    public " + mapType + " value(" + mapType + " collector, final " + valueType + " input)\n" +
                "    {\n" +
                "        collector.getIfAbsentPut(" + keyValueString + ", " + listEmpty + ")._valuesAdd(input);\n" +
                "        return collector;\n" +
                "    }\n" +
                "}))\n";
    }


    @Override
    public String buildBody()
    {
        String mapType = "MutableMap<Object, " + FullJavaPaths.List + "<Object>>";
        String injectIntoFnType = "DefendedFunction2<" + mapType + ", Object, " + mapType + ">";

        return "         new DefendedPureFunction2<Object, Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object p1, final Object p2, final ExecutionSupport es)\n" +
                "            {\n" +
                "                final " + FullJavaPaths.Function+ " func = p2 instanceof java.util.List?(" + FullJavaPaths.Function + ")((java.util.List)p2).get(0):(" + FullJavaPaths.Function + ")p2;\n" +
                "                return new PureMap(CompiledSupport.toPureCollection(p1).injectInto(new org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy<Object, " + FullJavaPaths.List + "<Object>>(org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy.HASHING_STRATEGY), new " + injectIntoFnType + "()\n" +
                "                {\n" +
                "                    PureFunction1<Object,Object> funcC = (PureFunction1<Object,Object>) CoreGen.getSharedPureFunction(func, es);\n" +
                "                    public " + mapType + " value(" + mapType + " collector, final Object _var)\n" +
                "                    {\n" +
                "                        collector.getIfAbsentPut(funcC.value(_var, es), new Root_meta_pure_functions_collection_List_Impl<Object>(\"Anonymous\"))._valuesAdd(_var);\n" +
                "                        return collector;\n" +
                "                    }\n" +
                "                }));\n"+
                "            }\n" +
                "        }";
    }
}
