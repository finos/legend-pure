// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.SVNCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ScratchCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNodeStatus;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.ImmutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.VCSCodeStorageBuilder;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.MutableVersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.UpdateReport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.VersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

public class PureCodeStorage implements MutableCodeStorage
{
    public static final String WELCOME_FILE_NAME = "welcome.pure";
    public static final String WELCOME_FILE_PATH = ROOT_PATH + WELCOME_FILE_NAME;
    private static final String WELCOME_RESOURCE_NAME = "/org/finos/legend/pure/m3/serialization/filesystem/welcome.pure";

    private final Path root;
    private final ImmutableList<RepositoryCodeStorage> codeStorages;
    private final ImmutableMap<String, RepositoryCodeStorage> codeStorageByName;
    private final ImmutableMap<String, CodeRepository> repositoriesByName;

    public PureCodeStorage(Path root, RepositoryCodeStorage... codeStorages)
    {
        this.root = root;
        this.codeStorages = Lists.immutable.with(codeStorages);
        this.codeStorageByName = indexCodeStoragesByName(codeStorages).toImmutable();
        this.repositoriesByName = this.codeStorages.asLazy().flatCollect(RepositoryCodeStorage.GET_REPOSITORIES).groupByUniqueKey(CodeRepository::getName, UnifiedMap.newMap(this.codeStorageByName.size())).toImmutable();
    }

    @Override
    public void initialize(Message message)
    {
        // Initialize code storage for each code repository
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            codeStorage.initialize(message);
        }

        // Create /welcome.pure if it does not exist
        Path welcomeFile = resolveWelcomePath();
        if (Files.notExists(welcomeFile))
        {
            try (OutputStream outStream = Files.newOutputStream(welcomeFile);
                 InputStream inStream = getClass().getResourceAsStream(WELCOME_RESOURCE_NAME))
            {
                byte[] buffer = new byte[2048];
                for (int read = inStream.read(buffer); read != -1; read = inStream.read(buffer))
                {
                    outStream.write(buffer, 0, read);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error creating /" + WELCOME_FILE_NAME, e);
            }
        }
    }

    @Override
    public String getRepoName(String path)
    {
        if (CodeStorageTools.isRootPath(path) || isWelcomePath(path))
        {
            return null;
        }
        String repoName = CodeStorageTools.getInitialPathElement(path);
        if (!this.repositoriesByName.containsKey(repoName))
        {
            throw new RuntimeException("The repo '" + repoName + "' can't be found!");
        }
        return repoName;
    }

    @Override
    public RichIterable<String> getAllRepoNames()
    {
        return this.repositoriesByName.keysView();
    }

    @Override
    public boolean isRepoName(String name)
    {
        return this.repositoriesByName.containsKey(name);
    }

    @Override
    public RichIterable<CodeRepository> getAllRepositories()
    {
        return this.repositoriesByName.valuesView();
    }

    @Override
    public CodeRepository getRepository(String name)
    {
        return this.repositoriesByName.get(name);
    }

