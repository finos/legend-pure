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

package org.finos.legend.pure.m3.serialization.runtime.cache;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m4.ModelRepository;

public class VoidPureGraphCache implements PureGraphCache
{
    public static final VoidPureGraphCache VOID_PURE_GRAPH_CACHE = new VoidPureGraphCache();

    private final CacheState state = new CacheState(false, -1L, true);

    private VoidPureGraphCache()
    {
        // Singleton
    }

    @Override
    public void deleteCache()
    {
    }

    @Override
    public void cacheRepoAndSources()
    {
    }

    @Override
    public boolean buildRepoAndSources(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        return false;
    }

    @Override
    public void setPureRuntime(PureRuntime pureRuntime)
    {
    }

    @Override
    public CacheState getCacheState()
    {
        return this.state;
    }
}
