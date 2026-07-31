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
 * Params for legend/deleteFile. Unloads a .pure source from the running session: removes it from the
 * open-document set, deletes it from the Pure runtime, and clears any overlay content. Unlike a
 * workspace/didChangeWatchedFiles Deleted event, this is NOT filtered out for currently-open
 * documents, so it reliably removes a file the bridge pushed via didOpen (e.g. a throwaway go()
 * wrapper) - the fix for orphan 'go__Any_MANY_ is defined more than once' overlays.
 * <p>
 * Accepts either a file:// uri or a raw sourceId (leading '/'); the handler resolves whichever form
 * is given.
 */
public class DeleteFileParams
{
    private String uri;

    public DeleteFileParams()
    {
    }

    public DeleteFileParams(String uri)
    {
        this.uri = uri;
    }

    public String getUri()
    {
        return this.uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }
}
