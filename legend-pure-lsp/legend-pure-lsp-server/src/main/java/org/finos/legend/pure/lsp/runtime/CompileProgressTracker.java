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

package org.finos.legend.pure.lsp.runtime;

/**
 * Derives a repositories-compiled/total count from the free-text progress messages
 * {@code IncrementalCompiler_New} already emits during {@code PureRuntime#initialize},
 * without requiring any change to the compiler itself. Plain prefix checks only -
 * this is called on every compiler progress message, including high-frequency ones
 * emitted per bound instance, so it must stay cheap.
 */
class CompileProgressTracker
{
    private static final String ORDER_PREFIX = "Compiling repositories in the following order:[";
    private static final String FINISHED_PREFIX = "Finished compiling ";

    private int total;
    private int completed;

    void reset()
    {
        this.total = 0;
        this.completed = 0;
    }

    void onMessage(String message)
    {
        if (message == null)
        {
            return;
        }
        if (message.startsWith(ORDER_PREFIX) && message.endsWith("]"))
        {
            String inner = message.substring(ORDER_PREFIX.length(), message.length() - 1);
            this.total = inner.isEmpty() ? 0 : inner.split(",").length;
            this.completed = 0;
        }
        else if (message.startsWith(FINISHED_PREFIX))
        {
            this.completed++;
        }
    }

    int getTotal()
    {
        return this.total;
    }

    int getCompleted()
    {
        return this.completed;
    }
}
