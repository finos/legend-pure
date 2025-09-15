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

package org.finos.legend.pure.m4.coreinstance.primitive.simple;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;

import java.math.BigDecimal;

class SimpleFloatCoreInstance extends AbstractSimplePrimitiveCoreInstance<BigDecimal> implements FloatCoreInstance
{
    private String name = null;

    SimpleFloatCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
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
    public FloatCoreInstance copy()
    {
        return new SimpleFloatCoreInstance(getValue(), getClassifier(), getSyntheticId());
    }
}
