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

import java.util.Collections;
import java.util.List;

public class WorkspaceDriftEvent
{
    private List<WorkspaceDriftEntry> entries;

    public WorkspaceDriftEvent()
    {
    }

    public WorkspaceDriftEvent(List<WorkspaceDriftEntry> entries)
    {
        this.entries = entries;
    }

    public static WorkspaceDriftEvent empty()
    {
        return new WorkspaceDriftEvent(Collections.emptyList());
    }

    public List<WorkspaceDriftEntry> getEntries()
    {
        return this.entries;
    }

    public void setEntries(List<WorkspaceDriftEntry> entries)
    {
        this.entries = entries;
    }
}
