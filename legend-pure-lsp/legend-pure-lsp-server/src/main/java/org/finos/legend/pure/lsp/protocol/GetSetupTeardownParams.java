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
 * Params for legend/getSetupTeardown: {@code functionPath} is the Pure path of a test function
 * (typically {@link TestFunctionInfo#getFunctionPath()}) to find the nearest Before/After-package
 * functions for.
 */
public class GetSetupTeardownParams
{
    private String functionPath;

    public GetSetupTeardownParams()
    {
    }

    public GetSetupTeardownParams(String functionPath)
    {
        this.functionPath = functionPath;
    }

    public String getFunctionPath()
    {
        return this.functionPath;
    }

    public void setFunctionPath(String functionPath)
    {
        this.functionPath = functionPath;
    }
}
