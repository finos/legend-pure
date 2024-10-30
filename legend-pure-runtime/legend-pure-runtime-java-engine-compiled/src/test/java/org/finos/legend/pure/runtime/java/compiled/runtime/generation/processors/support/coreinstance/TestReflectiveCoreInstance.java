// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime.generation.processors.support.coreinstance;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ReflectiveCoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestReflectiveCoreInstance
{
    @Test
    public void testEmptyKeyIndex()
    {
        ReflectiveCoreInstance.KeyIndex empty = ReflectiveCoreInstance.keyIndexBuilder().build();
        Assert.assertEquals(Sets.immutable.empty(), empty.getKeys().toSet());
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> empty.getRealKeyByName("fakeKey"));
        Assert.assertEquals("Unsupported key: fakeKey", e.getMessage());
    }

    @Test
    public void testKeyIndex()
    {
        ReflectiveCoreInstance.KeyIndex index = ReflectiveCoreInstance.keyIndexBuilder(18)
                .withKey("Root::meta::pure::metamodel::extension::ElementWithConstraints", "constraints")
                .withKey("Root::meta::pure::metamodel::extension::ElementWithStereotypes", "stereotypes")
                .withKey("Root::meta::pure::metamodel::extension::ElementWithTaggedValues", "taggedValues")
                .withKey("Root::meta::pure::test::Testable", "tests")
                .withKeys("Root::meta::pure::metamodel::type::Type", "generalizations", "name", "specializations")
                .withKeys("Root::meta::pure::metamodel::type::Class", "multiplicityParameters", "originalMilestonedProperties", "properties", "propertiesFromAssociations", "qualifiedProperties", "qualifiedPropertiesFromAssociations", "typeParameters")
                .withKeys("Root::meta::pure::metamodel::PackageableElement", "package", "referenceUsages")
                .withKeys("Root::meta::pure::metamodel::type::Any", "classifierGenericType", "elementOverride")
                .build();

        Assert.assertEquals(
                Sets.mutable.with("constraints", "tests", "generalizations", "name", "specializations", "stereotypes", "taggedValues", "multiplicityParameters", "originalMilestonedProperties", "properties", "propertiesFromAssociations", "qualifiedProperties", "qualifiedPropertiesFromAssociations", "typeParameters", "package", "referenceUsages", "classifierGenericType", "elementOverride"),
                index.getKeys().toSet());

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "extension", "children", "ElementWithConstraints", "properties", "constraints"),
                index.getRealKeyByName("constraints"));

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "extension", "children", "ElementWithStereotypes", "properties", "stereotypes"),
                index.getRealKeyByName("stereotypes"));

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "extension", "children", "ElementWithTaggedValues", "properties", "taggedValues"),
                index.getRealKeyByName("taggedValues"));

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "test", "children", "Testable", "properties", "tests"),
                index.getRealKeyByName("tests"));

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Type", "properties", "generalizations"),
                index.getRealKeyByName("generalizations"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Type", "properties", "name"),
                index.getRealKeyByName("name"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Type", "properties", "specializations"),
                index.getRealKeyByName("specializations"));

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "multiplicityParameters"),
                index.getRealKeyByName("multiplicityParameters"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "originalMilestonedProperties"),
                index.getRealKeyByName("originalMilestonedProperties"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "properties"),
                index.getRealKeyByName("properties"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "propertiesFromAssociations"),
                index.getRealKeyByName("propertiesFromAssociations"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "qualifiedProperties"),
                index.getRealKeyByName("qualifiedProperties"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "qualifiedPropertiesFromAssociations"),
                index.getRealKeyByName("qualifiedPropertiesFromAssociations"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Class", "properties", "typeParameters"),
                index.getRealKeyByName("typeParameters"));

        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "PackageableElement", "properties", "package"),
                index.getRealKeyByName("package"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "PackageableElement", "properties", "referenceUsages"),
                index.getRealKeyByName("referenceUsages"));


        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Any", "properties", "classifierGenericType"),
                index.getRealKeyByName("classifierGenericType"));
        Assert.assertEquals(
                Lists.immutable.with("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Any", "properties", "elementOverride"),
                index.getRealKeyByName("elementOverride"));

        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> index.getRealKeyByName("fakeKey"));
        Assert.assertEquals("Unsupported key: fakeKey", e.getMessage());
    }
}
