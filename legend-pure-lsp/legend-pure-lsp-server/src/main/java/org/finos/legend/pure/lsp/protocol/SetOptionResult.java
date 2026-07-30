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
 * Result of legend/setOption. Echoes back the resolved state so a caller can confirm
 * isOptionSet('&lt;name&gt;') now returns {@link #isEffective()} in this JVM.
 */
public class SetOptionResult
{
    private boolean success;
    private String name;
    private boolean effective;
    private String error;

    public SetOptionResult()
    {
    }

    public SetOptionResult(boolean success, String name, boolean effective, String error)
    {
        this.success = success;
        this.name = name;
        this.effective = effective;
        this.error = error;
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isEffective()
    {
        return this.effective;
    }

    public void setEffective(boolean effective)
    {
        this.effective = effective;
    }

    public String getError()
    {
        return this.error;
    }

    public void setError(String error)
    {
        this.error = error;
    }
}
