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

package org.finos.legend.pure.m3.tools.matcher;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public abstract class MatcherState
{
    protected final ProcessorSupport processorSupport;
    private final MutableSet<CoreInstance> visited = UnifiedSet.newSet();

    protected MatcherState(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    /**
     * Note that this instance has been visited.  Returns
     * whether this is the first time the instance has been
     * visited.  That is, it returns true if the instance
     * has NOT been visited before.
     *
     * @param instance visited instance
     * @return whether this is the first time the instance has been visited
     */
    public boolean noteVisited(CoreInstance instance)
    {
        return this.visited.add(instance);
    }

    public void removeVisited(CoreInstance instance)
    {
        this.visited.remove(instance);
    }

    public ProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }

    public boolean mostGeneralRunnersFirst()
    {
        return true;
    }

    public abstract InlineDSLLibrary getInlineDSLLibrary();
}
