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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;
import org.finos.legend.pure.runtime.java.compiled.statelistener.VoidJavaCompilerEventObserver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.function.Function;

public class Generate
{
    private final Message message;
    private final JavaCompilerEventObserver observer;
    private final MutableOrderedMap<String, ImmutableList<StringJavaSource>> javaSourcesByGroup = OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private ImmutableList<StringJavaSource> externalizableSources = Lists.immutable.empty();

    public Generate(Message message, JavaCompilerEventObserver observer)
    {
        this.message = message;
        this.observer = (observer == null) ? VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER : observer;
    }

    public Generate(Message message)
    {
        this(message, null);
    }

    public Generate(JavaCompilerEventObserver observer)
    {
        this(null, observer);
    }

    public Generate()
    {
        this(null, null);
    }

    MutableList<StringJavaSource> generate(String compileGroup, RichIterable<? extends Source> sources, JavaSourceCodeGenerator javaSourceCodeGenerator, Counter sourceCounter, int totalSourceCount)
    {
        MutableList<StringJavaSource> javaSources = Lists.mutable.empty();
        this.observer.startGeneratingJavaFiles(compileGroup);

        ListIterable<StringJavaSource> helpers = javaSourceCodeGenerator.generatePureCoreHelperClasses(javaSourceCodeGenerator.getProcessorContext());
        javaSources.addAllIterable(helpers);
        sourceCounter.add(helpers.size());

        Collection<StringJavaSource> extras = javaSourceCodeGenerator.generateExtensionsCode(compileGroup);
        javaSources.addAll(extras);
        sourceCounter.add(extras.size());

        sources.forEach(source ->
        {
            javaSources.addAllIterable(javaSourceCodeGenerator.generateCode(source, null));
            sourceCounter.increment();
            if (this.message != null)
            {
                this.message.setMessage("Generating Java sources (" + sourceCounter.getCount() + "/" + totalSourceCount + ")");
            }
        });
        this.observer.endGeneratingJavaFiles(compileGroup, javaSources);

        return javaSources;
    }

    void generateJavaCodeForSources(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo, Function<? super String, ? extends JavaSourceCodeGenerator> sourceCodeGeneratorFn)
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
                    JavaSourceCodeGenerator sourceCodeGenerator = sourceCodeGeneratorFn.apply(compileGroup);
                    ListIterable<StringJavaSource> compileGroupJavaSources = generate(compileGroup, sources, sourceCodeGenerator, sourceCounter, totalSourceCount);
                    this.javaSourcesByGroup.put(compileGroup, compileGroupJavaSources.toImmutable());
                }
            });
        }
    }

    public void generateJavaCodeForSources(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo, JavaSourceCodeGenerator sourceCodeGenerator)
    {
        generateJavaCodeForSources(compiledSourcesByRepo, compileGroup -> sourceCodeGenerator);
    }

    public void generateExternalizableAPI(JavaSourceCodeGenerator sourceCodeGenerator, String pack)
    {
        this.externalizableSources = sourceCodeGenerator.generateExternalizableAPI(pack).toImmutable();
    }

    public OrderedMap<String, ImmutableList<StringJavaSource>> getJavaSourcesByGroup()
    {
        return this.javaSourcesByGroup.asUnmodifiable();
    }

    @Deprecated
    public ListIterable<Pair<String, ImmutableList<StringJavaSource>>> getJavaSources()
    {
        return this.javaSourcesByGroup.keyValuesView().toList();
    }

    public ListIterable<StringJavaSource> getExternalizableSources()
    {
        return this.externalizableSources;
    }
}
