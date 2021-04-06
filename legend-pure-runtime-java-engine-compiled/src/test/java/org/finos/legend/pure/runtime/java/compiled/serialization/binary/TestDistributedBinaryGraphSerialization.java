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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class TestDistributedBinaryGraphSerialization extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testSerializationAndDeserialization() throws IOException
    {
        // Get serialized
        Serialized serialized = GraphSerializer.serializeAll(runtime.getCoreInstance("::"), processorSupport);

        // Serialize
        MutableMap<String, byte[]> fileBytes = Maps.mutable.empty();
        DistributedBinaryGraphSerializer.serialize(serialized, fileBytes);

        // Deserialize
        DistributedBinaryGraphDeserializer deserializer = DistributedBinaryGraphDeserializer.fromInMemoryByteArrays(fileBytes);

        Multimap<String, Obj> objsByClassifier = serialized.getObjects().groupBy(Obj::getClassifier);

        // Validate classifiers
        Verify.assertSetsEqual(objsByClassifier.keysView().toSet(), deserializer.getClassifiers().toSet());
        for (String classifierId : objsByClassifier.keysView())
        {
            Assert.assertTrue(classifierId, deserializer.hasClassifier(classifierId));
        }

        // Validate instances by classifier
        for (String classifierId : objsByClassifier.keysView())
        {
            MutableSet<Obj> instances = objsByClassifier.get(classifierId).toSet();
            MutableSet<String> instanceIds = instances.collect(Obj::getIdentifier);
            Verify.assertSetsEqual(classifierId, instanceIds, deserializer.getClassifierInstanceIds(classifierId).toSet());
            Verify.assertSetsEqual(classifierId, instances, deserializer.getInstances(classifierId, instanceIds).toSet());
        }

        // Validate all individual objs
        for (Obj obj : serialized.getObjects())
        {
            String classifierId = obj.getClassifier();
            String identifier = obj.getIdentifier();
            Assert.assertTrue(classifierId + " / " + identifier, deserializer.hasInstance(classifierId, identifier));
            Assert.assertEquals(obj, deserializer.getInstance(classifierId, identifier));
        }
    }
}
