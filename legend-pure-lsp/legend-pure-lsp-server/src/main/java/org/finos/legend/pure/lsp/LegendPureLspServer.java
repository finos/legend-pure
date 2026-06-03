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
import org.finos.legend.pure.lsp.protocol.DapEndpoint;
import org.finos.legend.pure.lsp.protocol.ExecuteGoResult;
import org.finos.legend.pure.lsp.protocol.LegendDebug;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LspStatus;
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
        // VS Code sends $/setTrace on every connection; the default LSP4J
        // implementation throws UnsupportedOperationException.
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
            synchronized (session)
            {
                return PackageTreeProvider.getChildren(session.getPureRuntime(), this.uriMapper, packagePath);
            }
        });
    }

    @JsonRequest("legend/executeGo")
    public CompletableFuture<ExecuteGoResult> executeGo()
    {
        return supplyAsync(() ->
        {
            LegendPureSession session = getSession();
            if (session == null || !session.isInitialized())
            {
                return new ExecuteGoResult(false, "Runtime not initialized", null);
            }
            synchronized (session)
            {
                LegendPureSession.ExecuteResult result = session.executeGo();
                return new ExecuteGoResult(result.isSuccess(), result.getError(), result.getOutput());
            }
        });
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
