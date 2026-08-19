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

package org.finos.legend.pure.lsp.mcp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.lsp.UriMapper;
import org.finos.legend.pure.lsp.WorkspaceSymbolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone MCP stdio server for a Pure workspace. Launched by an MCP client
 * (e.g. Claude Code) as:
 *   java -cp "legend-pure-mcp-server.jar:dependency/*" \
 *       org.finos.legend.pure.lsp.mcp.LegendPureMcpServer --workspace /path/to/project
 * stdout carries the MCP protocol; ALL logging goes to stderr.
 */
public class LegendPureMcpServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendPureMcpServer.class);

    public static void main(String[] args) throws IOException
    {
        PrintStream protocolOut = new PrintStream(
                new java.io.BufferedOutputStream(new FileOutputStream(FileDescriptor.out)), true);
        PrintStream stderrOut = new PrintStream(
                new java.io.BufferedOutputStream(new FileOutputStream(FileDescriptor.err)), true);
        // Anything printing to System.out (Pure console defaults, stray libraries) must not
        // corrupt the protocol stream - same discipline as LegendPureLspServer.main.
        System.setOut(stderrOut);
        System.setErr(stderrOut);

        Path workspace;
        try
        {
            workspace = resolveWorkspace(args);
        }
        catch (IllegalArgumentException e)
        {
            stderrOut.println("Usage: LegendPureMcpServer [--workspace <dir>]");
            stderrOut.println(e.getMessage());
            System.exit(1);
            return;
        }

        serve(workspace, System.in, protocolOut);
    }

    static Path resolveWorkspace(String[] args)
    {
        Path workspace = Paths.get("");
        for (int i = 0; i < args.length; i++)
        {
            if ("--workspace".equals(args[i]))
            {
                if ((i + 1) >= args.length)
                {
                    throw new IllegalArgumentException("--workspace requires a directory argument");
                }
                workspace = Paths.get(args[i + 1]);
                i++;
            }
        }
        Path normalized = workspace.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized))
        {
            throw new IllegalArgumentException("Workspace is not a directory: " + normalized);
        }
        return normalized;
    }

    static void serve(Path workspace, InputStream in, OutputStream out) throws IOException
    {
        LOGGER.info("Starting legend-pure-mcp-server for workspace {}", workspace);

        LegendPureSession session = new LegendPureSession();
        RepositoryScanner scanner = new RepositoryScanner();
        UriMapper uriMapper = new UriMapper();
        WorkspaceSymbolProvider symbols = new WorkspaceSymbolProvider();
        WorkspaceSync sync = new WorkspaceSync(scanner);
        InitGate gate = new InitGate();

        Thread initThread = new Thread(() ->
        {
            try
            {
                scanner.scan(Collections.singletonList(workspace));
                session.initialize(scanner);
                uriMapper.setRepositoryScanner(scanner);
                uriMapper.setPureRuntime(session.getPureRuntime());
                symbols.buildIndex(session.getPureRuntime());
                sync.seed();
                gate.ready();
                LOGGER.info("Pure runtime ready; MCP tools are live");
            }
            catch (Throwable t)
            {
                LOGGER.error("Pure runtime initialization failed", t);
                gate.fail("Pure runtime initialization failed: " + t);
            }
        }, "pure-mcp-init");
        initThread.setDaemon(true);
        initThread.start();

        PureToolRegistry registry = PureTools.buildRegistry(session, scanner, sync, uriMapper, symbols, gate);
        new McpStdioServer(registry, in, out, serverVersion()).run();
    }

    private static String serverVersion()
    {
        String version = LegendPureMcpServer.class.getPackage().getImplementationVersion();
        return (version != null) ? version : "dev";
    }
}
