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

import java.util.List;

/**
 * Params for legend/execute: run an arbitrary zero-argument function by Pure path (not just go()).
 * {@code function} may be a signature form ("my::pkg::testFoo():Boolean[1]"), a mangled id
 * ("my::pkg::testFoo__Boolean_1_"), or a bare path ("my::pkg::testFoo") in which case common
 * zero-arg return shapes are tried. Optional {@code files} are compiled as one atomic batch before
 * execution (same semantics as ExecuteGoParams.files).
 * <p>
 * {@code pctAdapterPath} is optional and only meaningful for a &lt;&lt;PCT.test&gt;&gt; function,
 * which takes exactly one parameter: the adapter {@code Function} itself. When set, it is the Pure
 * path of one of the adapters returned by legend/getPCTAdapters; omitting it leaves the zero-argument
 * behaviour unchanged for every other function.
 * <p>
 * {@code beforeFunctionPath}/{@code afterFunctionPath} are optional Pure paths (see
 * legend/getSetupTeardown) run immediately before/after {@code function}, within the same atomic
 * call - see {@code LegendPureSession#executeFunction(String, String, String, String)} for the
 * fail-fast/forgiving semantics of each.
 */
public class ExecuteFunctionParams
{
    private String function;
    private List<FileEntry> files;
    private String pctAdapterPath;
    private String beforeFunctionPath;
    private String afterFunctionPath;

    public ExecuteFunctionParams()
    {
    }

    public String getFunction()
    {
        return this.function;
    }

    public void setFunction(String function)
    {
        this.function = function;
    }

    public List<FileEntry> getFiles()
    {
        return this.files;
    }

    public void setFiles(List<FileEntry> files)
    {
        this.files = files;
    }

    public String getPctAdapterPath()
    {
        return this.pctAdapterPath;
    }

    public void setPctAdapterPath(String pctAdapterPath)
    {
        this.pctAdapterPath = pctAdapterPath;
    }

    public String getBeforeFunctionPath()
    {
        return this.beforeFunctionPath;
    }

    public void setBeforeFunctionPath(String beforeFunctionPath)
    {
        this.beforeFunctionPath = beforeFunctionPath;
    }

    public String getAfterFunctionPath()
    {
        return this.afterFunctionPath;
    }

    public void setAfterFunctionPath(String afterFunctionPath)
    {
        this.afterFunctionPath = afterFunctionPath;
    }
}
