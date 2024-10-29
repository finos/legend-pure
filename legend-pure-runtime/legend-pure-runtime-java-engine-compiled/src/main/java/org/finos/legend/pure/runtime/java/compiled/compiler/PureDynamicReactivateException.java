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

package org.finos.legend.pure.runtime.java.compiled.compiler;

import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class PureDynamicReactivateException extends PureExecutionException
{
    public PureDynamicReactivateException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(sourceInformation, info, cause, Stacks.mutable.empty());
    }

    public PureDynamicReactivateException(SourceInformation sourceInformation, String info)
    {
        super(sourceInformation, info, null);
    }

    public PureDynamicReactivateException(SourceInformation sourceInformation, Throwable cause)
    {
        super(sourceInformation, null, cause, Stacks.mutable.empty());
    }

    public PureDynamicReactivateException(String info, Throwable cause)
    {
        super(info, cause, Stacks.mutable.empty());
    }

    public PureDynamicReactivateException(SourceInformation sourceInformation)
    {
        super(sourceInformation, Stacks.mutable.empty());
    }

    public PureDynamicReactivateException(String info)
    {
        super(info, Stacks.mutable.empty());
    }

    public PureDynamicReactivateException(Throwable cause)
    {
        super(cause, Stacks.mutable.empty());
    }

    public PureDynamicReactivateException()
    {
        super();
    }
}
