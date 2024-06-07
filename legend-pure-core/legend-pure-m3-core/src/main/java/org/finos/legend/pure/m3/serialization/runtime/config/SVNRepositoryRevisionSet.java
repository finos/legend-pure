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

package org.finos.legend.pure.m3.serialization.runtime.config;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.block.factory.primitive.LongPredicates;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.util.Map;

public class SVNRepositoryRevisionSet
{
    private static final LongPredicate INVALID_REVISION = LongPredicates.lessThan(0L);

    private final ImmutableObjectLongMap<String> repoRevisions;

    private SVNRepositoryRevisionSet(ObjectLongMap<String> repoRevisions)
    {
        if (repoRevisions.anySatisfy(INVALID_REVISION))
        {
            throw new IllegalArgumentException(repoRevisions.keyValuesView().makeString("Invalid repository revision set: [", ", ", "]"));
        }
        this.repoRevisions = repoRevisions.toImmutable();
    }

    @Override
    public int hashCode()
    {
        return this.repoRevisions.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        return (other == this) ||
                ((other instanceof SVNRepositoryRevisionSet) &&
                        this.repoRevisions.equals(((SVNRepositoryRevisionSet)other).repoRevisions));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);
        builder.append('{');
        write(builder, ":", ", ", true);
        builder.append('}');
        return builder.toString();
    }

    /**
     * Get the revision for the given repository.  Returns -1
     * if the repository is not present.
     *
     * @param repository repository
     * @return repository revision
     */
    public long getRevision(String repository)
    {
        return this.repoRevisions.getIfAbsent(repository, -1L);
    }

    /**
     * Get the set of repositories.
     *
     * @return repositories
     */
    public RichIterable<String> getRepositories()
    {
        return this.repoRevisions.keysView();
    }

    /**
     * Get the single revision number for this set.  Returns
     * -1 if there are no revision numbers or more than one.
     *
     * @return single revision number
     */
    public long getSingleRevision()
    {
        LongIterator iterator = this.repoRevisions.longIterator();
        if (!iterator.hasNext())
        {
            return -1;
        }
        long revision = iterator.next();
        while (iterator.hasNext())
        {
            if (iterator.next() != revision)
            {
                return -1;
            }
        }
        return revision;
    }

    /**
     * Apply the given procedure to each repository-revision
     * pair.
     *
     * @param procedure repository-revision procedure
     */
    public void forEach(ObjectLongProcedure<? super String> procedure)
    {
        this.repoRevisions.forEachKeyValue(procedure);
    }

    /**
     * Return the size of the set (i.e., the number of
     * repository-revision pairs).
     *
     * @return size
     */
    public int size()
    {
        return this.repoRevisions.size();
    }

    /**
     * Write a representation of the repository revision set
     * to a string.
     *
     * @param repoRevSeparator     separator between each repository and its revision
     * @param repoRevPairSeparator separator between each repository-revision pair
     * @param sort                 whether to sort repositories
     * @return string representation of the repository revision set
     */
    public String writeString(String repoRevSeparator, String repoRevPairSeparator, boolean sort)
    {
        StringBuilder builder = new StringBuilder();
        write(builder, repoRevSeparator, repoRevPairSeparator, sort);
        return builder.toString();
    }

    /**
     * Write the repository revision set to the given appendable.
     *
     * @param appendable           appendable to write to
     * @param repoRevSeparator     separator between each repository and its revision
     * @param repoRevPairSeparator separator between each repository-revision pair
     * @param sort                 whether to sort repositories
     */
    public void write(Appendable appendable, String repoRevSeparator, String repoRevPairSeparator, boolean sort)
    {
        write(appendable, "", repoRevSeparator, repoRevPairSeparator, "", sort);
    }

    /**
     * Write the repository revision set to the given appendable.
     *
     * @param appendable           appendable to write to
     * @param prefix               prefix string
     * @param repoRevSeparator     separator between each repository and its revision
     * @param repoRevPairSeparator separator between each repository-revision pair
     * @param suffix               suffix string
     * @param sort                 whether to sort repositories
     */
    public void write(Appendable appendable, String prefix, String repoRevSeparator, String repoRevPairSeparator, String suffix, boolean sort)
    {
        try
        {
            if (this.repoRevisions.notEmpty())
            {
                RichIterable<String> repos = sort ? this.repoRevisions.keysView().toSortedList() : this.repoRevisions.keysView();
                appendable.append(prefix);
                boolean first = true;
                for (String repo : repos)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        appendable.append(repoRevPairSeparator);
                    }
                    appendable.append(repo);
                    appendable.append(repoRevSeparator);
                    appendable.append(Long.toString(this.repoRevisions.get(repo)));
                }
                appendable.append(suffix);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a JSON representation of the repository revision set.
     *
     * @return JSON representation
     */
    public String toJSON()
    {
        StringBuilder builder = new StringBuilder(128);
        writeJSON(builder);
        return builder.toString();
    }

    /**
     * Write a JSON representation of the repository revision set
     * to an appendable.
     *
     * @param appendable appendable to write to
     */
    public void writeJSON(Appendable appendable)
    {
        try
        {
            appendable.append('{');
            boolean first = true;
            for (String repo : getRepositories().toSortedList())
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    appendable.append(',');
                }
                appendable.append('"');
                appendable.append(JSONValue.escape(repo));
                appendable.append("\":");
                appendable.append(Long.toString(getRevision(repo)));
            }
            appendable.append('}');
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return whether this set of repository revisions is more
     * recent than another.  A set is more recent than another
     * if (1) both have the same repositories, (2) at least one
     * repository has a higher revision, and (3) no repository
     * has a lower revision.
     *
     * @param other other set of repository revisions
     * @return whether this set of repository revisions is more recent than other
     */
    public boolean isMoreRecentThan(SVNRepositoryRevisionSet other)
    {
        if (this == other)
        {
            return false;
        }

        if (size() != other.size())
        {
            return false;
        }

        boolean moreRecent = false;
        for (ObjectLongPair<String> pair : this.repoRevisions.keyValuesView())
        {
            long otherRevision = other.getRevision(pair.getOne());
            if (otherRevision == -1L)
            {
                return false;
            }

            long thisRevision = pair.getTwo();
            if (thisRevision < otherRevision)
            {
                return false;
            }
            if (thisRevision > otherRevision)
            {
                moreRecent = true;
            }
        }
        return moreRecent;
    }

    public static SVNRepositoryRevisionSet newWith(ObjectLongMap<String> repoRevisions)
    {
        return new SVNRepositoryRevisionSet(repoRevisions);
    }

    public static SVNRepositoryRevisionSet newWith()
    {
        return newWith(ObjectLongMaps.immutable.<String>with());
    }

    public static SVNRepositoryRevisionSet newWith(Iterable<String> repositories, long revision)
    {
        MutableObjectLongMap<String> repoRevisions = ObjectLongMaps.mutable.empty();
        for (String repository : repositories)
        {
            repoRevisions.put(repository, revision);
        }
        return newWith(repoRevisions);
    }

    public static SVNRepositoryRevisionSet newWith(String repository, long revision)
    {
        return newWith(ObjectLongHashMap.newWithKeysValues(repository, revision));
    }

    public static SVNRepositoryRevisionSet newWith(String repository1, long revision1, String repository2, long revision2)
    {
        return newWith(ObjectLongHashMap.newWithKeysValues(repository1, revision1, repository2, revision2));
    }

    public static SVNRepositoryRevisionSet newWith(String repository1, long revision1, String repository2, long revision2, String repository3, long revision3)
    {
        return newWith(ObjectLongHashMap.newWithKeysValues(repository1, revision1, repository2, revision2, repository3, revision3));
    }

    public static SVNRepositoryRevisionSet fromJSON(String jsonString)
    {
        Object json = JSONValue.parse(jsonString);
        if (json == null)
        {
            throw new IllegalArgumentException("Invalid JSON: " + jsonString);
        }
        if (!(json instanceof JSONObject))
        {
            throw new IllegalArgumentException("Invalid repository revision set JSON: " + jsonString);
        }
        return fromJSON((JSONObject)json);
    }

    public static SVNRepositoryRevisionSet fromJSON(JSONObject json)
    {
        MutableObjectLongMap<String> map = ObjectLongHashMap.newMap();
        for (Object entryObj : json.entrySet())
        {
            Map.Entry entry = (Map.Entry)entryObj;
            String repo = (String)entry.getKey();
            Object revision = entry.getValue();
            if (!(revision instanceof Long))
            {
                throw new IllegalArgumentException("Invalid repository revision set JSON: " + json);
            }
            map.put(repo, (Long)revision);
        }
        return newWith(map);
    }
}
