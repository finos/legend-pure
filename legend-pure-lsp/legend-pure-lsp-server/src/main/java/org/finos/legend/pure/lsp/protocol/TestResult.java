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
 * One executed entry of a legend/executeTests run: a test function, or a
 * {@code <<test.BeforePackage>>}/{@code <<test.AfterPackage>>} hook - reported as its own entry
 * (mirroring {@code PureTestBuilder#buildSuite}) so a failing setup shows up as one named failure
 * rather than a cascade. {@code hook}/{@code hookKind} distinguish these; {@code suitePath} is the
 * package node the entry belongs to.
 */
public class TestResult
{
    private String functionPath;
    private String name;
    private String suitePath;
    private String status;
    private long durationMs;
    private String message;
    private String output;
    private String pctAdapterPath;
    private boolean hook;
    private String hookKind;
    private String returnKind;
    private String returnType;
    private Object returnValue;
    private Integer returnSize;
    private boolean returnTruncated;

    public TestResult()
    {
    }

    public TestResult(String functionPath, String name, String suitePath, TestStatus status, long durationMs,
                      String message, String output, String pctAdapterPath, boolean hook, String hookKind)
    {
        this.functionPath = functionPath;
        this.name = name;
        this.suitePath = suitePath;
        this.status = status == null ? null : status.getProtocolValue();
        this.durationMs = durationMs;
        this.message = message;
        this.output = output;
        this.pctAdapterPath = pctAdapterPath;
        this.hook = hook;
        this.hookKind = hookKind;
    }

    public String getFunctionPath()
    {
        return this.functionPath;
    }

    public void setFunctionPath(String functionPath)
    {
        this.functionPath = functionPath;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSuitePath()
    {
        return this.suitePath;
    }

    public void setSuitePath(String suitePath)
    {
        this.suitePath = suitePath;
    }

    public String getStatus()
    {
        return this.status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public long getDurationMs()
    {
        return this.durationMs;
    }

    public void setDurationMs(long durationMs)
    {
        this.durationMs = durationMs;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getOutput()
    {
        return this.output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public String getPctAdapterPath()
    {
        return this.pctAdapterPath;
    }

    public void setPctAdapterPath(String pctAdapterPath)
    {
        this.pctAdapterPath = pctAdapterPath;
    }

    public boolean isHook()
    {
        return this.hook;
    }

    public void setHook(boolean hook)
    {
        this.hook = hook;
    }

    public String getHookKind()
    {
        return this.hookKind;
    }

    public void setHookKind(String hookKind)
    {
        this.hookKind = hookKind;
    }

    /**
     * {@code primitive} | {@code enum} | {@code collection} | {@code empty} | {@code complex}.
     * Set only for an entry that actually ran - a skipped or cancelled entry leaves it null. This
     * is what lets legend/executeTests' explicit-{@code functions} mode double as a batch runner
     * for ordinary value-returning functions, not just {@code <<test.Test>>} ones.
     */
    public String getReturnKind()
    {
        return this.returnKind;
    }

    public void setReturnKind(String returnKind)
    {
        this.returnKind = returnKind;
    }

    public String getReturnType()
    {
        return this.returnType;
    }

    public void setReturnType(String returnType)
    {
        this.returnType = returnType;
    }

    public Object getReturnValue()
    {
        return this.returnValue;
    }

    public void setReturnValue(Object returnValue)
    {
        this.returnValue = returnValue;
    }

    /** True element count, before any truncation. */
    public Integer getReturnSize()
    {
        return this.returnSize;
    }

    public void setReturnSize(Integer returnSize)
    {
        this.returnSize = returnSize;
    }

    public boolean isReturnTruncated()
    {
        return this.returnTruncated;
    }

    public void setReturnTruncated(boolean returnTruncated)
    {
        this.returnTruncated = returnTruncated;
    }
}
