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
 */
public class ExecuteFunctionParams
{
    private String function;
    private List<FileEntry> files;

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
}
