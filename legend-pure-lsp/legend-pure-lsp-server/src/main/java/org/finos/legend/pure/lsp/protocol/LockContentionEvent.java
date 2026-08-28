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

public class LockContentionEvent
{
    private boolean active;
    private String lockType;
    private String reason;
    private int pendingCount;

    public LockContentionEvent()
    {
    }

    public LockContentionEvent(boolean active, String lockType, String reason, int pendingCount)
    {
        this.active = active;
        this.lockType = lockType;
        this.reason = reason;
        this.pendingCount = pendingCount;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public String getLockType()
    {
        return this.lockType;
    }

    public void setLockType(String lockType)
    {
        this.lockType = lockType;
    }

    public String getReason()
    {
        return this.reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public int getPendingCount()
    {
        return this.pendingCount;
    }

    public void setPendingCount(int pendingCount)
    {
        this.pendingCount = pendingCount;
    }
}
