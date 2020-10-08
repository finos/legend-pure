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

package org.finos.legend.pure.m3.compiler.unload;

import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public class Unbinder
{
    public static void process(SetIterable<? extends CoreInstance> consolidatedCoreInstances, ModelRepository modelRepository, ParserLibrary library, InlineDSLLibrary dslLibrary, Context context, ProcessorSupport processorSupport, UnbindState state, Message message)
    {
        Matcher unbindMatcher = new Matcher(modelRepository, context, processorSupport);

        for (MatchRunner unbinder : LazyIterate.concatenate(library.getParsers().asLazy().flatCollect(Parser.GET_UNLOAD_UNBINDERS), dslLibrary.getInlineDSLs().asLazy().flatCollect(InlineDSL.GET_UNLOAD_UNBINDERS)))
        {
            unbindMatcher.addMatchIfTypeIsKnown(unbinder);
        }

        int message_count = 0;
        for (CoreInstance instance : consolidatedCoreInstances)
        {
            message_count++;
            if (message != null)
            {
                message.setMessage("Unbinding (" + message_count + "/" + consolidatedCoreInstances.size() + ")");
            }
            unbindMatcher.match(instance, state);
            context.update(instance);
        }
    }
}
