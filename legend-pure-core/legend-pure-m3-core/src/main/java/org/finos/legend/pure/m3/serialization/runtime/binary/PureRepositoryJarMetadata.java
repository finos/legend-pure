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

import java.nio.charset.StandardCharsets;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class PureRepositoryJarMetadata
{
    private final String platformVersion;
    private final String modelVersion;
    private final String repositoryName;
    private final ImmutableMap<String, String> definitionIndex;
    private final ImmutableMap<String, ImmutableSet<String>> externalReferenceIndex;

    private PureRepositoryJarMetadata(PureManifest manifest, ImmutableMap<String, String> definitionIndex, ImmutableMap<String, ImmutableSet<String>> externalReferenceIndex)
    {
        this.platformVersion = manifest.getPurePlatformVersion();
        this.modelVersion = manifest.getPureModelVersion();
        this.repositoryName = manifest.getPureRepositoryName();
        this.definitionIndex = definitionIndex;
        this.externalReferenceIndex = externalReferenceIndex;
    }

    public String getPurePlatformVersion()
    {
        return this.platformVersion;
    }

    public String getPureModelVersion()
    {
        return this.modelVersion;
    }

    public String getRepositoryName()
    {
        return this.repositoryName;
    }

    public ImmutableMap<String, String> getDefinitionIndex()
    {
        return this.definitionIndex;
    }

    public ImmutableMap<String, ImmutableSet<String>> getExternalReferenceIndex()
    {
        return this.externalReferenceIndex;
    }

    private static ImmutableMap<String, String> readDefinitionIndexFromJar(JarInputStream jarStream) throws IOException
    {
        JarEntry entry = jarStream.getNextJarEntry();
        if (!PureRepositoryJarTools.DEFINITION_INDEX_NAME.equals(entry.getName()))
        {
            throw new RuntimeException("Invalid Pure jar: expected " + PureRepositoryJarTools.DEFINITION_INDEX_NAME + ", found " + entry.getName());
        }

        ImmutableMap<String, String> index = readDefinitionIndex(new BufferedReader(new InputStreamReader(jarStream, Charset.defaultCharset())));
        jarStream.closeEntry();
        return index;
    }

    private static ImmutableMap<String, String> readDefinitionIndexFromFile(Path file) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset()))
        {
            return readDefinitionIndex(reader);
        }
    }

    private static ImmutableMap<String, ImmutableSet<String>> readExternalReferenceIndexFromJar(JarInputStream jarStream) throws IOException
    {
        JarEntry entry = jarStream.getNextJarEntry();
        if (!PureRepositoryJarTools.REFERENCE_INDEX_NAME.equals(entry.getName()))
        {
            throw new RuntimeException("Invalid Pure jar: expected " + PureRepositoryJarTools.REFERENCE_INDEX_NAME + ", found " + entry.getName());
        }

        ImmutableMap<String, ImmutableSet<String>> index = readExternalReferenceIndex(new BufferedReader(new InputStreamReader(jarStream, Charset.defaultCharset())));
        jarStream.closeEntry();
        return index;
    }

    private static ImmutableMap<String, ImmutableSet<String>> readExternalReferenceIndexFromFile(Path file) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset()))
        {
            return readExternalReferenceIndex(reader);
        }
    }

    private static ImmutableMap<String, String> readDefinitionIndex(Reader reader) throws IOException
    {
        Map<String, String> rawIndex;
        try
        {
            rawIndex = (Map<String, String>) JSONValue.parseWithException(reader);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Invalid Pure jar: could not parse definition index (" + PureRepositoryJarTools.DEFINITION_INDEX_NAME + ")", e);
        }
        return Maps.immutable.withAll(rawIndex);
    }

    private static ImmutableMap<String, ImmutableSet<String>> readExternalReferenceIndex(Reader reader) throws IOException
    {
        Map<?, ?> rawIndex;
        try
        {
            rawIndex = (Map<?, ?>) JSONValue.parseWithException(reader);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Invalid Pure jar: could not parse external reference index (" + PureRepositoryJarTools.REFERENCE_INDEX_NAME + ")", e);
        }

        MutableMap<String, ImmutableSet<String>> index = Maps.mutable.withInitialCapacity(rawIndex.size());
        for (Map.Entry<?, ?> keyValue : rawIndex.entrySet())
        {
            index.put((String) keyValue.getKey(), Sets.immutable.withAll((Iterable<String>) keyValue.getValue()));
        }
        return index.toImmutable();
    }

    public static PureRepositoryJarMetadata getPureMetadata(Path jarPath) throws IOException
    {
        try (JarInputStream stream = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarPath))))
        {
            return getPureMetadataFromJar(stream);
        }
    }

    public static PureRepositoryJarMetadata getPureMetadata(URL url) throws IOException
    {
        try (JarInputStream stream = new JarInputStream(new BufferedInputStream(url.openStream())))
        {
            return getPureMetadataFromJar(stream);
        }
    }

    public static PureRepositoryJarMetadata getPureMetadata(byte[] bytes) throws IOException
    {
        try (JarInputStream stream = new JarInputStream(new ByteArrayInputStream(bytes)))
        {
            return getPureMetadataFromJar(stream);
        }
    }

    public static PureRepositoryJarMetadata getPureMetadataFromUnpackedJar(Path root) throws IOException
    {
        Path manifestPath = findCaseInsensitive(root, PureRepositoryJarTools.META_INF_DIR_NAME, "MANIFEST.MF");
        Manifest manifest = new Manifest();
        try (InputStream manifestStream = new BufferedInputStream(Files.newInputStream(manifestPath)))
        {
            manifest.read(manifestStream);
        }
        PureManifest pureManifest = PureManifest.create(manifest);

        Path definitionIndexPath = findCaseInsensitive(root, PureRepositoryJarTools.META_INF_DIR_NAME, PureRepositoryJarTools.DEFINITION_INDEX_FILENAME);
        ImmutableMap<String, String> definitionIndex = readDefinitionIndexFromFile(definitionIndexPath);

        Path externalReferenceIndexPath = findCaseInsensitive(root, PureRepositoryJarTools.META_INF_DIR_NAME, PureRepositoryJarTools.REFERENCE_INDEX_FILENAME);
        ImmutableMap<String, ImmutableSet<String>> externalReferenceIndex = readExternalReferenceIndexFromFile(externalReferenceIndexPath);

        return new PureRepositoryJarMetadata(pureManifest, definitionIndex, externalReferenceIndex);
    }

    private static PureRepositoryJarMetadata getPureMetadataFromJar(JarInputStream stream) throws IOException
    {
        PureManifest manifest = PureManifest.create(stream.getManifest());
        ImmutableMap<String, String> definitionIndex = readDefinitionIndexFromJar(stream);
        ImmutableMap<String, ImmutableSet<String>> externalReferenceIndex = readExternalReferenceIndexFromJar(stream);
        return new PureRepositoryJarMetadata(manifest, definitionIndex, externalReferenceIndex);
    }

    private static Path findCaseInsensitive(Path root, String name, String... moreNames) throws IOException
    {
        // Check if we are lucky and can find it right away
        // Stick to Path.resolve(String) to accommodate JSI, whose Path's are special.
        Path givenPath = root.resolve(name);
        for (String moreName : moreNames)
        {
            givenPath = givenPath.resolve(moreName);
        }
        if (Files.exists(givenPath))
        {
            return givenPath;
        }

        // Search through directories
        MutableList<Path> results = Lists.mutable.empty();
        findInDirectory(root, caseInsensitiveNameEquals(name), results);
        if (results.isEmpty())
        {
            throw new FileNotFoundException("Cannot find " + givenPath);
        }
        for (String nextName : moreNames)
        {
            MutableList<Path> nextResults = Lists.mutable.empty();
            DirectoryStream.Filter<Path> nextNameFilter = caseInsensitiveNameEquals(nextName);
            for (Path directory : results)
            {
                findInDirectory(directory, nextNameFilter, nextResults);
            }
            if (nextResults.isEmpty())
            {
                throw new FileNotFoundException("Cannot find " + givenPath);
            }
            results = nextResults;
        }
        return results.get(0);
    }

    private static void findInDirectory(Path directory, DirectoryStream.Filter<Path> filter, MutableCollection<Path> results) throws IOException
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory, filter))
        {
            results.addAllIterable(dirStream);
        }
    }

    private static DirectoryStream.Filter<Path> caseInsensitiveNameEquals(String name)
    {
        return entry -> name.equalsIgnoreCase(entry.getFileName().toString());
    }
}
