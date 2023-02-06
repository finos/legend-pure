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

package org.finos.legend.pure.runtime.java.compiled.serialization;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
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
        CoreInstance expected = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::PrimitiveType");
        boolean primitiveClassiferSetForAllPrimitiveTypes = this.metadataLazy.getMetadata("meta::pure::metamodel::type::PrimitiveType").allSatisfy(ci -> ci.getClassifier() == expected);
        Assert.assertTrue(primitiveClassiferSetForAllPrimitiveTypes);
    }

    @Test
    public void testLazyMetaDataClassifierForClasses()
    {
        CoreInstance expected = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::Class");
        CoreInstance ci = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::function::property::Property");
        CoreInstance classifier = ci.getClassifier();
        Assert.assertEquals(expected, classifier);
    }

    @Test
    public void testLazyMetaDataClassifierForEnums()
    {
        CoreInstance expected = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::Enumeration");
        CoreInstance ci = this.metadataLazy.getMetadata("meta::pure::metamodel::type::Enumeration", "Root::meta::pure::metamodel::function::property::AggregationKind");
        CoreInstance classifier = ci.getClassifier();
        Assert.assertEquals(expected, classifier);
    }
}
