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

package org.finos.legend.pure.runtime.java.compiled.runtime.serialization;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphDeserializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TestLazyImplClassifiers extends AbstractPureTestWithCoreCompiled
{
    private Metadata metadataLazy;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Before
    public void setUpLazyMetaData()
    {
        MutableMap<String, byte[]> fileBytes = Maps.mutable.empty();
        DistributedBinaryGraphSerializer.newSerializer(runtime).serializeToInMemoryByteArrays(fileBytes);
        DistributedBinaryGraphDeserializer deserializer = DistributedBinaryGraphDeserializer.newBuilder(fileBytes).build();
        this.metadataLazy = MetadataLazy.newMetadata(Thread.currentThread().getContextClassLoader(), deserializer);
    }

    @Test
    public void testLazyMetaDataClassifierPrimitiveType()
    {
        CoreInstance expected = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "meta::pure::metamodel::type::PrimitiveType");
        MutableList<String> invalidPrimitiveTypes = this.metadataLazy.getClassifierInstances("meta::pure::metamodel::type::PrimitiveType")
                .collectIf(ci -> ci.getClassifier() != expected, CoreInstance::getName, Lists.mutable.empty());
        Assert.assertEquals("primitive types with the wrong classifier", Lists.fixedSize.empty(), invalidPrimitiveTypes);
    }

    @Test
    public void testLazyMetaDataClassifierForClasses()
    {
        CoreInstance expected = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "meta::pure::metamodel::type::Class");
        CoreInstance ci = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "meta::pure::metamodel::function::property::Property");
        CoreInstance classifier = ci.getClassifier();
        Assert.assertEquals(expected, classifier);
    }

    @Test
    public void testLazyMetaDataClassifierForEnums()
    {
        CoreInstance expected = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "meta::pure::metamodel::type::Enumeration");
        CoreInstance ci = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Enumeration", "meta::pure::metamodel::function::property::AggregationKind");
        CoreInstance classifier = ci.getClassifier();
        Assert.assertEquals(expected, classifier);
    }

    @Test
    public void testLazyMetaDataClassifierForGenericTypes()
    {
        CoreInstance alpha = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::PrimitiveType");
        MapIterable<String, CoreInstance> bravo = this.metadataLazy.getMetadata("meta::pure::metamodel::type::generics::GenericType");
        System.out.println();

        long count = bravo.keysView().sumOfInt(k -> k.getBytes(StandardCharsets.UTF_8).length);
        long size = bravo.keysView().size();

        System.out.println("GenericType keys size = " + size + ", bytes = " + count);
        //GenericType keys size = 44788, bytes = 237039     <-synthId - hash
        //GenericType keys size = 44788, bytes = 492668     <-synthId + hash

        //GenericType keys size = 43683, bytes = 7,436,293  <-kevId - hash
        //GenericType keys size = 43683, bytes = 480513     <-kevId + hash
    }
}
