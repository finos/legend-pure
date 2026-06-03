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
import org.finos.legend.pure.m3.SourceMutation;
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

                if (resolvedId != null)
                {
                    Source existingSource = runtime.getSourceById(resolvedId);
                    if (existingSource != null && existingSource.isImmutable())
                    {
                        LspLog.debug("Skipping modification of immutable source: " + resolvedId);
                        return LegendPureSession.CompileResult.success(Collections.emptyList());
                    }
                    String originalContent = (existingSource != null) ? existingSource.getContent() : null;

                    runtime.modify(resolvedId, content);
                    try
                    {
                        mutation = runtime.compile();
                    }
                    catch (Exception compileError)
                    {
                        if (originalContent != null)
                        {
                            restoreSourceContent(resolvedId, originalContent);
                        }
                        throw compileError;
                    }
                }
                else
                {
                    String inMemoryId = sourceId.startsWith("/") ? sourceId.substring(1) : sourceId;
                    try
                    {
                        mutation = runtime.createInMemoryAndCompile(Tuples.pair(inMemoryId, content));
                    }
                    catch (Exception compileError)
                    {
                        deleteSourceIfPresent(inMemoryId);
                        throw compileError;
                    }
                }
                return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
            }
            catch (Exception e)
            {
                return toCompileResult(e);
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
                return LegendPureSession.CompileResult.success(mutation.getModifiedFiles());
            }
            catch (Exception e)
            {
                restoreSnapshots(snapshots);
                return toCompileResult(e);
            }
        }
    }

    public void restoreFromDisk(String sourceId)
    {
        synchronized (this.session)
        {
            if (!this.session.isInitialized() || sourceId == null)
            {
                return;
            }
            PureRuntime runtime = this.session.getPureRuntime();
            try
            {
                String resolvedId = this.session.resolveSourceId(sourceId);
                if (resolvedId == null)
                {
                    return;
                }
                Source source = runtime.getSourceById(resolvedId);
                if (source == null || source.isImmutable())
                {
                    return;
                }
                if (source.isInMemory())
                {
                    runtime.delete(resolvedId);
                    runtime.compile();
                }
                else
                {
                    String diskContent = runtime.getCodeStorage().getContentAsText(resolvedId);
                    if (diskContent != null && !diskContent.equals(source.getContent()))
                    {
                        runtime.modify(resolvedId, diskContent);
                        runtime.compile();
                    }
                }
            }
            catch (Exception e)
            {
                LspLog.debug("restoreFromDisk failed for " + sourceId + ": " + e.getMessage());
            }
        }
    }

    private void restoreSourceContent(String sourceId, String content)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        try
        {
            runtime.modify(sourceId, content);
            runtime.compile();
            LOGGER.info("Restored original content for {} after compile failure", sourceId);
        }
        catch (Exception restoreError)
        {
            LOGGER.warn("Failed to restore original content for {}, runtime may be inconsistent",
                    sourceId, restoreError);
        }
    }

    private void deleteSourceIfPresent(String sourceId)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        try
        {
            if (runtime.getSourceById(sourceId) != null)
            {
                runtime.delete(sourceId);
                runtime.compile();
            }
        }
        catch (Exception restoreError)
        {
            LOGGER.warn("Failed to remove invalid new source {}, runtime may be inconsistent",
                    sourceId, restoreError);
        }
    }

    private List<SourceSnapshot> snapshot(List<LegendPureSession.FileChange> changes)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        List<SourceSnapshot> snapshots = new ArrayList<>();
        for (LegendPureSession.FileChange change : changes)
        {
            String resolvedId = this.session.resolveSourceId(change.getSourceId());
            Source source = resolvedId == null ? null : runtime.getSourceById(resolvedId);
            if (source != null && source.isImmutable())
            {
                snapshots.add(SourceSnapshot.immutable(resolvedId));
            }
            else if (source != null)
            {
                snapshots.add(SourceSnapshot.existing(resolvedId, source.getContent()));
            }
            else
            {
                snapshots.add(SourceSnapshot.missing(change.getSourceId()));
            }
        }
        return snapshots;
    }

    private void restoreSnapshots(List<SourceSnapshot> snapshots)
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
                if (snapshot.existed)
                {
                    Source source = runtime.getSourceById(snapshot.sourceId);
                    if (source != null && !source.isImmutable())
                    {
                        runtime.modify(snapshot.sourceId, snapshot.content);
                    }
                }
                else if (runtime.getSourceById(snapshot.sourceId) != null)
                {
                    runtime.delete(snapshot.sourceId);
                }
            }
            runtime.compile();
        }
        catch (Exception restoreError)
        {
            LOGGER.warn("Failed to restore runtime after bulk compile failure", restoreError);
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

        private SourceSnapshot(String sourceId, boolean existed, boolean immutable, String content)
        {
            this.sourceId = sourceId;
            this.existed = existed;
            this.immutable = immutable;
            this.content = content;
        }

        static SourceSnapshot existing(String sourceId, String content)
        {
            return new SourceSnapshot(sourceId, true, false, content);
        }

        static SourceSnapshot missing(String sourceId)
        {
            return new SourceSnapshot(sourceId, false, false, null);
        }

        static SourceSnapshot immutable(String sourceId)
        {
            return new SourceSnapshot(sourceId, true, true, null);
        }
    }
}
