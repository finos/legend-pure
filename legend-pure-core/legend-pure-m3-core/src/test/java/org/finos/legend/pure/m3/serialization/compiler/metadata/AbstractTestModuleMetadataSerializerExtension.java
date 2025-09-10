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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractTestModuleMetadataSerializerExtension extends AbstractMetadataTest
{
    private final ModuleMetadataSerializerExtension extension = getExtension();
    private final ModuleMetadataSerializer serializer = ModuleMetadataSerializer.builder().withExtension(this.extension).build();

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
    public void testFindWithServiceLoader()
    {
        MutableIntObjectMap<ModuleMetadataSerializerExtension> extensions = IntObjectMaps.mutable.empty();
        ServiceLoader.load(ModuleMetadataSerializerExtension.class).forEach(ext ->
        {
            if (extensions.put(ext.version(), ext) != null)
            {
                Assert.fail("Multiple extensions for version: " + ext.version());
            }
        });
        ModuleMetadataSerializerExtension foundExtension = extensions.get(this.extension.version());
        Assert.assertNotNull("Could not find version " + this.extension.version(), foundExtension);
        Assert.assertSame(this.extension.getClass(), foundExtension.getClass());
    }

    @Test
    public void testEmptyModule()
    {
        testModuleMetadataSerializes(ModuleMetadata.builder("empty_module").withReferenceIdVersion(1).build());
    }

    @Test
    public void testSimpleModuleWithOneSource()
    {
        testModuleMetadataSerializes(ModuleMetadata.builder("simple_module")
                .withReferenceIdVersion(1)
                .withElements(
                        newClass("model::classes::MySimpleClass", "/simple_module/model/classes.pure", 1, 1, 5, 1),
                        newClass("model::classes::MyOtherClass", "/simple_module/model/classes.pure", 6, 1, 10, 1))
                .withSource(newSource("/simple_module/model/classes.pure",
                        newSourceSection("Pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass"),
                        newSourceSection("Pure", "model::classes::MyThirdClass")))
                .withExternalReferences("model::classes::MySimpleClass", "model::classes::MyOtherClass.properties['simple'].classifierGenericType.typeArguments[1]")
                .withBackReferences(
                        "model::classes::MySimpleClass",
                        "model::classes::MyOtherClass.properties['simple'].classifierGenericType.typeArguments[1]",
                        refUsage("model::classes::MyOtherClass.properties['simple'].classifierGenericType.typeArguments[1]", "rawType", 0, "/simple_module/model/classes.pure", 7, 7, 7, 19))
                .build());
    }

    @Test
    public void testSimpleModuleWithMultipleSources()
    {
        testModuleMetadataSerializes(ModuleMetadata.builder("multi_source_module")
                .withReferenceIdVersion(1)
                .withElements(
                        newClass("model::classes::MySimpleClass",
                                "/multi_source_module/model/classes.pure", 1, 1, 5, 1),
                        newClass("model::classes::MyOtherClass",
                                "/multi_source_module/model/classes.pure", 6, 1, 10, 1),
                        newClass("model::classes::MyThirdClass",
                                "/multi_source_module/model/classes.pure", 12, 1, 20, 1),
                        newAssociation("model::associations::SimpleToOther",
                                "/multi_source_module/model/associations.pure", 2, 1, 7, 1),
                        newAssociation("model::associations::SimpleToThird",
                                "/multi_source_module/model/associations.pure", 9, 1, 16, 1),
                        newAssociation("model::associations::OtherToThird",
                                "/multi_source_module/model/associations.pure", 18, 1, 25, 1),
                        newEnumeration("model::enums::MyFirstEnumeration", "/multi_source_module/model/enums.pure", 3, 1, 6, 1),
                        newEnumeration("model::enums::MySecondEnumeration", "/multi_source_module/model/enums.pure", 8, 1, 10, 1))
                .withSources(
                        newSource("/multi_source_module/model/classes.pure",
                                newSourceSection("Pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass"),
                                newSourceSection("Pure", "model::classes::MyThirdClass")),
                        newSource("/multi_source_module/model/associations.pure",
                                newSourceSection("Pure", "model::associations::SimpleToOther"),
                                newSourceSection("Pure", "model::associations::SimpleToThird", "model::associations::OtherToThird")),
                        newSource("/multi_source_module/model/enums.pure", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration"))
                .withExternalReferences(
                        "model::classes::MySimpleClass",
                        "model::classes::MyOtherClass.properties['simple'].classifierGenericType.typeArguments[1]",
                        "model::classes::MyThirdClass.properties['simple'].classifierGenericType.typeArguments[1]",
                        "model::associations::SimpleToOther.properties['toSimple']",
                        "model::associations::SimpleToThird.properties['toThird']")
                .withExternalReferences(
                        "model::classes::MyOtherClass",
                        "model::associations::SimpleToOther.properties['toOther']",
                        "model::associations::OtherToThird.properties['toOther']",
                        "model::associations::OtherToThird.properties['toOther'].classifierGenericType.typeArguments[1]")
                .withExternalReferences(
                        "model::classes::MyThirdClass",
                        "model::associations::SimpleToThird.properties['toSimple']",
                        "model::associations::OtherToThird.properties['toThird']",
                        "model::associations::OtherToThird.properties['toThird'].classifierGenericType.typeArguments[1]")
                .withBackReferences(
                        "model::classes::MySimpleClass",
                        "model::classes::MyOtherClass.properties['simple'].classifierGenericType.typeArguments[1]",
                        refUsage("model::classes::MyOtherClass.properties['simple'].classifierGenericType.typeArguments[1]", "rawType", 0, "/multi_source_module/model/classes.pure", 7, 7, 7, 19))
                .withBackReferences(
                        "model::classes::MySimpleClass",
                        "model::classes::MyThirdClass.properties['simple'].classifierGenericType.typeArguments[1]",
                        refUsage("model::classes::MyThirdClass.properties['simple'].classifierGenericType.typeArguments[1]", "rawType"))
                .withBackReferences(
                        "model::classes::MySimpleClass",
                        "model::associations::SimpleToOther.properties['toSimple']",
                        propFromAssoc("model::associations::SimpleToOther.properties['toSimple']"))
                .withBackReferences(
                        "model::classes::MyOtherClass",
                        "model::associations::SimpleToOther.properties['toOther']",
                        propFromAssoc("model::associations::SimpleToOther.properties['toOther']"))
                .withBackReferences(
                        "model::classes::MySimpleClass",
                        "model::associations::SimpleToThird.properties['toThird']",
                        propFromAssoc("model::associations::SimpleToThird.properties['toThird']"))
                .withBackReferences(
                        "model::classes::MyThirdClass",
                        "model::associations::SimpleToThird.properties['toSimple']",
                        propFromAssoc("model::associations::SimpleToThird.properties['toSimple']"))
                .withBackReferences(
                        "model::classes::MyOtherClass",
                        "model::associations::OtherToThird.properties['toOther']",
                        propFromAssoc("model::associations::OtherToThird.properties['toOther']"))
                .withBackReferences(
                        "model::classes::MyOtherClass",
                        "model::associations::OtherToThird.properties['toOther'].classifierGenericType.typeArguments[1]",
                        refUsage("model::associations::OtherToThird.properties['toOther'].classifierGenericType.typeArguments[1]", "rawType"))
                .withBackReferences(
                        "model::classes::MyThirdClass",
                        "model::associations::OtherToThird.properties['toThird']",
                        propFromAssoc("model::associations::OtherToThird.properties['toThird']"))
                .withBackReferences(
                        "model::classes::MyThirdClass",
                        "model::associations::OtherToThird.properties['toThird'].classifierGenericType.typeArguments[1]",
                        refUsage("model::associations::OtherToThird.properties['toThird'].classifierGenericType.typeArguments[1]", "rawType"))
                .build());
    }

    protected abstract ModuleMetadataSerializerExtension getExtension();

    protected void testModuleMetadataSerializes(ModuleMetadata metadata)
    {
        testSerializes(metadata.getManifest(), this.serializer::serializeManifest, this.serializer::deserializeManifest);
        testSerializes(metadata.getSourceMetadata(), this.serializer::serializeSourceMetadata, this.serializer::deserializeSourceMetadata);
        testSerializes(metadata.getExternalReferenceMetadata(), this.serializer::serializeExternalReferenceMetadata, this.serializer::deserializeExternalReferenceMetadata);
        metadata.getBackReferenceMetadata()
                .getBackReferences()
                .forEach(br -> testSerializes(br, this.serializer::serializeBackReferenceMetadata, this.serializer::deserializeBackReferenceMetadata));
    }

    private <M> void testSerializes(M metadata, BiConsumer<Writer, M> serializer, Function<Reader, M> deserializer)
    {
        byte[] bytes = serialize(serializer, metadata);
        M deserialized = deserialize(deserializer, bytes);
        Assert.assertEquals(metadata, deserialized);

        byte[] bytes2 = serialize(serializer, metadata);
        Assert.assertArrayEquals("serialization instability for version " + this.extension.version() + " (" + this.extension.getClass().getName() + ")", bytes, bytes2);

        byte[] bytes3 = serialize(serializer, deserialized);
        Assert.assertArrayEquals("serialization instability for version " + this.extension.version() + " (" + this.extension.getClass().getName() + ")", bytes, bytes3);
    }

    private <M> byte[] serialize(BiConsumer<Writer, M> serializer, M metadata)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        serializer.accept(BinaryWriters.newBinaryWriter(bytes), metadata);
        return bytes.toByteArray();
    }

    private <M> M deserialize(Function<Reader, M> deserializer, byte[] bytes)
    {
        try (Reader reader = BinaryReaders.newBinaryReader(bytes))
        {
            return deserializer.apply(reader);
        }
    }
}
