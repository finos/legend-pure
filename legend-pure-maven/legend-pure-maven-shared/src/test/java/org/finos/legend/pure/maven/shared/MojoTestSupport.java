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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.ProjectDependenciesResolver;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared test utilities for Maven mojo unit tests.
 *
 * <p>Provides the three helpers that appear in every mojo test class:
 * <ul>
 *   <li>{@link #setField} — reflection-based field injection (bypasses Maven DI)</li>
 *   <li>{@link #executionWithPhase} — builds a {@link MojoExecution} for a given lifecycle phase</li>
 *   <li>{@link #EMPTY_RESOLVER} — a {@link ProjectDependenciesResolver} stub that returns no dependencies</li>
 * </ul>
 * </p>
 */
public final class MojoTestSupport
{
    private MojoTestSupport()
    {
    }

    /**
     * Injects {@code value} into the named field of {@code target} via reflection,
     * walking the class hierarchy until the field is found.
     */
    public static void setField(Object target, String fieldName, Object value) throws Exception
    {
        Class<?> clazz = target.getClass();
        while (clazz != null)
        {
            try
            {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            }
            catch (NoSuchFieldException e)
            {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + target.getClass());
    }

    /**
     * Creates a {@link MojoExecution} with the given lifecycle phase set.
     */
    public static MojoExecution executionWithPhase(String phase)
    {
        MojoExecution execution = new MojoExecution(new MojoDescriptor(), "test-id");
        execution.setLifecyclePhase(phase);
        return execution;
    }

    /**
     * A {@link ProjectDependenciesResolver} stub that returns empty dependency lists.
     * Use this when testing mojos that call {@code getDependencyURLs()} — the parent
     * classloader (the test JVM) already carries the required repositories.
     */
    public static final ProjectDependenciesResolver EMPTY_RESOLVER = request ->
    {
        return new DependencyResolutionResult()
        {
            @Override
            public org.eclipse.aether.graph.DependencyNode getDependencyGraph()
            {
                return null;
            }

            @Override
            public java.util.List<org.eclipse.aether.graph.Dependency> getDependencies()
            {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<org.eclipse.aether.graph.Dependency> getResolvedDependencies()
            {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<org.eclipse.aether.graph.Dependency> getUnresolvedDependencies()
            {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<Exception> getCollectionErrors()
            {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<Exception> getResolutionErrors(org.eclipse.aether.graph.Dependency dependency)
            {
                return Collections.emptyList();
            }
        };
    };

    /**
     * Convenience factory for a small {@link Set} from varargs, matching the
     * {@code setOf()} helper used across mojo test classes.
     */
    public static Set<String> setOf(String... values)
    {
        Set<String> set = new HashSet<>();
        for (String v : values)
        {
            set.add(v);
        }
        return set;
    }
}

