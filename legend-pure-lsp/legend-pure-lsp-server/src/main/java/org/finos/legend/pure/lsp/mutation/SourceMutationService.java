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

package org.finos.legend.pure.lsp.mutation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.LspLog;
import org.finos.legend.pure.lsp.OverlayWorkspaceCodeStorage;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.exception.PureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceMutationService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceMutationService.class);

    private final LegendPureSession session;

    public SourceMutationService(LegendPureSession session)
    {
        this.session = session;
    }

    public LegendPureSession.CompileResult modifyAndCompile(String sourceId, String content)
    {
        synchronized (this.session)
        {
            if (!this.session.isInitialized())
            {
                return LegendPureSession.CompileResult.notReady();
            }
            PureRuntime runtime = this.session.getPureRuntime();
            String effectiveSourceId = sourceId;
            SourceSnapshot snapshot = null;
            try
            {
                SourceMutation mutation;
                String resolvedId = this.session.resolveSourceId(sourceId);

                if (resolvedId == null && sourceId.startsWith("/"))
                {
                    try
                    {
                        runtime.loadSourceIfLoadable(sourceId);
                        resolvedId = sourceId;
                    }
                    catch (Exception ignored)
                    {
                    }
                }

                if (resolvedId == null && !sourceId.startsWith("/") && !sourceId.contains("/"))
                {
                    String matchingSuffix = "/" + sourceId;
                    for (Source source : runtime.getSourceRegistry().getSources())
                    {
                        if (source.getId().endsWith(matchingSuffix))
                        {
                            resolvedId = source.getId();
                            LOGGER.info("Matched bare filename '{}' to storage source '{}'", sourceId, resolvedId);
                            break;
                        }
                    }
                }
                effectiveSourceId = resolvedId == null ? sourceId : resolvedId;
                snapshot = snapshot(effectiveSourceId);

                if (resolvedId != null)
                {
                    Source existingSource = runtime.getSourceById(resolvedId);
                    if (existingSource != null && existingSource.isImmutable())
                    {
                        LspLog.debug("Skipping modification of immutable source: " + resolvedId);
                        return LegendPureSession.CompileResult.success(Collections.emptyList());
                    }

                    runtime.modify(resolvedId, content);
                    mutation = runtime.compile();
                }
                else if (isOverlayBackedSource(runtime, sourceId))
                {
                    runtime.getCodeStorage().writeContent(sourceId, content);
                    runtime.loadSourceIfLoadable(sourceId);
                    Source workspaceSource = runtime.getSourceById(sourceId);
                    if (workspaceSource == null)
                    {
                        throw new IllegalArgumentException("Could not load workspace source: " + sourceId);
                    }
                    if (workspaceSource.isImmutable())
                    {
                        return LegendPureSession.CompileResult.success(Collections.emptyList());
                    }
                    mutation = runtime.compile();
                }
                else
                {
                    String inMemoryId = sourceId.startsWith("/") ? sourceId.substring(1) : sourceId;
                    effectiveSourceId = inMemoryId;
                    snapshot = snapshot(effectiveSourceId);
                    mutation = runtime.createInMemoryAndCompile(Tuples.pair(inMemoryId, content));
                }
                return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
            }
            catch (Exception e)
            {
                return restoreAfterFailure(Collections.singletonList(snapshot == null ? snapshot(effectiveSourceId) : snapshot), e);
            }
        }
    }

    public LegendPureSession.CompileResult applyBulkChangesAndCompile(List<LegendPureSession.FileChange> changes)
    {
        synchronized (this.session)
        {
            if (!this.session.isInitialized())
            {
                return LegendPureSession.CompileResult.notReady();
            }
            PureRuntime runtime = this.session.getPureRuntime();
            List<SourceSnapshot> snapshots = snapshot(changes);
            try
            {
                for (LegendPureSession.FileChange change : changes)
                {
                    switch (change.getType())
                    {
                        case DELETE:
                            if (runtime.getSourceById(change.getSourceId()) != null)
                            {
                                runtime.delete(change.getSourceId());
                            }
                            break;
                        case CREATE_OR_MODIFY:
                            if (runtime.getSourceById(change.getSourceId()) == null && isOverlayBackedSource(runtime, change.getSourceId()))
                            {
                                runtime.getCodeStorage().writeContent(change.getSourceId(), change.getContent());
                            }
                            if (runtime.getSourceById(change.getSourceId()) == null && change.getSourceId().startsWith("/"))
                            {
                                try
                                {
                                    runtime.loadSourceIfLoadable(change.getSourceId());
                                }
                                catch (Exception ignored)
                                {
                                }
                            }
                            Source bulkSource = runtime.getSourceById(change.getSourceId());
                            if (bulkSource != null && bulkSource.isImmutable())
                            {
                                break;
                            }
                            if (bulkSource == null)
                            {
                                runtime.createInMemorySource(change.getSourceId(), change.getContent());
                            }
                            else
                            {
                                runtime.modify(change.getSourceId(), change.getContent());
                            }
                            break;
                    }
                }
                SourceMutation mutation = runtime.compile();
                clearOverlayForAcceptedDiskChanges(changes);
                return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
            }
            catch (Exception e)
            {
                return restoreAfterFailure(snapshots, e);
            }
        }
    }

    public LegendPureSession.CompileResult restoreFromDisk(String sourceId)
    {
        synchronized (this.session)
        {
            if (!this.session.isInitialized() || sourceId == null)
            {
                return LegendPureSession.CompileResult.notReady();
            }
            PureRuntime runtime = this.session.getPureRuntime();
            SourceSnapshot snapshot = snapshot(sourceId);
            try
            {
                String resolvedId = this.session.resolveSourceId(sourceId);
                String effectiveId = resolvedId == null ? sourceId : resolvedId;
                Source source = resolvedId == null ? null : runtime.getSourceById(resolvedId);
                if (source != null && source.isImmutable())
                {
                    return LegendPureSession.CompileResult.success(Collections.emptyList());
                }
                if (source != null && source.isInMemory())
                {
                    runtime.delete(effectiveId);
                    SourceMutation mutation = runtime.compile();
                    return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
                }

                OverlayWorkspaceCodeStorage overlayStorage = overlayStorage(runtime, effectiveId);
                if (overlayStorage != null)
                {
                    overlayStorage.clearOverlay(effectiveId);
                }

                if (overlayStorage != null && !runtime.getCodeStorage().exists(effectiveId))
                {
                    if (source != null)
                    {
                        runtime.delete(effectiveId);
                    }
                    SourceMutation mutation = runtime.compile();
                    return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
                }

                if (source == null)
                {
                    runtime.loadSourceIfLoadable(effectiveId);
                    source = runtime.getSourceById(effectiveId);
                    if (source == null)
                    {
                        return LegendPureSession.CompileResult.success(Collections.emptyList());
                    }
                }
                String diskContent = runtime.getCodeStorage().getContentAsText(effectiveId);
                if (!diskContent.equals(source.getContent()))
                {
                    runtime.modify(effectiveId, diskContent);
                }
                SourceMutation mutation = runtime.compile();
                clearOverlayIfDiskMatches(effectiveId, diskContent);
                return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
            }
            catch (Exception e)
            {
                LspLog.debug("restoreFromDisk failed for " + sourceId + ": " + e.getMessage());
                return restoreAfterFailure(Collections.singletonList(snapshot), e);
            }
        }
    }

    private LegendPureSession.CompileResult restoreAfterFailure(List<SourceSnapshot> snapshots, Exception originalError)
    {
        return restoreSnapshots(snapshots)
                ? toCompileResult(originalError)
                : LegendPureSession.CompileResult.error(originalError, true);
    }

    private SourceSnapshot snapshot(String sourceId)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        String resolvedId = this.session.resolveSourceId(sourceId);
        String effectiveId = resolvedId == null ? sourceId : resolvedId;
        Source source = resolvedId == null ? null : runtime.getSourceById(resolvedId);
        OverlayWorkspaceCodeStorage overlayStorage = overlayStorage(runtime, effectiveId);
        OverlayWorkspaceCodeStorage.OverlaySnapshot overlaySnapshot = overlayStorage == null ? null : overlayStorage.snapshot(effectiveId);
        if (source != null && source.isImmutable())
        {
            return SourceSnapshot.immutable(effectiveId, overlayStorage, overlaySnapshot);
        }
        return source == null
                ? SourceSnapshot.missing(effectiveId, overlayStorage, overlaySnapshot)
                : SourceSnapshot.existing(effectiveId, source.getContent(), overlayStorage, overlaySnapshot);
    }

    private List<SourceSnapshot> snapshot(List<LegendPureSession.FileChange> changes)
    {
        List<SourceSnapshot> snapshots = new ArrayList<>();
        for (LegendPureSession.FileChange change : changes)
        {
            snapshots.add(snapshot(change.getSourceId()));
        }
        return snapshots;
    }

    private boolean restoreSnapshots(List<SourceSnapshot> snapshots)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        try
        {
            for (int i = snapshots.size() - 1; i >= 0; i--)
            {
                SourceSnapshot snapshot = snapshots.get(i);
                if (snapshot.immutable)
                {
                    continue;
                }
                if (snapshot.overlayStorage != null)
                {
                    snapshot.overlayStorage.restore(snapshot.overlaySnapshot);
                }
                if (snapshot.existed)
                {
                    Source source = runtime.getSourceById(snapshot.sourceId);
                    if (source != null && !source.isImmutable())
                    {
                        runtime.modify(snapshot.sourceId, snapshot.content);
                    }
                    else if (snapshot.overlayStorage != null)
                    {
                        runtime.getCodeStorage().writeContent(snapshot.sourceId, snapshot.content);
                        runtime.loadSourceIfLoadable(snapshot.sourceId);
                    }
                }
                else
                {
                    runtime.delete(snapshot.sourceId);
                }
            }
            runtime.compile();
            return true;
        }
        catch (Exception restoreError)
        {
            LOGGER.warn("Failed to restore runtime after bulk compile failure", restoreError);
            return false;
        }
    }

    private void clearOverlayForAcceptedDiskChanges(List<LegendPureSession.FileChange> changes)
    {
        for (LegendPureSession.FileChange change : changes)
        {
            if (change.getType() == LegendPureSession.FileChangeType.CREATE_OR_MODIFY)
            {
                clearOverlayIfDiskMatches(change.getSourceId(), change.getContent());
            }
            else if (change.getType() == LegendPureSession.FileChangeType.DELETE)
            {
                clearOverlayIfDiskMissing(change.getSourceId());
            }
        }
    }

    private void clearOverlayIfDiskMatches(String sourceId, String content)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        OverlayWorkspaceCodeStorage overlayStorage = overlayStorage(runtime, sourceId);
        if (overlayStorage == null)
        {
            return;
        }
        try
        {
            if (content != null && content.equals(overlayStorage.getDiskContentAsText(sourceId)))
            {
                overlayStorage.clearOverlay(sourceId);
            }
        }
        catch (Exception ignore)
        {
            // Keep the accepted overlay if disk cannot be read.
        }
    }

    private void clearOverlayIfDiskMissing(String sourceId)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        OverlayWorkspaceCodeStorage overlayStorage = overlayStorage(runtime, sourceId);
        if (overlayStorage == null)
        {
            return;
        }
        try
        {
            overlayStorage.getDiskContentAsText(sourceId);
        }
        catch (Exception e)
        {
            overlayStorage.clearOverlay(sourceId);
        }
    }

    private static boolean isOverlayBackedSource(PureRuntime runtime, String sourceId)
    {
        return overlayStorage(runtime, sourceId) != null;
    }

    private static OverlayWorkspaceCodeStorage overlayStorage(PureRuntime runtime, String sourceId)
    {
        try
        {
            if (runtime == null || sourceId == null || !sourceId.startsWith("/"))
            {
                return null;
            }
            if (!(runtime.getCodeStorage() instanceof CompositeCodeStorage))
            {
                return runtime.getCodeStorage() instanceof OverlayWorkspaceCodeStorage
                        ? (OverlayWorkspaceCodeStorage) runtime.getCodeStorage()
                        : null;
            }
            CompositeCodeStorage composite = (CompositeCodeStorage) runtime.getCodeStorage();
            CodeRepository repository = composite.getRepositoryForPath(sourceId);
            if (repository == null)
            {
                return null;
            }
            RepositoryCodeStorage original = composite.getOriginalCodeStorage(repository);
            return original instanceof OverlayWorkspaceCodeStorage ? (OverlayWorkspaceCodeStorage) original : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static LegendPureSession.CompileResult toCompileResult(Exception e)
    {
        boolean isInternal = PureException.findPureException(e) == null;
        return LegendPureSession.CompileResult.error(e, isInternal);
    }

    private static class SourceSnapshot
    {
        private final String sourceId;
        private final boolean existed;
        private final boolean immutable;
        private final String content;
        private final OverlayWorkspaceCodeStorage overlayStorage;
        private final OverlayWorkspaceCodeStorage.OverlaySnapshot overlaySnapshot;

        private SourceSnapshot(String sourceId, boolean existed, boolean immutable, String content,
                               OverlayWorkspaceCodeStorage overlayStorage,
                               OverlayWorkspaceCodeStorage.OverlaySnapshot overlaySnapshot)
        {
            this.sourceId = sourceId;
            this.existed = existed;
            this.immutable = immutable;
            this.content = content;
            this.overlayStorage = overlayStorage;
            this.overlaySnapshot = overlaySnapshot;
        }

        static SourceSnapshot existing(String sourceId, String content, OverlayWorkspaceCodeStorage overlayStorage,
                                       OverlayWorkspaceCodeStorage.OverlaySnapshot overlaySnapshot)
        {
            return new SourceSnapshot(sourceId, true, false, content, overlayStorage, overlaySnapshot);
        }

        static SourceSnapshot missing(String sourceId, OverlayWorkspaceCodeStorage overlayStorage,
                                      OverlayWorkspaceCodeStorage.OverlaySnapshot overlaySnapshot)
        {
            return new SourceSnapshot(sourceId, false, false, null, overlayStorage, overlaySnapshot);
        }

        static SourceSnapshot immutable(String sourceId, OverlayWorkspaceCodeStorage overlayStorage,
                                        OverlayWorkspaceCodeStorage.OverlaySnapshot overlaySnapshot)
        {
            return new SourceSnapshot(sourceId, true, true, null, overlayStorage, overlaySnapshot);
        }
    }
}
