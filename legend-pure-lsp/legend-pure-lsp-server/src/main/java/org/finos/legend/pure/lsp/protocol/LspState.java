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

public enum LspState
{
    CREATED("created"),
    INITIALIZING("initializing"),
    READY("ready"),
    REINDEXING("reindexing"),
    RECOVERING("recovering"),
    DEGRADED("degraded"),
    FAILED("failed"),
    SHUTDOWN("shutdown");

    private final String protocolValue;

    LspState(String protocolValue)
    {
        this.protocolValue = protocolValue;
    }

    public String getProtocolValue()
    {
        return this.protocolValue;
    }
}
