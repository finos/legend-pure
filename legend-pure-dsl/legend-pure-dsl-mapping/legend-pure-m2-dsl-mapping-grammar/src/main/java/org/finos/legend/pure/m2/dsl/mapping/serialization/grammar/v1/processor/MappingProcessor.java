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

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SubstituteStore;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;

public class MappingProcessor extends Processor<Mapping>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.Mapping;
    }

    @Override
    public void process(Mapping mapping, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        RichIterable<? extends MappingInclude> includes = mapping._includes();

        RichIterable<? extends SetImplementation> classMappings = mapping._classMappings();
        // First, make sure all class mappings have their ids set
        for (SetImplementation classMapping : classMappings)
        {
            SetImplementationProcessor.ensureSetImplementationHasId(classMapping, repository, processorSupport);
        }

        processIncludes(matcher, state, includes, processorSupport);

        // Next, do full post-processing on the class mappings (which often assumes ids are set)
        for (SetImplementation classMapping : classMappings)
        {
            PostProcessor.processElement(matcher, classMapping, state, processorSupport);
        }

        for (EnumerationMapping enumerationMapping : mapping._enumerationMappings())
        {
            PostProcessor.processElement(matcher, enumerationMapping, state, processorSupport);
        }

        for (AssociationImplementation associationMapping : mapping._associationMappings())
        {
            PostProcessor.processElement(matcher, associationMapping, state, processorSupport);
        }
    }

    @Override
    public void populateReferenceUsages(Mapping mapping, ModelRepository repository, ProcessorSupport processorSupport)
    {
        RichIterable<? extends MappingInclude> includes = mapping._includes();
        for (MappingInclude include : includes)
        {
            // Process included mapping
            addReferenceUsageForToOneProperty(include, include._includedCoreInstance(), M3Properties.included, repository, processorSupport);

            // Process source substitutions
            RichIterable<? extends SubstituteStore> storeSubstitutions = include._storeSubstitutions();
            for (SubstituteStore storeSub : storeSubstitutions)
            {
                addReferenceUsageForToOneProperty(storeSub, storeSub._originalCoreInstance(), M2MappingProperties.original, repository, processorSupport, storeSub._originalCoreInstance().getSourceInformation());
                addReferenceUsageForToOneProperty(storeSub, storeSub._substituteCoreInstance(), M2MappingProperties.substitute, repository, processorSupport, storeSub._substituteCoreInstance().getSourceInformation());
            }
        }
    }

    private static void processIncludes(Matcher matcher, ProcessorState state, RichIterable<? extends MappingInclude> includes, ProcessorSupport processorSupport)
    {
        for (MappingInclude include : includes)
        {
            // Process included mapping
            Mapping includedMapping = (Mapping) ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);

            PostProcessor.processElement(matcher, includedMapping, state, processorSupport);

            // Process source substitutions
            RichIterable<? extends SubstituteStore> storeSubstitutions = include._storeSubstitutions();
            for (SubstituteStore storeSub : storeSubstitutions)
            {
                storeSub._owner(include);
                // Resolve the original and substitute store references
                ImportStub.withImportStubByPass(storeSub._originalCoreInstance(), processorSupport);
                ImportStub.withImportStubByPass(storeSub._substituteCoreInstance(), processorSupport);
            }
        }
    }
}