    @Override
    public CodeStorageNode getNode(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            return getRootNode();
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new IllegalArgumentException("Cannot find " + path);
            }
            return getWelcomeNode();
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage == null)
        {
            throw new IllegalArgumentException("Cannot get node for " + path);
        }
        return codeStorage.getNode(path);
    }

    @Override
    public RichIterable<CodeStorageNode> getFiles(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            final MutableList<CodeStorageNode> nodes = FastList.newList(this.codeStorageByName.size() + 1);
            this.codeStorageByName.forEachKeyValue(new Procedure2<String, RepositoryCodeStorage>()
            {
                @Override
                public void value(String name, RepositoryCodeStorage codeStorage)
                {
                    // node == null means that a new Pure repository has been added, but the directory is not present on disk.
                    // We ignore this case so that in the IDE those who do not have the directory are able to get it by updating.
                    CodeStorageNode node = codeStorage.getNode(name);
                    if (node != null)
                    {
                        nodes.add(node);
                    }
                }
            });
            if (welcomeExists())
            {
                nodes.add(getWelcomeNode());
            }
            return nodes;
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new IllegalArgumentException("Cannot get files for " + path);
            }
            return Lists.immutable.with(getWelcomeNode());
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage == null)
        {
            throw new IllegalArgumentException("Cannot get files for " + path);
        }
        return codeStorage.getFiles(path);
    }

    @Override
    public RichIterable<String> getFileOrFiles(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            MutableList<String> files = Lists.mutable.empty();
            for (RepositoryCodeStorage codeStorage : this.codeStorageByName.valuesView())
            {
                for (CodeRepository repository : codeStorage.getRepositories())
                {
                    files.addAllIterable(codeStorage.getFileOrFiles("/" + repository.getName()));
                }
            }
            if (welcomeExists())
            {
                files.add(WELCOME_FILE_PATH);
            }
            return files;
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new IllegalArgumentException("Cannot get files for " + path);
            }
            return Lists.immutable.with(WELCOME_FILE_PATH);
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage == null)
        {
            throw new IllegalArgumentException("Cannot get files for " + path);
        }
        return codeStorage.getFileOrFiles(path);
    }

    @Override
    public InputStream getContent(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new RuntimeException("Error getting content for " + path + ": no such file");
            }
            try
            {
                return Files.newInputStream(resolveWelcomePath());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting content for " + path, e);
            }
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage == null)
        {
            throw new RuntimeException("Error getting content for " + path);
        }
        return codeStorage.getContent(path);
    }

    @Override
    public byte[] getContentAsBytes(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new RuntimeException("Error getting content as bytes for " + path + ": no such file");
            }
            try
            {
                return Files.readAllBytes(resolveWelcomePath());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting content as bytes for " + path, e);
            }
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage == null)
        {
            throw new RuntimeException("Error getting content as bytes for " + path);
        }
        return codeStorage.getContentAsBytes(path);
    }

    @Override
    public String getContentAsText(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!welcomeExists())
            {
                throw new RuntimeException("Error getting content as text for " + path + ": no such file");
            }
            try
            {
                return new String(Files.readAllBytes(resolveWelcomePath()), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting content as text for " + path, e);
            }
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage == null)
        {
            throw new RuntimeException("Error getting content as text for " + path);
        }
        return codeStorage.getContentAsText(path);
    }

    @Override
    public boolean exists(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            return true;
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return welcomeExists();
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage != null) && codeStorage.exists(path);
    }

    @Override
    public boolean isFile(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            return false;
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return welcomeExists();
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage != null) && codeStorage.isFile(path);
    }

    @Override
    public boolean isFolder(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            return true;
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return false;
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage != null) && codeStorage.isFolder(path);
    }

    @Override
    public boolean isEmptyFolder(String path)
    {
        // Special case: /
        if (CodeStorageTools.isRootPath(path))
        {
            return this.codeStorages.isEmpty() && !welcomeExists();
        }

        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            return false;
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage != null) && codeStorage.isEmptyFolder(path);
    }

    @Override
    public boolean isVersioned(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage instanceof VersionControlledCodeStorage) && ((VersionControlledCodeStorage) codeStorage).isVersioned(path);
    }

    @Override
    public CodeRepository getRepositoryForPath(String path)
    {
        if (!path.isEmpty() && (path.charAt(0) == '/'))
        {
            int index = path.indexOf('/', 1);
            String rootPath = index != -1 ? path.substring(1, index) : path.substring(1);
            return this.repositoriesByName.get(rootPath);
        }
        return null;
    }

    @Override
    public boolean isRepositoryImmutable(CodeRepository repository)
    {
        return repository != null && this.codeStorageByName.get(repository.getName()) instanceof ImmutableRepositoryCodeStorage;
    }

    @Override
    public long getCurrentRevision(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage instanceof VersionControlledCodeStorage) ? ((VersionControlledCodeStorage) codeStorage).getCurrentRevision(path) : -1;
    }

    @Override
    public LongList getAllRevisions(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage instanceof VersionControlledCodeStorage) ? ((VersionControlledCodeStorage) codeStorage).getAllRevisions(path) : LongLists.immutable.empty();
    }

    @Override
    public RichIterable<Revision> getAllRevisionLogs(RichIterable<String> paths)
    {
        MutableList<Revision> allRevisions = Lists.mutable.empty();
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            if (codeStorage instanceof VersionControlledCodeStorage)
            {
                allRevisions.addAllIterable(((VersionControlledCodeStorage) codeStorage).getAllRevisionLogs(paths));
            }
        }
        return allRevisions;
    }

    public String getDiff(RichIterable<String> paths)
    {
        if (paths.isEmpty())
        {
            return "";
        }
        VersionControlledCodeStorage codeStorage = null;
        for (String path : paths)
        {
            RepositoryCodeStorage pathCodeStorage = getCodeStorage(path);
            if (!(pathCodeStorage instanceof VersionControlledCodeStorage))
            {
                throw new IllegalArgumentException("Cannot generate diff: " + path + " does not belong to a repository that supports diffs");
            }
            if (codeStorage == null)
            {
                codeStorage = (VersionControlledCodeStorage) pathCodeStorage;
            }
            else if (codeStorage != pathCodeStorage)
            {
                throw new IllegalArgumentException("Cannot generate diff: not all paths belong to the same repository");
            }
        }
        if (codeStorage == null)
        {
            throw new IllegalArgumentException("Cannot generate diff: could not determine repository for paths");
        }
        return codeStorage.getDiff(paths);
    }

    @Override
    public RichIterable<String> getUserFiles()
    {
        MutableList<String> userFiles = Lists.mutable.empty();
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            userFiles.addAllIterable(codeStorage.getUserFiles());
        }
        if (welcomeExists())
        {
            userFiles.add(WELCOME_FILE_PATH);
        }
        return userFiles;
    }

    @Override
    public RichIterable<CodeStorageNode> getModifiedUserFiles()
    {
        MutableList<CodeStorageNode> nodes = Lists.mutable.empty();
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            if (codeStorage instanceof VersionControlledCodeStorage)
            {
                nodes.addAllIterable(((VersionControlledCodeStorage) codeStorage).getModifiedUserFiles());
            }
        }
        return nodes;
    }

    @Override
    public RichIterable<CodeStorageNode> getUnversionedFiles()
    {
        MutableList<CodeStorageNode> nodes = Lists.mutable.empty();
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            if (codeStorage instanceof VersionControlledCodeStorage)
            {
                nodes.addAllIterable(((VersionControlledCodeStorage) codeStorage).getUnversionedFiles());
            }
        }
        return nodes;
    }

    @Override
    public OutputStream writeContent(String path)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!hasRootPath())
            {
                throw new RuntimeException("Error trying to get output stream for " + path + ": no such file");
            }
            try
            {
                return Files.newOutputStream(resolveWelcomePath());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error trying to get output stream for " + path, e);
            }
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("Cannot write content to " + path);
        }
        return ((MutableRepositoryCodeStorage) codeStorage).writeContent(path);
    }

    @Override
    public void writeContent(String path, String content)
    {
        // Special case: /welcome.pure
        if (isWelcomePath(path))
        {
            if (!hasRootPath())
            {
                throw new RuntimeException("Error trying to write content to " + path + ": no such file");
            }
            try
            {
                Files.write(resolveWelcomePath(), content.getBytes(StandardCharsets.UTF_8));
                return;
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error trying to write content to " + path, e);
            }
        }

        // General case
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("Cannot write content to '" + path + "'");
        }
        ((MutableRepositoryCodeStorage) codeStorage).writeContent(path, content);
    }

    @Override
    public void createFile(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("Cannot create file " + path);
        }
        ((MutableRepositoryCodeStorage) codeStorage).createFile(path);
    }

    @Override
    public void createFolder(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("Cannot create folder " + path);
        }
        ((MutableRepositoryCodeStorage) codeStorage).createFolder(path);
    }

    @Override
    public void deleteFile(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("Cannot delete file " + path);
        }
        ((MutableRepositoryCodeStorage) codeStorage).deleteFile(path);
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath)
    {
        RepositoryCodeStorage sourceCodeStorage = getCodeStorage(sourcePath);
        RepositoryCodeStorage destinationCodeStorage = getCodeStorage(destinationPath);
        if (!(sourceCodeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("SourceCodeStorage : '" + sourcePath + "' is not an instance of MutableRepositoryCodeStorage");
        }
        if (!(destinationCodeStorage instanceof MutableRepositoryCodeStorage))
        {
            throw new IllegalArgumentException("DestinationCodeStorage : '" + destinationCodeStorage + "' is not an instance of MutableRepositoryCodeStorage");
        }

        if (sourceCodeStorage.equals(destinationCodeStorage))
        {
            ((MutableRepositoryCodeStorage) sourceCodeStorage).moveFile(sourcePath, destinationPath);
        }
        else
        {
            ((MutableRepositoryCodeStorage) destinationCodeStorage).createFile(destinationPath);
            ((MutableRepositoryCodeStorage) destinationCodeStorage).writeContent(destinationPath, sourceCodeStorage.getContentAsText(sourcePath));
            ((MutableRepositoryCodeStorage) sourceCodeStorage).deleteFile(sourcePath);
        }
    }

    @Override
    public void markAsResolved(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableVersionControlledCodeStorage))
        {
            throw new IllegalArgumentException("Cannot mark " + path + " as resolved");
        }
        ((MutableVersionControlledCodeStorage) codeStorage).markAsResolved(path);
    }

    @Override
    public UpdateReport update(long version)
    {
        UpdateReport report = new UpdateReport();
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            if (codeStorage instanceof MutableVersionControlledCodeStorage)
            {
                ((MutableVersionControlledCodeStorage) codeStorage).update(report, version);
            }
        }
        return report;
    }

    @Override
    public UpdateReport update(String path, long version)
    {
        if (StringIterate.isEmpty(path) || CodeStorageTools.isRootPath(path))
        {
            return update(version);
        }

        UpdateReport report = new UpdateReport();
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (codeStorage instanceof MutableVersionControlledCodeStorage)
        {
            ((MutableVersionControlledCodeStorage) codeStorage).update(report, path, version);
        }

        return report;
    }

    @Override
    public RichIterable<String> revert(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage instanceof MutableVersionControlledCodeStorage) ? ((MutableVersionControlledCodeStorage) codeStorage).revert(path) : Lists.immutable.empty();
    }

    @Override
    public InputStream getBase(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableVersionControlledCodeStorage))
        {
            throw new IllegalArgumentException("Cannot getBase for " + path);
        }
        return ((MutableVersionControlledCodeStorage) codeStorage).getBase(path);
    }

    @Override
    public InputStream getConflictOld(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableVersionControlledCodeStorage))
        {
            throw new IllegalArgumentException("Cannot getConflictOld for " + path);
        }
        return ((MutableVersionControlledCodeStorage) codeStorage).getConflictOld(path);
    }

    @Override
    public InputStream getConflictNew(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        if (!(codeStorage instanceof MutableVersionControlledCodeStorage))
        {
            throw new IllegalArgumentException("Cannot getConflictNew for " + path);
        }
        return ((MutableVersionControlledCodeStorage) codeStorage).getConflictNew(path);
    }

    @Override
    public void commit(ListIterable<String> paths, String message)
    {
        if (paths.isEmpty())
        {
            return;
        }

        MutableVersionControlledCodeStorage codeStorage = null;
        for (String path : paths)
        {
            if (path == null)
            {
                throw new PureVCSException("Some of the paths provided for commit were found to be null");
            }
            RepositoryCodeStorage pathCodeStorage = getCodeStorage(path);
            if (!(pathCodeStorage instanceof MutableVersionControlledCodeStorage))
            {
                throw new PureVCSException("Cannot commit " + paths);
            }
            if (codeStorage == null)
            {
                codeStorage = (MutableVersionControlledCodeStorage) pathCodeStorage;
            }
            else if (codeStorage != pathCodeStorage)
            {
                throw new PureVCSException("Cannot commit " + paths);
            }
        }
        if (codeStorage == null)
        {
            throw new PureVCSException("Cannot commit " + paths);
        }
        codeStorage.commit(paths, message);
    }

    @Override
    public void cleanup()
    {
        MutableList<Exception> exceptions = Lists.mutable.empty();
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            if (codeStorage instanceof MutableVersionControlledCodeStorage)
            {
                try
                {
                    ((MutableVersionControlledCodeStorage) codeStorage).cleanup();
                }
                catch (Exception e)
                {
                    exceptions.add(e);
                }
            }
        }
        if (exceptions.size() == 1)
        {
            Exception e = exceptions.get(0);
            if (e instanceof PureVCSException)
            {
                throw (PureVCSException) e;
            }
            throw new PureVCSException("Error performing cleanup", e);
        }
        if (exceptions.size() > 1)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Multiple errors occurred during cleanup:");
            for (Exception e : exceptions)
            {
                pw.println("------");
                e.printStackTrace(pw);
            }
            pw.println("------");
            pw.flush();
            throw new PureVCSException(sw.toString());
        }
    }

    @Override
    public void applyPatch(String path, File patchFile)
    {
        try
        {
            for (RepositoryCodeStorage codeStorage : this.codeStorages)
            {
                if (codeStorage instanceof MutableVersionControlledCodeStorage)
                {
                    ((MutableVersionControlledCodeStorage) codeStorage).applyPatch(path, patchFile);
                }
            }
        }
        catch (Exception e)
        {
            if (e instanceof PureVCSException)
            {
                throw (PureVCSException) e;
            }
            throw new PureVCSException("Error applying patch", e);
        }
    }

    @Override
    public boolean hasConflicts(String path)
    {
        try
        {
            for (RepositoryCodeStorage codeStorage : this.codeStorages)
            {
                if (codeStorage instanceof MutableVersionControlledCodeStorage)
                {
                    if (((MutableVersionControlledCodeStorage) codeStorage).hasConflicts(path))
                    {
                        return true;
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (e instanceof PureVCSException)
            {
                throw (PureVCSException) e;
            }
            throw new PureVCSException("Error checking file conflicts", e);
        }
        return false;
    }

    private RepositoryCodeStorage getCodeStorage(String path)
    {
        return this.codeStorageByName.get(CodeStorageTools.getInitialPathElement(path));
    }

    private boolean isWelcomePath(String path)
    {
        return WELCOME_FILE_PATH.equals(path) || WELCOME_FILE_NAME.equals(path);
    }

    private boolean hasRootPath()
    {
        return this.root != null;
    }

    private Path resolveWelcomePath()
    {
        return this.root.resolve(WELCOME_FILE_NAME);
    }

    private boolean welcomeExists()
    {
        return hasRootPath() && Files.exists(resolveWelcomePath());
    }

    private CodeStorageNode getWelcomeNode()
    {
        return new RootCodeStorageNode(WELCOME_FILE_NAME, false);
    }

    private CodeStorageNode getRootNode()
    {
        return new RootCodeStorageNode("", true);
    }

    public static PureCodeStorage createCodeStorage(Path root, Iterable<? extends CodeRepository> repositories)
    {
        return new PureCodeStorage(root, getCodeStorages(repositories, root, null));
    }

    public static PureCodeStorage createCodeStorage(Path root, Iterable<? extends CodeRepository> repositories, VCSCodeStorageBuilder svnCodeStorageBuilder)
    {
        return new PureCodeStorage(root, getCodeStorages(repositories, root, svnCodeStorageBuilder));
    }

    private static RepositoryCodeStorage[] getCodeStorages(Iterable<? extends CodeRepository> repositories, Path root, VCSCodeStorageBuilder svnCodeStorageBuilder)
    {
        MutableList<RepositoryCodeStorage> codeStorages = Lists.mutable.empty();
        MutableList<SVNCodeRepository> svnCodeRepositories = Lists.mutable.empty();
        for (CodeRepository repository : repositories)
        {
            if (repository instanceof SVNCodeRepository)
            {
                svnCodeRepositories.add((SVNCodeRepository) repository);
            }
            else if (repository instanceof PlatformCodeRepository || repository instanceof GenericCodeRepository)
            {
                codeStorages.add(new ClassLoaderCodeStorage(repository));
            }
            else if (repository instanceof ScratchCodeRepository)
            {
                if (root == null)
                {
                    throw new RuntimeException("Cannot create code storage for scratch without a root directory");
                }
                codeStorages.add(new MutableFSCodeStorage(repository, root.resolve(repository.getName())));
            }
            else
            {
                throw new IllegalArgumentException("Unhandled code repository: " + repository);
            }
        }
        if (svnCodeRepositories.notEmpty())
        {
            if (svnCodeStorageBuilder == null)
            {
                throw new RuntimeException("Cannot create SVN code storage without an SVNCodeStorageBuilder");
            }
            codeStorages.addAllIterable(svnCodeStorageBuilder.createPureCodeStorage(svnCodeRepositories));
        }
        return codeStorages.toArray(new RepositoryCodeStorage[codeStorages.size()]);
    }

    private static ImmutableMap<String, RepositoryCodeStorage> indexCodeStoragesByName(RepositoryCodeStorage... codeStorages)
    {
        MutableMap<String, RepositoryCodeStorage> index = UnifiedMap.newMap(codeStorages.length);
        for (int i = 0; i < codeStorages.length; i++)
        {
            RepositoryCodeStorage codeStorage = codeStorages[i];
            for (CodeRepository repository : codeStorage.getRepositories())
            {
                RepositoryCodeStorage old = index.put(repository.getName(), codeStorage);
                if ((old != null) && (old != codeStorage))
                {
                    throw new IllegalArgumentException("Name conflict for " + repository.getName());
                }
            }
        }
        // platform is required, so add it if it is missing
        if (!index.containsKey(PlatformCodeRepository.NAME))
        {
            index.put(PlatformCodeRepository.NAME, new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()));
        }
        return index.toImmutable();
    }

    private static class RootCodeStorageNode implements CodeStorageNode
    {
        private final String name;
        private final boolean isDirectory;

        RootCodeStorageNode(String name, boolean isDirectory)
        {
            this.name = name;
            this.isDirectory = isDirectory;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public String getPath()
        {
            return "/" + this.name;
        }

        @Override
        public boolean isDirectory()
        {
            return this.isDirectory;
        }

        @Override
        public CodeStorageNodeStatus getStatus()
        {
            return CodeStorageNodeStatus.UNKNOWN;
        }
    }

    public static String getSourceRepoName(String sourceId)
    {
        if (!sourceId.isEmpty() && (sourceId.charAt(0) == '/'))
        {
            int index = sourceId.indexOf('/', 1);
            if (index != -1)
            {
                return sourceId.substring(1, index);
            }
        }
        return null;
    }

    public static boolean isSourceInRepository(String sourceId, String repository)
    {
        if ((sourceId == null) || sourceId.isEmpty())
        {
            return false;
        }

        int start = (sourceId.charAt(0) == '/') ? 1 : 0;
        int nextSlash = sourceId.indexOf('/', start);

        if (repository == null)
        {
            return nextSlash == -1;
        }

        int length = (nextSlash == -1) ? (sourceId.length() - start) : (nextSlash - start);
        return (length == repository.length()) && sourceId.startsWith(repository, start);
    }

    public static final Function<Source, String> GET_SOURCE_REPO = source ->
    {
        String repoName = getSourceRepoName(source.getId());
        return null == repoName ? null : repoName.startsWith("model") ? "model-all" : repoName;
    };


    public static RichIterable<CodeRepository> getVisibleRepositories(RichIterable<CodeRepository> codeRepositories, CodeRepository repository)
    {
        return codeRepositories.select(repository::isVisible);
    }

    public static MutableSet<String> getRepositoryDependenciesByName(RichIterable<CodeRepository> codeRepositories, CodeRepository repository)
    {
        ArrayDeque<CodeRepository> deque = new ArrayDeque<>(codeRepositories.size());
        deque.add(repository);
        return getRepositoryDependenciesByName(codeRepositories, deque);
    }

    public static MutableSet<String> getRepositoryDependenciesByName(RichIterable<CodeRepository> codeRepositories, Iterable<String> repositoryNames)
    {
        return getRepositoryDependenciesByName(codeRepositories, buildDequeFromNames(codeRepositories, repositoryNames));
    }

    private static MutableSet<String> getRepositoryDependenciesByName(RichIterable<CodeRepository> codeRepositories, Deque<CodeRepository> deque)
    {
        MutableSet<String> results = Sets.mutable.with();
        while (!deque.isEmpty())
        {
            CodeRepository repository = deque.removeLast();
            if (results.add(repository.getName()))
            {
                codeRepositories.select(repository::isVisible, deque);
            }
        }
        return results;
    }

    public static SetIterable<CodeRepository> getRepositoryDependencies(RichIterable<CodeRepository> codeRepositories, CodeRepository repository)
    {
        Deque<CodeRepository> deque = new ArrayDeque<>(codeRepositories.size());
        deque.add(repository);
        return getRepositoryDependencies(codeRepositories, deque);
    }

    public static SetIterable<CodeRepository> getRepositoryDependencies(RichIterable<CodeRepository> codeRepositories, Iterable<? extends CodeRepository> repositories)
    {
        return getRepositoryDependencies(codeRepositories, Iterate.addAllTo(repositories, new ArrayDeque<>()));
    }

    private static MutableSet<CodeRepository> getRepositoryDependencies(RichIterable<CodeRepository> codeRepositories, Deque<CodeRepository> deque)
    {
        MutableSet<CodeRepository> results = Sets.mutable.with();
        while (!deque.isEmpty())
        {
            CodeRepository repository = deque.removeLast();
            if (results.add(repository))
            {
                codeRepositories.select(repository::isVisible, deque);
            }
        }
        return results;
    }

    public static SetIterable<String> getRepositoriesDependendingOnByName(RichIterable<CodeRepository> codeRepositories, Iterable<String> repositoryNames)
    {
        return getRepositoriesDependendingOnByName(codeRepositories, buildDequeFromNames(codeRepositories, repositoryNames));
    }

    private static SetIterable<String> getRepositoriesDependendingOnByName(RichIterable<CodeRepository> codeRepositories, Deque<CodeRepository> deque)
    {
        MutableSet<String> results = Sets.mutable.with();
        while (!deque.isEmpty())
        {
            CodeRepository repository = deque.removeLast();
            if (results.add(repository.getName()))
            {
                codeRepositories.select(r -> r.isVisible(repository), deque);
            }
        }
        return results;
    }

    private static Deque<CodeRepository> buildDequeFromNames(RichIterable<CodeRepository> codeRepositories, Iterable<String> repositoryNames)
    {
        MapIterable<String, CodeRepository> codeRepositoriesByName = codeRepositories.groupByUniqueKey(CodeRepository::getName);
        Deque<CodeRepository> deque = new ArrayDeque<>();
        repositoryNames.forEach(n ->
        {
            CodeRepository repository = codeRepositoriesByName.get(n);
            if (repository == null)
            {
                throw new IllegalArgumentException("Unknown repository: \"" + n + "\"");
            }
            deque.add(repository);
        });
        return deque;
    }
}
