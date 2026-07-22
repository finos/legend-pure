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

package org.finos.legend.pure.lsp.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.LspLog;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.WorkspaceSymbolProvider;
import org.finos.legend.pure.lsp.diagnostics.DiagnosticService;
import org.finos.legend.pure.lsp.mutation.SourceMutationService;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LspState;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PureRuntimeManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureRuntimeManager.class);
    private static final int MAX_RECOVERY_ATTEMPTS = 3;

    private final RepositoryScanner repositoryScanner;
    private final UriMapper uriMapper;
    private final WorkspaceSymbolProvider symbolProvider;
    private final Runnable openDocumentCompiler;
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);

    private volatile LegendLanguageClient client;
    private volatile LegendPureSession session;
    private volatile SourceMutationService mutationService;
    private volatile int recoveryAttempts;
    private volatile List<Path> workspaceRoots = new ArrayList<>();
    private volatile Set<String> classpathRepositoryNames = Collections.emptySet();
    private volatile LspState state = LspState.CREATED;
    private volatile String statusMessage = "";

    public PureRuntimeManager(RepositoryScanner repositoryScanner, UriMapper uriMapper,
                              WorkspaceSymbolProvider symbolProvider, Runnable openDocumentCompiler)
    {
        this.repositoryScanner = repositoryScanner;
        this.uriMapper = uriMapper;
        this.symbolProvider = symbolProvider;
        this.openDocumentCompiler = openDocumentCompiler;
    }

    public void setClient(LegendLanguageClient client)
    {
        this.client = client;
    }

    public void configure(List<Path> workspaceRoots, Set<String> classpathRepositoryNames)
    {
        this.workspaceRoots = workspaceRoots == null ? Collections.emptyList() : new ArrayList<>(workspaceRoots);
        this.classpathRepositoryNames = classpathRepositoryNames == null || classpathRepositoryNames.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(classpathRepositoryNames));
        setStatus(LspState.INITIALIZING, "Starting");
    }

    public void initialize()
    {
        try
        {
            show(MessageType.Info, "Pure LSP: initializing runtime...");
            initializeRuntime("Ready");
            this.recoveryAttempts = 0;
            show(MessageType.Info, "Pure LSP: ready (" + repositoryCount() + " repos, "
                    + this.symbolProvider.size() + " symbols)");
            LspLog.info("Pure LSP initialized successfully - " + this.symbolProvider.size() + " symbols indexed");
        }
        catch (Exception e)
        {
            setStatus(LspState.FAILED, e.getMessage());
            LspLog.error("Pure LSP initialization FAILED: " + e.getMessage());
            e.printStackTrace(System.err);
            publishFailureDiagnostic(e);
            show(MessageType.Error, "Pure LSP failed: " + e.getMessage());
        }
    }

    public void reindex()
    {
        if (this.session == null)
        {
            return;
        }
        show(MessageType.Info, "Pure LSP: reindexing...");
        try
        {
            setStatus(LspState.REINDEXING, "Reindexing");
            initializeRuntime("Reindex complete");
            show(MessageType.Info, "Pure LSP: reindex complete");
        }
        catch (Exception e)
        {
            setStatus(LspState.FAILED, e.getMessage());
            LOGGER.error("Reindex failed", e);
            publishFailureDiagnostic(e);
            show(MessageType.Error, "Pure LSP reindex failed: " + e.getMessage());
        }
    }

    public void triggerRecovery()
    {
        if (!this.recoveryInProgress.compareAndSet(false, true))
        {
            LOGGER.warn("Recovery already in progress; ignoring duplicate recovery request");
            return;
        }
        if (this.recoveryAttempts >= MAX_RECOVERY_ATTEMPTS)
        {
            this.recoveryInProgress.set(false);
            setStatus(LspState.FAILED, "Manual restart required");
            LOGGER.error("Max recovery attempts ({}) reached. Manual restart required.", MAX_RECOVERY_ATTEMPTS);
            show(MessageType.Error, "Pure LSP: recovery failed " + MAX_RECOVERY_ATTEMPTS + " times. Please restart.");
            return;
        }
        this.recoveryAttempts++;

        try
        {
            setStatus(LspState.RECOVERING, "Recovering");
            LOGGER.warn("Triggering automatic recovery (attempt {}/{})...", this.recoveryAttempts, MAX_RECOVERY_ATTEMPTS);
            initializeRuntime("Recovered");
            this.recoveryAttempts = 0;
            show(MessageType.Info, "Pure LSP: recovered");
        }
        catch (Exception e)
        {
            setStatus(LspState.FAILED, e.getMessage());
            LOGGER.error("Recovery failed", e);
            publishFailureDiagnostic(e);
            show(MessageType.Error, "Pure LSP recovery failed: " + e.getMessage());
        }
        finally
        {
            this.recoveryInProgress.set(false);
            publishStatus();
        }
    }

    private void initializeRuntime(String readyMessage)
    {
        long start = System.currentTimeMillis();
        this.uriMapper.clear();
        rescanWorkspaceRoots();

        LegendPureSession nextSession = this.session == null ? new LegendPureSession() : this.session;
        nextSession.setClasspathRepositoryNames(this.classpathRepositoryNames);
        if (nextSession.isInitialized())
        {
            nextSession.reinitialize();
        }
        else
        {
            nextSession.initialize(this.repositoryScanner, this.classpathRepositoryNames);
        }
        this.session = nextSession;
        this.mutationService = nextSession.getMutationService();
        this.uriMapper.setPureRuntime(nextSession.getPureRuntime());

        this.symbolProvider.buildIndex(nextSession.getPureRuntime());
        this.openDocumentCompiler.run();

        long elapsed = System.currentTimeMillis() - start;
        setStatus(LspState.READY, readyMessage + " in " + elapsed + "ms");
    }

    private void publishFailureDiagnostic(Exception e)
    {
        LegendLanguageClient currentClient = this.client;
        if (currentClient == null)
        {
            return;
        }

        DiagnosticService diagnosticService = new DiagnosticService(currentClient, this.uriMapper);
        String errorUri = diagnosticService.resolveErrorUri(e);
        if (errorUri != null)
        {
            diagnosticService.publishException(errorUri, e, this.session);
        }
    }

    public void rescanWorkspaceRoots()
    {
        this.repositoryScanner.clear();
        if (!this.workspaceRoots.isEmpty())
        {
            this.repositoryScanner.scan(this.workspaceRoots);
            this.uriMapper.setRepositoryScanner(this.repositoryScanner);
            LspLog.info("Mapped " + this.repositoryScanner.getMappings().size()
                    + " repositories to filesystem paths");
        }
        else
        {
            LspLog.warn("No workspace roots provided; source ID resolution will be limited");
        }
    }

    public LegendPureSession getSession()
    {
        return this.session;
    }

    public SourceMutationService getMutationService()
    {
        return this.mutationService;
    }

    public LspStatus currentStatus()
    {
        return new LspStatus(
                this.state,
                repositoryCount(),
                this.symbolProvider.size(),
                this.recoveryAttempts,
                this.recoveryInProgress.get(),
                this.statusMessage
        );
    }

    public void shutdown()
    {
        setStatus(LspState.SHUTDOWN, "Shutdown");
    }

    private void setStatus(LspState state, String message)
    {
        this.state = state;
        this.statusMessage = message == null ? "" : message;
        publishStatus();
    }

    private int repositoryCount()
    {
        return this.repositoryScanner.getMappings().size();
    }

    private void publishStatus()
    {
        LegendLanguageClient currentClient = this.client;
        if (currentClient != null)
        {
            try
            {
                currentClient.statusChanged(currentStatus());
            }
            catch (Exception e)
            {
                LOGGER.debug("Failed to publish LSP status notification", e);
            }
        }
    }

    private void show(MessageType type, String message)
    {
        LegendLanguageClient currentClient = this.client;
        if (currentClient != null)
        {
            currentClient.showMessage(new MessageParams(type, message));
        }
    }
}
