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

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;

/**
 * An exception raised when something goes wrong during Pure execution.
 */
public class PureExecutionException extends PureException
{
    public PureExecutionException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(sourceInformation, info, cause);
    }

    public PureExecutionException(SourceInformation sourceInformation, String info)
    {
        super(sourceInformation, info, null);
    }

    public PureExecutionException(SourceInformation sourceInformation, Throwable cause)
    {
        super(sourceInformation, null, cause);
    }

    public PureExecutionException(String info, Throwable cause)
    {
        super(info, cause);
    }

    public PureExecutionException(SourceInformation sourceInformation)
    {
        super(sourceInformation);
    }

    public PureExecutionException(String info)
    {
        super(info);
    }

    public PureExecutionException(Throwable cause)
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
