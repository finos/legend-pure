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

package org.finos.legend.pure.runtime.java.compiled.delta;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.JavaCompilerEventHandler;
import org.finos.legend.pure.runtime.java.compiled.metadata.CompiledCoreInstanceBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEager;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEventObserver;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

import java.util.SortedMap;

/**
 * Meta data compiler event handler
 */
@Deprecated
class SerializingMetadataCompilerEventHandler implements CompilerEventHandlerMetadataProvider
{
    private volatile boolean coreSerialized = false;
    private volatile boolean otherSerialized = false;

    private MetadataEager metadataEager = new MetadataEager();
    private CompiledCoreInstanceBuilder instanceBuilder = new CompiledCoreInstanceBuilder();
    private SetIterable<CoreInstance> excluded = Sets.immutable.empty();

    private final ModelRepository repository;
    private final MetadataEventObserver observer;
    private final Message message;
    private final ProcessorSupport processorSupport;
    private final JavaCompilerEventHandler javaCompilerEventHandler;

    SerializingMetadataCompilerEventHandler(ModelRepository repository, MetadataEventObserver observer, Message message, ProcessorSupport processorSupport, JavaCompilerEventHandler javaCompilerEventHandler)
    {
        this.repository = repository;
        this.observer = observer;
        this.message = message;
        this.processorSupport = processorSupport;
        this.javaCompilerEventHandler = javaCompilerEventHandler;
    }

    @Override
    public void finishedCompilingCore(RichIterable<? extends Source> compiledSources)
    {
        serializeCoreGraph();
    }

    @Override
    public void compiled(SortedMap<String, RichIterable<? extends Source>> compiledSourcesByRepo, RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        if (this.coreSerialized)
        {
            serializeGraph(consolidatedCoreInstances);
        }
    }

    @Override
    public void invalidate(RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        try
        {
            for (CoreInstance instance : consolidatedCoreInstances)
            {
                instance.removeCompileState(GraphSerializer.SERIALIZED);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public CompiledCoreInstanceBuilder getInstanceBuilder()
    {
        return this.instanceBuilder;
    }

    @Override
    public Metadata getMetadata()
    {
        return this.metadataEager;
    }

    private void serializeCoreGraph()
    {
        this.observer.startSerializingCoreCompiledGraph();
        Serialized serialized = GraphSerializer.serializeAll(this.repository.getTopLevels(), this.processorSupport, true);

        if (this.message != null)
        {
            this.message.setMessage("Instantiating Graph");
        }

        MutableSet<CoreInstance> newExcluded = Sets.mutable.empty();
        GraphSerializer.buildGraph(serialized, this.metadataEager, this.instanceBuilder, this.repository.getExclusionSet(), newExcluded, this.javaCompilerEventHandler.getJavaCompiler().getCoreClassLoader());
        this.excluded = newExcluded;
        if (this.message != null)
        {
            this.message.setMessage("Graph instantiated");
        }
        this.coreSerialized = true;
        this.observer.endSerializingCoreCompiledGraph();

    }

    private void serializeGraph(RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        this.observer.startSerializingSystemCompiledGraph();
        if (this.message != null)
        {
            if (consolidatedCoreInstances == null)
            {
                this.message.setMessage("Serializing and instantiating Graph");
            }
            else
            {
                this.message.setMessage("Serializing and instantiating Graph (" + consolidatedCoreInstances.size() + " nodes)");
            }
        }
        MutableSet<CoreInstance> newExcluded = UnifiedSet.newSet(this.excluded.size());
        this.metadataEager.clear();
        int objectCount = GraphSerializer.serializeAllToMetadata(this.repository.getTopLevels(), this.metadataEager, this.instanceBuilder,
                this.repository.getExclusionSet(), newExcluded, this.javaCompilerEventHandler.getJavaCompiler().getClassLoader(), new M3ProcessorSupport(this.repository));
        this.excluded = newExcluded;

        this.coreSerialized = true;
        this.otherSerialized = true;

        this.observer.endSerializingSystemCompiledGraph(objectCount, 0);

        if (this.message != null)
        {
            this.message.setMessage("Graph instantiated");
        }
    }

    @Override
    public boolean isInitialized()
    {
        return this.coreSerialized && this.otherSerialized;
    }

    @Override
    public void reset()
    {
        this.coreSerialized = false;
        this.otherSerialized = false;
        this.metadataEager = new MetadataEager();
        this.instanceBuilder = new CompiledCoreInstanceBuilder();
    }


    @Override
    public SetIterable<CoreInstance> getExcluded()
    {
        return this.excluded;
    }

    @Override
    public void buildFullMetadata()
    {
        serializeGraph(null);
    }

    @Override
    public void buildMetadata(RichIterable<CoreInstance> newInstances)
    {
        Serialized newGraphEntries = GraphSerializer.serializeNew(newInstances, new M3ProcessorSupport(this.repository));
        GraphSerializer.buildGraph(newGraphEntries, this.metadataEager, this.instanceBuilder, this.javaCompilerEventHandler.getJavaCompileState().getClassLoader());
    }

    @Override
    public void startTransaction()
    {
        this.metadataEager.startTransaction();
        this.instanceBuilder.startTransaction();
    }

    @Override
    public void rollbackTransaction()
    {
        this.metadataEager.rollbackTransaction();
        this.instanceBuilder.rollbackTransaction();
    }
}
