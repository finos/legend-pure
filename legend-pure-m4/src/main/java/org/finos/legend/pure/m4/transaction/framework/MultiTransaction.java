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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;

public abstract class MultiTransaction extends Transaction
{
    private static final Procedure<Transaction> COMMIT = new Procedure<Transaction>()
    {
        @Override
        public void value(Transaction transaction)
        {
            transaction.commit();
        }
    };

    private static final Procedure<Transaction> ROLLBACK = new Procedure<Transaction>()
    {
        @Override
        public void value(Transaction transaction)
        {
            transaction.rollback();
        }
    };

    private static final Procedure<ThreadLocalTransactionContext> CLOSE = new Procedure<ThreadLocalTransactionContext>()
    {
        @Override
        public void value(ThreadLocalTransactionContext transaction)
        {
            transaction.close();
        }
    };

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
        forEachWithExceptionCollection(this.transactions, COMMIT, "committing");
    }

    @Override
    protected void doRollback()
    {
        forEachWithExceptionCollection(this.transactions, ROLLBACK, "rolling back");
    }

    @Override
    public ThreadLocalTransactionContext openInCurrentThread()
    {
        MutableList<ThreadLocalTransactionContext> contexts = FastList.newList(this.transactions.size() + 1);
        try
        {
            contexts.add(super.openInCurrentThread());
            for (Transaction transaction : this.transactions)
            {
                contexts.add(transaction.openInCurrentThread());
            }
        }
        catch (Exception e)
        {
            for (ThreadLocalTransactionContext context : contexts)
            {
                try
                {
                    context.close();
                }
                catch (Exception ignore)
                {
                    // Ignore
                }
            }
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
            forEachWithExceptionCollection(this.contexts, CLOSE, "closing");
        }
    }

    private static <T> void forEachWithExceptionCollection(ListIterable<T> list, Procedure<? super T> procedure, String descriptionForExceptions)
    {
        MutableList<Exception> exceptions = Lists.mutable.empty();
        list.forEachWith(new ExceptionCollectionProcedure<>(procedure), exceptions);
        if (exceptions.notEmpty())
        {
            StringBuilder message = new StringBuilder();
            if (exceptions.size() == 1)
            {
                message.append("An exception");
            }
            else
            {
                message.append(exceptions.size());
                message.append(" exceptions");
            }
            message.append(" occurred while ");
            message.append(descriptionForExceptions);
            message.append(':');
            for (Exception e : exceptions)
            {
                message.append("\n\t");
                message.append(e.getMessage());
            }

            throw new RuntimeException(message.toString(), exceptions.get(0));
        }
    }

    private static class ExceptionCollectionProcedure<T> implements Procedure2<T, MutableCollection<Exception>>
    {
        private final Procedure<T> procedure;

        private ExceptionCollectionProcedure(Procedure<T> procedure)
        {
            this.procedure = procedure;
        }

        @Override
        public void value(T element, MutableCollection<Exception> exceptions)
        {
            try
            {
                this.procedure.value(element);
            }
            catch (Exception e)
            {
                exceptions.add(e);
            }
        }
    }
}
