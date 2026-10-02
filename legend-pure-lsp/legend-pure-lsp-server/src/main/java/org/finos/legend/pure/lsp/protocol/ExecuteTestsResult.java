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
 * Result of legend/executeTests. {@code tests} is in {@code PureTestBuilder#buildSuite} execution
 * order, so a client can render it as a tree without re-sorting. {@code success} is false if any
 * test/hook failed, or if the run couldn't start at all (bad scope, uncompilable {@code files}, PCT
 * tests with no adapter) - in which case {@code error} explains why and {@code tests} is empty. A
 * failing hook does not abort the run. {@code skipped} ({@code <<test.ToFix>>}) is tracked separately
 * from {@code passed}/{@code failed} so the latter two stay comparable with a surefire run.
 */
public class ExecuteTestsResult
{
    private boolean success;
    private String error;
    private String errorUri;
    private String runId;
    private String scope;
    private int total;
    private int passed;
    private int failed;
    private int skipped;
    private long durationMs;
    private boolean cancelled;
    private List<TestResult> tests;

    public ExecuteTestsResult()
    {
    }

    public static ExecuteTestsResult failure(String error)
    {
        return failure(error, null);
    }

    public static ExecuteTestsResult failure(String error, String errorUri)
    {
        ExecuteTestsResult result = new ExecuteTestsResult();
        result.success = false;
        result.error = error;
        result.errorUri = errorUri;
        result.tests = Collections.emptyList();
        return result;
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getError()
    {
        return this.error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public String getErrorUri()
    {
        return this.errorUri;
    }

    public void setErrorUri(String errorUri)
    {
        this.errorUri = errorUri;
    }

    public String getRunId()
    {
        return this.runId;
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }

    public String getScope()
    {
        return this.scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public int getTotal()
    {
        return this.total;
    }

    public void setTotal(int total)
    {
        this.total = total;
    }

    public int getPassed()
    {
        return this.passed;
    }

    public void setPassed(int passed)
    {
        this.passed = passed;
    }

    public int getFailed()
    {
        return this.failed;
    }

    public void setFailed(int failed)
    {
        this.failed = failed;
    }

    public int getSkipped()
    {
        return this.skipped;
    }

    public void setSkipped(int skipped)
    {
        this.skipped = skipped;
    }

    public long getDurationMs()
    {
        return this.durationMs;
    }

    public void setDurationMs(long durationMs)
    {
        this.durationMs = durationMs;
    }

    /**
     * True when the run was stopped early by legend/cancelTests (or by the last client
     * disconnecting). {@code tests} then holds only the entries that completed, plus any teardown
     * hooks - which are still run, so a cancelled suite does not strand the state its setup created.
     * A cancelled run is not reported as {@code success}, since it did not finish.
     */
    public boolean isCancelled()
    {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    public List<TestResult> getTests()
    {
        return this.tests;
    }

    public void setTests(List<TestResult> tests)
    {
        this.tests = tests;
    }
}
