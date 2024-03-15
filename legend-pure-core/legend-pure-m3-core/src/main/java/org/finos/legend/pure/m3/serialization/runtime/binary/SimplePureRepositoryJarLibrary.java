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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.SetMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.utility.Iterate;

import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimplePureRepositoryJarLibrary extends AbstractPureRepositoryJarLibrary
{
    private final ImmutableMap<String, PureRepositoryJar> jarsByRepository;

    private SimplePureRepositoryJarLibrary(Iterable<? extends PureRepositoryJar> jars)
    {
        super(jars);
        this.jarsByRepository = indexRepositoryJars(jars);
    }

    @Override
    public boolean isKnownRepository(String repositoryName)
    {
        return this.jarsByRepository.containsKey(repositoryName);
    }

    @Override
    public byte[] readFile(String filePath)
    {
        String repository = getFileRepository(filePath);
        PureRepositoryJar jar = this.jarsByRepository.get(repository);
        if (jar == null)
        {
            throw new IllegalArgumentException("Could not find file: " + filePath);
        }
        return jar.readFile(filePath);
    }

    @Override
    public MapIterable<String, byte[]> readFiles(Iterable<String> filePaths)
    {
        SetMultimap<String, String> filesByRepository = Iterate.groupBy(filePaths, AbstractPureRepositoryJarLibrary::getFileRepository, Multimaps.mutable.set.empty());
        int size = filesByRepository.size();
        if (size == 0)
        {
            return Maps.immutable.empty();
        }
        if (size == 1)
        {
            String filePath = filesByRepository.valuesView().getFirst();
            byte[] bytes = readFile(filePath);
            return Maps.immutable.with(filePath, bytes);
        }

        MutableMap<String, byte[]> result = Maps.mutable.withInitialCapacity(size);
        for (String repo : filesByRepository.keysView())
        {
            PureRepositoryJar repoJar = this.jarsByRepository.get(repo);
            if (repoJar != null)
            {
                repoJar.readFiles(filesByRepository.get(repo), result);
            }
        }
        if (result.size() < size)
        {
            MutableList<String> missing = Lists.mutable.withInitialCapacity(size - result.size());
            for (String filePath : filePaths)
            {
                if (!result.containsKey(filePath))
                {
                    missing.add(filePath);
                }
            }
            missing.sortThis();
            throw new RuntimeException(missing.makeString("Could not find files: ", ", ", ""));
        }
        return result;
    }

    @Override
    public MapIterable<String, byte[]> readRepositoryFiles(Iterable<String> repositoryNames)
    {
        MutableMap<String, byte[]> result = Maps.mutable.empty();
        for (String repositoryName : repositoryNames)
        {
            PureRepositoryJar jar = this.jarsByRepository.get(repositoryName);
            if (jar == null)
            {
                throw new IllegalArgumentException("Cannot find repository: " + repositoryName);
            }
            jar.readAllFiles(result);
        }
        return result;
    }

    @Override
    public MapIterable<String, byte[]> readRepositoryFiles(String repositoryName)
    {
        PureRepositoryJar jar = this.jarsByRepository.get(repositoryName);
        if (jar == null)
        {
            throw new IllegalArgumentException("Cannot find repository: " + repositoryName);
        }
        return jar.readAllFiles();
    }

    @Override
    public MapIterable<String, byte[]> readAllFiles()
    {
        MutableMap<String, byte[]> result = Maps.mutable.empty();
        for (PureRepositoryJar jar : this.jarsByRepository.valuesView())
        {
            jar.readAllFiles(result);
        }
        return result;
    }

    @Override
    public RichIterable<String> getRepositoryFiles(String repositoryName)
    {
        PureRepositoryJar jar = this.jarsByRepository.get(repositoryName);
        if (jar == null)
        {
            throw new IllegalArgumentException("The Jar for the repository: " + repositoryName + " can't be found!");
        }
        return jar.getMetadata().getExternalReferenceIndex().keysView();
    }

    private static ImmutableMap<String, PureRepositoryJar> indexRepositoryJars(Iterable<? extends PureRepositoryJar> jars)
    {
        MutableMap<String, PureRepositoryJar> index = Maps.mutable.empty();
        for (PureRepositoryJar jar : jars)
        {
            PureRepositoryJarMetadata metadata = jar.getMetadata();
            String repositoryName = metadata.getRepositoryName();
            PureRepositoryJar old = index.put(repositoryName, jar);
            if (old != null)
            {
                throw new IllegalArgumentException("Multiple Pure repository jars for " + repositoryName);
            }
        }
        return index.toImmutable();
    }

    public static SimplePureRepositoryJarLibrary newLibrary(Iterable<? extends PureRepositoryJar> jars)
    {
        return new SimplePureRepositoryJarLibrary((jars instanceof LazyIterable) ? Lists.mutable.withAll(jars) : jars);
    }

    public static SimplePureRepositoryJarLibrary newLibrary(PureRepositoryJar... jars)
    {
        return newLibrary(ArrayAdapter.adapt(jars));
    }

    public static SimplePureRepositoryJarLibrary newLibraryFromPaths(Iterable<? extends Path> paths)
    {
        return newLibrary(Iterate.collect(paths, PureRepositoryJars::get));
    }

    public static SimplePureRepositoryJarLibrary newLibraryFromDirectory(Path directory)
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory, PureRepositoryJarTools::hasPureJarExtension))
        {
            return newLibraryFromPaths(dirStream);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting Pure repository jar library from directory " + directory, e);
        }
    }

    public static SimplePureRepositoryJarLibrary newLibraryFromURLs(Iterable<? extends URL> urls)
    {
        return newLibrary(Iterate.collect(urls, PureRepositoryJars::get));
    }
}
