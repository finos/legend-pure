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

    public ExecuteGoResult()
    {
    }

    public ExecuteGoResult(boolean success, String error, String output)
    {
        this.success = success;
        this.error = error;
        this.output = output;
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
}
