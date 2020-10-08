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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;

public class ExecutionEndListenerState
{
    private final boolean unexpectedEndState;
    private String endListenerStateMessage;

    static final Predicate<ExecutionEndListenerState> UNEXPECTED_END_STATE = new Predicate<ExecutionEndListenerState>()
    {
        @Override
        public boolean accept(ExecutionEndListenerState executionEndListenerState)
        {
            return executionEndListenerState.unexpectedEndState;
        }
    };

    static final Function<ExecutionEndListenerState, String> TO_END_LISTENER_STATE_MESSAGE = new Function<ExecutionEndListenerState, String>()
    {
        @Override
        public String valueOf(ExecutionEndListenerState executionEndListenerState)
        {
            return executionEndListenerState.endListenerStateMessage;
        }
    };

    public ExecutionEndListenerState(boolean unexpectedEndState, String endListenerStateMessage)
    {
        this.unexpectedEndState = unexpectedEndState;
        this.endListenerStateMessage = endListenerStateMessage;
    }

    public ExecutionEndListenerState(boolean unexpectedEndState)
    {
        this.unexpectedEndState = unexpectedEndState;
    }
}
