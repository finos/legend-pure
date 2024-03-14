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

package org.finos.legend.pure.m3.serialization.grammar;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;

public interface CoreInstanceFactoriesRegistry
{
    Function<CoreInstanceFactoriesRegistry, RichIterable<CoreInstanceFactoryRegistry>> CORE_INSTANCE_FACTORY_GETTOR = new Function<CoreInstanceFactoriesRegistry, RichIterable<CoreInstanceFactoryRegistry>>()
    {
        @Override
        public RichIterable<CoreInstanceFactoryRegistry> valueOf(CoreInstanceFactoriesRegistry registry)
        {
            return registry.getCoreInstanceFactoriesRegistry();
        }
    };

    RichIterable<CoreInstanceFactoryRegistry> getCoreInstanceFactoriesRegistry();
}
