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

abstract class AbstractPureGraphCache implements PureGraphCache
{
    private final CacheState cacheState = new CacheState(false, 0, true);
    protected PureRuntime pureRuntime;

    @Override
    public void deleteCache()
    {
        try
        {
            clearCaches();
            updateCacheState();
        }
        catch (RuntimeException|Error e)
        {
            updateCacheState(e);
            throw e;
        }
    }

    @Override
    public void setPureRuntime(PureRuntime pureRuntime)
    {
        this.pureRuntime = pureRuntime;
    }

    @Override
    public void cacheRepoAndSources()
    {
        try
        {
            clearCaches();
            this.cacheState.update(true, -1L, true, null);
            writeCaches();
            updateCacheState();
        }
        catch (RuntimeException|Error e)
        {
            try
            {
                clearCaches();
            }
            catch (Exception ee)
            {
                // Ignore
            }
            updateCacheState(e);
            throw e;
        }
    }

    @Override
    public boolean buildRepoAndSources(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        if (!cacheExists())
        {
            return false;
        }

        try
        {
            return buildFromCaches(modelRepository, sources, library, context, processorSupport, message);
        }
        catch (Exception e)
        {
            modelRepository.clear();
            updateCacheState(e);
            return false;
        }
        catch (Error e)
        {
            updateCacheState(e);
            throw e;
        }
    }

    @Override
    public CacheState getCacheState()
    {
        return this.cacheState;
    }

    protected abstract void clearCaches();

    protected abstract void writeCaches();

    protected abstract boolean buildFromCaches(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message) throws Exception;

    /**
     * Return whether the cache exists.  This should never
     * throw an exception, but should return false if there
     * is any issue in determining whether the cache exists.
     *
     * @return whether the cache exists
     */
    protected abstract boolean cacheExists();

    /**
     * Get the size of the cache.  If the cache does not
     * exist, this may return any value (but should not
     * throw an exception).  If the cache is not accessible,
     * this should return -1 (but should not throw an
     * exception).
     *
     * @return cache size
     */
    protected abstract long getCacheSize();

    protected void initializeCacheState()
    {
        initializeCacheState(null);
    }

    protected void initializeCacheState(Message message)
    {
        if (cacheExists())
        {
            updateCacheState();
            if (message != null)
            {
                message.setMessage(String.format("Cache found (size=%,dB)", getCacheSize()));
            }
        }
        else if (message != null)
        {
            message.setMessage("Cache not found...");
        }
    }

    protected void updateCacheState()
    {
        updateCacheState(true);
    }

    protected void updateCacheState(boolean lastOperationSuccessful)
    {
        updateCacheState(lastOperationSuccessful, null);
    }

    protected void updateCacheState(Throwable t)
    {
        updateCacheState((t == null), t);
    }

    protected void updateCacheState(boolean lastOperationSuccessful, Throwable t)
    {
        boolean exists = cacheExists();
        long size = exists ? getCacheSize() : -1L;
        this.cacheState.update(exists, size, lastOperationSuccessful, t);
    }
}
