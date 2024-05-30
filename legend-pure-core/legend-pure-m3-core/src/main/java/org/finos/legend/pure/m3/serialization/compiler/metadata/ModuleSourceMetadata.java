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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.serialization.compiler.ModuleHelper;
import org.finos.legend.pure.m3.tools.ListHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ModuleSourceMetadata
{
    private final String name;
    private final ImmutableList<SourceMetadata> sources;

    private ModuleSourceMetadata(String name, ImmutableList<SourceMetadata> sources)
    {
        this.name = name;
        this.sources = sources;
    }

    public String getModuleName()
    {
        return this.name;
    }

    public int getSourceCount()
    {
        return this.sources.size();
    }

    public ImmutableList<SourceMetadata> getSources()
    {
        return this.sources;
    }

    public void forEachSource(Consumer<? super SourceMetadata> consumer)
    {
        this.sources.forEach(consumer);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ModuleSourceMetadata))
        {
            return false;
        }

        ModuleSourceMetadata that = (ModuleSourceMetadata) other;
        return this.name.equals(that.name) &&
                this.sources.equals(that.sources);
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName())
                .append(" moduleName='").append(this.name).append("' sources=[");
        this.sources.forEach(source ->
        {
            builder.append(source.getSourceId()).append('{');
            source.getSections().forEach(section -> section.getElements().appendString(builder.append(section.getParser()), ":[", ", ", "], "));
            if (source.getSections().notEmpty())
            {
                builder.setLength(builder.length() - 2);
            }
            builder.append("}, ");
        });
        if (this.sources.notEmpty())
        {
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]>").toString();
    }

    public ModuleSourceMetadata withSource(SourceMetadata newSource)
    {
        return builder(this).withSource(newSource, true).build();
    }

    public ModuleSourceMetadata withSources(SourceMetadata... newSources)
    {
        return withSources(Arrays.asList(newSources));
    }

    public ModuleSourceMetadata withSources(Iterable<? extends SourceMetadata> newSources)
    {
        return builder(this).withSources(newSources, true).build();
    }

    public ModuleSourceMetadata withoutSource(String toRemove)
    {
        Builder builder = builder(this);
        return builder.removeSource(toRemove) ? builder.buildNoValidation() : this;
    }

    public ModuleSourceMetadata withoutSources(String... toRemove)
    {
        if (toRemove.length == 0)
        {
            return this;
        }
        Builder builder = builder(this);
        return builder.removeSources(toRemove) ? builder.buildNoValidation() : this;
    }

    public ModuleSourceMetadata withoutSources(Iterable<? extends String> toRemove)
    {
        Builder builder = builder(this);
        return builder.removeSources(toRemove) ? builder.buildNoValidation() : this;
    }

    public ModuleSourceMetadata withoutSources(Predicate<? super SourceMetadata> predicate)
    {
        Builder builder = builder(this);
        return builder.removeSources(predicate) ? builder.buildNoValidation() : this;
    }

    public ModuleSourceMetadata update(Iterable<? extends SourceMetadata> newSources, Iterable<? extends String> toRemove)
    {
        return update(newSources, getRemoveSourcePredicate(toRemove));
    }

    public ModuleSourceMetadata update(Iterable<? extends SourceMetadata> newSources, Predicate<? super SourceMetadata> toRemove)
    {
        MutableMap<String, SourceMetadata> newSourcesById = indexSources(newSources);
        if ((newSourcesById != null) && newSourcesById.notEmpty())
        {
            Builder builder = builder(this);
            builder.removeSources(toRemove);
            builder.updateSources(newSourcesById);
            return builder.build();
        }
        if (toRemove != null)
        {
            return withoutSources(toRemove);
        }
        return this;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static Builder builder(String name)
    {
        return builder().withModuleName(name);
    }

    public static Builder builder(int sourceCount)
    {
        return new Builder(sourceCount);
    }

    public static Builder builder(ModuleSourceMetadata metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String name;
        private final MutableList<SourceMetadata> sources;

        private Builder()
        {
            this.sources = Lists.mutable.empty();
        }

        private Builder(int sourceCount)
        {
            this.sources = Lists.mutable.ofInitialCapacity(sourceCount);
        }

        private Builder(ModuleSourceMetadata metadata)
        {
            this.name = metadata.getModuleName();
            this.sources = Lists.mutable.withAll(metadata.getSources());
        }

        public void setModuleName(String name)
        {
            this.name = name;
        }

        public void addSource(SourceMetadata source)
        {
            this.sources.add(Objects.requireNonNull(source, "source metadata may not be null"));
        }

        public void addSources(Iterable<? extends SourceMetadata> sources)
        {
            sources.forEach(this::addSource);
        }

        public void addSources(SourceMetadata... sources)
        {
            addSources(Arrays.asList(sources));
        }

        public void updateSource(SourceMetadata source)
        {
            Objects.requireNonNull(source, "source metadata may not be null");
            int[] count = {0};
            String sourceId = source.getSourceId();
            this.sources.replaceAll(s -> sourceId.equals(s.getSourceId()) ? ((count[0]++ == 0) ? source : null) : s);
            if (count[0] == 0)
            {
                this.sources.add(source);
            }
            else if (count[0] > 1)
            {
                this.sources.removeIf(Objects::isNull);
            }
        }

        public void updateSources(Iterable<? extends SourceMetadata> newSources)
        {
            updateSources(indexSources(newSources));
        }

        private void updateSources(MutableMap<String, SourceMetadata> newSourcesById)
        {
            if (newSourcesById.isEmpty())
            {
                return;
            }

            MutableSet<String> updated = Sets.mutable.empty();
            this.sources.replaceAll(s ->
            {
                String sourceId = s.getSourceId();
                SourceMetadata replacement = newSourcesById.remove(sourceId);
                if (replacement != null)
                {
                    updated.add(sourceId);
                    return replacement;
                }
                return updated.contains(sourceId) ? null : s;
            });
            this.sources.removeIf(Objects::isNull);
            if (newSourcesById.notEmpty())
            {
                this.sources.addAll(newSourcesById.values());
            }
        }

        public boolean removeSource(String toRemove)
        {
            return removeSources(getRemoveSourcePredicate(Sets.immutable.with(toRemove)));
        }

        public boolean removeSources(Iterable<? extends String> toRemove)
        {
            return removeSources(getRemoveSourcePredicate(toRemove));
        }

        public boolean removeSources(String... toRemove)
        {
            return (toRemove.length != 0) && removeSources(Sets.immutable.with(toRemove));
        }

        public boolean removeSources(Predicate<? super SourceMetadata> toRemove)
        {
            return (toRemove != null) && this.sources.removeIf(toRemove);
        }

        public Builder withModuleName(String name)
        {
            setModuleName(name);
            return this;
        }

        public Builder withSource(SourceMetadata source)
        {
            return withSource(source, false);
        }

        public Builder withSource(SourceMetadata source, boolean update)
        {
            if (update)
            {
                updateSource(source);
            }
            else
            {
                addSource(source);
            }
            return this;
        }

        public Builder withSources(Iterable<? extends SourceMetadata> sources)
        {
            return withSources(sources, false);
        }

        public Builder withSources(SourceMetadata... sources)
        {
            return withSources(Arrays.asList(sources));
        }

        public Builder withSources(Iterable<? extends SourceMetadata> sources, boolean update)
        {
            if (update)
            {
                updateSources(sources);
            }
            else
            {
                addSources(sources);
            }
            return this;
        }

        public Builder withoutSource(String toRemove)
        {
            removeSource(toRemove);
            return this;
        }

        public Builder withoutSources(Predicate<? super SourceMetadata> toRemove)
        {
            removeSources(toRemove);
            return this;
        }

        public Builder withoutSources(Iterable<? extends String> toRemove)
        {
            removeSources(toRemove);
            return this;
        }

        public Builder withoutSources(String... toRemove)
        {
            removeSources(toRemove);
            return this;
        }

        public ModuleSourceMetadata build()
        {
            Objects.requireNonNull(this.name, "module name may not be null");
            ListHelper.sortAndRemoveDuplicates(this.sources,
                    Comparator.comparing(SourceMetadata::getSourceId),
                    (previous, current) ->
                    {
                        String path = previous.getSourceId();
                        if (!path.equals(current.getSourceId()))
                        {
                            return false;
                        }
                        if (!previous.equals(current))
                        {
                            throw new IllegalArgumentException("Conflict for source: " + path);
                        }
                        return true;
                    });
            validateSources();
            return buildNoValidation();
        }

        private ModuleSourceMetadata buildNoValidation()
        {
            return new ModuleSourceMetadata(this.name, this.sources.toImmutable());
        }

        private void validateSources()
        {
            MutableList<String> invalidSourceIds = Lists.mutable.empty();
            this.sources.collectIf(
                    s -> !ModuleHelper.isSourceInModule(s.getSourceId(), this.name),
                    SourceMetadata::getSourceId,
                    invalidSourceIds);
            if (invalidSourceIds.notEmpty())
            {
                MutableSet<String> set = Sets.mutable.empty();
                invalidSourceIds.removeIf(s -> !set.add(s));
                StringBuilder builder = new StringBuilder("Invalid source");
                if (invalidSourceIds.size() > 1)
                {
                    invalidSourceIds.sortThis();
                    builder.append('s');
                }
                builder.append(" in module '").append(this.name).append("': ");
                invalidSourceIds.appendString(builder, ", ");
                throw new IllegalArgumentException(builder.toString());
            }
        }
    }

    private static MutableMap<String, SourceMetadata> indexSources(Iterable<? extends SourceMetadata> sources)
    {
        if (sources == null)
        {
            return null;
        }

        MutableMap<String, SourceMetadata> index = (sources instanceof Collection) ? Maps.mutable.ofInitialCapacity(((Collection<?>) sources).size()) : Maps.mutable.empty();
        sources.forEach(object ->
        {
            String key = Objects.requireNonNull(object, "source metadata may not be null").getSourceId();
            SourceMetadata old = index.put(key, object);
            if ((old != null) && !old.equals(object))
            {
                throw new IllegalArgumentException("Conflict for source: " + key);
            }
        });
        return index;
    }

    private static Predicate<SourceMetadata> getRemoveSourcePredicate(Iterable<? extends String> toRemove)
    {
        if (toRemove == null)
        {
            return null;
        }
        Set<? extends String> set = (toRemove instanceof Set) ? (Set<? extends String>) toRemove : Sets.mutable.withAll(toRemove);
        switch (set.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                String sourceToRemove = Iterate.getFirst(set);
                return (sourceToRemove == null) ? null : smd -> sourceToRemove.equals(smd.getSourceId());
            }
            default:
            {
                return smd -> set.contains(smd.getSourceId());
            }
        }
    }
}
