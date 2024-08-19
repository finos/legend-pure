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

package org.finos.legend.pure.runtime.java.shared.listeners;

public class ExecutionEndListenerState
{
    private final boolean unexpected;
    private final String message;
    private final Throwable throwable;

    private ExecutionEndListenerState(boolean unexpected, String message, Throwable throwable)
    {
        this.unexpected = unexpected;
        this.message = message;
        this.throwable = throwable;
    }

    public ExecutionEndListenerState(boolean unexpected, String message)
    {
        this(unexpected, message, null);
    }

    public ExecutionEndListenerState(boolean unexpected)
    {
        this(unexpected, null, null);
    }

    public ExecutionEndListenerState(Throwable throwable)
    {
        this((throwable != null), (throwable == null) ? null : throwable.getMessage(), throwable);
    }

    public boolean isUnexpectedEnd()
    {
        return this.unexpected;
    }

    public boolean hasMessage()
    {
        return this.message != null;
    }

    public String getMessage()
    {
        return this.message;
    }

    public boolean hasThrowable()
    {
        return this.throwable != null;
    }

    public Throwable getThrowable()
    {
        return this.throwable;
    }
}
