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

package org.finos.legend.pure.m4.coreinstance.primitive;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public final class BooleanCoreInstance extends PrimitiveCoreInstance<Boolean>
{
    public static final Function<CoreInstance, Boolean> FROM_CORE_INSTANCE_FN = new Function<CoreInstance, Boolean>()
    {
        public Boolean valueOf(CoreInstance coreInstance)
        {
            if (coreInstance == null)
            {
                return null;
            }
            else if (coreInstance instanceof BooleanCoreInstance)
            {
                return ((BooleanCoreInstance)coreInstance).getValue();
            }
            else
            {
                return ModelRepository.BOOLEAN_TRUE.equals(coreInstance.getName());
            }
        }
    };

    public static final Function2<CoreInstance, ModelRepository, BooleanCoreInstance> CAST_CORE_INSTANCE_FN = new Function2<CoreInstance, ModelRepository, BooleanCoreInstance>()
    {
        public BooleanCoreInstance value(CoreInstance coreInstance, ModelRepository repository)
        {
            if (coreInstance == null)
            {
                return null;
            }
            else if (coreInstance instanceof BooleanCoreInstance)
            {
                return (BooleanCoreInstance) coreInstance;
            }
            else
            {
                return repository.newBooleanCoreInstance(coreInstance.getName());
            }
        }
    };

    BooleanCoreInstance(Boolean value, CoreInstance classifier, int internalSyntheticId)
    {
        super(value, classifier, internalSyntheticId);
    }

    @Override
    public String getName()
    {
        return getValue() ? ModelRepository.BOOLEAN_TRUE : ModelRepository.BOOLEAN_FALSE;
    }

    @Override
    public CoreInstance copy()
    {
        return new BooleanCoreInstance(this.getValue(), this.getClassifier(), this.getSyntheticId());
    }
}
