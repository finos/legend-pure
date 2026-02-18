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

package org.finos.legend.pure.runtime.java.compiled.runtime.serialization.binary;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.PrintPureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJar;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphDeserializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedMetadataHelper;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.FileReader;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.FileWriter;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValueVisitor;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public abstract class TestDistributedBinaryGraphSerialization
{
    @Test
    public void testFullSerialization() throws IOException
    {
        PureRuntime runtime = buildRuntime();
        DistributedBinaryGraphSerializer.newSerializer(runtime).serialize(getFileWriter());
        ListIterable<Obj> expectedObjs = getExpectedObjsFromRuntime(runtime, IdBuilder.legacyBuilder(runtime.getProcessorSupport()).build());
        testSerialization(expectedObjs, Lists.immutable.empty(), true);
    }

    @Test
    public void testModularSerialization() throws IOException
    {
        String repo = "platform";
        PureRuntime runtime = buildRuntime(repo);

        DistributedBinaryGraphSerializer.newSerializer(runtime, repo).serialize(getFileWriter());
        ListIterable<Obj> expectedObjs = getExpectedObjsFromRuntime(runtime, IdBuilder.builder(runtime.getProcessorSupport()).withDefaultIdPrefix(DistributedMetadataHelper.getMetadataIdPrefix(repo)).build());
        testSerialization(expectedObjs, Lists.immutable.with(repo), false);
    }

    private PureRuntime buildRuntime(String... repos)
    {
        CodeRepositorySet.Builder repoSetBuilder = CodeRepositorySet.builder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories());
        if ((repos != null) && (repos.length > 0))
        {
            repoSetBuilder.subset(repos);
        }
        CodeRepositorySet repoSet = repoSetBuilder.build();
        MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(repoSet.getRepositories()));
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(new PrintPureRuntimeStatus(System.out))
                .setTransactionalByDefault(false)
                .build();
        MutableList<PureRepositoryJar> repoJars = GraphLoader.findJars(repoSet.getRepositoryNames(), Thread.currentThread().getContextClassLoader(), null, false);
        if (repoJars.isEmpty())
        {
            runtime.initialize();
        }
        else
        {
            try
            {
                GraphLoader loader = new GraphLoader(runtime.getModelRepository(), runtime.getContext(), runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), null, SimplePureRepositoryJarLibrary.newLibrary(repoJars));
                loader.loadAll();
                if (repoJars.size() < repoSet.size())
                {
                    MutableSet<String> found = repoJars.collect(j -> j.getMetadata().getRepositoryName(), Sets.mutable.ofInitialCapacity(repoJars.size()));
                    System.out.println(repoSet.getRepositoryNames().reject(found::contains, Lists.mutable.empty()).sortThis().makeString("Missing caches for: ", ", ", ""));
                    runtime.compile();
                }
                System.out.println("Initialized runtime from caches");
            }
            catch (Exception e)
            {
                System.out.println("Failed to initialize runtime from caches, will try to initialize from source");
                e.printStackTrace();

                runtime = new PureRuntimeBuilder(codeStorage)
                        .withRuntimeStatus(new PrintPureRuntimeStatus(System.out))
                        .setTransactionalByDefault(false)
                        .buildAndInitialize();
            }
        }
        return runtime;
    }

    private ListIterable<Obj> getExpectedObjsFromRuntime(PureRuntime runtime, IdBuilder idBuilder)
    {
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        MutableSet<CoreInstance> ignoredClassifiers = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();
        ArrayAdapter.adapt(M3Paths.EnumStub, M3Paths.ImportStub, M3Paths.PropertyStub, M3Paths.RouteNodePropertyStub).collect(processorSupport::package_getByUserPath, ignoredClassifiers);
        GraphSerializer.ClassifierCaches classifierCaches = new GraphSerializer.ClassifierCaches(processorSupport);
        return GraphNodeIterable.fromModelRepository(runtime.getModelRepository())
                .reject(i -> ignoredClassifiers.contains(i.getClassifier()))
                .collect(i -> GraphSerializer.buildObj(i, idBuilder, classifierCaches, processorSupport), Lists.mutable.empty());
    }

    private void testSerialization(ListIterable<Obj> expectedObjs, ListIterable<String> metadataNames, boolean strictForPackages) throws IOException
    {
        // Deserialize
        DistributedBinaryGraphDeserializer.Builder deserializerBuilder = DistributedBinaryGraphDeserializer.newBuilder(getFileReader());
        if ((metadataNames == null) || metadataNames.isEmpty())
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
            if (strictForPackages || !M3Paths.Package.equals(classifierId))
            {
                Assert.assertEquals(classifierId, instances, deserializer.getInstances(classifierId, instanceIds).toSortedListBy(Obj::getIdentifier));
            }
            else
            {
                Assert.assertEquals(classifierId, instances.collect(this::normalizeObj), deserializer.getInstances(classifierId, instanceIds).collect(this::normalizeObj, Lists.mutable.empty()).sortThisBy(Obj::getIdentifier));
            }
        }

        // Validate all individual objs
        for (Obj obj : expectedObjs)
        {
            String classifierId = obj.getClassifier();
            String identifier = obj.getIdentifier();
            Assert.assertTrue(classifierId + " / " + identifier, deserializer.hasInstance(classifierId, identifier));
            Obj found = deserializer.getInstance(classifierId, identifier);
            if ((found == null) || strictForPackages || !M3Paths.Package.equals(classifierId))
            {
                Assert.assertEquals(obj, deserializer.getInstance(classifierId, identifier));
            }
            else
            {
                Assert.assertEquals(normalizeObj(obj), normalizeObj(deserializer.getInstance(classifierId, identifier)));
            }
        }
    }

    private Obj normalizeObj(Obj obj)
    {
        MutableList<PropertyValue> normalizedPropertyValues = normalizePropertyValues(obj);
        return normalizedPropertyValues.equals(obj.getPropertyValues()) ?
               obj :
               Obj.newObj(obj.getClassifier(), obj.getIdentifier(), obj.getName(), normalizedPropertyValues, obj.getSourceInformation(), obj.isEnum());
    }

    private MutableList<PropertyValue> normalizePropertyValues(Obj obj)
    {
        MutableList<PropertyValue> normalized = obj.getPropertyValues().toSortedListBy(PropertyValue::getProperty);
        int childrenIndex;
        if (M3Paths.Package.equals(obj.getClassifier()) && ((childrenIndex = normalized.detectIndex(pv -> M3Properties.children.equals(pv.getProperty()))) >= 0))
        {
            PropertyValue children = normalized.get(childrenIndex);
            if (children instanceof PropertyValueMany)
            {
                PropertyValueMany childrenMany = (PropertyValueMany) children;
                RValueVisitor<Pair<String, String>> visitor = new RValueVisitor<Pair<String, String>>()
                {
                    @Override
                    public Pair<String, String> visit(Primitive primitive)
                    {
                        return Tuples.pair("", primitive.toString());
                    }

                    @Override
                    public Pair<String, String> visit(ObjRef objRef)
                    {
                        return Tuples.pair(objRef.getClassifierId(), objRef.toString());
                    }

                    @Override
                    public Pair<String, String> visit(EnumRef enumRef)
                    {
                        return Tuples.pair(enumRef.getEnumerationId(), enumRef.getEnumName());
                    }
                };
                normalized.set(childrenIndex, new PropertyValueMany(children.getProperty(), childrenMany.getValues().toSortedListBy(p -> p.visit(visitor))));
            }
        }
        return normalized;
    }

    protected abstract FileWriter getFileWriter() throws IOException;

    protected abstract FileReader getFileReader() throws IOException;
}
