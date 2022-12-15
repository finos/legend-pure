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

package org.finos.legend.pure.m3.navigation.property;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.GenericTypeWithXArguments;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Property
{
    /**
     * Get the property id of the property.
     * For Simple properties it is just the name.
     * For Qualified properties it is the function signature
     * containing the parameters.
     *
     * @param property         property
     * @param processorSupport processor support
     * @return property name
     */
    public static String getPropertyId(CoreInstance property, final ProcessorSupport processorSupport)
    {
        String name;
        if (Instance.instanceOf(property, M3Paths.QualifiedProperty, processorSupport))
        {
            CoreInstance functionType = processorSupport.function_getFunctionType(property);
            name = getPropertyName(property) + "(" + ListHelper.tail(functionType.getValueForMetaPropertyToMany(M3Properties.parameters)).collect(new org.eclipse.collections.api.block.function.Function<CoreInstance, String>()
            {
                @Override
                public String valueOf(CoreInstance coreInstance)
                {
                    return GenericType.print(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.genericType, processorSupport), processorSupport);
                }
            }).makeString(",") + ")";
        }
        else
        {
            name = getPropertyName(property);
        }
        return name;
    }

    public static String getPropertyName(CoreInstance property)
    {
        return PrimitiveUtilities.getStringValue(property.getValueForMetaPropertyToOne(M3Properties.name));
    }

    public static ListIterable<String> calculatePropertyPath(CoreInstance property, ProcessorSupport processorSupport)
    {
        // Example: [Root, children, core, children, Any, properties, classifierGenericType]
        String propertyName = getPropertyName(property);

        CoreInstance sourceType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.classifierGenericType, processorSupport), M3Properties.typeArguments, processorSupport).get(0), M3Properties.rawType, processorSupport);

        String propertiesProperty = getPropertiesProperty(sourceType, property);
        if (propertiesProperty == null)
        {
            throw new RuntimeException("Could not construct path for property '" + propertyName + "' on '" + PackageableElement.getUserPathForPackageableElement(sourceType) + "'");
        }

        MutableList<String> result = Lists.mutable.empty();
        PackageableElement.collectM4Path(sourceType, result);
        result.add(propertiesProperty);
        result.add(propertyName);
        return result;
    }

    private static String getPropertiesProperty(CoreInstance sourceType, CoreInstance property)
    {
        for (String propertiesProperty : _Class.SIMPLE_PROPERTIES_PROPERTIES)
        {
            if (sourceType.getValueForMetaPropertyToMany(propertiesProperty).contains(property))
            {
                return propertiesProperty;
            }
        }
        return null;
    }

    public static CoreInstance resolveInstancePropertyReturnMultiplicity(CoreInstance instance, CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance sourceGenericType = Instance.instanceOf(instance, M3Paths.ValueSpecification, processorSupport) ? Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.genericType, processorSupport) : Instance.extractGenericTypeFromInstance(instance, processorSupport);
        return resolvePropertyReturnMultiplicity(sourceGenericType, property, processorSupport);
    }

    public static CoreInstance resolvePropertyReturnMultiplicity(CoreInstance sourceGenericType, CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(property), M3Properties.returnMultiplicity, processorSupport);
        if (Multiplicity.isMultiplicityConcrete(multiplicity))
        {
            return multiplicity;
        }

        CoreInstance propertyOwnerRawType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.classifierGenericType, processorSupport), M3Properties.typeArguments, processorSupport).get(0), M3Properties.rawType, processorSupport);
        GenericTypeWithXArguments p = GenericType.resolveClassMultiplicityParameterUsingInheritance(sourceGenericType, propertyOwnerRawType, processorSupport);
        return p.getArgumentsByParameterName().get(Multiplicity.getMultiplicityParameter(multiplicity));
    }

    public static CoreInstance getSourceType(CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance sourceGenericType = Function.getParameterGenericType(property, 0, processorSupport);
        return Instance.getValueForMetaPropertyToOneResolved(sourceGenericType, M3Properties.rawType, processorSupport);
    }

    public static CoreInstance getDefaultValueExpression(CoreInstance defaultValue)
    {
        if (defaultValue != null)
        {
            return defaultValue.getValueForMetaPropertyToOne(M3Properties.functionDefinition).getValueForMetaPropertyToOne(M3Properties.expressionSequence);
        }
        return null;
    }

    public static ListIterable<? extends CoreInstance> getDefaultValue(CoreInstance defaultValue)
    {
        if (defaultValue != null)
        {
            CoreInstance expressionSequence = defaultValue.getValueForMetaPropertyToOne(M3Properties.functionDefinition).getValueForMetaPropertyToOne(M3Properties.expressionSequence);
            ListIterable<? extends CoreInstance> values = expressionSequence.getValueForMetaPropertyToMany(M3Properties.values);
            if (values.isEmpty())
            {
                values = Lists.immutable.with(expressionSequence);
            }

            return values;
        }
        return Lists.immutable.empty();
    }
}
