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

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;

public class Compile
{
    private final JavaCompilerEventObserver observer;
    private final PureJavaCompiler pureJavaCompiler;

    public Compile(PureJavaCompiler pureJavaCompiler, JavaCompilerEventObserver observer)
    {
        this.observer = observer;
        this.pureJavaCompiler = pureJavaCompiler;
    }

    public void compileJavaCodeForSources(ListIterable<Pair<String, ImmutableList<StringJavaSource>>> javaSourcesByCompileGroup) throws PureJavaCompileException
    {
        for(Pair<String, ImmutableList<StringJavaSource>> javaSources : javaSourcesByCompileGroup)
        {
            this.compile(javaSources.getOne(), javaSources.getTwo());
        }
    }

    public void compile(String compileGroup, ListIterable<StringJavaSource> javaSources) throws PureJavaCompileException
    {
        if (javaSources.notEmpty())
        {
            this.observer.startCompilingJavaFiles(compileGroup);
            MutableMap<String, StringJavaSource> javaSourcesByName = UnifiedMap.newMap(javaSources.size());
            for (StringJavaSource javaSource : javaSources)
            {
                StringJavaSource oldSource = javaSourcesByName.put(javaSource.getName(), javaSource);
                if ((oldSource != null) && !oldSource.getCode().equals(javaSource.getCode()))
                {
                    throw new RuntimeException("Java source " + javaSource.getName() + " defined more than once with different code.\n\nSOURCE 1:\n" + oldSource.getCode() + "\n\n\n==================\nSOURCE 2:\n" + javaSource.getCode());
                }
            }
            this.pureJavaCompiler.compile(javaSourcesByName.valuesView());
            this.observer.endCompilingJavaFiles(compileGroup);
        }
    }

    public void compileExternalizableAPI(ListIterable<StringJavaSource> externalizableSources) throws PureJavaCompileException
    {
        this.pureJavaCompiler.compile(externalizableSources);
    }

    public PureJavaCompiler getPureJavaCompiler()
    {
        return this.pureJavaCompiler;
    }
}
