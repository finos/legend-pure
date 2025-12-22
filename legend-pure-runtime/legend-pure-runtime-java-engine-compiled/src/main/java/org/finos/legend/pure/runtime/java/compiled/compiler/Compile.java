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

package org.finos.legend.pure.runtime.java.compiled.compiler;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.Log;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;
import org.finos.legend.pure.runtime.java.compiled.statelistener.VoidJavaCompilerEventObserver;

public class Compile
{
    private final JavaCompilerEventObserver observer;
    private final PureJavaCompiler pureJavaCompiler;

    public Compile(PureJavaCompiler pureJavaCompiler, JavaCompilerEventObserver observer)
    {
        this.observer = (observer == null) ? VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER : observer;
        this.pureJavaCompiler = pureJavaCompiler;
    }

    public Compile(PureJavaCompiler pureJavaCompiler)
    {
        this(pureJavaCompiler, null);
    }

    public void compileJavaCodeForSources(Iterable<? extends Pair<? extends String, ? extends Iterable<? extends StringJavaSource>>> javaSourcesByCompileGroup, Log log) throws PureJavaCompileException
    {
        for (Pair<? extends String, ? extends Iterable<? extends StringJavaSource>> javaSources : javaSourcesByCompileGroup)
        {
            log.debug("    Compiling group " + javaSources.getOne());
            compile(javaSources.getOne(), javaSources.getTwo(), log);
        }
    }

    public void compile(String compileGroup, Iterable<? extends StringJavaSource> javaSources, Log log) throws PureJavaCompileException
    {
        this.observer.startCompilingJavaFiles(compileGroup);
        MutableMap<String, StringJavaSource> javaSourcesByName = Maps.mutable.empty();
        javaSources.forEach(javaSource ->
        {
            StringJavaSource oldSource = javaSourcesByName.put(javaSource.getName(), javaSource);
            if ((oldSource != null) && !oldSource.getCode().equals(javaSource.getCode()))
            {
                throw new RuntimeException("Java source " + javaSource.getName() + " defined more than once with different code.\n\nSOURCE 1:\n" + oldSource.getCode() + "\n\n\n==================\nSOURCE 2:\n" + javaSource.getCode());
            }
        });
        long start = System.currentTimeMillis();
        log.debug("      compiling " + javaSourcesByName.valuesView().size() + " sources");
        if (javaSourcesByName.notEmpty())
        {
            this.pureJavaCompiler.compile(javaSourcesByName.valuesView());
        }
        log.debug("      finished in " + ((float) (System.currentTimeMillis() - start) / 1000) + "s");
        this.observer.endCompilingJavaFiles(compileGroup);
    }

    public void compileExternalizableAPI(ListIterable<? extends StringJavaSource> externalizableSources) throws PureJavaCompileException
    {
        this.pureJavaCompiler.compile(externalizableSources);
    }

    public PureJavaCompiler getPureJavaCompiler()
    {
        return this.pureJavaCompiler;
    }
}
