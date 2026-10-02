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

/**
 * Params for legend/cancelTests: stop an in-flight legend/executeTests run, identified by
 * {@code runId} (see {@link ExecuteTestsResult#getRunId()}), or every run via {@code all}.
 */
public class CancelTestsParams
{
    private String runId;
    private boolean all;

    public CancelTestsParams()
    {
    }

    public CancelTestsParams(String runId)
    {
        this.runId = runId;
    }

    public String getRunId()
    {
        return this.runId;
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }

    public boolean isAll()
    {
        return this.all;
    }

    public void setAll(boolean all)
    {
        this.all = all;
    }
}
