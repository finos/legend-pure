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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
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
        RichIterable<? extends CoreInstance> properties = includeInheritedProperties ? processorContext.getSupport().class_getSimpleProperties(sourceClass) : sourceClass.getValueForMetaPropertyToMany(M3Properties.properties);
        MutableList<Pair<String, String>> result = Lists.mutable.empty();
        properties.forEach(p ->
        {
            String javaExpression = getDefaultValueJavaExpression(p, doSingleWrap, processorContext);
            if (javaExpression != null)
            {
                result.add(Tuples.pair(p.getName(), javaExpression));
            }
        });
        return result.sortThisBy(Pair::getOne).collect(p -> formatString.apply(p.getOne(), p.getTwo()));
    }

    public static String getDefaultValueJavaExpression(CoreInstance property, boolean doSingleWrap, ProcessorContext processorContext)
    {
        CoreInstance defaultValue = property.getValueForMetaPropertyToOne(M3Properties.defaultValue);
        if (defaultValue == null)
        {
            return null;
        }

        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance expression = Property.getDefaultValueExpression(defaultValue);
        String value = ValueSpecificationProcessor.processValueSpecification(expression, processorContext);
        if ("this".equals(value))
        {
            CoreInstance expressionRawType = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.genericType, org.finos.legend.pure.m3.navigation.M3Properties.rawType, processorSupport);
            value = JavaPackageAndImportBuilder.buildImplClassNameFromType(expressionRawType, processorContext.getClassImplSuffix(), processorSupport) + ".this";
        }

        boolean propertyIsToOne = Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.multiplicity, processorSupport), false);
        CoreInstance expressionMultiplicity = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.multiplicity, processorSupport);
        if ((doSingleWrap || !propertyIsToOne) && (Multiplicity.isLowerZero(expressionMultiplicity) || Multiplicity.isToOne(expressionMultiplicity)))
        {
            //wrap
            value = "CompiledSupport.toPureCollection(" + value + ")";
        }

        return value;
    }
}
