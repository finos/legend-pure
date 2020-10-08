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

package org.finos.legend.pure.runtime.java.compiled.statelistener;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEventObserver;

public class VoidJavaCompilerEventObserver implements JavaCompilerEventObserver, MetadataEventObserver
{
    public static final VoidJavaCompilerEventObserver VOID_JAVA_COMPILER_EVENT_OBSERVER = new VoidJavaCompilerEventObserver();

    private VoidJavaCompilerEventObserver()
    {
        // Singleton
    }

    @Override
    public void startSerializingCoreCompiledGraph()
    {

    }

    @Override
    public void endSerializingCoreCompiledGraph()
    {

    }

    @Override
    public void startGeneratingJavaFiles(String compileGroup)
    {

    }

    @Override
    public void endGeneratingJavaFiles(String compileGroup, RichIterable<StringJavaSource> sources)
    {

    }

    @Override
    public void startCompilingJavaFiles(String compileGroup)
    {

    }

    @Override
    public void endCompilingJavaFiles(String compileGroup)
    {

    }

    @Override
    public void startSerializingSystemCompiledGraph()
    {

    }

    @Override
    public void endSerializingSystemCompiledGraph(int objectCount, int packageLinkCount)
    {

    }
}
