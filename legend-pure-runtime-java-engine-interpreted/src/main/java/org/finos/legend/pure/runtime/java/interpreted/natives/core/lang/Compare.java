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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.lang;

import java.util.Stack;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.tools.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

public class Compare extends NativeFunction
{
    private static final ImmutableList<String> PRIMITIVE_TYPE_COMPARISON_ORDER = Lists.immutable.with(M3Paths.Integer, M3Paths.Float, M3Paths.Number, M3Paths.DateTime, M3Paths.StrictDate, M3Paths.Date, M3Paths.Boolean, M3Paths.String);

    private final ModelRepository repository;

    public Compare(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance param1 = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance param2 = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        return ValueSpecificationBootstrap.newIntegerLiteral(this.repository, compare(param1, param2, processorSupport), processorSupport);
    }

    /**
     * System comparison function for two instances.
     *
     * @param instance1        first instance
     * @param instance2        second instance
     * @param processorSupport processor support
     * @return comparison
     */
    public static int compare(CoreInstance instance1, CoreInstance instance2, ProcessorSupport processorSupport)
    {
        if (instance1 == instance2)
        {
            return 0;
        }

        // Numbers
        Number num1 = getNumberValue(instance1, processorSupport);
        Number num2 = getNumberValue(instance2, processorSupport);
        if (num1 != null)
        {
            return (num2 == null) ? -1 : NumericUtilities.compare(num1, num2);
        }
        if (num2 != null)
        {
            return 1;
        }

        // Dates
        PureDate date1 = getDateValue(instance1, processorSupport);
        PureDate date2 = getDateValue(instance2, processorSupport);
        if (date1 != null)
        {
            return (date2 == null) ? -1 : date1.compareTo(date2);
        }
        if (date2 != null)
        {
            return 1;
        }

        // Booleans
        Boolean boolean1 = getBooleanValue(instance1, processorSupport);
        Boolean boolean2 = getBooleanValue(instance2, processorSupport);
        if (boolean1 != null)
        {
            return (boolean2 == null) ? -1 : boolean1.compareTo(boolean2);
        }
        if (boolean2 != null)
        {
            return 1;
        }

        // Strings
        String string1 = getStringValue(instance1, processorSupport);
        String string2 = getStringValue(instance2, processorSupport);
        if (string1 != null)
        {
            return (string2 == null) ? -1 : string1.compareTo(string2);
        }
        if (string2 != null)
        {
            return 1;
        }

        // General case
        CoreInstance type1 = instance1.getClassifier();
        CoreInstance type2 = instance2.getClassifier();
        if (type1 == type2)
        {
            return instance1.getName().compareTo(instance2.getName());
        }
        if (Instance.instanceOf(type1, M3Paths.PrimitiveType, processorSupport))
        {
            if (!Instance.instanceOf(type2, M3Paths.PrimitiveType, processorSupport))
            {
                return -1;
            }
            int index1 = PRIMITIVE_TYPE_COMPARISON_ORDER.indexOf(type1.getName());
            int index2 = PRIMITIVE_TYPE_COMPARISON_ORDER.indexOf(type2.getName());
            return Integer.compare(index1, index2);
        }
        if (Instance.instanceOf(type2, M3Paths.PrimitiveType, processorSupport))
        {
            return 1;
        }
        String path1 = PackageableElement.getUserPathForPackageableElement(type1);
        String path2 = PackageableElement.getUserPathForPackageableElement(type2);
        return path1.compareTo(path2);
    }

    private static Number getNumberValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance instanceof IntegerCoreInstance)
        {
            return ((IntegerCoreInstance)instance).getValue();
        }
        if (instance instanceof FloatCoreInstance)
        {
            return ((FloatCoreInstance)instance).getValue();
        }
        if (instance instanceof DecimalCoreInstance)
        {
            return ((DecimalCoreInstance)instance).getValue();
        }
        if (Instance.instanceOf(instance, M3Paths.Integer, processorSupport))
        {
            return PrimitiveUtilities.getIntegerValue(instance);
        }
        if (Instance.instanceOf(instance, M3Paths.Float, processorSupport))
        {
            return PrimitiveUtilities.getFloatValue(instance);
        }
        if (Instance.instanceOf(instance, M3Paths.Decimal, processorSupport))
        {
            return PrimitiveUtilities.getDecimalValue(instance);
        }
        return null;
    }

    private static PureDate getDateValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance instanceof DateCoreInstance)
        {
            return ((DateCoreInstance)instance).getValue();
        }
        if (Instance.instanceOf(instance, M3Paths.Date, processorSupport))
        {
            return PrimitiveUtilities.getDateValue(instance);
        }
        return null;
    }

    private static Boolean getBooleanValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return ((instance instanceof BooleanCoreInstance) || Instance.instanceOf(instance, M3Paths.Boolean, processorSupport)) ? PrimitiveUtilities.getBooleanValue(instance) : null;
    }

    private static String getStringValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return ((instance instanceof StringCoreInstance) || Instance.instanceOf(instance, M3Paths.String, processorSupport)) ? PrimitiveUtilities.getStringValue(instance) : null;
    }
}
