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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryFileManager;
import org.finos.legend.pure.runtime.java.compiled.delta.MetadataProvider;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataHolder;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListener;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionListeners;
import org.finos.legend.pure.runtime.java.shared.listeners.IdentifableExecutionEndListner;

public class CompiledExecutionSupport implements ExecutionSupport
{
    private final JavaCompilerState javaCompilerState;

    private final SourceRegistry sourceRegistry;
    private final CodeStorage codeStorage;

    private final IncrementalCompiler incrementalCompiler;
    private final ExecutionActivityListener executionActivityListener;
    private final ConsoleCompiled console;

    private final Context context = new Context();

    private final ExecutionListeners executionListeners = new ExecutionListeners();

    private final CompiledProcessorSupport processorSupport;

    private final FunctionCache functionCache;
    private final ClassCache classCache;

    private final MetadataAccessor metadataAccessor;
    private final MetadataProvider metadataProvider;

    private final RuntimeOptions options;

    private final MutableSet<String> extraSupportedTypes;

    private final MutableList<CompiledExtension> compiledExtensions;

    public CompiledExecutionSupport(JavaCompilerState javaCompilerState, CompiledProcessorSupport processorSupport, SourceRegistry sourceRegistry, CodeStorage codeStorage, IncrementalCompiler incrementalCompiler, ExecutionActivityListener executionActivityListener, ConsoleCompiled console, FunctionCache functionCache, ClassCache classCache, MetadataProvider metadataProvider, MutableSet<String> extraSupportedTypes, MutableList<CompiledExtension> compiledExtensions)
    {
        this(javaCompilerState, processorSupport, sourceRegistry, codeStorage, incrementalCompiler, executionActivityListener, console, functionCache, classCache, metadataProvider, extraSupportedTypes, compiledExtensions, null);
    }

    public CompiledExecutionSupport(JavaCompilerState javaCompilerState, CompiledProcessorSupport processorSupport, SourceRegistry sourceRegistry, CodeStorage codeStorage, IncrementalCompiler incrementalCompiler, ExecutionActivityListener executionActivityListener, ConsoleCompiled console, FunctionCache functionCache, ClassCache classCache, MetadataProvider metadataProvider, MutableSet<String> extraSupportedTypes, MutableList<CompiledExtension> compiledExtensions, RuntimeOptions options)
    {
        this.javaCompilerState = javaCompilerState;
        this.sourceRegistry = sourceRegistry;
        this.codeStorage = codeStorage;
        this.incrementalCompiler = incrementalCompiler;
        this.functionCache = functionCache;
        this.classCache = (classCache == null) ? new ClassCache(javaCompilerState.getClassLoader()) : ClassCache.reconcileWithClassLoader(classCache, javaCompilerState.getClassLoader());
        this.metadataProvider = metadataProvider;
        this.executionActivityListener = (executionActivityListener == null) ? VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER : executionActivityListener;
        this.console = console;
        this.processorSupport = processorSupport;
        this.metadataAccessor = new MetadataHolder(processorSupport.getMetadata());
        this.extraSupportedTypes = extraSupportedTypes;
        this.options = (options == null) ? name -> false : options;
        this.compiledExtensions = compiledExtensions;
    }

    public SetIterable<String> getExtraSupportedTypes()
    {
        return this.extraSupportedTypes;
    }

    public ClassLoader getClassLoader()
    {
        return this.javaCompilerState.getClassLoader();
    }

    public MemoryFileManager getMemoryFileManager()
    {
        return this.javaCompilerState.getMemoryFileManager();
    }

    public SourceRegistry getSourceRegistry()
    {
        return this.sourceRegistry;
    }

    public CodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public IncrementalCompiler getIncrementalCompiler()
    {
        return this.incrementalCompiler;
    }

    public ExecutionActivityListener getExecutionActivityListener()
    {
        return this.executionActivityListener;
    }

    public ConsoleCompiled getConsole()
    {
        return this.console;
    }

    public Context getContext()
    {
        return this.context;
    }

    public CompiledProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }

    public RuntimeOptions getRuntimeOptions()
    {
        return this.options;
    }

    @Deprecated
    public MapIterable getMetadata(String classifier)
    {
        return getMetadata().getMetadata(classifier);
    }

    @Deprecated
    public CoreInstance getMetadata(String classifier, String id)
    {
        return getMetadata().getMetadata(classifier, id);
    }

    @Deprecated
    public Metadata getMetadata()
    {
        return this.processorSupport.getMetadata();
    }

    public MetadataAccessor getMetadataAccessor()
    {
        return this.metadataAccessor;
    }

    public FunctionCache getFunctionCache()
    {
        return this.functionCache;
    }

    public ClassCache getClassCache()
    {
        return this.classCache;
    }

    public void registerExecutionEndListener(ExecutionEndListener executionEndListener)
    {
        this.executionListeners.registerExecutionEndListener(executionEndListener);
    }

    public void registerIdentifableExecutionEndListener(IdentifableExecutionEndListner identifableExecutionEndListener)
    {
        this.executionListeners.registerIdentifableExecutionEndListener(identifableExecutionEndListener);
    }

    public void unRegisterIdentifableExecutionEndListener(String listnerId)
    {
        this.executionListeners.unRegisterIdentifableExecutionEndListener(listnerId);
    }

    public void executionEnd(final Exception exception)
    {
        this.executionListeners.executionEnd(exception);
    }

    public ExecutionListeners getExecutionListeners()
    {
        return this.executionListeners;
    }

    public MetadataProvider getMetadataProvider()
    {
        return this.metadataProvider;
    }

    public MutableList<CompiledExtension> getCompiledExtensions()
    {
        return compiledExtensions;
    }
}