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

public class SyncWorkspaceResult
{
    private boolean success;
    private boolean deferred;
    private int created;
    private int modified;
    private int deleted;
    private String error;
    private String deferredReason;

    public SyncWorkspaceResult()
    {
    }

    private SyncWorkspaceResult(boolean success, int created, int modified, int deleted, String error)
    {
        this.success = success;
        this.created = created;
        this.modified = modified;
        this.deleted = deleted;
        this.error = error;
    }

    public static SyncWorkspaceResult success(int created, int modified, int deleted)
    {
        return new SyncWorkspaceResult(true, created, modified, deleted, null);
    }

    public static SyncWorkspaceResult failure(String error)
    {
        return new SyncWorkspaceResult(false, 0, 0, 0, error);
    }

    /**
     * Nothing was attempted: the graph lock is currently held (see LegendPureSession#isGraphLockHeld),
     * so nothing was applied and nothing was cleared from the drift watcher's dirty set - nothing here
     * is actually wrong, unlike {@link #failure}, hence a distinct flag rather than reusing error.
     */
    public static SyncWorkspaceResult deferred(String reason)
    {
        SyncWorkspaceResult result = new SyncWorkspaceResult(false, 0, 0, 0, null);
        result.deferred = true;
        result.deferredReason = reason;
        return result;
    }

    public boolean isDeferred()
    {
        return this.deferred;
    }

    public void setDeferred(boolean deferred)
    {
        this.deferred = deferred;
    }

    public String getDeferredReason()
    {
        return this.deferredReason;
    }

    public void setDeferredReason(String deferredReason)
    {
        this.deferredReason = deferredReason;
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public int getCreated()
    {
        return this.created;
    }

    public void setCreated(int created)
    {
        this.created = created;
    }

    public int getModified()
    {
        return this.modified;
    }

    public void setModified(int modified)
    {
        this.modified = modified;
    }

    public int getDeleted()
    {
        return this.deleted;
    }

    public void setDeleted(int deleted)
    {
        this.deleted = deleted;
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
