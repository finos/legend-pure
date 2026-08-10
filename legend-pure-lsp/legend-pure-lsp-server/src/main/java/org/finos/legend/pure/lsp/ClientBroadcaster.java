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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LegendLogEvent;
import org.finos.legend.pure.lsp.protocol.LockContentionEvent;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fans out every server-initiated push (diagnostics, status changes, log output, show-message) to
 * every currently-connected LSP client, instead of a single overwritten "most recent" client - see
 * {@link LegendPureLspServer#runSocketMode(LegendPureLspServer, int)}, whose accept loop genuinely
 * supports multiple concurrent connections (an IDE session plus separate agent/CLI tooling sharing
 * one warm compiled session). Implements {@link LegendLanguageClient} itself so it can be handed to
 * {@link org.finos.legend.pure.lsp.diagnostics.DiagnosticService},
 * {@link org.finos.legend.pure.lsp.runtime.PureRuntimeManager}, and {@link LspLog}'s sink exactly
 * like a single client - none of them need to know they're actually talking to N clients.
 * <p>
 * A broadcast failure to one client (e.g. a socket that's mid-close) is logged and that client is
 * deregistered; it never blocks delivery to the others.
 */
public class ClientBroadcaster implements LegendLanguageClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientBroadcaster.class);

    // Keyed by the raw client reference (the exact LanguageClient passed to register()/deregister())
    // rather than the wrapped LegendLanguageClient - register() may wrap a plain LanguageClient in a
    // fresh LegendLanguageClientAdapter each time it's called, and two such wrappers around the same
    // raw client are reference-distinct (no equals()/hashCode() override), so keying by the wrapper
    // would make deregister() silently fail to find/remove the entry.
    private final Map<LanguageClient, LegendLanguageClient> clients = new ConcurrentHashMap<>();

    /**
     * @return the wrapped client, so the caller can immediately push a catch-up snapshot (e.g. current
     * status) to exactly this new registrant - registering alone never does this (see class javadoc:
     * broadcasts only fire on future pushes), so a client connecting to an ALREADY-ready session would
     * otherwise never receive a legend/statusChanged notification at all.
     */
    public LegendLanguageClient register(LanguageClient rawClient)
    {
        LegendLanguageClient wrapped = (rawClient instanceof LegendLanguageClient)
                ? (LegendLanguageClient) rawClient
                : new LegendLanguageClientAdapter(rawClient);
        this.clients.put(rawClient, wrapped);
        return wrapped;
    }

    public void deregister(LanguageClient rawClient)
    {
        this.clients.remove(rawClient);
    }

    public int connectedClientCount()
    {
        return this.clients.size();
    }

    @Override
    public void telemetryEvent(Object object)
    {
        broadcast(c -> c.telemetryEvent(object));
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics)
    {
        broadcast(c -> c.publishDiagnostics(diagnostics));
    }

    @Override
    public void showMessage(MessageParams messageParams)
    {
        broadcast(c -> c.showMessage(messageParams));
    }

    @Override
    public void logMessage(MessageParams message)
    {
        broadcast(c -> c.logMessage(message));
    }

    @Override
    public void statusChanged(LspStatus status)
    {
        broadcast(c -> c.statusChanged(status));
    }

    @Override
    public void logOutput(LegendLogEvent event)
    {
        broadcast(c -> c.logOutput(event));
    }

    @Override
    public void workspaceDriftDetected(WorkspaceDriftEvent event)
    {
        broadcast(c -> c.workspaceDriftDetected(event));
    }

    @Override
    public void lockContention(LockContentionEvent event)
    {
        broadcast(c -> c.lockContention(event));
    }

    /**
     * No natural multi-client fan-out for a request/response call (whose answer would win?) - this
     * method isn't actually invoked anywhere in this server today. Best-effort: delegate to any one
     * currently-connected client, or resolve to a null action if none are connected.
     */
    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams)
    {
        for (LegendLanguageClient client : this.clients.values())
        {
            return client.showMessageRequest(requestParams);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void broadcast(Consumer<LegendLanguageClient> action)
    {
        for (Map.Entry<LanguageClient, LegendLanguageClient> entry : this.clients.entrySet())
        {
            try
            {
                action.accept(entry.getValue());
            }
            catch (Exception e)
            {
                // Deregister before logging: LOGGER here is a plain SLF4J logger, never LspLog,
                // specifically so this failure path can never re-enter
                // LspLog.publish() -> this.logOutput() -> broadcast() over a registry that still
                // contains the same dead client.
                this.clients.remove(entry.getKey());
                LOGGER.warn("Broadcast to a connected LSP client failed; deregistering it", e);
            }
        }
    }

    private static final class LegendLanguageClientAdapter implements LegendLanguageClient
    {
        private final LanguageClient delegate;

        private LegendLanguageClientAdapter(LanguageClient delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void telemetryEvent(Object object)
        {
            this.delegate.telemetryEvent(object);
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics)
        {
            this.delegate.publishDiagnostics(diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams)
        {
            this.delegate.showMessage(messageParams);
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams)
        {
            return this.delegate.showMessageRequest(requestParams);
        }

        @Override
        public void logMessage(MessageParams message)
        {
            this.delegate.logMessage(message);
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

        @Override
        public void lockContention(LockContentionEvent event)
        {
        }
    }
}
