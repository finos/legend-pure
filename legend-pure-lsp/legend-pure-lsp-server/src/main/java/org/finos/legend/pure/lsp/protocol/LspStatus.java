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

public class LspStatus
{
    private String state;
    private int repositoryCount;
    private int symbolCount;
    private int recoveryAttempts;
    private boolean recoveryInProgress;
    private String message;

    public LspStatus()
    {
    }

    public LspStatus(LspState state, int repositoryCount, int symbolCount, int recoveryAttempts,
                     boolean recoveryInProgress, String message)
    {
        this.state = state.getProtocolValue();
        this.repositoryCount = repositoryCount;
        this.symbolCount = symbolCount;
        this.recoveryAttempts = recoveryAttempts;
        this.recoveryInProgress = recoveryInProgress;
        this.message = message;
    }

    public String getState()
    {
        return this.state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public int getRepositoryCount()
    {
        return this.repositoryCount;
    }

    public void setRepositoryCount(int repositoryCount)
    {
        this.repositoryCount = repositoryCount;
    }

    public int getSymbolCount()
    {
        return this.symbolCount;
    }

    public void setSymbolCount(int symbolCount)
    {
        this.symbolCount = symbolCount;
    }

    public int getRecoveryAttempts()
    {
        return this.recoveryAttempts;
    }

    public void setRecoveryAttempts(int recoveryAttempts)
    {
        this.recoveryAttempts = recoveryAttempts;
    }

    public boolean isRecoveryInProgress()
    {
        return this.recoveryInProgress;
    }

    public void setRecoveryInProgress(boolean recoveryInProgress)
    {
        this.recoveryInProgress = recoveryInProgress;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
