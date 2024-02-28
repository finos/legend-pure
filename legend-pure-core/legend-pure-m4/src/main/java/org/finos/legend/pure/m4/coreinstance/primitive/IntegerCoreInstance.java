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

import java.math.BigInteger;

import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class IntegerCoreInstance extends PrimitiveCoreInstance<Number>
{
    public static final Function<CoreInstance, Number> FROM_CORE_INSTANCE_FN = new Function<CoreInstance, Number>()
    {
        public Number valueOf(CoreInstance coreInstance)
        {
            return coreInstance == null ? null : ((IntegerCoreInstance)coreInstance).getValue();
        }
    };

    private String name = null;

    private IntegerCoreInstance(Number value, CoreInstance classifier, int internalSyntheticId)
    {
        super(value, classifier, internalSyntheticId);
    }

    IntegerCoreInstance(Integer value, CoreInstance classifier, int internalSyntheticId)
    {
        this((Number)value, classifier, internalSyntheticId);
    }

    IntegerCoreInstance(Long value, CoreInstance classifier, int internalSyntheticId)
    {
        this((Number)value, classifier, internalSyntheticId);
    }

    IntegerCoreInstance(BigInteger value, CoreInstance classifier, int internalSyntheticId)
    {
        this((Number)value, classifier, internalSyntheticId);
    }

    @Override
    public String getName()
    {
        if (this.name == null)
        {
            this.name = getValue().toString();
        }
        return this.name;
    }

    @Override
    public CoreInstance copy()
    {
        return new IntegerCoreInstance(this.getValue(), this.getClassifier(), this.getSyntheticId());
    }
}
