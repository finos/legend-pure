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

package org.finos.legend.pure.m3.tests.tools;

import java.util.Iterator;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPackageTreeIterable extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testAllPackagesReached_DepthFirst()
    {
        MutableSet<Package> expected = Sets.mutable.withAll(LazyIterate.selectInstancesOf(GraphNodeIterable.fromModelRepository(this.repository), Package.class));
        PackageTreeIterable it = PackageTreeIterable.newRootPackageTreeIterable(this.repository, true);
        MutableSet<Package> actual = Sets.mutable.withAll(it);
        Verify.assertNotEmpty(actual);
        Verify.assertSetsEqual(expected, actual);
    }

    @Test
    public void testAllPackagesReached_BreadthFirst()
    {
        MutableSet<Package> expected = Sets.mutable.withAll(LazyIterate.selectInstancesOf(GraphNodeIterable.fromModelRepository(this.repository), Package.class));
        PackageTreeIterable it = PackageTreeIterable.newRootPackageTreeIterable(this.repository, false);
        MutableSet<Package> actual = Sets.mutable.withAll(it);
        Verify.assertNotEmpty(actual);
        Verify.assertSetsEqual(expected, actual);
    }

    @Test
    public void testDepthFirst()
    {
        PackageTreeIterable depthFirst = PackageTreeIterable.newRootPackageTreeIterable(this.repository, true);
        Iterator<Package> it = depthFirst.iterator();
        Package previous = it.next();
        while (it.hasNext())
        {
            Package current = it.next();
            if (current._package() != previous)
            {
                String previousPath = PackageableElement.getUserPathForPackageableElement(previous);
                ListIterable<String> previousSplitPath = PackageableElement.splitUserPath(previousPath);
                String currentPath = PackageableElement.getUserPathForPackageableElement(current);
                ListIterable<String> currentSplitPath = PackageableElement.splitUserPath(currentPath);
                Assert.assertTrue("Package " + previousPath + " should not precede " + currentPath + " in a depth first search", previousSplitPath.size() >= currentSplitPath.size());
            }
            previous = current;
        }
    }

    @Test
    public void testBreadthFirst()
    {
        PackageTreeIterable breadthFirst = PackageTreeIterable.newRootPackageTreeIterable(this.repository, false);
        Iterator<Package> it = breadthFirst.iterator();
        Package previous = it.next();
        String previousPath = PackageableElement.getUserPathForPackageableElement(previous);
        ListIterable<String> previousSplitPath = PackageableElement.splitUserPath(previousPath);
        while (it.hasNext())
        {
            Package current = it.next();
            String currentPath = PackageableElement.getUserPathForPackageableElement(current);
            ListIterable<String> currentSplitPath = PackageableElement.splitUserPath(currentPath);
            Assert.assertTrue("Package " + previousPath + " should not precede " + currentPath + " in a breadth first search", previousSplitPath.size() <= currentSplitPath.size());
            previous = current;
            previousPath = currentPath;
            previousSplitPath = currentSplitPath;
        }
    }
}
