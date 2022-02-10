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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class PackageTreeIterable extends AbstractLazyIterable<Package>
{
    private final ImmutableSet<Package> startingPackages;
    private final boolean depthFirst;

    private PackageTreeIterable(Iterable<? extends Package> startingPackages, boolean depthFirst)
    {
        this.startingPackages = Sets.immutable.withAll(startingPackages);
        this.depthFirst = depthFirst;
    }

    @Override
    public Iterator<Package> iterator()
    {
        return new PackageTreeIterator(this.startingPackages, this.depthFirst);
    }

    @Override
    public Spliterator<Package> spliterator()
    {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.DISTINCT | Spliterator.NONNULL);
    }

    @Override
    public void each(Procedure<? super Package> procedure)
    {
        for (Package pkg : this)
        {
            procedure.value(pkg);
        }
    }

    @Override
    public void forEach(Consumer<? super Package> consumer)
    {
        for (Package pkg : this)
        {
            consumer.accept(pkg);
        }
    }

    public boolean isDepthFirst()
    {
        return this.depthFirst;
    }

    public static PackageTreeIterable newPackageTreeIterable(Iterable<? extends Package> startingPackages, boolean depthFirst)
    {
        return new PackageTreeIterable(startingPackages, depthFirst);
    }

    public static PackageTreeIterable newPackageTreeIterable(Iterable<? extends Package> startingPackages)
    {
        return newPackageTreeIterable(startingPackages, true);
    }

    public static PackageTreeIterable newPackageTreeIterable(Package startingPackage, boolean depthFirst)
    {
        return newPackageTreeIterable(Lists.immutable.with(startingPackage), depthFirst);
    }

    public static PackageTreeIterable newPackageTreeIterable(Package startingPackage)
    {
        return newPackageTreeIterable(startingPackage, true);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ModelRepository repository, boolean depthFirst)
    {
        return newPackageTreeIterable((Package) repository.getTopLevel(M3Paths.Root), depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ModelRepository repository)
    {
        return newRootPackageTreeIterable(repository, true);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport, boolean depthFirst)
    {
        return newPackageTreeIterable((Package) processorSupport.repository_getTopLevel(M3Paths.Root), depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport)
    {
        return newRootPackageTreeIterable(processorSupport, true);
    }

    private static class PackageTreeIterator implements Iterator<Package>
    {
        private final Deque<Package> deque;
        private final boolean depthFirst;

        private PackageTreeIterator(ImmutableSet<Package> startingNodes, boolean depthFirst)
        {
            this.deque = new ArrayDeque<>(startingNodes.castToSet());
            this.depthFirst = depthFirst;
        }

        @Override
        public boolean hasNext()
        {
            return !this.deque.isEmpty();
        }

        @Override
        public Package next()
        {
            Package pkg = this.deque.removeFirst();
            pkg._children().forEach(this::possiblyAddChild);
            return pkg;
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