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

package org.finos.legend.pure.m4.transaction.framework;

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.eclipse.collections.impl.factory.primitive.LongIntMaps;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import java.util.concurrent.atomic.AtomicReference;

public abstract class TransactionManager<T extends Transaction>
{
    private final AtomicReference<T> committableTransaction = new AtomicReference<>();
    private ConcurrentMutableMap<T, MutableLongIntMap> transactions = ConcurrentHashMap.newMap();
    private ConcurrentMutableMap<Long, T> transactionsByThreadId = ConcurrentHashMap.newMap();

    public void clear()
    {
        // Get existing transactions
        ConcurrentMutableMap<T, MutableLongIntMap> previousTransactions = this.transactions;

        // Clear state
        this.transactions = ConcurrentHashMap.newMap();
        this.transactionsByThreadId = ConcurrentHashMap.newMap();
        this.committableTransaction.set(null);

        // Invalidate any transactions from previous state
        previousTransactions.forEachKey(Transaction::invalidateIfOpen);
    }

    public T getThreadLocalTransaction()
    {
        return this.transactionsByThreadId.isEmpty() ? null : this.transactionsByThreadId.get(Thread.currentThread().getId());
    }

    public boolean isRegistered(Transaction transaction)
    {
        return this.transactions.containsKey(transaction);
    }

    public T newTransaction(boolean committable)
    {
        T transaction = createTransaction(committable);
        try
        {
            registerTransaction(transaction);
        }
        catch (RuntimeException e)
        {
            try
            {
                handleRegistrationFailure(transaction);
            }
            catch (Exception ignore)
            {
                // ignore this exception
            }
            throw e;
        }
        return transaction;
    }

    protected abstract T createTransaction(boolean committable);

    protected void handleRegistrationFailure(T transaction)
    {
        // Do nothing by default
    }

    @SuppressWarnings("unchecked")
    ThreadLocalTransactionContext setThreadLocalTransaction(Transaction transaction)
    {
        if (transaction == null)
        {
            throw new IllegalArgumentException("transaction may not be null");
        }
        if (transaction.getTransactionManager() != this)
        {
            throw new IllegalArgumentException("transaction belongs to a different manager");
        }
        T t = (T) transaction;
        MutableLongIntMap threads = this.transactions.get(t);
        if (threads == null)
        {
            throw new IllegalStateException("Unknown transaction: " + t);
        }
        synchronized (threads)
        {
            if (!this.transactions.containsKey(t))
            {
                throw new IllegalStateException("Unknown transaction: " + t);
            }
            Thread thread = Thread.currentThread();
            long threadId = thread.getId();
            T current = this.transactionsByThreadId.putIfAbsent(threadId, t);
            if ((current != null) && (current != t))
            {
                throw new IllegalStateException("A different transaction is already registered for thread \"" + thread.getName() + "\" (id " + threadId + ")");
            }
            threads.addToValue(threadId, 1);
            return new ManagerThreadLocalTransactionContext(threadId, t);
        }
    }

    private void removeThreadLocalTransaction(long threadId, T transaction)
    {
        T current = this.transactionsByThreadId.get(threadId);
        if (current == transaction)
        {
            MutableLongIntMap threads = this.transactions.get(transaction);
            if (threads == null)
            {
                // this shouldn't happen, but just in case ...
                this.transactionsByThreadId.remove(threadId, transaction);
                return;
            }
            synchronized (threads)
            {
                int count = threads.addToValue(threadId, -1);
                if (count <= 0)
                {
                    this.transactionsByThreadId.remove(threadId, transaction);
                    threads.remove(threadId);
                }
            }
        }
    }

    private void registerTransaction(T transaction)
    {
        if (transaction.getTransactionManager() != this)
        {
            throw new IllegalArgumentException("Transaction is associated with a different transaction manager");
        }
        if (transaction.isCommittable())
        {
            if (!this.committableTransaction.compareAndSet(null, transaction))
            {
                throw new IllegalStateException("Cannot register a new committable transaction");
            }
        }
        MutableLongIntMap previous = this.transactions.putIfAbsent(transaction, LongIntMaps.mutable.empty());
        if (previous != null)
        {
            throw new IllegalStateException("Transaction has already been registered");
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean deregisterTransaction(Transaction transaction)
    {
        MutableLongIntMap threads = this.transactions.remove(transaction);
        if (threads == null)
        {
            return false;
        }
        synchronized (threads)
        {
            transaction.invalidateIfOpen();
            threads.forEachKey(threadId -> this.transactionsByThreadId.remove(threadId, transaction));
            if (transaction.isCommittable())
            {
                this.committableTransaction.compareAndSet((T) transaction, null);
            }
            return true;
        }
    }

    private class ManagerThreadLocalTransactionContext extends ThreadLocalTransactionContext
    {
        private final long threadId;
        private final T transaction;

        private ManagerThreadLocalTransactionContext(long threadId, T transaction)
        {
            this.threadId = threadId;
            this.transaction = transaction;
        }

        @Override
        protected void doClose()
        {
            removeThreadLocalTransaction(this.threadId, this.transaction);
        }
    }
}
