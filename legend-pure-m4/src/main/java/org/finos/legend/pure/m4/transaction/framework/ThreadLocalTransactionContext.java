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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A context which holds a {@link Transaction} open in a particular thread;
 * specifically, the thread in which {@linkplain Transaction#openInCurrentThread()}
 * was called. This means that the transaction will be returned by
 * {@linkplain TransactionManager#getThreadLocalTransaction()} when called in
 * that thread. This will remain true so long as this context is open. Once
 * {@code close} is called, the transaction will no longer be open in that
 * thread.
 *
 * <p>It is strongly recommended that this be used with a {@code try}-with-resources
 * statement to ensure that it is closed properly.
 */
public abstract class ThreadLocalTransactionContext implements AutoCloseable
{
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public void close()
    {
        if (this.closed.compareAndSet(false, true))
        {
            doClose();
        }
    }

    /**
     * Perform whatever actions are necessary for closing.
     * This will only be called once.
     */
    protected abstract void doClose();
}
