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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

/**
 * Any Stub Helper
 */
public class AnyStubHelper
{

    public static final Function<CoreInstance, CoreInstance> FROM_STUB_FN = new Function<CoreInstance, CoreInstance>()
    {
        public CoreInstance valueOf(CoreInstance instance)
        {
            if (instance instanceof ImportStub)
            {
                if (((ImportStub)instance)._idOrPath() != null && ((ImportStub)instance)._resolvedNodeCoreInstance() == null)
                {
                    throw new PureCompilationException("Error, ImportStub needs to be resolved before it can be accessed");
                }
                return ((ImportStub)instance)._resolvedNodeCoreInstance();
            }
            else if (instance instanceof PropertyStub)
            {
                if (((PropertyStub)instance)._resolvedPropertyCoreInstance() == null)
                {
                    throw new PureCompilationException("Error, PropertyStub needs to be resolved before it can be accessed");
                }
                return ((PropertyStub)instance)._resolvedPropertyCoreInstance();
            }
            else if (instance instanceof EnumStub)
            {
                if (((EnumStub)instance)._resolvedEnumCoreInstance() == null)
                {
                    throw new PureCompilationException("Error, EnumStub needs to be resolved before it can be accessed");
                }
                return ((EnumStub)instance)._resolvedEnumCoreInstance();
            }
            else
            {
                return instance;
            }
        }
    };

    private AnyStubHelper()
    {
    }
}
