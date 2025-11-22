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

package org.finos.legend.pure.m3.serialization.compiler.strings;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ServiceLoader;

public abstract class AbstractTestStringIndexer
{
    private final StringIndexerExtension extension = getExtension();
    private final StringIndexer stringIndexer = StringIndexer.builder().withExtension(this.extension).build();

    @Test
    public void testVersions()
    {
        int expectedVersion = getExtensionVersion();

        Assert.assertEquals(expectedVersion, this.stringIndexer.getDefaultVersion());
        Assert.assertTrue(this.stringIndexer.isVersionAvailable(expectedVersion));

        MutableIntList versions = IntLists.mutable.empty();
        this.stringIndexer.forEachVersion(versions::add);
        Assert.assertEquals(IntLists.mutable.with(expectedVersion), versions);
    }

    @Test
    public void testFindWithServiceLoader()
    {
        MutableIntObjectMap<StringIndexerExtension> extensions = IntObjectMaps.mutable.empty();
        ServiceLoader.load(StringIndexerExtension.class).forEach(ext ->
        {
            if (extensions.put(ext.version(), ext) != null)
            {
                Assert.fail("Multiple extensions for version: " + ext.version());
            }
        });
        int expectedVersion = getExtensionVersion();
        StringIndexerExtension foundExtension = extensions.get(expectedVersion);
        Assert.assertNotNull("Could not find version " + expectedVersion, foundExtension);
        Assert.assertSame(this.extension.getClass(), foundExtension.getClass());
    }

    @Test
    public void testNoStrings()
    {
        testSerializationRoundTrip();
    }

    @Test
    public void testTheQuickBrownFox()
    {
        testSerializationRoundTrip("the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog");
    }

    @Test
    public void testModelStrings()
    {
        testSerializationRoundTrip(M3Paths.Class, M3Paths.Association, "model::test::MyTestClass", "model::test::assocs::MyTestAssociation", "/test_module/testClass.pure", "/test_module/testAssocs.pure", "Integer.generalizations[0]", "meta::pure::functions::meta::tests::elementToPath::testEphemeralPackageableElements__Boolean_1_.expressionSequence[3].parametersValues[1].parametersValues[0].parametersValues[2].values[1].expression.parametersValues[2].values[1].expression.parametersValues[0].genericType.typeArguments[0]", "meta::pure::metamodel::type::Class.properties['property'].genericType", "test::model::TestClass.qualifiedProperties[id='qp(String[1])']");
    }

    @Test
    public void testManyStrings()
    {
        MutableSet<String> strings = Sets.mutable.with(M3Paths.Class, M3Paths.Association, "model::test::MyTestClass", "model::test::assocs::MyTestAssociation", "/test_module/testClass.pure", "/test_module/testAssocs.pure", "Integer.generalizations[0]", "meta::pure::functions::meta::tests::elementToPath::testEphemeralPackageableElements__Boolean_1_.expressionSequence[3].parametersValues[1].parametersValues[0].parametersValues[2].values[1].expression.parametersValues[2].values[1].expression.parametersValues[0].genericType.typeArguments[0]", "meta::pure::metamodel::type::Class.properties['property'].genericType", "test::model::TestClass.qualifiedProperties[id='qp(String[1])']");
        for (int i = 0; i <= Byte.MAX_VALUE; i++)
        {
            strings.add("model::test::_" + i + "::" + "_" + (i + 1));
        }
        Assert.assertTrue(strings.size() > Byte.MAX_VALUE);
        Assert.assertTrue(strings.size() < Short.MAX_VALUE);
        testSerializationRoundTrip(strings.toArray(new String[strings.size()]));
    }

    @Test
    public void testEvenMoreStrings()
    {
        MutableSet<String> strings = Sets.mutable.with(M3Paths.Class, M3Paths.Association, "model::test::MyTestClass", "model::test::assocs::MyTestAssociation", "/test_module/testClass.pure", "/test_module/testAssocs.pure", "Integer.generalizations[0]", "meta::pure::functions::meta::tests::elementToPath::testEphemeralPackageableElements__Boolean_1_.expressionSequence[3].parametersValues[1].parametersValues[0].parametersValues[2].values[1].expression.parametersValues[2].values[1].expression.parametersValues[0].genericType.typeArguments[0]", "meta::pure::metamodel::type::Class.properties['property'].genericType", "test::model::TestClass.qualifiedProperties[id='qp(String[1])']");
        for (int i = 0; i <= Short.MAX_VALUE; i++)
        {
            strings.add("model::test::_" + i + "::" + "_" + (i + 1));
        }
        Assert.assertTrue(strings.size() > Short.MAX_VALUE);
        testSerializationRoundTrip(strings.toArray(new String[strings.size()]));
    }

