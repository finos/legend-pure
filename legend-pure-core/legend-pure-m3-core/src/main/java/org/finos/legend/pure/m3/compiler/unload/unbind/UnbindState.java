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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class UnbindState extends MatcherState
{
    private final Context context;
    private final URLPatternLibrary URLPatternLibrary;

    private final InlineDSLLibrary inlineDSLLibrary;

    public UnbindState(Context context, URLPatternLibrary URLPatternLibrary, InlineDSLLibrary inlineDSLLibrary, ProcessorSupport processorSupport)
    {
        super(processorSupport);
        this.context = context;
        this.URLPatternLibrary = URLPatternLibrary;
        this.inlineDSLLibrary = inlineDSLLibrary;
    }

    @Override
    public boolean mostGeneralRunnersFirst()
    {
        // TODO Review whether this should return false.  Shouldn't unbinders be run in reverse order from post-processors?
        return true;
    }

    public URLPatternLibrary getURLPatternLibrary()
    {
        return this.URLPatternLibrary;
    }

    public void freeProcessedAndValidated(CoreInstance instance)
    {
        instance.markNotProcessed();
        this.context.update(instance);
    }

    public void freeValidated(CoreInstance instance)
    {
        instance.markNotValidated();
    }

    @Override
    public InlineDSLLibrary getInlineDSLLibrary()
    {
        return this.inlineDSLLibrary;
    }
}
