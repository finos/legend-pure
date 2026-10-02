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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.finos.legend.pure.lsp.protocol.LspStatus;
import org.junit.Assert;
import org.junit.Test;

public class LegendPureLspServerTest
{
    @Test
    public void connect_belowFrequentDisconnectThreshold_pushesNoWarning()
    {
        LegendPureLspServer server = new LegendPureLspServer();
        server.recordAbnormalDisconnect("Client /127.0.0.1:1 disconnected abnormally after 10ms: EOFException");
        server.recordAbnormalDisconnect("Client /127.0.0.1:1 disconnected abnormally after 10ms: EOFException");

        RecordingLanguageClient client = new RecordingLanguageClient();
        server.connect(client);

        Assert.assertTrue("No warning should fire below the frequent-disconnect threshold",
                client.warningMessages().isEmpty());
    }

    @Test
    public void connect_atFrequentDisconnectThreshold_pushesWarningNamingCountAndReason()
    {
        LegendPureLspServer server = new LegendPureLspServer();
        server.recordAbnormalDisconnect("Client /127.0.0.1:1 disconnected abnormally after 10ms: EOFException");
        server.recordAbnormalDisconnect("Client /127.0.0.1:1 disconnected abnormally after 20ms: EOFException");
        String lastReason = "Client /127.0.0.1:1 disconnected abnormally after 30ms: SocketException: Broken pipe";
        server.recordAbnormalDisconnect(lastReason);

        RecordingLanguageClient client = new RecordingLanguageClient();
        server.connect(client);

        List<MessageParams> warnings = client.warningMessages();
        Assert.assertEquals(1, warnings.size());
        String message = warnings.get(0).getMessage();
        Assert.assertTrue(message, message.contains("3 client disconnects"));
        Assert.assertTrue(message, message.contains(lastReason));
    }

    @Test
    public void countRecentAbnormalDisconnects_ignoresEntriesOutsideTheWindow()
    {
        LegendPureLspServer server = new LegendPureLspServer();
        Assert.assertEquals(0, server.countRecentAbnormalDisconnects());

        server.recordAbnormalDisconnect("recent disconnect");
        Assert.assertEquals(1, server.countRecentAbnormalDisconnects());
    }

    @Test
    public void status_reflectsMostRecentAbnormalDisconnect()
    {
        LegendPureLspServer server = new LegendPureLspServer();
        server.recordAbnormalDisconnect("first disconnect");
        server.recordAbnormalDisconnect("second disconnect");

        LspStatus status = server.status().join();

        Assert.assertEquals(2, status.getRecentDisconnectCount());
        Assert.assertEquals("second disconnect", status.getLastDisconnectReason());
    }

    @Test
    public void status_noDisconnectsYet_reportsZeroCountAndNullReason()
    {
        LegendPureLspServer server = new LegendPureLspServer();

        LspStatus status = server.status().join();

        Assert.assertEquals(0, status.getRecentDisconnectCount());
        Assert.assertNull(status.getLastDisconnectReason());
    }

    private static class RecordingLanguageClient implements LanguageClient
    {
        private final List<MessageParams> messages = Collections.synchronizedList(new ArrayList<>());

        List<MessageParams> warningMessages()
        {
            List<MessageParams> warnings = new ArrayList<>();
            for (MessageParams message : this.messages)
            {
                if (message.getType() == MessageType.Warning)
                {
                    warnings.add(message);
                }
            }
            return warnings;
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
    }

    @Test
    public void extractClasspathRepositoryNames_readsDirectInitializationOption()
    {
        InitializeParams params = new InitializeParams();
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("classpathRepositories", Arrays.asList(" core ", "", "pure_ide", "core"));
        params.setInitializationOptions(options);

        Assert.assertEquals(
                Arrays.asList("core", "pure_ide"),
                LegendPureLspServer.extractClasspathRepositoryNames(params));
    }

    @Test
    public void extractClasspathRepositoryNames_readsNestedServerInitializationOption()
    {
        InitializeParams params = new InitializeParams();
        Map<String, Object> serverOptions = new LinkedHashMap<>();
        serverOptions.put("classpathRepositories", Arrays.asList("extension_repo"));
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("server", serverOptions);
        params.setInitializationOptions(options);

        Assert.assertEquals(
                Arrays.asList("extension_repo"),
                LegendPureLspServer.extractClasspathRepositoryNames(params));
    }

    @Test
    public void extractClasspathRepositoryNames_readsJsonInitializationOption()
    {
        InitializeParams params = new InitializeParams();
        JsonArray repositories = new JsonArray();
        repositories.add("core");
        repositories.add("core");
        repositories.add("pure_ide");
        JsonObject options = new JsonObject();
        options.add("classpathRepositories", repositories);
        params.setInitializationOptions(options);

        Assert.assertEquals(
                Arrays.asList("core", "pure_ide"),
                LegendPureLspServer.extractClasspathRepositoryNames(params));
    }

    @Test
    public void extractClasspathRepositoryNames_defaultsToEmptyList()
    {
        Assert.assertEquals(
                Collections.emptyList(),
                LegendPureLspServer.extractClasspathRepositoryNames(new InitializeParams()));
    }

