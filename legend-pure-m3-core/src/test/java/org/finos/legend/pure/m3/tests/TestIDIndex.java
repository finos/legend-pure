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

package org.finos.legend.pure.m3.tests;

import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.PackageCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.PackageInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IDIndex;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class TestIDIndex extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    private static String TEST_PACKAGE = "TestPackage";

    private static IDIndex<String, Package> getIDIndex()
    {
        return IDIndex.newIDIndex(IndexSpecifications.getCoreInstanceNameIndexSpec());
    }

    @Test
    public void testAddInstanceAddWrapper() throws Exception
    {
        IDIndex<String, Package> idIndex = getIDIndex();

        PackageInstance instance = PackageInstance.createPersistent(this.repository, TEST_PACKAGE);
        PackageCoreInstanceWrapper wrapper = new PackageCoreInstanceWrapper(instance);

        idIndex.add(instance);
        idIndex.add(wrapper);

        // This should not throw an exception so long as both instance and wrapper are deemed equal.
    }

    @Test
    public void testAddWrapperAddInstance() throws Exception
    {
        IDIndex<String, Package> idIndex = getIDIndex();

        PackageInstance instance = PackageInstance.createPersistent(this.repository, TEST_PACKAGE);
        PackageCoreInstanceWrapper wrapper = new PackageCoreInstanceWrapper(instance);

        idIndex.add(wrapper);
        idIndex.add(instance);

        // This should not throw an exception so long as both instance and wrapper are deemed equal.
    }

    @Test
    public void testAddInstanceRemoveWrapper() throws Exception
    {
        IDIndex<String, Package> idIndex = getIDIndex();

        PackageInstance instance = PackageInstance.createPersistent(this.repository, TEST_PACKAGE);
        PackageCoreInstanceWrapper wrapper = new PackageCoreInstanceWrapper(instance);

        idIndex.add(instance);
        idIndex.remove(wrapper);

        Package testPackage = idIndex.get(TEST_PACKAGE);

        assertNull(testPackage);
    }

    @Test
    public void testAddWrapperRemoveInstance() throws Exception
    {
        IDIndex<String, Package> idIndex = getIDIndex();

        PackageInstance instance = PackageInstance.createPersistent(this.repository, TEST_PACKAGE);
        PackageCoreInstanceWrapper wrapper = new PackageCoreInstanceWrapper(instance);

        idIndex.add(wrapper);
        idIndex.remove(instance);

        Package testPackage = idIndex.get(TEST_PACKAGE);

        assertNull(testPackage);
    }
}