    protected void testSerializationRoundTrip(String... strings)
    {
        MutableList<Object> baseObjects = Lists.mutable.with(1, 2, 3L, -1, -2, -3L, 5.67f, -3.11111111d, 88888812389764.12341231, true, false, (short) 3, (byte) 0);
        MutableList<Object> allObjects = Lists.mutable.ofInitialCapacity(baseObjects.size() + 3 * strings.length);
        for (int i = 0, end = Math.max(baseObjects.size(), strings.length); i < end; i++)
        {
            if (i < baseObjects.size())
            {
                allObjects.add(baseObjects.get(i));
            }
            if (i < strings.length)
            {
                allObjects.add(strings[i]);
            }
        }
        byte[] bytes = toBytes(allObjects);
        MutableList<?> results = fromBytes(bytes, allObjects);
        Assert.assertEquals(allObjects, results);
    }

    protected byte[] toBytes(ListIterable<?> objects)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Writer writer = this.stringIndexer.writeStringIndex(BinaryWriters.newBinaryWriter(stream), objects.selectInstancesOf(String.class));
        objects.forEach(o ->
        {
            Class<?> cls = o.getClass();
            if (String.class == cls)
            {
                writer.writeString((String) o);
            }
            else if (Boolean.class == cls)
            {
                writer.writeBoolean((Boolean) o);
            }
            else if (Long.class == cls)
            {
                writer.writeLong((Long) o);
            }
            else if (Integer.class == cls)
            {
                writer.writeInt((Integer) o);
            }
            else if (Short.class == cls)
            {
                writer.writeShort((Short) o);
            }
            else if (Byte.class == cls)
            {
                writer.writeByte((Byte) o);
            }
            else if (Float.class == cls)
            {
                writer.writeFloat((Float) o);
            }
            else if (Double.class == cls)
            {
                writer.writeDouble((Double) o);
            }
            else if (cls.isArray())
            {
                Class<?> compType = cls.getComponentType();
                if (String.class == compType)
                {
                    writer.writeStringArray((String[]) o);
                }
                else if (long.class == compType)
                {
                    writer.writeLongArray((long[]) o);
                }
                else if (int.class == compType)
                {
                    writer.writeIntArray((int[]) o);
                }
                else if (short.class == compType)
                {
                    writer.writeShortArray((short[]) o);
                }
                else if (byte.class == compType)
                {
                    writer.writeByteArray((byte[]) o);
                }
                else if (float.class == compType)
                {
                    writer.writeFloatArray((float[]) o);
                }
                else if (double.class == compType)
                {
                    writer.writeDoubleArray((double[]) o);
                }
                else
                {
                    throw new RuntimeException("Cannot write array of " + compType.getName() + ": " + o);
                }
            }
            else
            {
                throw new RuntimeException("Cannot write instance of " + cls.getName() + ": " + o);
            }
        });
        return stream.toByteArray();
    }

    protected MutableList<?> fromBytes(byte[] bytes, MutableList<Object> expected)
    {
        Reader reader = this.stringIndexer.readStringIndex(BinaryReaders.newBinaryReader(bytes));
        return expected.collect(obj ->
        {
            Class<?> cls = obj.getClass();
            try
            {
                if (String.class == cls)
                {
                    return reader.readString();
                }
                if (Boolean.class == cls)
                {
                    return reader.readBoolean();
                }
                if (Long.class == cls)
                {
                    return reader.readLong();
                }
                if (Integer.class == cls)
                {
                    return reader.readInt();
                }
                if (Short.class == cls)
                {
                    return reader.readShort();
                }
                if (Byte.class == cls)
                {
                    return reader.readByte();
                }
                if (Float.class == cls)
                {
                    return reader.readFloat();
                }
                if (Double.class == cls)
                {
                    return reader.readDouble();
                }
                if (cls.isArray())
                {
                    Class<?> compType = cls.getComponentType();
                    if (String.class == compType)
                    {
                        return reader.readStringArray();
                    }
                    if (long.class == compType)
                    {
                        return reader.readLongArray();
                    }
                    if (int.class == compType)
                    {
                        return reader.readIntArray();
                    }
                    if (short.class == compType)
                    {
                        return reader.readShortArray();
                    }
                    if (byte.class == compType)
                    {
                        return reader.readByteArray();
                    }
                    if (float.class == compType)
                    {
                        return reader.readFloatArray();
                    }
                    if (double.class == compType)
                    {
                        return reader.readDoubleArray();
                    }
                    throw new RuntimeException("Cannot read an array of type " + compType.getName());
                }
                throw new RuntimeException("Cannot read an instance of type " + cls.getName());
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error trying to read instance of " + obj.getClass().getName() + ": " + obj, e);
            }
        });
    }

    protected abstract int getExtensionVersion();

    protected abstract StringIndexerExtension getExtension();
}
