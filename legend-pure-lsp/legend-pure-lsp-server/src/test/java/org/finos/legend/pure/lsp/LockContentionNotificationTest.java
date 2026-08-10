// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LegendLogEvent;
import org.finos.legend.pure.lsp.protocol.LockContentionEvent;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEvent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Exercises LegendPureSession's legend/lockContention notification (see acquireLock()/onBlockStart()/
 * onBlockEnd()): a caller that is actually forced to wait for the graph lock, for longer than the
 * notification delay, must produce exactly one active=true event followed by exactly one active=false
 * event. A caller granted the lock immediately, or one whose wait clears before the delay elapses,
 * must produce no events at all.
 */
public class LockContentionNotificationTest
{
    @Test
    public void blockedReadCallerProducesStartAndEndEvents() throws Exception
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        // Shrink the debounce delay so this test doesn't have to sleep for the real 5s default.
        session.setLockContentionNotificationDelayMs(50);

        RecordingClient client = new RecordingClient();
        session.setClient(client);

        LegendPureSession.LockHandle writeHandle = session.acquireGraphWriteLock();
        Assert.assertTrue("No contention expected for an immediately-granted lock", client.events.isEmpty());

        Thread reader = new Thread(() ->
        {
            try (LegendPureSession.LockHandle ignored = session.acquireGraphReadLock())
            {
                // Nothing to do - the point is just acquiring (and promptly releasing) it.
            }
        }, "test-blocked-reader");
        reader.start();

        // The reader can only be blocked (rather than racing ahead) while the write lock above is
        // still held, so this confirms acquireGraphReadLock() really did have to wait for it.
        Assert.assertTrue("Expected a lock-contention start event within 5s",
                client.activeLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue("Session should report itself contended while the reader waits", session.isLockContended());
        Assert.assertNotNull(session.lockContentionReason());

        writeHandle.close();
        reader.join(5000);
        Assert.assertFalse("Reader thread should have finished", reader.isAlive());

        Assert.assertTrue("Expected a lock-contention end event within 5s",
                client.clearedLatch.await(5, TimeUnit.SECONDS));
        Assert.assertFalse("Session should no longer report contention once the reader is done", session.isLockContended());

        Assert.assertEquals("Exactly one start + one end event expected", 2, client.events.size());
        LockContentionEvent start = client.events.get(0);
        Assert.assertTrue(start.isActive());
        Assert.assertEquals("read", start.getLockType());
        LockContentionEvent end = client.events.get(1);
        Assert.assertFalse(end.isActive());
        Assert.assertEquals("read", end.getLockType());
    }

    @Test
    public void immediatelyGrantedLock_producesNoContentionEvent()
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();

        RecordingClient client = new RecordingClient();
        session.setClient(client);

        try (LegendPureSession.LockHandle ignored = session.acquireGraphReadLock())
        {
            Assert.assertFalse(session.isLockContended());
        }
        Assert.assertTrue("An uncontended acquisition must not publish any event", client.events.isEmpty());
    }

    @Test
    public void contentionShorterThanNotificationDelay_producesNoEvent() throws Exception
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        session.setLockContentionNotificationDelayMs(300);

        RecordingClient client = new RecordingClient();
        session.setClient(client);

        LegendPureSession.LockHandle writeHandle = session.acquireGraphWriteLock();

        Thread reader = new Thread(() ->
        {
            try (LegendPureSession.LockHandle ignored = session.acquireGraphReadLock())
            {
                // Nothing to do - the point is just acquiring (and promptly releasing) it.
            }
        }, "test-short-blocked-reader");
        reader.start();

        // Confirm the reader is genuinely blocked (not just "hasn't started yet"), while staying well
        // under the 300ms debounce delay above so the active notification never gets a chance to fire.
        long deadline = System.currentTimeMillis() + 200;
        while (!session.isLockContended() && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(5);
        }
        Assert.assertTrue("Reader should be blocked on the write lock", session.isLockContended());

        writeHandle.close();
        reader.join(5000);
        Assert.assertFalse("Reader thread should have finished", reader.isAlive());

        // Give the (expected-to-be-cancelled) debounce timer a chance to fire if cancellation failed.
        Thread.sleep(500);
        Assert.assertTrue("Contention shorter than the debounce delay must not publish any event", client.events.isEmpty());
    }

    private static class RecordingClient implements LegendLanguageClient
    {
        final List<LockContentionEvent> events = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch activeLatch = new CountDownLatch(1);
        final CountDownLatch clearedLatch = new CountDownLatch(1);

        @Override
        public void lockContention(LockContentionEvent event)
        {
            this.events.add(event);
            if (event.isActive())
            {
                this.activeLatch.countDown();
            }
            else
            {
                this.clearedLatch.countDown();
            }
        }

        @Override
        public void telemetryEvent(Object object)
        {
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics)
        {
        }

        @Override
        public void showMessage(MessageParams messageParams)
        {
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams)
        {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams message)
        {
        }

        @Override
        public void statusChanged(LspStatus status)
        {
        }

        @Override
        public void logOutput(LegendLogEvent event)
        {
        }

        @Override
        public void workspaceDriftDetected(WorkspaceDriftEvent event)
        {
        }
    }
}
