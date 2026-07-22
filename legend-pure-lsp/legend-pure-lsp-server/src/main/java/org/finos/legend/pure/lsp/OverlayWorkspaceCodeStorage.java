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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNodeStatus;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.FSCodeStorage;

/**
 * In-memory overlay over FSCodeStorage: reads from disk, writes stay in memory.
 */
public class OverlayWorkspaceCodeStorage extends FSCodeStorage implements MutableRepositoryCodeStorage
{
    private final Map<String, String> contentByPath = new ConcurrentHashMap<>();
    private final Set<String> deletedPaths = ConcurrentHashMap.newKeySet();
    private final Set<String> createdFolders = ConcurrentHashMap.newKeySet();

    public OverlayWorkspaceCodeStorage(CodeRepository repository, Path root)
    {
        super(repository, root);
    }

    @Override
    public OutputStream writeContent(String path)
    {
        String normalized = normalizePath(path);
        return new ByteArrayOutputStream()
        {
            @Override
            public void close()
            {
                writeContent(normalized, new String(toByteArray(), StandardCharsets.UTF_8));
            }
        };
    }

    @Override
    public void writeContent(String path, String content)
    {
        String normalized = normalizePath(path);
        this.deletedPaths.remove(normalized);
        this.contentByPath.put(normalized, content == null ? "" : content);
        addParentFolders(normalized);
    }

    @Override
    public void createFile(String filePath)
    {
        writeContent(filePath, "");
    }

    @Override
    public void createFolder(String folderPath)
    {
        String normalized = normalizePath(folderPath);
        this.deletedPaths.remove(normalized);
        this.createdFolders.add(normalized);
        addParentFolders(normalized);
    }

    @Override
    public void deleteFile(String filePath)
    {
        String normalized = normalizePath(filePath);
        this.contentByPath.remove(normalized);
        this.createdFolders.remove(normalized);
        this.deletedPaths.add(normalized);
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath)
    {
        String content = getContentAsText(sourcePath);
        deleteFile(sourcePath);
        writeContent(destinationPath, content);
    }

    @Override
    public CodeStorageNode getNode(String path)
    {
        String normalized = normalizePath(path);
        if (this.deletedPaths.contains(normalized))
        {
            throw new IllegalArgumentException("Cannot find " + path);
        }
        if (this.contentByPath.containsKey(normalized))
        {
            return new OverlayCodeStorageNode(normalized, false);
        }
        if (isOverlayFolder(normalized))
        {
            return new OverlayCodeStorageNode(normalized, true);
        }
        return super.getNode(path);
    }

    @Override
    public RichIterable<CodeStorageNode> getFiles(String path)
    {
        String normalized = normalizePath(path);
        if (!isFolder(path))
        {
            return Lists.immutable.with(getNode(path));
        }

        Map<String, CodeStorageNode> children = new java.util.TreeMap<>();
        try
        {
            for (CodeStorageNode node : super.getFiles(path))
            {
                if (!this.deletedPaths.contains(node.getPath()))
                {
                    children.put(node.getPath(), node);
                }
            }
        }
        catch (Exception ignored)
        {
        }

        String prefix = folderPrefix(normalized);
        for (String overlayPath : overlayPaths())
        {
            if (overlayPath.startsWith(prefix) && !overlayPath.equals(normalized))
            {
                String child = directChild(normalized, overlayPath);
                if (child != null && !this.deletedPaths.contains(child))
                {
                    children.put(child, new OverlayCodeStorageNode(child, isOverlayFolder(child)));
                }
            }
        }
        return Lists.mutable.withAll(children.values());
    }

    @Override
    public RichIterable<String> getUserFiles()
    {
        Set<String> result = new LinkedHashSet<>();
        try
        {
            for (String file : super.getUserFiles())
            {
                if (!this.deletedPaths.contains(file))
                {
                    result.add(file);
                }
            }
        }
        catch (Exception ignored)
        {
        }

        for (String file : this.contentByPath.keySet())
        {
            if (!this.deletedPaths.contains(file) && CodeStorageTools.hasPureFileExtension(file))
            {
                result.add(file);
            }
        }
        return Lists.mutable.withAll(result);
    }

    @Override
    public RichIterable<String> getFileOrFiles(String path)
    {
        if (isFile(path))
        {
            return Lists.immutable.with(normalizePath(path));
        }
        if (isFolder(path))
        {
            String prefix = folderPrefix(normalizePath(path));
            MutableList<String> result = Lists.mutable.empty();
            for (String file : getUserFiles())
            {
                if (file.startsWith(prefix))
                {
                    result.add(file);
                }
            }
            return result;
        }
        return Lists.immutable.empty();
    }

    @Override
    public ByteArrayInputStream getContent(String path)
    {
        return new ByteArrayInputStream(getContentAsBytes(path));
    }

