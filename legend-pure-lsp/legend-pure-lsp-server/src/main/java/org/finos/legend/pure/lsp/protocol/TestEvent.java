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
 * One streamed progress event from an in-flight legend/executeTests run (see
 * {@link LegendLanguageClient#testEvent}), letting a client's test tree fill in as the run proceeds
 * instead of waiting for the final response. {@code runId} routes events when a client has concurrent
 * runs. Which fields are set depends on {@code kind}: RUN_STARTED/RUN_FINISHED carry {@code total}
 * and {@code scope}; SUITE_STARTED/SUITE_FINISHED carry {@code suitePath}; TEST_STARTED carries a
 * {@code result} with only identity fields populated; TEST_FINISHED carries the complete result.
 */
public class TestEvent
{
    private String runId;
    private String kind;
    private String scope;
    private String suitePath;
    private int total;
    private TestResult result;

    public TestEvent()
    {
    }

    public TestEvent(String runId, TestEventKind kind)
    {
        this.runId = runId;
        this.kind = kind == null ? null : kind.getProtocolValue();
    }

    public static TestEvent runStarted(String runId, String scope, int total)
    {
        TestEvent event = new TestEvent(runId, TestEventKind.RUN_STARTED);
        event.scope = scope;
        event.total = total;
        return event;
    }

    public static TestEvent runFinished(String runId, String scope, int total)
    {
        TestEvent event = new TestEvent(runId, TestEventKind.RUN_FINISHED);
        event.scope = scope;
        event.total = total;
        return event;
    }

    public static TestEvent suite(String runId, TestEventKind kind, String suitePath)
    {
        TestEvent event = new TestEvent(runId, kind);
        event.suitePath = suitePath;
        return event;
    }

    public static TestEvent test(String runId, TestEventKind kind, TestResult result)
    {
        TestEvent event = new TestEvent(runId, kind);
        event.result = result;
        event.suitePath = result == null ? null : result.getSuitePath();
        return event;
    }

    public String getRunId()
    {
        return this.runId;
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }

    public String getKind()
    {
        return this.kind;
    }

    public void setKind(String kind)
    {
        this.kind = kind;
    }

    public String getScope()
    {
        return this.scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public String getSuitePath()
    {
        return this.suitePath;
    }

    public void setSuitePath(String suitePath)
    {
        this.suitePath = suitePath;
    }

    public int getTotal()
    {
        return this.total;
    }

    public void setTotal(int total)
    {
        this.total = total;
    }

    public TestResult getResult()
    {
        return this.result;
    }

    public void setResult(TestResult result)
    {
        this.result = result;
    }
}
