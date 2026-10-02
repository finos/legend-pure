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

package org.finos.legend.pure.lsp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.lsp.mutation.SourceMutationService;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsParams;
import org.finos.legend.pure.lsp.protocol.ExecuteTestsResult;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LockContentionEvent;
import org.finos.legend.pure.lsp.protocol.PCTAdapterInfo;
import org.finos.legend.pure.lsp.protocol.TestEvent;
import org.finos.legend.pure.lsp.protocol.TestEventKind;
import org.finos.legend.pure.lsp.protocol.TestInvocation;
import org.finos.legend.pure.lsp.protocol.TestResult;
import org.finos.legend.pure.lsp.protocol.TestStatus;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.TestTools;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.MutableRuntimeOptions;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.mixed.LegendCompileMixedProcessorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendPureSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureSession.class);

    // Backstop against a lost task, not a per-test budget; raise via -Dlegend.lsp.testEntryTimeoutSeconds.
    private static final long ENTRY_WAIT_BACKSTOP_NANOS =
            java.util.concurrent.TimeUnit.SECONDS.toNanos(
                    Long.getLong("legend.lsp.testEntryTimeoutSeconds", 300L));
    private static final long ENTRY_WAIT_POLL_MILLIS = 1000L;
    private static final long DRAIN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(60L);

    private volatile PureRuntime pureRuntime;
    private volatile FunctionExecution functionExecution;
    private volatile boolean initialized;

    // Session-scoped and mutable, so legend/setOption can toggle options live without writing JVM system
    // properties. Created eagerly and never replaced, so toggles survive a runtime rebuild.
    private final MutableRuntimeOptions runtimeOptions = MutableRuntimeOptions.fromSystemProperties();

    private final SourceMutationService mutationService = new SourceMutationService(this);

    // In-flight legend/executeTests runs, keyed by runId, so legend/cancelTests and a client
    // disconnect can reach them.
    private final java.util.concurrent.ConcurrentMap<String, RunContext> activeRuns =
            new java.util.concurrent.ConcurrentHashMap<>();

    // Readers-writers lock protecting the compiled graph. Graph MUTATION (compile/reinitialize) is a
    // WRITER (exclusive); function EXECUTION is a READER (concurrent with other executions). This is
    // what guarantees "no mutation while an execution is in flight": a compile must acquire the write
    // lock, which cannot be granted while any execution holds a read lock, and vice versa. Fair mode
    // prevents a stream of executions from starving a pending compile (the auto-sync hook compiles
    // often). Replaces the old blanket `synchronized` that serialized everything. Declared as the
    // concrete ReentrantReadWriteLock (not the ReadWriteLock interface) so isWriteLocked()/
    // getReadLockCount()/getQueueLength() are available to describe contention in legend/lockContention.
    private final java.util.concurrent.locks.ReentrantReadWriteLock graphLock =
            new java.util.concurrent.locks.ReentrantReadWriteLock(true);

    // Count of threads currently blocked waiting to acquire the read/write side of graphLock. A
    // 0->1 transition means a caller just started waiting with nothing previously waiting - the
    // moment worth telling clients about; a 1->0 transition clears it. See acquireLock().
    private final AtomicInteger blockedReaders = new AtomicInteger();
    private final AtomicInteger blockedWriters = new AtomicInteger();

    // Most contention episodes clear within a few hundred ms (a routine auto-compile racing a hover/
    // definition request) and are not worth interrupting a client about. The active notification is
    // debounced behind this delay - see scheduleLockContentionNotification() - so only genuinely
    // long stalls get reported. Package-private (not final) so tests can shrink it instead of
    // sleeping for the real default.
    private static final long DEFAULT_LOCK_CONTENTION_NOTIFICATION_DELAY_MS = 5_000L;
    private long lockContentionNotificationDelayMs = DEFAULT_LOCK_CONTENTION_NOTIFICATION_DELAY_MS;

    private static final ScheduledExecutorService LOCK_CONTENTION_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r ->
    {
        Thread t = new Thread(r, "legend-pure-lsp-lock-contention-notifier");
        t.setDaemon(true);
        return t;
    });

    private final AtomicReference<ScheduledFuture<?>> pendingReadContentionNotification = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> pendingWriteContentionNotification = new AtomicReference<>();
    private final AtomicBoolean readContentionPublished = new AtomicBoolean(false);
    private final AtomicBoolean writeContentionPublished = new AtomicBoolean(false);

    // Tracks whether the graph might have an uncompiled mutation pending - see ensureCompiled().
    // Conservative by construction: SourceMutationService marks this true before attempting any
    // runtime mutation and clears it only once that same mutation's own compile() call actually
    // succeeds (markGraphDirty()/markGraphCompiled()). A caller that marks dirty but never confirms
    // compiled (an early "nothing to do" return, or a failure) just costs the next ensureCompiled()
    // one extra (harmless) write-lock round-trip - it can never incorrectly report clean.
    private final AtomicBoolean graphDirty = new AtomicBoolean(false);

    private volatile RepositoryScanner workspaceScanner;
    private volatile Set<String> classpathRepositoryNames = Collections.emptySet();
    private volatile java.util.function.Consumer<String> progressListener;
    private volatile LegendLanguageClient client;

    public void setProgressListener(java.util.function.Consumer<String> progressListener)
    {
        this.progressListener = progressListener;
    }

    /**
     * Wired by whoever owns this session's client connection(s) (see LegendPureLspServer/
     * PureRuntimeManager) so that lock-contention events (see acquireLock()) can be pushed to
     * connected clients the same way status/log/drift notifications already are.
     */
    public void setClient(LegendLanguageClient client)
    {
        this.client = client;
    }

    /**
     * Test seam for {@link #DEFAULT_LOCK_CONTENTION_NOTIFICATION_DELAY_MS} - lets tests exercise the
     * debounce without a real multi-second sleep.
     */
    void setLockContentionNotificationDelayMs(long delayMs)
    {
        this.lockContentionNotificationDelayMs = delayMs;
    }

    /**
     * Called by SourceMutationService before it attempts any runtime mutation, so a concurrent
     * caller's ensureCompiled() knows it can no longer take the fast (no-write-lock) path. Paired
     * with markGraphCompiled().
     */
    public void markGraphDirty()
    {
        this.graphDirty.set(true);
    }

    /**
     * Called by SourceMutationService once its own compile() call has actually succeeded (or once
     * it's confirmed nothing needed to change, e.g. an edit to an immutable source). Must NOT be
     * called from a failure path - see the class javadoc on graphDirty.
     */
    public void markGraphCompiled()
    {
        this.graphDirty.set(false);
    }

    public void initialize()
    {
        initialize(null);
    }

    public void initialize(RepositoryScanner scanner)
    {
        initialize(scanner, this.classpathRepositoryNames);
    }

    public void initialize(RepositoryScanner scanner, Collection<String> classpathRepositoryNames)
    {
        long start = System.currentTimeMillis();
        this.workspaceScanner = scanner;
        this.classpathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);

        this.pureRuntime = newRuntime(scanner, true, this.classpathRepositoryNames, Collections.emptySet(),
                false, Collections.emptySet(), this.progressListener, this.runtimeOptions);

        this.functionExecution = initializeFunctionExecution(
                new StackPreservingFunctionExecutionInterpreted(),
                this.pureRuntime,
                new Message(""));
        LspLog.info("StackPreservingFunctionExecutionInterpreted initialized");

        this.initialized = true;
        this.graphDirty.set(false);
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        LOGGER.info("Pure runtime initialized in {}s", elapsed);
    }

    public static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, Collections.emptySet());
    }

    public static PureRuntime newDebugRuntime(RepositoryScanner scanner, Collection<String> classpathRepositoryNames)
    {
        Set<String> normalizedClasspathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
        // excludedWorkspaceRepositoryNames must stay empty here: classpathRepositoryNames is only meant to
        // identify which repos are classpath-sourced, not to strip same-named repos out of the workspace
        // definition set. Passing it as both caused those repos to load wholesale from the classpath JAR
        // instead of the small scoped workspace directory, ballooning the debug compile far beyond what's
        // actually open (e.g. 2775 sources instead of ~261).
        return newRuntime(scanner, true, normalizedClasspathRepositoryNames, Collections.emptySet(),
                true, Collections.emptySet());
    }

    public static <T extends FunctionExecutionInterpreted> T initializeFunctionExecution(T functionExecution, PureRuntime runtime, Message message)
    {
        functionExecution.init(runtime, message);
        functionExecution.setProcessorSupport(new LegendCompileMixedProcessorSupport(
                runtime.getContext(),
                runtime.getModelRepository(),
                functionExecution.getProcessorSupport()));
        return functionExecution;
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, additionalWorkspaceDependencies, false);
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies, boolean workspaceDefinitionsOnly)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, additionalWorkspaceDependencies,
                workspaceDefinitionsOnly, Collections.emptySet());
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies, boolean workspaceDefinitionsOnly,
                                          Collection<String> excludedWorkspaceRepositoryNames)
    {
        return newRuntime(scanner, includeWorkspaceStorages, classpathRepositoryNames, additionalWorkspaceDependencies,
                workspaceDefinitionsOnly, excludedWorkspaceRepositoryNames, null, RuntimeOptions.defaultOptions());
    }

    private static PureRuntime newRuntime(RepositoryScanner scanner, boolean includeWorkspaceStorages, Collection<String> classpathRepositoryNames,
                                          Collection<String> additionalWorkspaceDependencies, boolean workspaceDefinitionsOnly,
                                          Collection<String> excludedWorkspaceRepositoryNames,
                                          java.util.function.Consumer<String> progressListener,
                                          RuntimeOptions options)
    {
        Set<String> normalizedClasspathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
        MutableList<RepositoryCodeStorage> storages = Lists.mutable.empty();
        Set<String> workspaceRepoNames = Collections.emptySet();

        if (includeWorkspaceStorages && scanner != null && !scanner.getMappings().isEmpty())
        {
            MutableList<RepositoryCodeStorage> workspaceStorages = workspaceDefinitionsOnly
                    ? scanner.buildWorkspaceDefinitionStorages(additionalWorkspaceDependencies, excludedWorkspaceRepositoryNames)
                    : scanner.buildWorkspaceStorages(additionalWorkspaceDependencies);
            storages.addAll(workspaceStorages);
            workspaceRepoNames = workspaceDefinitionsOnly
                    ? filteredWorkspaceRepoNames(scanner.getWorkspaceRepoNames(), excludedWorkspaceRepositoryNames)
                    : scanner.getWorkspaceRepoNames();
            LspLog.debug("Loaded " + workspaceStorages.size()
                    + (workspaceDefinitionsOnly
                    ? " workspace repo definition storage(s)"
                    : " workspace repos (overlay FS from disk)"));
        }

        org.eclipse.collections.api.RichIterable<CodeRepository> classpathRepos =
                CodeRepositoryProviderHelper.findCodeRepositories();
        Set<String> finalWorkspaceNames = workspaceRepoNames;
        Set<String> unresolvedClasspathRepositoryNames = new LinkedHashSet<>(normalizedClasspathRepositoryNames);
        Set<String> seenClasspathRepoNames = new LinkedHashSet<>();
        MutableList<CodeRepository> classpathStorageRepos = Lists.mutable.empty();
        for (CodeRepository repo : classpathRepos)
        {
            String name = repo.getName();
            if (shouldLoadClasspathRepository(name, finalWorkspaceNames, normalizedClasspathRepositoryNames))
            {
                if (name != null && !seenClasspathRepoNames.add(name))
                {
                    LspLog.debug("Skipping duplicate classpath repo: " + name);
                    continue;
                }
                classpathStorageRepos.add(repo);
                if (name != null)
                {
                    unresolvedClasspathRepositoryNames.remove(name);
                }
            }
            else if (name != null && finalWorkspaceNames.contains(name))
            {
                unresolvedClasspathRepositoryNames.remove(name);
                LspLog.debug("Classpath repo is loaded from workspace instead: " + name);
            }
        }
        if (!classpathStorageRepos.isEmpty())
        {
            storages.add(new ClassLoaderCodeStorage(classpathStorageRepos));
            LspLog.debug("Loaded " + classpathStorageRepos.size()
                    + " classpath repos (non-workspace)");
        }
        if (!unresolvedClasspathRepositoryNames.isEmpty())
        {
            LspLog.warn("Configured classpath repo(s) not found on runtime classpath: "
                    + unresolvedClasspathRepositoryNames);
        }

        LOGGER.info("Building PureRuntime with {} storage(s)...", storages.size());
        CompositeCodeStorage codeStorage = new CompositeCodeStorage(storages.toArray(new RepositoryCodeStorage[0]));

        PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                .withMessage(new Message(""))
                .setUseFastCompiler(true)
                .withOptions(options)
                .build();

        LOGGER.info("Initializing Pure runtime...");
        runtime.initialize(new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                super.setMessage(message);
                LOGGER.info(message);
                if (progressListener != null)
                {
                    progressListener.accept(message);
                }
            }
        });

        return runtime;
    }

    private static Set<String> filteredWorkspaceRepoNames(Set<String> workspaceRepositoryNames, Collection<String> excludedRepositoryNames)
    {
        if (workspaceRepositoryNames == null || workspaceRepositoryNames.isEmpty())
        {
            return Collections.emptySet();
        }
        if (excludedRepositoryNames == null || excludedRepositoryNames.isEmpty())
        {
            return workspaceRepositoryNames;
        }
        Set<String> filtered = new LinkedHashSet<>(workspaceRepositoryNames);
        filtered.removeAll(excludedRepositoryNames);
        return Collections.unmodifiableSet(filtered);
    }

    public void reinitialize()
    {
        try (LockHandle ignored = acquireGraphWriteLock())
        {
            // Deliberately do not clear pureRuntime/initialized up front: initialize() only
            // overwrites those fields once the new compile actually succeeds (each assignment
            // completes or not atomically), so a bad reindex/warm-up - e.g. a newly-scanned module
            // with one unparsable fixture file - leaves the previous, still-working session serving
            // requests instead of going permanently dark until a manual restart. Mirrors
            // SourceMutationService's restore-on-failure behavior for incremental edits.
            try
            {
                initialize(this.workspaceScanner, this.classpathRepositoryNames);
            }
            catch (Throwable e)
            {
                LspLog.warn("Reinitialize failed, keeping previous compiled session: " + e.getMessage());
                throw e;
            }
        }
    }

    public void setClasspathRepositoryNames(Collection<String> classpathRepositoryNames)
    {
        try (LockHandle ignored = acquireGraphWriteLock())
        {
            this.classpathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
        }
    }

    public SourceMutationService getMutationService()
    {
        return this.mutationService;
    }

    /**
     * The compiled graph is guarded by a single fair {@link java.util.concurrent.locks.ReadWriteLock}
     * ({@code graphLock}): every graph MUTATION (compile/reinitialize) takes {@link #acquireGraphWriteLock()}
     * (exclusive) and every graph READ - function execution AND the LSP providers (hover, completion,
     * references, ...) - takes {@link #acquireGraphReadLock()}. This single mechanism guarantees no mutation
     * runs while a read/execution is in flight, while still letting independent reads/executions run
     * concurrently. All production mutation paths funnel through {@link SourceMutationService}, which
     * acquires the write lock, so callers must NOT rely on the object monitor for graph exclusion.
     * <p>
     * Both accessors route through {@link #acquireLock(java.util.concurrent.locks.Lock, LockKind)} so
     * that a caller forced to actually wait (as opposed to acquiring immediately) is reflected in a
     * {@code legend/lockContention} notification to connected clients - see that method's javadoc.
     */
    public LockHandle acquireGraphReadLock()
    {
        return acquireLock(this.graphLock.readLock(), LockKind.READ);
    }

    public LockHandle acquireGraphWriteLock()
    {
        return acquireLock(this.graphLock.writeLock(), LockKind.WRITE);
    }

    /**
     * Test seam for asserting a run released the graph lock. Not for production callers: the read
     * lock is held continuously by routine hover/completion traffic, so this reads "held" almost
     * always - see {@link #tryAcquireGraphWriteLock(long)} for the usable form.
     */
    boolean isGraphLockHeld()
    {
        return this.graphLock.isWriteLocked() || this.graphLock.getReadLockCount() > 0;
    }

    /**
     * Bounded write-lock acquire for a caller (e.g. legend/syncWorkspace) that would rather report
     * "busy" than queue behind a paused SHARED debug session holding the lock indefinitely. A plain
     * snapshot of whether the lock is held would be useless here: the read lock is held continuously
     * by routine hover/completion traffic, so any such check reads "busy" during normal editing.
     *
     * @return a handle to release, or null if {@code timeoutMillis} elapsed first
     */
    public LockHandle tryAcquireGraphWriteLock(long timeoutMillis)
    {
        java.util.concurrent.locks.Lock lock = this.graphLock.writeLock();
        onBlockStart(LockKind.WRITE);
        try
        {
            return lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS) ? lock::unlock : null;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        finally
        {
            onBlockEnd(LockKind.WRITE);
        }
    }

    /**
     * Like {@link #lockContentionReason()}, but usable before a caller has actually started blocking.
     */
    public String graphLockHolderDescription()
    {
        if (this.graphLock.isWriteLocked())
        {
            return "write lock held (a compile is running)";
        }
        if (this.graphLock.getReadLockCount() > 0)
        {
            return "read lock(s) held (an execution or paused debug session is running)";
        }
        return null;
    }

    /**
     * Run a read-only graph access under the shared read lock, so a concurrent compile (write lock)
     * cannot mutate the graph mid-read. Convenience for the LSP read providers.
     */
    public <T> T withGraphReadLock(java.util.function.Supplier<T> action)
    {
        try (LockHandle ignored = acquireGraphReadLock())
        {
            return action.get();
        }
    }

    private enum LockKind
    {
        READ, WRITE
    }

    /**
     * A held graphLock permit; {@link #close()} releases it (equivalent to {@code Lock.unlock()}). A
     * plain method reference to {@code Lock::unlock} is enough to implement this, so callers get a
     * try-with-resources-friendly handle without any extra wrapper object.
     */
    public interface LockHandle extends AutoCloseable
    {
        @Override
        void close();
    }

    /**
     * Acquires {@code lock}, but only broadcasts a {@code legend/lockContention} notification if this
     * call actually has to wait for it (a caller granted the lock immediately produces no event, since
     * there is nothing to tell a client about). Concurrent waiters collapse to a single active/cleared
     * pair via {@link #blockedReaders}/{@link #blockedWriters}: the notification fires on the 0-&gt;1
     * transition (first waiter) and clears on the 1-&gt;0 transition (last waiter granted the lock), so
     * a burst of contending callers produces one "please wait" / one "cleared" pair rather than one
     * per thread. The active side of that pair is itself debounced - see scheduleLockContentionNotification().
     */
    private LockHandle acquireLock(java.util.concurrent.locks.Lock lock, LockKind kind)
    {
        if (!lock.tryLock())
        {
            onBlockStart(kind);
            lock.lock();
            onBlockEnd(kind);
        }
        return lock::unlock;
    }

    private void onBlockStart(LockKind kind)
    {
        AtomicInteger gauge = kind == LockKind.READ ? this.blockedReaders : this.blockedWriters;
        if (gauge.incrementAndGet() == 1)
        {
            scheduleLockContentionNotification(kind);
        }
    }

    private void onBlockEnd(LockKind kind)
    {
        AtomicInteger gauge = kind == LockKind.READ ? this.blockedReaders : this.blockedWriters;
        if (gauge.decrementAndGet() == 0)
        {
            ScheduledFuture<?> pending = pendingNotificationRef(kind).getAndSet(null);
            if (pending != null)
            {
                pending.cancel(false);
            }
            if (publishedFlag(kind).getAndSet(false))
            {
                publishLockContention(false, kind);
            }
        }
    }

    /**
     * Delays the "active" half of the notification pair by {@link #lockContentionNotificationDelayMs}:
     * most contention clears before the timer fires (a routine compile racing a hover/definition call),
     * in which case onBlockEnd() cancels this task and neither half of the pair is ever published - a
     * client only hears about contention that actually outlasts the debounce window.
     */
    private void scheduleLockContentionNotification(LockKind kind)
    {
        AtomicInteger gauge = kind == LockKind.READ ? this.blockedReaders : this.blockedWriters;
        ScheduledFuture<?> future = LOCK_CONTENTION_SCHEDULER.schedule(() ->
        {
            if (gauge.get() > 0)
            {
                publishedFlag(kind).set(true);
                publishLockContention(true, kind);
            }
        }, this.lockContentionNotificationDelayMs, TimeUnit.MILLISECONDS);
        pendingNotificationRef(kind).set(future);
    }

    private AtomicReference<ScheduledFuture<?>> pendingNotificationRef(LockKind kind)
    {
        return kind == LockKind.READ ? this.pendingReadContentionNotification : this.pendingWriteContentionNotification;
    }

    private AtomicBoolean publishedFlag(LockKind kind)
    {
        return kind == LockKind.READ ? this.readContentionPublished : this.writeContentionPublished;
    }

    private void publishLockContention(boolean active, LockKind kind)
    {
        LegendLanguageClient currentClient = this.client;
        if (currentClient == null)
        {
            return;
        }
        try
        {
            currentClient.lockContention(new LockContentionEvent(active, kind == LockKind.READ ? "read" : "write",
                    lockContentionReason(), this.graphLock.getQueueLength()));
        }
        catch (Exception e)
        {
            LOGGER.debug("Failed to publish lock contention notification", e);
        }
    }

    /**
     * Whether some caller is currently blocked waiting on either side of graphLock - folded into
     * legend/status (see LspStatus) so a client polling status, not just one live-streaming
     * legend/lockContention notifications, can also see it.
     */
    public boolean isLockContended()
    {
        return this.blockedReaders.get() > 0 || this.blockedWriters.get() > 0;
    }

    public String lockContentionReason()
    {
        if (!isLockContended())
        {
            return null;
        }
        return this.graphLock.isWriteLocked()
                ? "write lock held (a compile is running)"
                : "read lock(s) held (an execution is running)";
    }

    /**
     * Ids of runs currently in {@link #executeTests} - what {@link #cancelTests} can target.
     */
    public List<String> activeRunIds()
    {
        return Lists.mutable.withAll(this.activeRuns.keySet());
    }

    public String lockHeldBy()
    {
        if (this.graphLock.isWriteLocked())
        {
            return "write";
        }
        return this.graphLock.getReadLockCount() > 0 ? "read" : null;
    }

    // These delegate straight to SourceMutationService, which takes the write lock itself (the single
    // mutation chokepoint) - so no extra locking here. Retained as public API for tests/back-compat.
    public CompileResult restoreFromDisk(String sourceId)
    {
        return this.mutationService.restoreFromDisk(sourceId);
    }

    public CompileResult modifyAndCompile(String sourceId, String content)
    {
        return this.mutationService.modifyAndCompile(sourceId, content);
    }

    public CompileResult applyBulkChangesAndCompile(List<FileChange> changes)
    {
        return this.mutationService.applyBulkChangesAndCompile(changes);
    }

    /**
     * Compile any pending (uncompiled) sources, taking the WRITE lock only when {@link #graphDirty}
     * says there is something to do. Callers must invoke this from a request thread, never from the
     * execution pool: a pool thread blocked here cannot be freed until the test walker releases its
     * read lock, and the walker needs that same pool to finish.
     */
    void ensureCompiled()
    {
        if (!this.graphDirty.get())
        {
            return;
        }
        try (LockHandle ignored = acquireGraphWriteLock())
        {
            if (this.graphDirty.compareAndSet(true, false) && this.pureRuntime != null)
            {
                this.pureRuntime.compile();
            }
        }
    }

    public ExecuteResult executeGo()
    {
        FunctionCandidates candidates = new FunctionCandidates(
                Lists.mutable.with("go():Any[*]", "go():String[*]", "go():String[1]"),
                Lists.mutable.with("go__Any_MANY_", "go__String_MANY_", "go__String_1_"));
        return executeFunctionByCandidates(candidates, "go()",
                "No go() function found in compiled sources. Define: function go():Any[*] { ... }",
                null, null, null, null);
    }

    /**
     * Execute an arbitrary zero-argument function by its Pure path. Accepts either a functional
     * signature form ("pkg::foo():Any[*]") or the mangled core-instance id ("pkg::foo__Any_MANY_").
     * CONCURRENCY: this is a READER - it takes the read lock, so multiple executeFunction/executeGo
     * calls run in parallel (safe: the compiled graph is read-only during execution, each call gets
     * its OWN FunctionExecutionInterpreted instance (own console, own cancel flag) and its OWN
     * thread-local ModelRepositoryTransaction, rolled back at the end to isolate/reclaim transient
     * instances). A concurrent compile (writer) is blocked until all in-flight executions release the
     * read lock, and cannot interleave mid-execution.
     */
    public ExecuteResult executeFunction(String functionPath)
    {
        return executeFunction(functionPath, null);
    }

    /**
     * Same as {@link #executeFunction(String)}, but for a &lt;&lt;PCT.test&gt;&gt; function that takes
     * exactly one parameter: the adapter {@code Function} resolved from {@code pctAdapterPath} (e.g.
     * an in-memory vs. a real relational execution strategy). Mirrors the substitution
     * {@code TestRunner#executeTestFunc} already performs for bulk PCT runs
     * (org.finos.legend.pure.m3.execution.test.TestRunner in legend-pure-core) - resolve the adapter
     * by path and pass it as the sole wrapped argument - without pulling in that class's
     * TestCollection/TestCallBack machinery, which is built for running a whole suite rather than one
     * function invoked from an IDE gutter icon. A null/blank pctAdapterPath behaves exactly like
     * {@link #executeFunction(String)} (zero-argument call).
     */
    public ExecuteResult executeFunction(String functionPath, String pctAdapterPath)
    {
        return executeFunction(functionPath, pctAdapterPath, null, null);
    }

    /**
     * Same as {@link #executeFunction(String, String)}, but additionally runs {@code beforeFunctionPath}
     * (if set) immediately before, and {@code afterFunctionPath} (if set) immediately after, the target
     * function - all three within the same read-lock/transaction/console-capture as a single atomic
     * call. Mirrors {@code TestRunner#runTestsFromCollection}'s bracketing semantics at single-test
     * granularity: a before-function failure aborts the whole call (the target function and after are
     * never run); an after-function failure is appended to the output but never flips an otherwise
     * successful target-function result to failure. Either path may be null/blank to skip that step.
     */
    public ExecuteResult executeFunction(String functionPath, String pctAdapterPath, String beforeFunctionPath, String afterFunctionPath)
    {
        return executeFunction(functionPath, pctAdapterPath, beforeFunctionPath, afterFunctionPath, null);
    }

    /**
     * Same as {@link #executeFunction(String, String, String, String)}, but binding {@code arguments}
     * as the target function's {@code String[1]} parameters. Mutually exclusive with {@code pctAdapterPath}.
     */
    public ExecuteResult executeFunction(String functionPath, String pctAdapterPath, String beforeFunctionPath,
                                         String afterFunctionPath, List<String> arguments)
    {
        if (functionPath == null || functionPath.trim().isEmpty())
        {
            return new ExecuteResult(false, "Function path is required", null);
        }
        if (arguments != null && !arguments.isEmpty() && blankToNull(pctAdapterPath) != null)
        {
            return new ExecuteResult(false, "Pass either arguments or a pctAdapterPath, not both", null);
        }
        String path = functionPath.trim();
        return executeFunctionByCandidates(buildFunctionCandidates(path), path,
                "No function '" + path + "' found in compiled sources.", pctAdapterPath,
                blankToNull(beforeFunctionPath), blankToNull(afterFunctionPath), arguments);
    }

    /**
     * Discovers and runs every test in a package or a file - the batch counterpart to
     * {@link #executeFunction(String)}. Mirrors {@code PureTestBuilder#buildSuite}'s bracketing
     * (per-subpackage before/after hooks, including inherited ones) so a run here matches an
     * equivalent surefire run. Runs under a single read lock for the whole batch; {@code parallel}
     * fans out only the tests within one node - sibling subtrees routinely share state (e.g. H2
     * tables from a before hook) so the tree walk itself stays serial.
     *
     * @param resolvedSourceId when non-null, scope is that single file; otherwise
     *                         {@code params.getPackagePath()} is used.
     */
    public ExecuteTestsResult executeTests(ExecuteTestsParams params, String resolvedSourceId)
    {
        if (!this.initialized)
        {
            return ExecuteTestsResult.failure("Runtime not initialized");
        }
        boolean explicitInvocations = params.getInvocations() != null && !params.getInvocations().isEmpty();
        boolean explicitFunctions = explicitInvocations
                || (params.getFunctions() != null && !params.getFunctions().isEmpty());
        if (resolvedSourceId == null && blankToNull(params.getPackagePath()) == null && !explicitFunctions)
        {
            return ExecuteTestsResult.failure("One of a package path, a file uri or an explicit function list is required");
        }

        ensureCompiled();
        long startedAt = System.currentTimeMillis();

        try (LockHandle ignored = acquireGraphReadLock())
        {
            PureRuntime runtime = this.pureRuntime;
            if (runtime == null)
            {
                return ExecuteTestsResult.failure("Runtime not initialized");
            }

            int explicitCount = explicitInvocations ? params.getInvocations().size()
                    : (explicitFunctions ? params.getFunctions().size() : 0);
            String scope = explicitFunctions
                    ? explicitCount + " function(s)"
                    : (resolvedSourceId != null ? resolvedSourceId : params.getPackagePath().trim());
            TestDiscovery.Node root;
            try
            {
                if (explicitFunctions)
                {
                    // An unresolvable path is a caller bug, not a test outcome - fail the whole run
                    // rather than silently reporting it as a failed entry.
                    MutableList<TestDiscovery.Invocation> resolved = Lists.mutable.empty();
                    MutableList<String> unresolved = Lists.mutable.empty();
                    for (TestInvocation invocation : asInvocations(params))
                    {
                        String trimmed = invocation.getPath() == null ? "" : invocation.getPath().trim();
                        CoreInstance fn = trimmed.isEmpty() ? null : resolveFunction(runtime, buildFunctionCandidates(trimmed));
                        if (fn == null)
                        {
                            unresolved.add(trimmed);
                        }
                        else
                        {
                            resolved.add(new TestDiscovery.Invocation(fn, invocation.getArguments(),
                                    blankToNull(invocation.getLabel())));
                        }
                    }
                    if (unresolved.notEmpty())
                    {
                        return ExecuteTestsResult.failure("No function found in compiled sources: "
                                + unresolved.makeString(", "));
                    }
                    root = TestDiscovery.flat(scope, resolved);
                }
                else
                {
                    root = resolvedSourceId != null
                            ? TestDiscovery.forSource(runtime, resolvedSourceId, params.isIncludeVanilla(), params.isIncludePct())
                            : TestDiscovery.forPackage(runtime, scope, params.isRecursive(),
                                    params.isIncludeVanilla(), params.isIncludePct());
                }
            }
            catch (IllegalArgumentException e)
            {
                return ExecuteTestsResult.failure(e.getMessage());
            }

            if (root.isEmpty())
            {
                return emptyRun(params.getRunId(), scope, startedAt);
            }

            ProcessorSupport processorSupport = runtime.getProcessorSupport();
            String adapterPath = blankToNull(params.getPctAdapterPath());
            CoreInstance adapter = null;
            if (adapterPath != null)
            {
                adapter = resolvePctAdapter(runtime, processorSupport, adapterPath);
                if (adapter == null)
                {
                    return ExecuteTestsResult.failure(unknownAdapterMessage(runtime, adapterPath));
                }
            }
            else if (hasAnyTest(root) && onlyPCTTests(root, processorSupport))
            {
                // Nothing runnable without an adapter - fail loudly rather than report an all-skipped run.
                return ExecuteTestsResult.failure("Scope '" + scope + "' contains only PCT tests, which require an "
                        + "adapter. Pass a pctAdapterPath - see legend/getPCTAdapters for the choices.");
            }

            // Generate an id when the caller didn't supply one, so every run is cancellable.
            String runId = blankToNull(params.getRunId()) != null
                    ? params.getRunId().trim()
                    : java.util.UUID.randomUUID().toString();
            RunContext context = new RunContext(runtime, processorSupport, runId, adapter, adapterPath,
                    params.isParallel(), LegendPureLspServer.executionPool());
            int total = root.totalEntries();
            registerRun(context);
            notifyTestEvent(TestEvent.runStarted(context.runId, scope, total));
            try
            {
                walkNode(root, context);
            }
            finally
            {
                // Inside the read lock by construction: see RunContext#drain.
                context.drain(DRAIN_TIMEOUT_NANOS);
                unregisterRun(context);
                notifyTestEvent(TestEvent.runFinished(context.runId, scope, total));
            }

            ExecuteTestsResult result = summarize(context.results, runId, scope, startedAt);
            if (context.isCancelled())
            {
                result.setCancelled(true);
                result.setSuccess(false);
                result.setError("Run cancelled after " + result.getTests().size() + " of " + total + " entries");
            }
            return result;
        }
    }

    /**
     * {@code functions} is the shorthand for {@code invocations} with no arguments and no label.
     */
    private static List<TestInvocation> asInvocations(ExecuteTestsParams params)
    {
        if (params.getInvocations() != null && !params.getInvocations().isEmpty())
        {
            return params.getInvocations();
        }
        MutableList<TestInvocation> invocations = Lists.mutable.empty();
        for (String path : params.getFunctions())
        {
            TestInvocation invocation = new TestInvocation();
            invocation.setPath(path);
            invocations.add(invocation);
        }
        return invocations;
    }

    /**
     * Cancels an in-flight {@link #executeTests} run. Deliberately takes no lock - the run it's
     * cancelling holds the fair graphLock for its whole duration, so a cancel that waited on it could
     * queue behind the very run it's meant to stop.
     *
     * @param runId the run to cancel, or null together with {@code all} to cancel every run
     * @return the ids actually cancelled (empty if nothing matched)
     */
    public List<String> cancelTests(String runId, boolean all)
    {
        MutableList<String> cancelled = Lists.mutable.empty();
        for (RunContext run : this.activeRuns.values())
        {
            if (all || run.runId.equals(runId))
            {
                run.cancel();
                cancelled.add(run.runId);
            }
        }
        if (cancelled.notEmpty())
        {
            LspLog.info("cancelTests: cancelled " + cancelled.size() + " run(s): " + cancelled.makeString(", "));
        }
        return cancelled;
    }

    /**
     * Cancels every in-flight run because the last client went away - otherwise an abandoned run
     * keeps the graph read lock, blocking every later compile with nobody waiting on the result.
     * Keyed on "no clients left" rather than run ownership, since lsp4j gives each connection its own
     * Launcher with no cheap way to attribute a request handler to one.
     */
    public void onLastClientDisconnected()
    {
        if (this.activeRuns.isEmpty())
        {
            return;
        }
        LspLog.info("last client disconnected - cancelling " + this.activeRuns.size() + " in-flight test run(s)");
        cancelTests(null, true);
    }

    private void registerRun(RunContext context)
    {
        if (context.runId != null)
        {
            this.activeRuns.put(context.runId, context);
        }
    }

    private void unregisterRun(RunContext context)
    {
        if (context.runId != null)
        {
            this.activeRuns.remove(context.runId, context);
        }
    }

    private static ExecuteTestsResult emptyRun(String runId, String scope, long startedAt)
    {
        ExecuteTestsResult result = new ExecuteTestsResult();
        result.setSuccess(true);
        result.setRunId(runId);
        result.setScope(scope);
        result.setTests(Lists.mutable.empty());
        result.setDurationMs(System.currentTimeMillis() - startedAt);
        return result;
    }

    private static ExecuteTestsResult summarize(List<TestResult> results, String runId, String scope, long startedAt)
    {
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        for (TestResult entry : results)
        {
            if (TestStatus.PASSED.getProtocolValue().equals(entry.getStatus()))
            {
                passed++;
            }
            else if (TestStatus.FAILED.getProtocolValue().equals(entry.getStatus()))
            {
                failed++;
            }
            else
            {
                skipped++;
            }
        }
        ExecuteTestsResult result = new ExecuteTestsResult();
        result.setSuccess(failed == 0);
        result.setRunId(runId);
        result.setScope(scope);
        result.setTests(results);
        result.setTotal(results.size());
        result.setPassed(passed);
        result.setFailed(failed);
        result.setSkipped(skipped);
        result.setDurationMs(System.currentTimeMillis() - startedAt);
        return result;
    }

    /**
     * Resolves a {@code <<PCT.adapter>>} by path. A direct {@code _Package.getByUserPath} only
     * matches the mangled function id, which nobody actually types, so this falls back to matching
     * the plain path or simple name against the adapter list. Ambiguity is left unresolved rather
     * than guessed at - see {@link #unknownAdapterMessage}.
     */
    private static CoreInstance resolvePctAdapter(PureRuntime runtime, ProcessorSupport processorSupport, String adapterPath)
    {
        CoreInstance direct = _Package.getByUserPath(adapterPath, processorSupport);
        if (direct != null)
        {
            return direct;
        }
        List<PCTAdapterInfo> matches = matchingAdapters(runtime, adapterPath);
        if (matches.size() != 1)
        {
            return null;
        }
        return _Package.getByUserPath(matches.get(0).getPath(), processorSupport);
    }

    private static List<PCTAdapterInfo> matchingAdapters(PureRuntime runtime, String adapterPath)
    {
        List<PCTAdapterInfo> matches = Lists.mutable.empty();
        for (PCTAdapterInfo candidate : PCTAdapterProvider.getPCTAdapters(runtime))
        {
            String path = candidate.getPath();
            // Accept the mangled id, the plain path, the bare function name, or the display name -
            // whatever a caller copied out of unknownAdapterMessage's list.
            if (adapterPath.equals(path)
                    || (path != null && path.startsWith(adapterPath + "_"))
                    || adapterPath.equals(simpleAdapterName(path))
                    || adapterPath.equalsIgnoreCase(candidate.getName()))
            {
                matches.add(candidate);
            }
        }
        return matches;
    }

    /** "a::b::testAdapterForX_Function_1__X_o_" -> "testAdapterForX". */
    private static String simpleAdapterName(String path)
    {
        if (path == null)
        {
            return null;
        }
        String tail = path.substring(path.lastIndexOf("::") + 2);
        int signature = tail.indexOf("_Function_");
        return signature < 0 ? tail : tail.substring(0, signature);
    }

    private static String unknownAdapterMessage(PureRuntime runtime, String adapterPath)
    {
        List<PCTAdapterInfo> matches = matchingAdapters(runtime, adapterPath);
        if (matches.size() > 1)
        {
            return "PCT adapter '" + adapterPath + "' is ambiguous - matches: " + describeAdapters(matches);
        }
        List<PCTAdapterInfo> all = PCTAdapterProvider.getPCTAdapters(runtime);
        if (all.isEmpty())
        {
            return "PCT adapter '" + adapterPath + "' not found, and no <<PCT.adapter>> functions are "
                    + "loaded in this session at all.";
        }
        return "PCT adapter '" + adapterPath + "' not found in compiled sources. Available: " + describeAdapters(all);
    }

    private static String describeAdapters(List<PCTAdapterInfo> adapters)
    {
        StringBuilder builder = new StringBuilder();
        for (PCTAdapterInfo adapter : adapters)
        {
            if (builder.length() > 0)
            {
                builder.append(", ");
            }
            builder.append(adapter.getName()).append(" (").append(adapter.getPath()).append(")");
        }
        return builder.toString();
    }

    /**
     * Guards {@link #onlyPCTTests}, which is vacuously true for a scope whose entries are all hooks
     * or all {@code <<test.ToFix>>}.
     */
    private static boolean hasAnyTest(TestDiscovery.Node node)
    {
        if (!node.getTests().isEmpty())
        {
            return true;
        }
        for (TestDiscovery.Node child : node.getChildren())
        {
            if (hasAnyTest(child))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean onlyPCTTests(TestDiscovery.Node node, ProcessorSupport processorSupport)
    {
        for (TestDiscovery.Invocation test : node.getTests())
        {
            if (!PCTTools.isPCTTest(test.getFunction(), processorSupport))
            {
                return false;
            }
        }
        for (TestDiscovery.Node child : node.getChildren())
        {
            if (!onlyPCTTests(child, processorSupport))
            {
                return false;
            }
        }
        return true;
    }

    // before hooks -> subpackage nodes -> own tests -> after hooks. See PureTestBuilder#buildSuite.
    private void walkNode(TestDiscovery.Node node, RunContext context)
    {
        if (context.isCancelled())
        {
            return;
        }
        notifyTestEvent(TestEvent.suite(context.runId, TestEventKind.SUITE_STARTED, node.getPackagePath()));
        try
        {
            for (CoreInstance hook : node.getBeforeFunctions())
            {
                context.results.add(runEntry(TestDiscovery.Invocation.of(hook), node, context, "before"));
            }
            for (TestDiscovery.Node child : node.getChildren())
            {
                walkNode(child, context);
            }
            runNodeTests(node, context);
            // After-hooks below still run on cancellation: a cancelled run must not leave a package's
            // <<test.AfterPackage>> teardown unexecuted, or it would strand shared H2 tables and break
            // the NEXT run. Skipped entries are not worth reporting once cancelled, though.
            for (CoreInstance skipped : context.isCancelled() ? Collections.<CoreInstance>emptyList() : node.getSkipped())
            {
                TestResult result = new TestResult(pathOf(skipped), nameOf(skipped), node.getPackagePath(),
                        TestStatus.SKIPPED, 0L, "Skipped: carries the <<test.ToFix>> stereotype", null, null, false, null);
                context.results.add(result);
                notifyTestEvent(TestEvent.test(context.runId, TestEventKind.TEST_FINISHED, result));
            }
            for (CoreInstance hook : node.getAfterFunctions())
            {
                context.results.add(runEntry(TestDiscovery.Invocation.of(hook), node, context, "after"));
            }
        }
        finally
        {
            notifyTestEvent(TestEvent.suite(context.runId, TestEventKind.SUITE_FINISHED, node.getPackagePath()));
        }
    }

    private void runNodeTests(TestDiscovery.Node node, RunContext context)
    {
        List<TestDiscovery.Invocation> tests = node.getTests();
        if (tests.isEmpty())
        {
            return;
        }
        if (!context.parallel || tests.size() == 1)
        {
            // On the execution pool, not inline: running on this thread would sidestep the
            // per-JVM execution bound.
            for (TestDiscovery.Invocation test : tests)
            {
                context.results.add(awaitEntry(context.submit(() -> runEntry(test, node, context, null)),
                        context, test, node));
            }
            return;
        }

        // Results are collected positionally so the reported order stays deterministic (and matches
        // the serial run) even though completion order is not.
        List<java.util.concurrent.Future<TestResult>> futures = new java.util.ArrayList<>(tests.size());
        for (TestDiscovery.Invocation test : tests)
        {
            try
            {
                futures.add(context.submit(() -> runEntry(test, node, context, null)));
            }
            catch (java.util.concurrent.RejectedExecutionException e)
            {
                futures.add(java.util.concurrent.CompletableFuture.completedFuture(
                        cancelledResult(test, node)));
            }
        }
        for (int i = 0; i < futures.size(); i++)
        {
            context.results.add(awaitEntry(futures.get(i), context, tests.get(i), node));
        }
    }

    /**
     * Waits for one entry, bounded: this thread holds the graph read lock for the whole run, so an
     * unbounded wait would wedge every later compile. Polls rather than a single {@code get(timeout)}
     * since a task can sit queued a legitimately long time - the deadline is a backstop against a
     * lost task, not a per-test budget.
     */
    private TestResult awaitEntry(java.util.concurrent.Future<TestResult> future, RunContext context,
                                  TestDiscovery.Invocation test, TestDiscovery.Node node)
    {
        long deadline = System.nanoTime() + ENTRY_WAIT_BACKSTOP_NANOS;
        while (true)
        {
            try
            {
                return future.get(ENTRY_WAIT_POLL_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            catch (java.util.concurrent.TimeoutException e)
            {
                if (context.isCancelled())
                {
                    future.cancel(false);
                    return abandonEntry(context, test, cancelledResult(test, node));
                }
                if (System.nanoTime() - deadline > 0)
                {
                    future.cancel(false);
                    LOGGER.error("executeTests: gave up waiting for " + pathOf(test.getFunction())
                            + " after " + TimeUnit.NANOSECONDS.toSeconds(ENTRY_WAIT_BACKSTOP_NANOS) + "s");
                    return abandonEntry(context, test,
                            errorResult(test, node, "Timed out waiting for the test to complete"));
                }
            }
            catch (java.util.concurrent.CancellationException e)
            {
                return abandonEntry(context, test, cancelledResult(test, node));
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return abandonEntry(context, test,
                        errorResult(test, node, "Interrupted while awaiting test completion"));
            }
            catch (java.util.concurrent.ExecutionException e)
            {
                LOGGER.error("executeTests: unexpected failure running " + pathOf(test.getFunction()), e.getCause());
                return abandonEntry(context, test, errorResult(test, node,
                        "Unexpected failure: " + (e.getCause() == null ? e.toString() : e.getCause().toString())));
            }
        }
    }

    /**
     * Reports an entry the walker gave up waiting on. TEST_STARTED was already emitted by the pool
     * task, so without this the client's tree keeps that entry spinning forever.
     */
    private TestResult abandonEntry(RunContext context, TestDiscovery.Invocation invocation, TestResult result)
    {
        finishEntry(context, invocation, result);
        return result;
    }

    private TestResult cancelledResult(TestDiscovery.Invocation invocation, TestDiscovery.Node node)
    {
        return new TestResult(pathOf(invocation.getFunction()), nameOf(invocation.getFunction()),
                node.getPackagePath(), TestStatus.SKIPPED, 0L, "Skipped: run was cancelled", null, null, false, null);
    }

    private TestResult runEntry(TestDiscovery.Invocation invocation, TestDiscovery.Node node, RunContext context, String hookKind)
    {
        CoreInstance function = invocation.getFunction();
        String path = pathOf(function);
        String name = invocation.getLabel() == null ? nameOf(function) : invocation.getLabel();
        boolean isPct = PCTTools.isPCTTest(function, context.processorSupport);

        if (isPct && context.adapter == null)
        {
            TestResult skippedResult = new TestResult(path, name, node.getPackagePath(), TestStatus.SKIPPED, 0L,
                    "Skipped: PCT test requires an adapter (pass pctAdapterPath)", null, null, false, null);
            finishEntry(context, invocation, skippedResult);
            return skippedResult;
        }

        TestResult started = new TestResult(path, name, node.getPackagePath(), null, 0L, null, null,
                isPct ? context.adapterPath : null, hookKind != null, hookKind);
        notifyTestEvent(TestEvent.test(context.runId, TestEventKind.TEST_STARTED, started));

        MutableList<CoreInstance> args = stringArgs(context.runtime, invocation.getArguments());
        if (isPct)
        {
            args.add(ValueSpecificationBootstrap.wrapValueSpecification(context.adapter, false, context.processorSupport));
        }

        long began = System.currentTimeMillis();
        // Teardown is forced through even on a cancelled run: skipping it would strand whatever the
        // matching <<test.BeforePackage>> set up (typically shared H2 tables), which then fails the
        // NEXT run with "Table ... already exists". A second cancel still interrupts it.
        ExecuteResult outcome = runIsolated(context, function, args, path, "after".equals(hookKind));
        long elapsed = System.currentTimeMillis() - began;

        if (outcome == null)
        {
            TestResult cancelledResult = new TestResult(path, name, node.getPackagePath(), TestStatus.SKIPPED, 0L,
                    "Skipped: run was cancelled", null, isPct ? context.adapterPath : null,
                    hookKind != null, hookKind);
            finishEntry(context, invocation, cancelledResult);
            return cancelledResult;
        }

        TestResult result = new TestResult(path, name, node.getPackagePath(),
                outcome.isSuccess() ? TestStatus.PASSED : TestStatus.FAILED, elapsed,
                outcome.isSuccess() ? null : describeFailure(outcome, hookKind, path),
                outcome.getOutput(), isPct ? context.adapterPath : null, hookKind != null, hookKind);
        applyReturnValue(result, outcome.getReturnValue());
        finishEntry(context, invocation, result);
        return result;
    }

    /**
     * Emits TEST_FINISHED at most once per entry. The walker can abandon a slow or cancelled entry
     * and report it while the pool task is still running, so both sides race to finish the same
     * invocation - without this the client would see a SKIPPED and a PASSED for one test.
     */
    private void finishEntry(RunContext context, TestDiscovery.Invocation invocation, TestResult result)
    {
        if (context.markFinished(invocation))
        {
            notifyTestEvent(TestEvent.test(context.runId, TestEventKind.TEST_FINISHED, result));
        }
    }

    private static void applyReturnValue(TestResult result, PureValueExtractor.ExtractedValue value)
    {
        if (value == null)
        {
            return;
        }
        result.setReturnKind(value.getKind());
        result.setReturnType(value.getType());
        result.setReturnValue(value.getValue());
        result.setReturnSize(value.getSize());
        result.setReturnTruncated(value.isTruncated());
    }

    private static String describeFailure(ExecuteResult outcome, String hookKind, String path)
    {
        String detail = outcome.getError() == null ? "failed" : outcome.getError();
        if (hookKind == null)
        {
            return detail;
        }
        return ("before".equals(hookKind) ? "Setup function '" : "Teardown function '") + path + "' failed:\n" + detail;
    }

    private static TestResult errorResult(TestDiscovery.Invocation invocation, TestDiscovery.Node node, String message)
    {
        CoreInstance function = invocation.getFunction();
        String name = invocation.getLabel() == null ? nameOf(function) : invocation.getLabel();
        return new TestResult(pathOf(function), name, node.getPackagePath(),
                TestStatus.FAILED, 0L, message, null, null, false, null);
    }

    private static String pathOf(CoreInstance function)
    {
        return PackageableElement.getUserPathForPackageableElement(function);
    }

    private static String nameOf(CoreInstance function)
    {
        return DocumentOutlineProvider.getSimpleFunctionName(function);
    }

    /**
     * Runs one already-resolved function with its own executor, console capture and thread-local
     * transaction, so concurrent entries never share state. Caller must already hold the graph read
     * lock. The executor is registered with the run while it runs, so legend/cancelTests can abort it
     * mid-execution.
     *
     * @param force run even if the run is already cancelled (teardown hooks - see runEntry)
     * @return null if the run was cancelled before this execution could start
     */
    /**
     * Points at whichever channel actually carries the result, so a caller is not told to add
     * print() calls for a value already sitting in returnValue.
     */
    private static String noConsoleOutputHint(PureValueExtractor.ExtractedValue value)
    {
        return (value == null || PureValueExtractor.KIND_EMPTY.equals(value.getKind())
                || PureValueExtractor.KIND_COMPLEX.equals(value.getKind()))
                ? "Use print() to see results."
                : "See returnValue.";
    }

    private ExecuteResult runIsolated(RunContext context, CoreInstance function, MutableList<CoreInstance> args,
                                      String label, boolean force)
    {
        PureRuntime runtime = context.runtime;
        FunctionExecutionInterpreted exec = initializeFunctionExecution(
                new StackPreservingFunctionExecutionInterpreted(), runtime, new Message(""));
        if (!context.beginExecution(exec, force))
        {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Console console = exec.getConsole();
        org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction txn =
                runtime.getModelRepository().newTransaction(false);
        try (org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext ignore = txn.openInCurrentThread())
        {
            console.setPrintStream(new PrintStream(baos, true));
            console.setConsole(true);
            try
            {
                CoreInstance returned = exec.start(function, args);
                return new ExecuteResult(true, null, baosToString(baos),
                        PureValueExtractor.extract(returned, runtime.getProcessorSupport()));
            }
            catch (Exception e)
            {
                LOGGER.debug("executeTests: {} failed", label, e);
                String errorText = ExecutionFailureFormatter.format(e, baosToString(baos), this.functionExecution.getProcessorSupport());
                return new ExecuteResult(false, errorText, errorText);
            }
        }
        finally
        {
            context.endExecution(exec);
            console.setPrintStream(new PrintStream(new ByteArrayOutputStream(), true));
            console.setConsole(false);
            txn.rollback();
        }
    }

    private void notifyTestEvent(TestEvent event)
    {
        LegendLanguageClient currentClient = this.client;
        if (currentClient == null)
        {
            return;
        }
        try
        {
            currentClient.testEvent(event);
        }
        catch (Exception e)
        {
            // A client that has gone away mid-run must never take the run down with it.
            LOGGER.debug("executeTests: failed to deliver test event", e);
        }
    }

    /**
     * Per-run state. {@code results} is synchronized because parallel node fan-out completes on pool
     * threads, though entries are always appended in deterministic order by the submitting thread.
     */
    private static final class RunContext
    {
        private final PureRuntime runtime;
        private final ProcessorSupport processorSupport;
        private final String runId;
        private final CoreInstance adapter;
        private final String adapterPath;
        private final boolean parallel;
        private final List<TestResult> results = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        private final java.util.concurrent.ExecutorService pool;
        private final List<java.util.concurrent.Future<?>> submitted =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        // Two levels: `cancelled` stops the walk (FunctionExecutionInterpreted's own cancel flag is
        // consume-once, so it can't by itself stop more than one execution); `inFlight` lets a running
        // test be interrupted immediately via cancelExecution(), since the interpreter never polls
        // Thread.interrupted() and shutdownNow() would just drain the queue, not the current test.
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final Set<FunctionExecutionInterpreted> inFlight =
                java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

        private final Set<TestDiscovery.Invocation> finished =
                java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

        RunContext(PureRuntime runtime, ProcessorSupport processorSupport, String runId, CoreInstance adapter,
                   String adapterPath, boolean parallel, java.util.concurrent.ExecutorService pool)
        {
            this.runtime = runtime;
            this.processorSupport = processorSupport;
            this.runId = runId;
            this.adapter = adapter;
            this.adapterPath = adapterPath;
            this.parallel = parallel;
            this.pool = pool;
        }

        java.util.concurrent.Future<TestResult> submit(java.util.concurrent.Callable<TestResult> task)
        {
            java.util.concurrent.Future<TestResult> future = this.pool.submit(task);
            this.submitted.add(future);
            return future;
        }

        boolean isCancelled()
        {
            return this.cancelled.get();
        }

        boolean markFinished(TestDiscovery.Invocation invocation)
        {
            return this.finished.add(invocation);
        }

        /**
         * Waits out any execution the walk abandoned, aborting it first. The caller releases the
         * graph read lock as soon as this returns, so an execution still running past this point
         * would have a later compile mutate the graph underneath it.
         */
        void drain(long timeoutNanos)
        {
            signalInFlight();
            List<java.util.concurrent.Future<?>> pending;
            synchronized (this.submitted)
            {
                pending = new java.util.ArrayList<>(this.submitted);
            }
            long deadline = System.nanoTime() + timeoutNanos;
            for (java.util.concurrent.Future<?> future : pending)
            {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0)
                {
                    LOGGER.error("executeTests: gave up draining run {} - an execution may outlive the read lock",
                            this.runId);
                    return;
                }
                try
                {
                    future.get(remaining, java.util.concurrent.TimeUnit.NANOSECONDS);
                }
                catch (Exception ignored)
                {
                    // awaitEntry already reported the outcome; draining only cares that it stopped.
                }
            }
        }

        private void signalInFlight()
        {
            for (FunctionExecutionInterpreted execution : this.inFlight)
            {
                try
                {
                    execution.cancelExecution();
                }
                catch (Exception e)
                {
                    LOGGER.debug("cancelTests: failed to signal an in-flight execution", e);
                }
            }
        }

        void cancel()
        {
            this.cancelled.set(true);
            signalInFlight();
            // Cancel this run's own tasks rather than shutting the shared pool down - an uncancelled
            // queued task would leave the walk parked in Future.get() forever.
            synchronized (this.submitted)
            {
                for (java.util.concurrent.Future<?> future : this.submitted)
                {
                    future.cancel(false);
                }
            }
        }

        /**
         * Registers an executor as in-flight and reports whether it should run at all.
         * Register-then-recheck closes the race where a cancel fires between the walk's
         * cancelled-check and this registration.
         *
         * @return false if the run was already cancelled, in which case the caller must not execute
         */
        boolean beginExecution(FunctionExecutionInterpreted execution, boolean force)
        {
            this.inFlight.add(execution);
            if (!force && this.cancelled.get())
            {
                this.inFlight.remove(execution);
                return false;
            }
            return true;
        }

        void endExecution(FunctionExecutionInterpreted execution)
        {
            this.inFlight.remove(execution);
        }

    }

    /**
     * Finds the nearest {@code <<test.BeforePackage>>}/{@code <<test.AfterPackage>>} functions to
     * {@code functionPath} (see {@link TestTools#findNearestBeforePackageFunction}) - used by the IDE
     * gutter's "Run/Debug with Setup/Teardown" actions to discover what to bracket a test with before
     * offering it as a follow-up execution.
     */
    public SetupTeardownResult findSetupTeardown(String functionPath)
    {
        if (functionPath == null || functionPath.trim().isEmpty() || !this.initialized)
        {
            return new SetupTeardownResult(null, null, null, null);
        }

        ensureCompiled();
        try (LockHandle ignored = acquireGraphReadLock())
        {
            PureRuntime runtime = this.pureRuntime;
            if (runtime == null)
            {
                return new SetupTeardownResult(null, null, null, null);
            }
            CoreInstance function = resolveFunction(runtime, buildFunctionCandidates(functionPath.trim()));
            if (function == null)
            {
                return new SetupTeardownResult(null, null, null, null);
            }
            ProcessorSupport processorSupport = runtime.getProcessorSupport();
            CoreInstance before = TestTools.findNearestBeforePackageFunction(function, processorSupport);
            CoreInstance after = TestTools.findNearestAfterPackageFunction(function, processorSupport);
            return new SetupTeardownResult(
                    before == null ? null : PackageableElement.getUserPathForPackageableElement(before),
                    before == null ? null : DocumentOutlineProvider.getSimpleFunctionName(before),
                    after == null ? null : PackageableElement.getUserPathForPackageableElement(after),
                    after == null ? null : DocumentOutlineProvider.getSimpleFunctionName(after));
        }
    }

    private static String blankToNull(String value)
    {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    // Derive candidate lookups: the caller may pass a signature ("a::b():Any[*]"), a mangled id
    // ("a::b__Any_MANY_"), or a bare path ("a::b") in which case common zero-arg return shapes are tried.
    private static FunctionCandidates buildFunctionCandidates(String path)
    {
        MutableList<String> signatureCandidates = Lists.mutable.empty();
        MutableList<String> idCandidates = Lists.mutable.empty();
        if (path.contains("("))
        {
            signatureCandidates.add(path);
        }
        else if (path.contains("__"))
        {
            idCandidates.add(path);
        }
        else
        {
            signatureCandidates.add(path + "():Any[*]");
            signatureCandidates.add(path + "():Boolean[1]");
            signatureCandidates.add(path + "():String[*]");
            signatureCandidates.add(path + "():String[1]");
            idCandidates.add(path + "__Any_MANY_");
            idCandidates.add(path + "__Boolean_1_");
            idCandidates.add(path + "__String_MANY_");
            idCandidates.add(path + "__String_1_");
        }
        return new FunctionCandidates(signatureCandidates, idCandidates);
    }

    // Resolves a function against an already-fully-compiled runtime by trying each signature
    // candidate first, then each mangled-id candidate. Caller must hold graphLock.readLock().
    private static CoreInstance resolveFunction(PureRuntime runtime, FunctionCandidates candidates)
    {
        for (String sig : candidates.signatures)
        {
            CoreInstance function = runtime.getFunction(sig);
            if (function != null)
            {
                return function;
            }
        }
        for (String id : candidates.ids)
        {
            CoreInstance function = runtime.getCoreInstance(id);
            if (function != null)
            {
                return function;
            }
        }
        return null;
    }

    private static final class FunctionCandidates
    {
        final MutableList<String> signatures;
        final MutableList<String> ids;

        FunctionCandidates(MutableList<String> signatures, MutableList<String> ids)
        {
            this.signatures = signatures;
            this.ids = ids;
        }
    }

    private ExecuteResult executeFunctionByCandidates(FunctionCandidates candidates, String label,
                                                        String notFoundMessage, String pctAdapterPath,
                                                        String beforeFunctionPath, String afterFunctionPath,
                                                        List<String> arguments)
    {
        if (!this.initialized)
        {
            return new ExecuteResult(false, "Runtime not initialized", null);
        }

        // Compile pending sources under the WRITE lock first, so the read-locked execution below runs
        // against a stable, fully-compiled graph.
        ensureCompiled();

        try (LockHandle ignored = acquireGraphReadLock())
        {
            PureRuntime runtime = this.pureRuntime;
            if (runtime == null)
            {
                return new ExecuteResult(false, "Runtime not initialized", null);
            }
            CoreInstance function = resolveFunction(runtime, candidates);
            if (function == null)
            {
                LspLog.info("execute: no function found for " + label);
                return new ExecuteResult(false, notFoundMessage, null);
            }

            // Resolved eagerly (before anything runs) so a missing setup/teardown function fails fast
            // rather than after the target function has already produced side effects/output.
            CoreInstance beforeFunction = null;
            if (beforeFunctionPath != null)
            {
                beforeFunction = resolveFunction(runtime, buildFunctionCandidates(beforeFunctionPath));
                if (beforeFunction == null)
                {
                    return new ExecuteResult(false, "Before function '" + beforeFunctionPath + "' not found in compiled sources", null);
                }
            }
            CoreInstance afterFunction = null;
            if (afterFunctionPath != null)
            {
                afterFunction = resolveFunction(runtime, buildFunctionCandidates(afterFunctionPath));
                if (afterFunction == null)
                {
                    return new ExecuteResult(false, "After function '" + afterFunctionPath + "' not found in compiled sources", null);
                }
            }

            // A <<PCT.test>> function takes exactly one parameter: the adapter Function itself
            // (see TestRunner#executeTestFunc in legend-pure-core, which this mirrors). Resolving the
            // adapter and wrapping it as the sole argument here - rather than always calling with zero
            // args - is what lets a PCT test be run directly by path from this session.
            MutableList<CoreInstance> args = stringArgs(runtime, arguments);
            if (pctAdapterPath != null && !pctAdapterPath.trim().isEmpty())
            {
                String trimmedAdapterPath = pctAdapterPath.trim();
                CoreInstance adapter = resolvePctAdapter(runtime, runtime.getProcessorSupport(), trimmedAdapterPath);
                if (adapter == null)
                {
                    return new ExecuteResult(false, unknownAdapterMessage(runtime, trimmedAdapterPath), null);
                }
                args.add(ValueSpecificationBootstrap.wrapValueSpecification(adapter, false, runtime.getProcessorSupport()));
            }

            // Fresh executor per call => own console + own cancelExecution flag (no cross-talk between
            // concurrent executions). Bound to the SHARED, read-only compiled runtime.
            FunctionExecutionInterpreted exec = initializeFunctionExecution(
                    new StackPreservingFunctionExecutionInterpreted(), runtime, new Message(""));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Console console = exec.getConsole();
            // Isolate transient instances created during this execution in a per-run thread-local
            // transaction; roll back at the end so they are reclaimed and never accrete in the graph.
            org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction txn =
                    runtime.getModelRepository().newTransaction(false);
            try (org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext ignore = txn.openInCurrentThread())
            {
                PrintStream capturePrintStream = new PrintStream(baos, true);
                console.setPrintStream(capturePrintStream);
                console.setConsole(true);

                // Before-function failure aborts the whole call (target function and after never run) -
                // mirrors TestRunner#runTestsFromCollection's fail-fast setup handling.
                if (beforeFunction != null)
                {
                    try
                    {
                        exec.start(beforeFunction, Lists.mutable.empty());
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("execute: before-function '" + beforeFunctionPath + "' failed for " + label, e);
                        String errorText = "Setup function '" + beforeFunctionPath + "' failed:\n"
                                + ExecutionFailureFormatter.format(e, baosToString(baos), this.functionExecution.getProcessorSupport());
                        return new ExecuteResult(false, errorText, errorText);
                    }
                }

                LspLog.debug("execute: found " + label + " at "
                        + (function.getSourceInformation() != null ? function.getSourceInformation().getSourceId() : "unknown"));

                ExecuteResult mainResult;
                try
                {
                    CoreInstance returned = exec.start(function, args);
                    PureValueExtractor.ExtractedValue value =
                            PureValueExtractor.extract(returned, runtime.getProcessorSupport());
                    String consoleOutput = baosToString(baos);
                    if (consoleOutput.isEmpty())
                    {
                        consoleOutput = "(" + label + " returned successfully with no console output. "
                                + noConsoleOutputHint(value) + ")";
                    }
                    LspLog.debug("execute completed for " + label + ", output length: " + consoleOutput.length());
                    mainResult = new ExecuteResult(true, null, consoleOutput, value);
                }
                catch (Exception e)
                {
                    LOGGER.error("execute failed for " + label, e);
                    String errorText = ExecutionFailureFormatter.format(e, baosToString(baos), this.functionExecution.getProcessorSupport());
                    mainResult = new ExecuteResult(false, errorText, errorText);
                }

                // After-function failure never overrides the target function's own success/failure -
                // mirrors TestRunner#runTestsFromCollection, which likewise swallows teardown failures.
                if (afterFunction != null)
                {
                    try
                    {
                        exec.start(afterFunction, Lists.mutable.empty());
                    }
                    catch (Exception e)
                    {
                        LOGGER.warn("execute: after-function '" + afterFunctionPath + "' failed for " + label, e);
                        String afterError = "Teardown function '" + afterFunctionPath + "' failed: " + e.getMessage();
                        String combinedOutput = (mainResult.getOutput() == null ? "" : mainResult.getOutput() + "\n") + afterError;
                        mainResult = new ExecuteResult(mainResult.isSuccess(), mainResult.getError(), combinedOutput);
                    }
                }

                return mainResult;
            }
            finally
            {
                console.setPrintStream(new PrintStream(new ByteArrayOutputStream(), true));
                console.setConsole(false);
                txn.rollback();
            }
        }
    }

    /**
     * Binds literals to a function's {@code String[1]} parameters - the only type this session marshals.
     */
    private static MutableList<CoreInstance> stringArgs(PureRuntime runtime, List<String> values)
    {
        MutableList<CoreInstance> args = Lists.mutable.empty();
        if (values == null)
        {
            return args;
        }
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        for (String value : values)
        {
            CoreInstance literal = runtime.getModelRepository().newStringCoreInstance(value == null ? "" : value);
            args.add(ValueSpecificationBootstrap.wrapValueSpecification(literal, true, processorSupport));
        }
        return args;
    }

    private static String baosToString(ByteArrayOutputStream baos)
    {
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    static boolean shouldLoadClasspathRepository(String name, Set<String> workspaceRepoNames, Set<String> classpathRepositoryNames)
    {
        if (name == null)
        {
            return true;
        }
        if (workspaceRepoNames.contains(name))
        {
            return false;
        }
        return true;
    }

    private static Set<String> normalizeRepositoryNames(Collection<String> repositoryNames)
    {
        if (repositoryNames == null || repositoryNames.isEmpty())
        {
            return Collections.emptySet();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String repositoryName : repositoryNames)
        {
            if (repositoryName != null)
            {
                String trimmed = repositoryName.trim();
                if (!trimmed.isEmpty())
                {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(normalized);
    }

    public PureRuntime getPureRuntime()
    {
        return this.pureRuntime;
    }

    public MutableRuntimeOptions getRuntimeOptions()
    {
        return this.runtimeOptions;
    }

    /**
     * Sets or clears a Pure runtime option for this session, returning its new effective value. In-memory
     * only: no system property is written, and the change is visible to this session's runtime immediately.
     */
    public boolean setOption(String name, boolean value)
    {
        return this.runtimeOptions.setOption(name, value);
    }

    public FunctionExecution getFunctionExecution()
    {
        return this.functionExecution;
    }

    public Set<String> getClasspathRepositoryNames()
    {
        return this.classpathRepositoryNames;
    }

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public String resolveSourceId(String sourceId)
    {
        if (sourceId == null)
        {
            return null;
        }
        if (this.pureRuntime.getSourceById(sourceId) != null)
        {
            return sourceId;
        }
        String alt = sourceId.startsWith("/") ? sourceId.substring(1) : "/" + sourceId;
        if (this.pureRuntime.getSourceById(alt) != null)
        {
            return alt;
        }
        return null;
    }

    public static class ExecuteResult
    {
        private final boolean success;
        private final String error;
        private final String output;
        private final PureValueExtractor.ExtractedValue returnValue;

        ExecuteResult(boolean success, String error, String output)
        {
            this(success, error, output, null);
        }

        ExecuteResult(boolean success, String error, String output, PureValueExtractor.ExtractedValue returnValue)
        {
            this.success = success;
            this.error = error;
            this.output = output;
            this.returnValue = returnValue;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public String getError()
        {
            return this.error;
        }

        public String getOutput()
        {
            return this.output;
        }

        /**
         * The function's own return value when it is a primitive, enum, or collection of those -
         * null for a failed call, and {@code KIND_COMPLEX} with a null value for anything whose
         * graph we decline to walk.
         */
        public PureValueExtractor.ExtractedValue getReturnValue()
        {
            return this.returnValue;
        }
    }

    public static class SetupTeardownResult
    {
        private final String beforeFunctionPath;
        private final String beforeFunctionName;
        private final String afterFunctionPath;
        private final String afterFunctionName;

        SetupTeardownResult(String beforeFunctionPath, String beforeFunctionName, String afterFunctionPath, String afterFunctionName)
        {
            this.beforeFunctionPath = beforeFunctionPath;
            this.beforeFunctionName = beforeFunctionName;
            this.afterFunctionPath = afterFunctionPath;
            this.afterFunctionName = afterFunctionName;
        }

        public String getBeforeFunctionPath()
        {
            return this.beforeFunctionPath;
        }

        public String getBeforeFunctionName()
        {
            return this.beforeFunctionName;
        }

        public String getAfterFunctionPath()
        {
            return this.afterFunctionPath;
        }

        public String getAfterFunctionName()
        {
            return this.afterFunctionName;
        }
    }

    public enum FileChangeType
    {
        CREATE_OR_MODIFY,
        DELETE
    }

    public static class FileChange
    {
        private final String sourceId;
        private final String content;
        private final FileChangeType type;

        public FileChange(String sourceId, String content, FileChangeType type)
        {
            this.sourceId = sourceId;
            this.content = content;
            this.type = type;
        }

        public String getSourceId()
        {
            return this.sourceId;
        }

        public String getContent()
        {
            return this.content;
        }

        public FileChangeType getType()
        {
            return this.type;
        }
    }

    public static class CompileResult
    {
        private final boolean ready;
        private final boolean success;
        private final boolean internalError;
        private final Exception error;
        private final List<String> modifiedFiles;

        private CompileResult(boolean ready, boolean success, boolean internalError, Exception error, List<String> modifiedFiles)
        {
            this.ready = ready;
            this.success = success;
            this.internalError = internalError;
            this.error = error;
            this.modifiedFiles = modifiedFiles;
        }

        public static CompileResult notReady()
        {
            return new CompileResult(false, false, false, null, Collections.emptyList());
        }

        public static CompileResult success(Iterable<String> modifiedFiles)
        {
            return new CompileResult(true, true, false, null, Lists.mutable.withAll(modifiedFiles));
        }

        public static CompileResult error(Exception e, boolean internal)
        {
            return new CompileResult(true, false, internal, e, Collections.emptyList());
        }

        public boolean isReady()
        {
            return this.ready;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public boolean isInternalError()
        {
            return this.internalError;
        }

        public Exception getError()
        {
            return this.error;
        }

        public List<String> getModifiedFiles()
        {
            return this.modifiedFiles;
        }
    }
}
