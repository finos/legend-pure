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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;

import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class CachedPureRepositoryJarLibrary extends AbstractPureRepositoryJarLibrary
{
    private final ImmutableMap<String, byte[]> fileBytes;
    private final ImmutableMap<String, ImmutableList<String>> filesByRepo;

    private CachedPureRepositoryJarLibrary(Iterable<? extends PureRepositoryJar> jars)
    {
        super(jars);
        Pair<ImmutableMap<String, byte[]>, ImmutableMap<String, ImmutableList<String>>> caches = cacheFiles(jars);
        this.fileBytes = caches.getOne();
        this.filesByRepo = caches.getTwo();
    }

    @Override
    public boolean isKnownRepository(String repositoryName)
    {
        return this.filesByRepo.containsKey(repositoryName);
    }

    @Override
    public byte[] readFile(String filePath)
    {
        byte[] bytes = this.fileBytes.get(filePath);
        if (bytes == null)
        {
            throw new IllegalArgumentException("Could not find file: " + filePath);
        }
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public MapIterable<String, byte[]> readFiles(Iterable<String> filePaths)
    {
        MutableMap<String, byte[]> results = Maps.mutable.empty();
        putFileBytes(filePaths, results);
        return results;
    }

    @Override
    public MapIterable<String, byte[]> readRepositoryFiles(String repositoryName)
    {
        ImmutableList<String> repoFiles = this.filesByRepo.get(repositoryName);
        if (repoFiles == null)
        {
            throw new IllegalArgumentException("Cannot find repository: " + repositoryName);
        }
        MutableMap<String, byte[]> results = UnifiedMap.newMap(repoFiles.size());
        putFileBytes(repoFiles, results);
        return results;
    }

    @Override
    public MapIterable<String, byte[]> readRepositoryFiles(Iterable<String> repositoryNames)
    {
        MutableMap<String, byte[]> results = Maps.mutable.empty();
        for (String repositoryName : repositoryNames)
        {
            ImmutableList<String> repoFiles = this.filesByRepo.get(repositoryName);
            if (repoFiles == null)
            {
                throw new IllegalArgumentException("Cannot find repository: " + repositoryName);
            }
            putFileBytes(repoFiles, results);
        }
        return results;
    }

    @Override
    public MapIterable<String, byte[]> readAllFiles()
    {
        final MutableMap<String, byte[]> results = UnifiedMap.newMap(this.fileBytes.size());
        this.fileBytes.forEachKeyValue(new Procedure2<String, byte[]>()
        {
            @Override
            public void value(String filePath, byte[] bytes)
            {
                results.put(filePath, Arrays.copyOf(bytes, bytes.length));
            }
        });
        return results;
    }

    @Override
    public RichIterable<String> getRepositoryFiles(String repositoryName)
    {
        ImmutableList<String> files = this.filesByRepo.get(repositoryName);
        if (files == null)
        {
            throw new IllegalArgumentException("Unknown repository: " + repositoryName);
        }
        return files;
    }

    private void putFileBytes(Iterable<String> filePaths, MutableMap<String, byte[]> target)
    {
        for (String filePath : filePaths)
        {
            target.put(filePath, readFile(filePath));
        }
    }

    private static Pair<ImmutableMap<String, byte[]>, ImmutableMap<String, ImmutableList<String>>> cacheFiles(Iterable<? extends PureRepositoryJar> jars)
    {
        MutableMap<String, byte[]> fileBytes = Maps.mutable.empty();
        MutableMap<String, ImmutableList<String>> filesByRepo = Maps.mutable.empty();
        for (PureRepositoryJar jar : jars)
        {
            MapIterable<String, byte[]> jarFileBytes = jar.readAllFiles();
            fileBytes = fileBytes.withAllKeyValues(jarFileBytes.keyValuesView());
            filesByRepo.put(jar.getMetadata().getRepositoryName(), jarFileBytes.keysView().toSortedList().toImmutable());
        }
        return Tuples.pair(fileBytes.toImmutable(), filesByRepo.toImmutable());
    }

    public static CachedPureRepositoryJarLibrary newLibrary(Iterable<? extends PureRepositoryJar> jars)
    {
        return new CachedPureRepositoryJarLibrary((jars instanceof LazyIterable) ? FastList.newList(jars) : jars);
    }

    public static CachedPureRepositoryJarLibrary newLibrary(PureRepositoryJar... jars)
    {
        return newLibrary(ArrayAdapter.adapt(jars));
    }

    public static CachedPureRepositoryJarLibrary newLibraryFromPaths(Iterable<? extends Path> paths)
    {
        return newLibrary(Iterate.collect(paths, PureRepositoryJars.PATH_TO_JAR));
    }

    public static CachedPureRepositoryJarLibrary newLibraryFromDirectory(Path directory)
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory))
        {
            return newLibraryFromPaths(dirStream);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting Pure repository jar library from directory " + directory, e);
        }
    }

    public static CachedPureRepositoryJarLibrary newLibraryFromURLs(Iterable<? extends URL> urls)
    {
        return newLibrary(Iterate.collect(urls, PureRepositoryJars.URL_TO_JAR));
    }
}
