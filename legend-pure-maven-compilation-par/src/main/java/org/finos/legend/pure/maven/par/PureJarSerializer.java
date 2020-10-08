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

package org.finos.legend.pure.maven.par;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelRepositorySerializer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PureJarSerializer
{
    public static final String ARCHIVE_FILE_EXTENSION = "par";

    public static void writePureRepositoryJar(Path outputDirectory, Path sourceDirectory, String platformVersion, String modelVersion, String repositoryName) throws IOException
    {
        writePureRepositoryJars(outputDirectory, sourceDirectory, platformVersion, modelVersion, Sets.immutable.with(repositoryName));
    }

    public static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, String modelVersion, String... repositoryNames) throws IOException
    {
        writePureRepositoryJars(outputDirectory, sourceDirectory, platformVersion, modelVersion, Sets.immutable.with(repositoryNames));
    }

    public static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, String modelVersion, Iterable<String> repositoryNames) throws IOException
    {
        writePureRepositoryJars(outputDirectory, sourceDirectory, platformVersion, modelVersion, Sets.immutable.withAll(repositoryNames));
    }

    private static void writePureRepositoryJars(Path outputDirectory, Path sourceDirectory, String platformVersion, String modelVersion, ImmutableSet<String> repositoryNames) throws IOException
    {
        SetIterable<CodeRepository> repositoriesForCompilation = PureCodeStorage.getRepositoryDependencies(PureRepositoriesExternal.repositories, LazyIterate.collect(repositoryNames, (String repo) -> PureRepositoriesExternal.repositories.select(p ->p.getName().equals(repo)).getFirst()));
        MutableCodeStorage codeStorage = null;
        if (null== sourceDirectory)
        {
            codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(repositoriesForCompilation));
        }
        else
        {
            MutableList<RepositoryCodeStorage> repos = PureRepositoriesExternal.repositories.select(r -> !(r instanceof PlatformCodeRepository)).collect(r -> (RepositoryCodeStorage)new MutableFSCodeStorage(r, sourceDirectory.resolve(r.getName()))).toList();
            repos.add(new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()));
            codeStorage = new PureCodeStorage(sourceDirectory, repos.toArray(new RepositoryCodeStorage[1]));
        }

        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).buildAndInitialize();

        Files.createDirectories(outputDirectory);
        for (String repositoryName : repositoryNames)
        {
            Path outputFile = outputDirectory.resolve("pure-" + repositoryName + "." + ARCHIVE_FILE_EXTENSION);
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputFile)))
            {
                BinaryModelRepositorySerializer.serialize(outputStream, platformVersion, modelVersion, repositoryName, runtime);
            }
        }
    }
}
