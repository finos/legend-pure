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

package org.finos.legend.pure.lsp.protocol;

import java.util.Collections;
import java.util.List;

/**
 * Result of legend/cancelTests: the run ids actually signalled (empty means nothing matched, not an
 * error). A signal, not a join - the run itself unwinds asynchronously and returns its own partial
 * {@link ExecuteTestsResult} to whoever called legend/executeTests.
 */
public class CancelTestsResult
{
    private List<String> cancelledRunIds;
    private String message;

    public CancelTestsResult()
    {
    }

    public CancelTestsResult(List<String> cancelledRunIds, String message)
    {
        this.cancelledRunIds = cancelledRunIds == null ? Collections.emptyList() : cancelledRunIds;
        this.message = message;
    }

    /** Convenience for clients: whether anything was actually signalled. */
    public boolean isCancelled()
    {
        return this.cancelledRunIds != null && !this.cancelledRunIds.isEmpty();
    }

    public List<String> getCancelledRunIds()
    {
        return this.cancelledRunIds;
    }

    public void setCancelledRunIds(List<String> cancelledRunIds)
    {
        this.cancelledRunIds = cancelledRunIds;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
