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

package org.finos.legend.pure.runtime.java.compiled.serialization;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectBooleanMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.primitive.ObjectBooleanMaps;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;

import java.math.BigDecimal;

public class GraphSerializer
{
    @Deprecated
    public static Obj buildObj(CoreInstance instance, ClassifierCaches classifierCaches, ProcessorSupport processorSupport)
    {
        return buildObj(instance, IdBuilder.newIdBuilder(processorSupport), classifierCaches, processorSupport);
    }

    public static Obj buildObj(CoreInstance instance, IdBuilder idBuilder, ClassifierCaches classifierCaches, ProcessorSupport processorSupport)
    {
        SourceInformation sourceInformation = instance.getSourceInformation();
        String identifier = idBuilder.buildId(instance);
        String classifierString = classifierCaches.getClassifierId(instance.getClassifier());
        ListIterable<PropertyValue> propertyValues = collectProperties(instance, idBuilder, classifierCaches, processorSupport);
        boolean isEnum = classifierCaches.isEnum(instance);
        return Obj.newObj(classifierString, identifier, instance.getName(), propertyValues, sourceInformation, isEnum);
    }

    private static ListIterable<PropertyValue> collectProperties(CoreInstance instance, IdBuilder idBuilder, ClassifierCaches classifierCaches, ProcessorSupport processorSupport)
    {
        MutableList<PropertyValue> propertyValues = Lists.mutable.empty();
        instance.getKeys().forEach(key ->
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport);
            if (values.notEmpty())
            {
                PropertyValue propertyValue = (values.size() == 1) ?
                        new PropertyValueOne(key, buildRValue(values.get(0), idBuilder, classifierCaches, processorSupport)) :
                        new PropertyValueMany(key, values.collect(value -> buildRValue(value, idBuilder, classifierCaches, processorSupport), Lists.mutable.withInitialCapacity(values.size())));
                propertyValues.add(propertyValue);
            }
        });
        return propertyValues.isEmpty() ? null : propertyValues;
    }

    private static RValue buildRValue(CoreInstance value, IdBuilder idBuilder, ClassifierCaches classifierCaches, ProcessorSupport processorSupport)
    {
        CoreInstance classifier = value.getClassifier();
        if (classifierCaches.isPrimitiveType(classifier))
        {
            return new Primitive(processPrimitiveTypeJava(value, processorSupport));
        }
        String classifierId = classifierCaches.getClassifierId(classifier);
        return classifierCaches.isEnumeration(classifier) ? new EnumRef(classifierId, value.getName()) : new ObjRef(classifierId, idBuilder.buildId(value));
    }

    private static Object processPrimitiveTypeJava(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            throw new IllegalArgumentException("Cannot process null as a primitive value");
        }

        // Special handling for PrimitiveCoreInstances
        if (instance instanceof FloatCoreInstance)
        {
            return ((FloatCoreInstance) instance).getValue().doubleValue();
        }
        if (instance instanceof IntegerCoreInstance)
        {
            return ((IntegerCoreInstance) instance).getValue().longValue();
        }
        if (instance instanceof PrimitiveCoreInstance<?>)
        {
            return ((PrimitiveCoreInstance<?>) instance).getValue();
        }

        // General handling
        if (Instance.instanceOf(instance, M3Paths.String, processorSupport))
        {
            return instance.getName();
        }
        if (Instance.instanceOf(instance, M3Paths.Boolean, processorSupport))
        {
            return Boolean.valueOf(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.LatestDate, processorSupport))
        {
            return LatestDate.instance;
        }
        if (Instance.instanceOf(instance, M3Paths.Date, processorSupport))
        {
            return DateFunctions.parsePureDate(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.Float, processorSupport))
        {
            return Double.valueOf(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.Decimal, processorSupport))
        {
            return new BigDecimal(instance.getName());
        }
        if (Instance.instanceOf(instance, M3Paths.Integer, processorSupport))
        {
            return Long.valueOf(instance.getName());
        }

        // Unknown type
        StringBuilder message = new StringBuilder("Unhandled primitive (type = ");
        PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
        message.append("): ");
        instance.print(message, "");
        throw new IllegalArgumentException(message.toString());
    }

    public static Object valueSpecToJavaObject(CoreInstance instance, Context context, ProcessorSupport processorSupport, Metadata metamodel)
    {
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, M3Properties.values, processorSupport);
        switch (values.size())
        {
            case 0:
            {
                return Lists.mutable.empty();
            }
            case 1:
            {
                return valueSpecValueToJavaObject(values.get(0), context, processorSupport, metamodel);
            }
            default:
            {
                return values.collect(value -> valueSpecValueToJavaObject(value, context, processorSupport, metamodel), Lists.mutable.withInitialCapacity(values.size()));
            }
        }
    }

    private static Object valueSpecValueToJavaObject(CoreInstance value, Context context, ProcessorSupport processorSupport, Metadata metamodel)
    {
        // TODO refactor this
        if (value instanceof JavaCompiledCoreInstance)
        {
            if (processorSupport.instance_instanceOf(value, M3Paths.String))
            {
                return value.getName();
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Boolean))
            {
                return Boolean.valueOf(value.getName());
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Integer))
            {
                return Long.valueOf(value.getName());
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Float))
            {
                return Double.valueOf(value.getName());
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.LatestDate))
            {
                return LatestDate.instance;
            }
            if (processorSupport.instance_instanceOf(value, M3Paths.Date))
            {
                return DateFunctions.parsePureDate(value.getName());
            }
        }
        else
        {
            M3ProcessorSupport m3ProcessorSupport = new M3ProcessorSupport(context, value.getRepository());
            if (Type.isPrimitiveType(value.getClassifier(), m3ProcessorSupport))
            {
                return processPrimitiveTypeJava(value, m3ProcessorSupport);
            }
            if (Instance.instanceOf(value.getClassifier(), M3Paths.Enumeration, m3ProcessorSupport))
            {
                return metamodel.getEnum(MetadataJavaPaths.buildMetadataKeyFromType(value.getClassifier()), value.getName());
            }
            if (Instance.instanceOf(value, M3Paths.Class, m3ProcessorSupport))
            {
                return metamodel.getMetadata(MetadataJavaPaths.Class, PackageableElement.getUserPathForPackageableElement(value));
            }
        }
        return value;
    }

    public static class ClassifierCaches
    {
        private final SetIterable<CoreInstance> primitiveTypes;
        private final CoreInstance enumerationClass;
        private final MutableObjectBooleanMap<CoreInstance> enumerationCache = ObjectBooleanMaps.mutable.empty();
        private final MutableMap<CoreInstance, String> classifierIdCache = Maps.mutable.empty();

        public ClassifierCaches(ProcessorSupport processorSupport)
        {
            this.enumerationClass = _Package.getByUserPath(M3Paths.Enumeration, processorSupport);
            this.primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();
        }

        boolean isPrimitiveValue(CoreInstance instance)
        {
            return isPrimitiveType(instance.getClassifier());
        }

        boolean isEnum(CoreInstance instance)
        {
            return isEnumeration(instance.getClassifier());
        }

        boolean isPrimitiveType(CoreInstance classifier)
        {
            return this.primitiveTypes.contains(classifier);
        }

        boolean isEnumeration(CoreInstance classifier)
        {
            return this.enumerationCache.getIfAbsentPutWithKey(classifier, cl -> cl.getClassifier() == this.enumerationClass);
        }

        public String getClassifierId(CoreInstance classifier)
        {
            return this.classifierIdCache.getIfAbsentPutWithKey(classifier, ClassifierCaches::newClassifierId);
        }

        private static String newClassifierId(CoreInstance classifier)
        {
            return MetadataJavaPaths.buildMetadataKeyFromType(classifier).intern();
        }
    }
}
