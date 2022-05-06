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
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

import java.lang.reflect.Method;

/**
 * Cache of SharedPureFunctions for properties and functions
 */
public class FunctionCache
{
    //MutableMap<Root_meta_pure_metamodel_type_Type, org.eclipse.collections.api.map.MutableMap<String, SharedPureFunction>
    private final ConcurrentMutableMap<CoreInstance, ConcurrentMutableMap<String, SharedPureFunction<?>>> classPropertyJavaFunction = ConcurrentHashMap.newMap();

    //MutableMap<Root_meta_pure_metamodel_function_Function, SharedPureFunction>
    private final ConcurrentMutableMap<CoreInstance, SharedPureFunction<?>> pureFunctionJavaFunction = ConcurrentHashMap.newMap();

    public SharedPureFunction<?> getIfAbsentPutFunctionForClassProperty(CoreInstance srcType, CoreInstance propertyFunction, ClassLoader classLoader)
    {
        String propertyPureName = PrimitiveUtilities.getStringValue(propertyFunction.getValueForMetaPropertyToOne(M3Properties.name));
        if (srcType == null)
        {
            throw new IllegalArgumentException("Null source type for property: " + propertyPureName);
        }
        String propertyJavaName = "_" + propertyPureName;
        return this.classPropertyJavaFunction.getIfAbsentPut(srcType, ConcurrentHashMap::new).getIfAbsentPut(propertyJavaName, () ->
        {
            String javaClassName = TypeProcessor.fullyQualifiedJavaInterfaceNameForType(srcType);
            try
            {
                Class<?> srcClass = classLoader.loadClass(javaClassName);
                Method propertyMethod = srcClass.getMethod(propertyJavaName);
                return new JavaMethodSharedPureFunction<>(propertyMethod, propertyFunction.getSourceInformation());
            }
            catch (ClassNotFoundException e)
            {
                StringBuilder builder = new StringBuilder("Cannot find Java class ").append(javaClassName).append(" for Pure class ");
                PackageableElement.writeUserPathForPackageableElement(builder, srcType);
                throw new RuntimeException(builder.toString(), e);
            }
            catch (NoSuchMethodException e)
            {
                StringBuilder builder = new StringBuilder("Cannot find method ").append(propertyJavaName).append(" on Java class ").append(javaClassName)
                        .append(" for property ").append(propertyPureName).append(" on Pure class ");
                PackageableElement.writeUserPathForPackageableElement(builder, srcType);
                throw new RuntimeException(builder.toString(), e);
            }
        });
    }

    public SharedPureFunction<?> getIfAbsentPutJavaFunctionForPureFunction(CoreInstance pureFunction, Function0<? extends SharedPureFunction<?>> sharedPureFunctionFunctionCreator)
    {
        return this.pureFunctionJavaFunction.getIfAbsentPut(pureFunction, sharedPureFunctionFunctionCreator);
    }
}
