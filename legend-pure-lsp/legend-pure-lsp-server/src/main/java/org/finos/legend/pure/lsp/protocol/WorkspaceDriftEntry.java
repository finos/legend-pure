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

public class WorkspaceDriftEntry
{
    private String uri;
    private String changeType;

    public WorkspaceDriftEntry()
    {
    }

    public WorkspaceDriftEntry(String uri, DriftChangeType changeType)
    {
        this.uri = uri;
        this.changeType = changeType.getProtocolValue();
    }

    public String getUri()
    {
        return this.uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public String getChangeType()
    {
        return this.changeType;
    }

    public void setChangeType(String changeType)
    {
        this.changeType = changeType;
    }
}
