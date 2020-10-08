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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled;

import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.generated.Root_meta_pure_functions_lang_KeyValue_Impl;
import org.finos.legend.pure.generated.platform_pure_corefunctions_meta;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializer;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure.handleValidation;

public class JsonNativeImplementation
{
    public static String _toJson(RichIterable<?> pureObject, SourceInformation si, ExecutionSupport es, String typeKeyName, boolean includeType, boolean fullyQualifiedTypePath, boolean serializeQualifiedProperties, String dateTimeFormat, boolean serializePackageableElementName, boolean removePropertiesWithEmptyValues, boolean serializeMultiplicityAsNumber, String encryptionKey, String decryptionKey, RichIterable<? extends CoreInstance> encryptionStereotypes, RichIterable<? extends CoreInstance> decryptionStereotypes)
    {
        return JsonSerializer.toJson(pureObject, ((CompiledExecutionSupport)es).getProcessorSupport(), new JsonSerializationContext<Any, Object>(new JsonSerializationCache(), si, ((CompiledExecutionSupport)es).getProcessorSupport(), new Stack(), typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, encryptionStereotypes, decryptionKey, decryptionStereotypes)
        {
            @Override
            protected Object extractPrimitiveValue(Object potentiallyWrappedPrimitive)
            {
                return potentiallyWrappedPrimitive;
            }

            @Override
            protected RichIterable<?> getValueForProperty(Any pureObject, Property property, String className)
            {
                return this.findAndInvokePropertyMethod(pureObject, property.getName(), className, false);
            }

            @Override
            protected Object evaluateQualifiedProperty(Any pureObject, QualifiedProperty qualifiedProperty, Type type, Multiplicity multiplicity, String propertyName)
            {
                return this.findAndInvokePropertyMethod(pureObject, propertyName, pureObject.getName(), true);
            }

            @Override
            protected CoreInstance getClassifier(Any pureObject)
            {
                return ((CompiledExecutionSupport)es).getProcessorSupport().getClassifier(pureObject);
            }

            @Override
            protected RichIterable<CoreInstance> getMapKeyValues(Any pureObject)
            {
                return ((PureMap)pureObject).getMap().keyValuesView();
            }

            private RichIterable<?> findAndInvokePropertyMethod(Any pureObject, String propertyName, String className, boolean isQualified)
            {
                Method propertyGetter = null;
                try
                {
                    Object value;
                    if (isQualified)
                    {
                        propertyGetter = pureObject.getClass().getMethod(propertyName, ExecutionSupport.class);
                        value = propertyGetter.invoke(pureObject, es);
                    }
                    else
                    {
                        propertyGetter = pureObject.getClass().getMethod("_" + propertyName);
                        value = propertyGetter.invoke(pureObject);
                    }
                    return value instanceof RichIterable ? (RichIterable<?>)value : Lists.immutable.of(value);
                }
                catch (NoSuchMethodException e)
                {
                    throw new PureExecutionException(si, "Error retrieving value of a property: " + propertyName + " from the class " + className + ". Property might not exist", e);
                }
                catch (Exception e)
                {
                    throw new PureExecutionException(si, "Error serializing property: " + propertyName, e);
                }
            }
        }, si);
    }

    public static <T> T _fromJson(String json, Class<T> clazz, String _typeKeyName, boolean _failOnUnknownProperties, SourceInformation si, ExecutionSupport es, ConstraintsOverride constraintsHandler, RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup)
    {
        java.lang.Class c;
        String targetClassName = null;
        try
        {
            targetClassName = JavaPackageAndImportBuilder.platformJavaPackage() + ".Root_" + platform_pure_corefunctions_meta.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1__String_1_(clazz, "_", es);
            c = ((CompiledExecutionSupport)es).getClassLoader().loadClass(targetClassName);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to find  class " + targetClassName, e);
        }


        Map<String, Class> typeLookup = new HashMap<String, Class>();
        for (Pair<? extends String, ? extends String> pair : _typeLookup)
        {
            typeLookup.put(pair._first(), ((CompiledExecutionSupport)es).getMetadataAccessor().getClass("Root::" + pair._second()));
        }

        return (T)JsonDeserializer.fromJson(json, (Class<? extends Any>)clazz, new JsonDeserializationContext(new JsonDeserializationCache(), si, ((CompiledExecutionSupport)es).getProcessorSupport(), _typeKeyName, typeLookup, _failOnUnknownProperties, new ObjectFactory()
        {
            public <U extends Any> U newObject(Class<U> clazz, Map<String, RichIterable<?>> properties)
            {
                FastList<KeyValue> keyValues = new FastList<>();
                for (Map.Entry<String, RichIterable<?>> property : properties.entrySet())
                {
                    KeyValue keyValue = new Root_meta_pure_functions_lang_KeyValue_Impl("Anonymous");
                    keyValue._key(property.getKey());
                    for (Object value : property.getValue())
                    {
                        keyValue._valueAdd(value);
                    }
                    keyValues.add(keyValue);
                }
                U result = (U)Pure.newObject(clazz, keyValues, null, null, null, null, null, null, es);
                result._elementOverride(constraintsHandler);
                return (U)handleValidation(true, result, si, es);
            }

            public <T extends Any> T newUnitInstance(CoreInstance propertyType, String unitTypeString, Number unitValue) throws Exception
            {
                CoreInstance unitRetrieved = ((CompiledExecutionSupport)es).getProcessorSupport().package_getByUserPath(unitTypeString);
                if (!((CompiledExecutionSupport)es).getProcessorSupport().type_subTypeOf(unitRetrieved, propertyType))
                {
                    throw new PureExecutionException("Cannot match unit type: " + unitTypeString + " as subtype of type: " + PackageableElement.getUserPathForPackageableElement(propertyType));
                }

                String unitClassName = UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(unitRetrieved));

                java.lang.Class c = ((CompiledExecutionSupport)es).getClassLoader().loadClass("org.finos.legend.pure.generated." + unitClassName);

                java.lang.Class paramClasses[] = new java.lang.Class[]{String.class, ExecutionSupport.class};
                Method method = c.getMethod("_val", Number.class);
                Object classInstance = c.getConstructor(paramClasses).newInstance("Anonymous_NoCounter", es);
                method.invoke(classInstance, unitValue);
                return (T)classInstance;
            }
        }));
    }


}
