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

import java.math.BigDecimal;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class FloatCoreInstance extends PrimitiveCoreInstance<BigDecimal>
{
    public static final Function<CoreInstance, BigDecimal> FROM_CORE_INSTANCE_FN = FloatCoreInstance::valueOfCoreInstance;
    public static final Function2<CoreInstance, ModelRepository, FloatCoreInstance> CONVERT_CORE_INSTANCE_FN = FloatCoreInstance::convertCoreInstance;

    private String name = null;

    FloatCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        super(value, classifier, internalSyntheticId);
    }

    @Override
    public String getName()
    {
        if (this.name == null)
        {
            this.name = getValue().toPlainString();
        }
        return this.name;
    }

    @Override
    public CoreInstance copy()
    {
        return new FloatCoreInstance(this.getValue(), this.getClassifier(), this.getSyntheticId());
    }

    public static FloatCoreInstance convertCoreInstance(CoreInstance coreInstance, ModelRepository repository)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else if (coreInstance instanceof FloatCoreInstance)
        {
            return (FloatCoreInstance) coreInstance;
        }
        else
        {
            return repository.newFloatCoreInstance(coreInstance.getName());
        }
    }

    public static BigDecimal valueOfCoreInstance(CoreInstance coreInstance)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else if (coreInstance instanceof FloatCoreInstance)
        {
            return ((FloatCoreInstance)coreInstance).getValue();
        }
        else
        {
            return new BigDecimal(coreInstance.getName());
        }
    }
}
