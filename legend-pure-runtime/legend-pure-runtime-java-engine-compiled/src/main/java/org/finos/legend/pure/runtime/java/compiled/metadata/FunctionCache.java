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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Cache of SharedPureFunctions for properties and functions
 */
public class FunctionCache
{
    private final ConcurrentMutableMap<Type, ConcurrentMutableMap<String, SharedPureFunction<?>>> classPropertyJavaFunction = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<Function<?>, SharedPureFunction<?>> pureFunctionJavaFunction = ConcurrentHashMap.newMap();

    private final ClassCache classCache;

    public FunctionCache(ClassCache classCache)
    {
        this.classCache = classCache;
    }

    @Deprecated
    public FunctionCache()
    {
        this(null);
    }

    @Deprecated
    public SharedPureFunction<?> getIfAbsentPutFunctionForClassProperty(CoreInstance srcType, CoreInstance propertyFunction, ClassLoader classLoader)
    {
        Property<?, ?> property = (Property<?, ?>) propertyFunction;
        if (!Objects.equals(srcType, property._classifierGenericType()._typeArguments().getFirst()._rawType()))
        {
            throw new IllegalArgumentException("Invalid source type for property '" + property._name() + "': " + ((srcType == null) ? "null" : PackageableElement.getUserPathForPackageableElement(srcType)));
        }
        return getFunctionForClassProperty(property);
    }

    @Deprecated
    public SharedPureFunction<?> getIfAbsentPutJavaFunctionForPureFunction(CoreInstance pureFunction, Function0<? extends SharedPureFunction<?>> sharedPureFunctionFunctionCreator)
    {
        return getIfAbsentPutJavaFunctionForPureFunction((Function<?>) pureFunction, sharedPureFunctionFunctionCreator);
    }

    @Deprecated
    public SharedPureFunction<?> getIfAbsentPutFunctionForClassProperty(Property<?, ?> property, ClassLoader classLoader)
    {
        return getFunctionForClassProperty(property);
    }

    public SharedPureFunction<?> getFunctionForClassProperty(Property<?, ?> property)
    {
        Type srcType = property._classifierGenericType()._typeArguments().getFirst()._rawType();
        String propertyName = property._name();
        if (srcType == null)
        {
            throw new IllegalArgumentException("Null source type for property: " + propertyName);
        }
        return this.classPropertyJavaFunction.getIfAbsentPut(srcType, ConcurrentHashMap::new)
                .getIfAbsentPut(propertyName, () -> new JavaMethodSharedPureFunction<>(findGetterMethodForClassProperty(srcType, propertyName), property.getSourceInformation()));
    }

    public SharedPureFunction<?> getIfAbsentPutJavaFunctionForPureFunction(Function<?> pureFunction, Function0<? extends SharedPureFunction<?>> sharedPureFunctionFunctionCreator)
    {
        return this.pureFunctionJavaFunction.getIfAbsentPut(pureFunction, sharedPureFunctionFunctionCreator);
    }

    private Method findGetterMethodForClassProperty(Type srcType, String propertyName)
    {
        String javaMethodName = "_" + propertyName;
        Class<?> javaInterface = this.classCache.getIfAbsentPutInterfaceForType(srcType);
        try
        {
            return javaInterface.getMethod(javaMethodName);
        }
        catch (NoSuchMethodException e)
        {
            StringBuilder builder = new StringBuilder("Cannot find method ").append(javaMethodName).append(" on Java class ").append(javaInterface.getName())
                    .append(" for property ").append(propertyName).append(" on Pure class ");
            PackageableElement.writeUserPathForPackageableElement(builder, srcType);
            throw new RuntimeException(builder.toString(), e);
        }
    }

    @Deprecated
    public static FunctionCache reconcileFunctionCache(FunctionCache functionCache, ClassCache classCache)
    {
        Objects.requireNonNull(classCache, "null classCache");
        if ((functionCache == null) || (functionCache.classCache == null))
        {
            return new FunctionCache(classCache);
        }
        if (functionCache.classCache != classCache)
        {
            throw new RuntimeException("Conflict between class caches: " + functionCache.classCache + " vs " + classCache);
        }
        return functionCache;
    }
}
