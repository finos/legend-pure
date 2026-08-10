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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.finos.legend.pure.lsp.protocol.SyncWorkspaceParams;
import org.finos.legend.pure.lsp.protocol.SyncWorkspaceResult;
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

        applyChanges(session, changes);
    }

    /**
     * Shared tail for anything that hands a batch of externally-derived {@link LegendPureSession.FileChange}s
     * to the mutation service - today {@link #handleFileChanges} (client-reported watched-file events) and
     * {@link #syncWorkspace} (server-detected drift). Compiles the batch, then on success clears stale
     * diagnostics and rebuilds the symbol index; on a non-internal error, attributes it to one of the
     * changed files so it's visible somewhere rather than silently dropped.
     */
    private LegendPureSession.CompileResult applyChanges(LegendPureSession session, List<LegendPureSession.FileChange> changes)
    {
        LegendPureSession.CompileResult result = this.server.getMutationService().applyBulkChangesAndCompile(changes);

        if (result.isInternalError())
        {
            LOGGER.error("Internal error after file changes, triggering recovery", result.getError());
            this.server.triggerRecovery();
            return result;
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
        else if (result.getError() != null)
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
        return result;
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

    /**
     * Applies only the files the {@link WorkspaceDriftWatcher} (or an explicit uri selection) says are
     * actually out of sync, instead of a full {@link #executeCommand reindex} - see the watcher's javadoc
     * for why this stays a deliberate, user/client-triggered call rather than something the watcher fires
     * on its own.
     */
    SyncWorkspaceResult syncWorkspace(SyncWorkspaceParams params)
    {
        LegendPureSession session = this.server.getSession();
        if (session == null || !session.isInitialized())
        {
            return SyncWorkspaceResult.failure("Runtime not initialized");
        }
        if (this.server.getMutationService() == null)
        {
            return SyncWorkspaceResult.failure("Mutation service not available");
        }

        Collection<String> sourceIds = (params == null || params.getUris() == null || params.getUris().isEmpty())
                ? this.server.getDriftWatcher().getDirtySourceIds()
                : deriveSourceIds(params.getUris());

        if (sourceIds.isEmpty())
        {
            return SyncWorkspaceResult.success(0, 0, 0);
        }

        RepositoryScanner scanner = this.server.getRepositoryScanner();
        List<LegendPureSession.FileChange> changes = new ArrayList<>();
        int created = 0;
        int modified = 0;
        int deleted = 0;
        for (String sourceId : sourceIds)
        {
            Path path = scanner.resolve(sourceId);
            if (path == null)
            {
                changes.add(new LegendPureSession.FileChange(sourceId, null, LegendPureSession.FileChangeType.DELETE));
                deleted++;
                continue;
            }
            String content;
            try
            {
                content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                LOGGER.warn("Sync: failed to read {}: {}", path, e.getMessage());
                continue;
            }
            boolean existedBefore = session.getPureRuntime().getSourceById(sourceId) != null;
            changes.add(new LegendPureSession.FileChange(sourceId, content, LegendPureSession.FileChangeType.CREATE_OR_MODIFY));
            if (existedBefore)
            {
                modified++;
            }
            else
            {
                created++;
            }
        }

        // Only ever clear entries that were actually, successfully applied - a failed compile or an
        // open-document conflict means the drift is still real, so it must stay in the watcher's
        // dirty set (and stay visible to the next notification) rather than being silently dropped.
        List<LegendPureSession.FileChange> filtered = filterOpenDocumentConflicts(changes);
        if (filtered.isEmpty())
        {
            return SyncWorkspaceResult.success(0, 0, 0);
        }

        LegendPureSession.CompileResult result = applyChanges(session, filtered);

        if (result.isInternalError())
        {
            return SyncWorkspaceResult.failure("Internal error during sync; recovery triggered");
        }
        if (!result.isSuccess() && result.getError() != null)
        {
            return SyncWorkspaceResult.failure(result.getError().getMessage());
        }

        // Only the entries actually in the applied (post-filter) batch resolved - anything dropped by
        // filterOpenDocumentConflicts is still genuinely out of sync and must stay reported as such.
        List<String> appliedSourceIds = new ArrayList<>(filtered.size());
        for (LegendPureSession.FileChange change : filtered)
        {
            appliedSourceIds.add(change.getSourceId());
        }
        this.server.getDriftWatcher().clear(appliedSourceIds);
        this.server.getDriftWatcher().publishNow();

        LspLog.info("Synced workspace from disk: " + created + " created, " + modified + " modified, " + deleted + " deleted");
        return SyncWorkspaceResult.success(created, modified, deleted);
    }

    private Collection<String> deriveSourceIds(List<String> uris)
    {
        Set<String> sourceIds = new LinkedHashSet<>();
        for (String uri : uris)
        {
            // PureAutoSyncListener passes every changed .pure path unfiltered, including genuine
            // non-module fixtures (see UriMapper#deriveSourceId) - toSourceId(uri) legitimately
            // returns null for those, and a null sourceId must never enter the changes pipeline: it
            // would otherwise reach applyChanges/the mutation service as a FileChange with no real
            // source ID to compile against.
            String sourceId = this.server.getUriMapper().toSourceId(uri);
            if (sourceId != null)
            {
                sourceIds.add(sourceId);
            }
        }
        return sourceIds;
    }
}
