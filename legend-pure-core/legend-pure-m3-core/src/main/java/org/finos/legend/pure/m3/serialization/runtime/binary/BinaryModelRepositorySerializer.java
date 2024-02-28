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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.VersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class BinaryModelRepositorySerializer
{
    private final String platformVersion;
    private final String modelVersion;
    private final String repositoryName;
    private final PureRuntime runtime;
    private final MutableList<SourceSerializationResult> serializationResults = Lists.mutable.empty();
    private final MutableMap<String, byte[]> sourceSerializations = Maps.mutable.empty();

    private BinaryModelRepositorySerializer(String platformVersion, String modelVersion, String repositoryName, PureRuntime runtime)
    {
        if ((repositoryName != null) && runtime.getCodeStorage().getRepository(repositoryName) == null)
        {
            throw new IllegalArgumentException("Unknown repository: " + repositoryName);
        }
        this.platformVersion = platformVersion;
        this.modelVersion = modelVersion;
        this.repositoryName = repositoryName;
        this.runtime = runtime;
    }

    private void serialize(OutputStream stream) throws IOException
    {
        serializeSources();
        writeToJar(stream);
    }

    private void serializeSources()
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
        try (Writer writer = BinaryWriters.newBinaryWriter(stream))
        {
            this.runtime.getSourceRegistry().getSources().forEach(source ->
            {
                if (CompositeCodeStorage.isSourceInRepository(source.getId(), this.repositoryName))
                {
                    stream.reset();
                    SourceSerializationResult result = BinaryModelSourceSerializer.serialize(writer, source, this.runtime);
                    this.serializationResults.add(result);
                    this.sourceSerializations.put(source.getId(), stream.toByteArray());
                }
            });
        }
    }

    private void writeToJar(OutputStream stream) throws IOException
    {
        try (PureRepositoryJarBuilder jarBuilder = PureRepositoryJarBuilder.newBuilder(stream, getPlatformVersion(), getModelVersion(), this.repositoryName, this.serializationResults))
        {
            for (String path : this.sourceSerializations.keysView().toSortedListBy(BinaryModelRepositorySerializer::getFilePathSortKey))
            {
                jarBuilder.addFile(PureRepositoryJarTools.purePathToBinaryPath(path), this.sourceSerializations.get(path));
            }
        }
    }

    private String getPlatformVersion()
    {
        return (this.platformVersion == null) ? Version.PLATFORM : this.platformVersion;
    }

    private String getModelVersion()
    {
        if (this.modelVersion != null && CodeRepositoryProviderHelper.notPlatformAndCoreString.accept(this.repositoryName))
        {
            return this.modelVersion;
        }
        if (this.repositoryName == null || !CodeRepositoryProviderHelper.notPlatformAndCoreString.accept(this.repositoryName))
        {
            return null;
        }
        RepositoryCodeStorage codeStorage = this.runtime.getCodeStorage();
        Optional<String> repoRevision = codeStorage instanceof VersionControlledCodeStorage ? ((VersionControlledCodeStorage) codeStorage).getCurrentRevision(this.repositoryName) : Optional.empty();
        return repoRevision.isPresent() ? ("SNAPSHOT-FROM-SVN-" + repoRevision.get()) : null;
    }

    private static Pair<String, String> getFilePathSortKey(String path)
    {
        if (path.charAt(0) != '/')
        {
            path = "/" + path;
        }
        int index = path.lastIndexOf('/');
        return Tuples.pair(path.substring(0, index), path.substring(index + 1));
    }

    public static void serialize(OutputStream stream, String platformVersion, String modelVersion, String repositoryName, PureRuntime runtime) throws IOException
    {
        new BinaryModelRepositorySerializer(platformVersion, modelVersion, repositoryName, runtime).serialize(stream);
    }

    public static void serialize(OutputStream stream, String repository, PureRuntime runtime) throws IOException
    {
        serialize(stream, null, null, repository, runtime);
    }
}
