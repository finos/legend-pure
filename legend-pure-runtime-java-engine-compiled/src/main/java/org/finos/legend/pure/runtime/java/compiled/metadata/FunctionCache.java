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
import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

import java.lang.reflect.Method;

/**
 * Cache of SharedPureFunctions for properties and functions
 */
public class FunctionCache
{
    private static final Function0<ConcurrentMutableMap<String, SharedPureFunction>> NEW_CONCURRENT_MAP = new Function0<ConcurrentMutableMap<String, SharedPureFunction>>()
    {
        @Override
        public ConcurrentMutableMap<String, SharedPureFunction> value()
        {
            return ConcurrentHashMap.newMap();
        }
    };

    //MutableMap<Root_meta_pure_metamodel_type_Type, org.eclipse.collections.api.map.MutableMap<String, SharedPureFunction>
    private final ConcurrentMutableMap<CoreInstance, ConcurrentMutableMap<String, SharedPureFunction>> classPropertyJavaFunction = ConcurrentHashMap.newMap();

    //MutableMap<Root_meta_pure_metamodel_function_Function, SharedPureFunction>
    private final ConcurrentMutableMap<CoreInstance, SharedPureFunction> pureFunctionJavaFunction = ConcurrentHashMap.newMap();

    public SharedPureFunction getIfAbsentPutFunctionForClassProperty(final CoreInstance srcType, final CoreInstance propertyFunction, final ClassLoader classLoader)
    {
        if (srcType == null)
        {
            throw new IllegalArgumentException("Null source type for property: " + propertyFunction);
        }
        final String propertyJavaName = "_" + propertyFunction.getValueForMetaPropertyToOne(M3Properties.name).getName();
        return this.classPropertyJavaFunction.getIfAbsentPut(srcType, NEW_CONCURRENT_MAP).getIfAbsentPut(propertyJavaName, new CheckedFunction0<SharedPureFunction>()
        {
            @Override
            public SharedPureFunction safeValue() throws ClassNotFoundException, NoSuchMethodException
            {
                String javaClassName = TypeProcessor.fullyQualifiedJavaInterfaceNameForType(srcType);
                Class<?> srcClass = classLoader.loadClass(javaClassName);
                Method propertyMethod = srcClass.getMethod(propertyJavaName);
                return new JavaMethodSharedPureFunction(propertyMethod, propertyFunction.getSourceInformation());
            }
        });
    }

    public SharedPureFunction getIfAbsentPutJavaFunctionForPureFunction(CoreInstance pureFunction, Function0<SharedPureFunction> sharedPureFunctionFunctionCreator)
    {
        return this.pureFunctionJavaFunction.getIfAbsentPut(pureFunction, sharedPureFunctionFunctionCreator);
    }
}
