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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.function.Function;

public abstract class AbstractTestConcreteElementSerializerExtension extends AbstractReferenceTest
{
    private ConcreteElementSerializerExtension extension;
    private ConcreteElementSerializer serializer;
    private ConcreteElementDeserializer deserializer;
    private ReferenceIdProviders referenceIdProviders;
    private ReferenceIdResolvers referenceIdResolvers;
    private MapIterable<String, ImmutableList<String>> backRefProperties;

    @Before
    public void setUpExtension()
    {
        this.extension = getExtension();
        this.referenceIdProviders = ReferenceIdProviders.builder().withAvailableExtensions().withProcessorSupport(processorSupport).build();
        this.referenceIdResolvers = ReferenceIdResolvers.builder().withAvailableExtensions().withPackagePathResolver(processorSupport::package_getByUserPath).build();
        this.serializer = ConcreteElementSerializer.builder(processorSupport).withExtension(this.extension).withReferenceIdProviders(this.referenceIdProviders).build();
        this.deserializer = ConcreteElementDeserializer.builder().withExtension(this.extension).build();
        this.backRefProperties = M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS
                .groupByUniqueKey(ImmutableList::getLast, Maps.mutable.ofInitialCapacity(M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.size() + 1))
                .withKeyValue(M3Properties.children, M3PropertyPaths.children)
                .asUnmodifiable();
    }

    @Test
    public void testVersions()
    {
        int expectedVersion = this.extension.version();

        Assert.assertEquals(expectedVersion, this.serializer.getDefaultVersion());
        Assert.assertTrue(this.serializer.isVersionAvailable(expectedVersion));

        MutableIntList versions = IntLists.mutable.empty();
        this.serializer.forEachVersion(versions::add);
        Assert.assertEquals(IntLists.mutable.with(expectedVersion), versions);
    }

