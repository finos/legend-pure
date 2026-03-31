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

import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.finos.legend.pure.m4.transaction.framework.Transaction;
import org.finos.legend.pure.m4.transaction.framework.TransactionManager;
import org.finos.legend.pure.m4.transaction.framework.TransactionStateException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestTransaction
{
    private final StubTransactionManager manager = new StubTransactionManager();

    @Test
    public void testStateAfterCommit()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        Assert.assertTrue(transaction.isOpen());
        Assert.assertFalse(transaction.isCommitting());
        Assert.assertFalse(transaction.isCommitted());
        Assert.assertFalse(transaction.isRollingBack());
        Assert.assertFalse(transaction.isRolledBack());
        Assert.assertFalse(transaction.isInvalid());
        Assert.assertTrue(this.manager.isRegistered(transaction));

        transaction.commit();
        Assert.assertFalse(transaction.isOpen());
        Assert.assertFalse(transaction.isCommitting());
        Assert.assertTrue(transaction.isCommitted());
        Assert.assertFalse(transaction.isRollingBack());
        Assert.assertFalse(transaction.isRolledBack());
        Assert.assertFalse(transaction.isInvalid());
        Assert.assertFalse(this.manager.isRegistered(transaction));
    }

    @Test
    public void testCommitNonCommittable()
    {
        StubTransaction transaction = this.manager.newTransaction(false);
        IllegalStateException e = Assert.assertThrows(IllegalStateException.class, transaction::commit);
        Assert.assertEquals("Transaction is not committable", e.getMessage());
    }

    @Test
    public void testCommitAfterCommit()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        transaction.commit();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::commit);
        Assert.assertEquals("Unexpected transaction state while preparing to commit; expected: open; actual: committed", e.getMessage());
    }

    @Test
    public void testRollbackAfterCommit()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        transaction.commit();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::rollback);
        Assert.assertEquals("Unexpected transaction state while preparing to roll back; expected: open; actual: committed", e.getMessage());
    }

    @Test
    public void testOpenInThreadCurrentThreadAfterCommit()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        transaction.commit();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::openInCurrentThread);
        Assert.assertEquals("Unexpected transaction state; expected: open; actual: committed", e.getMessage());
    }

    @Test
    public void testStateAfterRollback()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        Assert.assertTrue(transaction.isOpen());
        Assert.assertFalse(transaction.isCommitting());
        Assert.assertFalse(transaction.isCommitted());
        Assert.assertFalse(transaction.isRollingBack());
        Assert.assertFalse(transaction.isRolledBack());
        Assert.assertFalse(transaction.isInvalid());
        Assert.assertTrue(this.manager.isRegistered(transaction));

        transaction.rollback();
        Assert.assertFalse(transaction.isOpen());
        Assert.assertFalse(transaction.isCommitting());
        Assert.assertFalse(transaction.isCommitted());
        Assert.assertFalse(transaction.isRollingBack());
        Assert.assertTrue(transaction.isRolledBack());
        Assert.assertFalse(transaction.isInvalid());
        Assert.assertFalse(this.manager.isRegistered(transaction));
    }

    @Test
    public void testCommitAfterRollback()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        transaction.rollback();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::commit);
        Assert.assertEquals("Unexpected transaction state while preparing to commit; expected: open; actual: rolled back", e.getMessage());
    }

    @Test
    public void testRollbackAfterRollback()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        transaction.rollback();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::rollback);
        Assert.assertEquals("Unexpected transaction state while preparing to roll back; expected: open; actual: rolled back", e.getMessage());
    }

    @Test
    public void testOpenInThreadCurrentThreadAfterRollback()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        transaction.rollback();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::openInCurrentThread);
        Assert.assertEquals("Unexpected transaction state; expected: open; actual: rolled back", e.getMessage());
    }

    @Test
    public void testOpenTransactionInCurrentThread() throws Exception
    {
        Transaction transaction = this.manager.newTransaction(true);
        Assert.assertNull(this.manager.getThreadLocalTransaction());
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Assert.assertSame(transaction, this.manager.getThreadLocalTransaction());
            StubTransaction[] otherThreadResult = new StubTransaction[1];
            Thread otherThread = new Thread(() -> otherThreadResult[0] = TestTransaction.this.manager.getThreadLocalTransaction());
            otherThread.setDaemon(true);
            otherThread.start();
            otherThread.join();
            Assert.assertNull(otherThreadResult[0]);
        }
        Assert.assertNull(this.manager.getThreadLocalTransaction());
    }

    @Test
    public void testOpenTransactionInOtherThread() throws Exception
    {
        Transaction transaction = this.manager.newTransaction(true);
        Assert.assertNull(this.manager.getThreadLocalTransaction());

        AtomicBoolean transactionOpened = new AtomicBoolean(false);
        AtomicBoolean transactionChecked = new AtomicBoolean(false);
        StubTransaction[] currentThreadResults = new StubTransaction[3];
        StubTransaction[] otherThreadResults = new StubTransaction[3];
        Thread otherThread = new Thread(() ->
        {
            otherThreadResults[0] = TestTransaction.this.manager.getThreadLocalTransaction();
            try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
            {
                transactionOpened.set(true);
                otherThreadResults[1] = TestTransaction.this.manager.getThreadLocalTransaction();
                while (!transactionChecked.get())
                {
                    // wait
                }
            }
            otherThreadResults[2] = TestTransaction.this.manager.getThreadLocalTransaction();
        });
        otherThread.setDaemon(true);

        currentThreadResults[0] = this.manager.getThreadLocalTransaction();
        otherThread.start();
        while (!transactionOpened.get())
        {
            // wait
        }
        currentThreadResults[1] = this.manager.getThreadLocalTransaction();
        transactionChecked.set(true);
        otherThread.join();
        currentThreadResults[2] = this.manager.getThreadLocalTransaction();

        Assert.assertNull(otherThreadResults[0]);
        Assert.assertSame(transaction, otherThreadResults[1]);
        Assert.assertNull(otherThreadResults[2]);

        Assert.assertNull(currentThreadResults[0]);
        Assert.assertNull(currentThreadResults[1]);
        Assert.assertNull(currentThreadResults[2]);
    }

    @Test
    public void testNestedOpenTransactionInCurrentThread()
    {
        Transaction transaction = this.manager.newTransaction(true);
        Assert.assertNull(this.manager.getThreadLocalTransaction());
        try (ThreadLocalTransactionContext ignore1 = transaction.openInCurrentThread())
        {
            Assert.assertSame(transaction, this.manager.getThreadLocalTransaction());
            try (ThreadLocalTransactionContext ignore2 = transaction.openInCurrentThread())
            {
                Assert.assertSame(transaction, this.manager.getThreadLocalTransaction());
                try (ThreadLocalTransactionContext ignore3 = transaction.openInCurrentThread())
                {
                    Assert.assertSame(transaction, this.manager.getThreadLocalTransaction());
                }
                Assert.assertSame(transaction, this.manager.getThreadLocalTransaction());
            }
            Assert.assertSame(transaction, this.manager.getThreadLocalTransaction());
        }
        Assert.assertNull(this.manager.getThreadLocalTransaction());
    }

    @Test
    public void testOpenDifferentTransactionsInSameThread()
    {
        Transaction transaction1 = this.manager.newTransaction(false);
        Transaction transaction2 = this.manager.newTransaction(false);
        Assert.assertNull(this.manager.getThreadLocalTransaction());
        openTwoTransactionsInSameThread(transaction1, transaction2);
        openTwoTransactionsInSameThread(transaction2, transaction1);
    }

    private void openTwoTransactionsInSameThread(Transaction transaction1, Transaction transaction2)
    {
        Assert.assertNull(this.manager.getThreadLocalTransaction());
        try (ThreadLocalTransactionContext ignore1 = transaction1.openInCurrentThread())
        {
            Assert.assertSame(transaction1, this.manager.getThreadLocalTransaction());
            IllegalStateException e = Assert.assertThrows(IllegalStateException.class, transaction2::openInCurrentThread);
            Assert.assertEquals("A different transaction is already registered for thread \"" + Thread.currentThread().getName() + "\" (id " + Thread.currentThread().getId() + ")", e.getMessage());
            Assert.assertSame(transaction1, this.manager.getThreadLocalTransaction());
        }
        Assert.assertNull(this.manager.getThreadLocalTransaction());
    }

    @Test
    public void testStateAfterClearingManager()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        Assert.assertTrue(transaction.isOpen());
        Assert.assertFalse(transaction.isCommitting());
        Assert.assertFalse(transaction.isCommitted());
        Assert.assertFalse(transaction.isRollingBack());
        Assert.assertFalse(transaction.isRolledBack());
        Assert.assertFalse(transaction.isInvalid());

        this.manager.clear();
        Assert.assertFalse(transaction.isOpen());
        Assert.assertFalse(transaction.isCommitting());
        Assert.assertFalse(transaction.isCommitted());
        Assert.assertFalse(transaction.isRollingBack());
        Assert.assertFalse(transaction.isRolledBack());
        Assert.assertTrue(transaction.isInvalid());
    }

    @Test
    public void testCommitAfterClearingManager()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        this.manager.clear();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::commit);
        Assert.assertEquals("Unexpected transaction state while preparing to commit; expected: open; actual: invalid", e.getMessage());
    }

    @Test
    public void testRollbackAfterClearingManager()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        this.manager.clear();
        TransactionStateException e = Assert.assertThrows(TransactionStateException.class, transaction::rollback);
        Assert.assertEquals("Unexpected transaction state while preparing to roll back; expected: open; actual: invalid", e.getMessage());
    }

    private static class StubTransaction extends Transaction
    {
        private StubTransaction(TransactionManager<?> transactionManager, boolean committable)
        {
            super(transactionManager, committable);
        }

        @Override
        protected void doCommit()
        {
            // Do nothing
        }

        @Override
        protected void doRollback()
        {
            // Do nothing
        }
    }

    private static class StubTransactionManager extends TransactionManager<StubTransaction>
    {
        @Override
        protected StubTransaction createTransaction(boolean committable)
        {
            return new StubTransaction(this, committable);
        }
    }
}
