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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.tools;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.lang.New;

public class DeserializationUtils
{
    private DeserializationUtils()
    {}

    /**
     * An association contains two properties. Given one of them, this method returns the other.
     * Ensure that the first arg is an instance of Association, otherwise may produce unexpected results.
     */
    private static CoreInstance otherPropertyInAssociation(CoreInstance association, CoreInstance thisProperty, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> associationProperties = Instance.getValueForMetaPropertyToManyResolved(association, M3Properties.properties, processorSupport);
        return associationProperties.get(thisProperty.equals(associationProperties.get(0)) ? 1 : 0);
    }

    /**
     * Apply to both JSON and AVRO.
     * Take JSON as example:
     * In JSON, associations can be described by an unlimited number of nested JSON Objects. As a result, reverse properties
     * from associations between two classes, X and Y, may be described both as properties on X and as properties on Y.
     * For example, '{"x": {"y": {}}}' where an instance of Y (the inner most '{}') is assigned as a property of an instance of X ('{"y": {}}')
     * and then a completely different instance of Y is given this instance of X. These two instances of Y must be reconciled.
     * For correctness with the behavior of compiled mode, where assigning reverse properties severs the prior references,
     * all reverse properties are first removed and then assigned here. As a result, the outermost layer of the nested JSON Objects
     * will always be the only set of properties that are kept.
     */
    public static void replaceReverseProperties(CoreInstance instance, ProcessorSupport processorSupport, SourceInformation si)
    {
        CoreInstance associationClass = processorSupport.package_getByUserPath(M3Paths.Association);
        for (String propertyName : instance.getKeys())
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, propertyName, processorSupport);
            if (values.notEmpty())
            {
                CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(processorSupport.getClassifier(instance), propertyName);
                CoreInstance propertyOwner = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.owner, processorSupport);
                if (Instance.instanceOf(propertyOwner, associationClass, processorSupport))
                {
                    CoreInstance reverseProperty = otherPropertyInAssociation(propertyOwner, property, processorSupport);
                    String reversePropertyName = Property.getPropertyName(reverseProperty);
                    for (CoreInstance value : values)
                    {
                        Instance.removeProperty(value, reversePropertyName, processorSupport);
                    }
                }
            }
        }

        // Multiplicity checks are skipped because they are performed in the forward process by the PropertyConverters
        New.updateReverseProperties(instance, si, processorSupport, true);
    }
}
