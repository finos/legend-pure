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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class StringCoreInstance extends PrimitiveCoreInstance<String>
{
    public static final Function<CoreInstance, String> FROM_CORE_INSTANCE_FN = StringCoreInstance::fromCoreInstance;

    StringCoreInstance(String value, CoreInstance classifier, int internalSyntheticId)
    {
        super(value, classifier, internalSyntheticId);
    }

    @Override
    public String getName()
    {
        return getValue();
    }

    @Override
    public CoreInstance copy()
    {
        return new StringCoreInstance(getValue(), getClassifier(), getSyntheticId());
    }

    public static String fromCoreInstance(CoreInstance instance)
    {
        return (instance == null) ? null : ((StringCoreInstance) instance).getValue();
    }
}
