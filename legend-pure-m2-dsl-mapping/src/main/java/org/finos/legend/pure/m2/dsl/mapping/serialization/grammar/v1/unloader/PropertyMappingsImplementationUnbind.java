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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.unloader;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OtherwiseEmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyOwnerImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PropertyMappingsImplementationUnbind implements MatchRunner<PropertyMappingsImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.PropertyMappingsImplementation;
    }

    @Override
    public void run(PropertyMappingsImplementation propertyMappingsImpl, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();
        unbindPropertyMappings(propertyMappingsImpl, modelRepository, processorSupport);
    }

    public static void unbindPropertyMappings(PropertyMappingsImplementation owner, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        for (PropertyMapping propertyMapping : owner._propertyMappings())
        {
            if (propertyMapping._localMappingProperty() != null && propertyMapping._localMappingProperty())
            {
                CoreInstance propertyCI = ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);
                if(propertyCI instanceof Property)
                {
                    String propertyName = ((Property)propertyCI)._name();
                    propertyMapping._propertyCoreInstance(modelRepository.newStringCoreInstance_cached(propertyName));
                }
            }
            if (propertyMapping instanceof PropertyMappingsImplementation)
            {
                //Embedded mappings
                unbindPropertyMappings((PropertyMappingsImplementation)propertyMapping, modelRepository, processorSupport);

                PropertyOwnerImplementationUnbind.unbindPropertyOwnerParent((PropertyOwnerImplementation)propertyMapping, processorSupport);
                if (propertyMapping instanceof OtherwiseEmbeddedSetImplementation)
                {
                    PropertyMapping otherwiseMapping = ((OtherwiseEmbeddedSetImplementation)propertyMapping)._otherwisePropertyMapping();
                    if(otherwiseMapping instanceof PropertyMappingsImplementation)
                    {
                        unbindPropertyMappings((PropertyMappingsImplementation)otherwiseMapping, modelRepository, processorSupport);
                    }
                }
            }
            CoreInstance property = ImportStub.withImportStubByPass(propertyMapping._propertyCoreInstance(), processorSupport);
            Shared.cleanUpReferenceUsage(property, propertyMapping, processorSupport);
            revertEdgePointPropertyToOriginal(propertyMapping, property, modelRepository, processorSupport);
        }
    }

    private static void revertEdgePointPropertyToOriginal(PropertyMapping propertyMapping, CoreInstance property, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (MilestoningFunctions.isEdgePointProperty(property, processorSupport))
        {
            String propertyName = property.getName();
            String originalPropertyName = MilestoningFunctions.getSourceEdgePointPropertyName(propertyName);
            propertyMapping._propertyCoreInstance(repository.newStringCoreInstance_cached(originalPropertyName));
        }
    }
}


