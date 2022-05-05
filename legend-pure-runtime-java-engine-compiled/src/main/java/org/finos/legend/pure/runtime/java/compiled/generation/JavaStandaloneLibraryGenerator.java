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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.RepositoryComparator;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.Compile;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.statelistener.VoidJavaCompilerEventObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarOutputStream;


public class JavaStandaloneLibraryGenerator
{
    private final PureRuntime runtime;
    private final Iterable<? extends CompiledExtension> extensions;
    private final boolean addExternalAPI;
    private final String externalAPIPackage;

    private JavaStandaloneLibraryGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage)
    {
        this.runtime = runtime;
        this.extensions = extensions;
        this.addExternalAPI = addExternalAPI;
        this.externalAPIPackage = externalAPIPackage;
    }

    public PureJavaCompiler compile(String repo, boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        return compile(repo, false, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public PureJavaCompiler compile(String repo, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        return compile(getSourcesToCompile(repo), modularMetadataIds, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public PureJavaCompiler compile(Iterable<String> repos, boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        return compile(repos, false, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public PureJavaCompiler compile(Iterable<String> repos, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        return compile(getSourcesToCompile(repos), modularMetadataIds, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public PureJavaCompiler compile(boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        return compile(false, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public PureJavaCompiler compile(boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        return compile(getSourcesToCompile(), modularMetadataIds, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public Generate generateOnly(String repo, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(repo, false, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public Generate generateOnly(String repo, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(getSourcesToCompile(repo), modularMetadataIds, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public Generate generateOnly(Iterable<String> repos, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(repos, false, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public Generate generateOnly(Iterable<String> repos, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(getSourcesToCompile(repos), modularMetadataIds, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public Generate generateOnly(boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(false, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public Generate generateOnly(boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(getSourcesToCompile(), modularMetadataIds, writeJavaSourcesToDisk, pathToWriteTo);
    }

    public PureJavaCompiler compile() throws PureJavaCompileException
    {
        return compile(false, null);
    }

    public void compileAndWriteClasses(Path directory) throws IOException, PureJavaCompileException
    {
        PureJavaCompiler compiler = compile();
        compiler.writeClassJavaSources(directory);
    }

    public void compileAndWriteClasses(JarOutputStream jarOutputStream) throws IOException, PureJavaCompileException
    {
        PureJavaCompiler compiler = compile();
        compiler.writeClassJavaSourcesToJar(jarOutputStream);
    }

    public void serializeAndWriteDistributedMetadata(Path directory) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(this.runtime).serializeToDirectory(directory);
    }

    public void serializeAndWriteDistributedMetadata(String repositoryName, Path directory) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(this.runtime, repositoryName).serializeToDirectory(directory);
    }

    public void serializeAndWriteDistributedMetadata(JarOutputStream jarOutputStream) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(this.runtime).serializeToJar(jarOutputStream);
    }

    public void serializeAndWriteDistributedMetadata(String repositoryName, JarOutputStream jarOutputStream) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(this.runtime, repositoryName).serializeToJar(jarOutputStream);
    }

    public void compileSerializeAndWriteClassesAndMetadata(JarOutputStream jarOutputStream) throws IOException, PureJavaCompileException
    {
        serializeAndWriteDistributedMetadata(jarOutputStream);
        compileAndWriteClasses(jarOutputStream);
    }

    private PureJavaCompiler compile(SortedMap<String, MutableList<Source>> sourcesToCompile, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        GenerateAndCompile generateAndCompile = new GenerateAndCompile();
        if (modularMetadataIds)
        {
            generateAndCompile.generateAndCompileJavaCodeForSources(sourcesToCompile, group -> getSourceCodeGenerator(group, writeJavaSourcesToDisk, pathToWriteTo));
            if (this.addExternalAPI)
            {
                generateAndCompile.generateAndCompileExternalizableAPI(getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo), this.externalAPIPackage);
            }
        }
        else
        {
            JavaSourceCodeGenerator javaSourceCodeGenerator = getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo);
            generateAndCompile.generateAndCompileJavaCodeForSources(sourcesToCompile, javaSourceCodeGenerator);
            if (this.addExternalAPI)
            {
                generateAndCompile.generateAndCompileExternalizableAPI(javaSourceCodeGenerator, this.externalAPIPackage);
            }
        }
        return generateAndCompile.getPureJavaCompiler();
    }

    private Generate generateOnly(SortedMap<String, MutableList<Source>> sourcesToCompile, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        Generate generate = new Generate();
        if (modularMetadataIds)
        {
            generate.generateJavaCodeForSources(sourcesToCompile, group -> getSourceCodeGenerator(group, writeJavaSourcesToDisk, pathToWriteTo));
            if (this.addExternalAPI)
            {
                generate.generateExternalizableAPI(getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo), this.externalAPIPackage);
            }
        }
        else
        {
            JavaSourceCodeGenerator javaSourceCodeGenerator = getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo);
            generate.generateJavaCodeForSources(sourcesToCompile, javaSourceCodeGenerator);
            if (this.addExternalAPI)
            {
                generate.generateExternalizableAPI(javaSourceCodeGenerator, this.externalAPIPackage);
            }
        }
        return generate;
    }

    private JavaSourceCodeGenerator getSourceCodeGenerator(String compileGroup, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        IdBuilder idBuilder = DistributedBinaryGraphSerializer.newIdBuilder(compileGroup, this.runtime.getProcessorSupport());
        JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(this.runtime.getProcessorSupport(), idBuilder, this.runtime.getCodeStorage(), writeJavaSourcesToDisk, pathToWriteTo, false, this.extensions, "UserCode", this.externalAPIPackage);
        javaSourceCodeGenerator.collectClassesToSerialize();
        return javaSourceCodeGenerator;
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile()
    {
        return getSourcesToCompile((Predicate<String>) null);
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile(String repo)
    {
        if (!this.runtime.getCodeStorage().isRepoName(repo))
        {
            throw new IllegalArgumentException("Repository \"" + repo + "\" is not present");
        }
        return getSourcesToCompile(repo::equals);
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile(Iterable<String> repos)
    {
        return getSourcesToCompile((repos instanceof Set) ? (Set<String>) repos : Sets.immutable.withAll(repos).castToSet());
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile(Set<String> repos)
    {
        if (!Iterate.allSatisfy(repos, this.runtime.getCodeStorage()::isRepoName))
        {
            MutableList<String> missingRepos = Iterate.reject(repos, this.runtime.getCodeStorage()::isRepoName, Lists.mutable.empty()).sortThis();
            throw new IllegalArgumentException(missingRepos.makeString("The following repositories are not present: ", ", ", ""));
        }
        return getSourcesToCompile(repos::contains);
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile(Predicate<String> repoFilter)
    {
        MutableMap<String, MutableList<Source>> sourcesByRepo = groupSourcesByRepo(repoFilter);
        SortedMap<String, MutableList<Source>> sortedMap = new TreeMap<>(new RepositoryComparator(this.runtime.getCodeStorage().getAllRepositories()));
        sortedMap.putAll(sourcesByRepo);
        return sortedMap;
    }

    private MutableMap<String, MutableList<Source>> groupSourcesByRepo(Predicate<String> repoFilter)
    {
        MutableMap<String, MutableList<Source>> sourcesByRepo = Maps.mutable.empty();
        this.runtime.getSourceRegistry().getSources().forEach((repoFilter == null) ?
                s -> sourcesByRepo.getIfAbsentPut(PureCodeStorage.getSourceRepoName(s.getId()), Lists.mutable::empty).add(s) :
                s ->
                {
                    String repo = PureCodeStorage.getSourceRepoName(s.getId());
                    if (repoFilter.test(repo))
                    {
                        sourcesByRepo.getIfAbsentPut(repo, Lists.mutable::empty).add(s);
                    }
                });
        return sourcesByRepo;
    }

    public static JavaStandaloneLibraryGenerator newGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage)
    {
        return new JavaStandaloneLibraryGenerator(runtime, extensions, addExternalAPI, externalAPIPackage);
    }

    public static PureJavaCompiler compileOnly(MapIterable<? extends String, ? extends Iterable<? extends StringJavaSource>> javaSources, ListIterable<? extends StringJavaSource> externalizableSources, boolean addExternalAPI) throws PureJavaCompileException
    {
        return compileOnly(javaSources.keyValuesView(), externalizableSources, addExternalAPI);
    }

    public static PureJavaCompiler compileOnly(Iterable<? extends Pair<? extends String, ? extends Iterable<? extends StringJavaSource>>> javaSources, ListIterable<? extends StringJavaSource> externalizableSources, boolean addExternalAPI) throws PureJavaCompileException
    {
        Compile compile = new Compile(new PureJavaCompiler(new Message("")), VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER);
        compile.compileJavaCodeForSources(javaSources);
        if (addExternalAPI)
        {
            compile.compileExternalizableAPI(externalizableSources);
        }
        return compile.getPureJavaCompiler();
    }
}
