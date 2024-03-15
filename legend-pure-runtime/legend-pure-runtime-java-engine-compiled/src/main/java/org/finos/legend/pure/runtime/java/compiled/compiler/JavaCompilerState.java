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

import org.finos.legend.pure.m3.serialization.runtime.Message;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public final class JavaCompilerState
{
    private final JavaCompiler javaCompiler;
    private final MemoryFileManager memoryFileManager;
    private final ThreadLocal<MemoryFileManager> memoryFileManagerLocal = new ThreadLocal<>();

    private final ClassLoader classLoader;

    public JavaCompilerState(MemoryFileManager memoryFileManager, ClassLoader classLoader)
    {
        this.memoryFileManager = memoryFileManager;
        this.classLoader = classLoader;
        this.javaCompiler = ToolProvider.getSystemJavaCompiler();
    }


    public void startTransaction()
    {
        this.memoryFileManagerLocal.set(new MemoryFileManager(this.javaCompiler, this.memoryFileManager, new Message("")));
    }

    public void commitTransaction()
    {
        //todo - we never commit at the moment, implement this later
    }

    public void rollbackTransaction()
    {
        this.memoryFileManagerLocal.remove();
    }

    public void compile(Iterable<StringJavaSource> javaSourcesToCompile) throws PureJavaCompileException
    {
        PureJavaCompiler.compile(this.javaCompiler, javaSourcesToCompile, this.getMemoryFileManager());
    }

    public MemoryFileManager getMemoryFileManager()
    {
        MemoryFileManager threadLocalFileManager = this.memoryFileManagerLocal.get();
        return threadLocalFileManager == null ? this.memoryFileManager : threadLocalFileManager;
    }

    public ClassLoader getClassLoader()
    {
        MemoryFileManager threadLocalFileManager = this.memoryFileManagerLocal.get();
        return threadLocalFileManager == null ? this.classLoader : new MemoryClassLoader(threadLocalFileManager, this.classLoader);
    }
}
