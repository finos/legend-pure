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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.procedure.checked.CheckedProcedure;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.CompilationFailure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.CompilationResult;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
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
import org.finos.legend.pure.runtime.java.compiled.delta.CodeBlockDeltaCompiler;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.JavaMethodWithParamsSharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.shared.hash.HashType;
import org.finos.legend.pure.runtime.java.shared.hash.HashingUtil;
import org.json.simple.JSONObject;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class Pure
{
    private static final ExecutorService traceAsyncExecutor = Executors.newCachedThreadPool(new ThreadFactory()
    {
        private final ThreadGroup group = System.getSecurityManager() == null
            ? Thread.currentThread().getThreadGroup()
            : System.getSecurityManager().getThreadGroup();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r)
        {
            Thread thread = new Thread(this.group, r, "trace-async-executor-thread-" + this.threadNumber.getAndIncrement(), 0);
            if (!thread.isDaemon())
            {
                thread.setDaemon(true);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY)
            {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    });

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

    public static Object alloyTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular, Bridge bridge)
    {
        String host = System.getProperty("alloy.test.server.host");
        long port = System.getProperty("alloy.test.server.port") == null ? -1 : Long.parseLong(System.getProperty("alloy.test.server.port"));
        if (host != null && port == -1)
        {
            throw new PureExecutionException("The system variable 'alloy.test.server.host' is set to '" + host + "' however 'alloy.test.server.port' has not been set!");
        }
        String clientVersion = System.getProperty("alloy.test.clientVersion");
        String serverVersion = System.getProperty("alloy.test.serverVersion");
        return host != null ? evaluate(es, alloyTest, bridge, clientVersion, serverVersion, host, port) : evaluate(es, regular, bridge);
    }

    public static Object legendTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular, Bridge bridge)
    {
        String host = System.getProperty("legend.test.server.host");
        long port = System.getProperty("legend.test.server.port") == null ? -1 : Long.parseLong(System.getProperty("legend.test.server.port"));
        String clientVersion = System.getProperty("legend.test.clientVersion");
        String serverVersion = System.getProperty("legend.test.serverVersion");
        String serializationKind = System.getProperty("legend.test.serializationKind");
        if (host != null)
        {
            if (port == -1)
            {
                throw new PureExecutionException("The system variable 'legend.test.server.host' is set to '" + host + "' however 'legend.test.server.port' has not been set!");
            }
            if (serializationKind == null || !(serializationKind.equals("text") || serializationKind.equals("json")))
            {
                serializationKind = "json";
            }
            if (clientVersion == null)
            {
                throw new PureExecutionException("The system variable 'legend.test.clientVersion' should be set");
            }
            if (serverVersion == null)
            {
                throw new PureExecutionException("The system variable 'legend.test.serverVersion' should be set");
            }
        }
        return host != null ? evaluate(es, alloyTest, bridge, clientVersion, serverVersion, serializationKind, host, port) : evaluate(es, regular, bridge);
    }

    public static <E> E getEnumByName(Enumeration<E> enumeration, final String name)
    {
        return enumeration._values().detect(new Predicate<E>()
        {
            @Override
            public boolean accept(E e)
            {
                return name.equals(((Enum) e)._name());
            }
        });
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, final MetadataAccessor ma, Function0<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> genericTypeBuilder, final ProcessorSupport processorSupport)
    {
        if (val == null)
        {
            return genericTypeBuilder.value()._rawType(ma.getBottomType());
        }
        if (val instanceof Any)
        {
            Any a = (Any) val;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType = a._classifierGenericType();
            return (genericType == null) ? genericTypeBuilder.value()._rawType(CompiledSupport.getType(a, ma)) : genericType;
        }
        if ((val instanceof Long) || (val instanceof BigInteger))
        {
            Type type = ma.getPrimitiveType("Integer");
            return genericTypeBuilder.value()._rawType(type);
        }
        if (val instanceof String)
        {
            Type type = ma.getPrimitiveType("String");
            return genericTypeBuilder.value()._rawType(type);
        }
        if (val instanceof Boolean)
        {
            Type type = ma.getPrimitiveType("Boolean");
            return genericTypeBuilder.value()._rawType(type);
        }
        if (val instanceof PureDate)
        {
            Type type = ma.getPrimitiveType(DateFunctions.datePrimitiveType((PureDate) val));
            return genericTypeBuilder.value()._rawType(type);
        }
        if (val instanceof Double)
        {
            Type type = ma.getPrimitiveType("Float");
            return genericTypeBuilder.value()._rawType(type);
        }
        if (val instanceof BigDecimal)
        {
            Type type = ma.getPrimitiveType("Decimal");
            return genericTypeBuilder.value()._rawType(type);
        }
        if (val instanceof RichIterable)
        {
            RichIterable<?> l = (RichIterable<?>) val;
            if (l.isEmpty())
            {
                return genericTypeBuilder.value()._rawType(ma.getBottomType());
            }
            if (l.size() == 1)
            {
                return safeGetGenericType(l.getFirst(), ma, genericTypeBuilder, processorSupport);
            }
            else
            {
                RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> r = l.collect(new Function<Object, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType>()
                {
                    @Override
                    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType valueOf(Object o)
                    {
                        return safeGetGenericType(o, ma, genericTypeBuilder, processorSupport);
                    }
                });
                MutableSet<CoreInstance> s = Sets.mutable.empty();
                Type t = r.getFirst()._rawType();
                for (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType a : r)
                {
                    if (a._rawType() == null)
                    {
                        throw new PureExecutionException("TODO: Find most common type for non-concrete generic type");
                    }
                    if (t != a._rawType())
                    {
                        s.add(a);
                    }
                }
                if (s.isEmpty())
                {
                    return r.getFirst();
                }
                else
                {
                    s.add(r.getFirst());
                    return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) GenericType.findBestCommonGenericType(s.toList(), true, false, processorSupport);
                }
            }
        }
        else
        {
            throw new PureExecutionException("ERROR unhandled type for value: " + val + " (instance of " + val.getClass() + ")");
        }
    }

    public static RichIterable<Type> getGeneralizations(Type type, ExecutionSupport es)
    {
        MutableList<Type> generalizations = FastList.newList();
        for (CoreInstance superType : org.finos.legend.pure.m3.navigation.type.Type.getGeneralizationResolutionOrder(type, ((CompiledExecutionSupport) es).getProcessorSupport()))
        {
            generalizations.add((Type) superType);
        }
        return generalizations;
    }

    public static Object rawEvalProperty(Property property, Object value, SourceInformation sourceInformation)
    {
        try
        {
            return value.getClass().getField("_" + property._name()).get(value);
        }
        catch (NoSuchFieldException e)
        {
            throw new PureExecutionException(sourceInformation, "Can't find the property '" + property._name() + "' in the class " + CompiledSupport.getPureClassName(value));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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

    public static SharedPureFunction<?> findSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        if (func instanceof Property)
        {
            Type srcType = func._classifierGenericType()._typeArguments().getFirst()._rawType();
            return ((CompiledExecutionSupport) es).getFunctionCache().getIfAbsentPutFunctionForClassProperty(srcType, func, ((CompiledExecutionSupport) es).getClassLoader());
        }
        if (func instanceof Path)
        {
            return new PureFunction1<Object, Object>()
            {

                @Override
                public Object execute(ListIterable vars, ExecutionSupport es)
                {
                    return value(vars.getFirst(), es);
                }

                @Override
                public Object value(Object o, ExecutionSupport es)
                {
                    RichIterable<?> result = ((Path<?, ?>) func)._path().injectInto(CompiledSupport.toPureCollection(o), (mutableList, path) ->
                    {
                        if (!(path instanceof PropertyPathElement))
                        {
                            throw new PureExecutionException("Only PropertyPathElement is supported yet!");
                        }
                        return mutableList.flatCollect(instance ->
                        {
                            MutableList<Object> parameters = ((PropertyPathElement) path)._parameters().collect(o1 -> o1 instanceof InstanceValue ? ((InstanceValue) o1)._values() : null, Lists.mutable.with(instance));
                            return CompiledSupport.toPureCollection(evaluate(es, ((PropertyPathElement) path)._property(), bridge, parameters.toArray()));
                        });
                    });
                    Multiplicity mult = func._classifierGenericType()._multiplicityArguments().getFirst();
                    return bridge.hasToOneUpperBound().apply(mult, es) ? result.getFirst() : result;
                }
            };
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
                return DynamicPureLambdaFunctionImpl.createPureLambdaFunction(lambda, openVariablesMap.getMap(), bridge);
            }
            return ((LambdaCompiledExtended) CompiledSupport.dynamicallyBuildLambdaFunction(func, es)).pureFunction();
        }
        if (func instanceof ConcreteFunctionDefinition)
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
                            return new JavaMethodWithParamsSharedPureFunction(method, paramClasses, func.getSourceInformation());
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

    public static Object evaluate(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, Object... instances)
    {
        if (func instanceof Property)
        {
            return getSharedPureFunction(func, bridge, es).execute(FastList.newListWith(instances[0]), es);
        }
        if (func instanceof Path)
        {
            return getSharedPureFunction(func, bridge, es).execute(FastList.newListWith(instances[0]), es);
        }
        if (func instanceof LambdaCompiledExtended)
        {
            return getSharedPureFunction(func, bridge, es).execute(Lists.fixedSize.with(instances), es);
        }
        if (func instanceof ConcreteFunctionDefinition)
        {
            JavaMethodWithParamsSharedPureFunction p = (JavaMethodWithParamsSharedPureFunction) getSharedPureFunction(func, bridge, es);
            Class<?>[] paramClasses = p.getParametersTypes();
            int l = paramClasses.length;
            MutableList<Object> paramInstances = FastList.newList();
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
            paramInstances.add(es);
            return p.execute(paramInstances, es);
        }
        if (func instanceof LambdaFunction)
        {
            return getSharedPureFunction(func, bridge, es).execute(Lists.fixedSize.with(instances), es);
        }
        SharedPureFunction<?> reflectiveNative = getSharedPureFunction(func, bridge, es);
        return reflectiveNative.execute(instances == null ? Lists.mutable.empty() : FastList.newListWith(instances), es);
    }

    public static Object evaluateToMany(ExecutionSupport es, Bridge bridge, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function func, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List> instances)
    {
        MutableList<Object> inputs = Lists.mutable.of();
        if (instances != null)
        {
            for (Object obj : instances)
            {
                inputs.add(((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List) obj)._values());
            }
        }
        return _evaluateToMany(es, bridge, func, inputs);
    }

    public static Object _evaluateToMany(ExecutionSupport es, Bridge bridge, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, ListIterable<?> paramInputs)
    {
        if (func instanceof LambdaCompiledExtended)
        {
            return ((LambdaCompiledExtended) func).pureFunction().execute(paramInputs, es);
        }
        if (func instanceof Property)
        {
            try
            {
                Object o = ((RichIterable<?>) paramInputs.getFirst()).getFirst();
                return o.getClass().getMethod("_" + func.getName()).invoke(o);
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
            if (func instanceof ConcreteFunctionDefinition)
            {
                return CompiledSupport.executeFunction(func, paramClasses, paramInstances, es);
            }
            if (func instanceof NativeFunction || func instanceof LambdaFunction)
            {
                SharedPureFunction<?> foundFunc = getNativeOrLambdaFunction(es, func);
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
                    throw new PureExecutionException(builder.toString());
                }
                return foundFunc.execute(Lists.mutable.with(paramInstances), es);
            }
            throw new PureExecutionException("Unknown function type:" + func.getClass().getName());
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

    public static Object get(RichIterable<?> list, String id)
    {
        return list.detect(e -> id.equals(((CoreInstance) e).getName()));
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

    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(Object l1, Object l2, Function0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> pairBuilder)
    {
        return zip((RichIterable<? extends U>) l1, (RichIterable<? extends V>) l2, pairBuilder);
    }

    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2, Function0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> pairBuilder)
    {
        return l1 == null || l2 == null ? FastList.<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>>newList() : l1.zip(l2).collect(new Function<Pair<? extends U, ? extends V>, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>>()
        {
            @Override
            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> valueOf(Pair<? extends U, ? extends V> pair)
            {
                return pairBuilder.value()._first(pair.getOne())._second(pair.getTwo());
            }
        });
    }

    public static Tag tag(Profile profile, final String tag)
    {
        return profile._p_tags().detect(new Predicate<Tag>()
        {
            @Override
            public boolean accept(Tag o)
            {
                return tag.equals(o._value());
            }
        });
    }

    public static Stereotype stereotype(Profile profile, final String stereotype)
    {
        return profile._p_stereotypes().detect(new Predicate<Stereotype>()
        {
            @Override
            public boolean accept(Stereotype o)
            {
                return stereotype.equals(o._value());
            }
        });
    }

    public static PureMap getOpenVariables(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge)
    {
        //In the case of LambdaFunction_Impl, we do not need to concern with OpenVariables
        if (func instanceof LambdaCompiledExtended)
        {
            SharedPureFunction pureFunction = ((LambdaCompiledExtended) func).pureFunction();
            MutableMap<String, Object> map = UnifiedMap.newMap();
            if (pureFunction instanceof PureLambdaFunction)
            {
                MutableMap<String, Object> __vars = ((PureLambdaFunction<?>) pureFunction).getOpenVariables();
                if (__vars != null)
                {
                    for (String key : __vars.keysView())
                    {
                        map.put(key, bridge.listBuilder().value()._valuesAddAll(CompiledSupport.toPureCollection(__vars.get(key))));
                    }
                }
            }
            return new PureMap(map);
        }
        else
        {
            return new PureMap(org.eclipse.collections.impl.factory.Maps.mutable.empty());
        }
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

    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType, RichIterable<? extends KeyValue> root_meta_pure_functions_lang_keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
    {
        return newObject((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);
    }

    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class aClass, RichIterable<? extends KeyValue> root_meta_pure_functions_lang_keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
    {
        return CompiledSupport.newObject(aClass, root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);
    }

    public static Object newObject(Bridge bridge, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class aClass, String name, RichIterable<? extends KeyExpression> root_meta_pure_functions_lang_keyExpressions, final ExecutionSupport es)
    {
        try
        {
            final Class<?> c = ((CompiledExecutionSupport) es).getClassLoader().loadClass(JavaPackageAndImportBuilder.platformJavaPackage() + ".Root_" + bridge.elementToPath().value(aClass, "_", es) + "_Impl");
            final Any result = (Any) c.getConstructor(String.class).newInstance(name);
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
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    public static Iterable<ListIterable<?>> collectIterable(LazyIterable iterable, final ListIterable<String> columnTypes)
    {
        return iterable.collect(new Function<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<?>, ListIterable<?>>()
        {
            @Override
            public ListIterable<?> valueOf(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<?> row)
            {
                MutableList<Object> result = FastList.newList();
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
            }
        });
    }

    public static TreeNode replaceTreeNode(TreeNode instance, TreeNode targetNode, TreeNode subTree)
    {
        if (instance == targetNode)
        {
            return subTree;
        }
        TreeNode result = instance.copy();
        replaceTreeNodeCopy(instance, result, targetNode, subTree);
        return result;
    }

    public static void replaceTreeNodeCopy(TreeNode instance, TreeNode result, TreeNode targetNode, TreeNode subTree)
    {
        result._childrenData(FastList.newList());

        for (TreeNode child : instance._childrenData())
        {
            if (child == targetNode)
            {
                result._childrenDataAdd(subTree);
            }
            else
            {
                TreeNode newCopy = child.copy();
                replaceTreeNodeCopy(child, newCopy, targetNode, subTree);
                result._childrenDataAdd(newCopy);
            }
        }
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
            if (obj instanceof CoreInstance) {
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
        boolean isToOneOrZeroToOne = bridge.hasToOneUpperBound().apply(m, es);

        if (!isToOneOrZeroToOne)
        {
            return RichIterable.class;
        }
        else if (gt._rawType() == null)
        {
            return Object.class;
        }
        else
        {
            return pureTypeToJavaClassForExecution(gt._rawType(), bridge.isToOne().apply(m, es), es);
        }
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

    public static boolean subTypeOf(Type subType, Type superType, ExecutionSupport es)
    {
        if (subType.equals(superType))
        {
            return true;
        }

        // NOTE: ClassNotFoundException can occur when we use subTypeOf() in engine where some
        // Java classes are not available during plan generation. There is a potentially
        // less performant alternative which is to use type_subTypeOf() as this will use the
        // metamodel graph instead of Java classes to test subtype; but this alternative is more reliable.
        // As such, to be defensive, we should fallback to the latter when the former fails with ClassNotFoundException
        // See https://github.com/finos/legend-pure/issues/324
        Class<?> theSubTypeClass;
        try
        {
            theSubTypeClass = pureTypeToJavaClass(subType, es);
        }
        catch (Exception e)
        {
            return ((CompiledExecutionSupport) es).getProcessorSupport().type_subTypeOf(subType, superType);
        }
        if (theSubTypeClass == Nil.class)
        {
            return true;
        }

        Class<?> theSuperTypeClass;
        try
        {
            theSuperTypeClass = pureTypeToJavaClass(superType, es);
        }
        catch (Exception e)
        {
            return ((CompiledExecutionSupport) es).getProcessorSupport().type_subTypeOf(subType, superType);
        }
        return (theSuperTypeClass == Any.class) || theSuperTypeClass.isAssignableFrom(theSubTypeClass);
    }

    public static CompilationResult compileCodeBlock(String source, Function0<CompilationResult> resultBuilder, Function0<CompilationFailure> failureBuilder, Function0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.SourceInformation> srcInfoBuilder, ExecutionSupport es)
    {
        CompilationResult result = null;
        if (source != null)
        {
            CodeBlockDeltaCompiler.CompilationResult compilationResult = CodeBlockDeltaCompiler.compileCodeBlock(source, ((CompiledExecutionSupport) es));
            result = convertCompilationResult(compilationResult, resultBuilder, failureBuilder, srcInfoBuilder);
        }
        return result;
    }

    public static RichIterable<CompilationResult> compileCodeBlocks(RichIterable<? extends String> sources, Function0<CompilationResult> resultBuilder, Function0<CompilationFailure> failureBuilder, Function0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.SourceInformation> srcInfoBuilder, ExecutionSupport es)
    {
        RichIterable<CodeBlockDeltaCompiler.CompilationResult> compilationResults = CodeBlockDeltaCompiler.compileCodeBlocks(sources, ((CompiledExecutionSupport) es));
        MutableList<CompilationResult> results = FastList.newList(sources.size());

        for (CodeBlockDeltaCompiler.CompilationResult compilationResult : compilationResults)
        {
            results.add(convertCompilationResult(compilationResult, resultBuilder, failureBuilder, srcInfoBuilder));
        }
        return results;
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.CompilationResult convertCompilationResult(CodeBlockDeltaCompiler.CompilationResult compilationResult, Function0<CompilationResult> resultBuilder, Function0<CompilationFailure> failureBuilder, Function0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.SourceInformation> srcInfoBuilder)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.CompilationResult result = resultBuilder.value();

        if (compilationResult.getFailureMessage() != null)
        {
            CompilationFailure failure = failureBuilder.value();
            failure._message(compilationResult.getFailureMessage());

            SourceInformation si = compilationResult.getFailureSourceInformation();

            if (si != null)
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.functions.meta.SourceInformation sourceInformation = srcInfoBuilder.value();
                sourceInformation._column(si.getColumn());
                sourceInformation._line(si.getLine());
                sourceInformation._endColumn(si.getEndColumn());
                sourceInformation._endLine(si.getEndLine());
                sourceInformation._startColumn(si.getStartColumn());
                sourceInformation._startLine(si.getStartLine());
                failure._sourceInformation(sourceInformation);
            }
            result._failure(failure);
        }
        else
        {
            ConcreteFunctionDefinition<?> cfd = (ConcreteFunctionDefinition<?>) compilationResult.getResult();
            result._result(cfd._expressionSequence().getFirst());
        }
        return result;
    }

    public static Object dynamicMatchWith(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, Bridge bridge, ExecutionSupport es)
    {
        return dynamicMatch(obj, funcs, var, true, bridge, es);
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

    public static <T, V> RichIterable<T> removeDuplicates(RichIterable<T> list, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> eqlFn, Bridge bridge, ExecutionSupport es)
    {
        if (list == null)
        {
            return Lists.immutable.empty();
        }

        MutableList<T> results = Lists.mutable.empty();
        if ((keyFn == null) && (eqlFn == null))
        {
            MutableSet<T> instances = new UnifiedSetWithHashingStrategy<T>(PureEqualsHashingStrategy.HASHING_STRATEGY);
            for (T item : list)
            {
                if (instances.add(item))
                {
                    results.add(item);
                }
            }
        }
        else if (keyFn == null)
        {
            for (T item : list)
            {
                if (!removeDuplicatesContains(results, item, eqlFn, bridge, es))
                {
                    results.add(item);
                }
            }
        }
        else if (eqlFn == null)
        {
            MutableSet<V> keys = new UnifiedSetWithHashingStrategy<V>(PureEqualsHashingStrategy.HASHING_STRATEGY);
            for (T item : list)
            {
                if (keys.add((V) evaluate(es, keyFn, bridge, item)))
                {
                    results.add(item);
                }
            }
        }
        else
        {
            MutableList<V> keys = Lists.mutable.empty();
            for (T item : list)
            {
                V key = (V) evaluate(es, keyFn, bridge, item);
                if (!removeDuplicatesContains(keys, key, eqlFn, bridge, es))
                {
                    keys.add(key);
                    results.add(item);
                }
            }
        }
        return results;
    }

    private static <T> boolean removeDuplicatesContains(RichIterable<T> list, T item, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function eqlFn, Bridge bridge, ExecutionSupport es)
    {
        for (T element : list)
        {
            if ((Boolean) evaluate(es, eqlFn, bridge, element, item))
            {
                return true;
            }
        }
        return false;
    }

    public static PureMap newMap(RichIterable pairs, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(PureEqualsHashingStrategy.HASHING_STRATEGY);
        for (Object po : pairs)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p = (org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair) po;
            map.put(p._first(), p._second());
        }
        return new PureMap(map);
    }

    public static PureMap newMap(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(PureEqualsHashingStrategy.HASHING_STRATEGY);
        if (p != null)
        {
            map.put(p._first(), p._second());
        }
        return new PureMap(map);
    }

    public static PureMap newMap(RichIterable pairs, Property property, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(new PropertyHashingStrategy(property, es));
        for (Object po : pairs)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p = (org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair) po;
            map.put(p._first(), p._second());
        }
        return new PureMap(map);
    }

    public static PureMap newMap(org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair pair, Property property, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(new PropertyHashingStrategy(property, es));
        if (pair != null)
        {
            map.put(pair._first(), pair._second());
        }
        return new PureMap(map);
    }

    public static PureMap newMap(RichIterable pairs, RichIterable properties, Bridge bridge, ExecutionSupport es)
    {
        MutableMap<Object, Object> map = UnifiedMapWithHashingStrategy.newMap(new PropertyHashingStrategy(properties, bridge, es));
        for (Object po : pairs)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p = (org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair) po;
            map.put(p._first(), p._second());
        }
        return new PureMap(map);
    }

    public static PureMap putAllPairs(PureMap pureMap, RichIterable pairs)
    {
        Map map = pureMap.getMap();
        MutableMap newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy(((UnifiedMapWithHashingStrategy) map).hashingStrategy(), map) : new UnifiedMap(map);
        for (Object po : pairs)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p = (org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair) po;
            newOne.put(p._first(), p._second());
        }
        return new PureMap(newOne);
    }

    public static PureMap putAllPairs(PureMap pureMap, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair pair)
    {
        Map map = pureMap.getMap();
        MutableMap newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy(((UnifiedMapWithHashingStrategy) map).hashingStrategy(), map) : new UnifiedMap(map);
        newOne.put(pair._first(), pair._second());
        return new PureMap(newOne);
    }

    public static PureMap putAllMaps(PureMap pureMap, PureMap other)
    {
        Map map = pureMap.getMap();
        MutableMap newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy(((UnifiedMapWithHashingStrategy) map).hashingStrategy(), map) : new UnifiedMap(map);
        newOne.putAll(other.getMap());
        return new PureMap(newOne);
    }

    public static PureMap replaceAll(PureMap pureMap, RichIterable pairs)
    {
        Map map = pureMap.getMap();
        MutableMap newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy(((UnifiedMapWithHashingStrategy) map).hashingStrategy()) : new UnifiedMap();
        for (Object po : pairs)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair p = (org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair) po;
            newOne.put(p._first(), p._second());
        }
        return new PureMap(newOne);
    }

    public static PureMap replaceAll(PureMap pureMap, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair pair)
    {
        Map map = pureMap.getMap();
        MutableMap newOne = map instanceof UnifiedMapWithHashingStrategy ? new UnifiedMapWithHashingStrategy(((UnifiedMapWithHashingStrategy) map).hashingStrategy()) : new UnifiedMap();
        newOne.put(pair._first(), pair._second());
        return new PureMap(newOne);
    }

    private static class PropertyHashingStrategy implements HashingStrategy
    {
        RichIterable<Property> properties;
        ExecutionSupport es;
        Bridge bridge;

        PropertyHashingStrategy(RichIterable<Property> properties, Bridge bridge, ExecutionSupport es)
        {
            this.properties = properties;
            this.bridge = bridge;
            this.es = es;
        }

        PropertyHashingStrategy(Property property, ExecutionSupport es)
        {
            this.properties = FastList.newListWith(property);
            this.es = es;
        }

        @Override
        public int computeHashCode(Object o)
        {
            int hashCode = 0;
            for (Property value : this.properties)
            {
                hashCode = (31 * hashCode) + CompiledSupport.safeHashCode(evaluate(this.es, value, bridge, o));
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o, Object e1)
        {
            for (Property value : this.properties)
            {
                if (!evaluate(this.es, value, bridge, o).equals(evaluate(this.es, value, bridge, e1)))
                {
                    return false;
                }
            }
            return true;
        }
    }

    public static Object reactivate(final ValueSpecification valueSpecification,
                                    final PureMap lambdaOpenVariablesMap,
                                    Bridge bridge,
                                    final ExecutionSupport es)
    {
        return reactivate(valueSpecification, lambdaOpenVariablesMap, true, bridge, es);
    }

    public static Object reactivate(final ValueSpecification valueSpecification,
                                    final PureMap lambdaOpenVariablesMap,
                                    final boolean allowJavaCompilation,
                                    Bridge bridge,
                                    final ExecutionSupport es)
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

    public static boolean canReactivateWithoutJavaCompilation(
        final ValueSpecification valueSpecification,
        final ExecutionSupport es,
        Bridge bridge
    )
    {
        return canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(UnifiedMap.newMap()), bridge);
    }

    public static boolean canReactivateWithoutJavaCompilation(
        final ValueSpecification valueSpecification,
        final ExecutionSupport es,
        final PureMap lambdaOpenVariablesMap,
        Bridge bridge
    )
    {
        return Reactivator.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge);
    }

    public static org.finos.legend.pure.m3.coreinstance.Package buildPackageIfNonExistent(final org.finos.legend.pure.m3.coreinstance.Package pack, final ListIterable<String> path, SourceInformation si, Function<String, org.finos.legend.pure.m3.coreinstance.Package> packageBuilder)
    {
        if (path.size() >= 1)
        {
            org.finos.legend.pure.m3.coreinstance.Package child = (org.finos.legend.pure.m3.coreinstance.Package) pack._children().detect(new Predicate<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>()
            {
                @Override
                public boolean accept(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement c)
                {
                    return c._name().equals(path.get(0));
                }
            });
            if (child == null)
            {
                child = packageBuilder.apply(path.get(0))._name(path.get(0))._package(pack);
                pack._childrenAdd(child);
            }
            return buildPackageIfNonExistent(child, ListHelper.subList(path, 1, path.size()), si, packageBuilder);
        }
        return pack;
    }

    public static Class fromJsonResolveType(JSONObject jsonObject, String pureType, Class typeFromClassMetaData, MetadataAccessor md, String typeKey, ClassLoader classLoader)
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

    public static String hash(String text, Object hashTypeObject)
    {
        Enum hashTypeEnum = (Enum) hashTypeObject;
        HashType hashType = HashType.valueOf(hashTypeEnum._name());

        return HashingUtil.hash(text, hashType);
    }

    public static Object traceSpan(final ExecutionSupport es,
                                   final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function function,
                                   final String operationName,
                                   final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function funcToGetTags,
                                   final boolean tagsCritical,
                                   Bridge bridge)
    {
        if (!GlobalTracer.isRegistered())
        {
            return evaluate(es, function, bridge, Lists.mutable.empty());
        }

        Span span = GlobalTracer.get().buildSpan(operationName).start();
        try (Scope scope = GlobalTracer.get().scopeManager().activate(span))
        {
            if (funcToGetTags != null)
            {
                try
                {
                    Future<?> future = traceAsyncExecutor.submit(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try (Scope scope = GlobalTracer.get().scopeManager().activate(span))
                            {
                                MutableMap<?, ?> tags = ((PureMap) evaluate(es, funcToGetTags, bridge, Lists.mutable.empty())).getMap();
                                for (Entry entry : tags.entrySet())
                                {
                                    String tag = (String) entry.getKey();
                                    String value = (String) entry.getValue();
                                    if (span != null)
                                    {
                                        span.setTag(tag, value);
                                    }
                                }
                            }
                        }
                    });
                    future.get(60, TimeUnit.SECONDS);
                }
                catch (TimeoutException e)
                {
                    if (span != null)
                    {
                        span.setTag("Exception", String.format("Timeout received before tags could be resolved"));
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (Exception e)
                {
                    if (tagsCritical)
                    {
                        throw new RuntimeException(e);
                    }
                    if (span != null)
                    {
                        span.setTag("Exception", String.format("Unable to resolve tags - [%s]", e.getMessage()));
                    }
                }
            }
            return evaluate(es, function, bridge, Lists.mutable.empty());
        }
        finally
        {
            if (span != null)
            {
                span.finish();
            }
        }
    }
}