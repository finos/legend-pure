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
                        "import org.eclipse.collections.api.block.function.Function0;\n" +
                        "import org.eclipse.collections.api.factory.Lists;\n" +
                        "import org.eclipse.collections.api.list.MutableList;\n" +
                        "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                        "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.GetterOverride;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;\n" +
                        "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.CoreHelper;\n" +
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
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction2;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedProcedure;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;\n" +
                        "\n" +
                        "import java.lang.reflect.Constructor;\n" +
                        "import java.lang.reflect.InvocationTargetException;\n" +
                        "import java.lang.reflect.Method;\n" +
                        "import java.math.BigDecimal;\n" +
                        "\n" +
                        "public class CoreGen extends CoreHelper\n" +
                        "{\n" +
                        "    public static final Bridge bridge = new BridgeImpl();\n" +
                        "\n" +
                        "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.safeGetGenericType(val, new DefendedFunction0<GenericType>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public GenericType value()\n" +
                        "            {\n" +
                        "                return new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"\");\n" +
                        "            }\n" +
                        "        }, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static SharedPureFunction getSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.getSharedPureFunction(func, bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object evaluate(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Object... instances)\n" +
                        "    {\n" +
                        "        return Pure.evaluate(es, func, bridge, instances);\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    public static Object evaluateToMany(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, RichIterable<? extends List<?>> instances)\n" +
                        "    {\n" +
                        "        MutableList<Object> inputs = Lists.mutable.of();\n" +
                        "        if (instances != null)\n" +
                        "        {\n" +
                        "            for (List<?> obj : instances)\n" +
                        "            {\n" +
                        "                inputs.add(obj._values());\n" +
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
                        "    public static <T, V> RichIterable<T> removeDuplicates(RichIterable<T> list, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> eqlFn, ExecutionSupport es)\n" +
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
                        "    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, String name, RichIterable<? extends KeyExpression> root_meta_pure_functions_lang_keyExpressions, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.newObject(bridge, aClass, name, root_meta_pure_functions_lang_keyExpressions, es);\n" +
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
                        "    public static String format(String formatString, Object formatArgs, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return CompiledSupport.format(formatString, formatArgs, new DefendedFunction2<Object, ExecutionSupport, String>()\n" +
                        "        {\n" +
                        "            public String value(Object any, ExecutionSupport executionSupport)\n" +
                        "            {\n" +
                        "                return toRepresentation(any, executionSupport);\n" +
                        "            }\n" +
                        "        }, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static String toRepresentation(Object any, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        if (any instanceof String)\n" +
                        "        {\n" +
                        "            return \"'\" + CompiledSupport.replace((String) any, \"'\", \"\\\\'\") + \"'\";\n" +
                        "        }\n" +
                        "        if (any instanceof org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate)\n" +
                        "        {\n" +
                        "            return \"%\" + CompiledSupport.pureToString((PureDate) any, es);\n" +
                        "        }\n" +
                        "        if (any instanceof BigDecimal)\n" +
                        "        {\n" +
                        "            return CompiledSupport.pureToString((BigDecimal) any, es) + \"D\";\n" +
                        "        }\n" +
                        "        if (any instanceof Number)\n" +
                        "        {\n" +
                        "            return CompiledSupport.pureToString((Number) any, es);\n" +
                        "        }\n" +
                        "        if (any instanceof Boolean)\n" +
                        "        {\n" +
                        "            return CompiledSupport.pureToString(((Boolean) any).booleanValue(), es);\n" +
                        "        }\n" +
                        "        if (any instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)\n" +
                        "        {\n" +
                        "            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement p = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) any;\n" +
                        "            if (p._name() != null)\n" +
                        "            {\n" +
                        "                return Pure.elementToPath(p, \"::\");\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return \"<\" + Pure.manageId(any) + \"instanceOf \" + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) CoreGen.safeGetGenericType(any, es)._rawType(), \"::\") + \">\";\n" +
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
                        "        return newObject((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    public static PureMap newMap(RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs, RichIterable<? extends Property<?, ?>> properties, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return newMap(pairs, properties, CoreGen.bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(Object l1, Object l2)\n" +
                        "    {\n" +
                        "        return zip(l1, l2, new DefendedFunction0<Pair<U, V>>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> value()\n" +
                        "            {\n" +
                        "                return new Root_meta_pure_functions_collection_Pair_Impl<U, V>(\"\");\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2)\n" +
                        "    {\n" +
                        "        return zip(l1, l2, new DefendedFunction0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> value()\n" +
                        "            {\n" +
                        "                return new Root_meta_pure_functions_collection_Pair_Impl<U, V>(\"\");\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(Object l1, Object l2, Function0<? extends Pair<U, V>> pairBuilder)\n" +
                        "    {\n" +
                        "        return zipImpl((RichIterable<? extends U>) l1, (RichIterable<? extends V>) l2, pairBuilder);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2, final Function0<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> pairBuilder)\n" +
                        "    {\n" +
                        "        return zipImpl(l1, l2, pairBuilder);\n" +
                        "    }\n" +
                        "\n" +
                        "    private static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zipImpl(RichIterable<? extends U> l1, RichIterable<? extends V> l2, final Function0<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> pairBuilder)\n" +
                        "    {\n" +
                        "        return l1 == null || l2 == null ? FastList.<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>>newList() : l1.zip(l2).collect(new DefendedFunction<org.eclipse.collections.api.tuple.Pair<? extends U, ? extends V>, Pair<U, V>>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> valueOf(org.eclipse.collections.api.tuple.Pair<? extends U, ? extends V> pair)\n" +
                        "            {\n" +
                        "                return pairBuilder.value()._first(pair.getOne())._second(pair.getTwo());\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object dynamicMatchWith(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return dynamicMatchWith(obj, funcs, var, CoreGen.bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static PureMap getOpenVariables(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func)\n" +
                        "    {\n" +
                        "        return Pure.getOpenVariables(func, CoreGen.bridge);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, true, CoreGen.bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, boolean allowJavaCompilation, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, allowJavaCompilation, CoreGen.bridge, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Double random()\n" +
                        "    {\n" +
                        "        return Math.random();\n" +
                        "    }\n" +
                        "\n" +
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
