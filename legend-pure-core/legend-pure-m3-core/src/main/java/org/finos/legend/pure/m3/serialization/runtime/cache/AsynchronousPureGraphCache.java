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
import org.finos.legend.pure.m4.logs.PureLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsynchronousPureGraphCache implements PureGraphCache
{
    private final BlockingQueue<Runnable> queue;
    private final ExecutorService executor;
    private final AtomicBoolean isShutDown = new AtomicBoolean(false);
    private final PureLogger logger;
    protected final PureGraphCache delegate;

    private AsynchronousPureGraphCache(PureGraphCache delegate, PureLogger logger)
    {
        this.delegate = delegate;
        this.queue = new LinkedBlockingQueue<>();
        this.logger = logger;
        this.executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, this.queue, new MyThreadFactory());
    }

    @Override
    public void deleteCache()
    {
        logger.log("Delete cache request received");
        checkIsShutDown();
        submitDelete();
    }

    public void deleteCacheSynchronously() throws ExecutionException, InterruptedException
    {
        checkIsShutDown();
        submitDelete().get();
    }

    @Override
    public void setPureRuntime(PureRuntime pureRuntime)
    {
        checkIsShutDown();
        this.delegate.setPureRuntime(pureRuntime);
    }

    @Override
    public void cacheRepoAndSources()
    {
        logger.log("Create cache request received");
        checkIsShutDown();
        submitCacheRepoAndSources();
    }

    public void cacheRepoAndSourcesSynchronously() throws ExecutionException, InterruptedException
    {
        checkIsShutDown();
        submitCacheRepoAndSources().get();
    }

    @Override
    public boolean buildRepoAndSources(ModelRepository modelRepository, SourceRegistry sources, ParserLibrary library, Context context, ProcessorSupport processorSupport, Message message)
    {
        checkIsShutDown();
        return this.delegate.buildRepoAndSources(modelRepository, sources, library, context, processorSupport, message);
    }

    @Override
    public CacheState getCacheState()
    {
        checkIsShutDown();
        return this.delegate.getCacheState();
    }

    public PureGraphCache getDelegate()
    {
        return this.delegate;
    }

    public void clearQueue()
    {
        checkIsShutDown();
        this.queue.clear();
    }

    public void shutDown()
    {
        if (this.isShutDown.compareAndSet(false, true))
        {
            // TODO consider doing some clean-up if anything was running
            this.executor.shutdownNow();
        }
    }

    private Future<?> submitDelete()
    {
        return this.executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                logger.log("Start deleting graph cache");
                AsynchronousPureGraphCache.this.delegate.deleteCache();
                logger.log("Finished deleting graph cache");
            }
        });
    }

    private Future<?> submitCacheRepoAndSources()
    {
        return this.executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                logger.log("Start creating graph cache");
                AsynchronousPureGraphCache.this.delegate.cacheRepoAndSources();
                logger.log("Finished creating graph cache");
            }
        });
    }

    private void checkIsShutDown()
    {
        if (this.isShutDown.get())
        {
            throw new IllegalStateException("AsynchronousPureGraphCache has been shut down");
        }
    }

    public static AsynchronousPureGraphCache wrap(PureGraphCache cache, PureLogger logger)
    {
        return new AsynchronousPureGraphCache(cache, logger);
    }

    private static class MyThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }
}
