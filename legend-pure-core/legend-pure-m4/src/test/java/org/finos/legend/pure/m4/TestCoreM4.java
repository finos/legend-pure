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

package org.finos.legend.pure.m4;

import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.Assert;
import org.junit.Test;

public class TestCoreM4
{
    @Test
    public void testReferentialIntegrity()
    {
        ModelRepository repository = new ModelRepository();
        try
        {
            new M4Parser().parse("^Class Class\n" +
                    "{\n" +
                    "    Element.properties[name] : 'Class'\n" +
                    "}\n" +
                    "^Class String" +
                    "{" +
                    "}\n" +
                    "^Class Element\n" +
                    "{\n" +
                    "    Element.properties[name] : 'Element',\n" +
                    "    Class.properties[properties] :\n" +
                    "        [\n" +
                    "            ^Property name\n" +
                    "                {\n" +
                    "                    Property.properties[type] : String\n" +
                    "                }\n" +
                    "        ]\n" +
                    "}\n", repository, new VoidM4StateListener());

            Assert.assertEquals("Class_0 instance Class_0\n" +
                    "    name_6(Property_5):\n" +
                    "        Class_2 instance String_1", repository.getTopLevel("Class").printFull(""));

            try
            {
                repository.getTopLevel("Element").printFull("");
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertEquals("Error resolving path [Class, properties, properties]: 'properties' is unknown for the key 'properties' in 'Class'", e.getMessage());
            }

            repository.validate(new VoidM4StateListener());

            Assert.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assert.assertNotNull(pe);
            Assert.assertTrue(pe instanceof PureCompilationException);
            Assert.assertEquals("Property has not been defined!", pe.getInfo());
        }
    }


    @Test
    public void testSimpleConsistent() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("^Class PrimitiveType\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "^PrimitiveType String\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "^Class Class\n" +
                "{\n" +
                "    Element.properties[name] : 'Class',\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property properties\n" +
                "                {\n" +
                "                    Property.properties[type] : Property\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Class Element\n" +
                "{\n" +
                "    Element.properties[name] : 'Element',\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property name\n" +
                "                {\n" +
                "                    Property.properties[type] : String\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Class Property\n" +
                "{\n" +
                "    Element.properties[name] : 'Property',\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property type\n" +
                "                {\n" +
                "                    Property.properties[type] : Class\n" +
                "                }\n" +
                "        ]\n" +
                "}\n", repository, new VoidM4StateListener());

        repository.validate(new VoidM4StateListener());

        Assert.assertEquals("Class_0 instance Class_0\n" +
                "    name_8(Property_4):\n" +
                "        Class_3 instance String_2\n" +
                "    properties_5(Property_4):\n" +
                "        properties_5 instance Property_4\n" +
                "            type_10(Property_4):\n" +
                "                Property_4 instance Class_0\n" +
                "                    name_8(Property_4):\n" +
                "                        Property_9 instance String_2\n" +
                "                    properties_5(Property_4):\n" +
                "                        type_10 instance Property_4\n" +
                "                            type_10(Property_4):\n" +
                "                                Class_0 instance Class_0\n" +
                "                                    [...]", repository.getTopLevel("Class").printFull(""));

