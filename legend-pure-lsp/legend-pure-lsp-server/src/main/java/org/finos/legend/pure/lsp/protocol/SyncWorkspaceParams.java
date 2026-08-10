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
 * {@code uris} null or empty means "sync everything currently known to be dirty" (the watcher's full
 * accumulated set); a non-empty list syncs only those specific files, letting a client offer the user a
 * checkbox list rather than an all-or-nothing sync.
 */
public class SyncWorkspaceParams
{
    private List<String> uris;

    public SyncWorkspaceParams()
    {
    }

    public SyncWorkspaceParams(List<String> uris)
    {
        this.uris = uris;
    }

    public List<String> getUris()
    {
        return this.uris;
    }

    public void setUris(List<String> uris)
    {
        this.uris = uris;
    }
}
