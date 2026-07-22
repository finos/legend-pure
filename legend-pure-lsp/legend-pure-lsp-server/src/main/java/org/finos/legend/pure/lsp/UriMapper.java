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
            return "/" + path.substring(idx + RESOURCES_MARKER.length());
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
                if (!firstSegment.contains(".") && !firstSegment.contains(" ") && firstSegment.length() < 60)
                {
                    RepositoryScanner repoScanner = this.repositoryScanner;
                    PureRuntime rt = this.pureRuntime;
                    if ((repoScanner != null && repoScanner.getWorkspaceRepoNames().contains(firstSegment))
                            || (rt != null && rt.getSourceById(path) != null))
                    {
                        return path;
                    }
                }
            }
        }

        int lastSlash = path.lastIndexOf('/');
        String filename = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
        LspLog.debug("Scratch file (in-memory): " + filename + " (from " + path + ")");
        return filename;
    }
}
