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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.AbstractLazySpliterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class PackageTreeIterable extends AbstractLazySpliterable<Package>
{
    private final ImmutableSet<Package> startingPackages;
    private final Function<? super Package, ? extends GraphWalkFilterResult> filter;
    private final boolean depthFirst;

    private PackageTreeIterable(Iterable<? extends Package> startingPackages, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
    {
        this.startingPackages = Sets.immutable.withAll(startingPackages);
        this.filter = filter;
        this.depthFirst = depthFirst;
    }

    @Override
    public Spliterator<Package> spliterator()
    {
        return new PackageTreeSpliterator(this.startingPackages, this.filter, this.depthFirst);
    }

    @Override
    public boolean isEmpty()
    {
        return this.startingPackages.isEmpty() || ((this.filter != null) && super.isEmpty());
    }

    @Override
    public boolean contains(Object object)
    {
        return (object instanceof Package) && super.contains(object);
    }

    @Override
    public LazyIterable<Package> distinct()
    {
        return this;
    }

    public boolean isDepthFirst()
    {
        return this.depthFirst;
    }

    public static PackageTreeIterable newPackageTreeIterable(Iterable<? extends Package> startingPackages, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
    {
        return new PackageTreeIterable(startingPackages, filter, depthFirst);
    }

    public static PackageTreeIterable newPackageTreeIterable(Iterable<? extends Package> startingPackages, boolean depthFirst)
    {
        return newPackageTreeIterable(startingPackages, null, depthFirst);
    }

    public static PackageTreeIterable newPackageTreeIterable(Iterable<? extends Package> startingPackages)
    {
        return newPackageTreeIterable(startingPackages, true);
    }

    public static PackageTreeIterable newPackageTreeIterable(Package startingPackage, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
    {
        return newPackageTreeIterable(Sets.immutable.with(startingPackage), filter, depthFirst);
    }

    public static PackageTreeIterable newPackageTreeIterable(Package startingPackage, boolean depthFirst)
    {
        return newPackageTreeIterable(startingPackage, null, depthFirst);
    }

    public static PackageTreeIterable newPackageTreeIterable(Package startingPackage)
    {
        return newPackageTreeIterable(startingPackage, true);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ModelRepository repository, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
    {
        return newPackageTreeIterable((Package) repository.getTopLevel(M3Paths.Root), filter, depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ModelRepository repository, boolean depthFirst)
    {
        return newRootPackageTreeIterable(repository, null, depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ModelRepository repository)
    {
        return newRootPackageTreeIterable(repository, true);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
    {
        return newPackageTreeIterable((Package) processorSupport.repository_getTopLevel(M3Paths.Root), filter, depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport, boolean depthFirst)
    {
        return newRootPackageTreeIterable(processorSupport, null, depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport)
    {
        return newRootPackageTreeIterable(processorSupport, true);
    }

    private static class PackageTreeSpliterator implements Spliterator<Package>
    {
        private final Deque<Package> deque;
        private final Function<? super Package, ? extends GraphWalkFilterResult> filter;
        private final boolean depthFirst;

        private PackageTreeSpliterator(Deque<Package> deque, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
        {
            this.deque = deque;
            this.filter = filter;
            this.depthFirst = depthFirst;
        }

        private PackageTreeSpliterator(ImmutableSet<Package> startingNodes, Function<? super Package, ? extends GraphWalkFilterResult> filter, boolean depthFirst)
        {
            this(new ArrayDeque<>(startingNodes.castToSet()), filter, depthFirst);
        }

        @Override
        public boolean tryAdvance(Consumer<? super Package> action)
        {
            while (!this.deque.isEmpty())
            {
                Package pkg = this.deque.pollFirst();
                GraphWalkFilterResult filterResult = filter(pkg);
                if (filterResult.shouldContinue())
                {
                    pkg._children().forEach(this::possiblyAddChild);
                }
                if (filterResult.shouldAccept())
                {
                    action.accept(pkg);
                    return true;
                }
            }
            return false;
        }

        @Override
        public Spliterator<Package> trySplit()
        {
            return (this.deque.size() < 2) ? null : new PackageTreeSpliterator(splitDeque(this.deque), this.filter, this.depthFirst);
        }

        @Override
        public long estimateSize()
        {
            return this.deque.isEmpty() ? 0L : Long.MAX_VALUE;
        }

        @Override
        public long getExactSizeIfKnown()
        {
            return this.deque.isEmpty() ? 0L : -1L;
        }

        @Override
        public int characteristics()
        {
            return NONNULL | DISTINCT;
        }

        private GraphWalkFilterResult filter(Package pkg)
        {
            if (this.filter != null)
            {
                GraphWalkFilterResult result = this.filter.apply(pkg);
                if (result != null)
                {
                    return result;
                }
            }
            return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
        }

        private void possiblyAddChild(CoreInstance child)
        {
            if (child instanceof Package)
            {
                if (this.depthFirst)
                {
                    this.deque.addFirst((Package) child);
                }
                else
                {
                    this.deque.addLast((Package) child);
                }
            }
        }
    }
}