    @Test
    public void testPlatformVsExtraRepos()
    {
        String platform = "platform";
        MutableList<CodeRepository> repos = runtime.getCodeStorage().getAllRepositories().select(r -> platform.equals(r.getName()), Lists.mutable.ofInitialCapacity(1));
        Assert.assertEquals(1, repos.size());

        CompositeCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories()));
        PureRuntime platformRuntime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(getPureRuntimeStatus())
                .withFactoryRegistryOverride(getFactoryRegistryOverride())
                .setTransactionalByDefault(isTransactionalByDefault())
                .withOptions(getOptions())
                .build();
        platformRuntime.loadAndCompileCore();
        platformRuntime.loadAndCompileSystem();

        ReferenceIdProviders platformReferenceIdProviders = ReferenceIdProviders.builder()
                .withAvailableExtensions()
                .withProcessorSupport(platformRuntime.getProcessorSupport())
                .build();
        ConcreteElementSerializer platformSerializer = ConcreteElementSerializer.builder(platformRuntime.getProcessorSupport())
                .withExtension(this.extension)
                .withReferenceIdProviders(platformReferenceIdProviders)
                .build();
        ConcreteElementDeserializer platformDeserializer = ConcreteElementDeserializer.builder()
                .withExtension(this.extension)
                .build();

        GraphTools.getTopLevelAndPackagedElements(platformRuntime.getProcessorSupport())
                .select(c -> c.getSourceInformation() != null)
                .forEach(platformElement ->
                {
                    String path = PackageableElement.getUserPathForPackageableElement(platformElement);
                    CoreInstance fullElement = runtime.getCoreInstance(path);

                    byte[] fullSerialized = serialize(path, fullElement, this.serializer);
                    byte[] platformSerialized = serialize(path, platformElement, platformSerializer);
                    Assert.assertArrayEquals(path, fullSerialized, platformSerialized);

                    DeserializedConcreteElement fullDeserialized = deserialize(path, fullSerialized, this.deserializer);
                    DeserializedConcreteElement platformDeserialized = deserialize(path, platformSerialized, platformDeserializer);
                    if (!platformDeserialized.equals(fullDeserialized))
                    {
                        Assert.assertEquals(path, platformDeserialized.getPath(), fullDeserialized.getPath());
                        Assert.assertEquals(path, platformDeserialized.getReferenceIdVersion(), fullDeserialized.getReferenceIdVersion());
                        Assert.assertEquals(path, platformDeserialized.getInstanceData().size(), fullDeserialized.getInstanceData().size());
                        if (!platformDeserialized.getInstanceData().equals(fullDeserialized.getInstanceData()))
                        {
                            platformDeserialized.getInstanceData().forEachWithIndex((platformInstanceData, i) ->
                            {
                                InstanceData fullInstanceData = fullDeserialized.getInstanceData(i);
                                Assert.assertEquals(path + ".instanceData[" + i + "]", platformInstanceData, fullInstanceData);
                            });
                        }
                        Assert.assertEquals(path, platformDeserialized, fullDeserialized);
                    }
                });
    }

    @Test
    public void testSerializeDeserializeAll()
    {
        GraphTools.getTopLevelAndPackagedElements(processorSupport)
                .select(c -> c.getSourceInformation() != null)
                .forEach(this::testSerializeDeserialize);
    }

    private void testSerializeDeserialize(CoreInstance element)
    {
        String path = PackageableElement.getUserPathForPackageableElement(element);
        DeserializedConcreteElement deserialized = serializeAndDeserialize(path, element, this.serializer, this.deserializer);
        Assert.assertEquals(path, deserialized.getPath());
        Assert.assertEquals(path, this.referenceIdProviders.getDefaultVersion(), deserialized.getReferenceIdVersion());
        IntObjectMap<CoreInstance> intRefs = buildInternalIdMap(deserialized, element);
        MutableObjectIntMap<CoreInstance> reverseIntRefs = ObjectIntMaps.mutable.ofInitialCapacity(intRefs.size());
        intRefs.forEachKeyValue((internalId, refElement) -> reverseIntRefs.put(refElement, internalId));
        assertDeserialization(deserialized, intRefs, reverseIntRefs);
    }

    private DeserializedConcreteElement serializeAndDeserialize(String path, CoreInstance element, ConcreteElementSerializer serializer, ConcreteElementDeserializer deserializer)
    {
        byte[] bytes = serialize(path, element, serializer);
        return deserialize(path, bytes, deserializer);
    }

    private byte[] serialize(String path, CoreInstance element, ConcreteElementSerializer serializer)
    {
        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            serializer.serialize(BinaryWriters.newBinaryWriter(stream), element);
            return stream.toByteArray();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error serializing " + path, e);
        }
    }

    private DeserializedConcreteElement deserialize(String path, byte[] bytes, ConcreteElementDeserializer deserializer)
    {
        try
        {
            return deserializer.deserialize(BinaryReaders.newBinaryReader(bytes));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error deserializing " + path, e);
        }
    }

    private IntObjectMap<CoreInstance> buildInternalIdMap(DeserializedConcreteElement concreteElement, CoreInstance element)
    {
        MutableIntObjectMap<CoreInstance> intRefs = IntObjectMaps.mutable.empty();
        intRefs.put(0, element);
        buildInternalIdMapFromOrdinaryProps(concreteElement, intRefs, concreteElement.getPath(), element, concreteElement.getConcreteElementData());
        buildInternalIdMapFromBackRefs(intRefs, concreteElement);
        Assert.assertEquals(concreteElement.getPath(), intRefs.keysView().toSortedList(), IntInterval.zeroTo(concreteElement.getInstanceData().size() - 1));
        return intRefs;
    }

    private void buildInternalIdMapFromOrdinaryProps(DeserializedConcreteElement concreteElement, MutableIntObjectMap<CoreInstance> intRefs, String path, CoreInstance element, InstanceData instanceData)
    {
        MutableMap<String, PropertyValues> deserializedPropertyValues = Maps.mutable.empty();
        instanceData.getPropertyValues().forEach(pv ->
        {
            if ((deserializedPropertyValues.put(pv.getPropertyName(), pv) != null))
            {
                Assert.fail("Multiple property values for '" + pv.getPropertyName() + "' for " + path);
            }
        });

        processorSupport.class_getSimpleProperties(processorSupport.getClassifier(element)).forEach(property ->
        {
            String key = property.getName();
            ListIterable<? extends CoreInstance> values = element.getValueForMetaPropertyToMany(key);
            if (values.notEmpty() && !isBackRefProperty(property))
            {
                ListIterable<ValueOrReference> pValues = deserializedPropertyValues.get(key).getValues();
                String keyPath = path + "." + key;
                Assert.assertEquals(keyPath, values.size(), pValues.size());
                values.forEachWithIndex((value, i) ->
                {
                    String keyPathWithIndex = keyPath + "[" + i + "]";
                    pValues.get(i).visit(new ValueOrReferenceConsumer()
                    {
                        @Override
                        protected void accept(Reference.InternalReference reference)
                        {
                            int internalId = reference.getId();
                            CoreInstance found = intRefs.get(internalId);
                            if (found == null)
                            {
                                intRefs.put(internalId, value);
                                buildInternalIdMapFromOrdinaryProps(concreteElement, intRefs, keyPathWithIndex, value, concreteElement.getInstanceData(internalId));
                            }
                        }
                    });
                });
            }
        });
    }

    private void buildInternalIdMapFromBackRefs(MutableIntObjectMap<CoreInstance> intRefs, DeserializedConcreteElement concreteElement)
    {
        MutableObjectIntMap<CoreInstance> reverseIntRefs = ObjectIntMaps.mutable.ofInitialCapacity(intRefs.size());
        intRefs.forEachKeyValue((internalId, instance) -> reverseIntRefs.put(instance, internalId));
        concreteElement.getInstanceData().forEachWithIndex((instanceData, id) ->
        {
            CoreInstance instance = intRefs.get(id);
            MutableList<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(M3Properties.referenceUsages)
                    .select(v -> reverseIntRefs.containsKey(getBackRefOwner(M3Properties.referenceUsages, v)), Lists.mutable.empty());
            if (values.notEmpty())
            {
                ListIterable<ValueOrReference> pValues = instanceData.getPropertyValues().detect(pv -> M3Properties.referenceUsages.equals(pv.getPropertyName())).getValues();
                Assert.assertEquals(instanceData.getReferenceId(), values.size(), pValues.size());
                values.sortThisByInt(v -> reverseIntRefs.get(getBackRefOwner(M3Properties.referenceUsages, v)));
                values.forEach(ru -> intRefs.put(intRefs.size(), ru));
            }
        });
    }

    private void assertDeserialization(DeserializedConcreteElement concreteElement, IntObjectMap<CoreInstance> intRefs, ObjectIntMap<CoreInstance> reverseIntRefs)
    {
        ReferenceIdResolver referenceIdResolver = this.referenceIdResolvers.resolver(concreteElement.getReferenceIdVersion());
        concreteElement.getInstanceData().forEachWithIndex((instanceData, intId) ->
        {
            String path = (instanceData.getReferenceId() == null) ? (concreteElement.getPath() + "#" + intId) : instanceData.getReferenceId();
            CoreInstance element = intRefs.get(intId);
            if (ModelRepository.isAnonymousInstanceName(element.getName()))
            {
                Assert.assertNull(path, instanceData.getName());
            }
            else
            {
                Assert.assertEquals(path, element.getName(), instanceData.getName());
            }
            Assert.assertEquals(path, element.getSourceInformation(), instanceData.getSourceInformation());
            Assert.assertEquals(path, PackageableElement.getUserPathForPackageableElement(processorSupport.getClassifier(element)), instanceData.getClassifierPath());

            MutableMap<String, PropertyValues> deserializedPropertyValues = Maps.mutable.empty();
            instanceData.getPropertyValues().forEach(pv ->
            {
                if ((deserializedPropertyValues.put(pv.getPropertyName(), pv) != null))
                {
                    Assert.fail("Multiple property values for '" + pv.getPropertyName() + "' for " + path);
                }
            });

            MutableList<String> nonEmptyPropertyKeys = Lists.mutable.empty();
            processorSupport.class_getSimpleProperties(processorSupport.getClassifier(element)).forEach(property ->
            {
                String key = property.getName();
                ListIterable<? extends CoreInstance> values = element.getValueForMetaPropertyToMany(key);
                if (values.notEmpty())
                {
                    boolean isBackRef = isBackRefProperty(property);
                    if (!isBackRef || values.anySatisfy(v -> reverseIntRefs.containsKey(getBackRefOwner(key, v))))
                    {
                        nonEmptyPropertyKeys.add(key);
                        PropertyValues pValues = deserializedPropertyValues.get(key);
                        if (pValues != null)
                        {
                            Assert.assertEquals(path + "." + key, PackageableElement.getUserPathForPackageableElement(Property.getPropertySourceType(property, processorSupport)), pValues.getPropertySourceType());
                        }
                    }
                }
            });
            Assert.assertEquals(path, nonEmptyPropertyKeys.toSortedList(), deserializedPropertyValues.keysView().toSortedList());

            nonEmptyPropertyKeys.forEach(key ->
            {
                ListIterable<? extends CoreInstance> values = element.getValueForMetaPropertyToMany(key);
                if (isBackRefProperty(key))
                {
                    values = values.select(v -> reverseIntRefs.containsKey(getBackRefOwner(key, v)), Lists.mutable.empty())
                            .sortThisBy(v -> reverseIntRefs.get(getBackRefOwner(key, v)));
                }
                ListIterable<ValueOrReference> pValues = deserializedPropertyValues.get(key).getValues();
                String keyPath = path + "." + key;
                Assert.assertEquals(keyPath, values.size(), pValues.size());
                values.forEachWithIndex((value, i) ->
                {
                    String keyPathWithIndex = keyPath + "[" + i + "]";
                    pValues.get(i).visit(new ValueOrReferenceConsumer()
                    {
                        @Override
                        protected void accept(Reference.ExternalReference reference)
                        {
                            CoreInstance resolved = referenceIdResolver.resolveReference(reference.getId());
                            Assert.assertSame(keyPathWithIndex + "=" + reference.getId(), value, resolved);
                        }

                        @Override
                        protected void accept(Reference.InternalReference reference)
                        {
                            int internalId = reference.getId();
                            CoreInstance found = intRefs.get(internalId);
                            Assert.assertSame(keyPathWithIndex + "=" + internalId, value, found);
                        }

                        @Override
                        protected void accept(Value.BooleanValue bValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getBooleanValue, bValue);
                        }

                        @Override
                        protected void accept(Value.ByteValue bValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getByteValue, bValue);
                        }

                        @Override
                        protected void accept(Value.DateValue dValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getDateValue, dValue);
                        }

                        @Override
                        protected void accept(Value.DateTimeValue dtValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getDateValue, dtValue);
                        }

                        @Override
                        protected void accept(Value.StrictDateValue sdValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getDateValue, sdValue);
                        }

                        @Override
                        protected void accept(Value.LatestDateValue ldValue)
                        {
                            assertValue(keyPathWithIndex, value, v -> null, ldValue);
                        }

                        @Override
                        protected void accept(Value.DecimalValue dValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getDecimalValue, dValue);
                        }

                        @Override
                        protected void accept(Value.FloatValue fValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getFloatValue, fValue);
                        }

                        @Override
                        protected void accept(Value.IntegerValue iValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getIntegerValue, iValue);
                        }

                        @Override
                        protected void accept(Value.StrictTimeValue stValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getStrictTimeValue, stValue);
                        }

                        @Override
                        protected void accept(Value.StringValue sValue)
                        {
                            assertValue(keyPathWithIndex, value, PrimitiveUtilities::getStringValue, sValue);
                        }
                    });
                });
            });
        });
    }

    private void assertValue(String message, CoreInstance expectedInstance, Function<CoreInstance, ?> valueExtractor, Value<?> actualValue)
    {
        Assert.assertEquals(message, PackageableElement.getUserPathForPackageableElement(processorSupport.getClassifier(expectedInstance)), actualValue.getClassifierPath());
        Assert.assertEquals(message, valueExtractor.apply(expectedInstance), actualValue.getValue());
    }

    private boolean isBackRefProperty(String propertyName)
    {
        return this.backRefProperties.containsKey(propertyName);
    }

    private boolean isBackRefProperty(CoreInstance property)
    {
        ImmutableList<String> backRefPropertyPath = this.backRefProperties.get(property.getName());
        return (backRefPropertyPath != null) && backRefPropertyPath.equals(Property.calculatePropertyPath(property, processorSupport));
    }

    private CoreInstance getBackRefOwner(String propertyName, CoreInstance value)
    {
        return M3Properties.referenceUsages.equals(propertyName) ? value.getValueForMetaPropertyToOne(M3Properties.owner) : value;
    }

    protected abstract ConcreteElementSerializerExtension getExtension();
}
