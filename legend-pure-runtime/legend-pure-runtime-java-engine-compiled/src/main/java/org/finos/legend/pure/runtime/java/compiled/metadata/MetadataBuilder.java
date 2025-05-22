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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;

@Deprecated
public class MetadataBuilder
{
    @Deprecated
    public static final CompileState SERIALIZED = CompileState.COMPILE_EVENT_EXTRA_STATE_1;

    private MetadataBuilder()
    {
    }

    @Deprecated
    public static MetadataEager indexAll(Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport)
    {
        return new MetadataEager(processorSupport);
    }

    @Deprecated
    public static MetadataEager indexAll(Iterable<? extends CoreInstance> startingNodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        return new MetadataEager(processorSupport);
    }

    @Deprecated
    public static MetadataEager indexNew(MetadataEager metadataEager, Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport)
    {
        return metadataEager;
    }

    @Deprecated
    public static MetadataEager indexNew(MetadataEager metadataEager, Iterable<? extends CoreInstance> startingNodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        return metadataEager;
    }
}
