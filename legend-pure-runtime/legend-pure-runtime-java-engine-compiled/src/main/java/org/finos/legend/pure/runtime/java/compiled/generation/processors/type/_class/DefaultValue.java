// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

import java.util.function.BiFunction;

public class DefaultValue
{
    public static ListIterable<String> manageDefaultValues(BiFunction<String, String, String> formatString, CoreInstance sourceClass, boolean doSingleWrap, ProcessorContext processorContext)
    {
        return manageDefaultValues(formatString, sourceClass, doSingleWrap, false, processorContext);
    }

    public static ListIterable<String> manageDefaultValues(BiFunction<String, String, String> formatString, CoreInstance sourceClass, boolean doSingleWrap, boolean includeInheritedProperties, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        RichIterable<? extends CoreInstance> properties = includeInheritedProperties ? _Class.getSimpleProperties(sourceClass, processorSupport) : sourceClass.getValueForMetaPropertyToMany(M3Properties.properties);

        return properties.collectIf(
                p -> p.getValueForMetaPropertyToOne(M3Properties.defaultValue) != null,
                p ->
                {
                    boolean propertyIsToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(p, M3Properties.multiplicity, processorSupport), false);
                    CoreInstance expression = Property.getDefaultValueExpression(Instance.getValueForMetaPropertyToOneResolved(p, M3Properties.defaultValue, processorSupport));
                    String value = ValueSpecificationProcessor.processValueSpecification(expression, processorContext);
                    if ("this".equals(value))
                    {
                        CoreInstance expressionRawType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.genericType, processorSupport), M3Properties.rawType, processorSupport);
                        value = PackageableElement.getSystemPathForPackageableElement(expressionRawType, "_") + processorContext.getClassImplSuffix() + "." + value;
                    }

                    CoreInstance expressionMultiplicity = Multiplicity.newMultiplicity(expression.getValueForMetaPropertyToMany(M3Properties.values).size(), processorSupport);

                    if ((doSingleWrap || !propertyIsToOne) && (Multiplicity.isLowerZero(expressionMultiplicity) || Multiplicity.isToOne(expressionMultiplicity)))
                    {
                        //wrap
                        value = "CompiledSupport.toPureCollection(" + value + ")";
                    }

                    return formatString.apply(p.getName(), value);
                },
                Lists.mutable.empty());
    }
}
