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

package org.finos.legend.pure.m3.serialization.compiler.reference.v1;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdExtension;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.Function;

public class ReferenceIdExtensionV1 implements ReferenceIdExtension
{
    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public ReferenceIdProvider newProvider(ProcessorSupport processorSupport)
    {
        return new ReferenceIdProviderV1(processorSupport);
    }

    @Override
    public ReferenceIdResolver newResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        return new ReferenceIdResolverV1(packagePathResolver);
    }
}
