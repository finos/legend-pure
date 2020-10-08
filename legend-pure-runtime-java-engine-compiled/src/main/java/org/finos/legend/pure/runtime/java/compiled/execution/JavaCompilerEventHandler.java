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

package org.finos.legend.pure.runtime.java.compiled.execution;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.CompilerEventHandler;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.GenerateAndCompile;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.serialization.PreCompiledPureGraphCache;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;

import java.util.SortedMap;

public class JavaCompilerEventHandler implements CompilerEventHandler
{
    private volatile boolean javaGeneratedAndCompiled = false;

    private final ProcessorSupport processorSupport;
    private final CodeStorage codeStorage;

    private final Message message;

    private final boolean includePureStackTrace;

    //Lifecycle of the compiled graph - clear each time we recompile
    private FunctionCache sharedFunctionCache = new FunctionCache();
    private ClassCache classCache = new ClassCache();

    private final JavaCompilerEventObserver observer;

    private GenerateAndCompile generateAndCompile;

    private MutableList<CompiledExtension> extensions;

    private JavaCompilerEventHandler(ProcessorSupport processorSupport, CodeStorage codeStorage, Message message, boolean includePureStackTrace, JavaCompilerEventObserver observer, MutableList<CompiledExtension> extensions)
    {
        this.processorSupport = processorSupport;
        this.codeStorage = codeStorage;
        this.message = message;
        this.observer = observer;
        this.generateAndCompile = new GenerateAndCompile(this.message, this.observer);
        this.includePureStackTrace = includePureStackTrace;
        this.extensions = extensions;
    }

    public JavaCompilerEventHandler(PureRuntime pureRuntime, Message message, boolean includePureStackTrace, JavaCompilerEventObserver observer, MutableList<CompiledExtension> extensions)
    {
        this(pureRuntime.getProcessorSupport(), pureRuntime.getCodeStorage(), message, includePureStackTrace, observer, extensions);
        if (pureRuntime.getCache() instanceof PreCompiledPureGraphCache)
        {
            this.javaGeneratedAndCompiled = true;
        }
    }

    @Override
    public void finishedCompilingCore(RichIterable<? extends Source> sources)
    {

    }

    @Override
    public void compiled(SortedMap<String, RichIterable<? extends Source>> compiledSourcesByRepo, RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        this.generateAndCompileJavaCode(compiledSourcesByRepo);
    }

    @Override
    public void invalidate(RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        try
        {

            for (CoreInstance instance: consolidatedCoreInstances)
            {

                if (Type.class.isInstance(instance))
                {
                    this.classCache.remove((Type)instance);
                }
            }

            this.sharedFunctionCache = new FunctionCache();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void generateAndCompileJavaCode(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo)
    {
        this.generateAndCompile.generateAndCompileJavaCodeForSources(compiledSourcesByRepo, this.getJavaSourceCodeGenerator());
        this.javaGeneratedAndCompiled = true;

        this.sharedFunctionCache = new FunctionCache();
        this.classCache = new ClassCache();
    }

    @Override
    public void reset()
    {
        this.javaGeneratedAndCompiled = false;
        this.generateAndCompile = new GenerateAndCompile(this.message, this.observer);
        this.sharedFunctionCache = new FunctionCache();
        this.classCache = new ClassCache();
    }


    @Override
    public boolean isInitialized()
    {
        return this.javaGeneratedAndCompiled;
    }

    public PureJavaCompiler getJavaCompiler()
    {
        return this.generateAndCompile.getPureJavaCompiler();
    }



    public JavaCompilerState getJavaCompileState()
    {
        return new JavaCompilerState(this.getJavaCompiler().getFileManager(), this.getJavaCompiler().getClassLoader());
    }

    public FunctionCache getFunctionCache()
    {
        return this.sharedFunctionCache;
    }

    public ClassCache getClassCache()
    {
        return this.classCache;
    }


    private JavaSourceCodeGenerator getJavaSourceCodeGenerator()
    {
        return new JavaSourceCodeGenerator(this.processorSupport, this.codeStorage, false, null, this.includePureStackTrace, this.extensions, "Dyna", JavaPackageAndImportBuilder.externalizablePackage());
    }

}
