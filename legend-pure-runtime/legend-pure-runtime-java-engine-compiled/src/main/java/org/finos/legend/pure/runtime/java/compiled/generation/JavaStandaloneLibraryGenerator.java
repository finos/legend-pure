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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.PureCompilerSerializer;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.RepositoryComparator;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.Compile;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.Log;
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
    private final boolean useLegacyMetadataForExternalAPI;
    private final Log log;
    private final boolean generatePureTests;

    private JavaStandaloneLibraryGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage, boolean useLegacyMetadataForExternalAPI, boolean generatePureTests, Log log)
    {
        this.runtime = runtime;
        this.extensions = extensions;
        this.addExternalAPI = addExternalAPI;
        this.externalAPIPackage = externalAPIPackage;
        this.useLegacyMetadataForExternalAPI = useLegacyMetadataForExternalAPI;
        this.log = log;
        this.generatePureTests = generatePureTests;
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

    public void compileAndWriteClasses(Path directory, Log log) throws IOException, PureJavaCompileException
    {
        PureJavaCompiler compiler = compile();
        compiler.writeClassJavaSources(directory, log);
    }

    public void compileAndWriteClasses(JarOutputStream jarOutputStream) throws IOException, PureJavaCompileException
    {
        PureJavaCompiler compiler = compile();
        compiler.writeClassJavaSourcesToJar(jarOutputStream);
    }

    public void serializeAndWriteMetadata(Path directory)
    {
        buildMetadataSerializer().serializeAll(directory);
    }

    public void serializeAndWriteMetadata(Path directory, String... repositories)
    {
        buildMetadataSerializer().serializeModules(directory, repositories);
    }

    public void serializeAndWriteMetadata(Path directory, Iterable<? extends String> repositories)
    {
        buildMetadataSerializer().serializeModules(directory, repositories);
    }

    public void serializeAndWriteMetadata(JarOutputStream jarOutputStream)
    {
        buildMetadataSerializer().serializeAll(jarOutputStream);
    }

    public void serializeAndWriteMetadata(JarOutputStream jarOutputStream, String... repositories)
    {
        buildMetadataSerializer().serializeModules(jarOutputStream, repositories);
    }

    public void serializeAndWriteMetadata(JarOutputStream jarOutputStream, Iterable<? extends String> repositories)
    {
        buildMetadataSerializer().serializeModules(jarOutputStream, repositories);
    }

    private PureCompilerSerializer buildMetadataSerializer()
    {
        ProcessorSupport processorSupport = this.runtime.getProcessorSupport();
        ReferenceIdProviders referenceIds = ReferenceIdProviders.builder().withProcessorSupport(processorSupport).withAvailableExtensions().build();
        FileSerializer fileSerializer = FileSerializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions().build())
                .withConcreteElementSerializer(ConcreteElementSerializer.builder(processorSupport).withLoadedExtensions().withReferenceIdProviders(referenceIds).build())
                .withModuleMetadataSerializer(ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
        return PureCompilerSerializer.builder()
                .withFileSerializer(fileSerializer)
                .withModuleMetadataGenerator(ModuleMetadataGenerator.fromProcessorSupport(processorSupport))
                .withProcessorSupport(processorSupport)
                .build();
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
        GenerateAndCompile generateAndCompile = new GenerateAndCompile(new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                if (!message.startsWith("Generating Java sources"))
                {
                    log.debug("  " + message);
                }
            }
        });

        if (modularMetadataIds)
        {
            generateAndCompile.generateAndCompileJavaCodeForSources(sourcesToCompile, group -> getSourceCodeGenerator(group, writeJavaSourcesToDisk, pathToWriteTo));
            if (this.addExternalAPI)
            {
                generateAndCompile.generateAndCompileExternalizableAPI(getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo), this.externalAPIPackage);
            }
            else
            {
                log.debug("    Skipping External API generation");
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
            else
            {
                log.debug("    Skipping External API generation");
            }
        }
        return generateAndCompile.getPureJavaCompiler();
    }

    private Generate generateOnly(SortedMap<String, MutableList<Source>> sourcesToCompile, boolean modularMetadataIds, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        Generate generate = new Generate(new Message("")
        {
            @Override
            public void setMessage(String message)
            {
                if (!message.startsWith("Generating Java sources"))
                {
                    log.debug("  " + message);
                }
            }
        });

        if (modularMetadataIds)
        {
            generate.generateJavaCodeForSources(sourcesToCompile, group -> getSourceCodeGenerator(group, writeJavaSourcesToDisk, pathToWriteTo), log);
            if (this.addExternalAPI)
            {
                generate.generateExternalizableAPI(getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo), this.externalAPIPackage);
            }
            else
            {
                log.debug("    Skipping External API generation");
            }
        }
        else
        {
            JavaSourceCodeGenerator javaSourceCodeGenerator = getSourceCodeGenerator(null, writeJavaSourcesToDisk, pathToWriteTo);
            generate.generateJavaCodeForSources(sourcesToCompile, javaSourceCodeGenerator, generatePureTests, log);
            if (this.addExternalAPI)
            {
                generate.generateExternalizableAPI(javaSourceCodeGenerator, this.externalAPIPackage);
            }
            else
            {
                log.debug("    Skipping External API generation");
            }
        }
        return generate;
    }

    private JavaSourceCodeGenerator getSourceCodeGenerator(String compileGroup, boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        IdBuilder idBuilder = DistributedBinaryGraphSerializer.newIdBuilder(compileGroup, this.runtime.getProcessorSupport());
        JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(this.runtime.getProcessorSupport(), idBuilder, this.runtime.getCodeStorage(), writeJavaSourcesToDisk, pathToWriteTo, false, this.extensions, "UserCode", this.externalAPIPackage, false, this.useLegacyMetadataForExternalAPI);
        javaSourceCodeGenerator.collectClassesToSerialize();
        return javaSourceCodeGenerator;
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile()
    {
        return getSourcesToCompile((Predicate<String>) null);
    }

    private SortedMap<String, MutableList<Source>> getSourcesToCompile(String repo)
    {
        if (this.runtime.getCodeStorage().getRepository(repo) == null)
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
        if (!Iterate.allSatisfy(repos, r -> this.runtime.getCodeStorage().getRepository(r) != null))
        {
            MutableList<String> missingRepos = Iterate.reject(repos, c -> this.runtime.getCodeStorage().getRepository(c) != null, Lists.mutable.empty()).sortThis();
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
                s -> sourcesByRepo.getIfAbsentPut(CompositeCodeStorage.getSourceRepoName(s.getId()), Lists.mutable::empty).add(s) :
                s ->
                {
                    String repo = CompositeCodeStorage.getSourceRepoName(s.getId());
                    if (repoFilter.test(repo))
                    {
                        sourcesByRepo.getIfAbsentPut(repo, Lists.mutable::empty).add(s);
                    }
                });
        return sourcesByRepo;
    }

    public static JavaStandaloneLibraryGenerator newGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage, boolean useLegacyMetadataForExternalAPI, boolean generatePureTests, Log log)
    {
        return new JavaStandaloneLibraryGenerator(runtime, extensions, addExternalAPI, externalAPIPackage, useLegacyMetadataForExternalAPI, generatePureTests, log);
    }

    public static JavaStandaloneLibraryGenerator newGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage, boolean generatePureTests, Log log)
    {
        return newGenerator(runtime, extensions, addExternalAPI, externalAPIPackage, true, generatePureTests, log);
    }

    public static JavaStandaloneLibraryGenerator newGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage, Log log)
    {
        return newGenerator(runtime, extensions, addExternalAPI, externalAPIPackage, true, log);
    }

    public static PureJavaCompiler compileOnly(MapIterable<? extends String, ? extends Iterable<? extends StringJavaSource>> javaSources, ListIterable<? extends StringJavaSource> externalizableSources, boolean addExternalAPI, Log log) throws PureJavaCompileException
    {
        return compileOnly(javaSources.keyValuesView(), externalizableSources, addExternalAPI, log);
    }

    public static PureJavaCompiler compileOnly(Iterable<? extends Pair<? extends String, ? extends Iterable<? extends StringJavaSource>>> javaSources, ListIterable<? extends StringJavaSource> externalizableSources, boolean addExternalAPI, Log log) throws PureJavaCompileException
    {
        Compile compile = new Compile(new PureJavaCompiler(new Message("")), VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER);
        compile.compileJavaCodeForSources(javaSources, log);
        if (addExternalAPI)
        {
            compile.compileExternalizableAPI(externalizableSources);
        }
        return compile.getPureJavaCompiler();
    }
}
