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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphDeserializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class TestLazyImplClassifiers extends AbstractPureTestWithCoreCompiled
{
    private Metadata metadataLazy;

    @Before
    public void setUp() throws IOException
    {
        Serialized serialized = GraphSerializer.serializeAll(this.runtime.getCoreInstance("::"), this.processorSupport);
        MutableMap<String, byte[]> fileBytes = Maps.mutable.empty();
        DistributedBinaryGraphSerializer.serialize(serialized, fileBytes);
        DistributedBinaryGraphDeserializer deserializer = DistributedBinaryGraphDeserializer.fromInMemoryByteArrays(fileBytes);
        metadataLazy = new MetadataLazy(getClass().getClassLoader(), deserializer);
    }

    @Test
    public void testLazyMetaDataClassifierPrimitiveType() throws IOException
    {
        final CoreInstance expected = metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::PrimitiveType");
        boolean primitiveClassiferSetForAllPrimitiveTypes = metadataLazy.getMetadata("meta::pure::metamodel::type::PrimitiveType").allSatisfy(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance ci)
            {
                return ci.getClassifier() == expected;
            }
        });
        Assert.assertTrue(primitiveClassiferSetForAllPrimitiveTypes);
    }

    @Test
    public void testLazyMetaDataClassifierForClasses() throws IOException
    {
        final CoreInstance expected = metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::Class");
        CoreInstance ci = metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::function::property::Property");
        CoreInstance classifier = ci.getClassifier();
        Assert.assertEquals(expected, classifier);
    }

    @Test
    public void testLazyMetaDataClassifierForEnums() throws IOException
    {
        final CoreInstance expected = metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::Enumeration");
        CoreInstance ci = metadataLazy.getMetadata("meta::pure::metamodel::type::Enumeration","Root::meta::pure::metamodel::function::property::AggregationKind");
        CoreInstance classifier = ci.getClassifier();
        Assert.assertEquals(expected, classifier);
    }
}
