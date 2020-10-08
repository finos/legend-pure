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
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.internal.IteratorIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
        return this.depthFirst ? new DepthFirstPackageTreeIterator() : new BreadthFirstPackageTreeIterator();
    }

    @Override
    public void each(Procedure<? super Package> procedure)
    {
        IteratorIterate.forEach(iterator(), procedure);
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
        return newPackageTreeIterable((Package)repository.getTopLevel(M3Paths.Root), depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ModelRepository repository)
    {
        return newRootPackageTreeIterable(repository, true);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport, boolean depthFirst)
    {
        return newPackageTreeIterable((Package)processorSupport.repository_getTopLevel(M3Paths.Root), depthFirst);
    }

    public static PackageTreeIterable newRootPackageTreeIterable(ProcessorSupport processorSupport)
    {
        return newRootPackageTreeIterable(processorSupport, true);
    }

    private abstract class PackageTreeIterator implements Iterator<Package>
    {
        protected final Deque<Package> deque = new ArrayDeque<>(PackageTreeIterable.this.startingPackages.castToSet());

        @Override
        public boolean hasNext()
        {
            return !this.deque.isEmpty();
        }

        @Override
        public Package next()
        {
            Package pkg = this.deque.poll();
            if (pkg == null)
            {
                throw new NoSuchElementException();
            }
            update(pkg);
            return pkg;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void update(Package pkg)
        {
            for (CoreInstance child : pkg._children())
            {
                if (child instanceof Package)
                {
                    addPackage((Package)child);
                }
            }
        }

        abstract protected void addPackage(Package pkg);
    }

    private class DepthFirstPackageTreeIterator extends PackageTreeIterator
    {
        @Override
        protected void addPackage(Package pkg)
        {
            this.deque.addFirst(pkg);
        }
    }

    private class BreadthFirstPackageTreeIterator extends PackageTreeIterator
    {
        @Override
        protected void addPackage(Package pkg)
        {
            this.deque.addLast(pkg);
        }
    }
}
