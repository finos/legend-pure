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

package org.finos.legend.pure.m3.tests.packageablelement;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPackageableElement extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getFactoryRegistryOverride());
    }

    @Test
    public void testSplitUserPath()
    {
        Assert.assertEquals(Lists.immutable.with(), PackageableElement.splitUserPath("::"));
        Assert.assertEquals(Lists.immutable.with("meta"), PackageableElement.splitUserPath("meta"));
        Assert.assertEquals(Lists.immutable.with("meta", "pure", "tests", "model", "simple", "Person"), PackageableElement.splitUserPath("meta::pure::tests::model::simple::Person"));
    }

    @Test
    public void testGetUserPathForPackageableElement()
    {
        assertUserPath("Root", "::");
        assertUserPath("Root", "Root");
        _Package.SPECIAL_TYPES.forEach(this::assertUserPath);
        Lists.immutable.with(
                M3Paths.PureZero, M3Paths.ZeroOne, M3Paths.ZeroMany, M3Paths.PureOne, M3Paths.OneMany,
                M3Paths.Class, M3Paths.Association, M3Paths.Enumeration,
                M3Paths.AbstractProperty, M3Paths.Property,
                M3Paths.Multiplicity, M3Paths.PackageableMultiplicity).forEach(this::assertUserPath);
    }

    private void assertUserPath(String path)
    {
        assertUserPath(path, path);
    }

    private void assertUserPath(String expectedPath, String lookupPath)
    {
        Assert.assertEquals(expectedPath, PackageableElement.getUserPathForPackageableElement(lookUpInstance(lookupPath)));
    }

    @Test
    public void testGetUserObjectPathForPackageableElementAsList()
    {
        assertUserPathAsList("Root", "::", true);
        assertUserPathAsList("Root", "Root", true);
        assertUserPathAsList("::", "::", false);
        assertUserPathAsList("::", "Root", false);
        _Package.SPECIAL_TYPES.forEach(path ->
        {
            assertUserPathAsList(path, false);
            assertUserPathAsList(path, true);
        });
        Lists.immutable.with(
                M3Paths.PureZero, M3Paths.ZeroOne, M3Paths.ZeroMany, M3Paths.PureOne, M3Paths.OneMany,
                M3Paths.Class, M3Paths.Association, M3Paths.Enumeration,
                M3Paths.AbstractProperty, M3Paths.Property,
                M3Paths.Multiplicity, M3Paths.PackageableMultiplicity).forEach(path ->
        {
            assertUserPathAsList(path, false);
            assertUserPathAsList("Root::" + path, path, true);
        });
    }

    private void assertUserPathAsList(String path, boolean includeRoot)
    {
        assertUserPathAsList(path, path, includeRoot);
    }

    private void assertUserPathAsList(String expectedPath, String lookupPath, boolean includeRoot)
    {
        ListIterable<String> expectedList = PackageableElement.splitUserPath(expectedPath);
        Assert.assertEquals(expectedPath, expectedList, PackageableElement.getUserObjectPathForPackageableElementAsList(lookUpInstance(lookupPath), includeRoot));
    }

    private CoreInstance lookUpInstance(String path)
    {
        CoreInstance instance = runtime.getCoreInstance(path);
        Assert.assertNotNull(path, instance);
        return instance;
    }

    @Test
    public void testGetSystemPathForPackageableElement()
    {
        assertUserPath("Root", "::");
        assertUserPath("Root", "Root");
        _Package.SPECIAL_TYPES.forEach(path -> assertSystemPath(path, path));
        Lists.immutable.with(
                M3Paths.PureZero, M3Paths.ZeroOne, M3Paths.ZeroMany, M3Paths.PureOne, M3Paths.OneMany,
                M3Paths.Class, M3Paths.Association, M3Paths.Enumeration,
                M3Paths.AbstractProperty, M3Paths.Property,
                M3Paths.Multiplicity, M3Paths.PackageableMultiplicity).forEach(path -> assertSystemPath("Root::" + path, path));
    }

    private void assertSystemPath(String expectedPath, String lookupPath)
    {
        Assert.assertEquals(expectedPath, PackageableElement.getSystemPathForPackageableElement(lookUpInstance(lookupPath)));
    }

    @Test
    public void testGetM4UserPathForPackageableElement()
    {
        _Package.SPECIAL_TYPES.forEach(path -> assertM4UserPath(path, path));

        assertM4UserPath("Root", "::");
        assertM4UserPath("Root", "Root");
        assertM4UserPath("Root.children[meta].children[pure].children[metamodel].children[type].children[Class]", M3Paths.Class);
    }

    private void assertM4UserPath(String expectedPath, String lookupPath)
    {
        Assert.assertEquals(expectedPath, PackageableElement.getM4UserPathForPackageableElement(lookUpInstance(lookupPath)));
    }

    @Test
    public void testGetUserObjectPathForPackageableElement()
    {
        _Package.SPECIAL_TYPES.forEach(path -> assertUserObjectPath(Lists.mutable.with(path), path));
        assertUserObjectPath(Lists.mutable.with("Root"), "::");
        assertUserObjectPath(Lists.mutable.with("Root"), "Root");
        assertUserObjectPath(Lists.mutable.with("Root", "meta", "meta::pure", "meta::pure::metamodel", "meta::pure::metamodel::type", "meta::pure::metamodel::type::Class"), M3Paths.Class);
    }

    private void assertUserObjectPath(ListIterable<String> expectedLookupPaths, String lookupPath)
    {
        Assert.assertEquals(
                expectedLookupPaths.collect(this::lookUpInstance),
                PackageableElement.getUserObjectPathForPackageableElement(lookUpInstance(lookupPath))
        );
    }
}
