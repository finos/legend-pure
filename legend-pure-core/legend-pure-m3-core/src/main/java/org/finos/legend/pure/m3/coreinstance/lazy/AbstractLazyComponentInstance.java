// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.coreinstance.lazy;

import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m4.ModelRepository;

public abstract class AbstractLazyComponentInstance extends AbstractLazyCoreInstance
{
    protected AbstractLazyComponentInstance(ModelRepository repository, int internalSyntheticId, String name, InstanceData instanceData, ReferenceIdResolver referenceIdResolver)
    {
        super(repository, internalSyntheticId, name, instanceData.getSourceInformation(), instanceData.getCompileStateBitSet(), instanceData.getClassifierPath(), referenceIdResolver);
    }

    protected AbstractLazyComponentInstance(AbstractLazyComponentInstance source)
    {
        super(source);
    }

    @Override
    public boolean isInitialized()
    {
        return true;
    }
}
