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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class CodeRepository
{
    private static final Pattern VALID_REPO_NAME_PATTERN = Pattern.compile("[a-z]++(_[a-z]++)*+");

    private final String name;
    private final Pattern allowedPackagesPattern;

    protected CodeRepository(String name, Pattern allowedPackagesPattern)
    {
        if (!isValidRepositoryName(name))
        {
            throw new IllegalArgumentException("Invalid repository name: " + name);
        }
        this.name = name;
        this.allowedPackagesPattern = allowedPackagesPattern;
    }

    public String getName()
    {
        return this.name;
    }

    public Pattern getAllowedPackagesPattern()
    {
        return this.allowedPackagesPattern;
    }

    public boolean isPackageAllowed(String pkg)
    {
        return (this.allowedPackagesPattern == null) || this.allowedPackagesPattern.matcher(pkg).matches();
    }

    /**
     * Return whether the other repository is visible to this
     * repository.  That is, is code in this repository
     * allowed to reference code in the other repository.
     *
     * @param other other code repository
     * @return whether other is visible to this
     */
    abstract public boolean isVisible(CodeRepository other);

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other == null || this.getClass() != other.getClass())
        {
            return false;
        }

        return this.name.equals(((CodeRepository) other).getName());
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public String toString()
    {
        return "<" + getClass().getSimpleName() + " \"" + this.name + "\">";
    }

    protected boolean isValidRepositoryName(String name)
    {
        return VALID_REPO_NAME_PATTERN.matcher(name).matches();
    }

    public static CodeRepository newPlatformCodeRepository()
    {
        return new PlatformCodeRepository();
    }

    public static CodeRepository newScratchCodeRepository()
    {
        return new ScratchCodeRepository();
    }

    /**
     * Produce a list of repositories sorted by visibility. In the resulting list, is one repository is visible to
     * another, then the visible repository will be earlier in the list. E.g., if repository A is visible to B, then A
     * will appear before B in the list. Two repositories which are not visible to each other may appear in any order.
     * An exception will be thrown if a visibility loop is detected. The result will be a new list, and othe original
     * will not be modified.
     *
     * @param repositories repositories to sort
     * @param <T>          repository type
     * @return list of repositories sorted by visibility
     */
    public static <T extends CodeRepository> MutableList<T> toSortedRepositoryList(Iterable<T> repositories)
    {
        if (repositories instanceof ListIterable)
        {
            return toSortedRepositoryList((ListIterable<T>) repositories);
        }
        if (repositories instanceof List)
        {
            return toSortedRepositoryList(ListAdapter.adapt((List<T>) repositories));
        }
        return toSortedRepositoryList(Lists.mutable.withAll(repositories));
    }

    /**
     * Produce a list of repositories sorted by visibility. In the resulting list, is one repository is visible to
     * another, then the visible repository will be earlier in the list. E.g., if repository A is visible to B, then A
     * will appear before B in the list. Two repositories which are not visible to each other may appear in any order.
     * An exception will be thrown if a visibility loop is detected. The result will be a new list, and othe original
     * will not be modified.
     *
     * @param repositories repositories to sort
     * @param <T>          repository type
     * @return list of repositories sorted by visibility
     */
    public static <T extends CodeRepository> MutableList<T> toSortedRepositoryList(ListIterable<T> repositories)
    {
        if (repositories.size() <= 1)
        {
            return Lists.mutable.withAll(repositories);
        }

        MutableList<T> result = Lists.mutable.ofInitialCapacity(repositories.size());

        // We use a LinkedHashMap so that iteration order will be deterministic, which in turn ensures a deterministic result.
        Map<T, MutableList<T>> remaining = new LinkedHashMap<>(repositories.size());
        repositories.forEach(r ->
        {
            MutableList<T> visible = repositories.select(r2 -> (r2 != r) && r.isVisible(r2), Lists.mutable.empty());
            if (visible.isEmpty())
            {
                // no other repositories visible, can go straight into result
                result.add(r);
            }
            else
            {
                remaining.put(r, visible);
            }
        });

        while (!remaining.isEmpty())
        {
            int beforeSize = remaining.size();
            Iterator<Map.Entry<T, MutableList<T>>> iterator = remaining.entrySet().iterator();
            while (iterator.hasNext())
            {
                Map.Entry<T, MutableList<T>> entry = iterator.next();
                MutableList<T> visible = entry.getValue();
                visible.removeIf(r -> !remaining.containsKey(r));
                if (visible.isEmpty())
                {
                    result.add(entry.getKey());
                    iterator.remove();
                }
            }
            if (remaining.size() == beforeSize)
            {
                // could not make progress - there must be a visibility loop
                StringBuilder builder = new StringBuilder("Could not consistently order the following repositories:");
                remaining.forEach((r, vis) -> vis.asLazy().collect(CodeRepository::getName).appendString(builder.append(" ").append(r.getName()), " (visible: ", ", ", "),"));
                builder.deleteCharAt(builder.length() - 1);
                throw new RuntimeException(builder.toString());
            }
        }

        return result;
    }
}
