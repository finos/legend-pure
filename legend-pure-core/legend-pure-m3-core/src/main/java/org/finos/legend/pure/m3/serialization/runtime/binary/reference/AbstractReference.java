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

package org.finos.legend.pure.m3.serialization.runtime.binary.reference;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public abstract class AbstractReference implements Reference
{
    private CoreInstance resolvedInstance = null;
    private String failureMessage = null;

    @Override
    public boolean resolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
    {
        if (isResolved())
        {
            return true;
        }

        synchronized (this)
        {
            if (!isResolved())
            {
                this.failureMessage = null;
                this.resolvedInstance = tryResolve(repository, processorSupport);
            }
        }
        return isResolved();
    }

    @Override
    public boolean isResolved()
    {
        return this.resolvedInstance != null;
    }

    @Override
    public CoreInstance getResolvedInstance()
    {
        return this.resolvedInstance;
    }

    @Override
    public String getFailureMessage()
    {
        return this.failureMessage;
    }

    protected void setFailureMessage(String message)
    {
        this.failureMessage = message;
    }

    protected abstract CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException;
}
