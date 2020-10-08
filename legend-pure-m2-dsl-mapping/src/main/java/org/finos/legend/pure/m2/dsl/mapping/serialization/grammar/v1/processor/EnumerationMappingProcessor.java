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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumValueMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;


public class EnumerationMappingProcessor extends Processor<EnumerationMapping<?>>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.EnumerationMapping;
    }

    @Override
    public void process(EnumerationMapping<?> enumerationMapping, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        // Resolve the enumeration reference
        ImportStub.withImportStubByPasses(Lists.mutable.with(enumerationMapping._enumerationCoreInstance()).withAll(collectSourceEnumerationCoreInstances(enumerationMapping)), processorSupport);

        // Resolve the enum references
        for (EnumValueMapping enumValueMapping : enumerationMapping._enumValueMappings())
        {
            ImportStub.withImportStubByPasses(Lists.mutable.with(enumValueMapping._enumCoreInstance()).withAll(collectSourceEnumCoreInstances(enumValueMapping)), processorSupport);
        }
    }

    @Override
    public void populateReferenceUsages(EnumerationMapping<?> enumerationMapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
        addReferenceUsagesForToManyProperty(enumerationMapping, Lists.mutable.with(enumerationMapping._enumerationCoreInstance()).withAll(collectSourceEnumerationCoreInstances(enumerationMapping)), M3Properties.enumeration, repository, processorSupport);
    }

    public static void processsEnumerationTransformer(GrammarInfoStub enumerationTransformer, PropertyMapping propertyMapping, ProcessorSupport processorSupport)
    {
        if (enumerationTransformer != null)
        {
            String transformerString = (String)enumerationTransformer._value();
            String mappingName = StringIterate.getFirstToken(transformerString, ",");
            String transformerName = StringIterate.getLastToken(transformerString, ",");

            Mapping mapping = (Mapping)processorSupport.package_getByUserPath(mappingName);
            if (mapping == null)
            {
                throw new PureCompilationException(propertyMapping.getSourceInformation(), "The mapping '" + mappingName + "' is unknown'. Likely a parser error");
            }

            EnumerationMapping<?> transformer = org.finos.legend.pure.m2.dsl.mapping.Mapping.getEnumerationMappingByName(mapping, transformerName, processorSupport);

            if (transformer == null)
            {
                throw new PureCompilationException(propertyMapping.getSourceInformation(), "The transformer '" + transformerName + "' is unknown or is not of type EnumerationMapping in the Mapping '" +
                        mappingName + "' for property " + ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport).getName());
            }
            enumerationTransformer._original(enumerationTransformer._value());
            enumerationTransformer._value(transformer);

        }
    }

    public static MutableList<? extends CoreInstance> collectSourceEnumCoreInstances(EnumValueMapping enumValueMapping)
    {
        return enumValueMapping._sourceValuesCoreInstance().toList();
    }

    public static MutableList<? extends CoreInstance> collectSourceEnumerationCoreInstances(EnumerationMapping<?> enumerationMapping)
    {
        MutableList<CoreInstance> result = Lists.mutable.empty();
        for (EnumValueMapping enumValueMapping : enumerationMapping._enumValueMappings())
        {
            for (CoreInstance sourceValue : enumValueMapping._sourceValuesCoreInstance())
            {
                if (sourceValue instanceof EnumStub)
                {
                    result.add(((EnumStub)sourceValue)._enumerationCoreInstance());
                }
            }
        }
        return result;
    }
}
