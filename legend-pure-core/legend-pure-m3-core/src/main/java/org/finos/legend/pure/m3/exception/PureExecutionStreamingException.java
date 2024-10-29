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

package org.finos.legend.pure.m3.exception;

import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * Indicates that an exception was thrown while we were writing to the response
 * during lazy streaming execution
 *
 * This separate exception is provide, because we have already written data to the response,
 * so the error handling needs to be different
 */
public class PureExecutionStreamingException extends PureExecutionException
{
    public PureExecutionStreamingException(Throwable cause, MutableStack<CoreInstance> callStack)
        {
            super(cause, callStack);
        }
}
