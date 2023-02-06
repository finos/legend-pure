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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.m4.transaction.TransactionObserver;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;

public class TrackingTransactionObserver implements TransactionObserver
{
    private final ModelRepository repository;

    private final MutableIntObjectMap<Pair<String, String>> added = IntObjectHashMap.newMap();

    private final MutableMap<String, String> modifiedAfter = Maps.mutable.of();
    private MutableMap<String, String> modifiedBefore = Maps.mutable.of();

    public TrackingTransactionObserver(ModelRepository repository)
    {
        this.repository = repository;
    }


    @Override
    public void added(RichIterable<CoreInstance> instances)
    {
        for (CoreInstance instance : instances)
        {
            this.added.put(instance.getSyntheticId(), Tuples.pair(instance.getClassifier().getName(), getKey(instance)));
        }
    }

    @Override
    public void modified(RichIterable<CoreInstance> instances)
    {
        for (CoreInstance instance : instances)
        {
            this.modifiedAfter.put(getKey(instance), getPrint(instance));
        }
        ModifiedPriorStateCollector collector = new ModifiedPriorStateCollector(this.repository, this.modifiedAfter.keysView());

        try
        {
            Thread t = new Thread(collector);
            t.start();
            t.join();
        }
        catch (InterruptedException e)
        {
            //Ignore
        }

        this.modifiedBefore = collector.getModifiedBefore();
    }

    public void compareToModifiedBefore(ModelRepository repository)
    {
        for (String key : this.modifiedBefore.keysView())
        {

            String value = this.modifiedBefore.get(key);

            if (value != null)
            {
                //System.out.println("Comparing:" + key + " bytes:" + value.getBytes().length);
                CoreInstance instance = getInstanceFromKey(repository, key);
                String now = getPrint(instance);
                Assert.assertEquals(value, now);
            }
//            else
//            {
//                System.out.println("Skipping:" + key);
//            }
        }
    }

    public void compareToAdded(ModelRepository currentRepository)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Writer writer = BinaryWriters.newBinaryWriter(bytes))
        {
            currentRepository.serialize(writer);
        }

        IntObjectMap<CoreInstance> instancesById;
        try (Reader reader = BinaryReaders.newBinaryReader(bytes.toByteArray()))
        {
            instancesById = BinaryRepositorySerializer.build(reader, new ModelRepository());
        }

        MutableList<Object> badStuff = Lists.mutable.of();
        for (int id : this.added.keysView().toArray())
        {
            Object instance = instancesById.get(id);
            if (instance != null)
            {
                badStuff.add(instance);
            }
        }
        Verify.assertEmpty(badStuff);
    }


    public static void compareBytes(byte[] before, byte[] after)
    {
        IntObjectMap<CoreInstance> instancesByIdBefore = BinaryRepositorySerializer.build(before, new ModelRepository());
        IntObjectMap<CoreInstance> instancesByIdAfter = BinaryRepositorySerializer.build(after, new ModelRepository());

        MutableMap<String, MutableList<CoreInstance>> instancesOnlyPresentBefore = Maps.mutable.of();
        instancesByIdBefore.forEachKey(id ->
        {
            CoreInstance instanceBefore = instancesByIdBefore.get(id);
            CoreInstance instance = instancesByIdAfter.get(id);
            if (instance == null)
            {
                String key = getKey(instanceBefore.getClassifier());
                instancesOnlyPresentBefore.getIfAbsentPut(key, Lists.mutable::empty).add(instanceBefore);
            }
        });

        MutableMap<String, MutableList<CoreInstance>> instancesOnlyPresentAfter = Maps.mutable.of();
        instancesByIdAfter.forEachKey(id ->
        {
            CoreInstance instanceAfter = instancesByIdAfter.get(id);
            CoreInstance instance = instancesByIdBefore.get(id);
            if (instance == null)
            {
                String key = getKey(instanceAfter.getClassifier());
                instancesOnlyPresentAfter.getIfAbsentPut(key, Lists.mutable::empty).add(instanceAfter);
            }
        });

        if (instancesOnlyPresentBefore.notEmpty())
        {
            instancesOnlyPresentBefore.forEachKeyValue((key, values1) ->
            {
                ListIterable<CoreInstance> values2 = instancesOnlyPresentAfter.get(key);

                Verify.assertNotNull("Extra element " + values1, values2);
                Verify.assertSize("Different number of " + key, values1.size(), values2);

/*                    String str = values1.collect(new Function<CoreInstance, String>()
                                            {
                                                @Override
                                                public String valueOf(CoreInstance object)
                                                {
                                                    return object.printWithoutDebug("", 2);
                                                }
                                            }).makeString("\n");*/

                if (values1.size() != values2.size())
                {
                    String str1 = values1.collect(object -> object.printWithoutDebug("", 2)).makeString("\n");
                    String str2 = values2.collect(object -> object.printWithoutDebug("", 2)).makeString("\n");
                    Verify.assertEquals(str1, str2);
                }
            });
        }


        if (instancesOnlyPresentBefore.notEmpty())
        {
            instancesOnlyPresentBefore.forEachKeyValue((key, values1) ->
            {
                ListIterable<CoreInstance> values2 = instancesOnlyPresentAfter.get(key);

                Verify.assertNotNull("Extra element " + values1, values2);
                Verify.assertSize("Different number of " + key, values1.size(), values2);
            });
        }

        if (instancesOnlyPresentAfter.notEmpty())
        {
            instancesOnlyPresentAfter.forEachKeyValue((key, values1) ->
            {
                ListIterable<CoreInstance> values2 = instancesOnlyPresentBefore.get(key);

                Verify.assertNotNull("Extra element " + values1, values2);
                Verify.assertSize("Different number of " + key, values1.size(), values2);
            });
        }

        Verify.assertEquals(instancesOnlyPresentBefore.size(), instancesOnlyPresentAfter.size());
    }

    private static String getPrint(CoreInstance instance)
    {
        return instance.printWithoutDebug("", 10);
    }

    private static String getKey(CoreInstance instance)
    {
        ListIterable<String> m4Path;
        if ("Property".equals(instance.getClassifier().getName()))
        {
            m4Path = new M3ProcessorSupport(null).property_getPath(instance);
        }
        else if ("QualifiedProperty".equals(instance.getClassifier().getName()))
        {
            m4Path = Lists.mutable.of(instance.getName());
        }
        else
        {
            String pathString = PackageableElement.getUserPathForPackageableElement(instance, "::");
            m4Path = "Root".equals(pathString) ? Lists.fixedSize.of(pathString) : _Package.convertM3PathToM4(pathString);
        }
        return m4Path.makeString(",");
    }

    private static CoreInstance getInstanceFromKey(ModelRepository repository, String key)
    {
        ListIterable<String> path = StringIterate.tokensToList(key, ",");
        return repository.resolve(path);
    }

    private static class ModifiedPriorStateCollector implements Runnable
    {
        private final ModelRepository repository;
        private final RichIterable<String> keys;
        private final MutableMap<String, String> modifiedBefore = Maps.mutable.of();

        private ModifiedPriorStateCollector(ModelRepository repository, RichIterable<String> keys)
        {
            this.repository = repository;
            this.keys = keys;
        }

        @Override
        public void run()
        {
            for (String key : this.keys)
            {
                CoreInstance instance = getInstanceFromKey(this.repository, key);
                this.modifiedBefore.put(key, getPrint(instance));
            }
        }

        public MutableMap<String, String> getModifiedBefore()
        {
            return this.modifiedBefore;
        }
    }

}
