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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

import java.util.List;
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
    public abstract boolean isVisible(CodeRepository other);

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

        return this.name != null && this.name.equals(((CodeRepository) other).getName());
    }

    @Override
    public int hashCode()
    {
        return this.name == null ? 0 : this.name.hashCode();
    }

    @Override
    public String toString()
    {
        return "<" + getClass().getSimpleName() + " \"" + this.name + "\">";
    }

    protected static boolean isValidRepositoryName(String name)
    {
        return name == null || VALID_REPO_NAME_PATTERN.matcher(name).matches();
    }

    public static CodeRepository newScratchCodeRepository()
    {
        return new ScratchCodeRepository();
    }

    public static CodeRepository newWelcomeCodeRepository()
    {
        return new WelcomeCodeRepository();
    }


    public static CodeRepository newScratchCodeRepository(String name)
    {
        return new ScratchCodeRepository(name);
    }

    /**
     * Produce a list of repositories sorted by visibility. In the resulting list, if one repository is visible to
     * another, then the visible repository will be earlier in the list. E.g., if repository A is visible to B, then A
     * will appear before B in the list. Two repositories which are not visible to each other may appear in any order.
     * However, the order of the result will not depend on the order of the input. An exception will be thrown if a
     * visibility loop is detected. The result will be a new list, and the original will not be modified.
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
     * Produce a list of repositories sorted by visibility. In the resulting list, if one repository is visible to
     * another, then the visible repository will be earlier in the list. E.g., if repository A is visible to B, then A
     * will appear before B in the list. Two repositories which are not visible to each other may appear in any order.
     * However, the order of the result will not depend on the order of the input. An exception will be thrown if a
     * visibility loop is detected. The result will be a new list, and the original will not be modified.
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
        MutableMap<T, MutableList<T>> remaining = Maps.mutable.empty();
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
        // none of these is visible to another - sort to maintain stability of result irrespective of input order
        result.sortThis(CodeRepository::compareReposSameVisibilityLevel);

        while (remaining.notEmpty())
        {
            MutableList<T> next = Lists.mutable.empty();
            remaining.forEachKeyValue((repo, visible) ->
            {
                visible.removeIf(r -> !remaining.containsKey(r));
                if (visible.isEmpty())
                {
                    next.add(repo);
                }
            });
            if (next.isEmpty())
            {
                // could not make progress - there must be a visibility loop
                StringBuilder builder = new StringBuilder("Could not consistently order the following repositories:");
                remaining.keysView().toSortedListBy(x -> x.getName() == null ? "" : x.getName())
                        .forEach(r -> remaining.get(r).collect(CodeRepository::getName).sortThis().appendString(builder.append(" ").append(r.getName()), " (visible: ", ", ", "),"));
                builder.deleteCharAt(builder.length() - 1);
                throw new RuntimeException(builder.toString());
            }
            // none of these is visible to another - sort to maintain stability of result irrespective of input order
            next.sortThis(CodeRepository::compareReposSameVisibilityLevel);
            next.forEach(remaining::remove);
            result.addAll(next);
        }

        return result;
    }

    private static int compareReposSameVisibilityLevel(CodeRepository repo1, CodeRepository repo2)
    {
        if (repo1 == repo2)
        {
            return 0;
        }

        int nameCmp = repo1.getName().compareTo(repo2.getName());
        if (nameCmp != 0)
        {
            return nameCmp;
        }

        Class<?> class1 = repo1.getClass();
        Class<?> class2 = repo2.getClass();
        if (class1 != class2)
        {
            return class1.getName().compareTo(class2.getName());
        }

        Pattern pattern1 = repo1.getAllowedPackagesPattern();
        Pattern pattern2 = repo2.getAllowedPackagesPattern();
        return (pattern1 == null) ?
                ((pattern2 == null) ? 0 : 1) :
                ((pattern2 == null) ? -1 : pattern1.pattern().compareTo(pattern2.pattern()));
    }
}
