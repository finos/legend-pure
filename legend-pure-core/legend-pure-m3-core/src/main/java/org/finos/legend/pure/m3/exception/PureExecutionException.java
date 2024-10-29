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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;

/**
 * An exception raised when something goes wrong during Pure execution.
 */
public class PureExecutionException extends PureException
{
    private MutableStack<CoreInstance> callStack;

    public PureExecutionException(SourceInformation sourceInformation, String info, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, info, cause);
    }

    public PureExecutionException(SourceInformation sourceInformation, String info, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, info, null);
    }

    public PureExecutionException(SourceInformation sourceInformation, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, null, cause);
    }

    public PureExecutionException(String info, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(info, cause);
    }

    public PureExecutionException(SourceInformation sourceInformation, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation);
    }

    public PureExecutionException(String info, MutableStack<CoreInstance> callStack)
    {
        super(info);
    }

    public PureExecutionException(Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(cause);
    }

    public PureExecutionException()
    {
        super();
    }

    @Override
    public String getExceptionName()
    {
        return "Execution error";
    }
}
