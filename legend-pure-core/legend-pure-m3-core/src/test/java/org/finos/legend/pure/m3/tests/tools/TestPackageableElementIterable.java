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

package org.finos.legend.pure.m3.tests.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m3.tools.PackageableElementIterable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPackageableElementIterable extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testEmpty()
    {
        Assert.assertEquals(Lists.immutable.empty(), PackageableElementIterable.builder().build().toList());
    }

    @Test
    public void testAllFromTopLevels_Repository()
    {
        MutableList<String> expected = GraphTools.getTopLevelNames().reject(M3Paths.Root::equals, Lists.mutable.empty()).sortThis();
        PackageTreeIterable.newRootPackageTreeIterable(repository, false).forEach(pkg ->
        {
            expected.add(PackageableElement.getUserPathForPackageableElement(pkg));
            pkg._children().collectIf(c -> !(c instanceof Package), PackageableElement::getUserPathForPackageableElement, expected);
        });
        Assert.assertEquals(expected, PackageableElementIterable.builder().withTopLevels(repository).build().collect(PackageableElement::getUserPathForPackageableElement).toList());
    }

    @Test
    public void testAllFromTopLevels_ProcessorSupport()
    {
        MutableList<String> expected = GraphTools.getTopLevelNames().reject(M3Paths.Root::equals, Lists.mutable.empty()).sortThis();
        PackageTreeIterable.newRootPackageTreeIterable(processorSupport, false).forEach(pkg ->
        {
            expected.add(PackageableElement.getUserPathForPackageableElement(pkg));
            pkg._children().collectIf(c -> !(c instanceof Package), PackageableElement::getUserPathForPackageableElement, expected);
        });
        Assert.assertEquals(expected, PackageableElementIterable.builder().withTopLevels(processorSupport).build().collect(PackageableElement::getUserPathForPackageableElement).toList());
    }

    @Test
    public void testPackageFilter()
    {
        MutableList<String> expected = GraphTools.getTopLevelNames().reject(M3Paths.Root::equals, Lists.mutable.empty()).sortThis();
        Package root = (Package) processorSupport.package_getByUserPath(M3Paths.Root);
        expected.add(M3Paths.Root);
        Assert.assertEquals(expected, PackageableElementIterable.builder().withTopLevels(processorSupport).withPackageFilter(p -> false).build().collect(PackageableElement::getUserPathForPackageableElement).toList());

        root._children().collect(PackageableElement::getUserPathForPackageableElement, expected);
        Assert.assertEquals(expected, PackageableElementIterable.builder().withTopLevels(processorSupport).withPackageFilter(p -> p == root).build().collect(PackageableElement::getUserPathForPackageableElement).toList());
    }

    @Test
    public void testMetaPure()
    {
        MutableList<String> expected = Lists.mutable.empty();
        Package metaPure = (Package) processorSupport.package_getByUserPath("meta::pure");
        PackageTreeIterable.newPackageTreeIterable(metaPure, false).forEach(pkg ->
        {
            expected.add(PackageableElement.getUserPathForPackageableElement(pkg));
            pkg._children().collectIf(c -> !(c instanceof Package), PackageableElement::getUserPathForPackageableElement, expected);
        });

        Assert.assertEquals(expected, PackageableElementIterable.builder().withPackage(metaPure).build().collect(PackageableElement::getUserPathForPackageableElement).toList());
        Assert.assertEquals(expected, PackageableElementIterable.builder().withElement(metaPure).build().collect(PackageableElement::getUserPathForPackageableElement).toList());
    }
}
