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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.multimap.set.SetMultimap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

class PureRepositoryJarBuilder implements Closeable
{
    private final String repositoryName;
    private final JarOutputStream jarStream;

    private PureRepositoryJarBuilder(OutputStream stream, String platformVersion, String modelVersion, String repositoryName, Iterable<? extends SourceSerializationResult> serializationResults) throws IOException
    {
        this.repositoryName = repositoryName;
        this.jarStream = new JarOutputStream(stream, PureManifest.create(platformVersion, modelVersion, (this.repositoryName == null) ? "root" : this.repositoryName));
        writeIndexes(serializationResults);
    }

    void addFile(String path, byte[] bytes) throws IOException
    {
        if ((path == null) || path.isEmpty())
        {
            throw new IllegalArgumentException("File path must be non-null and non-empty");
        }
        if (bytes == null)
        {
            throw new IllegalArgumentException("File bytes must be non-null");
        }
        validatePath(path);
        this.jarStream.putNextEntry(new JarEntry(canonicalizePath(path)));
        this.jarStream.write(bytes);
        this.jarStream.closeEntry();
    }

    @Override
    public void close() throws IOException
    {
        this.jarStream.finish();
    }

    private void writeIndexes(Iterable<? extends SourceSerializationResult> serializationResults) throws IOException
    {
        // Build indexes
        MutableList<String> binPaths = Lists.mutable.empty();
        MutableMap<String, String> instanceDefinitionIndex = Maps.mutable.empty();
        MutableSetMultimap<String, String> externalReferenceIndex = Multimaps.mutable.set.empty();
        for (SourceSerializationResult result : serializationResults)
        {
            String path = result.getSourceId();
            validatePath(path);
            String binPath = PureRepositoryJarTools.purePathToBinaryPath(path);
            binPaths.add(binPath);

            for (String definedInstance : result.getSerializedInstances())
            {
                String old = instanceDefinitionIndex.put(definedInstance, binPath);
                if (old != null)
                {
                    throw new RuntimeException(definedInstance + " defined in more than one source: " + PureRepositoryJarTools.binaryPathToPurePath(old) + " and " + path);
                }
            }
            externalReferenceIndex.putAll(binPath, result.getExternalReferences());
        }
        binPaths.sortThis();

        // Write indexes
        writeInstanceDefinitionIndex(instanceDefinitionIndex);
        writeExternalReferenceIndex(binPaths, externalReferenceIndex);
    }

    private void writeInstanceDefinitionIndex(MapIterable<String, String> instanceDefinitionIndex) throws IOException
    {
        this.jarStream.putNextEntry(new JarEntry(PureRepositoryJarTools.DEFINITION_INDEX_NAME));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.jarStream), 256);
        writer.append("{\n");
        boolean first = true;
        for (String instancePath : instanceDefinitionIndex.keysView().toSortedList())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                writer.append(",\n");
            }
            writer.append("\t\"");
            writer.append(instancePath);
            writer.append("\" : \"");
            writer.append(instanceDefinitionIndex.get(instancePath));
            writer.append("\"");
        }
        writer.append("\n}");
        writer.flush();
        this.jarStream.closeEntry();
    }

    private void writeExternalReferenceIndex(ListIterable<String> paths, SetMultimap<String, String> externalReferenceIndex) throws IOException
    {
        this.jarStream.putNextEntry(new JarEntry(PureRepositoryJarTools.REFERENCE_INDEX_NAME));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.jarStream));
        writer.append("{\n");
        boolean first = true;
        for (String binPath : paths)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                writer.append(",\n");
            }
            writer.append("\t\"");
            writer.append(binPath);
            writer.append("\" : ");
            SetIterable<String> references = externalReferenceIndex.get(binPath);
            if ((references == null) || references.isEmpty())
            {
                writer.append("[]");
            }
            else
            {
                references.toSortedList().appendString(writer, "[\"", "\", \"", "\"]");
            }
        }
        writer.append("\n}");
        writer.flush();
        this.jarStream.closeEntry();
    }

    private String canonicalizePath(String path)
    {
        return (path.charAt(0) == '/') ? path.substring(1) : path;
    }

    private void validatePath(String path)
    {
        if ((path == null) || path.isEmpty())
        {
            throw new IllegalArgumentException("Pure jar path may not be null or empty");
        }

        int start = (path.charAt(0) == '/') ? 1 : 0;
        int end = path.indexOf('/', start);

        if (this.repositoryName == null)
        {
            if (end != -1)
            {
                throw new IllegalArgumentException("Invalid path for Pure jar for root repository: " + path);
            }
        }
        else
        {
            if (end == -1)
            {
                end = path.length();
            }
            int regionLength = end - start;
            if ((this.repositoryName.length() != regionLength) || !path.regionMatches(start, this.repositoryName, 0, regionLength))
            {
                throw new IllegalArgumentException("Invalid path for Pure jar for repository '" + this.repositoryName + "': " + path);

            }
        }
    }

    static PureRepositoryJarBuilder newBuilder(OutputStream stream, String platformVersion, String modelVersion, String repositoryName, Iterable<? extends SourceSerializationResult> serializationResults) throws IOException
    {
        return new PureRepositoryJarBuilder(stream, platformVersion, modelVersion, repositoryName, serializationResults);
    }
}
