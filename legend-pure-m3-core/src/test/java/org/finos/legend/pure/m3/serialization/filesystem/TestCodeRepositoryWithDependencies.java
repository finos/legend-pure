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

package org.finos.legend.pure.m3.serialization.filesystem;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;

import java.util.regex.Pattern;

public class TestCodeRepositoryWithDependencies extends CodeRepository
{
    private final SetIterable<String> dependencies;

    public TestCodeRepositoryWithDependencies(String name, Pattern allowedPackagesPattern, Iterable<? extends CodeRepository> dependencies)
    {
        super(name, allowedPackagesPattern);
        this.dependencies = Iterate.collect(dependencies, CodeRepository::getName, Sets.mutable.empty());
    }

    public TestCodeRepositoryWithDependencies(String name, Pattern allowedPackagesPattern, CodeRepository... dependencies)
    {
        this(name, allowedPackagesPattern, ArrayAdapter.adapt(dependencies));
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        return (other == this) || ((other != null) && this.dependencies.contains(other.getName()));
    }
}
