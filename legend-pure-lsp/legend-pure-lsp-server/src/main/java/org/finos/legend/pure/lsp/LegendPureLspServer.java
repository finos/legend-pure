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
import org.finos.legend.pure.lsp.protocol.SetOptionParams;
import org.finos.legend.pure.lsp.protocol.SetOptionResult;
import org.finos.legend.pure.lsp.runtime.PureRuntimeManager;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendPureLspServer implements LanguageServer, LanguageClientAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureLspServer.class);
    private static final String VERSION = "0.3.0-2026-04-01";

    private LanguageClient rawClient;
    private LegendLanguageClient client;
    private DiagnosticService diagnosticService;

    private final UriMapper uriMapper = new UriMapper();
    private final RepositoryScanner repositoryScanner = new RepositoryScanner();
    private final WorkspaceSymbolProvider symbolProvider = new WorkspaceSymbolProvider();
    private final LegendTextDocumentService textDocumentService;
    private final LegendWorkspaceService workspaceService;
    private final PureRuntimeManager runtimeManager;
    private final DebugService debugService;
    private final LegendDebugSocketServer debugSocketServer;
    private final ExecutorService requestExecutor = Executors.newFixedThreadPool(4, r ->
    {
        Thread t = new Thread(r, "legend-pure-lsp-request");
        t.setDaemon(true);
        return t;
    });

    public LegendPureLspServer()
    {
        this.textDocumentService = new LegendTextDocumentService(this);
        this.runtimeManager = new PureRuntimeManager(
                this.repositoryScanner,
                this.uriMapper,
                this.symbolProvider,
                this.textDocumentService::compileOpenDocuments);
        this.debugService = new DebugService(
                this.runtimeManager,
                this.repositoryScanner,
                this.uriMapper,
                this.textDocumentService::getOpenDocumentSourceSnapshot);
        this.debugSocketServer = new LegendDebugSocketServer(this.debugService);
        this.workspaceService = new LegendWorkspaceService(this);
    }

    @Override
    public void connect(LanguageClient client)
    {
        this.rawClient = client;
        this.client = (client instanceof LegendLanguageClient)
                ? (LegendLanguageClient) client
                : new LegendLanguageClientAdapter(client);
        this.runtimeManager.setClient(this.client);
        this.diagnosticService = new DiagnosticService(client, this.uriMapper);
    }

    LanguageClient getClient()
    {
        return this.rawClient;
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

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<InitializeResult> initialize(InitializeParams params)
    {
        List<Path> workspaceRoots = extractWorkspaceRoots(params);
        Set<String> classpathRepositoryNames = new LinkedHashSet<>(extractClasspathRepositoryNames(params));
        this.runtimeManager.configure(workspaceRoots, classpathRepositoryNames);

        LspLog.info("Legend Pure LSP v" + VERSION + " starting");
        LspLog.info("Workspace roots: " + workspaceRoots);

        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        caps.setCompletionProvider(new org.eclipse.lsp4j.CompletionOptions(false, Arrays.asList(":", "$", ".")));
        caps.setDefinitionProvider(true);
        caps.setReferencesProvider(true);
        caps.setHoverProvider(true);
        caps.setDocumentSymbolProvider(true);
        caps.setWorkspaceSymbolProvider(true);
        caps.setCodeActionProvider(new CodeActionOptions(Collections.singletonList(org.eclipse.lsp4j.CodeActionKind.QuickFix)));

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
        runAsync(this.runtimeManager::initialize);
    }

    void triggerRecovery()
    {
        runAsync(this.runtimeManager::triggerRecovery);
    }

    CompletableFuture<Void> reindex()
    {
        return runAsync(this.runtimeManager::reindex);
    }

    @Override
    public void setTrace(SetTraceParams params)
    {
    }

    @Override
    public CompletableFuture<Object> shutdown()
    {
        this.debugService.shutdown();
        this.debugSocketServer.close();
        this.textDocumentService.shutdown();
        this.runtimeManager.shutdown();
        this.requestExecutor.shutdownNow();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit()
    {
        System.exit(0);
    }

    @JsonRequest("legend/status")
    public CompletableFuture<LspStatus> status()
    {
        return CompletableFuture.completedFuture(this.runtimeManager.currentStatus());
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
            LegendPureSession.ExecuteResult result = session.executeFunction(params.getFunction());
            return new ExecuteGoResult(result.isSuccess(), result.getError(), result.getOutput(), null);
        });
    }

    static final String PURE_OPTION_PREFIX = "pure.options.";

    /**
     * Sets or clears a Pure runtime option in this JVM so that isOptionSet('&lt;name&gt;') reflects it
     * live, without restarting the server. The Pure runtime resolves options through
     * RuntimeOptions.systemPropertyOptions(PURE_OPTION_PREFIX) - i.e. isOptionSet("X") is
     * Boolean.getBoolean("pure.options.X"), read on every call - so toggling the system property here
     * takes effect immediately for subsequent go()/execute() runs in this session.
     * <p>
     * value == true  -&gt; System.setProperty("pure.options." + name, "true")
     * value == false -&gt; System.clearProperty("pure.options." + name)
     * (clearing rather than storing "false" keeps the property table clean; Boolean.getBoolean
     * returns false for an absent property either way.)
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
            String name = params.getName().trim();
            String key = PURE_OPTION_PREFIX + name;
            if (params.isValue())
            {
                System.setProperty(key, "true");
            }
            else
            {
                System.clearProperty(key);
            }
            boolean effective = Boolean.getBoolean(key);
            LspLog.info("setOption: " + name + " -> " + effective + " (isOptionSet('" + name + "') now returns " + effective + ")");
            return new SetOptionResult(true, name, effective, null);
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

            java.util.concurrent.locks.Lock writeLock = session.graphWriteLock();
            writeLock.lock();
            try
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
            finally
            {
                writeLock.unlock();
            }
        });
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
            changes.add(new LegendPureSession.FileChange(sourceId, file.getContent(), LegendPureSession.FileChangeType.CREATE_OR_MODIFY));
        }

        // Whole batch under the write lock: the compile, the symbol-index rebuild, and the diagnostics
        // refresh are one atomic unit that no mutation can interleave - which is what lets executeGo /
        // execute call this with no outer lock. applyBulkChangesAndCompile re-takes the (reentrant)
        // write lock internally.
        java.util.concurrent.locks.Lock writeLock = session.graphWriteLock();
        writeLock.lock();
        try
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
        finally
        {
            writeLock.unlock();
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
            Class.forName("org.finos.legend.pure.lsp.CompletionProvider");
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

    /**
     * Daemon mode: bind a TCP socket on 127.0.0.1:port and serve LSP JSON-RPC over accepted
     * connections instead of over System.in/out. ONE LegendPureLspServer (one warm Pure runtime) is
     * created up front and reused across every client connection, so a client (the bridge) can
     * disconnect and reconnect without losing the compiled session - the whole point of decoupling
     * the JVM's lifecycle from any single client. Connections are handled one at a time (the bridge
     * holds a single long-lived connection); when a client drops, we loop back to accept the next.
     */
    private static void runSocketMode(int port, PrintStream stderrOut) throws Exception
    {
        LegendPureLspServer server = new LegendPureLspServer();
        // Bind only on loopback: this is a local dev-loop daemon, never exposed off-box.
        try (ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1")))
        {
            System.err.println("[LSP] socket daemon listening on 127.0.0.1:" + port
                    + " (pid decoupled from any launcher; reconnectable)");
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
                    return;
                }
                socket.setTcpNoDelay(true);
                System.err.println("[LSP] client connected from " + socket.getRemoteSocketAddress());
                try
                {
                    Launcher<LegendLanguageClient> launcher = new Launcher.Builder<LegendLanguageClient>()
                            .setLocalService(server)
                            .setRemoteInterface(LegendLanguageClient.class)
                            .setInput(socket.getInputStream())
                            .setOutput(socket.getOutputStream())
                            .create();
                    server.connect(launcher.getRemoteProxy());
                    // Blocks until this client disconnects (stream EOF); then we loop to accept again.
                    launcher.startListening().get();
                }
                catch (Exception e)
                {
                    System.err.println("[LSP] client session ended: " + e.getMessage());
                }
                finally
                {
                    try
                    {
                        socket.close();
                    }
                    catch (Exception ignored)
                    {
                    }
                    System.err.println("[LSP] client disconnected; awaiting reconnect (session kept warm)");
                }
            }
        }
    }

    private static class LegendLanguageClientAdapter implements LegendLanguageClient
    {
        private final LanguageClient delegate;

        private LegendLanguageClientAdapter(LanguageClient delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void telemetryEvent(Object object)
        {
            this.delegate.telemetryEvent(object);
        }

        @Override
        public void publishDiagnostics(org.eclipse.lsp4j.PublishDiagnosticsParams diagnostics)
        {
            this.delegate.publishDiagnostics(diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams)
        {
            this.delegate.showMessage(messageParams);
        }

        @Override
        public CompletableFuture<org.eclipse.lsp4j.MessageActionItem> showMessageRequest(org.eclipse.lsp4j.ShowMessageRequestParams requestParams)
        {
            return this.delegate.showMessageRequest(requestParams);
        }

        @Override
        public void logMessage(MessageParams message)
        {
            this.delegate.logMessage(message);
        }

        @Override
        public void statusChanged(LspStatus status)
        {
        }
    }
}
