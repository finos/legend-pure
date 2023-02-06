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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public abstract class TestDistributedBinaryGraphSerialization extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Test
    public void testFromRuntime() throws IOException
    {
        ListIterable<Obj> expectedObjs = getExpectedObjsFromRuntime();
        testSerialization(DistributedBinaryGraphSerializer.newSerializer(runtime), expectedObjs);
    }

    private ListIterable<Obj> getExpectedObjsFromRuntime()
    {
        MutableSet<CoreInstance> ignoredClassifiers = PrimitiveUtilities.getPrimitiveTypes(repository).toSet();
        ArrayAdapter.adapt(M3Paths.EnumStub, M3Paths.ImportStub, M3Paths.PropertyStub, M3Paths.RouteNodePropertyStub).collect(processorSupport::package_getByUserPath, ignoredClassifiers);
        IdBuilder idBuilder = IdBuilder.newIdBuilder(processorSupport);
        GraphSerializer.ClassifierCaches classifierCaches = new GraphSerializer.ClassifierCaches(processorSupport);
        return GraphNodeIterable.fromModelRepository(repository)
                .reject(i -> ignoredClassifiers.contains(i.getClassifier()))
                .collect(i -> GraphSerializer.buildObj(i, idBuilder, classifierCaches, processorSupport), Lists.mutable.empty());
    }

    private void testSerialization(DistributedBinaryGraphSerializer serializer, ListIterable<Obj> expectedObjs, String... metadataNames) throws IOException
    {
        // Serialize
        serializer.serialize(getFileWriter());

        // Deserialize
        DistributedBinaryGraphDeserializer.Builder deserializerBuilder = DistributedBinaryGraphDeserializer.newBuilder(getFileReader());
        if ((metadataNames == null) || (metadataNames.length == 0))
        {
            deserializerBuilder.withNoMetadataName();
        }
        else
        {
            deserializerBuilder.withMetadataNames(metadataNames);
        }
        DistributedBinaryGraphDeserializer deserializer = deserializerBuilder.build();

        // Validate classifiers
        ListMultimap<String, Obj> objsByClassifier = expectedObjs.groupBy(Obj::getClassifier);
        Assert.assertEquals(objsByClassifier.keysView().toSortedList().makeString("\n"), deserializer.getClassifiers().toSortedList().makeString("\n"));
        Assert.assertEquals(Lists.fixedSize.empty(), objsByClassifier.keysView().reject(deserializer::hasClassifier, Lists.mutable.empty()));

        // Validate instances by classifier
        for (String classifierId : objsByClassifier.keysView())
        {
            MutableList<Obj> instances = objsByClassifier.get(classifierId).toSortedListBy(Obj::getIdentifier);
            MutableList<String> instanceIds = instances.collect(Obj::getIdentifier);
            Assert.assertEquals(classifierId, instanceIds.makeString("\n"), deserializer.getClassifierInstanceIds(classifierId).toSortedList().makeString("\n"));
            Assert.assertEquals(classifierId, instances, deserializer.getInstances(classifierId, instanceIds).toSortedListBy(Obj::getIdentifier));
        }

        // Validate all individual objs
        for (Obj obj : expectedObjs)
        {
            String classifierId = obj.getClassifier();
            String identifier = obj.getIdentifier();
            Assert.assertTrue(classifierId + " / " + identifier, deserializer.hasInstance(classifierId, identifier));
            Assert.assertEquals(obj, deserializer.getInstance(classifierId, identifier));
        }
    }

    protected abstract FileWriter getFileWriter() throws IOException;

    protected abstract FileReader getFileReader() throws IOException;
}
