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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

import java.util.List;

public class CoreExtensionCompiled implements CompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "CoreGen",
                "package org.finos.legend.pure.generated;\n" +
                        "\n" +
                        "import org.eclipse.collections.api.RichIterable;\n" +
                        "import org.eclipse.collections.api.block.function.Function;\n" +
                        "import org.eclipse.collections.api.block.function.Function0;\n" +
                        "import org.eclipse.collections.api.factory.Lists;\n" +
                        "import org.eclipse.collections.api.list.ListIterable;\n" +
                        "import org.eclipse.collections.api.list.MutableList;\n" +
                        "import org.eclipse.collections.api.tuple.Pair;\n" +
                        "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                        "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.Package;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.GetterOverride;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;\n" +
                        "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                        "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                        "import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\n" +
                        "import org.finos.legend.pure.m3.navigation.ProcessorSupport;\n" +
                        "import org.finos.legend.pure.m3.tools.ListHelper;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.GetterOverrideExecutor;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2Wrapper;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction0;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedProcedure;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;\n" +
                        "\n" +
                        "import java.lang.reflect.Constructor;\n" +
                        "import java.lang.reflect.InvocationTargetException;\n" +
                        "import java.lang.reflect.Method;\n" +
                        "import java.math.BigDecimal;\n" +
                        "\n" +
                        "public class CoreGen\n" +
                        "{\n" +
                        "    public static final Bridge bridge = new BridgeImpl();\n" +
                        "\n" +
                        "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, MetadataAccessor ma, ProcessorSupport processorSupport)\n" +
                        "    {\n" +
                        "        return Pure.safeGetGenericType(val, ma, new DefendedFunction0<GenericType>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public GenericType value()\n" +
                        "            {\n" +
                        "                return new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"\");\n" +
                        "            }\n" +
                        "        }, processorSupport);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static SharedPureFunction getSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.getSharedPureFunction(func, bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object evaluate(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, Object... instances)\n" +
                        "    {\n" +
                        "        return Pure.evaluate(es, func, bridge, instances);\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    public static Object evaluateToMany(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, RichIterable<? extends List> instances)\n" +
                        "    {\n" +
                        "        return evaluateToMany(es, bridge, func, instances);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object evaluateToMany(ExecutionSupport es, Bridge bridge, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, RichIterable<? extends List> instances)\n" +
                        "    {\n" +
                        "        MutableList<Object> inputs = Lists.mutable.of();\n" +
                        "        if (instances != null)\n" +
                        "        {\n" +
                        "            for (Object obj : instances)\n" +
                        "            {\n" +
                        "                inputs.add(((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List) obj)._values());\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return Pure._evaluateToMany(es, bridge, func, inputs);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.dynamicMatch(obj, funcs, bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    private static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, boolean isMatchWith, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.dynamicMatch(obj, funcs, var, isMatchWith, bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <T, V> RichIterable<T> removeDuplicates(RichIterable<T> list, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function eqlFn, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.removeDuplicates(list, keyFn, eqlFn, bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(UnifiedMap.newMap()), bridge);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, PureMap lambdaOpenVariablesMap)\n" +
                        "    {\n" +
                        "        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge);\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class aClass, String name, RichIterable<? extends KeyExpression> root_meta_pure_functions_lang_keyExpressions, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.newObject(bridge, aClass, name, root_meta_pure_functions_lang_keyExpressions, es);\n" +
                        "\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    private static class BridgeImpl implements Bridge\n" +
                        "    {\n" +
                        "        @Override\n" +
                        "        public <T> List<T> buildList()\n" +
                        "        {\n" +
                        "            return new Root_meta_pure_functions_collection_List_Impl<>(\"\");\n" +
                        "        }\n" +
                        "\n" +
                        "        @Override\n" +
                        "        public LambdaCompiledExtended buildLambda(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> lambdaFunction, SharedPureFunction<Object> pureFunction)\n" +
                        "        {\n" +
                        "            return new PureCompiledLambda(lambdaFunction, pureFunction);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static String toRepresentation(java.lang.Object _any, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        if (_any instanceof String)\n" +
                        "        {\n" +
                        "            return \"'\" + CompiledSupport.replace((String) CompiledSupport.makeOne(_any), \"'\", \"\\\\'\") + \"'\";\n" +
                        "        }\n" +
                        "        if (_any instanceof org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate)\n" +
                        "        {\n" +
                        "            return \"%\" + CompiledSupport.pureToString((PureDate) CompiledSupport.makeOne(_any), es);\n" +
                        "        }\n" +
                        "        if (_any instanceof BigDecimal)\n" +
                        "        {\n" +
                        "            return CompiledSupport.pureToString((BigDecimal) CompiledSupport.makeOne(_any), es) + \"D\";\n" +
                        "        }\n" +
                        "        if (_any instanceof Number)\n" +
                        "        {\n" +
                        "            return CompiledSupport.pureToString((Number) CompiledSupport.makeOne(_any), es);\n" +
                        "        }\n" +
                        "        if (_any instanceof Boolean)\n" +
                        "        {\n" +
                        "            return CompiledSupport.pureToString(((Boolean) CompiledSupport.makeOne(_any)).booleanValue(), es);\n" +
                        "        }\n" +
                        "        if (_any instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)\n" +
                        "        {\n" +
                        "            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement p = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) _any;\n" +
                        "            if (p._name() == null)\n" +
                        "            {\n" +
                        "                return \"<\" + Pure.manageId(p) + \"instanceOf \" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) CoreGen.safeGetGenericType(p, ((CompiledExecutionSupport) es).getMetadataAccessor(), ((CompiledExecutionSupport) es).getProcessorSupport())._rawType(), \"::\") + \">\";\n" +
                        "            }\n" +
                        "            else\n" +
                        "            {\n" +
                        "                return Pure.elementToPath(p, \"::\");\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return \"<\" + Pure.manageId(_any) + \"instanceOf \" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) CoreGen.safeGetGenericType(_any, ((CompiledExecutionSupport) es).getMetadataAccessor(), ((CompiledExecutionSupport) es).getProcessorSupport())._rawType(), \"::\") + \">\";\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object newObject(final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        final ClassCache classCache = ((CompiledExecutionSupport) es).getClassCache();\n" +
                        "        Constructor<?> constructor = classCache.getIfAbsentPutConstructorForType(aClass);\n" +
                        "        final Any result;\n" +
                        "        try\n" +
                        "        {\n" +
                        "            result = (Any) constructor.newInstance(\"\");\n" +
                        "        }\n" +
                        "        catch (InvocationTargetException | InstantiationException | IllegalAccessException e)\n" +
                        "        {\n" +
                        "            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;\n" +
                        "            StringBuilder builder = new StringBuilder(\"Error instantiating \");\n" +
                        "            org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, aClass);\n" +
                        "            String eMessage = cause.getMessage();\n" +
                        "            if (eMessage != null)\n" +
                        "            {\n" +
                        "                builder.append(\": \").append(eMessage);\n" +
                        "            }\n" +
                        "            throw new RuntimeException(builder.toString(), cause);\n" +
                        "        }\n" +
                        "        keyExpressions.forEach(new DefendedProcedure<Root_meta_pure_functions_lang_KeyValue>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public void value(Root_meta_pure_functions_lang_KeyValue keyValue)\n" +
                        "            {\n" +
                        "                Method m = classCache.getIfAbsentPutPropertySetterMethodForType(aClass, keyValue._key());\n" +
                        "                try\n" +
                        "                {\n" +
                        "                    m.invoke(result, keyValue._value());\n" +
                        "                }\n" +
                        "                catch (InvocationTargetException | IllegalAccessException e)\n" +
                        "                {\n" +
                        "                    Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;\n" +
                        "                    StringBuilder builder = new StringBuilder(\"Error setting property '\").append(keyValue._key()).append(\"' for instance of \");\n" +
                        "                    org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, aClass);\n" +
                        "                    String eMessage = cause.getMessage();\n" +
                        "                    if (eMessage != null)\n" +
                        "                    {\n" +
                        "                        builder.append(\": \").append(eMessage);\n" +
                        "                    }\n" +
                        "                    throw new RuntimeException(builder.toString(), cause);\n" +
                        "                }\n" +
                        "\n" +
                        "            }\n" +
                        "        });\n" +
                        "        PureFunction2Wrapper getterToOneExecFunc = getterToOneExec == null ? null : new PureFunction2Wrapper(getterToOneExec, es);\n" +
                        "        PureFunction2Wrapper getterToManyExecFunc = getterToManyExec == null ? null : new PureFunction2Wrapper(getterToManyExec, es);\n" +
                        "        ElementOverride elementOverride = override;\n" +
                        "        if (override instanceof GetterOverride)\n" +
                        "        {\n" +
                        "            elementOverride = ((GetterOverride) elementOverride)._getterOverrideToOne(getterToOne)._getterOverrideToMany(getterToMany)._hiddenPayload(payload);\n" +
                        "            ((GetterOverrideExecutor) elementOverride).__getterOverrideToOneExec(getterToOneExecFunc);\n" +
                        "            ((GetterOverrideExecutor) elementOverride).__getterOverrideToManyExec(getterToManyExecFunc);\n" +
                        "        }\n" +
                        "        result._elementOverride(elementOverride);\n" +
                        "        return result;\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object newObject\n" +
                        "            (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType\n" +
                        "                     genericType, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> root_meta_pure_functions_lang_keyExpressions, ElementOverride\n" +
                        "                     override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function\n" +
                        "                     getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object\n" +
                        "                     payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return newObject((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);\n" +
                        "    }\n" +
                        "}"));
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform";
    }

    public static CompiledExtension extension()
    {
        return new CoreExtensionCompiled();
    }
}
