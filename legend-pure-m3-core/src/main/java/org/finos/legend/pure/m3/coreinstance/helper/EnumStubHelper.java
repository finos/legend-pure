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

package org.finos.legend.pure.m3.coreinstance.helper;

import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class EnumStubHelper
{
    public static final Function<CoreInstance, CoreInstance> FROM_STUB_FN = EnumStubHelper::fromEnumStub;

    public static CoreInstance fromEnumStub(CoreInstance instance)
    {
        return (instance instanceof EnumStub) ? fromEnumStub((EnumStub) instance) : instance;
    }

    public static CoreInstance fromEnumStub(EnumStub enumStub)
    {
        if (enumStub._resolvedEnumCoreInstance() == null)
        {
            throw new PureCompilationException("Error, EnumStub needs to be resolved before it can be accessed");
        }
        return enumStub._resolvedEnumCoreInstance();
    }

    public static boolean isUnresolved(EnumStub enumStub)
    {
        return enumStub._resolvedEnum() == null;
    }
}