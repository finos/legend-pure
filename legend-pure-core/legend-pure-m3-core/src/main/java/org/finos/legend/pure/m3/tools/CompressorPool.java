// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m3.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A pool for {@link Deflater} and {@link Inflater} objects to minimize the cost of creating and cleaning up
 * these native-resource-backed objects. The pool provides {@link CloseableDeflater} and {@link CloseableInflater}
 * wrappers that implement {@link AutoCloseable}, allowing them to be used in try-with-resources blocks.
 * When closed, the wrappers are returned to the pool for reuse rather than having their native resources freed.
 *
 * <p>Idle pooled objects are automatically reaped after a configurable timeout (default: 1 minute). The reaper
 * thread is a daemon thread that terminates itself when the pool is empty and is recreated when items are
 * returned to the pool.</p>
 *
 * <p>The pool can be used as an instance or through a global singleton via {@link #getInstance()}.</p>
 */
public class CompressorPool
{
    private static final long DEFAULT_IDLE_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(1);
    private static final CompressorPool INSTANCE = new CompressorPool();

    private final Deque<CloseableDeflater> deflaterPool = new ArrayDeque<>();
    private final Deque<CloseableInflater> inflaterPool = new ArrayDeque<>();
    private final Deque<CloseableDeflater> nowrapDeflaterPool = new ArrayDeque<>();
    private final Deque<CloseableInflater> nowrapInflaterPool = new ArrayDeque<>();
    private final long idleTimeoutNanos;
    private volatile boolean shutdown;
    private Thread reaperThread;

    public CompressorPool()
    {
        this(DEFAULT_IDLE_TIMEOUT_NANOS);
    }

    /**
     * Create a new CompressorPool with the specified idle timeout.
     *
     * @param idleTimeout the idle timeout value
     * @param unit        the time unit of the idle timeout
     */
    public CompressorPool(long idleTimeout, TimeUnit unit)
    {
        this(unit.toNanos(idleTimeout));
    }

    private CompressorPool(long idleTimeoutNanos)
    {
        if (idleTimeoutNanos <= 0)
        {
            throw new IllegalArgumentException("Idle timeout must be positive");
        }
        this.idleTimeoutNanos = idleTimeoutNanos;
    }

    /**
     * Returns the global singleton instance.
     *
     * @return the singleton CompressorPool
     */
    public static CompressorPool getInstance()
    {
        return INSTANCE;
    }

    /**
     * Shut down this pool, cleaning up all currently held deflaters and inflaters. After shutdown,
     * attempts to borrow will throw {@link IllegalStateException}, and attempts to return will
     * simply clean up the native resources. The singleton pool cannot be shut down.
     *
     * @throws IllegalStateException if this is the singleton pool
     */
    public void shutdown()
    {
        if (this.shutdown)
        {
            return;
        }
        if (this == INSTANCE)
        {
            throw new IllegalStateException("Cannot shut down the singleton CompressorPool");
        }

        List<CloseableDeflater> deflatersToEnd;
        List<CloseableInflater> inflatersToEnd;
        Thread reaper;
        synchronized (this)
        {
            if (this.shutdown)
            {
                deflatersToEnd = Collections.emptyList();
                inflatersToEnd = Collections.emptyList();
                reaper = null;
            }
            else
            {
                this.shutdown = true;
                deflatersToEnd = new ArrayList<>(this.deflaterPool.size() + this.nowrapDeflaterPool.size());
                deflatersToEnd.addAll(this.deflaterPool);
                deflatersToEnd.addAll(this.nowrapDeflaterPool);
                inflatersToEnd = new ArrayList<>(this.inflaterPool.size() + this.nowrapInflaterPool.size());
                inflatersToEnd.addAll(this.inflaterPool);
                inflatersToEnd.addAll(this.nowrapInflaterPool);
                this.deflaterPool.clear();
                this.nowrapDeflaterPool.clear();
                this.inflaterPool.clear();
                this.nowrapInflaterPool.clear();
                reaper = this.reaperThread;
                this.reaperThread = null;
            }
        }
        deflatersToEnd.forEach(CloseableDeflater::doEnd);
        inflatersToEnd.forEach(CloseableInflater::doEnd);
        if (reaper != null)
        {
            reaper.interrupt();
        }
    }

    /**
     * Returns whether this pool has been shut down.
     *
     * @return true if this pool has been shut down
     */
    public boolean isShutdown()
    {
        return this.shutdown;
    }

    /**
     * Borrow a {@link CloseableDeflater} from the pool. The deflater will have the specified compression level
     * and will use the raw deflate format (nowrap=true). When closed, it is returned to the pool.
     *
     * @param level the compression level (0-9 or -1)
     * @return a closeable deflater
     * @throws IllegalStateException if this pool has been shut down
     */
    public CloseableDeflater borrowDeflater(int level)
    {
        return borrowDeflater(level, false);
    }

    /**
     * Borrow a {@link CloseableDeflater} from the pool. When closed, it is returned to the pool.
     *
     * @param level  the compression level (0-9 or -1)
     * @param nowrap if true, use raw deflate format (no zlib header/trailer)
     * @return a closeable deflater
     * @throws IllegalStateException if this pool has been shut down
     */
    public CloseableDeflater borrowDeflater(int level, boolean nowrap)
    {
        synchronized (this)
        {
            ensureNotShutdown();
            CloseableDeflater deflater = getDeflaterPool(nowrap).pollFirst();
            if (deflater != null)
            {
                deflater.setLevel(level);
                return deflater;
            }
        }
        return new CloseableDeflater(this, level, nowrap);
    }

    /**
     * Borrow a {@link CloseableInflater} from the pool. The inflater will use the raw deflate format (nowrap=true).
     * When closed, it is returned to the pool.
     *
     * @return a closeable inflater
     * @throws IllegalStateException if this pool has been shut down
     */
    public CloseableInflater borrowInflater()
    {
        return borrowInflater(false);
    }

    /**
     * Borrow a {@link CloseableInflater} from the pool. When closed, it is returned to the pool.
     *
     * @param nowrap if true, use raw deflate format (no zlib header/trailer)
     * @return a closeable inflater
     * @throws IllegalStateException if this pool has been shut down
     */
    public CloseableInflater borrowInflater(boolean nowrap)
    {
        synchronized (this)
        {
            ensureNotShutdown();
            CloseableInflater inflater = getInflaterPool(nowrap).pollFirst();
            if (inflater != null)
            {
                return inflater;
            }
        }
        return new CloseableInflater(this, nowrap);
    }

    private void ensureNotShutdown()
    {
        if (this.shutdown)
        {
            throw new IllegalStateException("CompressorPool has been shut down");
        }
    }

    private void returnDeflater(CloseableDeflater deflater, boolean nowrap)
    {
        if (this.shutdown)
        {
            deflater.doEnd();
            return;
        }
        try
        {
            deflater.reset();
        }
        catch (Exception ignore)
        {
            deflater.doEnd();
            return;
        }
        deflater.lastReturnedNanos = System.nanoTime();
        synchronized (this)
        {
            getDeflaterPool(nowrap).addFirst(deflater);
            ensureReaperRunning();
        }
    }

    private void returnInflater(CloseableInflater inflater, boolean nowrap)
    {
        if (this.shutdown)
        {
            inflater.doEnd();
            return;
        }
        try
        {
            inflater.reset();
        }
        catch (Exception ignore)
        {
            inflater.doEnd();
            return;
        }
        inflater.lastReturnedNanos = System.nanoTime();
        synchronized (this)
        {
            getInflaterPool(nowrap).addFirst(inflater);
            ensureReaperRunning();
        }
    }

    private Deque<CloseableDeflater> getDeflaterPool(boolean nowrap)
    {
        return nowrap ? this.nowrapDeflaterPool : this.deflaterPool;
    }

    private Deque<CloseableInflater> getInflaterPool(boolean nowrap)
    {
        return nowrap ? this.nowrapInflaterPool : this.inflaterPool;
    }

    // Must be called while synchronized on this
    private void ensureReaperRunning()
    {
        if (!this.shutdown && (this.reaperThread == null || !this.reaperThread.isAlive()))
        {
            Thread t = new Thread(this::reap, "CompressorPool-reaper");
            t.setDaemon(true);
            t.start();
            this.reaperThread = t;
        }
    }

    private void reap()
    {
        try
        {
            long sleepTimeMillis = Math.max(TimeUnit.NANOSECONDS.toMillis(this.idleTimeoutNanos), 1L);
            while (true)
            {
                Thread.sleep(sleepTimeMillis);

                List<CloseableDeflater> expiredDeflaters = new ArrayList<>();
                List<CloseableInflater> expiredInflaters = new ArrayList<>();
                boolean empty;
                synchronized (this)
                {
                    long now = System.nanoTime();
                    collectExpiredDeflaters(this.deflaterPool, now, expiredDeflaters);
                    collectExpiredDeflaters(this.nowrapDeflaterPool, now, expiredDeflaters);
                    collectExpiredInflaters(this.inflaterPool, now, expiredInflaters);
                    collectExpiredInflaters(this.nowrapInflaterPool, now, expiredInflaters);
                    empty = this.deflaterPool.isEmpty() && this.nowrapDeflaterPool.isEmpty() &&
                            this.inflaterPool.isEmpty() && this.nowrapInflaterPool.isEmpty();
                    if (empty)
                    {
                        this.reaperThread = null;
                    }
                }
                // End native resources outside the lock
                expiredDeflaters.forEach(CloseableDeflater::doEnd);
                expiredInflaters.forEach(CloseableInflater::doEnd);
                if (empty)
                {
                    return;
                }
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void collectExpiredDeflaters(Deque<CloseableDeflater> deque, long now, List<CloseableDeflater> expired)
    {
        while (!deque.isEmpty() && (now - deque.peekLast().lastReturnedNanos) >= this.idleTimeoutNanos)
        {
            expired.add(deque.pollLast());
        }
    }

    private void collectExpiredInflaters(Deque<CloseableInflater> deque, long now, List<CloseableInflater> expired)
    {
        while (!deque.isEmpty() && (now - deque.peekLast().lastReturnedNanos) >= this.idleTimeoutNanos)
        {
            expired.add(deque.pollLast());
        }
    }

    /**
     * A {@link Deflater} subclass that implements {@link AutoCloseable}. When closed, it is returned to its
     * originating {@link CompressorPool} for reuse instead of having its native resources freed.
     *
     * <p>Note: the explicit {@code implements AutoCloseable} is required for JDK versions prior to 25,
     * where {@link Deflater} does not implement {@link AutoCloseable}. On JDK 25+, it is redundant but harmless.</p>
     */
    public static class CloseableDeflater extends Deflater implements AutoCloseable
    {
        private final CompressorPool pool;
        private final boolean nowrap;
        private long lastReturnedNanos;

        private CloseableDeflater(CompressorPool pool, int level, boolean nowrap)
        {
            super(level, nowrap);
            this.pool = pool;
            this.nowrap = nowrap;
        }

        /**
         * Overridden to prevent external callers from freeing native resources on a pooled deflater.
         */
        @Override
        public void end()
        {
            // no-op: prevent external callers from ending a pooled deflater
        }

        @Override
        public void close()
        {
            this.pool.returnDeflater(this, this.nowrap);
        }

        private void doEnd()
        {
            super.end();
        }
    }

    /**
     * An {@link Inflater} subclass that implements {@link AutoCloseable}. When closed, it is returned to its
     * originating {@link CompressorPool} for reuse instead of having its native resources freed.
     *
     * <p>Note: the explicit {@code implements AutoCloseable} is required for JDK versions prior to 25,
     * where {@link Inflater} does not implement {@link AutoCloseable}. On JDK 25+, it is redundant but harmless.</p>
     */
    public static class CloseableInflater extends Inflater implements AutoCloseable
    {
        private final CompressorPool pool;
        private final boolean nowrap;
        private long lastReturnedNanos;

        private CloseableInflater(CompressorPool pool, boolean nowrap)
        {
            super(nowrap);
            this.pool = pool;
            this.nowrap = nowrap;
        }

        /**
         * Overridden to prevent external callers from freeing native resources on a pooled inflater.
         */
        @Override
        public void end()
        {
            // no-op: prevent external callers from ending a pooled inflater
        }

        @Override
        public void close()
        {
            this.pool.returnInflater(this, this.nowrap);
        }

        private void doEnd()
        {
            super.end();
        }
    }
}
