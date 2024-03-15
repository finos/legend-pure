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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Transaction} should be created by a {@link TransactionManager}. A transaction
 * can be closed by either committing or rolling back. Every transaction should be closed
 * in one of those two ways.
 */
public abstract class Transaction
{
    private static final int OPEN = 0;
    private static final int COMMITTING = 1;
    private static final int COMMITTED = 2;
    private static final int ROLLING_BACK = -1;
    private static final int ROLLED_BACK = -2;
    private static final int INVALID = -3;

    private final AtomicInteger state = new AtomicInteger(OPEN);
    private final TransactionManager<?> manager;
    private final boolean committable;

    protected Transaction(TransactionManager<?> manager, boolean committable)
    {
        this.manager = manager;
        this.committable = committable;
    }

    public boolean isOpen()
    {
        return this.state.get() == OPEN;
    }

    public boolean isCommitting()
    {
        return this.state.get() == COMMITTING;
    }

    public boolean isCommitted()
    {
        return this.state.get() == COMMITTED;
    }

    public boolean isRollingBack()
    {
        return this.state.get() == ROLLING_BACK;
    }

    public boolean isRolledBack()
    {
        return this.state.get() == ROLLED_BACK;
    }

    public boolean isInvalid()
    {
        return this.state.get() == INVALID;
    }

    public boolean isCommittable()
    {
        return this.committable;
    }

    public void commit()
    {
        if (!isCommittable())
        {
            throw new IllegalStateException("Transaction is not committable");
        }
        if (!this.state.compareAndSet(OPEN, COMMITTING))
        {
            throw new TransactionStateException("Unexpected transaction state while preparing to commit", OPEN, this.state.get());
        }
        try
        {
            if (!this.manager.deregisterTransaction(this))
            {
                throw new IllegalStateException("Transaction is not registered with its manager");
            }
            doCommit();
            if (!this.state.compareAndSet(COMMITTING, COMMITTED))
            {
                throw new TransactionStateException("Unexpected transaction state while committing", COMMITTING, this.state.get());
            }
        }
        catch (Throwable t)
        {
            this.state.set(INVALID);
            throw t;
        }
    }

    public void rollback()
    {
        if (!this.state.compareAndSet(OPEN, ROLLING_BACK))
        {
            throw new TransactionStateException("Unexpected transaction state while preparing to roll back", OPEN, this.state.get());
        }
        try
        {
            this.manager.deregisterTransaction(this);
            doRollback();
            if (!this.state.compareAndSet(ROLLING_BACK, ROLLED_BACK))
            {
                throw new TransactionStateException("Unexpected transaction state while rolling back", ROLLING_BACK, this.state.get());
            }
        }
        catch (Throwable t)
        {
            this.state.set(INVALID);
            throw t;
        }
    }

    /**
     * Open the transaction in the current thread. This means that the transaction
     * will be returned by {@linkplain TransactionManager#getThreadLocalTransaction()}
     * when called in the current thread. This will remain the case until the {@code close}
     * method on the returned {@link ThreadLocalTransactionContext} is called.
     *
     * <p>It is strongly recommended that this be used with a {@code try}-with-resources
     * statement to ensure that the {@link ThreadLocalTransactionContext} is closed
     * properly.
     *
     * @return thread local transaction context
     */
    public ThreadLocalTransactionContext openInCurrentThread()
    {
        checkOpen();
        return this.manager.setThreadLocalTransaction(this);
    }

    public TransactionManager<?> getTransactionManager()
    {
        return this.manager;
    }

    protected void checkOpen()
    {
        checkState(OPEN);
    }

    protected void checkState(int expectedState)
    {
        int currentState = this.state.get();
        if (currentState != expectedState)
        {
            throw new TransactionStateException(expectedState, currentState);
        }
    }

    protected abstract void doCommit();

    protected abstract void doRollback();

    /**
     * If the transaction is currently open, set its state to invalid.
     *
     * @return if the transaction was invalidated
     */
    boolean invalidateIfOpen()
    {
        return this.state.compareAndSet(OPEN, INVALID);
    }

    static String getStateString(int state)
    {
        switch (state)
        {
            case OPEN:
            {
                return "open";
            }
            case COMMITTING:
            {
                return "committing";
            }
            case COMMITTED:
            {
                return "committed";
            }
            case ROLLING_BACK:
            {
                return "rolling back";
            }
            case ROLLED_BACK:
            {
                return "rolled back";
            }
            case INVALID:
            {
                return "invalid";
            }
            default:
            {
                throw new IllegalArgumentException("Unknown state: " + state);
            }
        }
    }
}
