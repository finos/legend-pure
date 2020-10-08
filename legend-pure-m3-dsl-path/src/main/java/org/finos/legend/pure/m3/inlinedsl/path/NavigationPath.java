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

package org.finos.legend.pure.m3.inlinedsl.path;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.inlinedsl.path.parser.NavigationParser;
import org.finos.legend.pure.m3.inlinedsl.path.processor.PathProcessor;
import org.finos.legend.pure.m3.inlinedsl.path.unloader.PathUnbind;
import org.finos.legend.pure.m3.inlinedsl.path.validation.PathValidator;
import org.finos.legend.pure.m3.inlinedsl.path.walker.PathWalk;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.PathCoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public class NavigationPath implements InlineDSL
{
    private final ImmutableList<MatchRunner> processors = Lists.immutable.<MatchRunner>with(new PathProcessor());
    private final ImmutableList<MatchRunner> unbind = Lists.immutable.<MatchRunner>with(new PathUnbind());
    private final ImmutableList<MatchRunner> walk = Lists.immutable.<MatchRunner>with(new PathWalk());
    private final ImmutableList<MatchRunner> validators = Lists.immutable.<MatchRunner>with(new PathValidator());

    @Override
    public String getName()
    {
        return "Navigation";
    }

    @Override
    public boolean match(String code)
    {
        return code.startsWith("/");
    }

    @Override
    public CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetX, int offsetY, ModelRepository modelRepository, Context context)
    {
        return new NavigationParser().parse(code, importId, fileName, offsetX, offsetY, modelRepository, context);
    }

    @Override
    public RichIterable<MatchRunner> getValidators()
    {
        return this.validators;
    }

    @Override
    public RichIterable<MatchRunner> getProcessors()
    {
        return this.processors;
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadWalkers()
    {
        return this.walk;
    }

    @Override
    public RichIterable<MatchRunner> getUnLoadUnbinders()
    {
        return this.unbind;
    }

    @Override
    public RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry()
    {
        return Lists.immutable.with(PathCoreInstanceFactoryRegistry.REGISTRY);
    }
}
