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

package org.finos.legend.pure.m2.inlinedsl.store;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.dsl.store.M2StorePaths;
import org.finos.legend.pure.m2.inlinedsl.store.processor.RelationStoreAccessorProcessor;
import org.finos.legend.pure.m2.inlinedsl.store.unloader.RelationStoreAccessorUnloader;
import org.finos.legend.pure.m2.inlinedsl.store.validation.RelationStoreAccessorValidation;
import org.finos.legend.pure.m2.inlinedsl.store.walker.RelationStoreAccessorWalker;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.StoreCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.MilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public class RelationStoreAccessor implements InlineDSL
{
    private static VisibilityValidator VISIBILITY_VALIDATOR = new RelationStoreAccessorValidation();

    @Override
    public String getName()
    {
        return "RelationStoreAccessor";
    }

    @Override
    public boolean match(String code)
    {
        return code.startsWith(">");
    }

    @Override
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetX, int offsetY, ModelRepository modelRepository, Context context)
    {
        AntlrSourceInformation sourceInformation = new AntlrSourceInformation(offsetX, offsetY, fileName, true);

        ProcessorSupport processorSupport = new M3ProcessorSupport(context, modelRepository);
        SourceInformation src = new SourceInformation(fileName, offsetX, offsetY, offsetX, offsetY + code.length());
        String info = code.trim().substring(1).trim();
        String first = info.substring(0, 1);
        if (!"{".equals(first) && !"}".equals(info.substring(info.length() - 2, info.length() - 1)))
        {
            throw new PureParserException(sourceInformation.getPureSourceInformation(0, 0, 0, code.length()), "RelationStoreAccessor must be of the form #>{a::Store.table}#");
        }
        info = info.substring(1, info.length() - 1);

        String[] path = info.split("\\.");

        org.finos.legend.pure.m3.coreinstance.meta.pure.store.RelationStoreAccessor<?> rel = ((org.finos.legend.pure.m3.coreinstance.meta.pure.store.RelationStoreAccessor<?>) processorSupport.newAnonymousCoreInstance(src, M2StorePaths.RelationStoreAccessor));
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
        return Lists.immutable.with(new RelationStoreAccessorProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.mutable.with(new RelationStoreAccessorWalker());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new RelationStoreAccessorUnloader());
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(StoreCoreInstanceFactoryRegistry.REGISTRY);
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
