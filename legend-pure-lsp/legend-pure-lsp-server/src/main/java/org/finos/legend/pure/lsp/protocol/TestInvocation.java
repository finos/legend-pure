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
 * One entry of {@link ExecuteTestsParams#getInvocations()}: a function plus the {@code String[1]}
 * literals to invoke it with. {@code label} names the entry in the reported {@link TestResult},
 * since the same function is typically invoked many times with different arguments.
 */
public class TestInvocation
{
    private String path;
    private List<String> arguments;
    private String label;

    public TestInvocation()
    {
    }

    public String getPath()
    {
        return this.path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public List<String> getArguments()
    {
        return this.arguments;
    }

    public void setArguments(List<String> arguments)
    {
        this.arguments = arguments;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }
}
