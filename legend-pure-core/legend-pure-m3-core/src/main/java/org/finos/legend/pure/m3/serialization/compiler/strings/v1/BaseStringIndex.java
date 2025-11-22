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

package org.finos.legend.pure.m3.serialization.compiler.strings.v1;

import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;

import java.util.function.Consumer;

abstract class BaseStringIndex
{
    private static final String[] SPECIAL_STRINGS = {
            // null and empty string
            null, "",
            // separators
            PackageableElement.DEFAULT_PATH_SEPARATOR, RepositoryCodeStorage.PATH_SEPARATOR, ".",
            // primitive types
            M3Paths.Boolean, M3Paths.Byte, M3Paths.Date, M3Paths.DateTime, M3Paths.Decimal, M3Paths.Float,
            M3Paths.Integer, M3Paths.LatestDate, M3Paths.Number, M3Paths.StrictDate, M3Paths.StrictTime, M3Paths.String,
            // common types
            M3Paths.Any, M3Paths.Association, M3Paths.Class, M3Paths.ConcreteFunctionDefinition, M3Paths.Enumeration,
            M3Paths.ImportGroup, M3Paths.LambdaFunction, M3Paths.List, M3Paths.Map, M3Paths.NativeFunction, M3Paths.Nil,
            M3Paths.Package, M3Paths.Pair, M3Paths.PrimitiveType, M3Paths.Profile, M3Paths.Property,
            M3Paths.QualifiedProperty, M3Paths.ReferenceUsage, M3Paths.Relation, M3Paths.RelationType,
            M3Paths.RootRouteNode, M3Paths.Stereotype, M3Paths.Tag, M3Paths.TaggedValue,
            // package names
            M3Paths.Root, "collection", "function", "functions", "meta", "metamodel", "property", "pure",
            "relationship", "string", "type",
            // properties
            M3Properties.children, M3Properties.classifierGenericType, M3Properties.constraints,
            M3Properties.expressionSequence, M3Properties.func, M3Properties.genericType, M3Properties.multiplicity,
            M3Properties.multiplicityArguments, M3Properties.multiplicityParameters, M3Properties.owner,
            M3Properties.postConstraints, M3Properties.preConstraints, M3Properties.properties,
            M3Properties.propertiesFromAssociations, M3Properties.qualifiedProperties,
            M3Properties.qualifiedPropertiesFromAssociations, M3Properties.rawType, M3Properties.resolvedEnum,
            M3Properties.resolvedNode, M3Properties.resolvedProperty, M3Properties.returnMultiplicity,
            M3Properties.returnType, M3Properties.stereotypes, M3Properties.tag, M3Properties.taggedValues,
            M3Properties.typeArguments, M3Properties.typeParameters, M3Properties.values,
            // M3 Pure parser name
            "Pure",
            // name multiplicities
            M3Paths.OneMany, M3Paths.PureOne, M3Paths.PureZero, M3Paths.ZeroMany, M3Paths.ZeroOne
    };
    private static final ObjectIntMap<String> SPECIAL_STRING_IDS = buildSpecialStringsToIdMap();

    private static final int MIN_SPECIAL_STRING_ID = -SPECIAL_STRINGS.length;

    private static final int MAX_1BYTE_ID = 0xff - SPECIAL_STRINGS.length;
    private static final int MAX_2BYTE_ID = 0xffff - SPECIAL_STRINGS.length;
    private static final int MAX_3BYTE_ID = 0xffffff - SPECIAL_STRINGS.length;

    /**
     * Return whether the given id is a special string id. Special string ids are always negative.
     *
     * @param id id
     * @return whether id is a special string id
     */
    static boolean isSpecialStringId(int id)
    {
        return (id < 0) && (id >= MIN_SPECIAL_STRING_ID);
    }

    /**
     * Return whether the given string is a special string.
     *
     * @param string string
     * @return whether string is a special string
     */
    static boolean isSpecialString(String string)
    {
        return SPECIAL_STRING_IDS.containsKey(string);
    }

    /**
     * Get the special string for the given id. Should call {@link #isSpecialStringId} first to ensure the id is a
     * special string id.
     *
     * @param id special string id
     * @return special string
     */
    static String getSpecialString(int id)
    {
        return SPECIAL_STRINGS[specialStringIdToIndex(id)];
    }

    /**
     * Get the id of the given special string. Special string ids are always negative. Returns 0 if the string is not a
     * special string.
     *
     * @param string special string
     * @return special string id or 0 if not found
     */
    static int getSpecialStringId(String string)
    {
        return SPECIAL_STRING_IDS.getIfAbsent(string, 0);
    }

    static void forEachSpecialString(Consumer<? super String> consumer)
    {
        for (String string : SPECIAL_STRINGS)
        {
            consumer.accept(string);
        }
    }

    private static int specialStringIdToIndex(int id)
    {
        return -(id + 1);
    }

    private static int specialStringIndexToId(int index)
    {
        return -(index + 1);
    }

    private static ObjectIntMap<String> buildSpecialStringsToIdMap()
    {
        int size = SPECIAL_STRINGS.length;
        MutableObjectIntMap<String> map = ObjectIntMaps.mutable.ofInitialCapacity(size);
        for (int i = 0; i < size; i++)
        {
            map.put(SPECIAL_STRINGS[i], specialStringIndexToId(i));
        }
        return map;
    }


    static int getStringIdByteWidth(int count)
    {
        return (count <= MAX_1BYTE_ID) ? 1 :
               ((count <= MAX_2BYTE_ID) ? 2 :
                ((count <= MAX_3BYTE_ID) ? 3 : 4));
    }

    static byte idToOneByte(int id)
    {
        return (byte) (id + SPECIAL_STRINGS.length + Byte.MIN_VALUE);
    }

    static int oneByteToId(byte b)
    {
        return ((int) b) - Byte.MIN_VALUE - SPECIAL_STRINGS.length;
    }

    static short idToTwoBytes(int id)
    {
        return (short) (id + SPECIAL_STRINGS.length + Short.MIN_VALUE);
    }

    static int twoBytesToId(short s)
    {
        return ((int) s) - Short.MIN_VALUE - SPECIAL_STRINGS.length;
    }

    static byte[] idToThreeBytes(int id)
    {
        return idToThreeBytes(id, new byte[3], 0);
    }

    static byte[] idToThreeBytes(int id, byte[] target, int offset)
    {
        int adjusted = id + SPECIAL_STRINGS.length;
        target[offset] = (byte) adjusted;
        target[offset + 1] = (byte) (adjusted >>> 8);
        target[offset + 2] = (byte) (adjusted >>> 16);
        return target;
    }

    static int threeBytesToId(byte[] bytes)
    {
        return threeBytesToId(bytes[0], bytes[1], bytes[2]);
    }

    static int threeBytesToId(byte[] bytes, int offset)
    {
        return threeBytesToId(bytes[offset], bytes[offset + 1], bytes[offset + 2]);
    }

    static int threeBytesToId(byte b1, byte b2, byte b3)
    {
        return (Byte.toUnsignedInt(b1) |
                (Byte.toUnsignedInt(b2) << 8) |
                (Byte.toUnsignedInt(b3) << 16)) - SPECIAL_STRINGS.length;
    }
}
