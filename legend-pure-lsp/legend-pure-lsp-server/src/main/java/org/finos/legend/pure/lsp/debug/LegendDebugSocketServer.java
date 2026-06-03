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

package org.finos.legend.pure.lsp.debug;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher;
import org.finos.legend.pure.lsp.protocol.DapEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendDebugSocketServer implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendDebugSocketServer.class);
    private static final String HOST = "127.0.0.1";

    private final DebugService debugService;
    private final ServerSocket serverSocket;
    private final ExecutorService acceptExecutor;
    private final ExecutorService connectionExecutor;

    private volatile boolean closed;

    public LegendDebugSocketServer(DebugService debugService)
    {
        this.debugService = debugService;
        try
        {
            this.serverSocket = new ServerSocket(0, 1, InetAddress.getByName(HOST));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not start Legend Pure DAP server", e);
        }
        this.acceptExecutor = Executors.newSingleThreadExecutor(r ->
        {
            Thread thread = new Thread(r, "legend-pure-dap-accept");
            thread.setDaemon(true);
            return thread;
        });
        this.connectionExecutor = Executors.newCachedThreadPool(r ->
        {
            Thread thread = new Thread(r, "legend-pure-dap");
            thread.setDaemon(true);
            return thread;
        });
        this.acceptExecutor.submit(this::acceptLoop);
    }

    public DapEndpoint endpoint()
    {
        return new DapEndpoint(HOST, this.serverSocket.getLocalPort());
    }

    private void acceptLoop()
    {
        while (!this.closed)
        {
            try
            {
                Socket socket = this.serverSocket.accept();
                this.connectionExecutor.submit(() -> handle(socket));
            }
            catch (IOException e)
            {
                if (!this.closed)
                {
                    LOGGER.warn("Legend Pure DAP accept failed", e);
                }
            }
        }
    }

    private void handle(Socket socket)
    {
        try (Socket closeable = socket)
        {
            LegendPureDebugAdapter adapter = new LegendPureDebugAdapter(this.debugService);
            Launcher<IDebugProtocolClient> launcher = DebugLauncher.createLauncher(
                    adapter,
                    IDebugProtocolClient.class,
                    closeable.getInputStream(),
                    closeable.getOutputStream());
            adapter.connect(launcher.getRemoteProxy());
            launcher.startListening().get();
        }
        catch (Exception e)
        {
            if (!this.closed)
            {
                LOGGER.debug("Legend Pure DAP connection ended", e);
            }
        }
    }

    @Override
    public void close()
    {
        this.closed = true;
        try
        {
            this.serverSocket.close();
        }
        catch (IOException ignored)
        {
        }
        this.acceptExecutor.shutdownNow();
        this.connectionExecutor.shutdownNow();
    }
}
