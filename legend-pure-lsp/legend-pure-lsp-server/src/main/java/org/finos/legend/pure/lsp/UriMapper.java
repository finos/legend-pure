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

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.FSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps between editor URIs and Pure source IDs.
 */
public class UriMapper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UriMapper.class);
    private static final String RESOURCES_MARKER = "/src/main/resources/";

    private final Map<String, String> uriToSourceId = new ConcurrentHashMap<>();
    private final Map<String, String> sourceIdToUri = new ConcurrentHashMap<>();
    private volatile RepositoryScanner repositoryScanner;
    private volatile PureRuntime pureRuntime;

    public void register(String uri, String sourceId)
    {
        this.uriToSourceId.put(uri, sourceId);
        this.sourceIdToUri.put(sourceId, uri);
    }

    public String toSourceId(String uri)
    {
        String cached = this.uriToSourceId.get(uri);
        if (cached != null)
        {
            return cached;
        }

        String sourceId = deriveSourceId(uri);
        if (sourceId == null)
        {
            // Not part of any registered Pure module (see deriveSourceId) - nothing to cache either
            // direction; ConcurrentHashMap also rejects null values outright.
            return null;
        }
        this.uriToSourceId.put(uri, sourceId);
        this.sourceIdToUri.put(sourceId, uri);
        return sourceId;
    }

    public void setRepositoryScanner(RepositoryScanner scanner)
    {
        this.repositoryScanner = scanner;
    }

    public void setPureRuntime(PureRuntime runtime)
    {
        this.pureRuntime = runtime;
    }

    public String toUri(String sourceId)
    {
        if (sourceId == null)
        {
            // A null sourceId means "no known Pure module for this file" (see deriveSourceId) - callers
            // that map a FileChange list back to URIs (e.g. syncWorkspace's open-document-conflict
            // filter) can legitimately hit this for a real, non-module .pure fixture file scanned off
            // disk. ConcurrentHashMap.get(null) throws NPE outright, unlike HashMap, so this must be
            // handled before ever reaching the map lookups below.
            return null;
        }

        String cached = this.sourceIdToUri.get(sourceId);
        if (cached != null)
        {
            return cached;
        }

        String alt = sourceId.startsWith("/") ? sourceId.substring(1) : "/" + sourceId;
        cached = this.sourceIdToUri.get(alt);
        if (cached != null)
        {
            this.sourceIdToUri.put(sourceId, cached);
            return cached;
        }

        PureRuntime runtime = this.pureRuntime;
        if (runtime != null)
        {
            String fileUri = resolveViaStorage(runtime, sourceId);
            if (fileUri == null)
            {
                fileUri = resolveViaStorage(runtime, alt);
            }
            if (fileUri != null)
            {
                this.sourceIdToUri.put(sourceId, fileUri);
                this.uriToSourceId.put(fileUri, sourceId);
                return fileUri;
            }
        }

        RepositoryScanner scanner = this.repositoryScanner;
        if (scanner != null)
        {
            String resolved = scanner.resolveToUri(sourceId);
            if (resolved != null)
            {
                this.sourceIdToUri.put(sourceId, resolved);
                this.uriToSourceId.put(resolved, sourceId);
                return resolved;
            }
        }

        if (sourceId.startsWith("/"))
        {
            String pureUri = "pure://" + sourceId;
            this.sourceIdToUri.put(sourceId, pureUri);
            LOGGER.debug("JAR-only source, using pure:// URI: {}", sourceId);
            return pureUri;
        }

        LOGGER.debug("Cannot resolve source ID to any URI: {}", sourceId);
        return null;
    }

    private String resolveViaStorage(PureRuntime runtime, String sourceId)
    {
        try
        {
            Source source = runtime.getSourceById(sourceId);
            if (source == null)
            {
                return null;
            }

            RepositoryCodeStorage codeStorage = runtime.getCodeStorage();
            if (!(codeStorage instanceof CompositeCodeStorage))
            {
                return null;
            }

            CompositeCodeStorage composite = (CompositeCodeStorage) codeStorage;
            CodeRepository repo = composite.getRepositoryForPath(sourceId);
            if (repo == null)
            {
                return null;
            }

            RepositoryCodeStorage repoStorage = composite.getOriginalCodeStorage(repo);
            if (repoStorage instanceof FSCodeStorage)
            {
                FSCodeStorage fsStorage = (FSCodeStorage) repoStorage;
                Path root = fsStorage.getRoot();
                if (root != null)
                {
                    String path = sourceId.startsWith("/") ? sourceId.substring(1) : sourceId;
                    String repoName = repo.getName();
                    if (repoName != null && path.startsWith(repoName + "/"))
                    {
                        path = path.substring(repoName.length() + 1);
                    }
                    Path fullPath = root.resolve(path);
                    if (java.nio.file.Files.exists(fullPath))
                    {
                        return fullPath.toUri().toString();
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("resolveViaStorage failed for {}: {}", sourceId, e.getMessage());
        }
        return null;
    }

    public void clear()
    {
        this.uriToSourceId.clear();
        this.sourceIdToUri.clear();
    }

    String deriveSourceId(String uri)
    {
        if (uri.startsWith("pure://"))
        {
            return uri.substring("pure://".length());
        }

        String path;
        try
        {
            path = URI.create(uri).getPath();
        }
        catch (Exception e)
        {
            path = uri;
        }

        int idx = path.indexOf(RESOURCES_MARKER);
        if (idx >= 0)
        {
            String candidate = "/" + path.substring(idx + RESOURCES_MARKER.length());
            // A src/main/resources-rooted path is only trustworthy as-is when there's no scanner to
            // check it against (kept for minimal/unit-test setups); once a real workspace scan is
            // available, the leading segment must name an actually-registered repo (a <name>.definition.json
            // module) - otherwise this is some other module's stray .pure file (e.g. a data fixture in a
            // sibling module with no definition.json of its own) that merely happens to sit under a
            // src/main/resources directory, and must not be treated as compilable.
            if (this.repositoryScanner == null || isKnownWorkspaceSource(candidate))
            {
                return candidate;
            }
        }

        RepositoryScanner scanner = this.repositoryScanner;
        if (scanner != null)
        {
            try
            {
                java.nio.file.Path filePath = java.nio.file.Paths.get(path);
                String derived = scanner.deriveSourceIdFromPath(filePath);
                if (derived != null)
                {
                    LspLog.debug("Derived source ID from repo scanner: " + derived);
                    return derived;
                }
            }
            catch (Exception ignored)
            {
            }
        }

        if (path.startsWith("/") && path.endsWith(".pure"))
        {
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash > 1)
            {
                String firstSegment = path.substring(1, secondSlash);
                if (!firstSegment.contains(".") && !firstSegment.contains(" ") && firstSegment.length() < 60
                        && isKnownWorkspaceSource(path))
                {
                    return path;
                }
            }
        }

        int lastSlash = path.lastIndexOf('/');
        String filename = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;

        // Nothing above recognized this as part of any registered Pure module. A .pure file that
        // genuinely exists on local disk at this exact path (e.g. a ###Lakehouse test fixture in a
        // module with no definition.json of its own) has a real on-disk identity that simply isn't Pure
        // source - unlike a jar-embedded platform source navigated to via go-to-definition, whose
        // literal ("!"-joined jar-entry-style) path never exists as a real file, so it correctly keeps
        // falling through to the scratch/in-memory handling below. Treating the former as scratch would
        // still attempt (and fail) to compile it, and worse, leaves nothing registered for its id, so a
        // later didClose's restoreFromDisk has nothing to restore. Must be ignored outright - the same
        // as a .java file would be - rather than routed through compilation at all.
        // Exception: welcome.pure is the standing go()-scratch convention for this dev loop (see
        // pure-lsp-go/pure-lsp-start skills) - a real repo-root file with no definition.json by design,
        // always meant to be edited and executed as scratch, never a genuine non-Pure fixture.
        if (scanner != null && !"welcome.pure".equals(filename) && isRealLocalFile(path))
        {
            LspLog.debug("Local .pure file is not part of any registered Pure module, ignoring: " + path);
            return null;
        }

        LspLog.debug("Non-workspace .pure file, treating as scratch (in-memory): " + filename + " (from " + path + ")");
        return filename;
    }

    private static boolean isRealLocalFile(String path)
    {
        if (!path.startsWith("/"))
        {
            return false;
        }
        try
        {
            return java.nio.file.Files.isRegularFile(java.nio.file.Paths.get(path));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * True if the leading path segment of a candidate sourceId (e.g. "/core/foo.pure" -> "core") names
     * a repo the workspace scan actually found a &lt;name&gt;.definition.json for, or the runtime already
     * has this exact source loaded (e.g. from classpath). This is the "is it part of a Pure module"
     * check the callers above rely on before treating a raw file path as a real, compilable source.
     */
    private boolean isKnownWorkspaceSource(String candidateSourceId)
    {
        String relative = candidateSourceId.startsWith("/") ? candidateSourceId.substring(1) : candidateSourceId;
        int slash = relative.indexOf('/');
        String firstSegment = slash > 0 ? relative.substring(0, slash) : relative;

        RepositoryScanner scanner = this.repositoryScanner;
        if (scanner != null && scanner.getWorkspaceRepoNames().contains(firstSegment))
        {
            return true;
        }

        PureRuntime runtime = this.pureRuntime;
        return runtime != null && runtime.getSourceById(candidateSourceId) != null;
    }
}