    @Override
    public byte[] getContentAsBytes(String path)
    {
        return getContentAsText(path).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getContentAsText(String path)
    {
        String normalized = normalizePath(path);
        if (this.deletedPaths.contains(normalized))
        {
            throw new RuntimeException("Error getting content as text for deleted overlay path " + path);
        }
        String overlayContent = this.contentByPath.get(normalized);
        if (overlayContent != null)
        {
            return overlayContent;
        }
        return super.getContentAsText(path);
    }

    public String getDiskContentAsText(String path)
    {
        return super.getContentAsText(path);
    }

    @Override
    public boolean exists(String path)
    {
        String normalized = normalizePath(path);
        if (this.deletedPaths.contains(normalized))
        {
            return false;
        }
        return this.contentByPath.containsKey(normalized) || isOverlayFolder(normalized) || super.exists(path);
    }

    @Override
    public boolean isFile(String path)
    {
        String normalized = normalizePath(path);
        if (this.deletedPaths.contains(normalized))
        {
            return false;
        }
        return this.contentByPath.containsKey(normalized) || super.isFile(path);
    }

    @Override
    public boolean isFolder(String path)
    {
        String normalized = normalizePath(path);
        if (this.deletedPaths.contains(normalized) || this.contentByPath.containsKey(normalized))
        {
            return false;
        }
        return isOverlayFolder(normalized) || super.isFolder(path);
    }

    @Override
    public boolean isEmptyFolder(String path)
    {
        if (!isFolder(path))
        {
            return false;
        }
        String prefix = folderPrefix(normalizePath(path));
        for (String file : getUserFiles())
        {
            if (file.startsWith(prefix))
            {
                return false;
            }
        }
        return true;
    }

    public void clearOverlay(String path)
    {
        String normalized = normalizePath(path);
        this.contentByPath.remove(normalized);
        this.deletedPaths.remove(normalized);
        this.createdFolders.remove(normalized);
    }

    public OverlaySnapshot snapshot(String path)
    {
        String normalized = normalizePath(path);
        return new OverlaySnapshot(
                normalized,
                this.contentByPath.containsKey(normalized),
                this.contentByPath.get(normalized),
                this.deletedPaths.contains(normalized),
                this.createdFolders.contains(normalized));
    }

    public void restore(OverlaySnapshot snapshot)
    {
        if (snapshot.hadContent)
        {
            this.contentByPath.put(snapshot.path, snapshot.content);
        }
        else
        {
            this.contentByPath.remove(snapshot.path);
        }

        if (snapshot.wasDeleted)
        {
            this.deletedPaths.add(snapshot.path);
        }
        else
        {
            this.deletedPaths.remove(snapshot.path);
        }

        if (snapshot.wasCreatedFolder)
        {
            this.createdFolders.add(snapshot.path);
        }
        else
        {
            this.createdFolders.remove(snapshot.path);
        }
    }

    private String normalizePath(String path)
    {
        String relative = getRelativePath(path).replace('\\', '/');
        String repoPath = "/" + this.repository.getName();
        return relative.isEmpty() ? repoPath : repoPath + "/" + relative;
    }

    private void addParentFolders(String normalizedPath)
    {
        int index = normalizedPath.lastIndexOf('/');
        while (index > 0)
        {
            String parent = normalizedPath.substring(0, index);
            if (parent.equals("/" + this.repository.getName()))
            {
                return;
            }
            this.createdFolders.add(parent);
            index = parent.lastIndexOf('/');
        }
    }

    private boolean isOverlayFolder(String normalizedPath)
    {
        if (this.createdFolders.contains(normalizedPath))
        {
            return true;
        }
        String prefix = folderPrefix(normalizedPath);
        for (String path : overlayPaths())
        {
            if (path.startsWith(prefix) && !this.deletedPaths.contains(path))
            {
                return true;
            }
        }
        return false;
    }

    private List<String> overlayPaths()
    {
        List<String> paths = new ArrayList<>(this.contentByPath.keySet());
        paths.addAll(this.createdFolders);
        return paths;
    }

    private static String folderPrefix(String normalizedFolder)
    {
        return normalizedFolder.endsWith("/") ? normalizedFolder : normalizedFolder + "/";
    }

    private static String directChild(String parent, String descendant)
    {
        String prefix = folderPrefix(parent);
        if (!descendant.startsWith(prefix))
        {
            return null;
        }
        int slash = descendant.indexOf('/', prefix.length());
        return slash < 0 ? descendant : descendant.substring(0, slash);
    }

    public static class OverlaySnapshot
    {
        private final String path;
        private final boolean hadContent;
        private final String content;
        private final boolean wasDeleted;
        private final boolean wasCreatedFolder;

        private OverlaySnapshot(String path, boolean hadContent, String content, boolean wasDeleted, boolean wasCreatedFolder)
        {
            this.path = path;
            this.hadContent = hadContent;
            this.content = content;
            this.wasDeleted = wasDeleted;
            this.wasCreatedFolder = wasCreatedFolder;
        }
    }

    private static class OverlayCodeStorageNode implements CodeStorageNode
    {
        private final String path;
        private final boolean directory;

        private OverlayCodeStorageNode(String path, boolean directory)
        {
            this.path = path;
            this.directory = directory;
        }

        @Override
        public boolean isDirectory()
        {
            return this.directory;
        }

        @Override
        public String getName()
        {
            int index = this.path.lastIndexOf('/');
            return index < 0 ? this.path : this.path.substring(index + 1);
        }

        @Override
        public String getPath()
        {
            return this.path;
        }

        @Override
        public CodeStorageNodeStatus getStatus()
        {
            return CodeStorageNodeStatus.UNKNOWN;
        }
    }
}
