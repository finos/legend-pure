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

public class ExecuteGoResult
{
    private boolean success;
    private String error;
    private String output;
    private String errorUri;
    private String returnKind;
    private String returnType;
    private Object returnValue;
    private Integer returnSize;
    private boolean returnTruncated;

    public ExecuteGoResult()
    {
    }

    public ExecuteGoResult(boolean success, String error, String output)
    {
        this(success, error, output, null);
    }

    public ExecuteGoResult(boolean success, String error, String output, String errorUri)
    {
        this.success = success;
        this.error = error;
        this.output = output;
        this.errorUri = errorUri;
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

    public String getOutput()
    {
        return this.output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public String getErrorUri()
    {
        return this.errorUri;
    }

    public void setErrorUri(String errorUri)
    {
        this.errorUri = errorUri;
    }

    /**
     * {@code primitive} | {@code enum} | {@code collection} | {@code empty} | {@code complex}.
     * Null when the call failed. {@code complex} carries a type and size but a null
     * {@link #getReturnValue()} - use print() for those.
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
