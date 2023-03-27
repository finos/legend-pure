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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;

public final class StrictTimeCoreInstance extends PrimitiveCoreInstance<PureStrictTime>
{
    public static final Function<CoreInstance, PureStrictTime> FROM_CORE_INSTANCE_FN = StrictTimeCoreInstance::valueOfCoreInstance;
    public static final Function2<CoreInstance, ModelRepository, StrictTimeCoreInstance> CONVERT_CORE_INSTANCE_FN = StrictTimeCoreInstance::convertCoreInstance;

    private String name = null;

    StrictTimeCoreInstance(PureStrictTime value, CoreInstance classifier, int internalSyntheticId)
    {
        super(value, classifier, internalSyntheticId);
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
        return new StrictTimeCoreInstance(this.getValue(), this.getClassifier(), this.getSyntheticId());
    }

    public static StrictTimeCoreInstance convertCoreInstance(CoreInstance coreInstance, ModelRepository repository)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else if (coreInstance instanceof StrictTimeCoreInstance)
        {
            return (StrictTimeCoreInstance) coreInstance;
        }
        else
        {
            return repository.newStrictTimeCoreInstance(coreInstance.getName());
        }
    }

    public static PureStrictTime valueOfCoreInstance(CoreInstance coreInstance)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else if (coreInstance instanceof StrictTimeCoreInstance)
        {
            return ((StrictTimeCoreInstance) coreInstance).getValue();
        }
        else
        {
            return StrictTimeFunctions.parsePureStrictTime(coreInstance.getName());
        }
    }
}
