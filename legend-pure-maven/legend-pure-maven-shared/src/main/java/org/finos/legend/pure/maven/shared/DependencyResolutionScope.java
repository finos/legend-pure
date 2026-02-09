// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.maven.shared;

import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Scopes for resolving the dependencies of a Maven module.
 */
public enum DependencyResolutionScope
{
    COMPILE_RESOLUTION_SCOPE("compile", asList("compile", "provided", "system")),
    COMPILE_RUNTIME_RESOLUTION_SCOPE("compile+runtime", asList("compile", "provided", "runtime", "system")),
    RUNTIME_RESOLUTION_SCOPE("runtime", asList("compile", "runtime")),
    RUNTIME_SYSTEM_RESOLUTION_SCOPE("runtime+system", asList("compile", "runtime", "system")),
    TEST_RESOLUTION_SCOPE("test", null, true);

    private final String name;
    private final ScopeDependencyFilter scopeDependencyFilter;
    private final boolean isTestScope;

    DependencyResolutionScope(String name, List<String> filters)
    {
        this(name, filters, false);
    }

    DependencyResolutionScope(String name, List<String> filters, boolean isTestScope)
    {
        this.name = Objects.requireNonNull(name);
        this.scopeDependencyFilter = filters != null ? new ScopeDependencyFilter(filters, null) : null;
        this.isTestScope = isTestScope;
    }

    public ScopeDependencyFilter getScopeDependencyFilter()
    {
        return this.scopeDependencyFilter;
    }

    public boolean isTestScope()
    {
        return this.isTestScope;
    }

    public String getName()
    {
        return name;
    }

    public static DependencyResolutionScope fromName(String name)
    {
        for (DependencyResolutionScope scope: DependencyResolutionScope.values())
        {
            if (scope.getName().equalsIgnoreCase(name))
            {
                return scope;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown dependency resolution scope: '%s'; valid values: %s",
                name,
                Arrays.stream(DependencyResolutionScope.values()).map(DependencyResolutionScope::getName).collect(Collectors.joining(", ", "[", "]")))
        );
    }
}
