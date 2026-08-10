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
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import org.finos.legend.pure.lsp.protocol.LegendLogEvent;
import org.finos.legend.pure.lsp.protocol.LogErrorEntry;

/**
 * Logs to stderr; stdout is reserved for JSON-RPC. Also broadcasts to an optional sink (set by
 * LegendPureLspServer.connect()) so a connected client can show these lines in a live log view - this
 * is the only way a client sees them once the server is run as a socket daemon it didn't spawn, since
 * there's no owned child process whose stdout/stderr IntelliJ (or any other launcher) can capture.
 */
public class LspLog
{
    // Bounded so a noisy failure mode can't grow this unboundedly; recent enough to diagnose "what
    // just went wrong" from legend/status without needing a live logOutput subscriber attached at
    // the time. Surfaced via LspStatus#getRecentErrors().
    private static final int MAX_RECENT_ERRORS = 20;
    private static final Deque<LogErrorEntry> recentErrors = new ConcurrentLinkedDeque<>();

    private static volatile Consumer<LegendLogEvent> sink;

    public static void setSink(Consumer<LegendLogEvent> newSink)
    {
        sink = newSink;
    }

    public static void info(String message)
    {
        System.err.println("[LSP] " + message);
        publish("INFO", message);
    }

    public static void info(String format, Object... args)
    {
        String message = String.format(format.replace("{}", "%s"), args);
        System.err.println("[LSP] " + message);
        publish("INFO", message);
    }

    public static void warn(String message)
    {
        System.err.println("[LSP-WARN] " + message);
        publish("WARN", message);
    }

    public static void error(String message)
    {
        System.err.println("[LSP-ERROR] " + message);
        recordError(message);
        publish("ERROR", message);
    }

    public static void debug(String message)
    {
        System.err.println("[LSP-DEBUG] " + message);
        publish("DEBUG", message);
    }

    public static List<LogErrorEntry> recentErrors()
    {
        return new ArrayList<>(recentErrors);
    }

    private static void recordError(String message)
    {
        recentErrors.addLast(new LogErrorEntry(System.currentTimeMillis(), message));
        while (recentErrors.size() > MAX_RECENT_ERRORS)
        {
            recentErrors.pollFirst();
        }
    }

    private static void publish(String level, String message)
    {
        Consumer<LegendLogEvent> currentSink = sink;
        if (currentSink != null)
        {
            try
            {
                currentSink.accept(new LegendLogEvent(level, message));
            }
            catch (Exception ignored)
            {
                // Never let a broken client-side log sink take down server logging itself.
            }
        }
    }
}
