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

package org.finos.legend.pure.m3.serialization.compiler.reference;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractReferenceIdResolver implements ReferenceIdResolver
{
    protected final Function<? super String, ? extends CoreInstance> packagePathResolver;

    protected AbstractReferenceIdResolver(Function<? super String, ? extends CoreInstance> packagePathResolver)
    {
        this.packagePathResolver = Objects.requireNonNull(packagePathResolver);
    }

    @Override
    public CoreInstance resolvePackagePath(String packagePath)
    {
        return this.packagePathResolver.apply(Objects.requireNonNull(packagePath));
    }
}
