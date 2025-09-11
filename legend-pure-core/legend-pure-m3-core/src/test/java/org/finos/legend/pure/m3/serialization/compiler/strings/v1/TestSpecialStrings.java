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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

// DO NOT MAKE CHANGES IN THIS TEST
// If changes are needed, make them in BaseStringIndex
public class TestSpecialStrings
{
    @Test
    public void testSpecialStringIds()
    {
        Assert.assertEquals(-1, BaseStringIndex.getSpecialStringId(null));
        Assert.assertEquals(-2, BaseStringIndex.getSpecialStringId(""));
        Assert.assertEquals(-3, BaseStringIndex.getSpecialStringId("::"));
        Assert.assertEquals(-4, BaseStringIndex.getSpecialStringId("/"));
        Assert.assertEquals(-5, BaseStringIndex.getSpecialStringId("."));
        Assert.assertEquals(-6, BaseStringIndex.getSpecialStringId("Boolean"));
        Assert.assertEquals(-7, BaseStringIndex.getSpecialStringId("Byte"));
        Assert.assertEquals(-8, BaseStringIndex.getSpecialStringId("Date"));
        Assert.assertEquals(-9, BaseStringIndex.getSpecialStringId("DateTime"));
        Assert.assertEquals(-10, BaseStringIndex.getSpecialStringId("Decimal"));
        Assert.assertEquals(-11, BaseStringIndex.getSpecialStringId("Float"));
        Assert.assertEquals(-12, BaseStringIndex.getSpecialStringId("Integer"));
        Assert.assertEquals(-13, BaseStringIndex.getSpecialStringId("LatestDate"));
        Assert.assertEquals(-14, BaseStringIndex.getSpecialStringId("Number"));
        Assert.assertEquals(-15, BaseStringIndex.getSpecialStringId("StrictDate"));
        Assert.assertEquals(-16, BaseStringIndex.getSpecialStringId("StrictTime"));
        Assert.assertEquals(-17, BaseStringIndex.getSpecialStringId("String"));
        Assert.assertEquals(-18, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::type::Any"));
        Assert.assertEquals(-19, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::relationship::Association"));
        Assert.assertEquals(-20, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::type::Class"));
        Assert.assertEquals(-21, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::function::ConcreteFunctionDefinition"));
        Assert.assertEquals(-22, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::type::Enumeration"));
        Assert.assertEquals(-23, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::import::ImportGroup"));
        Assert.assertEquals(-24, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::function::LambdaFunction"));
        Assert.assertEquals(-25, BaseStringIndex.getSpecialStringId("meta::pure::functions::collection::List"));
        Assert.assertEquals(-26, BaseStringIndex.getSpecialStringId("meta::pure::functions::collection::Map"));
        Assert.assertEquals(-27, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::function::NativeFunction"));
        Assert.assertEquals(-28, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::type::Nil"));
        Assert.assertEquals(-29, BaseStringIndex.getSpecialStringId("Package"));
        Assert.assertEquals(-30, BaseStringIndex.getSpecialStringId("meta::pure::functions::collection::Pair"));
        Assert.assertEquals(-31, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::type::PrimitiveType"));
        Assert.assertEquals(-32, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::extension::Profile"));
        Assert.assertEquals(-33, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::function::property::Property"));
        Assert.assertEquals(-34, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::function::property::QualifiedProperty"));
        Assert.assertEquals(-35, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::ReferenceUsage"));
        Assert.assertEquals(-36, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::relation::Relation"));
        Assert.assertEquals(-37, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::relation::RelationType"));
        Assert.assertEquals(-38, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::treepath::RootRouteNode"));
        Assert.assertEquals(-39, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::extension::Stereotype"));
        Assert.assertEquals(-40, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::extension::Tag"));
        Assert.assertEquals(-41, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::extension::TaggedValue"));
        Assert.assertEquals(-42, BaseStringIndex.getSpecialStringId("Root"));
        Assert.assertEquals(-43, BaseStringIndex.getSpecialStringId("collection"));
        Assert.assertEquals(-44, BaseStringIndex.getSpecialStringId("function"));
        Assert.assertEquals(-45, BaseStringIndex.getSpecialStringId("functions"));
        Assert.assertEquals(-46, BaseStringIndex.getSpecialStringId("meta"));
        Assert.assertEquals(-47, BaseStringIndex.getSpecialStringId("metamodel"));
        Assert.assertEquals(-48, BaseStringIndex.getSpecialStringId("property"));
        Assert.assertEquals(-49, BaseStringIndex.getSpecialStringId("pure"));
        Assert.assertEquals(-50, BaseStringIndex.getSpecialStringId("relationship"));
        Assert.assertEquals(-51, BaseStringIndex.getSpecialStringId("string"));
        Assert.assertEquals(-52, BaseStringIndex.getSpecialStringId("type"));
        Assert.assertEquals(-53, BaseStringIndex.getSpecialStringId("children"));
        Assert.assertEquals(-54, BaseStringIndex.getSpecialStringId("classifierGenericType"));
        Assert.assertEquals(-55, BaseStringIndex.getSpecialStringId("constraints"));
        Assert.assertEquals(-56, BaseStringIndex.getSpecialStringId("expressionSequence"));
        Assert.assertEquals(-57, BaseStringIndex.getSpecialStringId("func"));
        Assert.assertEquals(-58, BaseStringIndex.getSpecialStringId("genericType"));
        Assert.assertEquals(-59, BaseStringIndex.getSpecialStringId("multiplicity"));
        Assert.assertEquals(-60, BaseStringIndex.getSpecialStringId("multiplicityArguments"));
        Assert.assertEquals(-61, BaseStringIndex.getSpecialStringId("multiplicityParameters"));
        Assert.assertEquals(-62, BaseStringIndex.getSpecialStringId("owner"));
        Assert.assertEquals(-63, BaseStringIndex.getSpecialStringId("postConstraints"));
        Assert.assertEquals(-64, BaseStringIndex.getSpecialStringId("preConstraints"));
        Assert.assertEquals(-65, BaseStringIndex.getSpecialStringId("properties"));
        Assert.assertEquals(-66, BaseStringIndex.getSpecialStringId("propertiesFromAssociations"));
        Assert.assertEquals(-67, BaseStringIndex.getSpecialStringId("qualifiedProperties"));
        Assert.assertEquals(-68, BaseStringIndex.getSpecialStringId("qualifiedPropertiesFromAssociations"));
        Assert.assertEquals(-69, BaseStringIndex.getSpecialStringId("rawType"));
        Assert.assertEquals(-70, BaseStringIndex.getSpecialStringId("resolvedEnum"));
        Assert.assertEquals(-71, BaseStringIndex.getSpecialStringId("resolvedNode"));
        Assert.assertEquals(-72, BaseStringIndex.getSpecialStringId("resolvedProperty"));
        Assert.assertEquals(-73, BaseStringIndex.getSpecialStringId("returnMultiplicity"));
        Assert.assertEquals(-74, BaseStringIndex.getSpecialStringId("returnType"));
        Assert.assertEquals(-75, BaseStringIndex.getSpecialStringId("stereotypes"));
        Assert.assertEquals(-76, BaseStringIndex.getSpecialStringId("tag"));
        Assert.assertEquals(-77, BaseStringIndex.getSpecialStringId("taggedValues"));
        Assert.assertEquals(-78, BaseStringIndex.getSpecialStringId("typeArguments"));
        Assert.assertEquals(-79, BaseStringIndex.getSpecialStringId("typeParameters"));
        Assert.assertEquals(-80, BaseStringIndex.getSpecialStringId("values"));
        Assert.assertEquals(-81, BaseStringIndex.getSpecialStringId("Pure"));
        Assert.assertEquals(-82, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::multiplicity::OneMany"));
        Assert.assertEquals(-83, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::multiplicity::PureOne"));
        Assert.assertEquals(-84, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::multiplicity::PureZero"));
        Assert.assertEquals(-85, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::multiplicity::ZeroMany"));
        Assert.assertEquals(-86, BaseStringIndex.getSpecialStringId("meta::pure::metamodel::multiplicity::ZeroOne"));

        Assert.assertEquals(0, BaseStringIndex.getSpecialStringId("not a special string"));
        Assert.assertEquals(0, BaseStringIndex.getSpecialStringId("the quick brown fox"));
        Assert.assertEquals(0, BaseStringIndex.getSpecialStringId("jumped over the lazy dog"));
    }

    @Test
    public void testSpecialStringsById()
    {
        Assert.assertNull(BaseStringIndex.getSpecialString(-1));
        Assert.assertEquals("", BaseStringIndex.getSpecialString(-2));
        Assert.assertEquals("::", BaseStringIndex.getSpecialString(-3));
        Assert.assertEquals("/", BaseStringIndex.getSpecialString(-4));
        Assert.assertEquals(".", BaseStringIndex.getSpecialString(-5));
        Assert.assertEquals("Boolean", BaseStringIndex.getSpecialString(-6));
        Assert.assertEquals("Byte", BaseStringIndex.getSpecialString(-7));
        Assert.assertEquals("Date", BaseStringIndex.getSpecialString(-8));
        Assert.assertEquals("DateTime", BaseStringIndex.getSpecialString(-9));
        Assert.assertEquals("Decimal", BaseStringIndex.getSpecialString(-10));
        Assert.assertEquals("Float", BaseStringIndex.getSpecialString(-11));
        Assert.assertEquals("Integer", BaseStringIndex.getSpecialString(-12));
        Assert.assertEquals("LatestDate", BaseStringIndex.getSpecialString(-13));
        Assert.assertEquals("Number", BaseStringIndex.getSpecialString(-14));
        Assert.assertEquals("StrictDate", BaseStringIndex.getSpecialString(-15));
        Assert.assertEquals("StrictTime", BaseStringIndex.getSpecialString(-16));
        Assert.assertEquals("String", BaseStringIndex.getSpecialString(-17));
        Assert.assertEquals("meta::pure::metamodel::type::Any", BaseStringIndex.getSpecialString(-18));
        Assert.assertEquals("meta::pure::metamodel::relationship::Association", BaseStringIndex.getSpecialString(-19));
        Assert.assertEquals("meta::pure::metamodel::type::Class", BaseStringIndex.getSpecialString(-20));
        Assert.assertEquals("meta::pure::metamodel::function::ConcreteFunctionDefinition", BaseStringIndex.getSpecialString(-21));
        Assert.assertEquals("meta::pure::metamodel::type::Enumeration", BaseStringIndex.getSpecialString(-22));
        Assert.assertEquals("meta::pure::metamodel::import::ImportGroup", BaseStringIndex.getSpecialString(-23));
        Assert.assertEquals("meta::pure::metamodel::function::LambdaFunction", BaseStringIndex.getSpecialString(-24));
        Assert.assertEquals("meta::pure::functions::collection::List", BaseStringIndex.getSpecialString(-25));
        Assert.assertEquals("meta::pure::functions::collection::Map", BaseStringIndex.getSpecialString(-26));
        Assert.assertEquals("meta::pure::metamodel::function::NativeFunction", BaseStringIndex.getSpecialString(-27));
        Assert.assertEquals("meta::pure::metamodel::type::Nil", BaseStringIndex.getSpecialString(-28));
        Assert.assertEquals("Package", BaseStringIndex.getSpecialString(-29));
        Assert.assertEquals("meta::pure::functions::collection::Pair", BaseStringIndex.getSpecialString(-30));
        Assert.assertEquals("meta::pure::metamodel::type::PrimitiveType", BaseStringIndex.getSpecialString(-31));
        Assert.assertEquals("meta::pure::metamodel::extension::Profile", BaseStringIndex.getSpecialString(-32));
        Assert.assertEquals("meta::pure::metamodel::function::property::Property", BaseStringIndex.getSpecialString(-33));
        Assert.assertEquals("meta::pure::metamodel::function::property::QualifiedProperty", BaseStringIndex.getSpecialString(-34));
        Assert.assertEquals("meta::pure::metamodel::ReferenceUsage", BaseStringIndex.getSpecialString(-35));
        Assert.assertEquals("meta::pure::metamodel::relation::Relation", BaseStringIndex.getSpecialString(-36));
        Assert.assertEquals("meta::pure::metamodel::relation::RelationType", BaseStringIndex.getSpecialString(-37));
        Assert.assertEquals("meta::pure::metamodel::treepath::RootRouteNode", BaseStringIndex.getSpecialString(-38));
        Assert.assertEquals("meta::pure::metamodel::extension::Stereotype", BaseStringIndex.getSpecialString(-39));
        Assert.assertEquals("meta::pure::metamodel::extension::Tag", BaseStringIndex.getSpecialString(-40));
        Assert.assertEquals("meta::pure::metamodel::extension::TaggedValue", BaseStringIndex.getSpecialString(-41));
        Assert.assertEquals("Root", BaseStringIndex.getSpecialString(-42));
        Assert.assertEquals("collection", BaseStringIndex.getSpecialString(-43));
        Assert.assertEquals("function", BaseStringIndex.getSpecialString(-44));
        Assert.assertEquals("functions", BaseStringIndex.getSpecialString(-45));
        Assert.assertEquals("meta", BaseStringIndex.getSpecialString(-46));
        Assert.assertEquals("metamodel", BaseStringIndex.getSpecialString(-47));
        Assert.assertEquals("property", BaseStringIndex.getSpecialString(-48));
        Assert.assertEquals("pure", BaseStringIndex.getSpecialString(-49));
        Assert.assertEquals("relationship", BaseStringIndex.getSpecialString(-50));
        Assert.assertEquals("string", BaseStringIndex.getSpecialString(-51));
        Assert.assertEquals("type", BaseStringIndex.getSpecialString(-52));
        Assert.assertEquals("children", BaseStringIndex.getSpecialString(-53));
        Assert.assertEquals("classifierGenericType", BaseStringIndex.getSpecialString(-54));
        Assert.assertEquals("constraints", BaseStringIndex.getSpecialString(-55));
        Assert.assertEquals("expressionSequence", BaseStringIndex.getSpecialString(-56));
        Assert.assertEquals("func", BaseStringIndex.getSpecialString(-57));
        Assert.assertEquals("genericType", BaseStringIndex.getSpecialString(-58));
        Assert.assertEquals("multiplicity", BaseStringIndex.getSpecialString(-59));
        Assert.assertEquals("multiplicityArguments", BaseStringIndex.getSpecialString(-60));
        Assert.assertEquals("multiplicityParameters", BaseStringIndex.getSpecialString(-61));
        Assert.assertEquals("owner", BaseStringIndex.getSpecialString(-62));
        Assert.assertEquals("postConstraints", BaseStringIndex.getSpecialString(-63));
        Assert.assertEquals("preConstraints", BaseStringIndex.getSpecialString(-64));
        Assert.assertEquals("properties", BaseStringIndex.getSpecialString(-65));
        Assert.assertEquals("propertiesFromAssociations", BaseStringIndex.getSpecialString(-66));
        Assert.assertEquals("qualifiedProperties", BaseStringIndex.getSpecialString(-67));
        Assert.assertEquals("qualifiedPropertiesFromAssociations", BaseStringIndex.getSpecialString(-68));
        Assert.assertEquals("rawType", BaseStringIndex.getSpecialString(-69));
        Assert.assertEquals("resolvedEnum", BaseStringIndex.getSpecialString(-70));
        Assert.assertEquals("resolvedNode", BaseStringIndex.getSpecialString(-71));
        Assert.assertEquals("resolvedProperty", BaseStringIndex.getSpecialString(-72));
        Assert.assertEquals("returnMultiplicity", BaseStringIndex.getSpecialString(-73));
        Assert.assertEquals("returnType", BaseStringIndex.getSpecialString(-74));
        Assert.assertEquals("stereotypes", BaseStringIndex.getSpecialString(-75));
        Assert.assertEquals("tag", BaseStringIndex.getSpecialString(-76));
        Assert.assertEquals("taggedValues", BaseStringIndex.getSpecialString(-77));
        Assert.assertEquals("typeArguments", BaseStringIndex.getSpecialString(-78));
        Assert.assertEquals("typeParameters", BaseStringIndex.getSpecialString(-79));
        Assert.assertEquals("values", BaseStringIndex.getSpecialString(-80));
        Assert.assertEquals("Pure", BaseStringIndex.getSpecialString(-81));
        Assert.assertEquals("meta::pure::metamodel::multiplicity::OneMany", BaseStringIndex.getSpecialString(-82));
        Assert.assertEquals("meta::pure::metamodel::multiplicity::PureOne", BaseStringIndex.getSpecialString(-83));
        Assert.assertEquals("meta::pure::metamodel::multiplicity::PureZero", BaseStringIndex.getSpecialString(-84));
        Assert.assertEquals("meta::pure::metamodel::multiplicity::ZeroMany", BaseStringIndex.getSpecialString(-85));
        Assert.assertEquals("meta::pure::metamodel::multiplicity::ZeroOne", BaseStringIndex.getSpecialString(-86));

        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(0));
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(1));
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(10));
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(-87));
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(-89));
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(-530));
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> BaseStringIndex.getSpecialString(-12920));
    }

    @Test
    public void testIsSpecialStringId()
    {
        for (int i = -86; i <= -1; i++)
        {
            Assert.assertTrue(Integer.toString(i), BaseStringIndex.isSpecialStringId(i));
        }

        Assert.assertFalse(BaseStringIndex.isSpecialStringId(0));
        Assert.assertFalse(BaseStringIndex.isSpecialStringId(1));
        Assert.assertFalse(BaseStringIndex.isSpecialStringId(-87));
        Assert.assertFalse(BaseStringIndex.isSpecialStringId(-88));
        Assert.assertFalse(BaseStringIndex.isSpecialStringId(-129));
        Assert.assertFalse(BaseStringIndex.isSpecialStringId(-13550));
    }

    @Test
    public void testForEachSpecialString()
    {
        MutableList<String> expected = Lists.mutable.with(
                null,
                "",
                "::",
                "/",
                ".",
                "Boolean",
                "Byte",
                "Date",
                "DateTime",
                "Decimal",
                "Float",
                "Integer",
                "LatestDate",
                "Number",
                "StrictDate",
                "StrictTime",
                "String",
                "meta::pure::metamodel::type::Any",
                "meta::pure::metamodel::relationship::Association",
                "meta::pure::metamodel::type::Class",
                "meta::pure::metamodel::function::ConcreteFunctionDefinition",
                "meta::pure::metamodel::type::Enumeration",
                "meta::pure::metamodel::import::ImportGroup",
                "meta::pure::metamodel::function::LambdaFunction",
                "meta::pure::functions::collection::List",
                "meta::pure::functions::collection::Map",
                "meta::pure::metamodel::function::NativeFunction",
                "meta::pure::metamodel::type::Nil",
                "Package",
                "meta::pure::functions::collection::Pair",
                "meta::pure::metamodel::type::PrimitiveType",
                "meta::pure::metamodel::extension::Profile",
                "meta::pure::metamodel::function::property::Property",
                "meta::pure::metamodel::function::property::QualifiedProperty",
                "meta::pure::metamodel::ReferenceUsage",
                "meta::pure::metamodel::relation::Relation",
                "meta::pure::metamodel::relation::RelationType",
                "meta::pure::metamodel::treepath::RootRouteNode",
                "meta::pure::metamodel::extension::Stereotype",
                "meta::pure::metamodel::extension::Tag",
                "meta::pure::metamodel::extension::TaggedValue",
                "Root",
                "collection",
                "function",
                "functions",
                "meta",
                "metamodel",
                "property",
                "pure",
                "relationship",
                "string",
                "type",
                "children",
                "classifierGenericType",
                "constraints",
                "expressionSequence",
                "func",
                "genericType",
                "multiplicity",
                "multiplicityArguments",
                "multiplicityParameters",
                "owner",
                "postConstraints",
                "preConstraints",
                "properties",
                "propertiesFromAssociations",
                "qualifiedProperties",
                "qualifiedPropertiesFromAssociations",
                "rawType",
                "resolvedEnum",
                "resolvedNode",
                "resolvedProperty",
                "returnMultiplicity",
                "returnType",
                "stereotypes",
                "tag",
                "taggedValues",
                "typeArguments",
                "typeParameters",
                "values",
                "Pure",
                "meta::pure::metamodel::multiplicity::OneMany",
                "meta::pure::metamodel::multiplicity::PureOne",
                "meta::pure::metamodel::multiplicity::PureZero",
                "meta::pure::metamodel::multiplicity::ZeroMany",
                "meta::pure::metamodel::multiplicity::ZeroOne"
                );

        MutableList<String> actual = Lists.mutable.empty();
        BaseStringIndex.forEachSpecialString(actual::add);
        Assert.assertEquals(expected, actual);
        Assert.assertTrue(actual.size() <= Byte.MAX_VALUE);
        Assert.assertEquals("duplicates", Sets.mutable.withAll(actual).size(), actual.size());
    }
}
