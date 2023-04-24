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

import io.github.classgraph.ClassGraph;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.jar.JarOutputStream;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public class PureJavaCompiler
{
    private static final Map<ClassLoader, String> CLASSPATH_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final JavaCompiler compiler;
    private final MemoryFileManager coreManager;
    private final MemoryClassLoader coreClassLoader;
    private final MemoryFileManager dynamicManager;
    private MemoryClassLoader globalClassLoader;

    public PureJavaCompiler(Message message)
    {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.coreManager = new MemoryFileManager(this.compiler, message);
        this.dynamicManager = new MemoryFileManager(this.compiler, this.coreManager, message);
        this.coreClassLoader = new MemoryClassLoader(this.coreManager, Thread.currentThread().getContextClassLoader());
        this.globalClassLoader = new MemoryClassLoader(this.dynamicManager, this.coreClassLoader);
    }

    public MemoryClassLoader compile(Iterable<? extends StringJavaSource> javaSources) throws PureJavaCompileException
    {
        compile(this.compiler, javaSources, this.dynamicManager);
        this.globalClassLoader = new MemoryClassLoader(this.dynamicManager, this.coreClassLoader);
        return this.globalClassLoader;
    }

    public MemoryClassLoader getCoreClassLoader()
    {
        return this.coreClassLoader;
    }

    public MemoryClassLoader getClassLoader()
    {
        return this.globalClassLoader;
    }

    public MemoryFileManager getFileManager()
    {
        return this.dynamicManager;
    }

    public MemoryFileManager getCoreFileManager()
    {
        return this.coreManager;
    }

    public void writeClassJavaSourcesToJar(JarOutputStream outputStream) throws IOException
    {
        this.coreManager.writeClassJavaSourcesToJar(outputStream);
        this.dynamicManager.writeClassJavaSourcesToJar(outputStream);
    }

    public void writeClassJavaSources(Path directory) throws IOException
    {
        this.coreManager.writeClassJavaSources(directory);
        this.dynamicManager.writeClassJavaSources(directory);
    }

    public static void compile(JavaCompiler compiler, Iterable<? extends StringJavaSource> javaSources, JavaFileManager fileManager) throws PureJavaCompileException
    {
        compile(compiler, javaSources, fileManager, getClassPath(), null);
    }

    public static void compile(Iterable<? extends JavaFileObject> javaSources, JavaFileManager fileManager, String classPath, Integer sourceVersion) throws PureJavaCompileException
    {
        compile(ToolProvider.getSystemJavaCompiler(), javaSources, fileManager, classPath, sourceVersion);
    }

    public static void compile(JavaCompiler compiler, Iterable<? extends JavaFileObject> javaSources, JavaFileManager fileManager, String classPath, Integer sourceVersion) throws PureJavaCompileException
    {
        MutableList<String> options = buildCompileOptions(classPath, sourceVersion);
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector, options, null, javaSources);
        if (!task.call())
        {
            throw new PureJavaCompileException(diagnosticCollector);
        }
    }

    private static MutableList<String> buildCompileOptions(String classPath, Integer sourceVersion)
    {
        MutableList<String> options = Lists.mutable.empty();

        // classpath
        if ((classPath != null) && !classPath.isEmpty())
        {
            options.with("-classpath").with(classPath);
        }

        // source/target/release version
        if ((sourceVersion != null) && (sourceVersion < 7))
        {
            throw new IllegalArgumentException("Source version must be at least 7, got: " + sourceVersion);
        }
        String versionString = (sourceVersion == null) ? "7" : sourceVersion.toString();
        // When JDK 9+ is allowed, use this code instead: Runtime.version().version().get(0) <= 8
        if ("7".equals(versionString) || (SourceVersion.latest().ordinal() <= 8))
        {
            // if source version is less than 8 or if this JVM is version 8 or older, we use -source and -target options
            options.with("-source").with(versionString)
                    .with("-target").with(versionString);
        }
        else
        {
            // if this JVM is version 9 or newer, we use the --release option
            options.with("--release").with(versionString);
        }
        options.add("-XDuseUnsharedTable=true");
        return options;
    }

    private static String getClassPath()
    {
        return CLASSPATH_CACHE.computeIfAbsent(Thread.currentThread().getContextClassLoader(), cl -> new ClassGraph().getClasspath());
    }
}
