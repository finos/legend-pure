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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BinaryModelRepositorySerializer
{
    private static final Predicate2<Source, String> IS_SOURCE_IN_REPO = new Predicate2<Source, String>()
    {
        @Override
        public boolean accept(Source source, String repository)
        {
            return isSourceInRepository(source, repository);
        }
    };

    private static final Function<String, Pair<String, String>> FILE_PATH_SORT_KEY = new Function<String, Pair<String, String>>()
    {
        @Override
        public Pair<String, String> valueOf(String path)
        {
            if (path.charAt(0) != '/')
            {
                path = "/" + path;
            }
            int index = path.lastIndexOf('/');
            return Tuples.pair(path.substring(0, index), path.substring(index + 1));
        }
    };

    private final String platformVersion;
    private final String modelVersion;
    private final String repositoryName;
    private final PureRuntime runtime;
    private final MutableList<SourceSerializationResult> serializationResults = Lists.mutable.empty();
    private final MutableMap<String, byte[]> sourceSerializations = Maps.mutable.empty();

    private BinaryModelRepositorySerializer(String platformVersion, String modelVersion, String repositoryName, PureRuntime runtime)
    {
        if ((repositoryName != null) && !runtime.getCodeStorage().isRepoName(repositoryName))
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
            for (Source source : this.runtime.getSourceRegistry().getSources().selectWith(IS_SOURCE_IN_REPO, this.repositoryName))
            {
                stream.reset();
                SourceSerializationResult result = BinaryModelSourceSerializer.serialize(writer, source, this.runtime);
                this.serializationResults.add(result);
                this.sourceSerializations.put(source.getId(), stream.toByteArray());
            }
        }
    }

    private void writeToJar(OutputStream stream) throws IOException
    {
        try (PureRepositoryJarBuilder jarBuilder = PureRepositoryJarBuilder.newBuilder(stream, getPlatformVersion(), getModelVersion(), this.repositoryName, this.serializationResults))
        {
            for (String path : this.sourceSerializations.keysView().toSortedListBy(FILE_PATH_SORT_KEY))
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
        if (this.modelVersion != null)
        {
            return this.modelVersion;
        }
        if ((this.repositoryName == null) || PlatformCodeRepository.NAME.equals(this.repositoryName))
        {
            return null;
        }

        long repoRevision = this.runtime.getCodeStorage().getCurrentRevision(this.repositoryName);
        return (repoRevision == -1L) ? null : ("SNAPSHOT-FROM-SVN-" + repoRevision);
    }

    private static boolean isSourceInRepository(Source source, String repository)
    {
        return isSourceInRepository(source.getId(), repository);
    }

    private static boolean isSourceInRepository(String sourceId, String repository)
    {
        if ((sourceId == null) || sourceId.isEmpty())
        {
            return false;
        }

        int start = (sourceId.charAt(0) == '/') ? 1 : 0;
        int end = sourceId.indexOf('/', start);

        if (repository == null)
        {
            return end == -1;
        }

        if (end == -1)
        {
            return (sourceId.length() == (repository.length() + start)) &&
                    sourceId.startsWith(repository, start);
        }

        return (end == (repository.length() + start)) &&
                (sourceId.charAt(end) == '/') &&
                sourceId.startsWith(repository, start);
    }

    public static void serialize(OutputStream stream, String platformVersion, String modelVersion, String repositoryName, PureRuntime runtime) throws IOException
    {
        new BinaryModelRepositorySerializer(platformVersion, modelVersion, repositoryName, runtime).serialize(stream);
    }

    public static void serialize(OutputStream stream, String platformVersion, String repositoryName, PureRuntime runtime) throws IOException
    {
        serialize(stream, platformVersion, null, repositoryName, runtime);
    }

    public static void serialize(OutputStream stream, String repository, PureRuntime runtime) throws IOException
    {
        serialize(stream, null, null, repository, runtime);
    }
}
