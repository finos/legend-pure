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

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AssociationImplementationProcessor extends Processor<AssociationImplementation>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.AssociationImplementation;
    }

    @Override
    public void process(AssociationImplementation associationMapping, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        Association association = (Association) ImportStub.withImportStubByPass(associationMapping._associationCoreInstance(), processorSupport);
        if (association == null)
        {
            throw new PureCompilationException(associationMapping.getSourceInformation(), "Association mapping missing association");
        }

        if (associationMapping._id() == null)
        {
            String id = PackageableElement.getUserPathForPackageableElement(association, "_");
            associationMapping._id(id);
        }

        for (PropertyMapping propertyMapping : associationMapping._propertyMappings())
        {
            PropertyMappingProcessor.processPropertyMapping(propertyMapping, repository, processorSupport, association, associationMapping);
        }
    }

    @Override
    public void populateReferenceUsages(AssociationImplementation associationMapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
        addReferenceUsageForToOneProperty(associationMapping, associationMapping._associationCoreInstance(), M3Properties.association, repository, processorSupport);
        for (PropertyMapping propertyMapping : associationMapping._propertyMappings())
        {
            PropertyMappingProcessor.populateReferenceUsagesForPropertyMapping(propertyMapping, repository, processorSupport);
        }
    }
}


