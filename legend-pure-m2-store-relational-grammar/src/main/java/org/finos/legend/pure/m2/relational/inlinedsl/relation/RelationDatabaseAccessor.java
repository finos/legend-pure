// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.inlinedsl.relation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m2.relational.inlinedsl.relation.processor.RelationDatabaseAccessorProcessor;
import org.finos.legend.pure.m2.relational.inlinedsl.relation.unloader.RelationDatabaseAccessorUnloader;
import org.finos.legend.pure.m2.relational.inlinedsl.relation.validation.RelationDatabaseAccessorValidation;
import org.finos.legend.pure.m2.relational.inlinedsl.relation.walker.RelationDatabaseAccessorWalker;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.M3CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.MilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class RelationDatabaseAccessor implements InlineDSL
{
    private static VisibilityValidator VISIBILITY_VALIDATOR = new RelationDatabaseAccessorValidation();

    @Override
    public String getName()
    {
        return "RelationAccessor";
    }

    @Override
    public boolean match(String code)
    {
        return code.startsWith(">");
    }

    @Override
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetX, int offsetY, ModelRepository modelRepository, Context context)
    {
        ProcessorSupport processorSupport = new M3ProcessorSupport(context, modelRepository);
        SourceInformation src = new SourceInformation(fileName, offsetX, offsetY, offsetX, offsetY + code.length());

        String info = code.trim().substring(1);
        String[] path = info.split("\\.");

        Class<?> relationType = (Class<?>) processorSupport.package_getByUserPath(M2RelationalPaths.RelationDatabaseAccessor);

        org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationDatabaseAccessor<?> rel = ((org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationDatabaseAccessor<?>) modelRepository.newEphemeralCoreInstance("", relationType, src));

        rel._path(Lists.mutable.with(path));

        return rel;
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.mutable.empty();
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.with(new RelationDatabaseAccessorProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.mutable.with(new RelationDatabaseAccessorWalker());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new RelationDatabaseAccessorUnloader());
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(M3CoreInstanceFactoryRegistry.REGISTRY);
    }

    @Override
    public VisibilityValidator getVisibilityValidator()
    {
        return VISIBILITY_VALIDATOR;
    }

    @Override
    public MilestoningDatesVarNamesExtractor getMilestoningDatesVarNamesExtractor()
    {
        return null;
    }
}
