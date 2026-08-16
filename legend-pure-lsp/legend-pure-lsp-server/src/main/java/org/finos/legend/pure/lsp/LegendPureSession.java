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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.lsp.mutation.SourceMutationService;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.MutableRuntimeOptions;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
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
    // often). Replaces the old blanket `synchronized` that serialized everything.
    private final java.util.concurrent.locks.ReadWriteLock graphLock =
            new java.util.concurrent.locks.ReentrantReadWriteLock(true);

    private volatile RepositoryScanner workspaceScanner;
    private volatile Set<String> classpathRepositoryNames = Collections.emptySet();
    private volatile java.util.function.Consumer<String> progressListener;

    public void setProgressListener(java.util.function.Consumer<String> progressListener)
    {
        this.progressListener = progressListener;
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
        return newRuntime(scanner, true, normalizedClasspathRepositoryNames, Collections.emptySet(),
                true, normalizedClasspathRepositoryNames);
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
        this.graphLock.writeLock().lock();
        try
        {
            this.initialized = false;
            this.pureRuntime = null;
            initialize(this.workspaceScanner, this.classpathRepositoryNames);
        }
        finally
        {
            this.graphLock.writeLock().unlock();
        }
    }

    public void setClasspathRepositoryNames(Collection<String> classpathRepositoryNames)
    {
        this.graphLock.writeLock().lock();
        try
        {
            this.classpathRepositoryNames = normalizeRepositoryNames(classpathRepositoryNames);
        }
        finally
        {
            this.graphLock.writeLock().unlock();
        }
    }

    public SourceMutationService getMutationService()
    {
        return this.mutationService;
    }

    /**
     * The compiled graph is guarded by a single fair {@link java.util.concurrent.locks.ReadWriteLock}
     * ({@code graphLock}): every graph MUTATION (compile/reinitialize) takes {@link #graphWriteLock()}
     * (exclusive) and every graph READ - function execution AND the LSP providers (hover, completion,
     * references, ...) - takes {@link #graphReadLock()}. This single mechanism guarantees no mutation
     * runs while a read/execution is in flight, while still letting independent reads/executions run
     * concurrently. All production mutation paths funnel through {@link SourceMutationService}, which
     * acquires the write lock, so callers must NOT rely on the object monitor for graph exclusion.
     */
    public java.util.concurrent.locks.Lock graphReadLock()
    {
        return this.graphLock.readLock();
    }

    public java.util.concurrent.locks.Lock graphWriteLock()
    {
        return this.graphLock.writeLock();
    }

    /**
     * Run a read-only graph access under the shared read lock, so a concurrent compile (write lock)
     * cannot mutate the graph mid-read. Convenience for the LSP read providers.
     */
    public <T> T withGraphReadLock(java.util.function.Supplier<T> action)
    {
        this.graphLock.readLock().lock();
        try
        {
            return action.get();
        }
        finally
        {
            this.graphLock.readLock().unlock();
        }
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
    private void ensureCompiled()
    {
        this.graphLock.writeLock().lock();
        try
        {
            if (this.pureRuntime != null)
            {
                this.pureRuntime.compile();
            }
        }
        finally
        {
            this.graphLock.writeLock().unlock();
        }
    }

    public ExecuteResult executeGo()
    {
        return executeFunctionByCandidates(
                Lists.mutable.with("go():Any[*]", "go():String[*]", "go():String[1]"),
                Lists.mutable.with("go__Any_MANY_", "go__String_MANY_", "go__String_1_"),
                "go()",
                "No go() function found in compiled sources. Define: function go():Any[*] { ... }");
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
        if (functionPath == null || functionPath.trim().isEmpty())
        {
            return new ExecuteResult(false, "Function path is required", null);
        }
        String path = functionPath.trim();
        // Derive candidate lookups: the caller may pass a signature ("a::b():Any[*]") or a mangled id.
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
            // Bare path with no signature/mangling: try common zero-arg return shapes.
            signatureCandidates.add(path + "():Any[*]");
            signatureCandidates.add(path + "():Boolean[1]");
            signatureCandidates.add(path + "():String[*]");
            signatureCandidates.add(path + "():String[1]");
            idCandidates.add(path + "__Any_MANY_");
            idCandidates.add(path + "__Boolean_1_");
            idCandidates.add(path + "__String_MANY_");
            idCandidates.add(path + "__String_1_");
        }
        return executeFunctionByCandidates(signatureCandidates, idCandidates, path,
                "No function '" + path + "' found in compiled sources.");
    }

    private ExecuteResult executeFunctionByCandidates(MutableList<String> signatureCandidates,
                                                      MutableList<String> idCandidates,
                                                      String label, String notFoundMessage)
    {
        if (!this.initialized)
        {
            return new ExecuteResult(false, "Runtime not initialized", null);
        }

        // Compile pending sources under the WRITE lock first, so the read-locked execution below runs
        // against a stable, fully-compiled graph.
        ensureCompiled();

        this.graphLock.readLock().lock();
        try
        {
            PureRuntime runtime = this.pureRuntime;
            if (runtime == null)
            {
                return new ExecuteResult(false, "Runtime not initialized", null);
            }
            CoreInstance function = null;
            for (String sig : signatureCandidates)
            {
                function = runtime.getFunction(sig);
                if (function != null)
                {
                    break;
                }
            }
            if (function == null)
            {
                for (String id : idCandidates)
                {
                    function = runtime.getCoreInstance(id);
                    if (function != null)
                    {
                        break;
                    }
                }
            }
            if (function == null)
            {
                LspLog.info("execute: no function found for " + label);
                return new ExecuteResult(false, notFoundMessage, null);
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
                LspLog.debug("execute: found " + label + " at "
                        + (function.getSourceInformation() != null ? function.getSourceInformation().getSourceId() : "unknown"));
                PrintStream capturePrintStream = new PrintStream(baos, true);
                console.setPrintStream(capturePrintStream);
                console.setConsole(true);

                exec.start(function, FastList.newList());

                String consoleOutput = baosToString(baos);
                if (consoleOutput.isEmpty())
                {
                    consoleOutput = "(" + label + " returned successfully with no console output. Use print() to see results.)";
                }
                LspLog.debug("execute completed for " + label + ", output length: " + consoleOutput.length());
                return new ExecuteResult(true, null, consoleOutput);
            }
            catch (Exception e)
            {
                LOGGER.error("execute failed for " + label, e);
                String errorText = formatExecutionFailure(e, baos);
                return new ExecuteResult(false, errorText, errorText);
            }
            finally
            {
                console.setPrintStream(new PrintStream(new ByteArrayOutputStream(), true));
                console.setConsole(false);
                txn.rollback();
            }
        }
        finally
        {
            this.graphLock.readLock().unlock();
        }
    }

    private String formatExecutionFailure(Exception e, ByteArrayOutputStream consoleOutput)
    {
        StringBuilder builder = new StringBuilder();
        String printed = baosToString(consoleOutput);
        if (!printed.isEmpty())
        {
            builder.append(printed);
            if (!printed.endsWith("\n"))
            {
                builder.append('\n');
            }
        }

        PureException pureException = PureException.findPureException(e);
        if (pureException != null)
        {
            PureException original = pureException.getOriginatingPureException();
            if (original == null)
            {
                original = pureException;
            }
            if (pureException instanceof PureExecutionException)
            {
                builder.append(original.getMessage()).append('\n');
                StringBuffer buffer = new StringBuffer();
                ((PureExecutionException) pureException).printPureStackTrace(
                        buffer, "", this.functionExecution.getProcessorSupport());
                builder.append(buffer);
            }
            else if (pureException.hasPureStackTrace())
            {
                builder.append(original.getMessage()).append('\n')
                        .append(pureException.getPureStackTrace("    "));
            }
            else
            {
                builder.append(original.getMessage());
            }
        }
        else
        {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            builder.append(writer);
        }

        return builder.toString();
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