        Assert.assertEquals("Element_6 instance Class_0\n" +
                "    name_8(Property_4):\n" +
                "        Element_7 instance String_2\n" +
                "    properties_5(Property_4):\n" +
                "        name_8 instance Property_4\n" +
                "            type_10(Property_4):\n" +
                "                String_2 instance PrimitiveType_1", repository.getTopLevel("Element").printFull(""));

    }

    @Test
    public void testNameSpacing() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        String body = "^Class Class\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property properties\n" +
                "                {\n" +
                "                    Property.properties[type] : Property\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Class Property\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property type\n" +
                "                {\n" +
                "                    Property.properties[type] : Class\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Class Package\n" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property children\n" +
                "            {\n" +
                "                Property.properties[type] : Class\n" +
                "            }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Package Root\n" +
                "{\n" +
                "    Package.properties[children] :\n" +
                "        [\n" +
                "            ^Package subPackage\n" +
                "            {\n" +
                "                Package.properties[children] :\n" +
                "                                [\n" +
                "                                    ^Class ClassTest\n" +
                "                                    {\n" +
                "\n" +
                "                                    }\n" +
                "                                ]\n" +
                "            }\n" +
                "        ]\n" +
                "}\n" +
                "\n" +
                "^Root.children[subPackage].children[ClassTest] T\n" +
                "{\n" +
                "\n" +
                "}\n" +
                "\n" +
                "^Class NewInstance @Root.children[subPackage].children\n" +
                "{\n" +
                "\n" +
                "}";

        new M4Parser().parse(body, repository, new VoidM4StateListener());
        repository.validate(new VoidM4StateListener());

        Assert.assertEquals("T_9 instance ClassTest_8", repository.getTopLevel("T").printFull(""));

        Assert.assertEquals("Root_6 instance Package_4\n" +
                "    children_5(Property_1):\n" +
                "        subPackage_7 instance Package_4\n" +
                "            children_5(Property_1):\n" +
                "                ClassTest_8 instance Class_0\n" +
                "                NewInstance_10 instance Class_0", repository.getTopLevel("Root").printFull(""));
    }


    @Test
    public void testCodeLineAnnotation()
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("^Class Class ?[a/b/c/file.txt:1,3,1,9,8,9]? \n" +
                "{\n" +
                "    Element.properties[name] : 'Class',\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property properties\n" +
                "                {\n" +
                "                    Property.properties[type] : Property\n" +
                "                }\n" +
                "        ]\n" +
                "}\n" +
                "^Class String" +
                "{" +
                "}" +
                "^Class Property" +
                "{\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property type\n" +
                "                {\n" +
                "                    Property.properties[type] : String\n" +
                "                }\n" +
                "        ]\n" +
                "}" +
                "^Class Element ?[otherElement:4,5,4,11,10,19]?\n" +
                "{\n" +
                "    Element.properties[name] : 'Element',\n" +
                "    Class.properties[properties] :\n" +
                "        [\n" +
                "            ^Property name ?[file:5,7,5,7,14,22]?\n" +
                "                {\n" +
                "                    Property.properties[type] : String\n" +
                "                }\n" +
                "        ]\n" +
                "}\n", repository, new VoidM4StateListener());

        Assert.assertEquals("Class_0(a/b/c/file.txt:1,3,1,9,8,9) instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)\n" +
                "    name_8(file:5,7,5,7,14,22)(Property_3):\n" +
                "        Class_2 instance String_1\n" +
                "    properties_4(Property_3):\n" +
                "        properties_4 instance Property_3\n" +
                "            type_5(Property_3):\n" +
                "                Property_3 instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)\n" +
                "                    properties_4(Property_3):\n" +
                "                        type_5 instance Property_3\n" +
                "                            type_5(Property_3):\n" +
                "                                String_1 instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)", repository.getTopLevel("Class").printFull(""));

        Assert.assertEquals("Element_6(otherElement:4,5,4,11,10,19) instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)\n" +
                "    name_8(file:5,7,5,7,14,22)(Property_3):\n" +
                "        Element_7 instance String_1\n" +
                "    properties_4(Property_3):\n" +
                "        name_8(file:5,7,5,7,14,22) instance Property_3\n" +
                "            type_5(Property_3):\n" +
                "                String_1 instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)", repository.getTopLevel("Element").printFull(""));
    }


    @Test
    public void testForwardReference() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("^Root.children[core].children[Package] Root\n" +
                "{\n" +
                "    Root.children[core].children[Any].properties[name] : 'Root',\n" +
                "    Root.children[core].children[Package].properties[children] : [\n" +
                "                                        ^Root.children[core].children[Package] core\n" +
                "                                        {\n" +
                "                                            Root.children[core].children[Any].properties[name] : 'core',\n" +
                "                                            Root.children[core].children[Package].properties[children] : []\n" +
                "                                        }\n" +
                "                                    ]\n" +
                "}\n" +
                "^Root.children[core].children[Class] Property @Root.children[core].children" +
                "{" +
                "}" +
                "^Root.children[core].children[Class] String" +
                "{" +
                "}" +
                "^Root.children[core].children[Class] Package @Root.children[core].children\n" +
                "{\n" +
                "    Root.children[core].children[Any].properties[name] : 'Package',\n" +
                "    Root.children[core].children[Class].properties[properties] :\n" +
                "        [\n" +
                "            ^Root.children[core].children[Property] children\n" +
                "            {\n" +
                "                Root.children[core].children[Any].properties[name] : 'children'\n" +
                "            }\n" +
                "        ]\n" +
                "}\n" +
                "^Root.children[core].children[Class] Class @Root.children[core].children\n" +
                "{\n" +
                "    Root.children[core].children[Class].properties[properties] :\n" +
                "        [\n" +
                "            ^Root.children[core].children[Property] properties\n" +
                "                {\n" +
                "                    Root.children[core].children[Any].properties[name] : 'properties'\n" +
                "                }\n" +
                "        ]" +
                "}" +
                "^Root.children[core].children[Class] Any @Root.children[core].children\n" +
                "{\n" +
                "    Root.children[core].children[Any].properties[name] : 'Any',\n" +
                "    Root.children[core].children[Class].properties[properties] :\n" +
                "        [\n" +
                "            ^Root.children[core].children[Property] name\n" +
                "                {\n" +
                "                    Root.children[core].children[Any].properties[name] : 'classifierGenericType'\n" +
                "                }\n" +
                "        ]\n" +
                "}\n", repository, new VoidM4StateListener());
        repository.validate(new VoidM4StateListener());
    }
}
