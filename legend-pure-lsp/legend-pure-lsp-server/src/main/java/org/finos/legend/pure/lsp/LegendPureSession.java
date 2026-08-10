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
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LockContentionEvent;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.TestTools;
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

    private volatile PureRuntime pureRuntime;
    private volatile FunctionExecution functionExecution;
    private volatile boolean initialized;

    // Session-scoped and mutable, so legend/setOption can toggle options live without writing JVM system
    // properties. Created eagerly and never replaced, so toggles survive a runtime rebuild.
    private final MutableRuntimeOptions runtimeOptions = MutableRuntimeOptions.fromSystemProperties();

    private final SourceMutationService mutationService = new SourceMutationService(this);

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
     * Compile any pending (uncompiled) sources. This is a graph MUTATION so it takes the WRITE lock.
     * Execution paths call this first (outside their read lock) so that by the time they hold the read
     * lock the graph is fully compiled and stable. When nothing is pending, compile() is a no-op.
     */
    /**
     * Compile any pending (uncompiled) sources. Every SourceMutationService mutation already
     * compiles before releasing the write lock (the graphLock javadoc's single chokepoint), so by
     * the time any caller reaches here the graph is provably already compiled in the common case -
     * graphDirty lets that common case skip the write lock entirely instead of paying for a
     * round-trip that can only ever find nothing to do. See the graphDirty field javadoc for why
     * this is still correct (not just "usually correct") even if that invariant is ever violated by
     * a future change.
     */
    private void ensureCompiled()
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
                null, null, null);
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
        if (functionPath == null || functionPath.trim().isEmpty())
        {
            return new ExecuteResult(false, "Function path is required", null);
        }
        String path = functionPath.trim();
        return executeFunctionByCandidates(buildFunctionCandidates(path), path,
                "No function '" + path + "' found in compiled sources.", pctAdapterPath,
                blankToNull(beforeFunctionPath), blankToNull(afterFunctionPath));
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
                                                        String beforeFunctionPath, String afterFunctionPath)
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
            MutableList<CoreInstance> args = Lists.mutable.empty();
            if (pctAdapterPath != null && !pctAdapterPath.trim().isEmpty())
            {
                CoreInstance adapter = _Package.getByUserPath(pctAdapterPath.trim(), runtime.getProcessorSupport());
                if (adapter == null)
                {
                    return new ExecuteResult(false, "PCT adapter '" + pctAdapterPath.trim() + "' not found in compiled sources", null);
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
                    exec.start(function, args);
                    String consoleOutput = baosToString(baos);
                    if (consoleOutput.isEmpty())
                    {
                        consoleOutput = "(" + label + " returned successfully with no console output. Use print() to see results.)";
                    }
                    LspLog.debug("execute completed for " + label + ", output length: " + consoleOutput.length());
                    mainResult = new ExecuteResult(true, null, consoleOutput);
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

        ExecuteResult(boolean success, String error, String output)
        {
            this.success = success;
            this.error = error;
            this.output = output;
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
