// Copyright 2024 Goldman Sachs
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


package org.finos.legend.pure.m2.inlinedsl.graph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdGenerator;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReferenceIdGenerator extends AbstractPureTestWithCoreCompiled
{
    private static ReferenceIdGenerator idGenerator;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
        idGenerator = new ReferenceIdGenerator(processorSupport);
    }


    @After
    public void cleanRuntime()
    {
        runtime.delete("test.pure");
        runtime.compile();
    }

    @Test
    public void testAllElements()
    {
        runtime.createInMemorySource("test.pure",
                "Class SimpleClass2\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  id : Integer[1];\n" +
                        "}\n" +
                        "function testFunc4():Any[*]\n" +
                        "{\n" +
                        "  print(#{SimpleClass2{name}}#,1);\n" +
                        "}\n");
        runtime.compile();
        PackageTreeIterable.newRootPackageTreeIterable(processorSupport)
                .forEach(pkg ->
                {
                    if (pkg.getSourceInformation() != null)
                    {
                        assertIds(pkg);
                    }
                    pkg._children().forEach(c ->
                    {
                        if (!(c instanceof Package))
                        {
                            assertIds(c);
                        }
                    });
                });
    }

    private void assertIds(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element)
    {
        MutableMap<CoreInstance, String> idsByInstance = idGenerator.generateIdsForElement(element);
        String path = PackageableElement.getUserPathForPackageableElement(element);
        Assert.assertEquals(path, idsByInstance.get(element));
        Assert.assertSame(element, reverseIdMap(idsByInstance, path).get(path));
    }

    private void assertIds(String path, MutableMap<String, ?> expected)
    {
        validateExpectedIds(path, expected);
        MutableMap<CoreInstance, String> idsByInstance = idGenerator.generateIdsForElement(path);
        MutableMap<String, CoreInstance> instancesById = reverseIdMap(idsByInstance, path);
        if (!expected.equals(instancesById))
        {
            MutableList<Pair<String, ?>> expectedMismatches = Lists.mutable.empty();
            Counter expectedMissing = new Counter();
            Counter mismatches = new Counter();
            Counter unexpected = new Counter();
            expected.forEachKeyValue((id, instance) ->
            {
                CoreInstance actualInstance = instancesById.get(id);
                if (!instance.equals(actualInstance))
                {
                    expectedMismatches.add(Tuples.pair(id, instance));
                    ((actualInstance == null) ? expectedMissing : mismatches).increment();
                }
            });
            MutableList<Pair<String, ?>> actualMismatches = Lists.mutable.empty();
            instancesById.forEachKeyValue((id, instance) ->
            {
                Object expectedInstance = expected.get(id);
                if (!instance.equals(expectedInstance))
                {
                    actualMismatches.add(Tuples.pair(id, instance));
                    if (expectedInstance == null)
                    {
                        unexpected.increment();
                    }
                }
            });
            Assert.assertEquals(
                    "Ids for " + path + " not as expected (" + expectedMissing.getCount() + " expected missing, " + mismatches.getCount() + " mismatches, " + unexpected.getCount() + " unexpected found)",
                    expectedMismatches.sortThis().makeString(System.lineSeparator()),
                    actualMismatches.sortThis().makeString(System.lineSeparator()));
        }
    }

    private void validateExpectedIds(String path, MutableMap<String, ?> expected)
    {
        MutableList<String> nullInstances = Lists.mutable.empty();
        expected.forEachKeyValue((id, instance) ->
        {
            if (instance == null)
            {
                nullInstances.add(id);
            }
        });
        if (nullInstances.notEmpty())
        {
            StringBuilder builder = new StringBuilder("Null instances for ").append(nullInstances.size()).append(" expected ids for \"").append(path).append("\":");
            nullInstances.sortThis().appendString(builder, "\n\t", "\n\t", "");
            Assert.fail(builder.toString());
        }
    }

    private MutableMap<String, CoreInstance> reverseIdMap(MutableMap<CoreInstance, String> idsByInstance, String path)
    {
        MutableMap<String, CoreInstance> instancesById = Maps.mutable.ofInitialCapacity(idsByInstance.size());
        MutableSet<String> duplicateIds = Sets.mutable.empty();
        idsByInstance.forEachKeyValue((instance, id) ->
        {
            if (instancesById.put(id, instance) != null)
            {
                duplicateIds.add(id);
            }
        });
        if (duplicateIds.notEmpty())
        {
            Assert.fail(duplicateIds.toSortedList().makeString("Duplicate ids for " + path + ": \"", "\", \"", "\""));
        }
        return instancesById;
    }

    private Property<?, ?> findProperty(PropertyOwner owner, String name)
    {
        RichIterable<? extends Property<?, ?>> properties = (owner instanceof Class) ? ((Class<?>) owner)._properties() : ((Association) owner)._properties();
        Property<?, ?> property = properties.detect(p -> name.equals(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(p)));
        if (property == null)
        {
            Assert.fail("Could not find property '" + name + "' for " + PackageableElement.getUserPathForPackageableElement(owner));
        }
        return property;
    }

}
