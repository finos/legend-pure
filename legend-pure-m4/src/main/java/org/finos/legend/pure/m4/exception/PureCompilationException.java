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

package org.finos.legend.pure.m4.exception;

import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * An exception raised when something goes wrong during Pure compilation.
 */
public class PureCompilationException extends PureException
{
    public PureCompilationException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(sourceInformation, info, cause);
    }

    public PureCompilationException(SourceInformation sourceInformation, String info)
    {
        super(sourceInformation, info);
    }

    public PureCompilationException(SourceInformation sourceInformation, Throwable cause)
    {
        super(sourceInformation, cause);
    }

    public PureCompilationException(String info, Throwable cause)
    {
        super(info, cause);
    }

    public PureCompilationException(SourceInformation sourceInformation)
    {
        super(sourceInformation);
    }

    public PureCompilationException(String info)
    {
        super(info);
    }

    public PureCompilationException(Throwable cause)
    {
        super(cause);
    }

    public PureCompilationException()
    {
        super();
    }

    @Override
    public String getExceptionName()
    {
        return "Compilation error";
    }
}
