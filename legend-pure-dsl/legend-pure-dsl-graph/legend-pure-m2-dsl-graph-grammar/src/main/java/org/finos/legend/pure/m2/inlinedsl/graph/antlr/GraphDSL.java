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

package org.finos.legend.pure.m2.inlinedsl.graph.antlr;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.inlinedsl.graph.processor.RootGraphFetchTreeProcessor;
import org.finos.legend.pure.m2.inlinedsl.graph.unbinder.RootGraphFetchTreeUnbind;
import org.finos.legend.pure.m2.inlinedsl.graph.validator.RootGraphFetchTreeValidator;
import org.finos.legend.pure.m2.inlinedsl.graph.validator.RootGraphFetchTreeVisibilityValidator;
import org.finos.legend.pure.m2.inlinedsl.graph.walker.RootGraphFetchTreeUnloaderWalk;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.GraphCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.MilestoningDatesVarNamesExtractor;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.VisibilityValidator;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public class GraphDSL implements InlineDSL
{
    private static RootGraphFetchTreeVisibilityValidator ROOTGRAPHFETCHTREE_VISIBILITYVALIDATOR = new RootGraphFetchTreeVisibilityValidator();

    @Override
    public String getName()
    {
        return "Graph";
    }

    @Override
    public boolean match(String code)
    {
        return code.startsWith("{");
    }

    @Override
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetX, int offsetY, ModelRepository modelRepository, Context context)
    {
        return new GraphAntlrParser().parse(code, importId, fileName, offsetX, offsetY, modelRepository, context);
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return Lists.immutable.with(new RootGraphFetchTreeProcessor());
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return Lists.immutable.with(new RootGraphFetchTreeValidator());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return Lists.immutable.with(new RootGraphFetchTreeUnloaderWalk());
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return Lists.immutable.with(new RootGraphFetchTreeUnbind());
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(GraphCoreInstanceFactoryRegistry.REGISTRY);
    }

    @Override
    public VisibilityValidator getVisibilityValidator()
    {
        return ROOTGRAPHFETCHTREE_VISIBILITYVALIDATOR;
    }

    @Override
    public MilestoningDatesVarNamesExtractor getMilestoningDatesVarNamesExtractor()
    {
        return null;
    }
}
