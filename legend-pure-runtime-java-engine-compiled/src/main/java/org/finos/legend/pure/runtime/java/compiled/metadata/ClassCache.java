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
import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Class cache
 */
public class ClassCache
{
    private final ConcurrentMutableMap<Type, Class<?>> typeToJavaInterface = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<Type, ClassAttributes> typeToJavaConstructor = ConcurrentHashMap.newMap();

    public Class<?> getIfAbsentPutInterfaceForType(final Type _type, final ClassLoader classLoader)
    {
        if (_type == null)
        {
            throw new IllegalArgumentException("Null type");
        }

        return this.typeToJavaInterface.getIfAbsentPut(_type, new CheckedFunction0<Class<?>>()
        {
            @Override
            public Class safeValue() throws ClassNotFoundException
            {
                String javaClassName = CompiledSupport.fullyQualifiedJavaInterfaceNameForPackageableElement(_type);
                return classLoader.loadClass(javaClassName);
            }
        });
    }

    public Constructor getIfAbsentPutConstructorForType(Type _type, ClassLoader classLoader)
    {
        ClassAttributes attributes = this.getIfAbsentPutImplClassAttributesForType(_type, classLoader);
        return attributes.constructor;
    }

    public Method getIfAbsentPutPropertySetterMethodForType(Type _type, String propertyName, ClassLoader classLoader)
    {
        ClassAttributes attributes = this.getIfAbsentPutImplClassAttributesForType(_type, classLoader);
        return attributes.getIfAbsentPutSetterMethodForProperty(propertyName);
    }

    private ClassAttributes getIfAbsentPutImplClassAttributesForType(final Type _type, final ClassLoader classLoader)
    {
        if (_type == null)
        {
            throw new IllegalArgumentException("Null type");
        }

        return this.typeToJavaConstructor.getIfAbsentPut(_type, new CheckedFunction0<ClassAttributes>()
        {
            @Override
            public ClassAttributes safeValue() throws ClassNotFoundException, NoSuchMethodException
            {
                String javaClassName = JavaPackageAndImportBuilder.buildImplClassReferenceFromType(_type);
                Class<?> srcClass = classLoader.loadClass(javaClassName);
                return new ClassAttributes(srcClass, srcClass.getConstructor(String.class));
            }
        });
    }

    public void remove(Type _type)
    {
        this.typeToJavaInterface.remove(_type);
        this.typeToJavaConstructor.remove(_type);
    }

    private static class ClassAttributes
    {
        private final Class<?> implClass;
        private final Constructor constructor;
        private final ConcurrentMutableMap<String, Method> propertyNameToSetterMethod = ConcurrentHashMap.newMap();

        private ClassAttributes(Class<?> implClass, Constructor constructor)
        {
            this.implClass = implClass;
            this.constructor = constructor;
        }

        public Method getIfAbsentPutSetterMethodForProperty(final String propertyName)
        {
            if (propertyName == null)
            {
                throw new IllegalArgumentException("Null property name");
            }

            return this.propertyNameToSetterMethod.getIfAbsentPut(propertyName, new CheckedFunction0<Method>()
            {
                @Override
                public Method safeValue() throws NoSuchMethodException
                {
                    return ClassAttributes.this.implClass.getMethod("_" + propertyName, RichIterable.class);
                }
            });
        }
    }
}
