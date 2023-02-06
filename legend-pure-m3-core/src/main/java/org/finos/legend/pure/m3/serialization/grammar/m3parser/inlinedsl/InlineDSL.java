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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.serialization.grammar.CoreInstanceFactoriesRegistry;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface InlineDSL extends CoreInstanceFactoriesRegistry
{
    Function<InlineDSL, RichIterable<MatchRunner>> GET_PROCESSORS = InlineDSL::getProcessors;
    Function<InlineDSL, RichIterable<MatchRunner>> GET_UNLOAD_WALKERS = InlineDSL::getUnLoadWalkers;
    Function<InlineDSL, RichIterable<MatchRunner>> GET_UNLOAD_UNBINDERS = InlineDSL::getUnLoadUnbinders;

    String getName();
    boolean match(String code);
    CoreInstance parse(String code, ImportGroup importId, String fileName, int offsetX, int offsetY, ModelRepository modelRepository, Context context);

    RichIterable<MatchRunner> getProcessors();
    RichIterable<MatchRunner> getValidators();
    RichIterable<MatchRunner> getUnLoadWalkers();
    RichIterable<MatchRunner> getUnLoadUnbinders();

    VisibilityValidator getVisibilityValidator();

    MilestoningDatesVarNamesExtractor getMilestoningDatesVarNamesExtractor();

}
