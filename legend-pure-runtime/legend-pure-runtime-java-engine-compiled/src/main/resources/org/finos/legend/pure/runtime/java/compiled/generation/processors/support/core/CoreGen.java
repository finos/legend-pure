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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.GetterOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.CoreHelper;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.GetterOverrideExecutor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
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
        return org.finos.legend.pure.generated.platform_pure_essential_string_toString_toRepresentation.Root_meta_pure_functions_string_toRepresentation_Any_1__String_1_(any, es);
    }

    public static RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> processKeyExpressions(java.lang.Class<?> _class, Object instance, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ExecutionSupport es)
    {
        MutableList<Root_meta_pure_functions_lang_KeyValue> result = Lists.mutable.empty();
        MutableSet<String> keys = Sets.mutable.empty();
        for (Root_meta_pure_functions_lang_KeyValue kv : keyExpressions)
        {
            result.add(kv);
            keys.add(kv._key());
        }
        JavaCompiledCoreInstance coreInstance = (JavaCompiledCoreInstance) instance;
        for (String key : coreInstance.getDefaultValueKeys())
        {
            if (!keys.contains(key))
            {
                RichIterable<?> defaultValue = coreInstance.getDefaultValue(key, es);
                if ((defaultValue != null) && !defaultValue.isEmpty())
                {
                    result.add(new Root_meta_pure_functions_lang_KeyValue_Impl("")._key(key)._value(defaultValue));
                }
            }
        }
        return result;
    }

    public static Object newObject(final Class<?> aClass, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ElementOverride override, Function getterToOne, Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
    {
        return newObject(aClass, keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, null, es);
    }

    public static Object newObject(final Class<?> aClass, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ElementOverride override, Function getterToOne, Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, final SourceInformation sourceInfo, final ExecutionSupport es)
    {
        final CompiledExecutionSupport ces = (CompiledExecutionSupport) es;
        final ClassCache classCache = ces.getClassCache();
        final ProcessorSupport processorSupport = ces.getProcessorSupport();
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

        // Set source information on the instance if provided, but NOT on FunctionDefinitions
        // (setting source info on FunctionDefinitions breaks function lookup by name)
        if (sourceInfo != null && result instanceof CoreInstance && !org.finos.legend.pure.m3.navigation.function.Function.isFunctionDefinition((CoreInstance) result, processorSupport))
        {
            ((CoreInstance) result).setSourceInformation(sourceInfo);
        }

        // Build a GenericType for property type resolution
        // We use the instance's classifierGenericType if set (from key values), otherwise a basic one from the class
        // This is needed to properly resolve type parameters for generic classes like L<T>
        final CoreInstance basicGenericType = processorSupport.type_wrapGenericType(aClass);

        // Check if the class has type parameters - if so, we can only validate if the instance has a
        // classifierGenericType with type arguments set (from key values)
        final boolean classHasTypeParameters = !aClass._typeParameters().isEmpty();

        keyExpressions.forEach(new DefendedProcedure<Root_meta_pure_functions_lang_KeyValue>()
        {
            @Override
            public void value(Root_meta_pure_functions_lang_KeyValue keyValue)
            {
                String key = keyValue._key();
                RichIterable<?> values = keyValue._value();

                // Skip classifierGenericType validation - it's metadata, not a real property to validate
                if (!"classifierGenericType".equals(key))
                {
                    // Only validate if the class doesn't have type parameters, or if we can properly resolve them
                    // For generic classes like L<T>, we rely on outer validation (when assigned to properties like M.m)
                    if (!classHasTypeParameters)
                    {
                        // Validate type compatibility for each value
                        CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(aClass, key);
                        if (property != null)
                        {
                            CoreInstance propertyGenericType = org.finos.legend.pure.m3.navigation.generictype.GenericType.resolvePropertyReturnType(basicGenericType, property, processorSupport);
                            for (Object val : values)
                            {
                                CoreInstance valGenericType = safeGetGenericType(val, es);
                                validatePropertyType(propertyGenericType, valGenericType, val, sourceInfo, processorSupport);
                            }
                        }
                    }
                }

                Method m = classCache.getIfAbsentPutPropertySetterMethodForType(aClass, key);
                try
                {
                    m.invoke(result, values);
                }
                catch (InvocationTargetException | IllegalAccessException e)
                {
                    Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
                    StringBuilder builder = new StringBuilder("Error setting property '").append(key).append("' for instance of ");
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

    private static SourceInformation getSourceInfoFromValue(Object val)
    {
        // Try to get source info from the value itself
        if (val instanceof CoreInstance)
        {
            SourceInformation sourceInfo = ((CoreInstance) val).getSourceInformation();
            if (sourceInfo != null)
            {
                return sourceInfo;
            }
            // If value doesn't have source info, try its classifierGenericType
            if (val instanceof Any)
            {
                GenericType gt = ((Any) val)._classifierGenericType();
                if (gt != null)
                {
                    return ((CoreInstance) gt).getSourceInformation();
                }
            }
        }
        return null;
    }

    private static void validatePropertyType(CoreInstance propertyGenericType, CoreInstance valGenericType, Object val, SourceInformation fallbackSourceInfo, ProcessorSupport processorSupport)
    {
        // Use source info from value if available, otherwise use the fallback (from dynamicNew call site)
        SourceInformation sourceInfo = getSourceInfoFromValue(val);
        if (sourceInfo == null)
        {
            sourceInfo = fallbackSourceInfo;
        }

        boolean compatible;
        try
        {
            compatible = org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(valGenericType, propertyGenericType, processorSupport);
        }
        catch (Exception e)
        {
            String valTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(valGenericType, false, processorSupport);
            String propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, false, processorSupport);
            if (valTypeString.equals(propertyTypeString))
            {
                valTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(valGenericType, true, processorSupport);
                propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, true, processorSupport);
            }
            throw new PureExecutionException(sourceInfo, "Error checking if value type '" + valTypeString + "' is compatible with property type '" + propertyTypeString + "'", e, Stacks.mutable.<CoreInstance>empty());
        }
        if (!compatible)
        {
            String valTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(valGenericType, false, processorSupport);
            String propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, false, processorSupport);
            if (valTypeString.equals(propertyTypeString))
            {
                valTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(valGenericType, true, processorSupport);
                propertyTypeString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(propertyGenericType, true, processorSupport);
            }
            throw new PureExecutionException(sourceInfo, "Type Error: '" + valTypeString + "' not a subtype of '" + propertyTypeString + "'", Stacks.mutable.<CoreInstance>empty());
        }
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
        return newObject((Class<?>) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, null, es);
    }

    public static Object newObject(GenericType genericType,
                                   RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> root_meta_pure_functions_lang_keyExpressions,
                                   ElementOverride override,
                                   Function getterToOne,
                                   Function getterToMany,
                                   Object payload,
                                   PureFunction2 getterToOneExec,
                                   PureFunction2 getterToManyExec,
                                   SourceInformation sourceInfo,
                                   ExecutionSupport es)
    {
        return newObject((Class<?>) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, sourceInfo, es);
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

    public static boolean assertError(ExecutionSupport es, Function<?> func, String message, long line, long column, SourceInformation sourceInformation)
    {
        try
        {
            Pure.evaluate(es, func, bridge);
            throw new PureAssertFailException(sourceInformation, "No error was thrown");
        }
        catch (PureExecutionException e)
        {
            if ((message != null) && !message.equals(e.getInfo()))
            {
                throw new PureAssertFailException(sourceInformation, "Execution error message mismatch.\nThe actual message was \"" + e.getInfo() + "\"\nwhere the expected message was:\"" + message + "\"");
            }
            if ((line != -1) && (e.getSourceInformation().getLine() != line))
            {
                throw new PureAssertFailException(sourceInformation, "Execution error line mismatch. Actual: " + e.getSourceInformation().getLine() + " where expected: " + line);
            }
            if ((column != -1) && (e.getSourceInformation().getColumn() != column))
            {
                throw new PureAssertFailException(sourceInformation, "Execution error column mismatch. Actual: " + e.getSourceInformation().getColumn() + " where expected: " + column);
            }
        }
        return true;
    }
}
