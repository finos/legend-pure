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
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LegendLogEvent;
import org.finos.legend.pure.lsp.protocol.LockContentionEvent;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEvent;
import org.finos.legend.pure.lsp.protocol.LspState;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.junit.Assert;
import org.junit.Test;

public class ClientBroadcasterTest
{
    @Test
    public void broadcastsToAllRegisteredClients()
    {
        ClientBroadcaster broadcaster = new ClientBroadcaster();
        RecordingClient first = new RecordingClient();
        RecordingClient second = new RecordingClient();
        broadcaster.register(first);
        broadcaster.register(second);

        PublishDiagnosticsParams diagnostics = new PublishDiagnosticsParams("file:///a.pure", Collections.emptyList());
        broadcaster.publishDiagnostics(diagnostics);
        Assert.assertEquals(1, first.diagnostics.size());
        Assert.assertEquals(1, second.diagnostics.size());

        LspStatus status = new LspStatus(LspState.READY, 1, 1, 0, false, "ready", 0, 0);
        broadcaster.statusChanged(status);
        Assert.assertEquals(1, first.statuses.size());
        Assert.assertEquals(1, second.statuses.size());

        broadcaster.showMessage(new MessageParams(MessageType.Info, "hi"));
        Assert.assertEquals(1, first.messages.size());
        Assert.assertEquals(1, second.messages.size());

        broadcaster.logOutput(new LegendLogEvent("INFO", "log line"));
        Assert.assertEquals(1, first.logs.size());
        Assert.assertEquals(1, second.logs.size());

        broadcaster.lockContention(new LockContentionEvent(true, "write", "read lock(s) held", 1));
        Assert.assertEquals(1, first.lockContentions.size());
        Assert.assertEquals(1, second.lockContentions.size());
    }

    @Test
    public void oneClientThrowing_doesNotBlockDeliveryToOthers_andIsDeregistered()
    {
        ClientBroadcaster broadcaster = new ClientBroadcaster();
        RecordingClient healthy = new RecordingClient();
        ThrowingClient broken = new ThrowingClient();
        broadcaster.register(healthy);
        broadcaster.register(broken);
        Assert.assertEquals(2, broadcaster.connectedClientCount());

        broadcaster.publishDiagnostics(new PublishDiagnosticsParams("file:///a.pure", Collections.emptyList()));

        Assert.assertEquals("Healthy client must still receive the broadcast", 1, healthy.diagnostics.size());
        Assert.assertEquals("Throwing client should have been auto-deregistered",
                1, broadcaster.connectedClientCount());
    }

    @Test
    public void deregister_stopsFurtherBroadcasts()
    {
        ClientBroadcaster broadcaster = new ClientBroadcaster();
        RecordingClient client = new RecordingClient();
        broadcaster.register(client);
        broadcaster.deregister(client);

        broadcaster.publishDiagnostics(new PublishDiagnosticsParams("file:///a.pure", Collections.emptyList()));
        Assert.assertTrue("Deregistered client should receive nothing", client.diagnostics.isEmpty());
        Assert.assertEquals(0, broadcaster.connectedClientCount());
    }

    @Test
    public void broadcastWithNoRegisteredClients_doesNotThrow()
    {
        ClientBroadcaster broadcaster = new ClientBroadcaster();
        broadcaster.publishDiagnostics(new PublishDiagnosticsParams("file:///a.pure", Collections.emptyList()));
        broadcaster.statusChanged(new LspStatus(LspState.READY, 0, 0, 0, false, "ready", 0, 0));
        broadcaster.showMessage(new MessageParams(MessageType.Info, "hi"));
        broadcaster.logOutput(new LegendLogEvent("INFO", "log line"));
        // No exception is the assertion.
    }

    @Test
    public void register_wrapsPlainLanguageClient_soLegendNotificationsAreNoOpsNotErrors()
    {
        ClientBroadcaster broadcaster = new ClientBroadcaster();
        PlainLanguageClient plain = new PlainLanguageClient();
        broadcaster.register(plain);

        broadcaster.publishDiagnostics(new PublishDiagnosticsParams("file:///a.pure", Collections.emptyList()));
        Assert.assertEquals(1, plain.diagnostics.size());

        // statusChanged/logOutput have no meaning for a plain LanguageClient; the adapter no-ops them
        // rather than throwing, and that no-op must not cause deregistration.
        broadcaster.statusChanged(new LspStatus(LspState.READY, 0, 0, 0, false, "ready", 0, 0));
        Assert.assertEquals(1, broadcaster.connectedClientCount());
    }

    private static class RecordingClient implements LegendLanguageClient
    {
        final List<PublishDiagnosticsParams> diagnostics = Collections.synchronizedList(new ArrayList<>());
        final List<LspStatus> statuses = Collections.synchronizedList(new ArrayList<>());
        final List<MessageParams> messages = Collections.synchronizedList(new ArrayList<>());
        final List<LegendLogEvent> logs = Collections.synchronizedList(new ArrayList<>());
        final List<LockContentionEvent> lockContentions = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void telemetryEvent(Object object)
        {
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics)
        {
            this.diagnostics.add(diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams)
        {
            this.messages.add(messageParams);
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
            this.statuses.add(status);
        }

        @Override
        public void logOutput(LegendLogEvent event)
        {
            this.logs.add(event);
        }

        @Override
        public void workspaceDriftDetected(WorkspaceDriftEvent event)
        {
        }

        @Override
        public void lockContention(LockContentionEvent event)
        {
            this.lockContentions.add(event);
        }
    }

    private static class ThrowingClient implements LegendLanguageClient
    {
        @Override
        public void telemetryEvent(Object object)
        {
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public void showMessage(MessageParams messageParams)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public void logMessage(MessageParams message)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public void statusChanged(LspStatus status)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public void logOutput(LegendLogEvent event)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public void workspaceDriftDetected(WorkspaceDriftEvent event)
        {
            throw new RuntimeException("simulated dead connection");
        }

        @Override
        public void lockContention(LockContentionEvent event)
        {
            throw new RuntimeException("simulated dead connection");
        }
    }

    private static class PlainLanguageClient implements org.eclipse.lsp4j.services.LanguageClient
    {
        final List<PublishDiagnosticsParams> diagnostics = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void telemetryEvent(Object object)
        {
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics)
        {
            this.diagnostics.add(diagnostics);
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
    }
}