    @Test
    public void resolveSocketPort_parsesSocketArg()
    {
        Assert.assertEquals(7777, LegendPureLspServer.resolveSocketPort(new String[]{"--socket", "7777"}));
    }

    @Test
    public void resolveSocketPort_invalidSocketArg_returnsMinusOne()
    {
        Assert.assertEquals(-1, LegendPureLspServer.resolveSocketPort(new String[]{"--socket", "notaport"}));
    }

    @Test
    public void resolveSocketPort_noSocketArgOrProperty_returnsMinusOne()
    {
        String saved = System.getProperty("legend.lsp.socketPort");
        System.clearProperty("legend.lsp.socketPort");
        try
        {
            Assert.assertEquals(-1, LegendPureLspServer.resolveSocketPort(new String[]{"--other", "x"}));
        }
        finally
        {
            if (saved != null)
            {
                System.setProperty("legend.lsp.socketPort", saved);
            }
        }
    }

    @Test
    public void resolveSocketPort_readsSystemPropertyFallback()
    {
        String saved = System.getProperty("legend.lsp.socketPort");
        System.setProperty("legend.lsp.socketPort", "8899");
        try
        {
            Assert.assertEquals(8899, LegendPureLspServer.resolveSocketPort(new String[]{}));
        }
        finally
        {
            if (saved == null)
            {
                System.clearProperty("legend.lsp.socketPort");
            }
            else
            {
                System.setProperty("legend.lsp.socketPort", saved);
            }
        }
    }

    @Test
    public void resolveRequestPoolSize_noPropertySet_derivesFromAvailableProcessors()
    {
        withRequestPoolSizeProperty(null, () ->
                Assert.assertEquals(LegendPureLspServer.defaultRequestPoolSize(),
                        LegendPureLspServer.resolveRequestPoolSize()));
    }

    @Test
    public void resolveRequestPoolSize_readsValidPropertyOverride()
    {
        withRequestPoolSizeProperty("20", () ->
                Assert.assertEquals(20, LegendPureLspServer.resolveRequestPoolSize()));
    }

    @Test
    public void resolveRequestPoolSize_invalidProperty_fallsBackToDefault()
    {
        withRequestPoolSizeProperty("notanumber", () ->
                Assert.assertEquals(LegendPureLspServer.defaultRequestPoolSize(),
                        LegendPureLspServer.resolveRequestPoolSize()));
    }

    @Test
    public void resolveRequestPoolSize_nonPositiveProperty_fallsBackToDefault()
    {
        withRequestPoolSizeProperty("0", () ->
                Assert.assertEquals(LegendPureLspServer.defaultRequestPoolSize(),
                        LegendPureLspServer.resolveRequestPoolSize()));
        withRequestPoolSizeProperty("-5", () ->
                Assert.assertEquals(LegendPureLspServer.defaultRequestPoolSize(),
                        LegendPureLspServer.resolveRequestPoolSize()));
    }

    @Test
    public void requestPoolIsStrictlyLargerThanExecutionConcurrency()
    {
        Assert.assertTrue(
                "requestPool=" + LegendPureLspServer.defaultRequestPoolSize()
                        + " must exceed executionConcurrency=" + LegendPureLspServer.defaultExecutionConcurrency(),
                LegendPureLspServer.defaultRequestPoolSize() > LegendPureLspServer.defaultExecutionConcurrency());
    }

    @Test
    public void defaultExecutionConcurrency_isThreeQuartersOfCoresFlooredAtTwo()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        Assert.assertEquals(Math.max(2, (cores * 3) / 4), LegendPureLspServer.defaultExecutionConcurrency());
        Assert.assertTrue(LegendPureLspServer.defaultExecutionConcurrency() >= 2);
    }

    @Test
    public void resolveExecutionConcurrency_propertyOverridesAndValidates()
    {
        String name = "legend.lsp.executionConcurrency";
        String saved = System.getProperty(name);
        try
        {
            System.setProperty(name, "3");
            Assert.assertEquals(3, LegendPureLspServer.resolveExecutionConcurrency());
            System.setProperty(name, "0");
            Assert.assertEquals(LegendPureLspServer.defaultExecutionConcurrency(),
                    LegendPureLspServer.resolveExecutionConcurrency());
            System.setProperty(name, "notanumber");
            Assert.assertEquals(LegendPureLspServer.defaultExecutionConcurrency(),
                    LegendPureLspServer.resolveExecutionConcurrency());
            System.clearProperty(name);
            Assert.assertEquals(LegendPureLspServer.defaultExecutionConcurrency(),
                    LegendPureLspServer.resolveExecutionConcurrency());
        }
        finally
        {
            if (saved == null)
            {
                System.clearProperty(name);
            }
            else
            {
                System.setProperty(name, saved);
            }
        }
    }

    private static void withRequestPoolSizeProperty(String value, Runnable assertion)
    {
        String propertyName = "legend.lsp.requestPoolSize";
        String saved = System.getProperty(propertyName);
        if (value == null)
        {
            System.clearProperty(propertyName);
        }
        else
        {
            System.setProperty(propertyName, value);
        }
        try
        {
            assertion.run();
        }
        finally
        {
            if (saved == null)
            {
                System.clearProperty(propertyName);
            }
            else
            {
                System.setProperty(propertyName, saved);
            }
        }
    }
}
