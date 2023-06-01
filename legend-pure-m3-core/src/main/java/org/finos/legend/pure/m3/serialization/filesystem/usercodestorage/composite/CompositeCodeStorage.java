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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.*;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.MutableVersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.UpdateReport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.VersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class CompositeCodeStorage implements MutableVersionControlledCodeStorage
{
    private final MutableList<RepositoryCodeStorage> codeStorages;
    private final MutableMap<String, RepositoryCodeStorage> codeStorageByName;
    private final ImmutableMap<String, CodeRepository> repositoriesByName;

    private final ImmutableMap<String, Pair<CodeRepository, RepositoryCodeStorage>> codeStorageByRepositoryName;

    public CompositeCodeStorage(RichIterable<RepositoryCodeStorage> codeStorages)
    {
        this(codeStorages.toArray(new RepositoryCodeStorage[0]));
    }

    public CompositeCodeStorage(RepositoryCodeStorage... codeStorages)
    {
        codeStorages = codeStorages.length == 0 ? new RepositoryCodeStorage[]{new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository())} : codeStorages;
        this.codeStorages = Lists.mutable.with(codeStorages);
        this.codeStorageByName = indexCodeStoragesByName(codeStorages);
        this.repositoriesByName = this.codeStorages.asLazy().flatCollect(RepositoryCodeStorage::getAllRepositories).groupByUniqueKey(CodeRepository::getName, UnifiedMap.newMap(this.codeStorageByName.size())).toImmutable();
        this.codeStorageByRepositoryName = this.codeStorages.asLazy().flatCollect(c -> c.getAllRepositories().collect(r -> Tuples.pair(r, c))).groupByUniqueKey(r -> r.getOne().getName(), UnifiedMap.newMap()).toImmutable();
    }

    @Override
    public void initialize(Message message)
    {
        // Initialize code storage for each code repository
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            codeStorage.initialize(message);
        }
    }

    @Override
    public RepositoryCodeStorage getOriginalCodeStorage(CodeRepository codeRepository)
    {
        return this.codeStorageByRepositoryName.get(codeRepository.getName()).getTwo().getOriginalCodeStorage(codeRepository);
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
            return nodes;
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
                for (CodeRepository repository : codeStorage.getAllRepositories())
                {
                    files.addAllIterable(codeStorage.getFileOrFiles("/" + repository.getName()));
                }
            }
            return files;
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
            return this.codeStorages.isEmpty();
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
            return this.repositoriesByName.get(CodeStorageTools.getInitialPathElement(path));
        }
        return null;
    }

    @Override
    public Optional<String> getCurrentRevision(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage instanceof VersionControlledCodeStorage) ? ((VersionControlledCodeStorage) codeStorage).getCurrentRevision(path) : Optional.empty();
    }

    @Override
    public List<String> getAllRevisions(String path)
    {
        RepositoryCodeStorage codeStorage = getCodeStorage(path);
        return (codeStorage instanceof VersionControlledCodeStorage) ? ((VersionControlledCodeStorage) codeStorage).getAllRevisions(path) : Lists.mutable.empty();
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

    @Override
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
                throw new PureCodeStorageException("Some of the paths provided for commit were found to be null");
            }
            RepositoryCodeStorage pathCodeStorage = getCodeStorage(path);
            if (!(pathCodeStorage instanceof MutableVersionControlledCodeStorage))
            {
                throw new PureCodeStorageException("Cannot commit " + paths);
            }
            if (codeStorage == null)
            {
                codeStorage = (MutableVersionControlledCodeStorage) pathCodeStorage;
            }
            else if (codeStorage != pathCodeStorage)
            {
                throw new PureCodeStorageException("Cannot commit " + paths);
            }
        }
        if (codeStorage == null)
        {
            throw new PureCodeStorageException("Cannot commit " + paths);
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
            if (e instanceof PureCodeStorageException)
            {
                throw (PureCodeStorageException) e;
            }
            throw new PureCodeStorageException("Error performing cleanup", e);
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
            throw new PureCodeStorageException(sw.toString());
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
            if (e instanceof PureCodeStorageException)
            {
                throw (PureCodeStorageException) e;
            }
            throw new PureCodeStorageException("Error applying patch", e);
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
            if (e instanceof PureCodeStorageException)
            {
                throw (PureCodeStorageException) e;
            }
            throw new PureCodeStorageException("Error checking file conflicts", e);
        }
        return false;
    }

    @Override
    public void update(UpdateReport report, long version)
    {
        for (RepositoryCodeStorage codeStorage : this.codeStorages)
        {
            if (codeStorage instanceof MutableVersionControlledCodeStorage)
            {
                ((MutableVersionControlledCodeStorage) codeStorage).update(report, version);
            }
        }
    }

    @Override
    public void update(UpdateReport report, String path, long version)
    {
        if (StringIterate.isEmpty(path) || CodeStorageTools.isRootPath(path))
        {
            update(report, version);
        }
        else
        {
            RepositoryCodeStorage codeStorage = getCodeStorage(path);
            if (codeStorage instanceof MutableVersionControlledCodeStorage)
            {
                ((MutableVersionControlledCodeStorage) codeStorage).update(report, path, version);
            }
        }
    }

    private CodeStorageNode getRootNode()
    {
        return new RootCodeStorageNode("", true);
    }

    private RepositoryCodeStorage getCodeStorage(String path)
    {
        return this.codeStorageByName.get(CodeStorageTools.getInitialPathElement(path));
    }


    private static MutableMap<String, RepositoryCodeStorage> indexCodeStoragesByName(RepositoryCodeStorage... codeStorages)
    {
        MutableMap<String, RepositoryCodeStorage> index = UnifiedMap.newMap(codeStorages.length);
        for (int i = 0; i < codeStorages.length; i++)
        {
            RepositoryCodeStorage codeStorage = codeStorages[i];
            for (CodeRepository repository : codeStorage.getAllRepositories())
            {
                RepositoryCodeStorage old = index.put(repository.getName(), codeStorage);
                if ((old != null) && (old != codeStorage))
                {
                    throw new IllegalArgumentException("Name conflict for " + repository.getName());
                }
            }
        }
        // platform is required, so add it if it is missing
        if (!index.containsKey("platform"))
        {
            throw new RuntimeException("platform can't be found!");
        }
        return index;
    }

    public static class RootCodeStorageNode implements CodeStorageNode
    {
        private final String name;
        private final boolean isDirectory;

        public RootCodeStorageNode(String name, boolean isDirectory)
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

    public static final Function<Source, String> GET_SOURCE_REPO = source -> getSourceRepoName(source.getId());

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
