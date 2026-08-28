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

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.finos.legend.pure.lsp.diagnostics.DiagnosticService;
import org.finos.legend.pure.lsp.debug.DebugService;
import org.finos.legend.pure.lsp.debug.LegendDebugSocketServer;
import org.finos.legend.pure.lsp.mutation.SourceMutationService;
import org.finos.legend.pure.lsp.protocol.CheckBatchParams;
import org.finos.legend.pure.lsp.protocol.CheckBatchResult;
import org.finos.legend.pure.lsp.protocol.DapEndpoint;
import org.finos.legend.pure.lsp.protocol.ExecuteFunctionParams;
import org.finos.legend.pure.lsp.protocol.ExecuteGoParams;
import org.finos.legend.pure.lsp.protocol.ExecuteGoResult;
import org.finos.legend.pure.lsp.protocol.FileEntry;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.finos.legend.pure.lsp.protocol.DeleteFileParams;
import org.finos.legend.pure.lsp.protocol.DeleteFileResult;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.finos.legend.pure.lsp.protocol.PCTAdapterInfo;
import org.finos.legend.pure.lsp.protocol.ResolveSourceUriParams;
import org.finos.legend.pure.lsp.protocol.ResolveSourceUriResult;
import org.finos.legend.pure.lsp.protocol.SetOptionParams;
import org.finos.legend.pure.lsp.protocol.SetOptionResult;
import org.finos.legend.pure.lsp.protocol.SyncWorkspaceParams;
import org.finos.legend.pure.lsp.protocol.SyncWorkspaceResult;
import org.finos.legend.pure.lsp.protocol.GetSetupTeardownParams;
import org.finos.legend.pure.lsp.protocol.SetupTeardownInfo;
import org.finos.legend.pure.lsp.protocol.TestFunctionInfo;
import org.finos.legend.pure.lsp.protocol.TestFunctionsParams;
import org.finos.legend.pure.lsp.runtime.PureRuntimeManager;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendPureLspServer implements LanguageServer, LanguageClientAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureLspServer.class);
    private static final String VERSION = "0.3.0-2026-04-01";

    // Configurable at startup only (read once, here) via -Dlegend.lsp.requestPoolSize=<N> - e.g. via
    // the launcher scripts' generic --jvm-arg passthrough. Default of 12 is deliberately well above
    // the old hardcoded 4: this environment's cgroup caps the JVM at 16 available processors (see
    // Runtime.getRuntime().availableProcessors()), and every LSP request type (executeGo/execute,
    // hover, completion, diagnostics, ...) funnels through this one pool, so 4 left most of that
    // capacity unused and made concurrent test/execute traffic queue well before hardware was the
    // limit. 12 leaves headroom for GC and the daemon's other background threads (drift-watcher,
    // compile-debounce) rather than claiming the full 16.
    private static final String REQUEST_POOL_SIZE_PROPERTY = "legend.lsp.requestPoolSize";
    private static final int DEFAULT_REQUEST_POOL_SIZE = 12;

    private final ClientBroadcaster clientBroadcaster = new ClientBroadcaster();
    private final DiagnosticService diagnosticService;

    private final UriMapper uriMapper = new UriMapper();
    private final RepositoryScanner repositoryScanner = new RepositoryScanner();
    private final WorkspaceSymbolProvider symbolProvider = new WorkspaceSymbolProvider();
    private final LegendTextDocumentService textDocumentService;
    private final LegendWorkspaceService workspaceService;
    private final PureRuntimeManager runtimeManager;
    private final DebugService debugService;
    private final LegendDebugSocketServer debugSocketServer;
    private final WorkspaceDriftWatcher driftWatcher;
    private final int requestPoolSize = resolveRequestPoolSize();
    private final ExecutorService requestExecutor = Executors.newFixedThreadPool(this.requestPoolSize, r ->
    {
        Thread t = new Thread(r, "legend-pure-lsp-request");
        t.setDaemon(true);
        return t;
    });

    // Set once, by whichever fires first: preconfigureAndWarm() (a config-driven launcher) or the
    // first connecting client's initialize/initialized. Every later connection's initialize/initialized
    // is a pure attach to the already-warm session - see the javadoc on preconfigureAndWarm() for why
    // this matters (a second IDE window's handshake used to silently force a full recompile).
    private final AtomicBoolean workspaceConfigured = new AtomicBoolean(false);
    private final AtomicBoolean runtimeInitializeStarted = new AtomicBoolean(false);

    // True only for the socket-daemon accept loop (runSocketMode), never for the classic single-client
    // stdio main() path. Guards shutdown()/exit(): in stdio mode there's exactly one client and exiting
    // the JVM on its request is correct LSP behaviour; in daemon mode the same process outlives any one
    // connection, so a client's own shutdown/exit must only ever end its own connection.
    private volatile boolean daemonMode = false;

    // Observability only (see status()): which port/transport this daemon is actually reachable on,
    // so a client polling legend/status doesn't need to already know how it was launched.
    private volatile int port = -1;
    private volatile String transport = "stdio";

    public LegendPureLspServer()
    {
        this.diagnosticService = new DiagnosticService(this.clientBroadcaster, this.uriMapper);
        this.textDocumentService = new LegendTextDocumentService(this);
        this.runtimeManager = new PureRuntimeManager(
                this.repositoryScanner,
                this.uriMapper,
                this.symbolProvider,
                this.textDocumentService::compileOpenDocuments,
                this.diagnosticService);
        this.runtimeManager.setClient(this.clientBroadcaster);
        this.debugService = new DebugService(
                this.runtimeManager,
                this.repositoryScanner,
                this.uriMapper,
                this.textDocumentService::getOpenDocumentSourceSnapshot);
        this.debugSocketServer = new LegendDebugSocketServer(this.debugService);
        this.workspaceService = new LegendWorkspaceService(this);
        this.driftWatcher = new WorkspaceDriftWatcher(
                this.repositoryScanner,
                this.uriMapper,
                this.textDocumentService::hasOpenDocument,
                this.clientBroadcaster::workspaceDriftDetected);
        LspLog.setSink(this.clientBroadcaster::logOutput);
    }

    /**
     * Configures and compiles the workspace up front, from explicit roots, instead of waiting for a
     * connecting client's {@code initialize} request to supply {@code workspaceFolders} - lets a
     * config-driven launcher (e.g. a plain, IDE-runnable {@code main()} that isn't itself an LSP client)
     * produce an already-warm session before any client connects. Any client that connects later - the
     * first one, or a second/third IDE window joining an already-warm daemon - just attaches to this
     * same session; its {@code initialize}/{@code initialized} handshake no longer reconfigures or
     * recompiles anything (see {@link #initialize(InitializeParams)}/{@link #initialized(InitializedParams)}).
     */
    public void preconfigureAndWarm(List<Path> repoRoots, Set<String> classpathRepositoryNames)
    {
        LspLog.info("Pre-configuring workspace from " + repoRoots.size()
                + " repo root(s) (config-driven, before any client connects)");
        this.workspaceConfigured.set(true);
        this.runtimeInitializeStarted.set(true);
        this.runtimeManager.configure(repoRoots, classpathRepositoryNames);
        this.runtimeManager.initialize();
        startOrRestartDriftWatcher();
    }

    @Override
    public void connect(LanguageClient client)
    {
        LegendLanguageClient wrapped = this.clientBroadcaster.register(client);
        // Catch up this newly-connected client on current status right away. Without this, a client
        // connecting to a session that's already "ready" (a reconnect, or a second client joining an
        // existing warm daemon) never receives a legend/statusChanged notification at all - nothing
        // has "changed" from the server's perspective - so any client-side readiness gate that waits
        // for that notification (e.g. the IntelliJ plugin's Execute Go action awaiting whenReady())
        // hangs indefinitely instead of observing the session is already up.
        wrapped.statusChanged(this.runtimeManager.currentStatus());
    }

    /**
     * Deregisters a client whose connection has ended, so future broadcasts (diagnostics, status,
     * log output, show-message) stop targeting it - called from {@link #serveSocketConnection} once
     * that connection's {@code startListening()} future completes. Package-private: only the socket
     * accept loop needs this; the stdio {@code main()} path never disconnects (see its call site).
     */
    void disconnect(LanguageClient client)
    {
        this.clientBroadcaster.deregister(client);
    }

    LanguageClient getClient()
    {
        return this.clientBroadcaster;
    }

    DiagnosticService getDiagnosticService()
    {
        return this.diagnosticService;
    }

    LegendPureSession getSession()
    {
        return this.runtimeManager.getSession();
    }

    SourceMutationService getMutationService()
    {
        return this.runtimeManager.getMutationService();
    }

    UriMapper getUriMapper()
    {
        return this.uriMapper;
    }

    WorkspaceSymbolProvider getSymbolProvider()
    {
        return this.symbolProvider;
    }

    RepositoryScanner getRepositoryScanner()
    {
        return this.repositoryScanner;
    }

    WorkspaceDriftWatcher getDriftWatcher()
    {
        return this.driftWatcher;
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        List<Path> workspaceRoots = extractWorkspaceRoots(params);
        Set<String> classpathRepositoryNames = new LinkedHashSet<>(extractClasspathRepositoryNames(params));
        if (this.workspaceConfigured.compareAndSet(false, true))
        {
            this.runtimeManager.configure(workspaceRoots, classpathRepositoryNames);
            LspLog.info("Legend Pure LSP v" + VERSION + " starting");
            LspLog.info("Workspace roots: " + workspaceRoots);
        }
        else
        {
            LspLog.info("Legend Pure LSP v" + VERSION + ": new client attaching to the already-configured"
                    + " session; ignoring this client's workspace roots (" + workspaceRoots + ")");
        }

        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        caps.setCompletionProvider(new org.eclipse.lsp4j.CompletionOptions(false, Arrays.asList(":", "$", ".")));
        caps.setDefinitionProvider(true);
        caps.setReferencesProvider(true);
        caps.setHoverProvider(true);
        caps.setDocumentSymbolProvider(true);
        caps.setWorkspaceSymbolProvider(true);
        caps.setCodeActionProvider(new CodeActionOptions(Collections.singletonList(org.eclipse.lsp4j.CodeActionKind.QuickFix)));
        caps.setFoldingRangeProvider(true);

        SemanticTokensLegend legend = new SemanticTokensLegend(
                SemanticTokensProvider.TOKEN_TYPES,
                SemanticTokensProvider.TOKEN_MODIFIERS);
        SemanticTokensWithRegistrationOptions semanticOptions = new SemanticTokensWithRegistrationOptions(legend);
        semanticOptions.setFull(true);
        semanticOptions.setRange(false);
        caps.setSemanticTokensProvider(semanticOptions);
        caps.setExecuteCommandProvider(new ExecuteCommandOptions(Collections.singletonList(LegendWorkspaceService.CMD_REINDEX)));
        return CompletableFuture.completedFuture(new InitializeResult(caps));
    }

    @Override
    public void initialized(InitializedParams params)
    {
        if (this.runtimeInitializeStarted.compareAndSet(false, true))
        {
            runAsync(this.runtimeManager::initialize).thenRun(this::startOrRestartDriftWatcher);
        }
        else
        {
            LspLog.info("Runtime already initializing/initialized; skipping a redundant full compile"
                    + " for this newly-connected client");
        }
    }

    void triggerRecovery()
    {
        runAsync(this.runtimeManager::triggerRecovery);
    }

    /**
     * (Re)starts {@link #driftWatcher} against whatever resource roots {@link #repositoryScanner} just
     * discovered - called after every full scan (initial warm-up, first client's initialize/initialized,
     * and reindex), since a reindex can discover modules that did not exist at the previous scan.
     */
    private void startOrRestartDriftWatcher()
    {
        this.driftWatcher.stop();
        this.driftWatcher.start(this.repositoryScanner.getMappings().values());
    }

    CompletableFuture<Void> reindex()
    {
        return runAsync(this.runtimeManager::reindex).thenRun(this::startOrRestartDriftWatcher);
    }

    @Override
    public void setTrace(SetTraceParams params)
    {
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        if (this.daemonMode)
        {
            // Shared across every connected client - tearing these down for one connection's shutdown
            // request would break every other window still attached to this daemon. This connection's
            // own teardown happens on socket close (see disconnect(LanguageClient), serveSocketConnection).
            return CompletableFuture.completedFuture(null);
        }
        this.debugService.shutdown();
        this.debugSocketServer.close();
        this.textDocumentService.shutdown();
        this.runtimeManager.shutdown();
        this.driftWatcher.stop();
        this.requestExecutor.shutdownNow();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit()
    {
        if (this.daemonMode)
        {
            // A client's exit() ends its own connection, not the shared daemon process - see shutdown()
            // above. The client is expected to close its side of the socket after this notification,
            // which is what actually unblocks serveSocketConnection()'s accept-loop thread and runs the
            // existing disconnect(LanguageClient) cleanup for just that one connection.
            return;
        }
        System.exit(0);
    }

    @JsonRequest("legend/status")
    public CompletableFuture<LspStatus> status()
    {
        return CompletableFuture.completedFuture(buildStatus());
    }

    /**
     * Merges server/session-level observability (client count, port/transport, launch config, recent
     * errors, live lock contention) onto PureRuntimeManager's compile-progress status, so a client
     * polling legend/status - not just one live-streaming legend/lockContention or legend/logOutput
     * notifications - gets the full picture in one call. Kept here (not in PureRuntimeManager) because
     * every one of these fields describes the SERVER/session, not compile progress.
     */
    private LspStatus buildStatus()
    {
        LspStatus status = this.runtimeManager.currentStatus();
        status.setConnectedClientCount(this.clientBroadcaster.connectedClientCount());
        status.setPort(this.port);
        status.setTransport(this.transport);
        status.setRequestPoolSize(this.requestPoolSize);
        List<Path> workspaceRoots = this.runtimeManager.getWorkspaceRoots();
        List<String> repoRoots = new ArrayList<>(workspaceRoots.size());
        for (Path root : workspaceRoots)
        {
            repoRoots.add(root.toString());
        }
        status.setRepoRoots(repoRoots);
        status.setJvmArgs(new ArrayList<>(
                java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()));
        status.setRecentErrors(LspLog.recentErrors());
        LegendPureSession session = getSession();
        if (session != null)
        {
            status.setLockContended(session.isLockContended());
            status.setLockContentionReason(session.lockContentionReason());
        }
        return status;
    }

    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier)
    {
        return CompletableFuture.supplyAsync(supplier, this.requestExecutor);
    }

    CompletableFuture<Void> runAsync(Runnable runnable)
    {
        return CompletableFuture.runAsync(runnable, this.requestExecutor);
    }

    @Override
    public TextDocumentService getTextDocumentService()
    {
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService()
    {
        return this.workspaceService;
    }

    @JsonRequest("legend/getPackageChildren")
    public CompletableFuture<List<PackageChildInfo>> getPackageChildren(String packagePath)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return Collections.<PackageChildInfo>emptyList();
            }
            return session.withGraphReadLock(() ->
                    PackageTreeProvider.getChildren(session.getPureRuntime(), this.uriMapper, packagePath));
        });
    }

    /**
     * Finds real compiled functions carrying the &lt;&lt;test.Test&gt;&gt; stereotype in the given source,
     * for gutter-icon placement client-side. See TestFunctionProvider for why this is a semantic
     * stereotype check (TestTools#hasTestStereotype) rather than a text/regex scan.
     */
    @JsonRequest("legend/testFunctions")
    public CompletableFuture<List<TestFunctionInfo>> testFunctions(TestFunctionsParams params)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized() || params == null || params.getUri() == null)
            {
                return Collections.<TestFunctionInfo>emptyList();
            }

            String rawUri = params.getUri();
            String sourceId = rawUri.startsWith("pure://")
                    ? rawUri.substring("pure://".length())
                    : this.uriMapper.toSourceId(rawUri);
            String resolvedId = session.resolveSourceId(sourceId);
            if (resolvedId == null)
            {
                return Collections.<TestFunctionInfo>emptyList();
            }

            return session.withGraphReadLock(() ->
                    TestFunctionProvider.getTestFunctions(session.getPureRuntime(), resolvedId));
        });
    }

    @JsonRequest("legend/executeGo")
    public CompletableFuture<ExecuteGoResult> executeGo(ExecuteGoParams params)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return new ExecuteGoResult(false, "Runtime not initialized", null);
            }
            List<FileEntry> files = params == null ? null : params.getFiles();
            // NOTE: no `synchronized (session)` here anymore. Compilation (compileBatch) and execution
            // (executeGo) each acquire the session's internal ReadWriteLock (write for compile, read
            // for execute). Dropping the object-monitor lets independent executions run concurrently
            // on the requestExecutor pool while compiles remain exclusive.
            if (files != null && !files.isEmpty())
            {
                CheckBatchResult batchResult = compileBatch(session, files);
                if (!batchResult.isSuccess())
                {
                    return new ExecuteGoResult(false, batchResult.getError(), null, batchResult.getErrorUri());
                }
            }
            LegendPureSession.ExecuteResult result = session.executeGo();
            return new ExecuteGoResult(result.isSuccess(), result.getError(), result.getOutput(), null);
        });
    }

    /**
     * Execute an arbitrary zero-argument function by Pure path (not just go()). Runs concurrently with
     * other executeFunction/executeGo calls (the session read-locks execution; the requestExecutor is
     * a pool). Optional files are compiled as one batch (write-locked) before executing.
     */
    @JsonRequest("legend/execute")
    public CompletableFuture<ExecuteGoResult> execute(ExecuteFunctionParams params)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return new ExecuteGoResult(false, "Runtime not initialized", null);
            }
            if (params == null || params.getFunction() == null || params.getFunction().trim().isEmpty())
            {
                return new ExecuteGoResult(false, "Function path is required (params.function)", null);
            }
            List<FileEntry> files = params.getFiles();
            if (files != null && !files.isEmpty())
            {
                CheckBatchResult batchResult = compileBatch(session, files);
                if (!batchResult.isSuccess())
                {
                    return new ExecuteGoResult(false, batchResult.getError(), null, batchResult.getErrorUri());
                }
            }
            LegendPureSession.ExecuteResult result = session.executeFunction(params.getFunction(), params.getPctAdapterPath(),
                    params.getBeforeFunctionPath(), params.getAfterFunctionPath());
            return new ExecuteGoResult(result.isSuccess(), result.getError(), result.getOutput(), null);
        });
    }

    /**
     * Finds the nearest &lt;&lt;test.BeforePackage&gt;&gt;/&lt;&lt;test.AfterPackage&gt;&gt; functions to
     * {@code params.functionPath} (see TestTools#findNearestBeforePackageFunction in legend-pure-core),
     * for the gutter's "Run/Debug with Setup/Teardown" actions to discover what to bracket a test with
     * before threading the result into {@link #execute(ExecuteFunctionParams)} via
     * {@link ExecuteFunctionParams#setBeforeFunctionPath}/{@link ExecuteFunctionParams#setAfterFunctionPath}.
     */
    @JsonRequest("legend/getSetupTeardown")
    public CompletableFuture<SetupTeardownInfo> getSetupTeardown(GetSetupTeardownParams params)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized() || params == null || params.getFunctionPath() == null)
            {
                return new SetupTeardownInfo(null, null, null, null);
            }
            LegendPureSession.SetupTeardownResult result = session.findSetupTeardown(params.getFunctionPath());
            return new SetupTeardownInfo(result.getBeforeFunctionPath(), result.getBeforeFunctionName(),
                    result.getAfterFunctionPath(), result.getAfterFunctionName());
        });
    }

    /**
     * Finds every element in the currently-loaded graph carrying the &lt;&lt;PCT.adapter&gt;&gt;
     * stereotype - the same discovery meta::pure::ide::testing::getPCTAdapters() performs in Pure -
     * so a client can offer a choice of adapters (e.g. in-memory vs. a real relational execution
     * strategy) when running a &lt;&lt;PCT.test&gt;&gt; function via {@link #execute(ExecuteFunctionParams)}.
     */
    @JsonRequest("legend/getPCTAdapters")
    public CompletableFuture<List<PCTAdapterInfo>> getPCTAdapters()
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return Collections.<PCTAdapterInfo>emptyList();
            }
            return session.withGraphReadLock(() ->
                    PCTAdapterProvider.getPCTAdapters(session.getPureRuntime()));
        });
    }

    static final String PURE_OPTION_PREFIX = "pure.options.";

    /**
     * Sets or clears a Pure runtime option so that isOptionSet('&lt;name&gt;') reflects it live, without
     * restarting the server. The session holds a MutableRuntimeOptions, seeded once from the
     * "pure.options.*" system properties and thereafter mutated in memory, so the change takes effect
     * immediately for subsequent go()/execute() runs.
     * <p>
     * The option is scoped to this session: no system property is written, so other code in this JVM
     * reading "pure.options.*" directly will not observe the toggle.
     */
    @JsonRequest("legend/setOption")
    public CompletableFuture<SetOptionResult> setOption(SetOptionParams params)
    {
        return supplyAsync(() ->
        {
            if (params == null || params.getName() == null || params.getName().trim().isEmpty())
            {
                return new SetOptionResult(false, params == null ? null : params.getName(), false, "Option name is required");
            }
            LegendPureSession session = getSession();
            if (session == null)
            {
                return new SetOptionResult(false, params.getName(), false, "Session is not available");
            }
            String name = params.getName().trim();
            boolean effective = session.setOption(name, params.isValue());
            LspLog.info("setOption: " + name + " -> " + effective + " (isOptionSet('" + name + "') now returns " + effective + ")");
            return new SetOptionResult(true, name, effective, null);
        });
    }

    /**
     * Reports the live set of Pure runtime options currently in effect, closing the gap where
     * setOption can only confirm the value it just set, never the full current state. Scans this
     * JVM's system properties for the PURE_OPTION_PREFIX namespace and returns the bare option names
     * - i.e. every name for which isOptionSet(name) currently returns true.
     */
    @JsonRequest("legend/getOptions")
    public CompletableFuture<List<String>> getOptions()
    {
        return supplyAsync(() ->
        {
            List<String> options = new ArrayList<>();
            for (String key : System.getProperties().stringPropertyNames())
            {
                if (key.startsWith(PURE_OPTION_PREFIX))
                {
                    options.add(key.substring(PURE_OPTION_PREFIX.length()));
                }
            }
            return options;
        });
    }

    /**
     * Unloads a .pure source from the running session: removes it from the open-document set (so it
     * is not re-pushed as an open doc), deletes it from the Pure runtime, and clears any overlay
     * content, then recompiles. Unlike a workspace/didChangeWatchedFiles Deleted event (which is
     * filtered out for currently-open documents), this reliably removes a file the bridge pushed via
     * didOpen - e.g. a throwaway go() wrapper - which is the fix for orphan
     * 'go__Any_MANY_ is defined more than once' overlays lingering in the warm session.
     * <p>
     * Accepts either a file:// uri or a raw sourceId. removed=false (with success=true) means the
     * source was not present to begin with - not an error.
     */
    @JsonRequest("legend/deleteFile")
    public CompletableFuture<DeleteFileResult> deleteFile(DeleteFileParams params)
    {
        return supplyAsync(() ->
        {
            if (params == null || params.getUri() == null || params.getUri().trim().isEmpty())
            {
                return new DeleteFileResult(false, null, false, "File uri/sourceId is required");
            }
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return new DeleteFileResult(false, null, false, "Runtime not initialized");
            }
            SourceMutationService mutationService = getMutationService();
            if (mutationService == null)
            {
                return new DeleteFileResult(false, null, false, "Mutation service not available");
            }

            String raw = params.getUri().trim();
            String uri = raw.startsWith("file://") ? raw : ("file://" + raw);

            // Drop it from the open-document set first so a subsequent compile does not re-add it.
            this.textDocumentService.removeOpenDocument(uri);

            try (LegendPureSession.LockHandle ignored = session.acquireGraphWriteLock())
            {
                // Resolve to the sourceId the runtime actually registered. The input may be a file://
                // uri, an absolute path, or a raw sourceId, and the uriMapper's derivation for a
                // pushed file is not a straight path copy - so try the mapped id, the raw input, and
                // the leading-slash-normalised forms until one names a live source.
                String sourceId = null;
                for (String candidate : new String[]{this.uriMapper.toSourceId(uri), raw,
                        raw.startsWith("/") ? raw : ("/" + raw), raw.startsWith("/") ? raw.substring(1) : raw})
                {
                    if (candidate != null && session.getPureRuntime().getSourceById(candidate) != null)
                    {
                        sourceId = candidate;
                        break;
                    }
                }
                if (sourceId == null)
                {
                    String derived = this.uriMapper.toSourceId(uri);
                    LspLog.info("deleteFile: " + derived + " (from " + raw + ") not present in session (nothing to remove)");
                    return new DeleteFileResult(true, derived, false, null);
                }
                LegendPureSession.CompileResult result = mutationService.applyBulkChangesAndCompile(
                        Collections.singletonList(new LegendPureSession.FileChange(
                                sourceId, null, LegendPureSession.FileChangeType.DELETE)));
                if (result.isInternalError())
                {
                    this.triggerRecovery();
                    return new DeleteFileResult(false, sourceId, false,
                            "Internal error deleting " + sourceId + "; recovery triggered");
                }
                if (!result.isSuccess() && result.getError() != null)
                {
                    return new DeleteFileResult(false, sourceId, false, result.getError().getMessage());
                }
                LspLog.info("deleteFile: removed " + sourceId + " from session");
                return new DeleteFileResult(true, sourceId, true, null);
            }
        });
    }

    @JsonRequest("legend/syncWorkspace")
    public CompletableFuture<SyncWorkspaceResult> syncWorkspace(SyncWorkspaceParams params)
    {
        return supplyAsync(() -> this.workspaceService.syncWorkspace(params));
    }

    @JsonRequest("legend/checkBatch")
    public CompletableFuture<CheckBatchResult> checkBatch(CheckBatchParams params)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return CheckBatchResult.failure("Runtime not initialized", null);
            }
            List<FileEntry> files = params == null ? null : params.getFiles();
            if (files == null || files.isEmpty())
            {
                return CheckBatchResult.failure("'files' must be a non-empty list", null);
            }
            // compileBatch takes the write lock itself.
            return compileBatch(session, files);
        });
    }

    /**
     * Applies every file in one atomic, single-compile batch (see
     * SourceMutationService#applyBulkChangesAndCompile) instead of the one-compile-per-file path
     * textDocument/didChange uses. On success every file (and anything else transitively
     * affected) is guaranteed clean; on failure the whole batch is rolled back and exactly one
     * error is reported, attributed to its real source file via DiagnosticService#resolveErrorUri
     * where the exception carries source information.
     *
     * Takes the session write lock itself for the whole batch (compile + index + diagnostics), so it
     * is safe to call with no outer lock - e.g. directly from legend/executeGo and legend/execute.
     */
    private CheckBatchResult compileBatch(LegendPureSession session, List<FileEntry> files)
    {
        List<LegendPureSession.FileChange> changes = new ArrayList<>(files.size());
        for (FileEntry file : files)
        {
            String sourceId = this.uriMapper.toSourceId(file.getUri());
            if (sourceId == null)
            {
                // Not part of any registered Pure module (e.g. a fixture resource that merely has a
                // .pure extension) - must not poison an otherwise-valid atomic batch compile.
                LspLog.debug("Skipping non-module file in batch: " + file.getUri());
                continue;
            }
            changes.add(new LegendPureSession.FileChange(sourceId, file.getContent(), LegendPureSession.FileChangeType.CREATE_OR_MODIFY));
        }

        // Whole batch under the write lock: the compile, the symbol-index rebuild, and the diagnostics
        // refresh are one atomic unit that no mutation can interleave - which is what lets executeGo /
        // execute call this with no outer lock. applyBulkChangesAndCompile re-takes the (reentrant)
        // write lock internally.
        try (LegendPureSession.LockHandle ignored = session.acquireGraphWriteLock())
        {
            LegendPureSession.CompileResult result = session.getMutationService().applyBulkChangesAndCompile(changes);

            if (!result.isReady())
            {
                return CheckBatchResult.failure("Runtime not ready", null);
            }
            if (result.isInternalError())
            {
                LspLog.error("Internal error during batch compile, triggering recovery: " + result.getError());
                triggerRecovery();
                return CheckBatchResult.failure("Internal error, runtime is recovering: " + result.getError().getMessage(), null);
            }
            if (result.isSuccess())
            {
                // getModifiedFiles() only reports knock-on effects elsewhere in the graph, not the
                // files that were the direct input to this compile - without also clearing the
                // input files' own diagnostics here, a real LSP client (VS Code) would keep showing
                // stale errors on them after a fix, since nothing would ever tell it they're clean.
                LinkedHashSet<String> modifiedUris = new LinkedHashSet<>();
                for (FileEntry file : files)
                {
                    modifiedUris.add(file.getUri());
                }
                for (String sourceId : result.getModifiedFiles())
                {
                    String uri = this.uriMapper.toUri(sourceId);
                    if (uri != null)
                    {
                        modifiedUris.add(uri);
                    }
                }
                if (this.diagnosticService != null)
                {
                    for (String uri : modifiedUris)
                    {
                        this.diagnosticService.clear(uri);
                    }
                }
                this.symbolProvider.buildIndex(session.getPureRuntime());
                return CheckBatchResult.success(new ArrayList<>(modifiedUris));
            }

            Exception error = result.getError();
            String errorUri = this.diagnosticService == null ? null : this.diagnosticService.resolveErrorUri(error);
            String fallbackUri = files.get(0).getUri();
            // Publish to the file the error actually resolves to (not blindly files.get(0)), so the
            // squiggle lands on the real offending source.
            String publishUri = errorUri != null ? errorUri : fallbackUri;
            List<org.eclipse.lsp4j.Diagnostic> errorDiagnostics = null;
            if (this.diagnosticService != null)
            {
                errorDiagnostics = this.diagnosticService.fromException(error);
                this.diagnosticService.publishException(publishUri, error, session);
            }
            return CheckBatchResult.failure(error.getMessage(), publishUri, errorDiagnostics);
        }
    }

    @JsonRequest("legend/getSourceContent")
    public CompletableFuture<String> getSourceContent(String sourceId)
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return null;
            }

            String id = sourceId;
            if (id.startsWith("pure://"))
            {
                id = id.substring("pure://".length());
            }

            String resolvedId = session.resolveSourceId(id);
            if (resolvedId == null)
            {
                LspLog.debug("getSourceContent: unknown source ID: " + sourceId);
                return null;
            }

            Source source = session.getPureRuntime().getSourceById(resolvedId);
            if (source == null)
            {
                return null;
            }

            LspLog.debug("getSourceContent: serving " + resolvedId + " (" + source.getContent().length() + " chars)");
            return source.getContent();
        });
    }

    /**
     * Resolves a Pure internal sourceId (as embedded in a "resource:&lt;sourceId&gt; line:.. column:.."
     * stack-trace location reference) to an editor-navigable URI, so a client can turn stack trace
     * lines into clickable links. Delegates to the same {@link UriMapper#toUri} logic already used
     * internally (see compileBatch). Null/empty uri in the result means unresolvable.
     */
    @JsonRequest("legend/resolveSourceUri")
    public CompletableFuture<ResolveSourceUriResult> resolveSourceUri(ResolveSourceUriParams params)
    {
        return supplyAsync(() ->
        {
            ResolveSourceUriResult result = new ResolveSourceUriResult();
            String sourceId = params == null ? null : params.getSourceId();
            result.setUri(sourceId == null ? null : this.uriMapper.toUri(sourceId));
            return result;
        });
    }

    @JsonRequest("legend/debug/start")
    public CompletableFuture<LegendDebug.Response> debugStart(LegendDebug.StartParams params)
    {
        return supplyAsync(() -> this.debugService.start(params));
    }

    @JsonRequest("legend/debug/continue")
    public CompletableFuture<LegendDebug.Response> debugContinue()
    {
        return supplyAsync(this.debugService::continueExecution);
    }

    @JsonRequest("legend/debug/stepIn")
    public CompletableFuture<LegendDebug.Response> debugStepIn()
    {
        return supplyAsync(this.debugService::stepIn);
    }

    @JsonRequest("legend/debug/stepOver")
    public CompletableFuture<LegendDebug.Response> debugStepOver()
    {
        return supplyAsync(this.debugService::stepOver);
    }

    @JsonRequest("legend/debug/stepOut")
    public CompletableFuture<LegendDebug.Response> debugStepOut()
    {
        return supplyAsync(this.debugService::stepOut);
    }

    @JsonRequest("legend/debug/evaluate")
    public CompletableFuture<LegendDebug.EvaluateResult> debugEvaluate(LegendDebug.EvaluateParams params)
    {
        return supplyAsync(() -> this.debugService.evaluate(params));
    }

    @JsonRequest("legend/debug/variables")
    public CompletableFuture<List<LegendDebug.Variable>> debugVariables(LegendDebug.VariablesParams params)
    {
        return supplyAsync(() -> this.debugService.variables(params));
    }

    @JsonRequest("legend/debug/stop")
    public CompletableFuture<LegendDebug.Response> debugStop()
    {
        return supplyAsync(this.debugService::stop);
    }

    @JsonRequest("legend/debug/dapEndpoint")
    public CompletableFuture<DapEndpoint> debugDapEndpoint()
    {
        return CompletableFuture.completedFuture(this.debugSocketServer.endpoint());
    }

    private static List<Path> extractWorkspaceRoots(InitializeParams params)
    {
        List<Path> roots = new ArrayList<>();
        List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null)
        {
            for (WorkspaceFolder folder : folders)
            {
                Path path = uriToPath(folder.getUri());
                if (path != null)
                {
                    roots.add(path);
                }
            }
        }
        if (roots.isEmpty() && params.getRootUri() != null)
        {
            Path path = uriToPath(params.getRootUri());
            if (path != null)
            {
                roots.add(path);
            }
        }
        return roots;
    }

    private static Path uriToPath(String uri)
    {
        if (uri == null || uri.isEmpty())
        {
            return null;
        }
        try
        {
            return Paths.get(URI.create(uri));
        }
        catch (Exception e)
        {
            LOGGER.warn("Cannot convert URI to path: {}", uri, e);
            return null;
        }
    }

    static List<String> extractClasspathRepositoryNames(InitializeParams params)
    {
        if (params == null)
        {
            return Collections.emptyList();
        }

        Object initializationOptions = params.getInitializationOptions();
        Object value = readOption(initializationOptions, "classpathRepositories");
        if (value == null)
        {
            value = readOption(readOption(initializationOptions, "server"), "classpathRepositories");
        }
        return toStringList(value);
    }

    private static Object readOption(Object options, String property)
    {
        if (options instanceof JsonObject)
        {
            JsonObject object = (JsonObject) options;
            return object.has(property) ? object.get(property) : null;
        }
        if (options instanceof Map<?, ?>)
        {
            return ((Map<?, ?>) options).get(property);
        }
        return null;
    }

    private static List<String> toStringList(Object value)
    {
        Set<String> result = new LinkedHashSet<>();
        if (value instanceof JsonArray)
        {
            for (JsonElement item : (JsonArray) value)
            {
                addStringValue(result, item);
            }
        }
        else if (value instanceof Iterable<?>)
        {
            for (Object item : (Iterable<?>) value)
            {
                addStringValue(result, item);
            }
        }
        else
        {
            addStringValue(result, value);
        }
        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(result));
    }

    private static void addStringValue(Set<String> result, Object value)
    {
        String stringValue = toStringValue(value);
        if (stringValue != null)
        {
            String trimmed = stringValue.trim();
            if (!trimmed.isEmpty())
            {
                result.add(trimmed);
            }
        }
    }

    private static String toStringValue(Object value)
    {
        if (value == null || "JsonNull".equals(value.getClass().getSimpleName()))
        {
            return null;
        }
        if (value instanceof String)
        {
            return (String) value;
        }
        if (value instanceof JsonElement)
        {
            JsonElement element = (JsonElement) value;
            return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : null;
        }
        return String.valueOf(value);
    }

    public static void main(String[] args) throws Exception
    {
        PrintStream originalOut = new PrintStream(
                new java.io.BufferedOutputStream(new java.io.FileOutputStream(java.io.FileDescriptor.out)), true);
        PrintStream stderrOut = new PrintStream(
                new java.io.BufferedOutputStream(new java.io.FileOutputStream(java.io.FileDescriptor.err)), true);
        System.setOut(stderrOut);
        System.setErr(stderrOut);

        java.security.CodeSource codeSource = LegendPureLspServer.class.getProtectionDomain().getCodeSource();
        String jarLocation = codeSource != null ? codeSource.getLocation().toString() : "unknown";
        System.err.println("[LSP] Running from: " + jarLocation);

        try
        {
            Class.forName("com.google.gson.Gson");
            Class.forName("com.google.gson.internal.bind.NumberTypeAdapter");
            Class.forName("org.eclipse.collections.impl.block.procedure.MinComparatorProcedure");
            Class.forName("org.eclipse.lsp4j.adapters.SymbolInformationTypeAdapter");
            Class.forName("org.eclipse.lsp4j.debug.services.IDebugProtocolServer");
            Class.forName("org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher");
            Class.forName("org.finos.legend.pure.lsp.HoverProvider");
            Class.forName("org.finos.legend.pure.lsp.NavigationProvider");
            Class.forName("org.finos.legend.pure.lsp.ReferencesProvider");
            Class.forName("org.finos.legend.pure.lsp.SemanticTokensProvider");
            Class.forName("org.finos.legend.pure.lsp.DocumentOutlineProvider");
            Class.forName("org.finos.legend.pure.lsp.PackageTreeProvider");
            Class.forName("org.finos.legend.pure.lsp.WorkspaceSymbolProvider");
            Class.forName("org.finos.legend.pure.lsp.TestFunctionProvider");
            Class.forName("org.finos.legend.pure.lsp.CompletionProvider");
            Class.forName("org.finos.legend.pure.lsp.FoldingRangeProvider");
            Class.forName("org.finos.legend.pure.lsp.PCTAdapterProvider");
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("[LSP-ERROR] Critical class not found: " + e.getMessage()
                    + ". Launch with -cp including the server jar, target/dependency/*, and any extension jars.");
            System.exit(1);
        }

        // Socket mode (--socket <port> or -Dlegend.lsp.socketPort=<port>): run as a standalone
        // daemon that listens on a TCP socket instead of reading System.in. This decouples the JVM
        // from any parent process's stdin pipe, so it survives the launcher/bridge going away and a
        // fresh client can reconnect to the same warm session. Without it, the JVM's lifecycle is
        // bound to whoever owns its stdin pipe (the classic stdio LSP model, kept as the default).
        int socketPort = resolveSocketPort(args);
        if (socketPort > 0)
        {
            runSocketMode(socketPort, stderrOut);
            return;
        }

        LegendPureLspServer server = new LegendPureLspServer();
        Launcher<LegendLanguageClient> launcher = new Launcher.Builder<LegendLanguageClient>()
                .setLocalService(server)
                .setRemoteInterface(LegendLanguageClient.class)
                .setInput(System.in)
                .setOutput(originalOut)
                .create();
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }

    static int resolveSocketPort(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if ("--socket".equals(args[i]) && (i + 1) < args.length)
            {
                try
                {
                    return Integer.parseInt(args[i + 1].trim());
                }
                catch (NumberFormatException ignored)
                {
                    return -1;
                }
            }
        }
        String prop = System.getProperty("legend.lsp.socketPort");
        if (prop != null && !prop.trim().isEmpty())
        {
            try
            {
                return Integer.parseInt(prop.trim());
            }
            catch (NumberFormatException ignored)
            {
                return -1;
            }
        }
        return -1;
    }

    static int resolveRequestPoolSize()
    {
        String prop = System.getProperty(REQUEST_POOL_SIZE_PROPERTY);
        if (prop == null || prop.trim().isEmpty())
        {
            return DEFAULT_REQUEST_POOL_SIZE;
        }
        try
        {
            int size = Integer.parseInt(prop.trim());
            if (size > 0)
            {
                return size;
            }
            LspLog.warn(REQUEST_POOL_SIZE_PROPERTY + "=" + prop + " must be positive; using default " + DEFAULT_REQUEST_POOL_SIZE);
        }
        catch (NumberFormatException e)
        {
            LspLog.warn("Invalid " + REQUEST_POOL_SIZE_PROPERTY + "=" + prop + "; using default " + DEFAULT_REQUEST_POOL_SIZE);
        }
        return DEFAULT_REQUEST_POOL_SIZE;
    }

    /**
     * Daemon mode: bind a TCP socket on 127.0.0.1:port and serve LSP JSON-RPC over accepted
     * connections instead of over System.in/out. ONE LegendPureLspServer (one warm Pure runtime) is
     * created up front and reused across every client connection, so a client can disconnect and
     * reconnect without losing the compiled session - the whole point of decoupling the JVM's
     * lifecycle from any single client.
     * <p>
     * Connections are accepted and served CONCURRENTLY (each on its own thread from
     * {@code connectionExecutor}), not one-at-a-time: this lets, e.g., an IntelliJ session and a
     * separate CLI dev-loop bridge both hold a live connection to the same warm daemon at once instead
     * of one blocking the other's accept. All graph mutation/read still funnels through
     * {@link LegendPureSession}'s {@code graphLock}, so concurrent requests from different connections
     * are safe at that level. Asynchronous pushes (diagnostics, {@code legend/statusChanged}, log
     * output, show-message) are broadcast to every currently-connected client via
     * {@link ClientBroadcaster} - registered in {@link #connect(LanguageClient)}, deregistered in
     * {@link #serveSocketConnection} once that connection ends - so an IntelliJ session and a
     * separately-connected CLI/agent bridge both keep receiving live updates. A concurrent
     * {@link DebugService} debug session remains a single active slot: two clients racing a debug
     * start will have one preempt the other (unrelated to, and not fixed by, the broadcast above).
     */
    private static void runSocketMode(int port, PrintStream stderrOut) throws Exception
    {
        runSocketMode(new LegendPureLspServer(), port);
    }

    /**
     * Same daemon loop as {@link #runSocketMode(int, PrintStream)}, but against a caller-supplied,
     * already-constructed server instance instead of always creating a fresh one. This lets a launcher
     * (e.g. a config-driven, IDE-runnable bootstrap) pre-configure the workspace via
     * {@link #preconfigureAndWarm(List, Set)} before any client connects, rather than waiting on the
     * first client's {@code initialize} request to supply workspace roots.
     */
    public static void runSocketMode(LegendPureLspServer server, int port) throws Exception
    {
        server.daemonMode = true;
        server.port = port;
        server.transport = "socket";
        ExecutorService connectionExecutor = Executors.newCachedThreadPool(r ->
        {
            Thread t = new Thread(r, "legend-pure-lsp-socket-connection");
            t.setDaemon(true);
            return t;
        });
        // Bind only on loopback: this is a local dev-loop daemon, never exposed off-box.
        try (ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1")))
        {
            System.err.println("[LSP] socket daemon listening on 127.0.0.1:" + port
                    + " (pid decoupled from any launcher; reconnectable; concurrent connections supported)");
            // Signal readiness to a spawner watching stderr for a fixed marker.
            System.err.println("[LSP] SOCKET_READY " + port);
            while (true)
            {
                Socket socket;
                try
                {
                    socket = serverSocket.accept();
                }
                catch (Exception e)
                {
                    System.err.println("[LSP] accept failed, daemon exiting: " + e.getMessage());
                    connectionExecutor.shutdownNow();
                    return;
                }
                System.err.println("[LSP] client connected from " + socket.getRemoteSocketAddress());
                connectionExecutor.submit(() -> serveSocketConnection(server, socket));
            }
        }
        finally
        {
            connectionExecutor.shutdownNow();
        }
    }

    private static void serveSocketConnection(LegendPureLspServer server, Socket socket)
    {
        LanguageClient remoteClient = null;
        try
        {
            socket.setTcpNoDelay(true);
            Launcher<LegendLanguageClient> launcher = new Launcher.Builder<LegendLanguageClient>()
                    .setLocalService(server)
                    .setRemoteInterface(LegendLanguageClient.class)
                    .setInput(socket.getInputStream())
                    .setOutput(socket.getOutputStream())
                    .create();
            remoteClient = launcher.getRemoteProxy();
            server.connect(remoteClient);
            // Blocks (on this connection's own thread) until this client disconnects (stream EOF).
            launcher.startListening().get();
        }
        catch (Exception e)
        {
            System.err.println("[LSP] client session ended: " + e.getMessage());
        }
        finally
        {
            if (remoteClient != null)
            {
                // This is the only disconnect signal LSP4J gives us - startListening().get() above
                // returning/throwing on this connection's own thread - so deregister right here.
                server.disconnect(remoteClient);
            }
            try
            {
                socket.close();
            }
            catch (Exception ignored)
            {
            }
            System.err.println("[LSP] client disconnected (session kept warm)");
        }
    }
}
