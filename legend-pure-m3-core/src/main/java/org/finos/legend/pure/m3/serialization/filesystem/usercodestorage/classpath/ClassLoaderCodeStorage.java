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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.multimap.set.SetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.function.checked.CheckedFunction;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.AbstractMultipleRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.ImmutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.tools.FileTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassLoaderCodeStorage extends AbstractMultipleRepositoryCodeStorage implements ImmutableRepositoryCodeStorage
{
    private static final ConcurrentMutableMap<String, ImmutableMap<String, ClassLoaderCodeStorageNode>> MAIN_CLASS_LOADER_CACHE = ConcurrentHashMap.newMap();

    private static final Predicate<CodeStorageNode> IS_PURE_FILE = new Predicate<CodeStorageNode>()
    {
        @Override
        public boolean accept(CodeStorageNode node)
        {
            return !node.isDirectory() && CodeStorageTools.isPureFilePath(node.getPath());
        }
    };

    private static final Function<URL, ResourceType> GET_RESOURCE_TYPE = new CheckedFunction<URL, ResourceType>()
    {
        @Override
        public ResourceType safeValueOf(URL url) throws IOException
        {
            return getResourceType(url);
        }
    };

    private static final Function<String, ClassLoaderDirectoryNode> NEW_DIR_NODE = new Function<String, ClassLoaderDirectoryNode>()
    {
        @Override
        public ClassLoaderDirectoryNode valueOf(String path)
        {
            return new ClassLoaderDirectoryNode(path);
        }
    };

    private final ClassLoader classLoader;
    private volatile ImmutableMap<String, ImmutableMap<String, ClassLoaderCodeStorageNode>> nodesByPathByRepo; //NOSONAR we actually want to protect the pointer

    public ClassLoaderCodeStorage(ClassLoader classLoader, Iterable<? extends CodeRepository> repositories)
    {
        super(repositories);
        this.classLoader = (classLoader == null) ? getClass().getClassLoader() : classLoader;
    }

    public ClassLoaderCodeStorage(ClassLoader classLoader, CodeRepository... repositories)
    {
        this(classLoader, ArrayAdapter.adapt(repositories));
    }

    public ClassLoaderCodeStorage(ClassLoader classLoader, CodeRepository repository)
    {
        super(repository);
        this.classLoader = (classLoader == null) ? getClass().getClassLoader() : classLoader;
    }

    public ClassLoaderCodeStorage(Iterable<? extends CodeRepository> repositories)
    {
        this(null, repositories);
    }

    public ClassLoaderCodeStorage(CodeRepository... repositories)
    {
        this(null, repositories);
    }

    public ClassLoaderCodeStorage(CodeRepository repository)
    {
        this(null, repository);
    }

    @Override
    public void initialize(Message message)
    {
        // Do nothing
    }

    @Override
    public CodeStorageNode getNode(String path)
    {
        return findNodeOrThrow(path);
    }

    @Override
    public RichIterable<CodeStorageNode> getFiles(String path)
    {
        ClassLoaderCodeStorageNode node = findNodeOrThrow(path);
        if (!node.isDirectory())
        {
            return Lists.immutable.empty();
        }
        ClassLoaderDirectoryNode dirNode = (ClassLoaderDirectoryNode)node;
        RichIterable<CodeStorageNode> children = dirNode.getChildren();
        if (children == null)
        {
            dirNode.initializeChildren(getRepoNodesByPath(CodeStorageTools.getInitialPathElement(node.getPath())));
            children = dirNode.getChildren();
        }
        return children;
    }

    @Override
    public RichIterable<String> getUserFiles()
    {
        initializeNodes();
        return getRepositoryNames().flatCollect(new Function<String, RichIterable<String>>()
        {
            @Override
            public RichIterable<String> valueOf(String repoName)
            {
                ImmutableMap<String, ClassLoaderCodeStorageNode> repoNodesByPath = getRepoNodesByPath(repoName);
                return (repoNodesByPath == null) ? Lists.immutable.<String>empty() : repoNodesByPath.valuesView().collectIf(IS_PURE_FILE, CodeStorageNode.GET_PATH);
            }
        });
    }

    @Override
    public RichIterable<String> getFileOrFiles(String path)
    {
        ClassLoaderCodeStorageNode node = findNodeOrThrow(path);
        if (!node.isDirectory())
        {
            return Lists.immutable.with(node.getPath());
        }

        ClassLoaderDirectoryNode dirNode = (ClassLoaderDirectoryNode)node;
        RichIterable<ClassLoaderCodeStorageNode> descendents = dirNode.getDescendants();
        if (descendents == null)
        {
            dirNode.initializeDescendents(getRepoNodesByPath(CodeStorageTools.getInitialPathElement(node.getPath())));
            descendents = dirNode.getDescendants();
        }
        return descendents.collectIf(Predicates.not(CodeStorageNode.IS_DIRECTORY), CodeStorageNode.GET_PATH);
    }

    @Override
    public InputStream getContent(String path)
    {
        ClassLoaderCodeStorageNode node = findNodeOrThrow(path);
        if (node.isDirectory())
        {
            throw new RuntimeException("Cannot get content for directory: " + path);
        }
        try
        {
            return ((ClassLoaderFileNode)node).getContent();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting content for: " + path, e);
        }
    }

    @Override
    public byte[] getContentAsBytes(String path)
    {
        ClassLoaderCodeStorageNode node = findNodeOrThrow(path);
        if (node.isDirectory())
        {
            throw new RuntimeException("Cannot get content as bytes for directory: " + path);
        }
        try
        {
            return ((ClassLoaderFileNode)node).getContentAsBytes();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting content as bytes for: " + path, e);
        }
    }

    @Override
    public String getContentAsText(String path)
    {
        ClassLoaderCodeStorageNode node = findNodeOrThrow(path);
        if (node.isDirectory())
        {
            throw new RuntimeException("Cannot get content as text for directory: " + path);
        }
        try
        {
            return ((ClassLoaderFileNode)node).getContentAsText();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting content as text for: " + path, e);
        }
    }

    @Override
    public boolean exists(String path)
    {
        return findNode(path) != null;
    }

    @Override
    public boolean isFile(String path)
    {
        ClassLoaderCodeStorageNode node = findNode(path);
        return (node != null) && !node.isDirectory();
    }

    @Override
    public boolean isFolder(String path)
    {
        ClassLoaderCodeStorageNode node = findNode(path);
        return (node != null) && node.isDirectory();
    }

    @Override
    public boolean isEmptyFolder(String path)
    {
        ClassLoaderCodeStorageNode node = findNode(path);
        if ((node == null) || !node.isDirectory())
        {
            return false;
        }
        ClassLoaderDirectoryNode dirNode = (ClassLoaderDirectoryNode)node;
        RichIterable<CodeStorageNode> children = dirNode.getChildren();
        if (children == null)
        {
            dirNode.initializeChildren(getRepoNodesByPath(CodeStorageTools.getInitialPathElement(node.getPath())));
            children = dirNode.getChildren();
        }
        return children.isEmpty();
    }

    protected ClassLoaderCodeStorageNode findNodeOrThrow(String path)
    {
        ClassLoaderCodeStorageNode node = findNode(path);
        if (node == null)
        {
            throw new RuntimeException("Cannot find path '" + path + "'");
        }
        return node;
    }


    protected ClassLoaderCodeStorageNode findNode(String path)
    {
        initializeNodes();
        String fullPath = CodeStorageTools.canonicalizePath(path);
        String repoName = CodeStorageTools.getInitialPathElement(fullPath);
        return getNodeInRepo(repoName, fullPath);
    }

    protected ClassLoaderCodeStorageNode getNodeInRepo(String repositoryName, String path)
    {
        ImmutableMap<String, ClassLoaderCodeStorageNode> repoNodesByPath = getRepoNodesByPath(repositoryName);
        return (repoNodesByPath == null) ? null : repoNodesByPath.get(path);
    }

    protected ImmutableMap<String, ClassLoaderCodeStorageNode> getRepoNodesByPath(String repositoryName)
    {
        return this.nodesByPathByRepo.get(repositoryName);
    }

    private void initializeNodes()
    {
        if (this.nodesByPathByRepo == null)
        {
            synchronized (this)
            {
                if (this.nodesByPathByRepo == null)
                {
                    try
                    {
                        this.nodesByPathByRepo = buildNodeMap();
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException("Error initializing code storage", e);
                    }
                }
            }
        }
    }

    private ImmutableMap<String, ImmutableMap<String, ClassLoaderCodeStorageNode>> buildNodeMap() throws IOException
    {
        RichIterable<String> repositoryNames = getRepositoryNames();
        final MutableMap<String, ImmutableMap<String, ClassLoaderCodeStorageNode>> result = UnifiedMap.newMap(repositoryNames.size());
        final boolean canUseCache = canUseMainClassLoaderCache();
        if (canUseCache)
        {
            repositoryNames = repositoryNames.toSet();
            Iterator<String> repoNameIterator = repositoryNames.iterator();
            while (repoNameIterator.hasNext())
            {
                String repositoryName = repoNameIterator.next();
                ImmutableMap<String, ClassLoaderCodeStorageNode> repositoryNodes = MAIN_CLASS_LOADER_CACHE.get(repositoryName);
                if (repositoryNodes != null)
                {
                    result.put(repositoryName, repositoryNodes);
                    repoNameIterator.remove();
                }
            }
        }

        if (repositoryNames.notEmpty())
        {
            MutableMap<String, MutableMap<String, ClassLoaderCodeStorageNode>> workingSet = UnifiedMap.newMap(repositoryNames.size());
            SetMultimap<String, URL> pathURLs = findURLsByPath(repositoryNames);
            for (Pair<String, RichIterable<URL>> pair : pathURLs.keyMultiValuePairsView())
            {
                String path = pair.getOne();
                RichIterable<URL> urls = pair.getTwo();
                ClassLoaderCodeStorageNode node;
                if (urls.size() == 1)
                {
                    URL url = urls.getFirst();
                    switch (url.getProtocol())
                    {
                        case "file":
                        {
                            Path filePath = fileURLToPath(url);
                            node = Files.isDirectory(filePath) ? new ClassLoaderDirectoryNode(path) : new ClassLoaderPathFileNode(path, filePath);
                            break;
                        }
                        case "jar":
                        {
                            node = (getJarURLResourceType(url) == ResourceType.DIRECTORY) ? new ClassLoaderDirectoryNode(path) : new ClassLoaderURLFileNode(path, url);
                            break;
                        }
                        default:
                        {
                            throw new RuntimeException("Unhandled URL: " + url);
                        }
                    }
                }
                else
                {
                    EnumSet<ResourceType> resourceTypes = urls.collect(GET_RESOURCE_TYPE, EnumSet.noneOf(ResourceType.class));
                    if (resourceTypes.size() > 1)
                    {
                        throw new RuntimeException("Invalid URLs for '" + path + "' - both file and directory URLs: " + urls);
                    }
                    if (resourceTypes.contains(ResourceType.DIRECTORY))
                    {
                        node = new ClassLoaderDirectoryNode(path);
                    }
                    else
                    {
                        // Check that all URLs have the same content
                        byte[] bytes = null;
                        for (URL url : urls)
                        {
                            byte[] content = readURLContent(url);
                            if (bytes == null)
                            {
                                bytes = content;
                            }
                            else if (!Arrays.equals(bytes, content))
                            {
                                throw new RuntimeException("Invalid URLs for '" + path + "' - different content: " + urls);
                            }
                        }
                        // Choose just one URL for the actual node
                        URL firstURL = urls.minBy(Functions.getToString());
                        node = "file".equals(firstURL.getProtocol()) ? new ClassLoaderPathFileNode(path, fileURLToPath(firstURL)) : new ClassLoaderURLFileNode(path, firstURL);
                    }
                }
                String repositoryName = CodeStorageTools.getInitialPathElement(path);
                workingSet.getIfAbsentPut(repositoryName, Functions0.<String, ClassLoaderCodeStorageNode>newUnifiedMap()).put(path, node);
            }
            workingSet.forEachKeyValue(new Procedure2<String, MutableMap<String, ClassLoaderCodeStorageNode>>()
            {
                @Override
                public void value(String repositoryName, MutableMap<String, ClassLoaderCodeStorageNode> nodeMap)
                {
                    for (String path : nodeMap.keysView().toList())
                    {
                        for (int slashIndex = path.indexOf('/', 1); slashIndex != -1; slashIndex = path.indexOf('/', slashIndex + 1))
                        {
                            String ancestorPath = path.substring(0, slashIndex);
                            ClassLoaderCodeStorageNode ancestorNode = nodeMap.getIfAbsentPutWithKey(ancestorPath, NEW_DIR_NODE);
                            if (!ancestorNode.isDirectory())
                            {
                                throw new RuntimeException("Found both file and directory for path: " + ancestorPath);
                            }
                        }
                    }
                    ImmutableMap<String, ClassLoaderCodeStorageNode> immutableNodeMap = nodeMap.toImmutable();
                    result.put(repositoryName, immutableNodeMap);
                    if (canUseCache)
                    {
                        MAIN_CLASS_LOADER_CACHE.putIfAbsent(repositoryName, immutableNodeMap);
                    }
                }
            });
        }

        return result.toImmutable();
    }

    private boolean canUseMainClassLoaderCache()
    {
        return this.classLoader == getClass().getClassLoader();
    }

    private SetMultimap<String, URL> findURLsByPath(Iterable<String> repositoryNames)
    {
        MutableSetMultimap<String, URL> pathURLs = Multimaps.mutable.set.empty();
        try
        {
            MutableSet<Path> searchedPaths = Sets.mutable.empty();
            MutableSet<String> searchedJarFileURLs = Sets.mutable.empty();
            for (String repository : repositoryNames)
            {
                Enumeration<URL> repoURLs = this.classLoader.getResources(repository);
                while (repoURLs.hasMoreElements())
                {
                    URL url = repoURLs.nextElement();
                    switch (url.getProtocol())
                    {
                        case "file":
                        {
                            Path path = fileURLToPath(url);
                            if (searchedPaths.add(path))
                            {
                                BasicFileAttributes attributes = FileTools.getBasicFileAttributes(path);
                                if (attributes != null)
                                {
                                    if (!attributes.isDirectory())
                                    {
                                        throw new RuntimeException("Invalid URL for repository '" + repository + "': " + url);
                                    }
                                    collectFromPath(CodeStorage.ROOT_PATH, path, pathURLs);
                                }
                            }
                            break;
                        }
                        case "jar":
                        {
                            JarURLConnection connection = (JarURLConnection)url.openConnection();
                            String jarFileURL = connection.getJarFileURL().toString();
                            if (searchedJarFileURLs.add(jarFileURL))
                            {
                                collectFromJarFile(connection.getJarFile(), jarFileURL, pathURLs);
                            }
                            break;
                        }
                        default:
                        {
                            throw new RuntimeException("Unhandled URL for repository '" + repository + "': " + url);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error initializing " + getClass().getSimpleName() + " for repositories: " + getRepositoryNames().toSortedList(), e);
        }
        return pathURLs;
    }

    private void collectFromPath(String parentPath, Path filePath, MutableMultimap<String, URL> pathURLs) throws IOException
    {
        String path = CodeStorageTools.joinPaths(parentPath, filePath.getFileName().toString());
        pathURLs.put(path, filePath.toAbsolutePath().normalize().toUri().toURL());
        if (Files.isDirectory(filePath))
        {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(filePath))
            {
                for (Path childPath : dirStream)
                {
                    collectFromPath(path, childPath, pathURLs);
                }
            }
        }
    }

    private void collectFromJarFile(JarFile jarFile, String jarFileURL, MutableMultimap<String, URL> pathURLs) throws IOException
    {
        Enumeration<JarEntry> entries = jarFile.entries();  //NOSONAR JARs are trusted
        while (entries.hasMoreElements())
        {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            int firstSlashIndex = entryName.indexOf('/');
            String initialElement = (firstSlashIndex == -1) ? entryName : entryName.substring(0, firstSlashIndex);
            if (hasRepo(initialElement))
            {
                pathURLs.put(CodeStorageTools.canonicalizePath(entryName), createJarEntryURL(jarFileURL, entryName));
            }
        }
    }

    private URL createJarEntryURL(String jarFileURL, String entryName) throws MalformedURLException
    {
        return new URL("jar:" + jarFileURL + "!/" + entryName);
    }

    private static ResourceType getResourceType(URL url) throws IOException
    {
        switch (url.getProtocol())
        {
            case "file":
            {
                return getFileURLResourceType(url);
            }
            case "jar":
            {
                return getJarURLResourceType(url);
            }
            default:
            {
                throw new RuntimeException("Invalid URL: " + url);
            }
        }
    }

    private static ResourceType getFileURLResourceType(URL url) throws IOException
    {
        Path path = fileURLToPath(url);
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        if (attributes.isDirectory())
        {
            return ResourceType.DIRECTORY;
        }
        if (attributes.isRegularFile())
        {
            return ResourceType.FILE;
        }
        throw new RuntimeException("Invalid URL: " + url);
    }

    private static ResourceType getJarURLResourceType(URL url) throws IOException
    {
        URLConnection urlConnection = url.openConnection();
        if (!(urlConnection instanceof JarURLConnection))
        {
            throw new RuntimeException("Invalid URL: " + url);
        }
        JarEntry entry = ((JarURLConnection)urlConnection).getJarEntry();
        return entry.isDirectory() ? ResourceType.DIRECTORY : ResourceType.FILE;
    }

    private static Path fileURLToPath(URL fileURL)
    {
        try
        {
            return Paths.get(fileURL.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException("Invalid URL: " + fileURL, e);
        }
    }

    private static byte[] readURLContent(URL url) throws IOException
    {
        if ("file".equals(url.getProtocol()))
        {
            return Files.readAllBytes(fileURLToPath(url));
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int bufferSize = 8196;
        byte[] buffer = new byte[bufferSize];
        try (InputStream stream = url.openStream())
        {
            for (int read = stream.read(buffer, 0, bufferSize); read != -1; read = stream.read(buffer, 0, bufferSize))
            {
                bytes.write(buffer, 0, read);
            }
        }
        return bytes.toByteArray();
    }

    private enum ResourceType
    {
        FILE, DIRECTORY;
    }
}
