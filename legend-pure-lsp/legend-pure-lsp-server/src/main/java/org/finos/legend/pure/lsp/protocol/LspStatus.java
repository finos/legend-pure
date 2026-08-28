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

public class LspStatus
{
    private String state;
    private int repositoryCount;
    private int symbolCount;
    private int recoveryAttempts;
    private boolean recoveryInProgress;
    private String message;
    private int compiledRepositories;
    private int totalRepositories;

    // Observability fields, filled in by LegendPureLspServer#status() on top of whatever
    // PureRuntimeManager#currentStatus() already built - see that method for why these live here
    // rather than being threaded through PureRuntimeManager itself (they describe the server/session,
    // not compile progress).
    private int connectedClientCount;
    private int port = -1;
    private String transport;
    private int requestPoolSize;
    private List<String> repoRoots = Collections.emptyList();
    private List<String> jvmArgs = Collections.emptyList();
    private List<LogErrorEntry> recentErrors = Collections.emptyList();
    private boolean lockContended;
    private String lockContentionReason;

    public LspStatus()
    {
    }

    public LspStatus(LspState state, int repositoryCount, int symbolCount, int recoveryAttempts,
                     boolean recoveryInProgress, String message)
    {
        this(state, repositoryCount, symbolCount, recoveryAttempts, recoveryInProgress, message, 0, 0);
    }

    public LspStatus(LspState state, int repositoryCount, int symbolCount, int recoveryAttempts,
                     boolean recoveryInProgress, String message, int compiledRepositories, int totalRepositories)
    {
        this.state = state.getProtocolValue();
        this.repositoryCount = repositoryCount;
        this.symbolCount = symbolCount;
        this.recoveryAttempts = recoveryAttempts;
        this.recoveryInProgress = recoveryInProgress;
        this.message = message;
        this.compiledRepositories = compiledRepositories;
        this.totalRepositories = totalRepositories;
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

    public int getCompiledRepositories()
    {
        return this.compiledRepositories;
    }

    public void setCompiledRepositories(int compiledRepositories)
    {
        this.compiledRepositories = compiledRepositories;
    }

    public int getTotalRepositories()
    {
        return this.totalRepositories;
    }

    public void setTotalRepositories(int totalRepositories)
    {
        this.totalRepositories = totalRepositories;
    }

    public int getConnectedClientCount()
    {
        return this.connectedClientCount;
    }

    public void setConnectedClientCount(int connectedClientCount)
    {
        this.connectedClientCount = connectedClientCount;
    }

    public int getPort()
    {
        return this.port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getTransport()
    {
        return this.transport;
    }

    public void setTransport(String transport)
    {
        this.transport = transport;
    }

    public int getRequestPoolSize()
    {
        return this.requestPoolSize;
    }

    public void setRequestPoolSize(int requestPoolSize)
    {
        this.requestPoolSize = requestPoolSize;
    }

    public List<String> getRepoRoots()
    {
        return this.repoRoots;
    }

    public void setRepoRoots(List<String> repoRoots)
    {
        this.repoRoots = repoRoots == null ? Collections.emptyList() : repoRoots;
    }

    public List<String> getJvmArgs()
    {
        return this.jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs)
    {
        this.jvmArgs = jvmArgs == null ? Collections.emptyList() : jvmArgs;
    }

    public List<LogErrorEntry> getRecentErrors()
    {
        return this.recentErrors;
    }

    public void setRecentErrors(List<LogErrorEntry> recentErrors)
    {
        this.recentErrors = recentErrors == null ? Collections.emptyList() : recentErrors;
    }

    public boolean isLockContended()
    {
        return this.lockContended;
    }

    public void setLockContended(boolean lockContended)
    {
        this.lockContended = lockContended;
    }

    public String getLockContentionReason()
    {
        return this.lockContentionReason;
    }

    public void setLockContentionReason(String lockContentionReason)
    {
        this.lockContentionReason = lockContentionReason;
    }
}
