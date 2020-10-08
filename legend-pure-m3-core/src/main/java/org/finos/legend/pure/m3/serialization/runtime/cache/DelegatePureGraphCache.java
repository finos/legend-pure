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

public class DelegatePureGraphCache implements PureGraphCache
{
    private final PureGraphCache delegate;
    private final DelegationBehavior deleteBehavior;
    private final DelegationBehavior cacheBehavior;
    private final DelegationBehavior buildBehavior;

    private DelegatePureGraphCache(PureGraphCache delegate, DelegationBehavior deleteBehavior, DelegationBehavior cacheBehavior, DelegationBehavior buildBehavior)
    {
        this.delegate = delegate;
        this.deleteBehavior = deleteBehavior;
        this.cacheBehavior = cacheBehavior;
        this.buildBehavior = buildBehavior;
    }

    @Override
    public void deleteCache()
    {
        switch (this.deleteBehavior)
        {
            case DELEGATE:
            {
                this.delegate.deleteCache();
                return;
            }
            case IGNORE:
            {
                return;
            }
            case THROW:
            {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public void setPureRuntime(PureRuntime pureRuntime)
    {
        this.delegate.setPureRuntime(pureRuntime);
    }

    @Override
    public void cacheRepoAndSources()
    {
        switch (this.cacheBehavior)
        {
            case DELEGATE:
            {
                this.delegate.cacheRepoAndSources();
                return;
            }
            case IGNORE:
            {
                return;
            }
            case THROW:
            {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public boolean buildRepoAndSources(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        switch (this.buildBehavior)
        {
            case DELEGATE:
            {
                return this.delegate.buildRepoAndSources(modelRepository, sources, library, context, processorSupport, message);
            }
            case IGNORE:
            {
                return false;
            }
            case THROW:
            {
                throw new UnsupportedOperationException();
            }
        }
        return false;
    }

    @Override
    public CacheState getCacheState()
    {
        return this.delegate.getCacheState();
    }

    public PureGraphCache getDelegate()
    {
        return this.delegate;
    }

    public static PureGraphCache wrap(PureGraphCache delegate, DelegationBehavior deleteBehavior, DelegationBehavior cacheBehavior, DelegationBehavior buildBehavior)
    {
        return new DelegatePureGraphCache(delegate, deleteBehavior, cacheBehavior, buildBehavior);
    }

    public static PureGraphCache wrapReadOnly(PureGraphCache delegate, boolean throwOnDelete, boolean throwOnCache)
    {
        DelegationBehavior deleteBehavior = throwOnDelete ? DelegationBehavior.THROW : DelegationBehavior.IGNORE;
        DelegationBehavior cacheBehavior = throwOnCache ? DelegationBehavior.THROW : DelegationBehavior.IGNORE;
        return wrap(delegate, deleteBehavior, cacheBehavior, DelegationBehavior.DELEGATE);
    }

    public enum DelegationBehavior
    {
        DELEGATE, IGNORE, THROW;
    }
}
