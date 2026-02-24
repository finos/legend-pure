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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
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
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class AbstractElementBuilderTest<VP extends CoreInstance, CE extends CoreInstance, CI extends CoreInstance> extends AbstractReferenceTest
{
    @ClassRule
    public static TemporaryFolder TMP = new TemporaryFolder();

    private static Path serializationDir;
    private static FileDeserializer fileDeserializer;
    private static MetadataIndex metadataIndex;

    protected ModelRepository elementModelRepository;
    protected ElementBuilderWrapper elementBuilder;
    protected ElementLoader elementLoader;
    protected ProcessorSupport elementProcessorSupport;

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

        metadataIndex = MetadataIndex.builder()
                .withModules(runtime.getCodeStorage().getAllRepositories().collect(r -> fileDeserializer.deserializeModuleManifest(serializationDir, r.getName())))
                .build();
    }

    @Before
    public void setUpElementBuilderAndLoader()
    {
        this.elementModelRepository = new ModelRepository();
        this.elementBuilder = new ElementBuilderWrapper(newElementBuilder());
        this.elementLoader = ElementLoader.builder()
                .withMetadataIndex(metadataIndex)
                .withElementBuilder(this.elementBuilder)
                .withAvailableReferenceIdExtensions()
                .withFileDeserializer(fileDeserializer)
                .withDirectory(serializationDir)
                .build();
        this.elementProcessorSupport = new ElementLoaderProcessorSupport(this.elementModelRepository, this.elementLoader);
    }

    @Test
    public void testAllTopLevelAndPackagedElements()
    {
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            String path = PackageableElement.getUserPathForPackageableElement(element);
            if (element.getSourceInformation() == null)
            {
                assertLoadVirtualPackage(path, false);
            }
            else
            {
                String classifierPath = PackageableElement.getUserPathForPackageableElement(element.getClassifier());
                assertLoadConcreteElement(path, classifierPath, false);
            }
        });
    }

    @Test
    public void testPrimitiveValueClassifiers()
    {
        CoreInstance stringType = this.elementLoader.loadElement(M3Paths.String);
        CoreInstance stringTypeName = stringType.getValueForMetaPropertyToOne(M3Properties.name);
        Assert.assertSame(stringType, stringTypeName.getClassifier());
    }

    @Test
    public void testBuildRootPackage()
    {
        testConcretePackage(M3Paths.Root);
    }

    @Test
    public void testBuildMetaPackage()
    {
        testConcretePackage("meta");
    }

    @Test
    public void testBuildMetaPurePackage()
    {
        testConcretePackage("meta::pure");
    }

    @Test
    public void testBuildTestPackage()
    {
        testVirtualPackage("test");
    }

    @Test
    public void testBuildTestModelPackage()
    {
        testVirtualPackage("test::model");
    }

    private void testConcretePackage(String path)
    {
        testConcreteElement(path, M3Paths.Package);
    }

    @Test
    public void testBuildClassClass()
    {
        testClass(M3Paths.Class);
    }

    @Test
    public void testBuildAssociationClass()
    {
        testClass(M3Paths.Association);
    }

    @Test
    public void testBuildPackageClass()
    {
        testClass(M3Paths.Package);
    }

    @Test
    public void testBuildPrimitiveTypeClass()
    {
        testClass(M3Paths.PrimitiveType);
    }

    @Test
    public void testBuildSimpleClass()
    {
        testClass("test::model::SimpleClass");
    }

    @Test
    public void testBuildClassWithQualifiedProperties()
    {
        testClass("test::model::ClassWithQualifiedProperties");
    }

    @Test
    public void testBuildClassWithTypeVariables()
    {
        testClass("test::model::ClassWithTypeVariables");
    }

    private void testClass(String path)
    {
        testConcreteElement(path, M3Paths.Class);
    }

    @Test
    public void testBuildInteger()
    {
        testPrimitiveType(M3Paths.Integer);
    }

    @Test
    public void testBuildNumber()
    {
        testPrimitiveType(M3Paths.Number);
    }

    @Test
    public void testBuildString()
    {
        testPrimitiveType(M3Paths.String);
    }

    @Test
    public void testBuildRangedInt()
    {
        testPrimitiveType("test::model::RangedInt");
    }

    private void testPrimitiveType(String path)
    {
        testConcreteElement(path, M3Paths.PrimitiveType);
    }

    @Test
    public void testBuildAggregationKind()
    {
        testEnumeration(M3Paths.AggregationKind);
    }

    @Test
    public void testBuildSimpleEnumeration()
    {
        testEnumeration("test::model::SimpleEnumeration");
    }

    private void testEnumeration(String path)
    {
        testConcreteElement(path, M3Paths.Enumeration);
    }

    @Test
    public void testBuildLeftRightAssociation()
    {
        testAssociation("test::model::LeftRight");
    }

    @Test
    public void testBuildAssociationWithMilestoning1()
    {
        testAssociation("test::model::AssociationWithMilestoning1");
    }

    @Test
    public void testBuildAssociationWithMilestoning2()
    {
        testAssociation("test::model::AssociationWithMilestoning2");
    }

    @Test
    public void testBuildAssociationWithMilestoning3()
    {
        testAssociation("test::model::AssociationWithMilestoning3");
    }

    private void testAssociation(String path)
    {
        testConcreteElement(path, M3Paths.Association);
    }

    @Test
    public void testBuildMass()
    {
        testMeasure("test::model::Mass");
    }

    @Test
    public void testBuildCurrency()
    {
        testMeasure("test::model::Currency");
    }

    private void testMeasure(String path)
    {
        testConcreteElement(path, M3Paths.Measure);
    }

    @Test
    public void testBuildTestFunc()
    {
        testConcreteFunctionDefinition("test::model::testFunc_T_m__Function_$0_1$__String_m_");
    }

    @Test
    public void testBuildTestFunc2()
    {
        testConcreteFunctionDefinition("test::model::testFunc2__String_1_");
    }

    @Test
    public void testBuildTestFunc3()
    {
        testConcreteFunctionDefinition("test::model::testFunc3__Any_MANY_");
    }

    @Test
    public void testBuildTestFunc4()
    {
        testConcreteFunctionDefinition("test::model::testFunc4_ClassWithMilestoning1_1__ClassWithMilestoning3_MANY_");
    }

    private void testConcreteFunctionDefinition(String path)
    {
        testConcreteElement(path, M3Paths.ConcreteFunctionDefinition);
    }

    protected void testConcreteElement(String path, String classifierPath)
    {
        testConcreteElement(path, classifierPath, true);
    }

    protected void testConcreteElement(String path, String classifierPath, boolean requireNotLoaded)
    {
        CE element = assertLoadConcreteElement(path, classifierPath, requireNotLoaded);
        testConcreteElement(path, classifierPath, element);
    }

    protected void testVirtualPackage(String path)
    {
        VP element = assertLoadVirtualPackage(path);
        testVirtualPackage(path, element);
    }

    protected abstract ElementBuilder newElementBuilder();

    protected abstract Class<? extends VP> getExpectedVirtualPackageClass();

    protected abstract Class<? extends CE> getExpectedConcreteElementClass(String classifierPath);

    protected abstract Class<? extends CI> getExpectedComponentInstanceClass(String classifierPath);

    protected void testConcreteElement(String path, String classifierPath, CE element)
    {
        CoreInstance srcElement = runtime.getCoreInstance(path);
        Assert.assertNotNull(path, srcElement);

        CoreInstance classifierGenericType = element.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        CoreInstance srcClassifierGenericType = srcElement.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        if (classifierGenericType == null)
        {
            Assert.assertNull(path, srcClassifierGenericType);
        }
        else
        {
            assertComponentInstanceClass(path, M3Paths.GenericType, classifierGenericType);
            Assert.assertEquals(path, printGenericType(srcClassifierGenericType, true), printGenericType(classifierGenericType, false));
        }
    }

    protected void testVirtualPackage(String path, VP element)
    {
        CoreInstance classifierGenericType = element.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        if (classifierGenericType != null)
        {
            assertComponentInstanceClass(path, M3Paths.GenericType, classifierGenericType);
            Assert.assertEquals(path, M3Paths.Package, printGenericType(classifierGenericType, false));
        }
    }

    protected VP assertLoadVirtualPackage(String path)
    {
        return assertLoadVirtualPackage(path, true);
    }

    @SuppressWarnings("unchecked")
    protected VP assertLoadVirtualPackage(String path, boolean requireNotLoaded)
    {
        if (requireNotLoaded)
        {
            Assert.assertFalse(path, this.elementBuilder.virtualPackages.contains(path));
        }
        CoreInstance element = this.elementLoader.loadElement(path);
        Assert.assertNotNull(path, element);
        Assert.assertTrue(path, this.elementBuilder.virtualPackages.contains(path));
        assertVirtualPackageClass(path, element);
        assertPackageableElement(path, M3Paths.Package, element);
        return (VP) element;
    }

    protected CE assertLoadConcreteElement(String path, String classifierPath)
    {
        return assertLoadConcreteElement(path, classifierPath, true);
    }

    @SuppressWarnings("unchecked")
    protected CE assertLoadConcreteElement(String path, String classifierPath, boolean requireNotLoaded)
    {
        if (requireNotLoaded)
        {
            Assert.assertFalse(path, this.elementBuilder.concreteElements.contains(path));
        }
        CoreInstance element = this.elementLoader.loadElement(path);
        Assert.assertNotNull(path, element);
        Assert.assertTrue(path, this.elementBuilder.concreteElements.contains(path));
        assertConcreteElementClass(path, classifierPath, element);
        assertPackageableElement(path, classifierPath, element);
        return (CE) element;
    }

    private void assertPackageableElement(String path, String classifierPath, CoreInstance element)
    {
        // This asserts things about a PackageableElement which can be accessed without deserializing the element

        // full system path
        if (element instanceof Any)
        {
            Assert.assertEquals(path, GraphTools.isTopLevelName(classifierPath) ? classifierPath : ("Root::" + classifierPath), ((Any) element).getFullSystemPath());
        }

        // source element
        CoreInstance srcElement = runtime.getCoreInstance(path);
        Assert.assertNotNull(path, srcElement);

        // name and package
        String name = PrimitiveUtilities.getStringValue(element.getValueForMetaPropertyToOne(M3Properties.name));
        CoreInstance pkg = element.getValueForMetaPropertyToOne(M3Properties._package);
        int lastColon = path.lastIndexOf(':');
        if (lastColon == -1)
        {
            Assert.assertEquals(path, name);
            Assert.assertEquals(path, element.getName());
            if (GraphTools.isTopLevelName(path))
            {
                Assert.assertNull(path, pkg);
            }
            else
            {
                assertConcreteElementClass(path, M3Paths.Package, pkg);
                Assert.assertEquals(path, M3Paths.Root, getUserPath(pkg));
                Assert.assertSame(path, this.elementLoader.loadElement(M3Paths.Root), pkg);
                Assert.assertTrue(path, this.elementBuilder.isConcreteElementLoaded(M3Paths.Root));
            }
        }
        else
        {
            String expectedName = path.substring(lastColon + 1);
            String expectedPackagePath = path.substring(0, lastColon - 1);
            Assert.assertEquals(path, expectedName, name);
            Assert.assertEquals(path, expectedName, element.getName());
            Assert.assertEquals(path, expectedPackagePath, getUserPath(pkg));
            if (this.elementBuilder.isConcreteElementLoaded(expectedPackagePath))
            {
                assertConcreteElementClass(path, M3Paths.Package, pkg);
            }
            else if (this.elementBuilder.isVirtualPackageLoaded(expectedPackagePath))
            {
                assertVirtualPackageClass(path, pkg);
            }
            else
            {
                Assert.fail("Package for " + path + " not loaded");
            }
            Assert.assertSame(path, this.elementLoader.loadElement(expectedPackagePath), pkg);
        }

        // source information
        Assert.assertEquals(path, srcElement.getSourceInformation(), element.getSourceInformation());

        // classifier
        CoreInstance classifier = element.getClassifier();
        Assert.assertEquals(path, classifierPath, getUserPath(classifier));
        Assert.assertTrue(path, this.elementBuilder.isConcreteElementLoaded(classifierPath));
        Assert.assertSame(this.elementLoader.loadElement(classifierPath), classifier);

        // Java class
        Assert.assertSame(getExpectedConcreteElementClass(M3Paths.Class), classifier.getClass());
    }

    protected void assertVirtualPackageClass(String message, CoreInstance pkg)
    {
        assertClass(message, getExpectedVirtualPackageClass(), pkg);
    }

    protected void assertVirtualPackageClass(String message, Iterable<? extends CoreInstance> packages)
    {
        assertClass(message, getExpectedVirtualPackageClass(), packages);
    }

    protected void assertConcreteElementClass(String message, String classifierPath, CoreInstance element)
    {
        assertClass(message, getExpectedConcreteElementClass(classifierPath), element);
    }

    protected void assertConcreteElementClass(String message, String classifierPath, Iterable<? extends CoreInstance> elements)
    {
        assertClass(message, getExpectedConcreteElementClass(classifierPath), elements);
    }

    protected void assertComponentInstanceClass(String message, String classifierPath, CoreInstance instance)
    {
        assertClass(message, getExpectedComponentInstanceClass(classifierPath), instance);
    }

    protected void assertComponentInstanceClass(String message, String classifierPath, Iterable<? extends CoreInstance> instances)
    {
        assertClass(message, getExpectedComponentInstanceClass(classifierPath), instances);
    }

    protected void assertClass(String message, Class<?> expectedJavaClass, CoreInstance instance)
    {
        Assert.assertSame(message, expectedJavaClass, instance.getClass());
    }

    protected void assertClass(String message, Class<?> expectedJavaClass, Iterable<? extends CoreInstance> instances)
    {
        MutableSet<Class<?>> actualJavaClasses = Iterate.collect(instances, Object::getClass, Sets.mutable.empty());
        if (actualJavaClasses.size() == 1)
        {
            Assert.assertSame(message, expectedJavaClass, actualJavaClasses.getAny());
        }
        else if (actualJavaClasses.size() > 1)
        {
            Assert.assertEquals(message, Sets.fixedSize.with(expectedJavaClass), actualJavaClasses);
        }
    }

    protected String printGenericType(CoreInstance genericType, boolean sourceModel)
    {
        return GenericType.print(genericType, true, getProcessorSupport(sourceModel));
    }

    protected ProcessorSupport getProcessorSupport(boolean sourceModel)
    {
        return sourceModel ? processorSupport : this.elementProcessorSupport;
    }

    protected static String getUserPath(CoreInstance element)
    {
        return PackageableElement.getUserPathForPackageableElement(element);
    }

    protected static StringBuilder appendUserPath(StringBuilder builder, CoreInstance element)
    {
        return PackageableElement.writeUserPathForPackageableElement(builder, element);
    }

    protected static class ElementBuilderWrapper implements ElementBuilder
    {
        private final MutableSet<String> concreteElements = Sets.mutable.<String>empty().asSynchronized();
        private final MutableSet<String> virtualPackages = Sets.mutable.<String>empty().asSynchronized();
        private final ElementBuilder delegate;

        private ElementBuilderWrapper(ElementBuilder delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void initialize(ElementLoader elementLoader)
        {
            this.delegate.initialize(elementLoader);
        }

        @Override
        public CoreInstance buildVirtualPackage(VirtualPackageMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
        {
            this.virtualPackages.add(metadata.getPath());
            return this.delegate.buildVirtualPackage(metadata, index, referenceIds, backRefProviderDeserializer);
        }

        @Override
        public CoreInstance buildConcreteElement(ConcreteElementMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
        {
            this.concreteElements.add(metadata.getPath());
            return this.delegate.buildConcreteElement(metadata, index, referenceIds, deserializer, backRefProviderDeserializer);
        }

        @Override
        public CoreInstance buildComponentInstance(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver)
        {
            throw new UnsupportedOperationException("This should not be called directly");
        }

        public boolean isConcreteElementLoaded(String path)
        {
            return this.concreteElements.contains(path);
        }

        public boolean isVirtualPackageLoaded(String path)
        {
            return this.virtualPackages.contains(path);
        }
    }

    private static class ElementLoaderProcessorSupport extends M3ProcessorSupport
    {
        private final ElementLoader elementLoader;

        private ElementLoaderProcessorSupport(ModelRepository modelRepository, ElementLoader elementLoader)
        {
            super(modelRepository);
            this.elementLoader = elementLoader;
        }

        @Override
        public CoreInstance package_getByUserPath(String path)
        {
            return this.elementLoader.loadElement(path);
        }

        @Override
        public CoreInstance repository_getTopLevel(String root)
        {
            return package_getByUserPath(root);
        }

        @Override
        public CoreInstance type_BottomType()
        {
            return package_getByUserPath(M3Paths.Nil);
        }

        @Override
        public CoreInstance type_TopType()
        {
            return package_getByUserPath(M3Paths.Any);
        }
    }
}
