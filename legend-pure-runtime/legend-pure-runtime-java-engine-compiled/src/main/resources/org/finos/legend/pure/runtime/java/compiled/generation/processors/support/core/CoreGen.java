// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.generated;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.GetterOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.CoreHelper;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.GetterOverrideExecutor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.QuantityCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2Wrapper;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction0;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedProcedure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.SecureRandom;

public class CoreGen extends CoreHelper
{
    public static final Bridge bridge = new Bridge()
    {
        @Override
        public <T> List<T> buildList()
        {
            return new Root_meta_pure_functions_collection_List_Impl<>("");
        }

        @Override
        public LambdaCompiledExtended buildLambda(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> lambdaFunction, SharedPureFunction<Object> pureFunction)
        {
            return new PureCompiledLambda(lambdaFunction, pureFunction);
        }
    };

    private static final SecureRandom random = new SecureRandom();

    public static GenericType safeGetGenericType(Object val, ExecutionSupport es)
    {
        return Pure.safeGetGenericType(val, new DefendedFunction0<GenericType>()
        {
            @Override
            public GenericType value()
            {
                return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("");
            }
        }, es);
    }

    public static SharedPureFunction getSharedPureFunction(Function<?> func, ExecutionSupport es)
    {
        return Pure.getSharedPureFunction(func, bridge, es);
    }

    public static Object evaluate(ExecutionSupport es, Function<?> func, Object... instances)
    {
        return Pure.evaluate(es, func, bridge, instances);
    }


    public static Object evaluateToMany(ExecutionSupport es, Function<?> func, RichIterable<? extends List<?>> instances)
    {
        MutableList<Object> inputs = Lists.mutable.empty();
        if (instances != null)
        {
            for (List<?> obj : instances)
            {
                inputs.add(obj._values());
            }
        }
        return Pure._evaluateToMany(es, bridge, func, inputs);
    }

    public static Object dynamicMatch(Object obj, RichIterable<Function<?>> funcs, ExecutionSupport es)
    {
        return Pure.dynamicMatch(obj, funcs, bridge, es);
    }

    private static Object dynamicMatch(Object obj, RichIterable<Function<?>> funcs, Object var, boolean isMatchWith, ExecutionSupport es)
    {
        return Pure.dynamicMatch(obj, funcs, var, isMatchWith, bridge, es);
    }

