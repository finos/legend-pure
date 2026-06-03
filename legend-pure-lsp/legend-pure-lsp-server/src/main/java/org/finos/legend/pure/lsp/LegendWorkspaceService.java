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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendWorkspaceService implements WorkspaceService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendWorkspaceService.class);
    static final String CMD_REINDEX = "legend.reindexWorkspace";

    private final LegendPureLspServer server;

    LegendWorkspaceService(LegendPureLspServer server)
    {
        this.server = server;
    }

    private static final int MAX_WORKSPACE_SYMBOLS = 500;

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params)
    {
        return this.server.supplyAsync(() ->
        {
            LegendPureSession session = this.server.getSession();
            if (session == null || !session.isInitialized())
            {
                return Either.forLeft(Collections.emptyList());
            }

            List<SymbolInformation> symbols = this.server.getSymbolProvider().search(
                    this.server.getUriMapper(),
                    params.getQuery(),
                    MAX_WORKSPACE_SYMBOLS
            );
            return Either.<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>forLeft(symbols);
        });
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params)
    {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params)
    {
        this.server.runAsync(() -> handleFileChanges(params));
    }

    private void handleFileChanges(DidChangeWatchedFilesParams params)
    {
        LegendPureSession session = this.server.getSession();
        if (session == null || !session.isInitialized())
        {
            return;
        }

        FileChangeHandler handler = new FileChangeHandler(this.server.getUriMapper());
        List<LegendPureSession.FileChange> changes = handler.toFileChanges(params.getChanges());

        if (changes.isEmpty())
        {
            return;
        }

        changes = filterOpenDocumentConflicts(changes);
        if (changes.isEmpty())
        {
            return;
        }

        if (this.server.getMutationService() == null)
        {
            return;
        }

        LegendPureSession.CompileResult result = this.server.getMutationService().applyBulkChangesAndCompile(changes);

        if (result.isInternalError())
        {
            LOGGER.error("Internal error after file changes, triggering recovery", result.getError());
            this.server.triggerRecovery();
            return;
        }

        if (result.isSuccess())
        {
            for (LegendPureSession.FileChange change : changes)
            {
                String uri = this.server.getUriMapper().toUri(change.getSourceId());
                if (uri != null)
                {
                    this.server.getDiagnosticService().clear(uri);
                }
            }
            this.server.getSymbolProvider().buildIndex(session.getPureRuntime());
        }
        else if (!result.isInternalError() && result.getError() != null)
        {
            String fallbackUri = null;
            for (LegendPureSession.FileChange change : changes)
            {
                fallbackUri = this.server.getUriMapper().toUri(change.getSourceId());
                if (fallbackUri != null)
                {
                    break;
                }
            }
            if (fallbackUri != null)
            {
                this.server.getDiagnosticService().publishException(fallbackUri, result.getError(), session);
            }
        }
    }

    private List<LegendPureSession.FileChange> filterOpenDocumentConflicts(List<LegendPureSession.FileChange> changes)
    {
        LegendTextDocumentService textDocumentService = (LegendTextDocumentService) this.server.getTextDocumentService();
        List<LegendPureSession.FileChange> filtered = new ArrayList<>(changes.size());
        for (LegendPureSession.FileChange change : changes)
        {
            String uri = this.server.getUriMapper().toUri(change.getSourceId());
            if (uri != null && textDocumentService.hasOpenDocument(uri))
            {
                String openContent = textDocumentService.getOpenDocumentContent(uri);
                if (change.getType() == LegendPureSession.FileChangeType.DELETE ||
                        !Objects.equals(openContent, change.getContent()))
                {
                    LspLog.debug("Ignoring disk change for open document: " + uri);
                    continue;
                }
            }
            filtered.add(change);
        }
        return filtered;
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params)
    {
        if (CMD_REINDEX.equals(params.getCommand()))
        {
            return this.server.reindex().thenApply(ignored -> null);
        }
        return CompletableFuture.completedFuture(null);
    }
}
