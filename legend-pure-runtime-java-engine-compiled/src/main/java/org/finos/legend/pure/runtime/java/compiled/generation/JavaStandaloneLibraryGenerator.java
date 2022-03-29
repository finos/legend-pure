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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.RepositoryComparator;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.Compile;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.statelistener.VoidJavaCompilerEventObserver;

import java.io.IOException;
import java.nio.file.Path;
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

    public PureJavaCompiler compile(boolean writeJavaSourcesToDisk, Path pathToWriteTo) throws PureJavaCompileException
    {
        GenerateAndCompile generateAndCompile = compile(this.runtime, writeJavaSourcesToDisk, pathToWriteTo, this.extensions, this.addExternalAPI, this.externalAPIPackage);
        return generateAndCompile.getPureJavaCompiler();
    }

    public Generate generateOnly(boolean writeJavaSourcesToDisk, Path pathToWriteTo)
    {
        return generateOnly(this.runtime, writeJavaSourcesToDisk, pathToWriteTo, this.extensions, this.addExternalAPI, this.externalAPIPackage);
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

    public void serializeAndWriteDistributedMetadata(String metadataName, Path directory) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(metadataName, this.runtime).serializeToDirectory(directory);
    }

    public void serializeAndWriteDistributedMetadata(JarOutputStream jarOutputStream) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(this.runtime).serializeToJar(jarOutputStream);
    }

    public void serializeAndWriteDistributedMetadata(String metadataName, JarOutputStream jarOutputStream) throws IOException
    {
        DistributedBinaryGraphSerializer.newSerializer(metadataName, this.runtime).serializeToJar(jarOutputStream);
    }

    public void compileSerializeAndWriteClassesAndMetadata(JarOutputStream jarOutputStream) throws IOException, PureJavaCompileException
    {
        serializeAndWriteDistributedMetadata(jarOutputStream);
        compileAndWriteClasses(jarOutputStream);
    }

    public static JavaStandaloneLibraryGenerator newGenerator(PureRuntime runtime, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage)
    {
        return new JavaStandaloneLibraryGenerator(runtime, extensions, addExternalAPI, externalAPIPackage);
    }

    private static GenerateAndCompile compile(PureRuntime runtime, boolean writeJavaSourcesToDisk, Path pathToWriteTo, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage) throws PureJavaCompileException
    {
        // Group sources by repo and separate platform sources
        MutableListMultimap<String, Source> sourcesByRepo = runtime.getSourceRegistry().getSources().groupBy((Source source) -> PureCodeStorage.getSourceRepoName(source.getId()), Multimaps.mutable.list.empty());

        // Generate and compile Java code
        GenerateAndCompile generateAndCompile = new GenerateAndCompile(new Message(""), VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER);
        JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(runtime.getProcessorSupport(), runtime.getCodeStorage(), writeJavaSourcesToDisk, pathToWriteTo, false, extensions, "UserCode", externalAPIPackage);
        RichIterable<CodeRepository> repositories = runtime.getCodeStorage().getAllRepositories();
        javaSourceCodeGenerator.collectClassesToSerialize();
        generateAndCompile.generateAndCompileJavaCodeForSources(TreeSortedMap.newMap(new RepositoryComparator(repositories), sourcesByRepo.toMap()), javaSourceCodeGenerator);
        if (addExternalAPI)
        {
            generateAndCompile.generateAndCompileExternalizableAPI(javaSourceCodeGenerator, externalAPIPackage);
        }
        return generateAndCompile;
    }

    private static Generate generateOnly(PureRuntime runtime, boolean writeJavaSourcesToDisk, Path pathToWriteTo, Iterable<? extends CompiledExtension> extensions, boolean addExternalAPI, String externalAPIPackage)
    {
        // Group sources by repo and separate platform sources
        MutableListMultimap<String, Source> sourcesByRepo = runtime.getSourceRegistry().getSources().groupBy((Source source) -> PureCodeStorage.getSourceRepoName(source.getId()), Multimaps.mutable.list.empty());

        // Generate Java code
        Generate generate = new Generate(new Message(""), VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER);
        JavaSourceCodeGenerator javaSourceCodeGenerator = new JavaSourceCodeGenerator(runtime.getProcessorSupport(), runtime.getCodeStorage(), writeJavaSourcesToDisk, pathToWriteTo, false, extensions, "UserCode", externalAPIPackage);
        RichIterable<CodeRepository> repositories = runtime.getCodeStorage().getAllRepositories();
        javaSourceCodeGenerator.collectClassesToSerialize();
        generate.generateJavaCodeForSources(TreeSortedMap.newMap(new RepositoryComparator(repositories), sourcesByRepo.toMap()), javaSourceCodeGenerator);
        if (addExternalAPI)
        {
            generate.generateExternalizableAPI(javaSourceCodeGenerator, externalAPIPackage);
        }
        return generate;
    }

    public static PureJavaCompiler compileOnly(ListIterable<Pair<String, ImmutableList<StringJavaSource>>> javaSources, ListIterable<StringJavaSource> externalizableSources, boolean addExternalAPI) throws PureJavaCompileException
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
