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

import java.util.concurrent.CountDownLatch;

/**
 * Runtime initialization runs on a background thread so the MCP handshake responds
 * immediately; tool handlers block here until the session is usable (or init failed).
 */
public class InitGate
{
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile String failure;

    public void ready()
    {
        this.latch.countDown();
    }

    public void fail(String message)
    {
        this.failure = message;
        this.latch.countDown();
    }

    /**
     * Blocks until initialization finishes. Returns null when the session is ready,
     * otherwise a message describing why initialization failed.
     */
    public String await()
    {
        try
        {
            this.latch.await();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "Interrupted while waiting for Pure runtime initialization";
        }
        return this.failure;
    }
}
