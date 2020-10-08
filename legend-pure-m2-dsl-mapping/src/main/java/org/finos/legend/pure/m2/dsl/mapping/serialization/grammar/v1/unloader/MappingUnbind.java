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

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.Shared;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SubstituteStore;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class MappingUnbind implements MatchRunner<Mapping>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.Mapping;
    }

    @Override
    public void run(Mapping mapping, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ProcessorSupport processorSupport = state.getProcessorSupport();

        ListIterable<? extends MappingInclude> includes = mapping._includes().toList();
        MutableList<SetImplementation> embeddedClassMappings = Lists.mutable.of();

        for (SetImplementation classMapping : mapping._classMappings())
        {
            if (classMapping instanceof EmbeddedSetImplementation)
            {
                embeddedClassMappings.add(classMapping);
            }
            matcher.fullMatch(classMapping, state);
        }

        for (SetImplementation embeddedClassMapping : embeddedClassMappings)
        {
            mapping._classMappings(((ImmutableList<SetImplementation>)Lists.immutable.withAll(mapping._classMappings())).newWithout(embeddedClassMapping));
        }

        for (EnumerationMapping enumerationMapping : mapping._enumerationMappings())
        {
            matcher.fullMatch(enumerationMapping, state);
        }

        for (AssociationImplementation associationMapping : mapping._associationMappings())
        {
            matcher.fullMatch(associationMapping, state);
        }

        for (MappingInclude include : includes)
        {
            this.unbindInclude(include, processorSupport);
        }
    }

    private void unbindInclude(MappingInclude include, ProcessorSupport processorSupport)
    {
        // Unbind reference to included mapping
        ImportStub includedMapping = (ImportStub)include._includedCoreInstance();
        Shared.cleanUpReferenceUsage(includedMapping, include, processorSupport);
        Shared.cleanImportStub(includedMapping, processorSupport);

        // Unbind store substitutions
        for (SubstituteStore storeSubstitution : include._storeSubstitutions())
        {
            this.unbindStoreSubstitution(storeSubstitution, processorSupport);
        }
    }

    private void unbindStoreSubstitution(SubstituteStore storeSubstitution, ProcessorSupport processorSupport)
    {
        // Remove reference to owner
        storeSubstitution._ownerRemove();

        // Unbind original
        ImportStub original = (ImportStub)storeSubstitution._originalCoreInstance();
        Shared.cleanUpReferenceUsage(original, storeSubstitution, processorSupport);
        Shared.cleanImportStub(original, processorSupport);

        // Unbind substitute
        ImportStub substitute = (ImportStub)storeSubstitution._substituteCoreInstance();
        Shared.cleanUpReferenceUsage(substitute, storeSubstitution, processorSupport);
        Shared.cleanImportStub(substitute, processorSupport);
    }
}
