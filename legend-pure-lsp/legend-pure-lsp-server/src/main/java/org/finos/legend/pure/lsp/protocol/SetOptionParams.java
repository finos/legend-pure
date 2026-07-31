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
 * Params for legend/setOption. Sets or clears a Pure runtime option so isOptionSet('&lt;name&gt;')
 * reflects it live in this JVM, without a restart.
 * <p>
 * The Pure runtime resolves options via RuntimeOptions.systemPropertyOptions("pure.options."),
 * i.e. isOptionSet("X") == Boolean.getBoolean("pure.options.X"), read on every call. So this handler
 * maps to a System.setProperty("pure.options." + name, "true") when value is true, and a
 * System.clearProperty("pure.options." + name) when value is false (clearing, not "false", so
 * Boolean.getBoolean returns false either way but the property does not linger).
 */
public class SetOptionParams
{
    private String name;
    private boolean value;

    public SetOptionParams()
    {
    }

    public SetOptionParams(String name, boolean value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isValue()
    {
        return this.value;
    }

    public void setValue(boolean value)
    {
        this.value = value;
    }
}
