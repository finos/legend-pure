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

package org.finos.legend.pure.m3.navigation.measure;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Measure
{
    /**
     * The structure of a unit instance is an InstanceValue wrapping an InstanceValue in its values field,
     * where the inner InstanceValue's values field points to the numeric value instance representing the value of the instance.
     * Thus, in InstanceValueExecutor, we do not flatten the deep structure all the way down to the leaf values property.
     *
     * e.g. let x = 5 Kilogram  --> ^InstanceValue{ genericType = ^GenericType(rawType = ^ImportStub( ^GenericType(rawType =Kilogram)), values = ^InstanceValue(genericType = ^ImportStub( ^GenericType(rawType =Kilogram)), values=^IntegerCoreInstance(val=5)}
     *
     * The expectation during processing is that this structure is immutable, however the ImportStub will be cleaned during bind/unbind.
     *
     * For InstanceValueProcessor, we do not copy over the genericType and multiplicity from a unit instance's values property,
     * since otherwise the matcher will match both the outer and inner InstanceValue of the instance, and in the inner instance, copy over the value type (e.g. Integer).
     *
     * With above, for ValueSpecificationUnbind, we do not remove the genericType and multiplicity from a unit instance,
     * as we do not recover them in InstanceValueProcessor.
     *
     * For ValueSpecificationBootstrap, we do not wrap a unit instance with another layer of InstanceValue because of the
     * change for the flattening logic above. For some native functions e.g. GenericType, for unit instances we do not
     * extract the classifier as we do for PrimitiveType instances but extract the generic type's raw type.
     * Thus these methods.
     */
    public static boolean isUnitOrMeasureInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isInstanceValueWithNonEmptyValuesOfType(instance, Unit.class, processorSupport) || isInstanceValueWithNonEmptyValuesOfType(instance, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure.class, processorSupport);
    }

    public static boolean isUnitInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return isInstanceValueWithNonEmptyValuesOfType(instance, Unit.class, processorSupport);
    }

    public static boolean isUnitGenericType(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        boolean result = false;
        if (genericType != null)
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            result = rawType instanceof Unit;
        }
        return result;
    }

    private static boolean isInstanceValueWithNonEmptyValuesOfType(CoreInstance instance, Class type, ProcessorSupport processorSupport)
    {
        boolean result = false;
        if (instance != null)
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, M3Properties.rawType, processorSupport);
            result = type.isInstance(rawType);
        }
        return result && Instance.instanceOf(instance, M3Paths.InstanceValue, processorSupport) && !Instance.getValueForMetaPropertyToManyResolved(instance, M3Properties.values, processorSupport).isEmpty();
    }
}
