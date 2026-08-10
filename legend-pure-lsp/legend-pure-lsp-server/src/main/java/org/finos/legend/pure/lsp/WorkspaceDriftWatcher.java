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
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.finos.legend.pure.lsp.protocol.DriftChangeType;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEntry;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEvent;

/**
 * Watches the workspace's resource roots for out-of-band {@code .pure} file changes - a rebase, a
 * checkout, a patch applied outside the editor - none of which generate a
 * {@code textDocument/didChange} (nothing typed anything) or reach the compiled graph any other way.
 * Purely observational: it only accumulates a dirty set and, after a short debounce settles, broadcasts
 * it via {@link org.finos.legend.pure.lsp.protocol.LegendLanguageClient#workspaceDriftDetected}. It never
 * triggers a recompile itself - that stays a deliberate, user-initiated {@code legend/syncWorkspace} call
 * (see {@link LegendWorkspaceService}), so a large burst of events (a rebase touching hundreds of files)
 * never causes surprise recompiles mid-operation.
 * <p>
 * Scope matches {@link FileChangeHandler}'s existing convention: {@code .pure} files only. Non-source
 * fixtures under a repo (e.g. {@code .legend}/{@code .json} test resources) are read directly off disk by
 * Pure's own {@code readFile(...)} at execution time and were never part of the compiled graph to begin
 * with, so drift in them isn't meaningful here.
 */
public class WorkspaceDriftWatcher
{
    private static final long DEBOUNCE_MS = 10_000;

    private final RepositoryScanner repositoryScanner;
    private final UriMapper uriMapper;
    private final Predicate<String> isDocumentOpen;
    private final Consumer<WorkspaceDriftEvent> publisher;

