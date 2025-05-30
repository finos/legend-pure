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

package org.finos.legend.pure.m3.navigation.valuespecification;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class ValueSpecification
{
    public static boolean isExecutable(CoreInstance valueSpecification, ProcessorSupport processorSupport)
    {
        return !"NonExecutableValueSpecification".equals(processorSupport.getClassifier(valueSpecification).getName());
    }

    public static boolean instanceOf(CoreInstance valueSpecification, String typeName, ProcessorSupport processorSupport)
    {
        CoreInstance type = processorSupport.package_getByUserPath(typeName);
        if (type == null)
        {
            throw new RuntimeException(typeName + " is unknown!");
        }
        return instanceOf(valueSpecification, type, processorSupport);
    }

    public static boolean instanceOf(CoreInstance valueSpecification, CoreInstance type, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, M3Properties.rawType, processorSupport);
        return (rawType != null) && processorSupport.type_subTypeOf(rawType, type);
    }

    public static boolean isInstanceValue(CoreInstance instance, ProcessorSupport processorSupport)
    {
        if (instance == null)
        {
            return false;
        }
        if (instance instanceof InstanceValue)
        {
            return true;
        }
        return (!(instance instanceof Any) || (instance instanceof AbstractCoreInstanceWrapper)) && processorSupport.instance_instanceOf(instance, M3Paths.InstanceValue);
    }

    /**
     * Get a single value for a value specification.  If there are no values,
     * then null is returned.  If there is more than one value, then an
     * exception is thrown.
     *
     * @param valueSpecification value specification
     * @return value specification single value or null
     */
    public static CoreInstance getValue(CoreInstance valueSpecification, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.values, processorSupport);
    }

    /**
     * Get the values for the value specification.
     *
     * @param valueSpecification value specification
     * @return value specification values
     */
    public static ListIterable<? extends CoreInstance> getValues(CoreInstance valueSpecification, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToManyResolved(valueSpecification, M3Properties.values, processorSupport);
    }
}
