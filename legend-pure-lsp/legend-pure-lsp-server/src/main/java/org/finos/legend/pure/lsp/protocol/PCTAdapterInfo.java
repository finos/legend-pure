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
 * One adapter found by legend/getPCTAdapters: a Function in the compiled graph carrying the
 * &lt;&lt;PCT.adapter&gt;&gt; stereotype (see org.finos.legend.pure.m3.pct.shared.PCTTools#PCT_PROFILE).
 * {@code name} is its 'adapterName' tagged value, falling back to the element's own simple name when
 * absent; {@code path} is the fully-qualified Pure path, suitable for
 * ExecuteFunctionParams#pctAdapterPath.
 */
public class PCTAdapterInfo
{
    private String name;
    private String path;

    public PCTAdapterInfo()
    {
    }

    public PCTAdapterInfo(String name, String path)
    {
        this.name = name;
        this.path = path;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPath()
    {
        return this.path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
}