    private final Map<String, DriftChangeType> dirty = new ConcurrentHashMap<>();
    private final Map<WatchKey, Path> watchKeyToDir = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r ->
    {
        Thread t = new Thread(r, "legend-pure-lsp-drift-watcher-debounce");
        t.setDaemon(true);
        return t;
    });

    private volatile ScheduledFuture<?> pendingPublish;
    private volatile WatchService watchService;
    private volatile Thread watchThread;
    private volatile boolean running = false;

    public WorkspaceDriftWatcher(RepositoryScanner repositoryScanner, UriMapper uriMapper,
            Predicate<String> isDocumentOpen, Consumer<WorkspaceDriftEvent> publisher)
    {
        this.repositoryScanner = repositoryScanner;
        this.uriMapper = uriMapper;
        this.isDocumentOpen = isDocumentOpen;
        this.publisher = publisher;
    }

    public synchronized void start(Collection<Path> resourceRoots)
    {
        if (this.running)
        {
            return;
        }
        try
        {
            this.watchService = FileSystems.getDefault().newWatchService();
        }
        catch (IOException e)
        {
            LspLog.warn("Could not start workspace drift watcher: " + e.getMessage());
            return;
        }
        this.running = true;
        for (Path root : resourceRoots)
        {
            registerRecursively(root);
        }
        this.watchThread = new Thread(this::watchLoop, "legend-pure-lsp-drift-watcher");
        this.watchThread.setDaemon(true);
        this.watchThread.start();
        LspLog.info("Workspace drift watcher started over " + resourceRoots.size()
                + " root(s), watching " + this.watchKeyToDir.size() + " director(y/ies)");
    }

    public synchronized void stop()
    {
        if (!this.running)
        {
            return;
        }
        this.running = false;
        if (this.watchThread != null)
        {
            this.watchThread.interrupt();
        }
        if (this.watchService != null)
        {
            try
            {
                this.watchService.close();
            }
            catch (IOException ignored)
            {
            }
        }
        this.debounceExecutor.shutdownNow();
    }

    private void registerRecursively(Path root)
    {
        if (!Files.isDirectory(root))
        {
            return;
        }
        try
        {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    if (name.startsWith(".") || "target".equals(name) || "node_modules".equals(name))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    registerDir(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            LspLog.warn("Failed to register drift watches under " + root + ": " + e.getMessage());
        }
    }

    private void registerDir(Path dir)
    {
        try
        {
            WatchKey key = dir.register(this.watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            this.watchKeyToDir.put(key, dir);
        }
        catch (IOException e)
        {
            LspLog.warn("Failed to watch directory " + dir + ": " + e.getMessage());
        }
    }

    private void watchLoop()
    {
        while (this.running)
        {
            WatchKey key;
            try
            {
                key = this.watchService.take();
            }
            catch (InterruptedException | ClosedWatchServiceException e)
            {
                return;
            }

            Path dir = this.watchKeyToDir.get(key);
            if (dir != null)
            {
                for (WatchEvent<?> event : key.pollEvents())
                {
                    handleEvent(dir, event);
                }
            }
            boolean valid = key.reset();
            if (!valid)
            {
                this.watchKeyToDir.remove(key);
            }
        }
    }

    private void handleEvent(Path dir, WatchEvent<?> event)
    {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW)
        {
            // The OS-level event queue dropped events - our dirty set can no longer be trusted as
            // complete. Surface this via a log line rather than silently under-reporting drift; a full
            // Reindex is the correct recovery, not something this watcher should force on its own.
            LspLog.warn("Workspace drift watcher event queue overflowed; some external file changes may"
                    + " not be reflected. Run Reindex Pure Workspace if the workspace seems out of sync.");
            return;
        }

        Path name = (Path) event.context();
        Path fullPath = dir.resolve(name);

        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath))
        {
            registerRecursively(fullPath);
            return;
        }

        if (!name.toString().endsWith(".pure"))
        {
            return;
        }

        String sourceId = this.repositoryScanner.deriveSourceIdFromPath(fullPath);
        if (sourceId == null)
        {
            return;
        }

        DriftChangeType type = (kind == StandardWatchEventKinds.ENTRY_DELETE) ? DriftChangeType.DELETED
                : (kind == StandardWatchEventKinds.ENTRY_CREATE) ? DriftChangeType.CREATED
                : DriftChangeType.MODIFIED;
        this.dirty.compute(sourceId, (id, existing) -> mergeType(existing, type));
        schedulePublish();
    }

    /**
     * A plain open+write+close (what any normal file save does) fires CREATE immediately followed by
     * MODIFY on most watch implementations - naively taking "whatever arrived last" would label every
     * brand-new file as "modified" instead of "created". CREATED sticks until an explicit DELETE; a
     * DELETE immediately after a not-yet-synced CREATED means the file never existed as far as anyone
     * downstream is concerned, so the entry is dropped entirely rather than reported as either.
     */
    private static DriftChangeType mergeType(DriftChangeType existing, DriftChangeType incoming)
    {
        if (existing == null)
        {
            return incoming;
        }
        if (incoming == DriftChangeType.DELETED)
        {
            return (existing == DriftChangeType.CREATED) ? null : DriftChangeType.DELETED;
        }
        return (existing == DriftChangeType.CREATED) ? DriftChangeType.CREATED : incoming;
    }

    private synchronized void schedulePublish()
    {
        if (this.pendingPublish != null)
        {
            this.pendingPublish.cancel(false);
        }
        try
        {
            this.pendingPublish = this.debounceExecutor.schedule(this::publish, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
        catch (java.util.concurrent.RejectedExecutionException ignored)
        {
            // Watcher is stopping/stopped.
        }
    }

    private void publish()
    {
        List<WorkspaceDriftEntry> entries = new ArrayList<>();
        for (Map.Entry<String, DriftChangeType> e : this.dirty.entrySet())
        {
            String uri = this.uriMapper.toUri(e.getKey());
            if (uri == null || this.isDocumentOpen.test(uri))
            {
                continue;
            }
            entries.add(new WorkspaceDriftEntry(uri, e.getValue()));
        }
        if (entries.isEmpty())
        {
            return;
        }
        this.publisher.accept(new WorkspaceDriftEvent(entries));
    }

    /**
     * Re-broadcasts the current (post-filter) drift state immediately, bypassing the debounce - used
     * after a sync applies some but not all dirty entries, so every connected client's view of "what's
     * still out of sync" updates right away instead of waiting for the next unrelated file event.
     */
    public void publishNow()
    {
        List<WorkspaceDriftEntry> entries = new ArrayList<>();
        for (Map.Entry<String, DriftChangeType> e : this.dirty.entrySet())
        {
            String uri = this.uriMapper.toUri(e.getKey());
            if (uri == null || this.isDocumentOpen.test(uri))
            {
                continue;
            }
            entries.add(new WorkspaceDriftEntry(uri, e.getValue()));
        }
        this.publisher.accept(new WorkspaceDriftEvent(entries));
    }

    public Set<String> getDirtySourceIds()
    {
        return new LinkedHashSet<>(this.dirty.keySet());
    }

    public void clear(Collection<String> sourceIds)
    {
        for (String id : sourceIds)
        {
            this.dirty.remove(id);
        }
    }
}
