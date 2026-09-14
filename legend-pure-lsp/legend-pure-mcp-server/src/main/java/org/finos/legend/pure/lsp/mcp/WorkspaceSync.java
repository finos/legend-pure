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

package org.finos.legend.pure.lsp.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disk is the source of truth: agents edit .pure files with their own tools, and this
 * class detects what changed since the last successful sync so the session can be
 * updated with one bulk compile. No file watching - sync is computed on demand.
 */
public class WorkspaceSync
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceSync.class);

    private final RepositoryScanner scanner;
    private final Map<String, String> knownHashes = new HashMap<>();

    public WorkspaceSync(RepositoryScanner scanner)
    {
        this.scanner = scanner;
    }

    public void seed() throws IOException
    {
        this.knownHashes.clear();
        Map<String, String> onDisk = scanDisk();
        for (Map.Entry<String, String> entry : onDisk.entrySet())
        {
            this.knownHashes.put(entry.getKey(), hash(entry.getValue()));
        }
        LOGGER.info("WorkspaceSync seeded with {} sources", this.knownHashes.size());
    }

    public List<LegendPureSession.FileChange> computeChanges() throws IOException
    {
        Map<String, String> onDisk = scanDisk();
        List<LegendPureSession.FileChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> entry : onDisk.entrySet())
        {
            String sourceId = entry.getKey();
            String content = entry.getValue();
            String contentHash = hash(content);
            if (!contentHash.equals(this.knownHashes.get(sourceId)))
            {
                changes.add(new LegendPureSession.FileChange(
                        sourceId, content, LegendPureSession.FileChangeType.CREATE_OR_MODIFY));
            }
        }
        for (String knownId : this.knownHashes.keySet())
        {
            if (!onDisk.containsKey(knownId))
            {
                changes.add(new LegendPureSession.FileChange(
                        knownId, null, LegendPureSession.FileChangeType.DELETE));
            }
        }
        return changes;
    }

    public void markApplied(List<LegendPureSession.FileChange> changes)
    {
        for (LegendPureSession.FileChange change : changes)
        {
            if (change.getType() == LegendPureSession.FileChangeType.DELETE)
            {
                this.knownHashes.remove(change.getSourceId());
            }
            else
            {
                this.knownHashes.put(change.getSourceId(), hash(change.getContent()));
            }
        }
    }

    private Map<String, String> scanDisk() throws IOException
    {
        Map<String, String> contents = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : this.scanner.getMappings().entrySet())
        {
            Path repoDir = entry.getValue().resolve(entry.getKey());
            if (!Files.isDirectory(repoDir))
            {
                continue;
            }
            Files.walkFileTree(repoDir, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    if (file.getFileName().toString().endsWith(".pure"))
                    {
                        String sourceId = WorkspaceSync.this.scanner.deriveSourceIdFromPath(file);
                        if (sourceId != null)
                        {
                            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                            contents.put(sourceId, content);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return contents;
    }

    private static String hash(String content)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes)
            {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
