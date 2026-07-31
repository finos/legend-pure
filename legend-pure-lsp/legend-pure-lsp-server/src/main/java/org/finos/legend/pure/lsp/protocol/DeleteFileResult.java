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
 * Result of legend/deleteFile. {@link #isRemoved()} is true when the source was actually present and
 * has been unloaded (false when it was not in the session to begin with - not an error). {@code error}
 * is populated only when the delete + recompile failed.
 */
public class DeleteFileResult
{
    private boolean success;
    private String sourceId;
    private boolean removed;
    private String error;

    public DeleteFileResult()
    {
    }

    public DeleteFileResult(boolean success, String sourceId, boolean removed, String error)
    {
        this.success = success;
        this.sourceId = sourceId;
        this.removed = removed;
        this.error = error;
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getSourceId()
    {
        return this.sourceId;
    }

    public void setSourceId(String sourceId)
    {
        this.sourceId = sourceId;
    }

    public boolean isRemoved()
    {
        return this.removed;
    }

    public void setRemoved(boolean removed)
    {
        this.removed = removed;
    }

    public String getError()
    {
        return this.error;
    }

    public void setError(String error)
    {
        this.error = error;
    }
}
