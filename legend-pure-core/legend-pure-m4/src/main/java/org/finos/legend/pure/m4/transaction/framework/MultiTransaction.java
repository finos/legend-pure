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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

import java.util.function.Consumer;

public abstract class MultiTransaction extends Transaction
{
    private final ListIterable<Transaction> transactions;

    protected MultiTransaction(TransactionManager<?> manager, boolean committable, ListIterable<Transaction> transactions)
    {
        super(manager, committable);
        this.transactions = transactions;
    }

    protected MultiTransaction(TransactionManager<?> manager, boolean committable, Transaction... transactions)
    {
        this(manager, committable, ArrayAdapter.adapt(transactions));
    }

    @Override
    protected void doCommit()
    {
        forEachWithExceptionCollection(this.transactions, Transaction::commit, "committing");
    }

    @Override
    protected void doRollback()
    {
        forEachWithExceptionCollection(this.transactions, Transaction::rollback, "rolling back");
    }

    @Override
    public ThreadLocalTransactionContext openInCurrentThread()
    {
        MutableList<ThreadLocalTransactionContext> contexts = Lists.mutable.ofInitialCapacity(this.transactions.size() + 1);
        try
        {
            contexts.add(super.openInCurrentThread());
            this.transactions.collect(Transaction::openInCurrentThread, contexts);
        }
        catch (Exception e)
        {
            contexts.forEach(context ->
            {
                try
                {
                    context.close();
                }
                catch (Exception ignore)
                {
                    // Ignore
                }
            });
            throw e;
        }
        return new ThreadLocalMultiTransactionContext(contexts);
    }

    private static class ThreadLocalMultiTransactionContext extends ThreadLocalTransactionContext
    {
        private final ListIterable<ThreadLocalTransactionContext> contexts;

        protected ThreadLocalMultiTransactionContext(ListIterable<ThreadLocalTransactionContext> contexts)
        {
            this.contexts = contexts;
        }

        @Override
        protected void doClose()
        {
            forEachWithExceptionCollection(this.contexts, ThreadLocalTransactionContext::close, "closing");
        }
    }

    private static <T> void forEachWithExceptionCollection(ListIterable<T> list, Consumer<? super T> action, String descriptionForExceptions)
    {
        MutableList<Exception> exceptions = Lists.mutable.empty();
        list.forEach(t ->
        {
            try
            {
                action.accept(t);
            }
            catch (Exception e)
            {
                exceptions.add(e);
            }
        });
        if (exceptions.notEmpty())
        {
            StringBuilder message = new StringBuilder();
            if (exceptions.size() == 1)
            {
                message.append("An exception");
            }
            else
            {
                message.append(exceptions.size()).append(" exceptions");
            }
            message.append(" occurred while ").append(descriptionForExceptions).append(':');
            exceptions.forEach(e -> message.append("\n\t").append(e.getMessage()));
            Exception mainException = exceptions.get(0);
            if (exceptions.size() > 1)
            {
                exceptions.subList(1, exceptions.size()).forEach(e ->
                {
                    try
                    {
                        mainException.addSuppressed(e);
                    }
                    catch (Exception ignore)
                    {
                        // ignore exceptions while trying to add suppressed exceptions
                    }
                });
            }
            throw new RuntimeException(message.toString(), mainException);
        }
    }
}
