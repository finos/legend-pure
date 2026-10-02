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

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.finos.legend.pure.lsp.protocol.LegendLanguageClient;
import org.finos.legend.pure.lsp.protocol.LegendLogEvent;
import org.finos.legend.pure.lsp.protocol.LockContentionEvent;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.finos.legend.pure.lsp.protocol.TestEvent;
import org.finos.legend.pure.lsp.protocol.WorkspaceDriftEvent;

/**
 * Base for test clients that only care about one notification. Every other LegendLanguageClient
 * method no-ops, so a test can override just what it needs instead of restating the whole interface
 * (and so adding a notification to the interface does not break every test double at once).
 */
public class NoOpLegendLanguageClient implements LegendLanguageClient
{
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

    @Override
    public void lockContention(LockContentionEvent event)
    {
    }

    @Override
    public void testEvent(TestEvent event)
    {
    }
}
