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
import org.finos.legend.pure.m3.tools.ListHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ModuleManifest
{
    private final String name;
    private final ImmutableList<String> dependencies;
    private final ImmutableList<ConcreteElementMetadata> elements;

    private ModuleManifest(String name, ImmutableList<String> dependencies, ImmutableList<ConcreteElementMetadata> elements)
    {
        this.name = name;
        this.dependencies = dependencies;
        this.elements = elements;
    }

    public String getModuleName()
    {
        return this.name;
    }

    public ImmutableList<String> getDependencies()
    {
        return this.dependencies;
    }

    public int getElementCount()
    {
        return this.elements.size();
    }

    public ImmutableList<ConcreteElementMetadata> getElements()
    {
        return this.elements;
    }

    public void forEachElement(Consumer<? super ConcreteElementMetadata> consumer)
    {
        this.elements.forEach(consumer);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ModuleManifest))
        {
            return false;
        }

        ModuleManifest that = (ModuleManifest) other;
        return this.name.equals(that.name) &&
                this.dependencies.equals(that.dependencies) &&
                this.elements.equals(that.elements);
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
                .append(" moduleName='").append(this.name).append("' ");
        this.dependencies.appendString(builder, "dependencies=[", ", ", "] ");
        builder.append("elements=[");
        if (this.elements.notEmpty())
        {
            this.elements.forEach(e -> e.appendString(builder.append('{')).append("}, "));
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]>").toString();
    }

    public ModuleManifest withElement(ConcreteElementMetadata newElement)
    {
        return builder(this).withElement(newElement, true).build();
    }

    public ModuleManifest withElements(ConcreteElementMetadata... newElements)
    {
        return withElements(Arrays.asList(newElements));
    }

    public ModuleManifest withElements(Iterable<? extends ConcreteElementMetadata> newElements)
    {
        return builder(this).withElements(newElements, true).build();
    }

    public ModuleManifest withoutElement(String toRemove)
    {
        Builder builder = builder(this);
        return builder.removeElement(toRemove) ? builder.buildNoValidation() : this;
    }

    public ModuleManifest withoutElements(String... toRemove)
    {
        if (toRemove.length == 0)
        {
            return this;
        }
        Builder builder = builder(this);
        return builder.removeElements(toRemove) ? builder.buildNoValidation() : this;
    }

    public ModuleManifest withoutElements(Iterable<? extends String> toRemove)
    {
        Builder builder = builder(this);
        return builder.removeElements(toRemove) ? builder.buildNoValidation() : this;
    }

    public ModuleManifest withoutElements(Predicate<? super ConcreteElementMetadata> predicate)
    {
        Builder builder = builder(this);
        return builder.removeElements(predicate) ? builder.buildNoValidation() : this;
    }

    public ModuleManifest update(Iterable<? extends ConcreteElementMetadata> newElements, Iterable<? extends String> toRemove)
    {
        return update(newElements, getRemoveElementPredicate(toRemove));
    }

    public ModuleManifest update(Iterable<? extends ConcreteElementMetadata> newElements, Predicate<? super ConcreteElementMetadata> toRemove)
    {
        MutableMap<String, ConcreteElementMetadata> newElementsByPath = indexElements(newElements);
        if ((newElementsByPath != null) && newElementsByPath.notEmpty())
        {
            Builder builder = builder(this);
            builder.removeElements(toRemove);
            builder.updateElements(newElementsByPath);
            return builder.build();
        }
        if (toRemove != null)
        {
            return withoutElements(toRemove);
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

    @Deprecated
    public static Builder builder(int elementCount)
    {
        return builder(0, elementCount);
    }

    public static Builder builder(int dependencyCount, int elementCount)
    {
        return new Builder(dependencyCount, elementCount);
    }

    public static Builder builder(ModuleManifest metadata)
    {
        return new Builder(metadata);
    }

    public static class Builder
    {
        private String name;
        private final MutableList<String> dependencies;
        private final MutableList<ConcreteElementMetadata> elements;

        private Builder()
        {
            this.dependencies = Lists.mutable.empty();
            this.elements = Lists.mutable.empty();
        }

        private Builder(int dependencyCount, int elementCount)
        {
            this.dependencies = Lists.mutable.ofInitialCapacity(dependencyCount);
            this.elements = Lists.mutable.ofInitialCapacity(elementCount);
        }

        private Builder(ModuleManifest metadata)
        {
            this.name = metadata.getModuleName();
            this.dependencies = Lists.mutable.withAll(metadata.getDependencies());
            this.elements = Lists.mutable.withAll(metadata.getElements());
        }

        public void setModuleName(String name)
        {
            this.name = name;
        }

        public void addDependency(String dependency)
        {
            this.dependencies.add(Objects.requireNonNull(dependency, "dependency may not be null"));
        }

        public void addDependencies(Iterable<? extends String> dependencies)
        {
            dependencies.forEach(this::addDependency);
        }

        public void addDependencies(String... dependencies)
        {
            addDependencies(Arrays.asList(dependencies));
        }

        public boolean removeDependency(String toRemove)
        {
            return removeDependencies(Sets.immutable.with(toRemove));
        }

        public boolean removeDependencies(Iterable<? extends String> toRemove)
        {
            return removeDependencies(getRemoveDependencyPredicate(toRemove));
        }

        public boolean removeDependencies(String... toRemove)
        {
            return removeDependencies(Sets.immutable.with(toRemove));
        }

        public boolean removeDependencies(Predicate<? super String> toRemove)
        {
            return (toRemove != null) && this.dependencies.removeIf(toRemove::test);
        }

        public void clearDependencies()
        {
            this.dependencies.clear();
        }

        public void addElement(ConcreteElementMetadata element)
        {
            this.elements.add(Objects.requireNonNull(element, "element metadata may not be null"));
        }

        public void addElements(Iterable<? extends ConcreteElementMetadata> elements)
        {
            elements.forEach(this::addElement);
        }

        public void addElements(ConcreteElementMetadata... elements)
        {
            addElements(Arrays.asList(elements));
        }

        public void updateElement(ConcreteElementMetadata element)
        {
            Objects.requireNonNull(element, "element metadata may not be null");
            int[] count = {0};
            String path = element.getPath();
            this.elements.replaceAll(e -> path.equals(e.getPath()) ? ((count[0]++ == 0) ? element : null) : e);
            if (count[0] == 0)
            {
                this.elements.add(element);
            }
            else if (count[0] > 1)
            {
                this.elements.removeIf(Objects::isNull);
            }
        }

        public void updateElements(Iterable<? extends ConcreteElementMetadata> newElements)
        {
            updateElements(indexElements(newElements));
        }

        private void updateElements(MutableMap<String, ConcreteElementMetadata> newElementsByPath)
        {
            if (newElementsByPath.isEmpty())
            {
                return;
            }

            MutableSet<String> updated = Sets.mutable.empty();
            this.elements.replaceAll(e ->
            {
                String path = e.getPath();
                ConcreteElementMetadata replacement = newElementsByPath.remove(path);
                if (replacement != null)
                {
                    updated.add(path);
                    return replacement;
                }
                return updated.contains(path) ? null : e;
            });
            this.elements.removeIf(Objects::isNull);
            if (newElementsByPath.notEmpty())
            {
                this.elements.addAll(newElementsByPath.values());
            }
        }

        public boolean removeElement(String toRemove)
        {
            return removeElements(getRemoveElementPredicate(Sets.immutable.with(toRemove)));
        }

        public boolean removeElements(Iterable<? extends String> toRemove)
        {
            return removeElements(getRemoveElementPredicate(toRemove));
        }

        public boolean removeElements(String... toRemove)
        {
            return (toRemove.length != 0) && removeElements(Sets.immutable.with(toRemove));
        }

        public boolean removeElements(Predicate<? super ConcreteElementMetadata> toRemove)
        {
            return (toRemove != null) && this.elements.removeIf(toRemove::test);
        }

        public Builder withModuleName(String name)
        {
            setModuleName(name);
            return this;
        }

        public Builder withDependency(String dependency)
        {
            addDependency(dependency);
            return this;
        }

        public Builder withDependencies(Iterable<? extends String> dependencies)
        {
            addDependencies(dependencies);
            return this;
        }

        public Builder withDependencies(String... dependencies)
        {
            addDependencies(dependencies);
            return this;
        }

        public Builder withoutDependency(String toRemove)
        {
            removeDependency(toRemove);
            return this;
        }

        public Builder withoutDependencies(Iterable<? extends String> toRemove)
        {
            removeDependencies(toRemove);
            return this;
        }

        public Builder withoutDependencies(String... toRemove)
        {
            removeDependencies(toRemove);
            return this;
        }

        public Builder withoutDependencies(Predicate<? super String> toRemove)
        {
            removeDependencies(toRemove);
            return this;
        }

        public Builder withNoDependencies()
        {
            clearDependencies();
            return this;
        }

        public Builder withElement(ConcreteElementMetadata element)
        {
            return withElement(element, false);
        }

        public Builder withElement(ConcreteElementMetadata element, boolean update)
        {
            if (update)
            {
                updateElement(element);
            }
            else
            {
                addElement(element);
            }
            return this;
        }

        public Builder withElements(Iterable<? extends ConcreteElementMetadata> elements)
        {
            return withElements(elements, false);
        }

        public Builder withElements(ConcreteElementMetadata... elements)
        {
            return withElements(Arrays.asList(elements));
        }

        public Builder withElements(Iterable<? extends ConcreteElementMetadata> elements, boolean update)
        {
            if (update)
            {
                updateElements(elements);
            }
            else
            {
                addElements(elements);
            }
            return this;
        }

        public Builder withoutElement(String toRemove)
        {
            removeElement(toRemove);
            return this;
        }

        public Builder withoutElements(Predicate<? super ConcreteElementMetadata> toRemove)
        {
            removeElements(toRemove);
            return this;
        }

        public Builder withoutElements(Iterable<? extends String> toRemove)
        {
            removeElements(toRemove);
            return this;
        }

        public Builder withoutElements(String... toRemove)
        {
            removeElements(toRemove);
            return this;
        }

        public ModuleManifest build()
        {
            Objects.requireNonNull(this.name, "module name may not be null");
            ListHelper.sortAndRemoveDuplicates(this.dependencies);
            ListHelper.sortAndRemoveDuplicates(this.elements,
                    Comparator.comparing(PackageableElementMetadata::getPath),
                    (previous, current) ->
                    {
                        String path = previous.getPath();
                        if (!path.equals(current.getPath()))
                        {
                            return false;
                        }
                        if (!previous.equals(current))
                        {
                            throw new IllegalArgumentException("Conflict for element: " + path);
                        }
                        return true;
                    });
            return buildNoValidation();
        }

        private ModuleManifest buildNoValidation()
        {
            return new ModuleManifest(this.name, this.dependencies.toImmutable(), this.elements.toImmutable());
        }
    }

    private static MutableMap<String, ConcreteElementMetadata> indexElements(Iterable<? extends ConcreteElementMetadata> elements)
    {
        if (elements == null)
        {
            return null;
        }

        MutableMap<String, ConcreteElementMetadata> index = (elements instanceof Collection) ? Maps.mutable.ofInitialCapacity(((Collection<?>) elements).size()) : Maps.mutable.empty();
        elements.forEach(object ->
        {
            String key = Objects.requireNonNull(object, "element metadata may not be null").getPath();
            ConcreteElementMetadata old = index.put(key, object);
            if ((old != null) && !old.equals(object))
            {
                throw new IllegalArgumentException("Conflict for element: " + key);
            }
        });
        return index;
    }

    private static Predicate<ConcreteElementMetadata> getRemoveElementPredicate(Iterable<? extends String> toRemove)
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
                String elementToRemove = Iterate.getFirst(set);
                return (elementToRemove == null) ? null : emd -> elementToRemove.equals(emd.getPath());
            }
            default:
            {
                return emd -> set.contains(emd.getPath());
            }
        }
    }

    private static Predicate<String> getRemoveDependencyPredicate(Iterable<? extends String> toRemove)
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
                String dependencyToRemove = Iterate.getFirst(set);
                return (dependencyToRemove == null) ? null : dependencyToRemove::equals;
            }
            default:
            {
                return set::contains;
            }
        }
    }
}
