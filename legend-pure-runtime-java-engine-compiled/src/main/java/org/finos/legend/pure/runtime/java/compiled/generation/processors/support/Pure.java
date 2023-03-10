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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.procedure.checked.CheckedProcedure;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ModelElementAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Nil;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.router.RoutedValueSpecification;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureDynamicReactivateException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.JavaMethodWithParamsSharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.json.simple.JSONObject;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Pure
{
    public static String elementToPath(PackageableElement element, String separator)
    {
        MutableList<PackageableElement> elements = Lists.mutable.empty();
        elements(element, elements);
        return elements.toReversed().subList(1, elements.size()).collect(ModelElementAccessor::_name).makeString(separator);
    }

    private static void elements(PackageableElement element, List<PackageableElement> elements)
    {
        elements.add(element);
        if (element._package() != null)
        {
            elements(element._package(), elements);
        }
    }


    public static boolean isToOne(Multiplicity multiplicity)
    {
        return multiplicity._lowerBound()._value() == 1L && hasToOneUpperBound(multiplicity);
    }

    public static boolean hasToOneUpperBound(Multiplicity multiplicity)
    {
        return multiplicity._upperBound() != null && multiplicity._upperBound()._value() != null && multiplicity._upperBound()._value() == 1L;
    }

    public static CoreInstance getProperty(String className, String propertyName, MetadataAccessor ma)
    {
        if (propertyName == null)
        {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cls = ma.getClass(className);
        for (CoreInstance property : cls._propertiesFromAssociations())
        {
            if (propertyName.equals(property.getName()))
            {
                return property;
            }
        }
        for (CoreInstance property : cls._properties())
        {
            if (propertyName.equals(property.getName()))
            {
                return property;
            }
        }
        throw new PureExecutionException("Can't find the property '" + propertyName + "' in the class '" + className + "'");
    }

    public static <E> E getEnumByName(Enumeration<E> enumeration, String name)
    {
        return enumeration._values().detect(e -> name.equals(((Enum) e)._name()));
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, MetadataAccessor ma, Supplier<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> genericTypeBuilder, ProcessorSupport processorSupport)
    {
        if (val == null)
        {
            return genericTypeBuilder.get()._rawType(ma.getBottomType());
        }
        if (val instanceof Any)
        {
            Any a = (Any) val;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType = a._classifierGenericType();
            return (genericType == null) ? genericTypeBuilder.get()._rawType(CompiledSupport.getType(a, ma)) : genericType;
        }
        if ((val instanceof Long) || (val instanceof BigInteger))
        {
            Type type = ma.getPrimitiveType("Integer");
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof String)
        {
            Type type = ma.getPrimitiveType("String");
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof Boolean)
        {
            Type type = ma.getPrimitiveType("Boolean");
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof PureDate)
        {
            Type type = ma.getPrimitiveType(DateFunctions.datePrimitiveType((PureDate) val));
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof Double)
        {
            Type type = ma.getPrimitiveType("Float");
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof BigDecimal)
        {
            Type type = ma.getPrimitiveType("Decimal");
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof RichIterable)
        {
            RichIterable<?> l = (RichIterable<?>) val;
            if (l.isEmpty())
            {
                return genericTypeBuilder.get()._rawType(ma.getBottomType());
            }
            if (l.size() == 1)
            {
                return safeGetGenericType(l.getFirst(), ma, genericTypeBuilder, processorSupport);
            }
            RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> r = l.collect(o -> safeGetGenericType(o, ma, genericTypeBuilder, processorSupport));
            MutableSet<CoreInstance> s = Sets.mutable.empty();
            Type t = r.getFirst()._rawType();
            r.forEach(a ->
            {
                if (a._rawType() == null)
                {
                    throw new PureExecutionException("TODO: Find most common type for non-concrete generic type");
                }
                if (t != a._rawType())
                {
                    s.add(a);
                }
            });
            if (s.isEmpty())
            {
                return r.getFirst();
            }

            s.add(r.getFirst());
            return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) GenericType.findBestCommonGenericType(s.toList(), true, false, processorSupport);
        }
        throw new PureExecutionException("ERROR unhandled type for value: " + val + " (instance of " + val.getClass() + ")");
    }


    public static SharedPureFunction<?> getSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        SharedPureFunction<?> foundFunc = findSharedPureFunction(func, bridge, es);
        if (foundFunc == null)
        {
            throw new PureExecutionException("Can't execute " + func + " | name:'" + func._name() + "' id:'" + func.getName() + "' yet");
        }
        return foundFunc;
    }

    @SuppressWarnings("unchecked")
    public static SharedPureFunction<?> findSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        MutableList<PureFunction1<Object, Object>> extra = ((CompiledExecutionSupport) es).getCompiledExtensions().asLazy().collect(x -> x.getExtraFunctionEvaluation(func, bridge, es)).select(Objects::nonNull, Lists.mutable.empty());
        if (extra.size() == 1)
        {
            return extra.get(0);
        }
        if (extra.size() > 1)
        {
            throw new RuntimeException("Error");
        }
        if (func instanceof Property)
        {
            Type srcType = func._classifierGenericType()._typeArguments().getFirst()._rawType();
            return ((CompiledExecutionSupport) es).getFunctionCache().getIfAbsentPutFunctionForClassProperty(srcType, func, ((CompiledExecutionSupport) es).getClassLoader());
        }
        if (func instanceof LambdaCompiledExtended)
        {
            return ((LambdaCompiledExtended) func).pureFunction();
        }
        if (func instanceof LambdaFunction)
        {
            LambdaFunction<?> lambda = (LambdaFunction<?>) func;
            if (canFindNativeOrLambdaFunction(es, func))
            {
                return getNativeOrLambdaFunction(es, func);
            }
            if (Reactivator.canReactivateWithoutJavaCompilation(lambda, es, getOpenVariables(func, bridge), bridge))
            {
                PureMap openVariablesMap = getOpenVariables(func, bridge);
                return DynamicPureFunctionImpl.createPureFunction(lambda, openVariablesMap.getMap(), bridge);
            }
            return ((LambdaCompiledExtended) CompiledSupport.dynamicallyBuildLambdaFunction(func, es)).pureFunction();
        }
        if (func instanceof ConcreteFunctionDefinition)
        {
            if (func.getSourceInformation() != null)
            {
                return ((CompiledExecutionSupport) es).getFunctionCache().getIfAbsentPutJavaFunctionForPureFunction(func, () ->
                        {
                            try
                            {
                                RichIterable<? extends VariableExpression> params = ((FunctionType) func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();
                                Class<?>[] paramClasses = new Class[params.size() + 1];
                                int index = 0;
                                for (VariableExpression o : params)
                                {
                                    paramClasses[index] = pureTypeToJavaClassForExecution(o, bridge, es);
                                    index++;
                                }
                                paramClasses[params.size()] = ExecutionSupport.class;
                                Method method = ((CompiledExecutionSupport) es).getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + IdBuilder.sourceToId(func.getSourceInformation())).getMethod(FunctionProcessor.functionNameToJava(func), paramClasses);
                                return new JavaMethodWithParamsSharedPureFunction<>(method, paramClasses, func.getSourceInformation());
                            }
                            catch (RuntimeException e)
                            {
                                throw e;
                            }
                            catch (Exception e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
            else
            {
                PureMap openVars = new PureMap(Maps.mutable.empty());
                if (Reactivator.canReactivateWithoutJavaCompilation(func, es, openVars, bridge))
                {
                    return DynamicPureFunctionImpl.createPureFunction((FunctionDefinition<?>) func, openVars.getMap(), bridge);
                }
            }
        }

        MutableMap<String, SharedPureFunction<?>> functions;
        try
        {
            Class<?> myClass = ((CompiledExecutionSupport) es).getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + IdBuilder.sourceToId(func.getSourceInformation()));
            functions = (MutableMap<String, SharedPureFunction<?>>) myClass.getDeclaredField("__functions").get(null);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return functions.get(func.getName());
    }

    public static Object evaluate(ExecutionSupport es,
                                  org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func,
                                  Bridge bridge,
                                  Object... instances)
    {
        if (func instanceof ConcreteFunctionDefinition)
        {
            SharedPureFunction<?> pureFunc = getSharedPureFunction(func, bridge, es);
            if (pureFunc instanceof JavaMethodWithParamsSharedPureFunction)
            {
                JavaMethodWithParamsSharedPureFunction<?> p = (JavaMethodWithParamsSharedPureFunction<?>) pureFunc;
                Class<?>[] paramClasses = p.getParametersTypes();
                int l = paramClasses.length;
                MutableList<Object> paramInstances = Lists.mutable.ofInitialCapacity(l - 1);
                for (int i = 0; i < (l - 1); i++)
                {
                    if (instances[i] instanceof RichIterable && paramClasses[i] != RichIterable.class)
                    {
                        paramInstances.add(CompiledSupport.toOne(((RichIterable<?>) instances[i]), null));
                    }
                    else
                    {
                        paramInstances.add(instances[i]);
                    }
                }
                return p.execute(paramInstances, es);
            }
            else
            {
                pureFunc.execute(Lists.fixedSize.with(instances), es);
            }
        }
        SharedPureFunction<?> reflectiveNative = getSharedPureFunction(func, bridge, es);
        return reflectiveNative.execute(instances == null ? Lists.mutable.empty() : Lists.mutable.with(instances), es);
    }

    public static Object _evaluateToMany(ExecutionSupport es, Bridge bridge, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, ListIterable<?> paramInputs)
    {
        if (func instanceof Property)
        {
            try
            {
                Object o = ((RichIterable<?>) paramInputs.getFirst()).getFirst();
                return o.getClass().getMethod("_" + func.getName()).invoke(o);
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        RichIterable<? extends VariableExpression> params = ((FunctionType) func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();
        Class<?>[] paramClasses = new Class<?>[params.size()];
        int index = 0;
        for (VariableExpression o : params)
        {
            paramClasses[index] = pureTypeToJavaClassForExecution(o, bridge, es);
            index++;
        }

        Object[] paramInstances = new Object[params.size()];
        Iterator<?> iterator = paramInputs.iterator();
        for (int i = 0; i < params.size(); i++)
        {
            paramInstances[i] = (paramClasses[i] == RichIterable.class) ? iterator.next() : ((RichIterable<?>) iterator.next()).getFirst();
        }
        try
        {
            if (func instanceof QualifiedProperty)
            {
                Object o = ((RichIterable<?>) paramInputs.getFirst()).getFirst();
                return CompiledSupport.executeMethod(o.getClass(), func._functionName(), func, Arrays.copyOfRange(paramClasses, 1, paramClasses.length),
                        o, Arrays.copyOfRange(paramInstances, 1, paramInstances.length), es);
            }
            if (func instanceof NativeFunction || func instanceof LambdaFunction || func instanceof ConcreteFunctionDefinition)
            {
                SharedPureFunction<?> foundFunc = findSharedPureFunction(func, bridge, es);
                if (foundFunc == null)
                {
                    StringBuilder builder = new StringBuilder("Can't execute ").append(func).append(" | name: ");
                    String name = func._name();
                    if (name == null)
                    {
                        builder.append("null");
                    }
                    else
                    {
                        builder.append("'").append(name).append("'");
                    }
                    builder.append(" id: '").append(func.getName()).append("' yet");
                    throw new PureExecutionException(func.getSourceInformation(), builder.toString());
                }
                return foundFunc.execute(Lists.mutable.with(paramInstances), es);
            }
            throw new PureExecutionException(func.getSourceInformation(), "Unknown function type:" + func.getClass().getName());
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static SharedPureFunction<?> getNativeOrLambdaFunction(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func)
    {
        return ((CompiledExecutionSupport) es).getFunctionCache().getIfAbsentPutJavaFunctionForPureFunction(func, () ->
        {
            try
            {
                Class<?> myClass = ((CompiledExecutionSupport) es).getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + IdBuilder.sourceToId(func.getSourceInformation()));
                MutableMap<String, SharedPureFunction<?>> functions = (MutableMap<String, SharedPureFunction<?>>) myClass.getDeclaredField("__functions").get(null);
                return functions.get(func.getName());
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public static boolean canFindNativeOrLambdaFunction(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func)
    {
        try
        {
            return getNativeOrLambdaFunction(es, func) != null;
        }
        catch (Exception ignore)
        {
            return false;
        }
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> genericTypeClass(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType)
    {
        Type t = genericType._rawType();
        if (t instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)
        {
            return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) t;
        }
        return null;
    }

    public static String manageId(Object obj)
    {
        return (obj instanceof Any) ? ((Any) obj).getName() : String.valueOf(obj);
    }


    public static PureMap getOpenVariables(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge)
    {
        MutableMap<String, Object> map = Maps.mutable.empty();
        if (func instanceof LambdaFunction)
        {
            //In the case of LambdaFunction_Impl, we do not need to concern with OpenVariables
            //(Is this because you can't dynamically create an instance of LambdaFunction_Impl with
            //any open variables?)
            if (func instanceof LambdaCompiledExtended)
            {
                SharedPureFunction<?> pureFunction = ((LambdaCompiledExtended) func).pureFunction();
                if (pureFunction instanceof PureLambdaFunction)
                {
                    MutableMap<String, Object> __vars = ((PureLambdaFunction<?>) pureFunction).getOpenVariables();
                    if (__vars != null)
                    {
                        __vars.forEachKeyValue((key, value) -> map.put(key, bridge.buildList()._valuesAddAll(CompiledSupport.toPureCollection(value))));
                    }
                }
            }
            else if (func instanceof PureLambdaFunction)
            {
                map.putAll(((PureLambdaFunction) func).getOpenVariables());
            }
            // This can be helpful for debugging, but perhaps should actually
            // be checked inside 'evaluate' on a lambda instead?
            // else if (!((LambdaFunction) func)._openVariables().isEmpty()) {
            //     throw new PureExecutionException("Can't resolve open variables [" + String.join(",", ((LambdaFunction) func)._openVariables().toList().sortThis())
            //            + "] for lambda implementation: " + func.getClass().getName()
            //             + " (source: " + String.valueOf(func.getSourceInformation()) + ")");
            // }
        }
        return new PureMap(map);
    }

    public static Object handleValidation(boolean goDeep, Object input, SourceInformation si, ExecutionSupport es)
    {
        if (!(input instanceof Any))
        {
            return input;
        }
        Any returnObject = (Any) input;
        ElementOverride elementOverride = returnObject._elementOverride();
        if (elementOverride instanceof ConstraintsOverride)
        {
            ConstraintsOverride constraintsOverride = (ConstraintsOverride) elementOverride;
            if (constraintsOverride._constraintsManager() != null)
            {
                return CompiledSupport.executeFunction(constraintsOverride._constraintsManager(), new Class[]{Object.class}, new Object[]{input}, es);
            }
        }
        return CompiledSupport.validate(goDeep, returnObject, si, es);
    }

    public static Object newObject(Bridge bridge, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, String name, RichIterable<? extends KeyExpression> root_meta_pure_functions_lang_keyExpressions, ExecutionSupport es)
    {
        try
        {
            Class<?> c = ((CompiledExecutionSupport) es).getClassLoader().loadClass(JavaPackageAndImportBuilder.platformJavaPackage() + ".Root_" + Pure.elementToPath(aClass, "_") + "_Impl");
            Any result = (Any) c.getConstructor(String.class).newInstance(name);
            root_meta_pure_functions_lang_keyExpressions.forEach(new CheckedProcedure<KeyExpression>()
            {
                @Override
                public void safeValue(KeyExpression o) throws Exception
                {
                    Object res = reactivate(o._expression(), new PureMap(Maps.fixedSize.empty()), bridge, es);
                    Method m = c.getMethod("_" + o._key()._values().getFirst(), RichIterable.class);
                    if (res instanceof RichIterable)
                    {
                        m.invoke(result, res);
                    }
                    else
                    {
                        m.invoke(result, Lists.fixedSize.of(res));
                    }
                }
            });

            return result;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    public static Iterable<ListIterable<?>> collectIterable(LazyIterable iterable, ListIterable<String> columnTypes)
    {
        return iterable.collect((org.eclipse.collections.api.block.function.Function<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<?>, ListIterable<?>>) row ->
        {
            MutableList<Object> result = Lists.mutable.ofInitialCapacity(columnTypes.size());
            int i = 0;
            for (Object obj : row._values())
            {
                String s = obj.toString();
                String type = columnTypes.get(i);
                if (StringIterate.isEmpty(s))
                {
                    result.add(null);
                }
                else if ("Integer".equals(type))
                {
                    result.add(Long.valueOf(s));
                }
                else
                {
                    result.add(s);
                }
                i++;
            }
            return result;
        });
    }


    public static boolean instanceOf(Object obj, Type type, ExecutionSupport es)
    {
        if (type instanceof Enumeration)
        {
            return (obj instanceof Enum) && type.equals(((CompiledExecutionSupport) es).getMetadataAccessor().getEnumeration(((Enum) obj).getFullSystemPath()));
        }

        Class<?> javaClass;
        try
        {
            javaClass = pureTypeToJavaClass(type, es);
        }
        catch (Exception e)
        {
            if (obj instanceof CoreInstance)
            {
                return Instance.instanceOf((CoreInstance) obj, type, ((CompiledExecutionSupport) es).getProcessorSupport());
            }
            throw e;
        }
        if (javaClass == Any.class)
        {
            return true;
        }
        if (javaClass == Nil.class)
        {
            return false;
        }
        return javaClass.isInstance(obj);
    }

    public static boolean instanceOfEnumeration(Object obj, String enumerationSystemPath)
    {
        return (obj instanceof Enum) && enumerationSystemPath.equals(((Enum) obj).getFullSystemPath());
    }

    public static boolean matches(Object obj, Type type, int lowerBound, int upperBound, ExecutionSupport es)
    {
        if (obj == null)
        {
            return numberMatchesMultiplicity(0, lowerBound, upperBound);
        }
        if (type instanceof Enumeration)
        {
            return matchesEnumeration(obj, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getSystemPathForPackageableElement(type), lowerBound, upperBound);
        }
        return matches(obj, pureTypeToJavaClass(type, es), lowerBound, upperBound);
    }

    public static boolean matches(Object obj, Class<?> javaClass, int lowerBound, int upperBound)
    {
        if (obj == null)
        {
            return numberMatchesMultiplicity(0, lowerBound, upperBound);
        }
        if ((javaClass == Object.class) || (javaClass == Any.class))
        {
            return numberMatchesMultiplicity((obj instanceof Iterable) ? Iterate.sizeOf((Iterable<?>) obj) : 1, lowerBound, upperBound);
        }
        if (javaClass == Nil.class)
        {
            return (lowerBound == 0) && (obj instanceof Iterable) && Iterate.isEmpty((Iterable<?>) obj);
        }
        if (obj instanceof Iterable)
        {
            Iterable<?> collection = (Iterable<?>) obj;
            if (!numberMatchesMultiplicity(Iterate.sizeOf(collection), lowerBound, upperBound))
            {
                return false;
            }
            for (Object instance : collection)
            {
                if (!javaClass.isInstance(instance))
                {
                    return false;
                }
            }
            return true;
        }
        return numberMatchesMultiplicity(1, lowerBound, upperBound) && javaClass.isInstance(obj);
    }

    public static boolean matchesEnumeration(Object obj, String enumerationSystemPath, int lowerBound, int upperBound)
    {
        if (obj == null)
        {
            return numberMatchesMultiplicity(0, lowerBound, upperBound);
        }
        if (obj instanceof Iterable)
        {
            Iterable<?> collection = (Iterable<?>) obj;
            if (!numberMatchesMultiplicity(Iterate.sizeOf(collection), lowerBound, upperBound))
            {
                return false;
            }
            for (Object instance : collection)
            {
                if (!instanceOfEnumeration(instance, enumerationSystemPath))
                {
                    return false;
                }
            }
            return true;
        }
        return numberMatchesMultiplicity(1, lowerBound, upperBound) && instanceOfEnumeration(obj, enumerationSystemPath);
    }

    private static boolean numberMatchesMultiplicity(int number, int lowerBound, int upperBound)
    {
        return (lowerBound <= number) && ((upperBound < 0) || (number <= upperBound));
    }

    private static Class<?> pureTypeToJavaClassForExecution(ValueSpecification vs, Bridge bridge, ExecutionSupport es)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType gt = vs._genericType();
        Multiplicity m = vs._multiplicity();
        if (!Pure.hasToOneUpperBound(m))
        {
            return RichIterable.class;
        }
        if (gt._rawType() == null)
        {
            return Object.class;
        }
        return pureTypeToJavaClassForExecution(gt._rawType(), Pure.isToOne(m), es);
    }

    public static Class<?> pureTypeToJavaClass(Type _class, ExecutionSupport es)
    {
        return pureTypeToJavaClass(_class, false, es);
    }

    private static Class<?> pureTypeToJavaClassForExecution(Type _class, boolean useJavaPrimitives, ExecutionSupport es)
    {
        Class<?> clazz = pureTypeToJavaClass(_class, useJavaPrimitives, es);
        return clazz == Any.class || clazz == Nil.class ? Object.class : clazz;
    }

    private static Class<?> pureTypeToJavaClass(Type type, boolean useJavaPrimitives, ExecutionSupport es)
    {
        if (type instanceof Enumeration)
        {
            return Enum.class;
        }
        if (type instanceof PrimitiveType)
        {
            switch (type.getName())
            {
                case "Integer":
                {
                    return useJavaPrimitives ? long.class : Long.class;
                }
                case "Number":
                {
                    return Number.class;
                }
                case "Float":
                {
                    return useJavaPrimitives ? double.class : Double.class;
                }
                case "Decimal":
                {
                    return BigDecimal.class;
                }
                case "Boolean":
                {
                    return useJavaPrimitives ? boolean.class : Boolean.class;
                }
                case "String":
                {
                    return String.class;
                }
                case "Date":
                {
                    return PureDate.class;
                }
                case "StrictDate":
                {
                    return StrictDate.class;
                }
                case "LatestDate":
                {
                    return LatestDate.class;
                }
                case "DateTime":
                {
                    return DateTime.class;
                }
                default:
                {
                    CompiledExecutionSupport ces = (CompiledExecutionSupport) es;
                    return ces.getClassCache().getIfAbsentPutInterfaceForType(type);
                }
            }
        }

        CompiledExecutionSupport ces = (CompiledExecutionSupport) es;
        Class<?> theClass = ces.getClassCache().getIfAbsentPutInterfaceForType(type);
        return theClass.equals(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Map.class) ? PureMap.class : theClass;
    }

    public static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Bridge bridge, ExecutionSupport es)
    {
        return dynamicMatch(obj, funcs, null, false, bridge, es);
    }

    public static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, boolean isMatchWith, Bridge bridge, ExecutionSupport es)
    {
        for (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> f : funcs)
        {
            VariableExpression p = ((FunctionType) f._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().getFirst();
            Multiplicity mul = p._multiplicity();
            Long upper = mul._upperBound()._value();
            if (matches(obj, p._genericType()._rawType(), mul._lowerBound()._value().intValue(), upper == null ? -1 : upper.intValue(), es))
            {
                return isMatchWith ? evaluate(es, f, bridge, obj, var) : evaluate(es, f, bridge, obj);
            }
        }
        CompiledSupport.matchFailure(obj, null);
        return null;
    }

    public static <T> RichIterable<T> removeDuplicates(T item, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> eqlFn)
    {
        return (item == null) ? Lists.immutable.empty() : Lists.immutable.with(item);
    }

    public static <T> RichIterable<T> removeDuplicates(RichIterable<T> list, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> eqlFn, Bridge bridge, ExecutionSupport es)
    {
        if (list == null)
        {
            return Lists.immutable.empty();
        }

        if (eqlFn == null)
        {
            MutableSet<Object> set = new UnifiedSetWithHashingStrategy<>(PureEqualsHashingStrategy.HASHING_STRATEGY);
            return list.select((keyFn == null) ? set::add : i -> set.add(evaluate(es, keyFn, bridge, i)), Lists.mutable.empty());
        }

        if (keyFn == null)
        {
            MutableList<T> results = Lists.mutable.empty();
            return list.select(i -> results.noneSatisfy(e -> (Boolean) evaluate(es, eqlFn, bridge, e, i)), results);
        }

        MutableList<T> results = Lists.mutable.empty();
        MutableList<Object> keys = Lists.mutable.empty();
        list.forEach(item ->
        {
            Object key = evaluate(es, keyFn, bridge, item);
            if (keys.noneSatisfy(e -> (Boolean) evaluate(es, eqlFn, bridge, e, key)))
            {
                keys.add(key);
                results.add(item);
            }
        });
        return results;
    }

    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, Bridge bridge, ExecutionSupport es)
    {
        return reactivate(valueSpecification, lambdaOpenVariablesMap, true, bridge, es);
    }

    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, boolean allowJavaCompilation, Bridge bridge, ExecutionSupport es)
    {
        if (valueSpecification instanceof RoutedValueSpecification)
        {
            return reactivate(((RoutedValueSpecification) valueSpecification)._value(), lambdaOpenVariablesMap, allowJavaCompilation, bridge, es);
        }
        //Determine if the whole expression can be evaluated using the fast path
        //If any of the sub-expressions can't be evaluated then it is faster to use the slow path for the whole
        //expression, so that we only call the Java compiler once - otherwise it may be called several times as it
        //encounters each sub expression that cannot be evaluated

        if (Reactivator.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge))
        {
            try
            {
                return Reactivator.reactivateWithoutJavaCompilation(bridge, valueSpecification, lambdaOpenVariablesMap, es);
            }
            catch (PureDynamicReactivateException e)
            {
                throw new RuntimeException("Out of sync state between can reactivate and actual reactive without Java source code", e);
            }
        }
        else
        {
            return CompiledSupport.dynamicallyEvaluateValueSpecification(valueSpecification, lambdaOpenVariablesMap, es);
        }
    }

    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, Bridge bridge)
    {
        return canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(Maps.mutable.empty()), bridge);
    }

    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, PureMap lambdaOpenVariablesMap, Bridge bridge)
    {
        return Reactivator.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge);
    }

    public static org.finos.legend.pure.m3.coreinstance.Package buildPackageIfNonExistent(org.finos.legend.pure.m3.coreinstance.Package pack, ListIterable<String> path, SourceInformation si, Function<String, Package> packageBuilder)
    {
        if (path.size() >= 1)
        {
            org.finos.legend.pure.m3.coreinstance.Package child = (org.finos.legend.pure.m3.coreinstance.Package) pack._children().detect(c -> c._name().equals(path.get(0)));
            if (child == null)
            {
                child = packageBuilder.apply(path.get(0))._name(path.get(0))._package(pack);
                pack._childrenAdd(child);
            }
            return buildPackageIfNonExistent(child, ListHelper.subList(path, 1, path.size()), si, packageBuilder);
        }
        return pack;
    }

    public static Class<?> fromJsonResolveType(JSONObject jsonObject, String pureType, Class<?> typeFromClassMetaData, MetadataAccessor md, String typeKey, ClassLoader classLoader)
    {
        String targetTypeName = (String) jsonObject.get(typeKey);
        if (targetTypeName != null)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cls = md.getClass(pureType);
            Deque<Generalization> deque = new ArrayDeque<>(cls._specializations().toSet());
            MutableSet<Generalization> set = Sets.mutable.ofAll(cls._specializations());
            while (!deque.isEmpty())
            {
                Type type = deque.poll()._specific();
                if (targetTypeName.equals(type._name()))
                {
                    try
                    {
                        return classLoader.loadClass(TypeProcessor.fullyQualifiedJavaInterfaceNameForType(type));
                    }
                    catch (ClassNotFoundException e)
                    {
                        // Type specified is incorrect or problem with metadata. Return default.
                        return typeFromClassMetaData;
                    }
                }
                for (Generalization g : type._specializations())
                {
                    if (!set.contains(g))
                    {
                        set.add(g);
                        deque.addLast(g);
                    }
                }
            }
        }
        return typeFromClassMetaData;
    }
}