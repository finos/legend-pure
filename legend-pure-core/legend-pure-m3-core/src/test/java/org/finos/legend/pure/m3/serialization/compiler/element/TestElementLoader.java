// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.PackageCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElementCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.compiler.ModuleHelper;
import org.finos.legend.pure.m3.serialization.compiler.PureCompilerSerializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FileDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProvider;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.VirtualPackageMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class TestElementLoader extends AbstractReferenceTest
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    private static Path serializationDir;
    private static FileDeserializer fileDeserializer;
    private static MetadataIndex metadataIndex;
    private static MapIterable<String, CoreInstance> elementsByPath;

    private static final FakeElementBuilder elementBuilder = new FakeElementBuilder();

    @BeforeClass
    public static void serialize() throws IOException
    {
        serializationDir = TMP.newFolder("files").toPath();

        ReferenceIdProviders referenceIds = ReferenceIdProviders.builder().withProcessorSupport(processorSupport).withAvailableExtensions().build();
        FileSerializer fileSerializer = FileSerializer.builder()
                .withFilePathProvider(FilePathProvider.builder().withLoadedExtensions().build())
                .withSerializers(ConcreteElementSerializer.builder(processorSupport).withLoadedExtensions().withReferenceIdProviders(referenceIds).build(), ModuleMetadataSerializer.builder().withLoadedExtensions().build())
                .build();
        fileDeserializer = fileSerializer.getDeserializer();

        PureCompilerSerializer.builder()
                .withFileSerializer(fileSerializer)
                .withModuleMetadataGenerator(ModuleMetadataGenerator.fromPureRuntime(runtime))
                .withProcessorSupport(processorSupport)
                .build()
                .serializeAll(serializationDir);

        elementsByPath = GraphTools.getTopLevelAndPackagedElements(processorSupport).toMap(PackageableElement::getUserPathForPackageableElement, e -> e);

        MutableSet<String> allModules = elementsByPath.valuesView().collect(ModuleHelper::getElementModule).select(Objects::nonNull, Sets.mutable.empty());
        metadataIndex = MetadataIndex.builder()
                .withModules(allModules.asLazy().collect(m -> fileDeserializer.deserializeModuleManifest(serializationDir, m)))
                .build();
    }

    @Test
    public void testTestTestPackage()
    {
        testVirtualPackage("test");
    }

    @Test
    public void testTestModelPackage()
    {
        testVirtualPackage("test::model");
    }

    @Test
    public void testSimpleClass()
    {
        testConcreteElement("test::model::SimpleClass", M3Paths.Class);
    }

    @Test
    public void testSimpleEnumeration()
    {
        testConcreteElement("test::model::SimpleEnumeration", M3Paths.Enumeration);
    }

    @Test
    public void testLeftRight()
    {
        testConcreteElement("test::model::LeftRight", M3Paths.Association);
    }

    @Test
    public void testClassWithQualifiedProperties()
    {
        testConcreteElement("test::model::ClassWithQualifiedProperties", M3Paths.Class);
    }

    @Test
    public void testFunc()
    {
        testConcreteElement("test::model::testFunc_T_m__Function_$0_1$__String_m_", M3Paths.ConcreteFunctionDefinition);
    }

    @Test
    public void testFunc2()
    {
        testConcreteElement("test::model::testFunc2__String_1_", M3Paths.ConcreteFunctionDefinition);
    }

    @Test
    public void testFunc3()
    {
        testConcreteElement("test::model::testFunc3__Any_MANY_", M3Paths.ConcreteFunctionDefinition);
    }

    @Test
    public void testFunc4()
    {
        testConcreteElement("test::model::testFunc4_ClassWithMilestoning1_1__ClassWithMilestoning3_MANY_", M3Paths.ConcreteFunctionDefinition);
    }

    @Test
    public void testCurrency()
    {
        testConcreteElement("test::model::Currency", M3Paths.Measure);
    }

    @Test
    public void testMass()
    {
        testConcreteElement("test::model::Mass", M3Paths.Measure);
    }

    @Test
    public void testRangedInt()
    {
        testConcreteElement("test::model::RangedInt", M3Paths.PrimitiveType);
    }

    @Test
    public void testClassWithTypeVariables()
    {
        testConcreteElement("test::model::ClassWithTypeVariables", M3Paths.Class);
    }

    @Test
    public void testNonExistentElements()
    {
        testNonExistentElement("test::model::DoesNotExist");
        testNonExistentElement("test::model::AlsoDoesNotExist");
        testNonExistentElement("ttttt");
        testNonExistentElement("not even a valid path @#$%@&$%^!@#$^%^&?:::");
        testNonExistentElement("");
        testNonExistentElement(null);
    }

    private void testVirtualPackage(String path)
    {
        CoreInstance virtualPackage = processorSupport.package_getByUserPath(path);
        Assert.assertNotNull(path, virtualPackage);
        executeElementLoaderTest(loader ->
        {
            int beforeVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int beforeConcreteElementCount = elementBuilder.getConcreteElementCounter();
            CoreInstance loaded = loader.loadElement(path);
            int afterVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int afterConcreteElementCount = elementBuilder.getConcreteElementCounter();

            Assert.assertEquals(path, beforeVirtualPackageCount + 1, afterVirtualPackageCount);
            Assert.assertEquals(path, beforeConcreteElementCount, afterConcreteElementCount);
            Assert.assertTrue(path, loaded instanceof VirtualPackageWrapper);
            VirtualPackageWrapper virtualPkg = (VirtualPackageWrapper) loaded;
            Assert.assertSame(path, virtualPackage, virtualPkg.getInstance());
            Assert.assertEquals(path, metadataIndex.getPackageMetadata(path), virtualPkg.getMetadata());

            // Try loading a second time: ensure the builder is not invoked again
            Assert.assertSame(path, loaded, loader.loadElement(path));
            Assert.assertEquals(path, beforeVirtualPackageCount + 1, afterVirtualPackageCount);
            Assert.assertEquals(path, beforeConcreteElementCount, afterConcreteElementCount);
        });
    }

    private void testConcreteElement(String path, String classifierPath)
    {
        CoreInstance concreteElement = processorSupport.package_getByUserPath(path);
        Assert.assertNotNull(path, concreteElement);
        executeElementLoaderTest(loader ->
        {
            Assert.assertTrue(path, loader.elementExists(path));
            int beforeVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int beforeConcreteElementCount = elementBuilder.getConcreteElementCounter();
            CoreInstance loaded = loader.loadElement(path);
            int afterVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int afterConcreteElementCount = elementBuilder.getConcreteElementCounter();

            Assert.assertEquals(path, beforeVirtualPackageCount, afterVirtualPackageCount);
            Assert.assertEquals(path, beforeConcreteElementCount + 1, afterConcreteElementCount);
            Assert.assertTrue(path, loaded instanceof FakeConcreteElement);
            FakeConcreteElement concreteElem = (FakeConcreteElement) loaded;
            Assert.assertSame(path, concreteElement, concreteElem.getInstance());
            Assert.assertEquals(path, metadataIndex.getElement(path), concreteElem.getMetadata());
            Assert.assertNull(path, concreteElem.getDeserialized());

            concreteElem.deserialize();
            DeserializedConcreteElement deserialized = concreteElem.getDeserialized();
            Assert.assertEquals(path, deserialized.getPath());
            Assert.assertEquals(path, classifierPath, deserialized.getConcreteElementData().getClassifierPath());
            DeserializedConcreteElement expectedDeserialized = fileDeserializer.deserializeElement(serializationDir, path);
            Assert.assertEquals(path, expectedDeserialized, deserialized);

            // Try loading a second time: ensure the builder is not invoked again
            Assert.assertSame(path, loaded, loader.loadElement(path));
            Assert.assertEquals(path, beforeVirtualPackageCount, elementBuilder.getVirtualPackageCounter());
            Assert.assertEquals(path, beforeConcreteElementCount + 1, elementBuilder.getConcreteElementCounter());
        });
    }

    private void testNonExistentElement(String path)
    {
        executeElementLoaderTest(loader ->
        {
            int beforeVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int beforeConcreteElementCount = elementBuilder.getConcreteElementCounter();

            Assert.assertFalse(path, loader.elementExists(path));
            Assert.assertNull(path, loader.loadElement(path));
            Assert.assertEquals(beforeVirtualPackageCount, elementBuilder.getVirtualPackageCounter());
            Assert.assertEquals(beforeConcreteElementCount, elementBuilder.getConcreteElementCounter());

            IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> loader.loadElementStrict(path));
            Assert.assertEquals((path == null) ? "path may not be null" : ("Element not found: " + path), e.getMessage());
            Assert.assertEquals(beforeVirtualPackageCount, elementBuilder.getVirtualPackageCounter());
            Assert.assertEquals(beforeConcreteElementCount, elementBuilder.getConcreteElementCounter());
        });
    }

    @Test
    public void testTopLevelsAndPackaged()
    {
        executeElementLoaderTest(loader -> elementsByPath.forEachKeyValue((path, e) ->
        {
            Assert.assertTrue(path, loader.elementExists(path));

            int beforeVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int beforeConcreteElementCount = elementBuilder.getConcreteElementCounter();
            CoreInstance loaded = loader.loadElement(path);
            int afterVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int afterConcreteElementCount = elementBuilder.getConcreteElementCounter();

            int expectedAfterVirtualPackageCount = beforeVirtualPackageCount + (e.getSourceInformation() == null ? 1 : 0);
            int expectedAfterConcreteElementCount = beforeConcreteElementCount + (e.getSourceInformation() == null ? 0 : 1);
            Assert.assertEquals(path, expectedAfterVirtualPackageCount, afterVirtualPackageCount);
            Assert.assertEquals(path, expectedAfterConcreteElementCount, afterConcreteElementCount);

            if (e.getSourceInformation() != null)
            {
                Assert.assertTrue(path, loaded instanceof FakeConcreteElement);
                FakeConcreteElement concreteElement = (FakeConcreteElement) loaded;
                Assert.assertSame(path, e, concreteElement.getInstance());
                Assert.assertEquals(path, metadataIndex.getElement(path), concreteElement.getMetadata());
                Assert.assertNull(path, concreteElement.getDeserialized());
            }
            else
            {
                Assert.assertTrue(path, _Package.isPackage(e, processorSupport));
                Assert.assertTrue(path, loaded instanceof VirtualPackageWrapper);
                VirtualPackageWrapper virtualPackage = (VirtualPackageWrapper) loaded;
                Assert.assertSame(path, e, virtualPackage.getInstance());
                Assert.assertEquals(path, metadataIndex.getPackageMetadata(path), virtualPackage.getMetadata());
            }

            // Try loading a second time: ensure the builder is not invoked again
            Assert.assertSame(path, loaded, loader.loadElement(path));
            Assert.assertEquals(path, expectedAfterVirtualPackageCount, afterVirtualPackageCount);
            Assert.assertEquals(path, expectedAfterConcreteElementCount, afterConcreteElementCount);
        }));
    }

    @Test
    public void testConcurrency()
    {
        MutableList<String> paths = Lists.mutable.<String>ofInitialCapacity(elementsByPath.size()).withAll(elementsByPath.keysView());
        int parallelism = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int parallelThreshold = paths.size() / (parallelism * 2);
        class LoadElementAction extends RecursiveTask<MutableMap<String, CoreInstance>>
        {
            private final ElementLoader loader;
            private final int start;
            private final int end;

            LoadElementAction(ElementLoader loader, int start, int end)
            {
                this.loader = loader;
                this.start = start;
                this.end = end;
            }

            @Override
            protected MutableMap<String, CoreInstance> compute()
            {
                if (this.end - this.start <= parallelThreshold)
                {
                    MutableMap<String, CoreInstance> map = Maps.mutable.ofInitialCapacity(this.end - this.start);
                    paths.forEach(this.start, this.end - 1, path -> map.put(path, this.loader.loadElement(path)));
                    return map;
                }

                int mid = (this.start + this.end) / 2;
                LoadElementAction left = new LoadElementAction(this.loader, this.start, mid);
                LoadElementAction right = new LoadElementAction(this.loader, mid, this.end);
                invokeAll(left, right);
                MutableMap<String, CoreInstance> map = left.getRawResult();
                map.putAll(right.getRawResult());
                return map;
            }
        }

        executeElementLoaderTest(loader ->
        {
            int initialVirtualPackageCount = elementBuilder.getVirtualPackageCounter();
            int initialConcreteElementCount = elementBuilder.getConcreteElementCounter();
            Twin<MutableMap<String, CoreInstance>> result;
            ForkJoinPool pool = new ForkJoinPool(parallelism);
            try
            {
                Assert.assertEquals(parallelism, pool.getParallelism());
                result = pool.invoke(new RecursiveTask<Twin<MutableMap<String, CoreInstance>>>()
                {
                    @Override
                    protected Twin<MutableMap<String, CoreInstance>> compute()
                    {
                        LoadElementAction left = new LoadElementAction(loader, 0, paths.size());
                        LoadElementAction right = new LoadElementAction(loader, 0, paths.size());
                        invokeAll(left, right);
                        return Tuples.twin(left.getRawResult(), right.getRawResult());
                    }
                });
            }
            finally
            {
                pool.shutdownNow();
            }
            Assert.assertEquals(result.getOne(), result.getTwo());
            Assert.assertEquals(elementsByPath, result.getOne().collect((path, e) -> Tuples.pair(path, ((ElementWrapper) e).getInstance())));
            int virtualPackageCount = elementsByPath.valuesView().count(e -> e.getSourceInformation() == null);
            Assert.assertEquals(initialVirtualPackageCount + virtualPackageCount, elementBuilder.getVirtualPackageCounter());
            int concreteElementCount = elementsByPath.size() - virtualPackageCount;
            Assert.assertEquals(initialConcreteElementCount + concreteElementCount, elementBuilder.getConcreteElementCounter());
        });
    }

    private static void executeElementLoaderTest(Consumer<? super ElementLoader> test)
    {
        elementBuilder.initLoader.set(null);
        int beforeInitCount = elementBuilder.initCounter.get();
        ElementLoader dirLoader = ElementLoader.builder()
                .withMetadataIndex(metadataIndex)
                .withElementBuilder(elementBuilder)
                .withAvailableReferenceIdExtensions()
                .withFileDeserializer(fileDeserializer)
                .withDirectory(serializationDir)
                .build();
        Assert.assertSame(dirLoader, elementBuilder.initLoader.get());
        Assert.assertEquals(beforeInitCount + 1, elementBuilder.initCounter.get());
        test.accept(dirLoader);
        Assert.assertSame(dirLoader, elementBuilder.initLoader.get());
        Assert.assertEquals(beforeInitCount + 1, elementBuilder.initCounter.get());

        URL url;
        try
        {
            url = serializationDir.toUri().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{url}))
        {
            elementBuilder.initLoader.set(null);
            ElementLoader classLoaderLoader = ElementLoader.builder()
                    .withMetadataIndex(metadataIndex)
                    .withElementBuilder(elementBuilder)
                    .withAvailableReferenceIdExtensions()
                    .withFileDeserializer(fileDeserializer)
                    .withClassLoader(classLoader)
                    .build();
            Assert.assertSame(classLoaderLoader, elementBuilder.initLoader.get());
            Assert.assertEquals(beforeInitCount + 2, elementBuilder.initCounter.get());
            test.accept(classLoaderLoader);
            Assert.assertSame(classLoaderLoader, elementBuilder.initLoader.get());
            Assert.assertEquals(beforeInitCount + 2, elementBuilder.initCounter.get());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static class FakeElementBuilder implements ElementBuilder
    {
        private final AtomicReference<ElementLoader> initLoader = new AtomicReference<>();
        private final AtomicInteger initCounter = new AtomicInteger(0);
        private final AtomicInteger virtualPackageCounter = new AtomicInteger(0);
        private final AtomicInteger concreteElementCounter = new AtomicInteger(0);

        @Override
        public void initialize(ElementLoader loader)
        {
            Assert.assertNotNull(loader);
            this.initCounter.incrementAndGet();
            Assert.assertTrue(this.initLoader.compareAndSet(null, loader));
        }

        @Override
        public CoreInstance buildVirtualPackage(VirtualPackageMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
        {
            this.virtualPackageCounter.incrementAndGet();
            return new VirtualPackageWrapper(metadata);
        }

        @Override
        public CoreInstance buildConcreteElement(ConcreteElementMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
        {
            this.concreteElementCounter.incrementAndGet();
            return new FakeConcreteElement(metadata, deserializer);
        }

        @Override
        public CoreInstance buildComponentInstance(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver)
        {
            throw new UnsupportedOperationException();
        }

        int getVirtualPackageCounter()
        {
            return this.virtualPackageCounter.get();
        }

        int getConcreteElementCounter()
        {
            return this.concreteElementCounter.get();
        }
    }

    private interface ElementWrapper
    {
        CoreInstance getInstance();
    }

    private static class VirtualPackageWrapper extends PackageCoreInstanceWrapper implements ElementWrapper
    {
        private final VirtualPackageMetadata metadata;

        private VirtualPackageWrapper(VirtualPackageMetadata metadata)
        {
            super(Objects.requireNonNull(processorSupport.package_getByUserPath(metadata.getPath()), metadata.getPath()));
            this.metadata = metadata;
        }

        @Override
        public CoreInstance getInstance()
        {
            return this.instance;
        }

        VirtualPackageMetadata getMetadata()
        {
            return this.metadata;
        }
    }

    private static class FakeConcreteElement extends PackageableElementCoreInstanceWrapper implements ElementWrapper
    {
        private final ConcreteElementMetadata metadata;
        private Supplier<? extends DeserializedConcreteElement> deserializer;
        private DeserializedConcreteElement deserialized;

        public FakeConcreteElement(ConcreteElementMetadata metadata, Supplier<? extends DeserializedConcreteElement> deserializer)
        {
            super(Objects.requireNonNull(processorSupport.package_getByUserPath(metadata.getPath()), metadata.getPath()));
            this.metadata = metadata;
            this.deserializer = deserializer;
        }

        @Override
        public CoreInstance getInstance()
        {
            return this.instance;
        }

        ConcreteElementMetadata getMetadata()
        {
            return this.metadata;
        }

        void deserialize()
        {
            if (this.deserializer != null)
            {
                this.deserialized = this.deserializer.get();
                this.deserializer = null;
            }
        }

        DeserializedConcreteElement getDeserialized()
        {
            return this.deserialized;
        }
    }
}
