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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.Compile;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;

import java.util.SortedMap;

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

    public PureJavaCompiler getPureJavaCompiler()
    {
        return this.pureJavaCompiler;
    }


    public void generateAndCompileJavaCodeForSources(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo, JavaSourceCodeGenerator sourceCodeGenerator)
    {

        try
        {
            if (this.message != null)
            {
                this.message.setMessage("Generating and compiling Java source code ...");
            }

            compiledSourcesByRepo = ReposWithBadDependencies.combineReposWithBadDependencies(compiledSourcesByRepo);

            int totalSourceCount = compiledSourcesByRepo.size();

            if (totalSourceCount > 0)
            {
                int count = 0;

                JavaSourceCodeGenerator javaSourceCodeGenerator = sourceCodeGenerator;

                for (String compileGroup : compiledSourcesByRepo.keySet())
                {
                    if (!PlatformCodeRepository.NAME.equals(compileGroup))
                    {
                        RichIterable<? extends Source> sources = compiledSourcesByRepo.get(compileGroup);
                        MutableList<StringJavaSource> javaSources = FastList.newList();
                        count = this.generate.generate(compileGroup, sources, javaSourceCodeGenerator, javaSources, totalSourceCount, count);
                        this.compile.compile(compileGroup, javaSources);
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    void generateAndCompileExternalizableAPI(JavaSourceCodeGenerator sourceCodeGenerator, String externalAPIPackage) throws PureJavaCompileException
    {
        this.generate.generateExternalizableAPI(sourceCodeGenerator, externalAPIPackage);
        this.compile.compileExternalizableAPI(this.generate.getExternalizableSources());
    }
}
