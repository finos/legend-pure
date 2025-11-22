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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.AbstractLazySpliterable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PackageableElementIterable extends AbstractLazySpliterable<PackageableElement>
{
    private final ImmutableList<PackageableElement> startingElements;
    private final ImmutableList<Package> startingPackages;
    private final Predicate<? super Package> packageFilter;

    private PackageableElementIterable(ImmutableList<PackageableElement> startingElements, ImmutableList<Package> startingPackages, Predicate<? super Package> packageFilter)
    {
        this.startingElements = startingElements;
        this.startingPackages = startingPackages;
        this.packageFilter = packageFilter;
    }

    @Override
    public boolean isEmpty()
    {
        return this.startingElements.isEmpty() && this.startingPackages.isEmpty();
    }

    @Override
    public PackageableElement getFirst()
    {
        return this.startingElements.notEmpty() ? this.startingElements.getFirst() : this.startingPackages.getFirst();
    }

    @Override
    public boolean contains(Object object)
    {
        return (object instanceof PackageableElement) &&
                (this.startingElements.contains(object) || this.startingPackages.contains(object) || super.contains(object));
    }

    @Override
    public LazyIterable<PackageableElement> distinct()
    {
        return this;
    }

    @Override
    public Spliterator<PackageableElement> spliterator()
    {
        return new PackageableElementSpliterator(this.startingElements, this.startingPackages, this.packageFilter);
    }

    private static class PackageableElementSpliterator implements Spliterator<PackageableElement>
    {
        private final Deque<PackageableElement> elementDeque;
        private final Deque<Package> packageDeque;
        private final Predicate<? super Package> packageFilter;

        private PackageableElementSpliterator(Deque<PackageableElement> elementDeque, Deque<Package> packageDeque, Predicate<? super Package> packageFilter)
        {
            this.elementDeque = elementDeque;
            this.packageDeque = packageDeque;
            this.packageFilter = packageFilter;
        }

        private PackageableElementSpliterator(ImmutableList<PackageableElement> elements, ImmutableList<Package> packages, Predicate<? super Package> packageFilter)
        {
            this.elementDeque = new ArrayDeque<>(elements.castToList());
            this.packageDeque = new ArrayDeque<>(packages.castToList());
            this.packageFilter = packageFilter;
        }

        @Override
        public boolean tryAdvance(Consumer<? super PackageableElement> action)
        {
            if (!this.elementDeque.isEmpty())
            {
                PackageableElement element = this.elementDeque.pollFirst();
                action.accept(element);
                return true;
            }

            if (!this.packageDeque.isEmpty())
            {
                Package pkg = this.packageDeque.pollFirst();
                if (shouldContinue(pkg))
                {
                    pkg._children().forEach(c ->
                    {
                        if (c instanceof Package)
                        {
                            this.packageDeque.add((Package) c);
                        }
                        else
                        {
                            this.elementDeque.add(c);
                        }
                    });
                }
                action.accept(pkg);
                return true;
            }

            return false;
        }

        @Override
        public Spliterator<PackageableElement> trySplit()
        {
            if ((this.packageDeque.size() < 2) && (this.elementDeque.size() < 100))
            {
                return null;
            }

            Deque<PackageableElement> newElementDeque = splitDeque(this.elementDeque);
            Deque<Package> newPackageDeque = splitDeque(this.packageDeque);
            return new PackageableElementSpliterator(newElementDeque, newPackageDeque, this.packageFilter);
        }

        @Override
        public long estimateSize()
        {
            return getSize(Long.MAX_VALUE);
        }

        @Override
        public long getExactSizeIfKnown()
        {
            return getSize(-1L);
        }

        private long getSize(long unknown)
        {
            return this.packageDeque.isEmpty() ? this.elementDeque.size() : unknown;
        }

        @Override
        public int characteristics()
        {
            return NONNULL | DISTINCT;
        }

        private boolean shouldContinue(Package pkg)
        {
            return (this.packageFilter == null) || this.packageFilter.test(pkg);
        }
    }

    public static PackageableElementIterable fromRepository(ModelRepository repository)
    {
        return fromRepository(repository, null);
    }

    public static PackageableElementIterable fromRepository(ModelRepository repository, Predicate<? super Package> packageFilter)
    {
        return builder().withTopLevels(repository).withPackageFilter(packageFilter).build();
    }

    public static PackageableElementIterable fromProcessorSupport(ProcessorSupport processorSupport)
    {
        return fromProcessorSupport(processorSupport, null);
    }

    public static PackageableElementIterable fromProcessorSupport(ProcessorSupport processorSupport, Predicate<? super Package> packageFilter)
    {
        return builder().withTopLevels(processorSupport).withPackageFilter(packageFilter).build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final MutableList<PackageableElement> startingElements = Lists.mutable.empty();
        private final MutableList<Package> startingPackages = Lists.mutable.empty();
        private Predicate<? super Package> packageFilter;

        private Builder()
        {
        }

        public void addElement(CoreInstance element)
        {
            if (element instanceof Package)
            {
                this.startingPackages.add((Package) element);
            }
            else if (element instanceof PackageableElement)
            {
                this.startingElements.add((PackageableElement) element);
            }
            else
            {
                throw new IllegalArgumentException("Invalid element: " + element);
            }
        }

        public void addElements(Iterable<? extends CoreInstance> elements)
        {
            elements.forEach(this::addElement);
        }

        public void addElements(CoreInstance... elements)
        {
            addElements(Arrays.asList(elements));
        }

        public void addPackage(CoreInstance pkg)
        {
            if (pkg instanceof Package)
            {
                this.startingPackages.add((Package) pkg);
            }
            else
            {
                throw new IllegalArgumentException("Invalid package: " + pkg);
            }
        }

        public void addPackages(Iterable<? extends CoreInstance> packages)
        {
            packages.forEach(this::addPackage);
        }

        public void addPackages(CoreInstance... packages)
        {
            addPackages(Arrays.asList(packages));
        }

        public void addTopLevels(ModelRepository repository)
        {
            addElements(repository.getTopLevels().toSortedListBy(CoreInstance::getName));
        }

        public void addTopLevels(ProcessorSupport processorSupport)
        {
            addElements(GraphTools.getTopLevelNames().collect(processorSupport::repository_getTopLevel, Lists.mutable.empty()).sortThisBy(CoreInstance::getName));
        }

        public void setPackageFilter(Predicate<? super Package> packageFilter)
        {
            this.packageFilter = packageFilter;
        }

        public Builder withElement(CoreInstance element)
        {
            addElement(element);
            return this;
        }

        public Builder withElements(Iterable<? extends CoreInstance> elements)
        {
            addElements(elements);
            return this;
        }

        public Builder withElements(CoreInstance... elements)
        {
            addElements(elements);
            return this;
        }

        public Builder withPackage(CoreInstance element)
        {
            addPackage(element);
            return this;
        }

        public Builder withPackages(Iterable<? extends CoreInstance> elements)
        {
            addPackages(elements);
            return this;
        }

        public Builder withPackages(CoreInstance... elements)
        {
            addPackages(elements);
            return this;
        }

        public Builder withTopLevels(ModelRepository repository)
        {
            addTopLevels(repository);
            return this;
        }

        public Builder withTopLevels(ProcessorSupport processorSupport)
        {
            addTopLevels(processorSupport);
            return this;
        }

        public Builder withPackageFilter(Predicate<? super Package> packageFilter)
        {
            setPackageFilter(packageFilter);
            return this;
        }

        public PackageableElementIterable build()
        {
            removeDuplicates(this.startingElements);
            removeDuplicates(this.startingPackages);
            return new PackageableElementIterable(this.startingElements.toImmutable(), this.startingPackages.toImmutable(), this.packageFilter);
        }

        private static void removeDuplicates(MutableList<?> list)
        {
            MutableSet<Object> set = Sets.mutable.ofInitialCapacity(list.size());
            list.removeIf(e -> !set.add(e));
        }
    }
}
