// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.runtime.cache;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CacheState
{
    private boolean cached;
    private boolean lastOperationSuccessful;
    private String lastStackTrace;
    private long currentCacheSize;

    private CacheState(boolean cached, long currentCacheSize, boolean lastOperationSuccessful, String lastStackTrace)
    {
        this.cached = cached;
        this.lastOperationSuccessful = lastOperationSuccessful;
        this.lastStackTrace = lastStackTrace;
        this.currentCacheSize = currentCacheSize;
    }

    public CacheState(boolean cached, long currentCacheSize, boolean lastOperationSuccessful, Throwable lastError)
    {
        this(cached, currentCacheSize, lastOperationSuccessful, getStackTrace(lastError));
    }

    public CacheState(boolean cached, long currentCacheSize, boolean lastOperationSuccessful)
    {
        this(cached, currentCacheSize, lastOperationSuccessful, (String)null);
    }

    public boolean isCached()
    {
        return this.cached;
    }

    public boolean isLastOperationSuccessful()
    {
        return this.lastOperationSuccessful;
    }

    public String getLastStackTrace()
    {
        return this.lastStackTrace;
    }

    public String getLastErrorMessage()
    {
        return (this.lastStackTrace == null) ? null : this.lastStackTrace.split(System.getProperty("line.separator"))[0];
    }

    public long getCurrentCacheSize()
    {
        return this.currentCacheSize;
    }

    public void update(boolean cached, long currentCacheSize, boolean lastOperationSuccessful, Throwable lastError)
    {
        this.cached = cached;
        this.lastOperationSuccessful = lastOperationSuccessful;
        this.lastStackTrace = getStackTrace(lastError);
        this.currentCacheSize = currentCacheSize;
    }

    private static String getStackTrace(Throwable t)
    {
        if (t == null)
        {
            return null;
        }
        else
        {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
    }
}
