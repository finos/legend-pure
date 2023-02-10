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

package org.finos.legend.pure.runtime.java.compiled.serialization;

import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;

public class PreCompiledPureGraphCache extends ClassLoaderPureGraphCache
{
    private final Path preCompiledLib;

    public PreCompiledPureGraphCache(ClassLoader classLoader, ForkJoinPool forkJoinPool, Path preCompiledLib)
    {
        super(classLoader, forkJoinPool);
        this.preCompiledLib = preCompiledLib;
    }

    public PreCompiledPureGraphCache(ClassLoader classLoader, Path preCompiledLib)
    {
        this(classLoader, null, preCompiledLib);
    }

    public PreCompiledPureGraphCache(ForkJoinPool forkJoinPool, Path preCompiledLib)
    {
        this(null, forkJoinPool, preCompiledLib);
    }

    public PreCompiledPureGraphCache(Path preCompiledLib)
    {
        this(null, null, preCompiledLib);
    }

    public void prepareClassLoader(MemoryClassLoader classLoader)
    {
        try
        {
            classLoader.loadClassesFromJarFile(this.preCompiledLib);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
