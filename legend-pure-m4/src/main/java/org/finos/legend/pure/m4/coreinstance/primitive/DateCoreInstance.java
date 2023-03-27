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
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

public final class DateCoreInstance extends PrimitiveCoreInstance<PureDate>
{
    public static final Function<CoreInstance, PureDate> FROM_CORE_INSTANCE_FN = DateCoreInstance::valueOfCoreInstance;
    public static final Function2<CoreInstance, ModelRepository, DateCoreInstance> CONVERT_CORE_INSTANCE_FN = DateCoreInstance::convertCoreInstance;

    private String name = null;

    DateCoreInstance(PureDate value, CoreInstance classifier, int internalSyntheticId)
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
        return new DateCoreInstance(this.getValue(), this.getClassifier(), this.getSyntheticId());
    }

    public static DateCoreInstance convertCoreInstance(CoreInstance coreInstance, ModelRepository repository)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else if (coreInstance instanceof DateCoreInstance)
        {
            return (DateCoreInstance) coreInstance;
        }
        else
        {
            return repository.newDateCoreInstance(coreInstance.getName());
        }
    }

    public static PureDate valueOfCoreInstance(CoreInstance coreInstance)
    {
        if (coreInstance == null)
        {
            return null;
        }
        else if (coreInstance instanceof DateCoreInstance)
        {
            return ((DateCoreInstance)coreInstance).getValue();
        }
        else
        {
            return DateFunctions.parsePureDate(coreInstance.getName());
        }
    }
}
