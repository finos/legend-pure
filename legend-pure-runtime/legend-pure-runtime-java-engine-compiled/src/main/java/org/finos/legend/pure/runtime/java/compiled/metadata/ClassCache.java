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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Class cache
 */
public class ClassCache
{
    private final ConcurrentMutableMap<Type, TypeJavaInfo> typeToAttributes = ConcurrentHashMap.newMap();
    private final ClassLoader classLoader;

    public ClassCache(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @Deprecated
    public ClassCache()
    {
        this(null);
    }

    @Deprecated
    public Class<?> getIfAbsentPutInterfaceForType(Type _type, ClassLoader classLoader)
    {
        validateClassLoaderForLegacyMethods(classLoader);
        return getIfAbsentPutInterfaceForType(_type);
    }

    @Deprecated
    public Constructor<?> getIfAbsentPutConstructorForType(Type _type, ClassLoader classLoader)
    {
        validateClassLoaderForLegacyMethods(classLoader);
        return getIfAbsentPutConstructorForType(_type);
    }

    @Deprecated
    public Method getIfAbsentPutPropertySetterMethodForType(Type _type, String propertyName, ClassLoader classLoader)
    {
        validateClassLoaderForLegacyMethods(classLoader);
        return getIfAbsentPutPropertySetterMethodForType(_type, propertyName);
    }

    @Deprecated
    private void validateClassLoaderForLegacyMethods(ClassLoader classLoader)
    {
        if ((classLoader != null) && (classLoader != this.classLoader))
        {
            throw new IllegalArgumentException("Invalid class loader: " + classLoader);
        }
    }

    public Class<?> getIfAbsentPutImplForType(Type type)
    {
        TypeJavaInfo java = getJavaInfoForType(type);
        return java.implClass;
    }

    public Class<?> getIfAbsentPutInterfaceForType(Type type)
    {
        TypeJavaInfo java = getJavaInfoForType(type);
        return java.interfaceClass;
    }

    public Constructor<?> getIfAbsentPutConstructorForType(Type type)
    {
        TypeJavaInfo java = getJavaInfoForType(type);
        return java.constructor;
    }

    public Method getIfAbsentPutPropertySetterMethodForType(Type type, String propertyName)
    {
        TypeJavaInfo java = getJavaInfoForType(type);
        return java.getSetterMethodForProperty(propertyName);
    }

    public void remove(Type type)
    {
        if (type != null)
        {
            this.typeToAttributes.remove(type);
        }
    }

    private TypeJavaInfo getJavaInfoForType(Type type)
    {
        return this.typeToAttributes.getIfAbsentPutWithKey(Objects.requireNonNull(type, "Null type"), this::buildJavaInfo);
    }

    private TypeJavaInfo buildJavaInfo(Type type)
    {
        Class<?> interfaceClass = getJavaInterfaceForPureType(type);
        Class<?> implClass = getJavaImplClassForPureType(type);
        Constructor<?> constructor;
        try
        {
            constructor = implClass.getConstructor(String.class);
        }
        catch (NoSuchMethodException e)
        {
            StringBuilder builder = new StringBuilder("Could not find constructor for ");
            PackageableElement.writeUserPathForPackageableElement(builder, type);
            builder.append(" (").append(implClass.getSimpleName()).append(')');
            throw new RuntimeException(builder.toString(), e);
        }
        return new TypeJavaInfo(interfaceClass, implClass, constructor);
    }

    private Class<?> getJavaInterfaceForPureType(Type type)
    {
        String javaClassName = JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(type);
        try
        {
            return this.classLoader.loadClass(javaClassName);
        }
        catch (ClassNotFoundException e)
        {
            StringBuilder builder = new StringBuilder("Could not find Java interface for ");
            PackageableElement.writeUserPathForPackageableElement(builder, type);
            builder.append(" (").append(javaClassName).append(')');
            throw new RuntimeException(builder.toString(), e);
        }
    }

    private Class<?> getJavaImplClassForPureType(Type type)
    {
        String javaClassName = JavaPackageAndImportBuilder.buildImplClassReferenceFromType(type);
        try
        {
            return this.classLoader.loadClass(javaClassName);
        }
        catch (ClassNotFoundException e)
        {
            StringBuilder builder = new StringBuilder("Could not find Java implementation class for ");
            PackageableElement.writeUserPathForPackageableElement(builder, type);
            builder.append(" (").append(javaClassName).append(')');
            throw new RuntimeException(builder.toString(), e);
        }
    }

    @Deprecated
    public static ClassCache reconcileWithClassLoader(ClassCache classCache, ClassLoader classLoader)
    {
        Objects.requireNonNull(classLoader, "null classLoader");
        if ((classCache == null) || (classCache.classLoader == null))
        {
            return new ClassCache(classLoader);
        }
        if (classCache.classLoader != classLoader)
        {
            throw new RuntimeException("Conflict between class loaders: " + classCache.classLoader + " vs " + classLoader);
        }
        return classCache;
    }

    private static class TypeJavaInfo
    {
        private final Class<?> interfaceClass;
        private final Class<?> implClass;
        private final Constructor<?> constructor;
        private final ConcurrentMutableMap<String, Method> propertySetterMethods = ConcurrentHashMap.newMap();

        private TypeJavaInfo(Class<?> interfaceClass, Class<?> implClass, Constructor<?> constructor)
        {
            this.interfaceClass = interfaceClass;
            this.implClass = implClass;
            this.constructor = constructor;
        }

        Method getSetterMethodForProperty(String propertyName)
        {
            return this.propertySetterMethods.getIfAbsentPutWithKey(Objects.requireNonNull(propertyName, "Null property name"), this::findPropertySetterMethod);
        }

        private Method findPropertySetterMethod(String propertyName)
        {
            try
            {
                return this.implClass.getMethod("_" + propertyName, RichIterable.class);
            }
            catch (NoSuchMethodException e)
            {
                throw new RuntimeException("Could not find setter method for property '" + propertyName + "'", e);
            }
        }
    }
}
