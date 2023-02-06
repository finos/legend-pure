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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.Counter;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.Compile;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;

import java.util.SortedMap;
import java.util.function.Function;

/**
 * Utility that generates and compiles
 */
public class GenerateAndCompile
{
    private final Message message;
    private final PureJavaCompiler pureJavaCompiler;
    private final Generate generate;
    private final Compile compile;

    public GenerateAndCompile(Message message, JavaCompilerEventObserver observer)
    {
        this.message = message;
        this.pureJavaCompiler = new PureJavaCompiler(this.message);
        this.generate = new Generate(message, observer);
        this.compile = new Compile(this.pureJavaCompiler, observer);
    }

    public GenerateAndCompile(Message message)
    {
        this(message, null);
    }

    public GenerateAndCompile(JavaCompilerEventObserver observer)
    {
        this(null, observer);
    }

    public GenerateAndCompile()
    {
        this(null, null);
    }

    public PureJavaCompiler getPureJavaCompiler()
    {
        return this.pureJavaCompiler;
    }

    void generateAndCompileJavaCodeForSources(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo, Function<? super String, ? extends JavaSourceCodeGenerator> sourceCodeGeneratorFn)
    {
        if (this.message != null)
        {
            this.message.setMessage("Generating and compiling Java source code ...");
        }

        compiledSourcesByRepo = ReposWithBadDependencies.combineReposWithBadDependencies(compiledSourcesByRepo);
        Counter sourceCounter = new Counter();
        compiledSourcesByRepo.forEach((compileGroup, sources) -> sourceCounter.add(sources.size()));
        int totalSourceCount = sourceCounter.getCount();
        if (totalSourceCount > 0)
        {
            sourceCounter.reset();
            compiledSourcesByRepo.forEach((compileGroup, sources) ->
            {
                if (sources.notEmpty())
                {
                    ListIterable<StringJavaSource> compileGroupJavaSources = this.generate.generate(compileGroup, sources, sourceCodeGeneratorFn.apply(compileGroup), sourceCounter, totalSourceCount);
                    try
                    {
                        this.compile.compile(compileGroup, compileGroupJavaSources);
                    }
                    catch (PureJavaCompileException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    public void generateAndCompileJavaCodeForSources(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo, JavaSourceCodeGenerator sourceCodeGenerator)
    {
        generateAndCompileJavaCodeForSources(compiledSourcesByRepo, compileGroup -> sourceCodeGenerator);
    }

    void generateAndCompileExternalizableAPI(JavaSourceCodeGenerator sourceCodeGenerator, String externalAPIPackage) throws PureJavaCompileException
    {
        this.generate.generateExternalizableAPI(sourceCodeGenerator, externalAPIPackage);
        this.compile.compileExternalizableAPI(this.generate.getExternalizableSources());
    }
}
