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

package org.finos.legend.pure.m3.tests.elements.property;

import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProperty extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }


    @Test
    public void testGetPath()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A\n" +
                "{\n" +
                "   prop1 : String[1];\n" +
                "   prop2 : String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  prop3 : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "Association test::AB\n" +
                "{\n" +
                "  aToB : B[*];\n" +
                "  bToA : A[*];\n" +
                "}\n" +
                "\n" +
                "Class test::subpackage::C extends A\n" +
                "{\n" +
                "  prop2 : String[0..1];\n" +
                "  prop4 : String[1];\n" +
                "}");

        CoreInstance classA = this.runtime.getCoreInstance("test::A");
        Assert.assertNotNull(classA);
        MapIterable<String, CoreInstance> classAProperties = this.processorSupport.class_getSimplePropertiesByName(classA);
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "properties", "prop1"), this.processorSupport.property_getPath(classAProperties.get("prop1")));
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "properties", "prop2"), this.processorSupport.property_getPath(classAProperties.get("prop2")));
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "propertiesFromAssociations", "aToB"), this.processorSupport.property_getPath(classAProperties.get("aToB")));

        CoreInstance classB = this.runtime.getCoreInstance("test::B");
        Assert.assertNotNull(classB);
        MapIterable<String, CoreInstance> classBProperties = this.processorSupport.class_getSimplePropertiesByName(classB);
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "B", "properties", "prop3"), this.processorSupport.property_getPath(classBProperties.get("prop3")));
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "B", "propertiesFromAssociations", "bToA"), this.processorSupport.property_getPath(classBProperties.get("bToA")));

        CoreInstance classC = this.runtime.getCoreInstance("test::subpackage::C");
        Assert.assertNotNull(classC);
        MapIterable<String, CoreInstance> classCProperties = this.processorSupport.class_getSimplePropertiesByName(classC);
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "properties", "prop1"), this.processorSupport.property_getPath(classCProperties.get("prop1")));
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "subpackage", "children", "C", "properties", "prop2"), this.processorSupport.property_getPath(classCProperties.get("prop2")));
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "subpackage", "children", "C", "properties", "prop4"), this.processorSupport.property_getPath(classCProperties.get("prop4")));
        Assert.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "propertiesFromAssociations", "aToB"), this.processorSupport.property_getPath(classCProperties.get("aToB")));
    }

    @Test
    public void testGetSourceType()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A\n" +
                "{\n" +
                "   prop1 : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  prop2 : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "Association test::AB\n" +
                "{\n" +
                "  aToB : B[*];\n" +
                "  bToA : A[*];\n" +
                "}");

        CoreInstance classA = this.runtime.getCoreInstance("test::A");
        CoreInstance classB = this.runtime.getCoreInstance("test::B");
        CoreInstance associationAB = this.runtime.getCoreInstance("test::AB");

        Assert.assertNotNull(classA);
        Assert.assertNotNull(classB);
        Assert.assertNotNull(associationAB);

        CoreInstance prop1 = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "prop1");
        CoreInstance prop2 = classB.getValueInValueForMetaPropertyToMany(M3Properties.properties, "prop2");
        CoreInstance aToB = associationAB.getValueInValueForMetaPropertyToMany(M3Properties.properties, "aToB");
        CoreInstance bToA = associationAB.getValueInValueForMetaPropertyToMany(M3Properties.properties, "bToA");

        Assert.assertNotNull(prop1);
        Assert.assertNotNull(prop2);
        Assert.assertNotNull(aToB);
        Assert.assertNotNull(bToA);

        Assert.assertSame(classA, Property.getSourceType(prop1, this.processorSupport));
        Assert.assertSame(classB, Property.getSourceType(prop2, this.processorSupport));
        Assert.assertSame(classA, Property.getSourceType(aToB, this.processorSupport));
        Assert.assertSame(classB, Property.getSourceType(bToA, this.processorSupport));
    }
}
