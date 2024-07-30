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
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.procedure.checked.CheckedProcedure;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.DefaultValue;
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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureDynamicReactivateException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.QuantityCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.metadata.JavaMethodWithParamsSharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.json.simple.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Pure
{
    public static ListIterable<PackageableElement> elementPath(PackageableElement element)
    {
        return elementPath(element, 1);
    }

    private static MutableList<PackageableElement> elementPath(PackageableElement element, int size)
    {
        Package pkg = element._package();
        return ((pkg == null) ? Lists.mutable.<PackageableElement>ofInitialCapacity(size) : elementPath(pkg, size + 1)).with(element);
    }

    public static String elementToPath(PackageableElement element, String separator)
    {
        return elementToPath(element, separator, false);
    }

    public static String elementToPath(PackageableElement element, String separator, boolean includeRoot)
    {
        String name = element._name();
        Package pkg = element._package();
        if (pkg == null)
        {
            return ((name != null) && (includeRoot || !M3Paths.Root.equals(name))) ? name : "";
        }

        StringBuilder builder = new StringBuilder(64);
        writeParentPackagePath(builder, pkg, separator, includeRoot);
        if (name != null)
        {
            builder.append(name);
        }
        return builder.toString();
    }

    private static void writeParentPackagePath(StringBuilder builder, Package pkg, String separator, boolean includeRoot)
    {
        String name = pkg._name();
        Package parent = pkg._package();
        if (parent != null)
        {
            writeParentPackagePath(builder, parent, separator, includeRoot);
            if (name != null)
            {
                builder.append(name);
            }
            builder.append(separator);
        }
        else if (includeRoot && (name != null))
        {
            builder.append(name).append(separator);
        }
    }

    public static PackageableElement lenientPathToElement(String path, String separator, SourceInformation sourceInfo, ExecutionSupport executionSupport)
    {
        return pathToElement(path, separator, true, sourceInfo, executionSupport);
    }

    public static PackageableElement pathToElement(String path, String separator, SourceInformation sourceInfo, ExecutionSupport executionSupport)
    {
        return pathToElement(path, separator, false, sourceInfo, executionSupport);
    }

    private static PackageableElement pathToElement(String path, String separator, boolean allowNotFound, SourceInformation sourceInfo, ExecutionSupport executionSupport)
    {
        CompiledExecutionSupport compiledExecSupport = (CompiledExecutionSupport) executionSupport;
        Package root = compiledExecSupport.getMetadataAccessor().getPackage(M3Paths.Root);
        if (root == null)
        {
            throw new PureExecutionException(sourceInfo, "Cannot find " + M3Paths.Root, Stacks.mutable.empty());
        }
        if (path.isEmpty() || path.equals(separator) || M3Paths.Root.equals(path))
        {
            return root;
        }

        PackageableElement result = pathToElementFromRoot(root, path, separator);
        if (result == null)
        {
            result = (PackageableElement) compiledExecSupport.getProcessorSupport().package_getByUserPath("::".equals(separator) ? path : path.replace(separator, "::"));
            if ((result == null) && !allowNotFound)
            {
                throw new PureExecutionException(sourceInfo, pathToElementNotFoundErrorMessage(path, separator, compiledExecSupport), Stacks.mutable.empty());
            }
        }
        return result;
    }

    private static PackageableElement pathToElementFromRoot(Package root, String path, String separator)
    {
        Package pkg = root;
        ListIterable<String> pathElements = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.splitUserPath(path, separator);
        for (String name : pathElements.subList(0, pathElements.size() - 1))
        {
            PackageableElement element = pkg._children().detect(c -> name.equals(c._name()));
            if (!(element instanceof Package))
            {
                return null;
            }
            pkg = (Package) element;
        }
        String name = pathElements.getLast();
        return pkg._children().detect(c -> name.equals(c._name()));
    }

    @SuppressWarnings("unchecked")
    private static String pathToElementNotFoundErrorMessage(String path, String separator, CompiledExecutionSupport executionSupport)
    {
        if (M3Paths.Root.equals(path) || separator.equals(path))
        {
            return "Could not find " + path;
        }

        ListIterable<String> pathElements = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.splitUserPath(path, separator);
        if (pathElements.size() == 1)
        {
            return "'" + path + "' is not a valid PackageableElement";
        }

        MetadataAccessor metadataAccessor = executionSupport.getMetadataAccessor();
        StringBuilder builder = new StringBuilder(path.length()).append('\'').append(path).append("' is not a valid PackageableElement");
        for (int i = pathElements.size() - 1; i > 0; i--)
        {
            ListIterable<String> subList = pathElements.subList(0, i);
            String packageString = subList.injectInto(new StringBuilder(M3Paths.Root), (b, e) -> b.append("::").append(e)).toString();
            try
            {
                if (metadataAccessor.getPackage(packageString) != null)
                {
                    builder.append(": could not find '").append(pathElements.get(i)).append("' in ");
                    subList.appendString(builder, separator);
                    return builder.toString();
                }
            }
            catch (Exception ignore)
            {
                // ignore exceptions when trying to generate the error message
            }
        }
        return builder.append(": could not find '").append(pathElements.get(0)).append("' in ").append(M3Paths.Root).toString();
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
        throw new PureExecutionException("Can't find the property '" + propertyName + "' in the class '" + className + "'", Stacks.mutable.empty());
    }

    public static <E> E getEnumByName(Enumeration<E> enumeration, String name)
    {
        return enumeration._values().detect(e -> name.equals(((Enum) e)._name()));
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, Supplier<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> genericTypeBuilder, ExecutionSupport execSupport)
    {
        CompiledExecutionSupport compExecSupport = (CompiledExecutionSupport) execSupport;
        return safeGetGenericType(val, compExecSupport.getMetadataAccessor(), genericTypeBuilder, compExecSupport.getProcessorSupport());
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, MetadataAccessor ma, Supplier<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> genericTypeBuilder, ProcessorSupport processorSupport)
    {
        if (val == null)
        {
            return genericTypeBuilder.get()._rawType(ma.getBottomType());
        }
        if (val instanceof QuantityCoreInstance)
        {
            return genericTypeBuilder.get()._rawType(((QuantityCoreInstance) val).getUnit());
        }
        if (val instanceof Any)
        {
            Any a = (Any) val;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType = a._classifierGenericType();
            return (genericType == null) ? genericTypeBuilder.get()._rawType(CompiledSupport.getType(a, ma)) : genericType;
        }
        if ((val instanceof Long) || (val instanceof BigInteger))
        {
            Type type = ma.getPrimitiveType(M3Paths.Integer);
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof String)
        {
            Type type = ma.getPrimitiveType(M3Paths.String);
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof Boolean)
        {
            Type type = ma.getPrimitiveType(M3Paths.Boolean);
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof PureDate)
        {
            Type type = ma.getPrimitiveType(DateFunctions.datePrimitiveType((PureDate) val));
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof Double)
        {
            Type type = ma.getPrimitiveType(M3Paths.Float);
            return genericTypeBuilder.get()._rawType(type);
        }
        if (val instanceof BigDecimal)
        {
            Type type = ma.getPrimitiveType(M3Paths.Decimal);
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
                    throw new PureExecutionException("TODO: Find most common type for non-concrete generic type", Stacks.mutable.empty());
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
        throw new PureExecutionException("ERROR unhandled type for value: " + val + " (instance of " + val.getClass() + ")", Stacks.mutable.empty());
    }


    public static SharedPureFunction<?> getSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        SharedPureFunction<?> foundFunc = findSharedPureFunction(func, bridge, es);
        if (foundFunc == null)
        {
            throw new PureExecutionException("Can't execute " + func + " | name:'" + func._name() + "' id:'" + func.getName() + "' yet", Stacks.mutable.empty());
        }
        return foundFunc;
    }

    @SuppressWarnings("unchecked")
    public static SharedPureFunction<?> findSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        CompiledExecutionSupport ces = (CompiledExecutionSupport) es;
        MutableList<PureFunction1<Object, Object>> extra = ces.getCompiledExtensions().asLazy().collect(x -> x.getExtraFunctionEvaluation(func, bridge, es)).select(Objects::nonNull, Lists.mutable.empty());
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
            return ces.getFunctionCache().getFunctionForClassProperty((Property<?, ?>) func);
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
                return ces.getFunctionCache().getIfAbsentPutJavaFunctionForPureFunction(func, () ->
                        {
                            try
                            {
                                RichIterable<? extends VariableExpression> params = ((FunctionType) func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();
                                Class<?>[] paramClasses = new Class[params.size() + 1];
                                int index = 0;
                                for (VariableExpression o : params)
                                {
                                    paramClasses[index++] = pureTypeToJavaClassForExecution(o, bridge, es);
                                }
                                paramClasses[params.size()] = ExecutionSupport.class;
                                Method method = ces.getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + IdBuilder.sourceToId(func.getSourceInformation())).getMethod(FunctionProcessor.functionNameToJava(func), paramClasses);
                                return new JavaMethodWithParamsSharedPureFunction<>(method, paramClasses, func.getSourceInformation());
                            }
                            catch (ReflectiveOperationException e)
                            {
                                throw new RuntimeException(e);
                            }
                        });
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
            Class<?> myClass = ces.getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + IdBuilder.sourceToId(func.getSourceInformation()));
            functions = (MutableMap<String, SharedPureFunction<?>>) myClass.getDeclaredField("__functions").get(null);
        }
        catch (ReflectiveOperationException e)
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
            if (paramInputs.size() > 1)
            {
                throw new PureExecutionException(func.getSourceInformation(), "Error accessing property '" + func.getName() + "': too many arguments (expected 1, got " + paramInputs.size() + ")", Stacks.mutable.empty());
            }
            Object instance = getInstanceForPropertyEvaluate(paramInputs, func.getName(), func.getSourceInformation());
            try
            {
                return instance.getClass().getMethod("_" + func.getName()).invoke(instance);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof Error)
                {
                    throw (Error) cause;
                }
                if (cause instanceof PureException)
                {
                    throw (PureException) cause;
                }
                throw new PureExecutionException(func.getSourceInformation(), "Error invoking property '" + func.getName() + "'", cause, Stacks.mutable.empty());
            }
            catch (Exception e)
            {
                throw new PureExecutionException(func.getSourceInformation(), "Error accessing property '" + func.getName() + "'", e, Stacks.mutable.empty());
            }
        }

        RichIterable<? extends VariableExpression> params = ((FunctionType) func._classifierGenericType()._typeArguments().getAny()._rawType())._parameters();
        Class<?>[] paramClasses = new Class<?>[params.size()];
        int index = 0;
        for (VariableExpression o : params)
        {
            paramClasses[index++] = pureTypeToJavaClassForExecution(o, bridge, es);
        }

        Object[] paramInstances = new Object[params.size()];
        paramInputs.forEachWithIndex((input, i) -> paramInstances[i] = (paramClasses[i] == RichIterable.class) ? input : ((RichIterable<?>) input).getAny());
        try
        {
            if (func instanceof QualifiedProperty)
            {
                Object o = getInstanceForPropertyEvaluate(paramInputs, func._functionName(), func.getSourceInformation());
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
                    throw new PureExecutionException(func.getSourceInformation(), builder.toString(), Stacks.mutable.empty());
                }
                return foundFunc.execute(Lists.mutable.with(paramInstances), es);
            }
            throw new PureExecutionException(func.getSourceInformation(), "Unknown function type:" + func.getClass().getName(), Stacks.mutable.empty());
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error executing ");
            String name = func._name();
            if (name == null)
            {
                builder.append("anonymous function");
            }
            else
            {
                builder.append("'").append(name).append("'");
            }
            String message = e.getMessage();
            if (message != null)
            {
                builder.append(": ").append(message);
            }
            throw new PureExecutionException(func.getSourceInformation(), builder.toString(), e, Stacks.mutable.empty());
        }
    }

    private static Object getInstanceForPropertyEvaluate(ListIterable<?> paramInputs, String name, SourceInformation sourceInfo)
    {
        if (paramInputs.notEmpty())
        {
            Object first = paramInputs.get(0);
            if (first instanceof Iterable)
            {
                Object instance = Iterate.getFirst((Iterable<?>) first);
                if (instance != null)
                {
                    int size = Iterate.sizeOf((Iterable<?>) first);
                    if (size > 1)
                    {
                        throw new PureExecutionException(sourceInfo, "Error accessing property '" + name + "': got too many instances (expected 1, got " + size + ")", Stacks.mutable.empty());
                    }
                    return instance;
                }
            }
            else if (first != null)
            {
                return first;
            }
        }
        throw new PureExecutionException(sourceInfo, "Error accessing property '" + name + "': no instance", Stacks.mutable.empty());
    }

    @SuppressWarnings("unchecked")
    private static SharedPureFunction<?> getNativeOrLambdaFunction(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func)
    {
        CompiledExecutionSupport ces = (CompiledExecutionSupport) es;
        return ces.getFunctionCache().getIfAbsentPutJavaFunctionForPureFunction(func, () ->
        {
            try
            {
                Class<?> myClass = ces.getClassLoader().loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + IdBuilder.sourceToId(func.getSourceInformation()));
                MutableMap<String, SharedPureFunction<?>> functions = (MutableMap<String, SharedPureFunction<?>>) myClass.getDeclaredField("__functions").get(null);
                return functions.get(func.getName());
            }
            catch (ReflectiveOperationException e)
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
            Class<?> c = ((CompiledExecutionSupport) es).getClassLoader().loadClass(JavaPackageAndImportBuilder.platformJavaPackage() + "." + Pure.elementToPath(aClass, "_", true) + "_Impl");
            Any result = (Any) c.getConstructor(String.class).newInstance(name);
            // Set default values
            RichIterable<CoreInstance> classProperties = _Class.getSimpleProperties(aClass, ((CompiledExecutionSupport) es).getProcessorSupport());
            classProperties.forEach(new CheckedProcedure<CoreInstance>()
            {
                @Override
                public void safeValue(CoreInstance p) throws Exception
                {
                    if (p instanceof Property<?, ?>)
                    {
                        Property<?, ?> prop = (Property<?, ?>) p;
                        DefaultValue defaultValue = prop._defaultValue();
                        if (defaultValue != null)
                        {
                            Object res = reactivate(defaultValue._functionDefinition()._expressionSequence().getFirst(), new PureMap(Maps.fixedSize.empty()), bridge, es);
                            Method method = c.getMethod("_" + prop._name(), RichIterable.class);
                            if (res instanceof RichIterable)
                            {
                                method.invoke(result, res);
                            }
                            else
                            {
                                method.invoke(result, Lists.fixedSize.of(res));
                            }
                        }
                    }
                }
            });
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
            MutableSet<Object> set = PureEqualsHashingStrategy.newMutableSet();
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

    public static Package buildPackageIfNonExistent(Package pack, ListIterable<String> path, SourceInformation si, Function<String, Package> packageBuilder)
    {
        return path.injectInto(pack, (pkg, name) ->
        {
            PackageableElement child = pkg._children().detect(c -> name.equals(c._name()));
            if (child == null)
            {
                Package newPkg =  packageBuilder.apply(name)._name(name)._package(pkg);
                pkg._childrenAdd(newPkg);
                return newPkg;
            }
            if (!(child instanceof Package))
            {
                StringBuilder builder = new StringBuilder("Package '");
                org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, pkg);
                builder.append("' already has a child named '").append(name).append("' which is not a package");
                throw new PureExecutionException(si, builder.toString(), Stacks.mutable.empty());
            }
            return (Package) child;
        });
    }

    public static Class<?> fromJsonResolveType(JSONObject jsonObject, String pureType, Class<?> typeFromClassMetaData, String typeKey, ExecutionSupport execSupport)
    {
        return fromJsonResolveType(jsonObject, pureType, typeFromClassMetaData, typeKey, (CompiledExecutionSupport) execSupport);
    }

    public static Class<?> fromJsonResolveType(JSONObject jsonObject, String pureType, Class<?> typeFromClassMetaData, String typeKey, CompiledExecutionSupport execSupport)
    {
        String targetTypeName = (String) jsonObject.get(typeKey);
        if (targetTypeName != null)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cls = execSupport.getMetadataAccessor().getClass(pureType);
            MutableSet<Generalization> set = Sets.mutable.ofAll(cls._specializations());
            Deque<Generalization> deque = new ArrayDeque<>(set);
            while (!deque.isEmpty())
            {
                Type type = deque.poll()._specific();
                if (targetTypeName.equals(type._name()))
                {
                    try
                    {
                        return execSupport.getClassCache().getIfAbsentPutInterfaceForType(type);
                    }
                    catch (Exception ignore)
                    {
                        // Type specified is incorrect or problem with metadata. Return default.
                        return typeFromClassMetaData;
                    }
                }
                type._specializations().forEach(spec ->
                {
                    if (set.add(spec))
                    {
                        deque.addLast(spec);
                    }
                });
            }
        }
        return typeFromClassMetaData;
    }
}