    public static <T, V> RichIterable<T> removeDuplicates(RichIterable<T> list, Function<?> keyFn, Function<?> eqlFn, ExecutionSupport es)
    {
        return Pure.removeDuplicates(list, keyFn, eqlFn, bridge, es);
    }


    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es)
    {
        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(Maps.mutable.empty()), bridge);
    }

    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, PureMap lambdaOpenVariablesMap)
    {
        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge);
    }

    public static Object newObject(Class<?> aClass, String name, RichIterable<? extends KeyExpression> keyExpressions, ExecutionSupport es)
    {
        return Pure.newObject(bridge, aClass, name, keyExpressions, es);
    }

    public static String format(String formatString, Object formatArgs, ExecutionSupport es)
    {
        return CompiledSupport.format(formatString, formatArgs, new DefendedFunction2<Object, ExecutionSupport, String>()
        {
            public String value(Object any, ExecutionSupport executionSupport)
            {
                return toRepresentation(any, executionSupport);
            }
        }, es);
    }

    public static String toRepresentation(Object any, ExecutionSupport es)
    {
        if (any instanceof String)
        {
            return "'" + CompiledSupport.replace((String) any, "'", "\\'") + "'";
        }
        if (any instanceof org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate)
        {
            return "%" + CompiledSupport.pureToString((PureDate) any, es);
        }
        if (any instanceof BigDecimal)
        {
            return CompiledSupport.pureToString((BigDecimal) any, es) + "D";
        }
        if (any instanceof Number)
        {
            return CompiledSupport.pureToString((Number) any, es);
        }
        if (any instanceof Boolean)
        {
            return CompiledSupport.pureToString(((Boolean) any).booleanValue(), es);
        }
        if (any instanceof PackageableElement)
        {
            PackageableElement p = (PackageableElement) any;
            if (p._name() != null)
            {
                return Pure.elementToPath(p, "::");
            }
        }
        if (any instanceof QuantityCoreInstance)
        {
            return ((QuantityCoreInstance) any).getName();
        }
        return "<" + Pure.manageId(any) + " instanceOf " + Pure.elementToPath((PackageableElement) CoreGen.safeGetGenericType(any, es)._rawType(), "::") + ">";
    }

    public static RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> processKeyExpressions(java.lang.Class<?> _class, Object instance, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ExecutionSupport es)
    {
        try
        {
            Method method = _class.getMethod("defaultValues", ExecutionSupport.class);
            MutableList<org.eclipse.collections.api.tuple.Pair<String, RichIterable<? extends Object>>> vals = (MutableList<org.eclipse.collections.api.tuple.Pair<String, RichIterable<? extends Object>>>) method.invoke(instance, es);
            MutableMap<String, Root_meta_pure_functions_lang_KeyValue> defaultVals = Maps.mutable.empty();
            MutableMap<String, Root_meta_pure_functions_lang_KeyValue> given = Maps.mutable.empty();

            MutableList<Root_meta_pure_functions_lang_KeyValue> defaultVals_L = vals.collect(new DefendedFunction<org.eclipse.collections.api.tuple.Pair<String, RichIterable<? extends Object>>, Root_meta_pure_functions_lang_KeyValue>()
            {
                @Override
                public Root_meta_pure_functions_lang_KeyValue valueOf(org.eclipse.collections.api.tuple.Pair<String, RichIterable<?>> v)
                {
                    return new Root_meta_pure_functions_lang_KeyValue_Impl("")._key(v.getOne())._value(v.getTwo());
                }
            });
            MutableList<? extends Root_meta_pure_functions_lang_KeyValue> givenL = keyExpressions.toList();
            for (Root_meta_pure_functions_lang_KeyValue kv : defaultVals_L)
            {
                defaultVals.put(kv._key(), kv);
            }
            for (Root_meta_pure_functions_lang_KeyValue kv : givenL)
            {
                given.put(kv._key(), kv);

            }
            defaultVals.putAll(given);
            return defaultVals.valuesView();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Object newObject(final Class<?> aClass, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ElementOverride override, Function getterToOne, Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
    {
        final ClassCache classCache = ((CompiledExecutionSupport) es).getClassCache();
        Constructor<?> constructor = classCache.getIfAbsentPutConstructorForType(aClass);
        final Any result;
        try
        {
            result = (Any) constructor.newInstance("");
            keyExpressions = processKeyExpressions(classCache.getIfAbsentPutImplForType(aClass), result, keyExpressions, es);
        }
        catch (InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
            StringBuilder builder = new StringBuilder("Error instantiating ");
            org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, aClass);
            String eMessage = cause.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new RuntimeException(builder.toString(), cause);
        }
        keyExpressions.forEach(new DefendedProcedure<Root_meta_pure_functions_lang_KeyValue>()
        {
            @Override
            public void value(Root_meta_pure_functions_lang_KeyValue keyValue)
            {
                Method m = classCache.getIfAbsentPutPropertySetterMethodForType(aClass, keyValue._key());
                try
                {
                    m.invoke(result, keyValue._value());
                }
                catch (InvocationTargetException | IllegalAccessException e)
                {
                    Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
                    StringBuilder builder = new StringBuilder("Error setting property '").append(keyValue._key()).append("' for instance of ");
                    org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, aClass);
                    String eMessage = cause.getMessage();
                    if (eMessage != null)
                    {
                        builder.append(": ").append(eMessage);
                    }
                    throw new RuntimeException(builder.toString(), cause);
                }

            }
        });
        PureFunction2Wrapper getterToOneExecFunc = getterToOneExec == null ? null : new PureFunction2Wrapper(getterToOneExec, es);
        PureFunction2Wrapper getterToManyExecFunc = getterToManyExec == null ? null : new PureFunction2Wrapper(getterToManyExec, es);
        ElementOverride elementOverride = override;
        if (override instanceof GetterOverride)
        {
            elementOverride = ((GetterOverride) elementOverride)._getterOverrideToOne(getterToOne)._getterOverrideToMany(getterToMany)._hiddenPayload(payload);
            ((GetterOverrideExecutor) elementOverride).__getterOverrideToOneExec(getterToOneExecFunc);
            ((GetterOverrideExecutor) elementOverride).__getterOverrideToManyExec(getterToManyExecFunc);
        }
        result._elementOverride(elementOverride);
        return result;
    }

    public static Object newObject(GenericType genericType,
                                   RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> root_meta_pure_functions_lang_keyExpressions,
                                   ElementOverride override,
                                   Function getterToOne,
                                   Function getterToMany,
                                   Object payload,
                                   PureFunction2 getterToOneExec,
                                   PureFunction2 getterToManyExec,
                                   ExecutionSupport es)
    {
        return newObject((Class<?>) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);
    }

    public static PureMap newMap(RichIterable<? extends Pair<?, ?>> pairs, RichIterable<? extends Property<?, ?>> properties, ExecutionSupport es)
    {
        return newMap(pairs, properties, CoreGen.bridge, es);
    }

    public static <U, V> RichIterable<Pair<U, V>> zip(Object l1, Object l2)
    {
        return zip(l1, l2, new DefendedFunction0<Pair<U, V>>()
        {
            @Override
            public Pair<U, V> value()
            {
                return new Root_meta_pure_functions_collection_Pair_Impl<U, V>("");
            }
        });
    }

    public static <U, V> RichIterable<Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2)
    {
        return zip(l1, l2, new DefendedFunction0<Pair<U, V>>()
        {
            @Override
            public Pair<U, V> value()
            {
                return new Root_meta_pure_functions_collection_Pair_Impl<U, V>("");
            }
        });
    }

    public static <U, V> RichIterable<Pair<U, V>> zip(Object l1, Object l2, Function0<? extends Pair<U, V>> pairBuilder)
    {
        return zipImpl((RichIterable<? extends U>) l1, (RichIterable<? extends V>) l2, pairBuilder);
    }

    public static <U, V> RichIterable<Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2, final Function0<? extends Pair<U, V>> pairBuilder)
    {
        return zipImpl(l1, l2, pairBuilder);
    }

    private static <U, V> RichIterable<Pair<U, V>> zipImpl(RichIterable<? extends U> l1, RichIterable<? extends V> l2, final Function0<? extends Pair<U, V>> pairBuilder)
    {
        return (l1 == null) || (l2 == null) ?
               Lists.immutable.<Pair<U, V>>empty() :
               l1.zip(l2).collect(new DefendedFunction<org.eclipse.collections.api.tuple.Pair<? extends U, ? extends V>, Pair<U, V>>()
               {
                   @Override
                   public Pair<U, V> valueOf(org.eclipse.collections.api.tuple.Pair<? extends U, ? extends V> pair)
                   {
                       return pairBuilder.value()._first(pair.getOne())._second(pair.getTwo());
                   }
               });
    }

    public static Object dynamicMatchWith(Object obj, RichIterable<Function<?>> funcs, Object var, ExecutionSupport es)
    {
        return dynamicMatchWith(obj, funcs, var, CoreGen.bridge, es);
    }

    public static PureMap getOpenVariables(Function<?> func)
    {
        return Pure.getOpenVariables(func, CoreGen.bridge);
    }

    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es)
    {
        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, true, CoreGen.bridge, es);
    }

    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, boolean allowJavaCompilation, ExecutionSupport es)
    {
        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, allowJavaCompilation, CoreGen.bridge, es);
    }

    public static Double random()
    {
        return random.nextDouble();
    }
}
