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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;

import java.util.SortedMap;

public class Generate
{
    private final Message message;
    private final JavaCompilerEventObserver observer;
    private final MutableList<Pair<String, ImmutableList<StringJavaSource>>> javaSources;
    private ListIterable<StringJavaSource> externalizableSources = Lists.mutable.empty();

    public Generate(Message message, JavaCompilerEventObserver observer)
    {
        this.message = message;
        this.observer = observer;
        this.javaSources = FastList.newList();
    }

    public int generate(String compileGroup, RichIterable<? extends Source> sources, JavaSourceCodeGenerator javaSourceCodeGenerator, MutableList<StringJavaSource> javaSources, int totalSourceCount, int start)
    {
        int count = start;
        this.observer.startGeneratingJavaFiles(compileGroup);
        for (Source src : sources)
        {
            javaSources.addAllIterable(javaSourceCodeGenerator.generateCode(src));
            count++;
            if (this.message != null)
            {
                this.message.setMessage("Generating Java sources (" + count + "/" + totalSourceCount + ")");
            }
        }
        this.observer.endGeneratingJavaFiles(compileGroup, javaSources);
        return count;
    }

    public void generateJavaCodeForSources(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo, JavaSourceCodeGenerator sourceCodeGenerator)
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

                for (String compileGroup : compiledSourcesByRepo.keySet())
                {
                    if (!PlatformCodeRepository.NAME.equals(compileGroup))
                    {
                        RichIterable<? extends Source> sources = compiledSourcesByRepo.get(compileGroup);
                        MutableList<StringJavaSource> javaSources = FastList.newList();

                        count = this.generate(compileGroup, sources, sourceCodeGenerator, javaSources, totalSourceCount, count);
                        this.javaSources.add(Tuples.pair(compileGroup, javaSources.toImmutable()));
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

    public void generateExternalizableAPI(JavaSourceCodeGenerator sourceCodeGenerator, String pack)
    {
        this.externalizableSources = sourceCodeGenerator.generateExternalizableAPI(pack);
    }

    public ListIterable<Pair<String, ImmutableList<StringJavaSource>>> getJavaSources()
    {
        return this.javaSources.toImmutable();
    }

    public ListIterable<StringJavaSource> getExternalizableSources()
    {
        return this.externalizableSources.toImmutable();
    }
}
