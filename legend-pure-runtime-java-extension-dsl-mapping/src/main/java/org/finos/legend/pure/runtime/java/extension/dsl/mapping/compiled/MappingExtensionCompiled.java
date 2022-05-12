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

package org.finos.legend.pure.runtime.java.extension.dsl.mapping.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class MappingExtensionCompiled extends BaseCompiledExtension
{
    public MappingExtensionCompiled()
    {
        super(
                Lists.fixedSize.with(),
                Lists.fixedSize.with(),
                Lists.fixedSize.with((mapping, cg, processorContext) ->
                {
                    if (Instance.instanceOf(mapping, M2MappingPaths.Mapping, cg.getProcessorSupport()))
                    {
                        for (CoreInstance classMapping : mapping.getValueForMetaPropertyToMany(M2MappingProperties.classMappings))
                        {
                            if (Instance.instanceOf(classMapping, M2MappingPaths.PureInstanceSetImplementation, cg.getProcessorSupport()))
                            {
                                if (classMapping.getValueForMetaPropertyToOne(M2MappingProperties.filter) != null)
                                {
                                    ValueSpecificationProcessor.createFunctionForLambda(mapping, classMapping.getValueForMetaPropertyToOne(M2MappingProperties.filter), cg.getProcessorSupport(), processorContext);
                                }
                                for (CoreInstance propertyMapping : classMapping.getValueForMetaPropertyToMany(M2MappingProperties.propertyMappings))
                                {
                                    ValueSpecificationProcessor.createFunctionForLambda(mapping, propertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.transform), cg.getProcessorSupport(), processorContext);
                                }
                            }
                            else
                            {
                                CompiledExtensionLoader.extensions().flatCollect(CompiledExtension::getExtraClassMappingProcessors).forEach(ep -> ep.value(mapping, classMapping, processorContext, cg.getProcessorSupport()));
                            }
                        }
                    }
                }),
                Lists.fixedSize.with());
    }

    public static CompiledExtension extension()
    {
        return new MappingExtensionCompiled();
    }
}
