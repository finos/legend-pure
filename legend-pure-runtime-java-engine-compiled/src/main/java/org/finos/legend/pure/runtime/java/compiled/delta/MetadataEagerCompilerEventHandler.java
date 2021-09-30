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
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEager;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEventObserver;

import java.util.SortedMap;

/**
 * Creates metadata eager by indexing the graph
 */
public class MetadataEagerCompilerEventHandler implements CompilerEventHandlerMetadataProvider
{
    private volatile boolean coreIndexed = false;
    private volatile boolean otherIndexed = false;

    private MetadataEager metadataEager;

    private final ModelRepository repository;
    private final MetadataEventObserver observer;
    private final Message message;
    private final ProcessorSupport processorSupport;


    public MetadataEagerCompilerEventHandler(ModelRepository repository, MetadataEventObserver observer, Message message, ProcessorSupport processorSupport)
    {
        this.repository = repository;
        this.observer = observer;
        this.message = message;
        this.processorSupport = processorSupport;
    }

    @Override
    public void finishedCompilingCore(RichIterable<? extends Source> compiledSources)
    {
        this.observer.startSerializingCoreCompiledGraph();
        this.buildMetadata();
        this.observer.endSerializingCoreCompiledGraph();
        this.coreIndexed = true;
    }

    @Override
    public void compiled(SortedMap<String, RichIterable<? extends Source>> compiledSourcesByRepo, RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        this.buildFullMetadata();
    }

    @Override
    public void buildFullMetadata()
    {
        this.observer.startSerializingSystemCompiledGraph();
        this.buildMetadata();
        this.observer.endSerializingSystemCompiledGraph(0, 0);
        this.coreIndexed = true;
        this.otherIndexed = true;
    }

    @Override
    public void buildMetadata(RichIterable<CoreInstance> newInstances)
    {
        this.metadataEager = MetadataBuilder.indexNew(this.metadataEager, newInstances, new M3ProcessorSupport(this.repository));
    }

    @Override
    public void startTransaction()
    {
        this.metadataEager.startTransaction();
    }

    @Override
    public void rollbackTransaction()
    {
        this.metadataEager.rollbackTransaction();
    }

    private void buildMetadata()
    {
        if (this.message != null)
        {
            this.message.setMessage("Instantiating Graph");
        }

        this.metadataEager = MetadataBuilder.indexAll(this.repository.getTopLevels(), this.processorSupport);

        if (this.message != null)
        {
            this.message.setMessage("Graph instantiated");
        }
    }

    @Override
    public void invalidate(RichIterable<? extends CoreInstance> consolidatedCoreInstances)
    {
        this.metadataEager = new MetadataEager();
    }

    @Override
    public void reset()
    {
        this.coreIndexed = false;
        this.otherIndexed = false;
        this.metadataEager = new MetadataEager();
    }

    @Override
    public boolean isInitialized()
    {
        return this.coreIndexed && this.otherIndexed;
    }

    @Override
    public MetadataEager getMetadata()
    {
        return this.metadataEager;
    }

    @Override
    public SetIterable<CoreInstance> getExcluded()
    {
        return this.repository.getExclusionSet();
    }


}
