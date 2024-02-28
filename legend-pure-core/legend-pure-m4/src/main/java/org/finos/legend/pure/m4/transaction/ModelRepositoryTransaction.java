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

package org.finos.legend.pure.m4.transaction;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.ConcurrentHashSet;
import org.finos.legend.pure.m4.transaction.framework.Transaction;
import org.finos.legend.pure.m4.transaction.framework.TransactionManager;

public class ModelRepositoryTransaction extends Transaction
{
    private final MutableSet<CoreInstance> newInstances = ConcurrentHashSet.newSet();
    private final ConcurrentMutableMap<CoreInstance, Object> modifiedInstanceStates = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, CoreInstance> topLevels = ConcurrentHashMap.newMap();

    private final ModelRepository modelRepository;
    private final TransactionObserver transactionObserver;

    private ModelRepositoryTransaction(TransactionManager<?> manager, boolean committable, ModelRepository modelRepository, TransactionObserver transactionObserver)
    {
        super(manager, committable);
        this.modelRepository = modelRepository;
        this.transactionObserver = transactionObserver;
        this.modelRepository.getTopLevels().groupByUniqueKey(CoreInstance::getName, this.topLevels);
    }

    public ModelRepository getModelRepository()
    {
        return this.modelRepository;
    }

    public void registerNew(CoreInstance coreInstance)
    {
        checkOpen();
        this.newInstances.add(coreInstance);
    }

    public void registerModified(CoreInstance coreInstance, Object state)
    {
        checkOpen();
        this.modifiedInstanceStates.putIfAbsent(coreInstance, state);
    }

    public boolean isRegistered(CoreInstance coreInstance)
    {
        return this.newInstances.contains(coreInstance) || this.modifiedInstanceStates.containsKey(coreInstance);
    }

    public Object getState(CoreInstance instance)
    {
        return this.modifiedInstanceStates.get(instance);
    }

    public RichIterable<CoreInstance> getNewInstancesInTransaction()
    {
        return this.newInstances.asUnmodifiable();
    }

    public CoreInstance getOrAddTopLevel(CoreInstance topLevel)
    {
        return this.topLevels.getIfAbsentPut(topLevel.getName(), topLevel);
    }

    public CoreInstance getTopLevel(String name)
    {
        return this.topLevels.get(name);
    }

    public RichIterable<CoreInstance> getTopLevels()
    {
        return this.topLevels.valuesView();
    }

    @Override
    protected void doCommit()
    {
        this.modelRepository.commitTransactionTopLevels(this);
        this.modifiedInstanceStates.forEachKey(instance -> instance.commit(this));
        if (this.transactionObserver != null)
        {
            this.transactionObserver.added(this.newInstances.asUnmodifiable());
            this.transactionObserver.modified(this.modifiedInstanceStates.keysView());
        }
    }

    @Override
    protected void doRollback()
    {
        this.modifiedInstanceStates.forEachKey(instance -> instance.rollback(this));
    }

    public static ModelRepositoryTransaction newTransaction(TransactionManager<? super ModelRepositoryTransaction> manager, boolean committable, ModelRepository modelRepository, TransactionObserver transactionObserver)
    {
        return new ModelRepositoryTransaction(manager, committable, modelRepository, transactionObserver);
    }
}
