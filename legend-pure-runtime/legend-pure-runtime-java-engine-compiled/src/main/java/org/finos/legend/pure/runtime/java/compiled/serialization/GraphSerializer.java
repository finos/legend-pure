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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.JavaCompiledCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

import java.math.BigDecimal;

public class GraphSerializer
{
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

}
