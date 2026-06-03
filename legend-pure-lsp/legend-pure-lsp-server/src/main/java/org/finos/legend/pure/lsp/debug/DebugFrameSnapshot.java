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

package org.finos.legend.pure.lsp.debug;

import org.finos.legend.pure.runtime.java.interpreted.VariableContext;

class DebugFrameSnapshot
{
    private final int id;
    private final int variablesReference;
    private final String name;
    private final DebugExecutionLocation location;
    private final VariableContext variableContext;

    DebugFrameSnapshot(int id, int variablesReference, String name, DebugExecutionLocation location,
                       VariableContext variableContext)
    {
        this.id = id;
        this.variablesReference = variablesReference;
        this.name = (name == null || name.isEmpty()) ? "Pure frame" : name;
        this.location = location;
        this.variableContext = variableContext;
    }

    int getId()
    {
        return this.id;
    }

    int getVariablesReference()
    {
        return this.variablesReference;
    }

    String getName()
    {
        return this.name;
    }

    DebugExecutionLocation getLocation()
    {
        return this.location;
    }

    VariableContext getVariableContext()
    {
        return this.variableContext;
    }
}
